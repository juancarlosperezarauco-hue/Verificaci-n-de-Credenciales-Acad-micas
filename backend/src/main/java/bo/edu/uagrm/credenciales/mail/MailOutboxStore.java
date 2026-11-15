package bo.edu.uagrm.credenciales.mail;

import bo.edu.uagrm.credenciales.identity.StudentAccountTokenRepository;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class MailOutboxStore {
 private final EntityManager em;private final MailCipher cipher;private final StudentAccountTokenRepository tokens;private final Clock clock;
 private final int maxAttempts;private final int retrySeconds;private final int leaseSeconds;
 public MailOutboxStore(EntityManager em,MailCipher cipher,StudentAccountTokenRepository tokens,Clock clock,
  @Value("${app.mail-outbox.max-attempts:5}") int max,@Value("${app.mail-outbox.retry-seconds:30}") int retry,@Value("${app.mail-outbox.lease-seconds:120}") int lease){
  if(max<1||max>10||retry<1||lease<30)throw new IllegalArgumentException("Configuración de correo inválida");
  this.em=em;this.cipher=cipher;this.tokens=tokens;this.clock=clock;maxAttempts=max;retrySeconds=retry;leaseSeconds=lease;
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void enqueue(UUID student,UUID token,String kind,Instant expires,String to,String subject,String body){
  var row=new MailOutbox();row.id=UUID.randomUUID();row.studentId=student;row.tokenId=token;row.kind=kind;row.state=MailState.PENDING;
  row.createdAt=clock.instant();row.nextAttemptAt=row.createdAt;row.expiresAt=expires;row.encryptedPayload=cipher.encrypt(row.id,new MailCipher.Payload(to,subject,body));em.persist(row);
 }
 @Transactional(readOnly=true)
 public List<UUID> due(){
  return em.createQuery("select m.id from MailOutbox m where (m.state in (:pending,:retry) and m.nextAttemptAt<=:now) or (m.state=:processing and m.leaseUntil<=:now) order by m.nextAttemptAt,m.id",UUID.class)
   .setParameter("pending",MailState.PENDING).setParameter("retry",MailState.RETRY).setParameter("processing",MailState.PROCESSING).setParameter("now",clock.instant()).setMaxResults(5).getResultList();
 }
 public record Delivery(UUID id,UUID claim,UUID tokenId,UUID studentId,String encryptedPayload,int attempts,Instant expiresAt){@Override public String toString(){return "MailDelivery[redacted]";}}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public Optional<Delivery> claim(UUID id){
  Instant now=clock.instant();UUID claim=UUID.randomUUID();
  int rows=em.createQuery("update MailOutbox m set m.state=:processing,m.claimId=:claim,m.leaseUntil=:lease,m.attempts=m.attempts+1 where m.id=:id and ((m.state in (:pending,:retry) and m.nextAttemptAt<=:now) or (m.state=:processing and m.leaseUntil<=:now))")
   .setParameter("processing",MailState.PROCESSING).setParameter("claim",claim).setParameter("lease",now.plusSeconds(leaseSeconds)).setParameter("id",id).setParameter("pending",MailState.PENDING).setParameter("retry",MailState.RETRY).setParameter("now",now).executeUpdate();
  if(rows==0)return Optional.empty();em.clear();var row=em.find(MailOutbox.class,id);
  if(row.attempts>maxAttempts){row.attempts=maxAttempts;terminal(row,MailState.FAILED,"DELIVERY_INTERRUPTED");return Optional.empty();}
  return Optional.of(new Delivery(row.id,claim,row.tokenId,row.studentId,row.encryptedPayload,row.attempts,row.expiresAt));
 }
 @Transactional(readOnly=true)
 public boolean valid(Delivery row){
  return row.expiresAt.isAfter(clock.instant())&&tokens.findById(row.tokenId).filter(t->t.getStudent().getId().equals(row.studentId)&&t.getStudent().isActive()&&t.getUsedAt()==null&&t.getExpiresAt().isAfter(clock.instant())).isPresent();
 }
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void finish(Delivery delivery,MailState state,String error){
  var rows=em.createQuery("select m from MailOutbox m where m.id=:id and m.claimId=:claim and m.state=:processing",MailOutbox.class).setParameter("id",delivery.id).setParameter("claim",delivery.claim).setParameter("processing",MailState.PROCESSING).setLockMode(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE).getResultList();
  if(rows.isEmpty())return;var row=rows.getFirst();
  if(state==MailState.RETRY&&row.attempts<maxAttempts){row.state=MailState.RETRY;row.nextAttemptAt=clock.instant().plusSeconds(Math.min(900L,retrySeconds*(1L<<Math.min(10,row.attempts-1))));row.lastError=error;row.claimId=null;row.leaseUntil=null;}
  else{terminal(row,state==MailState.RETRY?MailState.FAILED:state,error);}
 }
 private void terminal(MailOutbox row,MailState state,String error){row.state=state;row.lastError=error;row.encryptedPayload=null;row.claimId=null;row.leaseUntil=null;if(state==MailState.SENT)row.sentAt=clock.instant();}
}
