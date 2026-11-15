package bo.edu.uagrm.credenciales.audit;

import bo.edu.uagrm.credenciales.security.UserPrincipal;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.context.request.*;

@Service @Transactional(propagation=Propagation.MANDATORY)
public class AuditTrail {
 public enum Resource {ORGANIZATION,ROLE,USER,CREDENTIAL,ACCOUNT,MFA,SESSION,MAIL,SECURITY,IDENTITY_REVIEW}
 private final EntityManager em;private final Clock clock;
 public AuditTrail(EntityManager em,Clock clock){this.em=em;this.clock=clock;}
 public void record(AuditAction action,UUID tenant,Resource type,UUID resource){record(action,tenant,type,resource,null);}
 public void record(AuditAction action,UUID tenant,Resource type,UUID resource,Change change){
  var row=new AuditEvent();row.id=UUID.randomUUID();row.occurredAt=clock.instant();row.tenantId=tenant;
  var authentication=SecurityContextHolder.getContext().getAuthentication();
  if(authentication!=null&&authentication.getPrincipal() instanceof UserPrincipal actor){
   row.actorId=actor.userId();row.actorType=actor.role()==bo.edu.uagrm.credenciales.user.UserRole.STUDENT?"STUDENT":"USER";row.actorRole=actor.role().name();
  }else{row.actorType="SYSTEM";row.actorRole="SYSTEM";}
  row.action=action;row.resourceType=type.name();row.resourceId=resource;row.correlationId=UUID.randomUUID();
  if(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs&&attrs.getRequest().getAttribute(RequestCorrelationFilter.ATTRIBUTE) instanceof UUID id)row.correlationId=id;
  row.changes=change==null?null:change.value;em.persist(row);
 }
 /** Only enums and role UUIDs enter the stored change; no free-text payloads. */
 public static final class Change {
  private final String value;private Change(String value){this.value=value;}
  public static Change permissions(Set<bo.edu.uagrm.credenciales.rbac.PermissionCode> before,Set<bo.edu.uagrm.credenciales.rbac.PermissionCode> after){return new Change("permissions: "+sorted(before)+" â†’ "+sorted(after));}
  public static Change roles(Set<UUID> before,Set<UUID> after){return new Change("roles: "+sorted(before)+" â†’ "+sorted(after));}
  public static Change status(bo.edu.uagrm.credenciales.platform.OrganizationStatus before,bo.edu.uagrm.credenciales.platform.OrganizationStatus after){return new Change("status: "+before+" â†’ "+after);}
  public static Change keyIds(String before,String after){bo.edu.uagrm.credenciales.security.KeySet.validateId(before);bo.edu.uagrm.credenciales.security.KeySet.validateId(after);return new Change("keyId: "+before+" → "+after);}
  public static Change purged(int count){return new Change("securityRecordsRemoved: "+Math.max(0,count));}
  private static String sorted(Set<?> values){return values.stream().map(Object::toString).sorted().toList().toString();}
 }
}
