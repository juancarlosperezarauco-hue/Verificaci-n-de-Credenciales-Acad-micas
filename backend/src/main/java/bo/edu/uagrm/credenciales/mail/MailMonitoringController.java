package bo.edu.uagrm.credenciales.mail;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController @Transactional(readOnly=true)
public class MailMonitoringController {
 private final EntityManager em;
 public MailMonitoringController(EntityManager em){this.em=em;}
 public record Item(UUID id,UUID studentId,String kind,MailState state,int attempts,Instant createdAt,Instant nextAttemptAt,Instant sentAt,String lastError){}
 public record Page(List<Item> items,long total,int page,int size){}
 @GetMapping("/api/platform/mail") @PreAuthorize("hasRole('SUPER_ADMIN')")
 public Page list(@RequestParam(required=false) MailState state,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
  if(page<0||page>100000||size<1||size>100)throw new IllegalArgumentException("Revisa la paginación.");
  String where=state==null?"":" where m.state=:state";
  var count=em.createQuery("select count(m) from MailOutbox m"+where,Long.class);var rows=em.createQuery("select m from MailOutbox m"+where+" order by m.createdAt desc,m.id desc",MailOutbox.class);
  if(state!=null){count.setParameter("state",state);rows.setParameter("state",state);}
  var items=rows.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(m->new Item(m.id,m.studentId,m.kind,m.state,m.attempts,m.createdAt,m.nextAttemptAt,m.sentAt,m.lastError)).toList();
  return new Page(items,count.getSingleResult(),page,size);
 }
}
