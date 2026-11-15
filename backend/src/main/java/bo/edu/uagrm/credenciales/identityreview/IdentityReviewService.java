package bo.edu.uagrm.credenciales.identityreview;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.consent.ConsentRepository;
import bo.edu.uagrm.credenciales.identity.*;
import bo.edu.uagrm.credenciales.platform.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.tenant.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class IdentityReviewService {
    private final IdentitySubmissionRepository submissions;
    private final StudentAccountRepository accounts;
    private final ConsentRepository consents;
    private final TenantRepository tenants;
    private final GovernanceRepository governance;
    private final DocumentStoragePort storage;
    private final IdentityImages images;
    private final ObjectMapper json;
    private final Clock clock;
    private final AuditTrail audit;

    public IdentityReviewService(IdentitySubmissionRepository submissions,StudentAccountRepository accounts,
        ConsentRepository consents,TenantRepository tenants,GovernanceRepository governance,
        DocumentStoragePort storage,IdentityImages images,ObjectMapper json,Clock clock,AuditTrail audit) {
        this.submissions=submissions;this.accounts=accounts;this.consents=consents;this.tenants=tenants;
        this.governance=governance;this.storage=storage;this.images=images;this.json=json;this.clock=clock;this.audit=audit;
    }

    @Transactional(readOnly=true)
    public IdentityReviewDtos.Own own(UUID studentId) {
        var student=account(studentId,false);
        var targets=consents.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream()
            .filter(c->c.getUsedAt()!=null&&c.getRevokedAt()==null&&active(c.getTenant()))
            .map(c->new IdentityReviewDtos.Target(c.getTenant().getId(),c.getTenant().getName(),c.getTenant().getSlug()))
            .distinct().sorted(Comparator.comparing(IdentityReviewDtos.Target::name)).toList();
        var history=submissions.findAllByStudentIdOrderBySubmittedAtDesc(studentId).stream().map(s->view(s,student)).toList();
        return new IdentityReviewDtos.Own(student.isProfileComplete(),student.isEmailVerified(),targets,history);
    }

    public IdentityReviewDtos.Item submit(UUID studentId,UUID tenantId,boolean accepted,
        MultipartFile front,MultipartFile back,MultipartFile photo) {
        var student=account(studentId,true);
        if(!student.isProfileComplete()||!student.isEmailVerified())
            throw new IllegalArgumentException("Completa tu perfil y confirma tu correo antes de enviar documentos.");
        if(!accepted)throw new IllegalArgumentException("Debes aceptar el envío de estos documentos a la institución elegida.");
        UUID consent=linked(studentId,tenantId,null);
        String snapshot=encode(IdentityReviewDtos.Snapshot.from(student));
        String fingerprint=fingerprint(student);
        var previous=submissions.findAllByStudentIdAndTenantIdAndWithdrawnAtIsNullOrderBySubmittedAtDesc(studentId,tenantId);
        for(var row:previous)if(row.consentId.equals(consent)&&row.profileHash.equals(fingerprint)&&(row.status==IdentitySubmission.Status.PENDING||row.status==IdentitySubmission.Status.VERIFIED))
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        // Validate every file before making any external write.
        byte[] cleanFront=images.normalize(front),cleanBack=images.normalize(back);
        byte[] cleanPhoto=photo==null||photo.isEmpty()?null:images.normalize(photo);
        var row=new IdentitySubmission();row.id=UUID.randomUUID();row.studentId=studentId;row.tenantId=tenantId;
        row.consentId=consent;
        row.status=IdentitySubmission.Status.PENDING;row.profileJson=snapshot;row.profileHash=fingerprint;
        row.submittedAt=clock.instant();row.acceptedAt=row.submittedAt;
        String prefix="private/identity/"+studentId+"/"+row.id+"/";
        row.frontKey=store(prefix+"front.jpg",cleanFront);row.frontHash=IdentityImages.hash(cleanFront);
        row.backKey=store(prefix+"back.jpg",cleanBack);row.backHash=IdentityImages.hash(cleanBack);
        if(cleanPhoto!=null){row.photoKey=store(prefix+"photo.jpg",cleanPhoto);row.photoHash=IdentityImages.hash(cleanPhoto);}
        // Superseded pending requests release the unique slot, retaining their history.
        for(var old:previous)if(old.status==IdentitySubmission.Status.PENDING){old.withdrawnAt=clock.instant();}
        submissions.flush();submissions.saveAndFlush(row);
        audit.record(AuditAction.IDENTITY_SUBMITTED,tenantId,AuditTrail.Resource.IDENTITY_REVIEW,row.id);
        return view(row,student);
    }

    public void withdraw(UUID studentId,UUID id) {
        account(studentId,true);
        var row=submissions.findByIdAndStudentId(id,studentId).orElseThrow(IdentityReviewService::missing);
        if(row.withdrawnAt==null){row.withdrawnAt=clock.instant();audit.record(AuditAction.IDENTITY_WITHDRAWN,row.tenantId,AuditTrail.Resource.IDENTITY_REVIEW,row.id);}
    }

    @Transactional(readOnly=true)
    public IdentityReviewDtos.Results inbox(UserPrincipal p,IdentitySubmission.Status status,int page,int size) {
        UUID tenant=tenant(p);if(page<0||size<1||size>50)throw new IllegalArgumentException("Paginación inválida.");
        var result=submissions.inbox(tenant,status,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"submittedAt").and(Sort.by("id"))));
        return new IdentityReviewDtos.Results(result.stream().map(s->view(s,account(s.studentId,false))).toList(),result.getTotalElements(),page,size);
    }

    public IdentityReviewDtos.Item detail(UserPrincipal p,UUID id) {
        var row=authorizedRow(p,id);return view(row,account(row.studentId,false));
    }

    public IdentityReviewDtos.Item decide(UserPrincipal p,UUID id,IdentityReviewDtos.Decision decision) {
        UUID tenant=tenant(p);
        var row=authorizedRow(p,id);var student=account(row.studentId,false);
        if(row.status!=IdentitySubmission.Status.PENDING||!current(row,student))throw new ResponseStatusException(HttpStatus.CONFLICT);
        if(decision.status()!=IdentitySubmission.Status.VERIFIED&&decision.status()!=IdentitySubmission.Status.REJECTED)
            throw new IllegalArgumentException("Selecciona aprobar o rechazar.");
        String reason=decision.reason()==null?null:decision.reason().strip();
        if(decision.status()==IdentitySubmission.Status.REJECTED&&(reason==null||reason.length()<10))
            throw new IllegalArgumentException("Explica el rechazo con al menos 10 caracteres.");
        row.status=decision.status();row.reason=row.status==IdentitySubmission.Status.REJECTED?reason:null;
        row.reviewerId=p.userId();row.reviewedAt=clock.instant();
        audit.record(row.status==IdentitySubmission.Status.VERIFIED?AuditAction.IDENTITY_VERIFIED:AuditAction.IDENTITY_REJECTED,tenant,AuditTrail.Resource.IDENTITY_REVIEW,row.id);
        return view(row,student);
    }

    public byte[] ownFile(UUID student,UUID id,String part) {
        account(student,true);var row=submissions.findByIdAndStudentId(id,student).orElseThrow(IdentityReviewService::missing);
        return load(row,part);
    }
    public byte[] institutionFile(UserPrincipal p,UUID id,String part) {
        var row=authorizedRow(p,id);
        byte[] bytes=load(row,part);
        audit.record(AuditAction.IDENTITY_DOCUMENT_VIEWED,row.tenantId,AuditTrail.Resource.IDENTITY_REVIEW,row.id);
        return bytes;
    }

    private byte[] load(IdentitySubmission row,String part) {
        String key,hash;
        switch(part){case "FRONT"->{key=row.frontKey;hash=row.frontHash;}case "BACK"->{key=row.backKey;hash=row.backHash;}case "PHOTO"->{key=row.photoKey;hash=row.photoHash;}default->throw missing();}
        if(key==null)throw missing();
        byte[] bytes;try{bytes=storage.load(key);}catch(RuntimeException failed){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);}
        if(!IdentityImages.hash(bytes).equals(hash))throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        return bytes;
    }
    private String store(String key,byte[] bytes) {
        try{storage.store(key,bytes,"image/jpeg");return key;}
        catch(RuntimeException failed){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);}
    }
    private UUID tenant(UserPrincipal p) {if(p.tenantId()==null||!p.permissions().contains("IDENTITY_REVIEW"))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return p.tenantId();}
    private StudentAccount account(UUID id,boolean lock) {return (lock?accounts.findLockedById(id):accounts.findById(id)).filter(StudentAccount::isActive).orElseThrow(IdentityReviewService::missing);}
    private UUID linked(UUID student,UUID tenant,UUID required) {
        var links=consents.lockActive(student,tenant);
        if(!tenants.findById(tenant).map(this::active).orElse(false))throw missing();
        return links.stream().filter(c->required==null||c.getId().equals(required)).map(c->c.getId()).findFirst().orElseThrow(IdentityReviewService::missing);
    }
    private IdentitySubmission authorizedRow(UserPrincipal p,UUID id) {
        UUID tenant=tenant(p);
        UUID student=submissions.owner(id,tenant).orElseThrow(IdentityReviewService::missing);
        account(student,true);
        // All mutations take the student lock first; read the row only afterwards.
        var row=submissions.locked(id,tenant).orElseThrow(IdentityReviewService::missing);
        linked(student,tenant,row.consentId);
        if(row.withdrawnAt!=null)throw missing();return row;
    }
    private boolean active(Tenant t) {return t.isActive()&&governance.findById(t.getId()).map(g->g.getStatus()==OrganizationStatus.ACTIVE).orElse(true);}
    private String fingerprint(StudentAccount account){return IdentityImages.hash((encode(IdentityReviewDtos.Snapshot.from(account))+":"+account.getIdentityRevision()).getBytes(StandardCharsets.UTF_8));}
    private boolean current(IdentitySubmission row,StudentAccount account) {return row.profileHash.equals(fingerprint(account));}
    private String encode(Object value) {try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("No se pudo preparar la revisión.");}}
    private IdentityReviewDtos.Item view(IdentitySubmission row,StudentAccount student) {
        var tenant=tenants.findById(row.tenantId).orElseThrow(IdentityReviewService::missing);
        boolean changed=!current(row,student);
        boolean sharing=row.withdrawnAt==null&&active(tenant)&&consents.findById(row.consentId).map(c->c.getUsedAt()!=null&&c.getRevokedAt()==null).orElse(false);
        IdentityReviewDtos.Snapshot profile;
        try{profile=json.readValue(row.profileJson,IdentityReviewDtos.Snapshot.class);}catch(JsonProcessingException e){throw new IllegalStateException("No se pudo leer la revisión.");}
        return new IdentityReviewDtos.Item(row.id,row.studentId,row.tenantId,tenant.getName(),row.status.name(),changed||!sharing?"UNVERIFIED":row.status.name(),profile,changed,sharing,row.photoKey!=null,row.submittedAt,row.reviewedAt,row.withdrawnAt,row.reason);
    }
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND);}
}
