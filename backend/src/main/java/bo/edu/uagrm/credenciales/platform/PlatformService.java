package bo.edu.uagrm.credenciales.platform;
import bo.edu.uagrm.credenciales.tenant.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class PlatformService {
 private final TenantRepository tenants; private final GovernanceRepository governance; private final StatusEventRepository events;
 public PlatformService(TenantRepository tenants,GovernanceRepository governance,StatusEventRepository events){this.tenants=tenants;this.governance=governance;this.events=events;}
 public record Organization(UUID id,String name,String slug,InstitutionType institutionType,boolean active,OrganizationStatus status){}
 private Tenant tenant(UUID id){return tenants.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));}
 private Organization response(Tenant t){return new Organization(t.getId(),t.getName(),t.getSlug(),t.getInstitutionType(),t.isActive(),governance.findById(t.getId()).map(OrganizationGovernance::getStatus).orElse(t.isActive()?OrganizationStatus.ACTIVE:OrganizationStatus.INACTIVE));}
 public List<Organization> list(){return tenants.findAll().stream().map(this::response).toList();}
 public Organization get(UUID id){return response(tenant(id));}
 public Organization change(UUID id,UUID actor,OrganizationStatus status,String reason){
  Tenant t=tenant(id);var g=governance.findById(id).orElseGet(()->new OrganizationGovernance(id,t.isActive()));
  var previous=g.getStatus();g.change(status);governance.save(g);t.setActive(status==OrganizationStatus.ACTIVE);
  events.save(new OrganizationStatusEvent(id,actor,previous,status,reason.trim()));return response(t);
 }
 public List<OrganizationStatusEvent> history(UUID id){tenant(id);return events.findByTenantIdOrderByCreatedAtDesc(id);}
}
