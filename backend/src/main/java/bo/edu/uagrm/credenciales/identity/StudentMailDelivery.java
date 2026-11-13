package bo.edu.uagrm.credenciales.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StudentMailDelivery {
    private static final Logger log = LoggerFactory.getLogger(StudentMailDelivery.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;

    public record Message(String to, String subject, String body) {
        @Override public String toString() { return "StudentMailMessage[redacted]"; }
    }

    public StudentMailDelivery(ObjectProvider<JavaMailSender> senders,
                               @Value("${app.mail-from:no-reply@credenciales.local}") String from) {
        this.senders = senders;
        this.from = from;
    }

    @Async("studentMailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void deliver(Message event) {
        try {
            JavaMailSender sender = senders.getIfAvailable();
            if (sender == null) return;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(event.to());
            message.setSubject(event.subject());
            message.setText(event.body());
            sender.send(message);
        } catch (RuntimeException ignored) {
            // Do not disclose destination, token, body or provider exception details.
            log.warn("No se pudo entregar un correo de seguridad; el usuario puede solicitar otro enlace.");
        }
    }
}
