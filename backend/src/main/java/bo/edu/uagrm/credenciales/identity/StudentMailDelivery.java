package bo.edu.uagrm.credenciales.identity;

import bo.edu.uagrm.credenciales.mail.MailOutboxStore;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

/** Synchronous enqueue joins the token transaction; delivery happens later. */
@Component
public class StudentMailDelivery {
 private final MailOutboxStore outbox;
 public StudentMailDelivery(MailOutboxStore outbox){this.outbox=outbox;}
 public record Message(UUID studentId,UUID tokenId,String kind,Instant expiresAt,String to,String subject,String body){@Override public String toString(){return "StudentMailMessage[redacted]";}}
 @EventListener @Transactional(propagation=Propagation.MANDATORY)
 public void enqueue(Message message){outbox.enqueue(message.studentId(),message.tokenId(),message.kind(),message.expiresAt(),message.to(),message.subject(),message.body());}
}
