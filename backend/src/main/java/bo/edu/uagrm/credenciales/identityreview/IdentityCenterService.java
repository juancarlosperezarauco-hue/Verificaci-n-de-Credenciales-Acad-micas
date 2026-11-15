package bo.edu.uagrm.credenciales.identityreview;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.identity.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.shared.*;
import bo.edu.uagrm.credenciales.storage.ManagedStorage;
import bo.edu.uagrm.credenciales.credential.CredentialDtos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
@Service @Transactional
public class IdentityCenterService {
 public record Claim(@NotBlank @Pattern(regexp="[A-Za-z]{2}") String country,@NotBlank @Pattern(regexp="CI|DNI|PASAPORTE|OTRO") String documentType,@NotBlank @Size(max=80) String documentNumber,@NotBlank @Size(min=20,max=2000) String reason){}
 public record Resolution(@NotBlank @Pattern(regexp="OBSERVE|REJECT|CORRECT_PROFILE") String action,@NotBlank @Size(min=10,max=2000) String reason,@Valid IdentityDtos.UpdateProfile profile){}
 public record Event(String action,String status,String reason,Instant occurredAt){}
 public record Case(UUID id,UUID studentId,String country,String documentType,String documentNumber,String reason,IdentityDispute.Status status,String resolution,boolean documentsAvailable,Instant createdAt,Instant resolvedAt,List<Event> events){}
 private final IdentityDisputeRepository disputes;private final IdentityDisputeEventRepository history;private final TenantAccess access;private final IdentityService identities;private final ManagedStorage storage;private final IdentityImages images;private final SessionRegistry sessions;private final AuditTrail audit;private final Clock clock;
 public IdentityCenterService(IdentityDisputeRepository d,IdentityDisputeEventRepository h,TenantAccess a,IdentityService i,ManagedStorage s,IdentityImages im,SessionRegistry se,AuditTrail au,Clock c){disputes=d;history=h;access=a;identities=i;storage=s;images=im;sessions=se;audit=au;clock=c;}
 public Map<String,Boolean> avatarState(UUID student){return Map.of("present",access.own(student,false).getAvatarKey()!=null);}
 public void avatar(UUID student,MultipartFile file){var a=access.own(student,true);byte[] normalized=images.normalize(file);try{var source=ImageIO.read(new ByteArrayInputStream(normalized));double scale=Math.min(1d,512d/Math.max(source.getWidth(),source.getHeight()));var small=new BufferedImage(Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),BufferedImage.TYPE_INT_RGB);var g=small.createGraphics();g.drawImage(source,0,0,small.getWidth(),small.getHeight(),null);g.dispose();source.flush();var out=new ByteArrayOutputStream();ImageIO.write(small,"jpeg",out);small.flush();String old=a.getAvatarKey();a.avatar(storage.store(null,student,"AVATAR",out.toByteArray(),"image/jpeg"));storage.requestDeletion(old);audit.record(AuditAction.AVATAR_UPDATED,null,AuditTrail.Resource.ACCOUNT,student);}catch(IOException e){throw new IllegalArgumentException("No se pudo preparar la fotografía.");}}
 public void removeAvatar(UUID student){var a=access.own(student,true);storage.requestDeletion(a.getAvatarKey());a.avatar(null);audit.record(AuditAction.AVATAR_UPDATED,null,AuditTrail.Resource.ACCOUNT,student);}
 public CredentialDtos.StoredDocument avatar(UUID student){var a=access.own(student,false);if(a.getAvatarKey()==null)throw TenantAccess.missing();String type=("image/jpeg".equals(a.getAvatarContentType())||"image/png".equals(a.getAvatarContentType()))?a.getAvatarContentType():"application/octet-stream";return new CredentialDtos.StoredDocument(storage.load(a.getAvatarKey()),type,"foto-perfil.jpg");}
 public Case create(UUID student,Claim claim,MultipartFile front,MultipartFile back){var a=access.own(student,true);if(!a.isEmailVerified())throw new IllegalArgumentException("Confirma tu correo antes de abrir una disputa de identidad.");if(disputes.findAllByStudentIdOrderByCreatedAtDesc(student).stream().anyMatch(r->r.status==IdentityDispute.Status.OPEN||r.status==IdentityDispute.Status.OBSERVED))throw TenantAccess.conflict();byte[] f=images.normalize(front),b=images.normalize(back);var row=new IdentityDispute();row.id=UUID.randomUUID();row.studentId=student;row.country=claim.country().toUpperCase(Locale.ROOT);row.documentType=claim.documentType();row.documentNumber=normalize(claim.documentNumber());row.reason=claim.reason().strip();row.status=IdentityDispute.Status.OPEN;row.createdAt=clock.instant();row.frontKey=storage.store(null,row.id,"IDENTITY_DISPUTE",f,"image/jpeg");row.backKey=storage.store(null,row.id,"IDENTITY_DISPUTE",b,"image/jpeg");row.frontHash=IdentityImages.hash(f);row.backHash=IdentityImages.hash(b);disputes.saveAndFlush(row);event(row,student,"OPEN",row.reason);audit.record(AuditAction.IDENTITY_DISPUTE_CREATED,null,AuditTrail.Resource.IDENTITY_DISPUTE,row.id);return view(row);}
 @Transactional(readOnly=true) public List<Case> own(UUID student){access.own(student,false);return disputes.findAllByStudentIdOrderByCreatedAtDesc(student).stream().map(this::view).toList();}
 @Transactional(readOnly=true) public ResultPage<Case> inbox(IdentityDispute.Status status,int page,int size){ResultPage.validate(page,size);var rows=disputes.inbox(status,PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt")));return new ResultPage<>(rows.stream().map(this::view).toList(),rows.getTotalElements(),page,size);}
 public Case withdraw(UUID student,UUID id){var row=owned(student,id);open(row);row.status=IdentityDispute.Status.WITHDRAWN;row.resolvedAt=clock.instant();row.retiredAt=row.resolvedAt;event(row,student,"WITHDRAW",null);return view(row);}
 public Case resubmit(UUID student,UUID id,String explanation,MultipartFile front,MultipartFile back){var row=owned(student,id);if(row.status!=IdentityDispute.Status.OBSERVED)throw TenantAccess.conflict();if(explanation==null||explanation.strip().length()<10||explanation.length()>2000)throw new IllegalArgumentException("Explica la corrección (10–2000 caracteres).");byte[] f=images.normalize(front),b=images.normalize(back);storage.requestDeletion(row.frontKey);storage.requestDeletion(row.backKey);row.frontKey=storage.store(null,row.id,"IDENTITY_DISPUTE",f,"image/jpeg");row.backKey=storage.store(null,row.id,"IDENTITY_DISPUTE",b,"image/jpeg");row.frontHash=IdentityImages.hash(f);row.backHash=IdentityImages.hash(b);row.status=IdentityDispute.Status.OPEN;event(row,student,"RESUBMIT",explanation);return view(row);}
 public Case resolve(UserPrincipal p,UUID id,Resolution input){root(p);UUID student=disputes.owner(id).orElseThrow(TenantAccess::missing);var account=access.own(student,true);var row=disputes.locked(id).orElseThrow(TenantAccess::missing);open(row);
  switch(input.action()){
   case "OBSERVE"->{if(row.status!=IdentityDispute.Status.OPEN)throw TenantAccess.conflict();row.status=IdentityDispute.Status.OBSERVED;}
   case "REJECT"->row.status=IdentityDispute.Status.REJECTED;
   case "CORRECT_PROFILE"->{if(row.status!=IdentityDispute.Status.OPEN||row.frontKey==null||row.backKey==null||input.profile()==null)throw new IllegalArgumentException("Revisa la evidencia y completa el perfil corregido.");var profile=input.profile();if(!profile.country().equalsIgnoreCase(row.country)||!profile.documentType().equals(row.documentType)||!normalize(profile.documentNumber()).equals(row.documentNumber))throw new IllegalArgumentException("El perfil corregido debe corresponder al documento reclamado.");identities.updateProfile(student,profile);account.invalidateSessions();sessions.revokeStudent(student);row.status=IdentityDispute.Status.RESOLVED;}
   default->throw new IllegalArgumentException("Resolución no permitida.");
  }
  row.resolution=input.reason().strip();row.resolverId=p.userId();if(row.status!=IdentityDispute.Status.OBSERVED){row.resolvedAt=clock.instant();row.retiredAt=row.resolvedAt;}event(row,p.userId(),input.action(),row.resolution);audit.record(AuditAction.IDENTITY_DISPUTE_RESOLVED,null,AuditTrail.Resource.IDENTITY_DISPUTE,id);return view(row);
 }
 public CredentialDtos.StoredDocument ownFile(UUID student,UUID id,String part){return file(owned(student,id),part);}
 public CredentialDtos.StoredDocument rootFile(UserPrincipal p,UUID id,String part){root(p);UUID student=disputes.owner(id).orElseThrow(TenantAccess::missing);access.own(student,true);var row=disputes.locked(id).orElseThrow(TenantAccess::missing);audit.record(AuditAction.IDENTITY_DOCUMENT_VIEWED,null,AuditTrail.Resource.IDENTITY_DISPUTE,id);return file(row,part);}
 private CredentialDtos.StoredDocument file(IdentityDispute row,String part){String key,hash;if(part.equals("FRONT")){key=row.frontKey;hash=row.frontHash;}else if(part.equals("BACK")){key=row.backKey;hash=row.backHash;}else throw TenantAccess.missing();if(key==null)throw new IllegalArgumentException("La evidencia fue retirada según la política de conservación.");byte[] bytes=storage.load(key);if(!IdentityImages.hash(bytes).equals(hash))throw new IllegalStateException("La evidencia no coincide con su huella.");return new CredentialDtos.StoredDocument(bytes,"image/jpeg","evidencia-identidad.jpg");}
 private IdentityDispute owned(UUID student,UUID id){access.own(student,true);return disputes.locked(id).filter(r->r.studentId.equals(student)).orElseThrow(TenantAccess::missing);}
 private void root(UserPrincipal p){if(p.role()!=bo.edu.uagrm.credenciales.user.UserRole.SUPER_ADMIN||p.tenantId()!=null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN);}
 private void open(IdentityDispute row){if(row.status!=IdentityDispute.Status.OPEN&&row.status!=IdentityDispute.Status.OBSERVED)throw TenantAccess.conflict();}
 private String normalize(String value){return value.strip().toUpperCase(Locale.ROOT).replaceAll("\\s+","");}
 private void event(IdentityDispute row,UUID actor,String action,String reason){var e=new IdentityDisputeEvent();e.id=UUID.randomUUID();e.disputeId=row.id;e.actorId=actor;e.action=action;e.status=row.status.name();e.reason=reason;e.occurredAt=clock.instant();history.save(e);}
 private Case view(IdentityDispute r){return new Case(r.id,r.studentId,r.country,r.documentType,r.documentNumber,r.reason,r.status,r.resolution,r.frontKey!=null&&r.backKey!=null,r.createdAt,r.resolvedAt,history.findAllByDisputeIdOrderByOccurredAtAsc(r.id).stream().map(e->new Event(e.action,e.status,e.reason,e.occurredAt)).toList());}
}
