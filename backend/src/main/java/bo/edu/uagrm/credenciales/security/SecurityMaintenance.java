package bo.edu.uagrm.credenciales.security;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.mail.*;
import bo.edu.uagrm.credenciales.user.AppUserRepository;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class SecurityMaintenance {
 private final EntityManager em;private final Clock clock;private final MfaCrypto mfa;private final MailOutboxStore mail;private final MailCipher mailCipher;private final JwtService jwt;private final AuditTrail audit;private final AppUserRepository users;private final int days;private final boolean enabled;
 private static final UUID POLICY_ID=UUID.fromString("00000000-0000-0000-0000-000000000017");
 public SecurityMaintenance(EntityManager em,Clock clock,MfaCrypto mfa,MailOutboxStore mail,MailCipher mailCipher,JwtService jwt,AuditTrail audit,AppUserRepository users,@Value("${app.security.retention-days:30}") int days,@Value("${app.security.maintenance-enabled:true}") boolean enabled){
  if(days<7||days>365)throw new IllegalArgumentException("La retención debe ser de 7 a 365 días");this.em=em;this.clock=clock;this.mfa=mfa;this.mail=mail;this.mailCipher=mailCipher;this.jwt=jwt;this.audit=audit;this.users=users;this.days=days;this.enabled=enabled;
 }
 public record Signer(String keyId,long sessions){}
 public record Status(int retentionDays,int cleanupBatch,int encryptionBatch,boolean scheduled,String jwtKeyId,String mfaKeyId,String mailKeyId,long pendingMfa,long pendingMail,List<Signer> activeJwtByKey){}
 public record Reencrypted(int mfa,int mail){}
 public record Cleaned(int sessions,int challenges,int backups,int mail,int tokens,int preparations){public int total(){return sessions+challenges+backups+mail+tokens+preparations;}}
 private String oldMfa(){return "("+mfa.oldPredicate("m.encryptedSecret")+" or "+mfa.oldPredicate("m.pendingSecret")+")";}
 @Transactional(readOnly=true)
 public Status status(){
  var signers=em.createQuery("select s.signingKeyId,count(s) from LoginSession s where s.revokedAt is null and s.expiresAt>:now group by s.signingKeyId order by s.signingKeyId",Object[].class).setParameter("now",clock.instant()).getResultList().stream().map(r->new Signer((String)r[0],(Long)r[1])).toList();
  long pending=em.createQuery("select count(m) from UserMfa m where "+oldMfa(),Long.class).getSingleResult();
  return new Status(days,500,50,enabled,jwt.currentId(),mfa.currentId(),mailCipher.currentId(),pending,mail.rotationPending(),signers);
 }
 public Reencrypted reEncrypt(){
  var rows=em.createQuery("select m from UserMfa m where "+oldMfa()+" order by m.userId",UserMfa.class).setMaxResults(50).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
  for(var row:rows){
   String old=mfa.needsRotation(row.encryptedSecret)?mfa.keyId(row.encryptedSecret):mfa.keyId(row.pendingSecret);
   if(mfa.needsRotation(row.encryptedSecret))row.encryptedSecret=mfa.encrypt(row.userId,mfa.decrypt(row.userId,row.encryptedSecret));
   if(mfa.needsRotation(row.pendingSecret))row.pendingSecret=mfa.encrypt(row.userId,mfa.decrypt(row.userId,row.pendingSecret));
   var owner=users.findById(row.userId).orElseThrow();UUID tenant=owner.getTenant()==null?null:owner.getTenant().getId();audit.record(AuditAction.SECRET_REENCRYPTED,tenant,AuditTrail.Resource.MFA,row.userId,AuditTrail.Change.keyIds(old,mfa.currentId()));
  }
  return new Reencrypted(rows.size(),mail.reEncrypt());
 }
 public Cleaned cleanup(){
  Instant now=clock.instant(),cutoff=now.minus(Duration.ofDays(days));
  int messages=purge("MailOutbox","m","m.terminalAt<:cut and m.encryptedPayload is null and m.state in (bo.edu.uagrm.credenciales.mail.MailState.SENT,bo.edu.uagrm.credenciales.mail.MailState.FAILED,bo.edu.uagrm.credenciales.mail.MailState.CANCELLED)","cut",cutoff);
  int tokens=purge("StudentAccountToken","t","t.expiresAt<:cut and not exists (select m.id from MailOutbox m where m.tokenId=t.id)","cut",cutoff);
  int sessions=purge("LoginSession","s","s.expiresAt<:cut or s.revokedAt<:cut","cut",cutoff);
  int challenges=purge("MfaChallenge","c","c.expiresAt<:cut","cut",cutoff);
  int backups=purge("MfaRecoveryCode","r","r.used=true and r.usedAt<:cut","cut",cutoff);
  var ids=em.createQuery("select m.userId from UserMfa m where m.pendingUntil<=:now",UUID.class).setParameter("now",now).setMaxResults(500).getResultList();
  int preparations=ids.isEmpty()?0:em.createQuery("update UserMfa m set m.pendingSecret=null,m.pendingUntil=null,m.encryptedSecret=case when m.enabled=false then null else m.encryptedSecret end where m.userId in :ids and m.pendingUntil<=:now").setParameter("ids",ids).setParameter("now",now).executeUpdate();
  var result=new Cleaned(sessions,challenges,backups,messages,tokens,preparations);
  if(result.total()>0)audit.record(AuditAction.SECURITY_RECORDS_PURGED,null,AuditTrail.Resource.SECURITY,POLICY_ID,AuditTrail.Change.purged(result.total()));
  return result;
 }
 private int purge(String entity,String alias,String condition,String parameter,Instant cutoff){
  var ids=em.createQuery("select "+alias+".id from "+entity+" "+alias+" where ("+condition+") order by "+alias+".id",UUID.class).setParameter(parameter,cutoff).setMaxResults(500).getResultList();
  return ids.isEmpty()?0:em.createQuery("delete from "+entity+" "+alias+" where "+alias+".id in :ids and ("+condition+")").setParameter("ids",ids).setParameter(parameter,cutoff).executeUpdate();
 }
}
