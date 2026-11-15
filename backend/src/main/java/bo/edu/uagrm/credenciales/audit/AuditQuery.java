package bo.edu.uagrm.credenciales.audit;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true)
public class AuditQuery {
 private final EntityManager em;
 public AuditQuery(EntityManager em){this.em=em;}
 public record Item(UUID id,Instant occurredAt,UUID tenantId,UUID actorId,String actorType,String actorRole,AuditAction action,String resourceType,UUID resourceId,UUID correlationId,String changes){}
 public record Page(List<Item> items,long total,int page,int size){}
 public Page search(boolean global,UUID tenant,AuditAction action,UUID actor,UUID resource,Instant from,Instant before,int page,int size){
  if(page<0||page>100000||size<1||size>100||from!=null&&before!=null&&!from.isBefore(before))throw new IllegalArgumentException("Revisa los filtros y la paginación.");
  if(!global&&tenant==null)throw new IllegalArgumentException("Se requiere una organización.");
  StringBuilder where=new StringBuilder(" where 1=1");Map<String,Object> args=new LinkedHashMap<>();
  add(where,args,"tenantId",tenant);add(where,args,"action",action);add(where,args,"actorId",actor);add(where,args,"resourceId",resource);
  if(from!=null){where.append(" and e.occurredAt>=:from");args.put("from",from);}
  if(before!=null){where.append(" and e.occurredAt<:before");args.put("before",before);}
  var count=em.createQuery("select count(e) from AuditEvent e"+where,Long.class);args.forEach(count::setParameter);
  var query=em.createQuery("select e from AuditEvent e"+where+" order by e.occurredAt desc,e.id desc",AuditEvent.class);args.forEach(query::setParameter);
  var items=query.setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(e->new Item(e.id,e.occurredAt,e.tenantId,e.actorId,e.actorType,e.actorRole,e.action,e.resourceType,e.resourceId,e.correlationId,e.changes)).toList();
  return new Page(items,count.getSingleResult(),page,size);
 }
 private void add(StringBuilder where,Map<String,Object> args,String key,Object value){if(value!=null){where.append(" and e.").append(key).append("=:").append(key);args.put(key,value);}}
}
