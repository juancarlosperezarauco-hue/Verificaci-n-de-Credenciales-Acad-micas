package bo.edu.uagrm.credenciales.identityreview;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.identity.StudentAccountRepository;
import bo.edu.uagrm.credenciales.storage.ManagedStorage;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional
public class EvidenceLifecycle {
 private final JdbcTemplate jdbc;private final ManagedStorage storage;private final StudentAccountRepository accounts;private final AuditTrail audit;private final Clock clock;private final int days;private final IdentitySubmissionRepository reviews;private final IdentityReviewService verification;private final bo.edu.uagrm.credenciales.consent.ConsentRepository consents;
 public EvidenceLifecycle(JdbcTemplate j,ManagedStorage s,StudentAccountRepository a,AuditTrail audit,Clock c,IdentitySubmissionRepository reviews,IdentityReviewService verification,bo.edu.uagrm.credenciales.consent.ConsentRepository consents,@Value("${app.evidence.retention-days:90}") int days){if(days<7||days>3650)throw new IllegalArgumentException("Retención de evidencia entre 7 y 3650 días.");jdbc=j;storage=s;accounts=a;this.audit=audit;clock=c;this.days=days;this.reviews=reviews;this.verification=verification;this.consents=consents;}
 public int days(){return days;}
 @EventListener @Transactional(propagation=Propagation.MANDATORY)
 public void profile(EvidenceEvents.ProfileChanged event){jdbc.update("update identity_review_submissions set retired_at=? where student_id=? and retired_at is null",Timestamp.from(clock.instant()),event.student());if(event.deactivated())jdbc.update("update identity_disputes set retired_at=? where student_id=? and retired_at is null",Timestamp.from(clock.instant()),event.student());}
 @EventListener @Transactional(propagation=Propagation.MANDATORY)
 public void consent(EvidenceEvents.ConsentWithdrawn event){jdbc.update("update identity_review_submissions set retired_at=? where consent_id=? and retired_at is null",Timestamp.from(clock.instant()),event.consent());}
 public int purgeExpired(){var cutoff=Timestamp.from(clock.instant().minus(Duration.ofDays(days)));int count=0;
  for(String table:List.of("identity_review_submissions","identity_disputes")){
   var candidates=jdbc.queryForList("select id,student_id from "+table+" where retired_at<? and files_purged_at is null order by retired_at limit 50",cutoff);
   for(var candidate:candidates){UUID id=(UUID)candidate.get("id"),student=(UUID)candidate.get("student_id");var account=accounts.findLockedById(student).orElseThrow();var rows=jdbc.queryForList("select * from "+table+" where id=? and retired_at<? and files_purged_at is null for update",id,cutoff);if(rows.isEmpty())continue;var row=rows.getFirst();UUID tenant=table.equals("identity_review_submissions")?(UUID)row.get("tenant_id"):null;
    boolean live=false;if(account.isActive()){if(table.equals("identity_review_submissions")){var review=reviews.findById(id).orElseThrow();live=review.withdrawnAt==null&&review.status!=IdentitySubmission.Status.REJECTED&&verification.current(review,account)&&consents.findById(review.consentId).map(c->c.getUsedAt()!=null&&c.getRevokedAt()==null).orElse(false);}else live=Set.of("OPEN","OBSERVED").contains(row.get("status"));}if(live){jdbc.update("update "+table+" set retired_at=null where id=?",id);continue;}
    for(String part:List.of("front","back","photo")){if(part.equals("photo")&&table.equals("identity_disputes"))continue;storage.retireIdentityObject((String)row.get(part+"_key"),id,tenant,(String)row.get(part+"_hash"));}
    jdbc.update("update "+table+" set front_key=null,back_key=null,"+(table.equals("identity_review_submissions")?"photo_key=null,":"")+"files_purged_at=? where id=?",Timestamp.from(clock.instant()),id);audit.record(AuditAction.EVIDENCE_RETIRED,tenant,table.equals("identity_disputes")?AuditTrail.Resource.IDENTITY_DISPUTE:AuditTrail.Resource.IDENTITY_REVIEW,id);count++;
   }
  }return count;
 }
}
