package bo.edu.uagrm.credenciales.mail;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MailOutboxWorker {
 private final MailOutboxStore store;private final MailCipher cipher;private final ObjectProvider<JavaMailSender> senders;private final String from;private final boolean enabled;
 public MailOutboxWorker(MailOutboxStore store,MailCipher cipher,ObjectProvider<JavaMailSender> senders,@Value("${app.mail-from:no-reply@credenciales.local}") String from,@Value("${app.mail-outbox.worker-enabled:true}") boolean enabled){this.store=store;this.cipher=cipher;this.senders=senders;this.from=from;this.enabled=enabled;}
 @Scheduled(fixedDelayString="${app.mail-outbox.poll-ms:1000}")
 public void poll(){if(enabled)dispatchDue();}
 public void dispatchDue(){for(var id:store.due())dispatch(id);}
 public void dispatch(java.util.UUID id){store.claim(id).ifPresent(this::send);}
 private void send(MailOutboxStore.Delivery delivery){
  try{
   if(!store.valid(delivery)){store.finish(delivery,MailState.CANCELLED,"TOKEN_UNAVAILABLE");return;}
   MailCipher.Payload payload;
   try{payload=cipher.decrypt(delivery.id(),delivery.encryptedPayload());}catch(IllegalStateException invalid){store.finish(delivery,MailState.FAILED,"PAYLOAD_UNAVAILABLE");return;}
   var sender=senders.getIfAvailable();if(sender==null){store.finish(delivery,MailState.RETRY,"SENDER_UNAVAILABLE");return;}
   var message=new SimpleMailMessage();message.setFrom(from);message.setTo(payload.to());message.setSubject(payload.subject());message.setText(payload.body());sender.send(message);
   store.finish(delivery,MailState.SENT,null);
  }catch(RuntimeException failure){store.finish(delivery,MailState.RETRY,"DELIVERY_FAILED");}
 }
}
