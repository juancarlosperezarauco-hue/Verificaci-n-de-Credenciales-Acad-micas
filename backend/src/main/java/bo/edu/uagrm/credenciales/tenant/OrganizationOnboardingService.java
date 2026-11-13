package bo.edu.uagrm.credenciales.tenant;
import bo.edu.uagrm.credenciales.auth.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service @Transactional
public class OrganizationOnboardingService {
 private final OrganizationProfileRepository profiles;private final TenantRepository tenants;private final AuthService auth;
 public OrganizationOnboardingService(OrganizationProfileRepository profiles,TenantRepository tenants,AuthService auth){this.profiles=profiles;this.tenants=tenants;this.auth=auth;}
 public AuthDtos.AuthResponse register(OrganizationDtos.Registration r){
 var d=r.profile();var session=auth.register(new AuthDtos.RegisterRequest(d.legalName(),r.organizationSlug(),d.institutionType(),d.representative().email(),r.password()));
 update(UUID.fromString(session.tenantId()),d);return session;
 }
 public OrganizationDtos.Details get(UUID tenant){return profiles.findById(tenant).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND)).details();}
 public OrganizationDtos.Details update(UUID tenant,OrganizationDtos.Details d){
  var t=tenants.findById(tenant).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  var p=profiles.findById(tenant).orElseGet(()->new OrganizationProfile(tenant));p.update(d);
  t.updateIdentification(d.legalName().trim(),d.institutionType());profiles.saveAndFlush(p);return p.details();
 }
}
