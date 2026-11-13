package bo.edu.uagrm.credenciales;

import bo.edu.uagrm.credenciales.platform.*;
import bo.edu.uagrm.credenciales.tenant.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationContext;

/** Prepares approved data for existing operational tests; onboarding is tested independently over HTTP. */
final class ApprovedOrganizationFixture {
    static void approve(JsonNode session, ApplicationContext context) {
        UUID id=UUID.fromString(session.get("tenantId").asText());
        var tenants=context.getBean(TenantRepository.class);
        var tenant=tenants.findById(id).orElseThrow();
        var details=context.getBean(ObjectMapper.class).convertValue(Map.ofEntries(
                Map.entry("legalName",tenant.getName()), Map.entry("institutionType",tenant.getInstitutionType().name()),
                Map.entry("educationLevels", List.of()), Map.entry("country","BO"), Map.entry("region","Santa Cruz"),
                Map.entry("address","Av. Prueba 100"), Map.entry("phone","70000000"),
                Map.entry("contactEmail","contact@example.test"), Map.entry("representative",Map.of(
                        "fullName","Administrador Prueba", "documentType","CI", "documentNumber",id.toString(),
                        "position","Director", "email","admin@example.test", "phone","70000001"))), OrganizationDtos.Details.class);
        context.getBean(OrganizationOnboardingService.class).update(id,details);
        tenant=tenants.findById(id).orElseThrow();
        tenant.setActive(true);
        tenants.saveAndFlush(tenant);
        var governance=context.getBean(GovernanceRepository.class);
        var state=governance.findById(id).orElseThrow();
        state.change(OrganizationStatus.ACTIVE);
        governance.saveAndFlush(state);
    }
}
