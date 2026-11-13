package bo.edu.uagrm.credenciales;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.platform.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.tenant.*;
import bo.edu.uagrm.credenciales.user.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class OrganizationOnboardingIntegrationTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired ApplicationContext context;
    @Autowired AppUserRepository users; @Autowired TenantRepository tenants;
    @Autowired PasswordEncoder passwords; @Autowired JwtService jwt;
    private static final String PASSWORD="OnboardingPassword123!";

    private Map<String,Object> profile() {
        return Map.ofEntries(Map.entry("legalName","Academia Nueva"), Map.entry("institutionType","ACADEMIA"),
                Map.entry("educationLevels", List.of()), Map.entry("country","BO"), Map.entry("region","La Paz"),
                Map.entry("address","Av. Educación 100"), Map.entry("phone","70000000"), Map.entry("contactEmail","contact@example.test"),
                Map.entry("representative",Map.of("fullName","Directora Prueba", "documentType","CI", "documentNumber","123456",
                        "position","Directora", "email","admin@example.test", "phone","70000001")));
    }
    private JsonNode post(String path,String token,Object body,int code) throws Exception {
        String result=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.nullNode():json.readTree(result);
    }
    private JsonNode register(boolean full) throws Exception {
        String slug="onboard-"+UUID.randomUUID();
        return full ? post("/api/auth/organizations/register","",Map.of("organizationSlug",slug,"password",PASSWORD,"profile",profile()),201)
                : post("/api/auth/register","",Map.of("organizationSlug",slug,"organizationName","Academia Nueva",
                        "institutionType","ACADEMIA","email","admin@example.test","password",PASSWORD),201);
    }
    private String root() {
        var user=users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test",passwords.encode(PASSWORD),UserRole.SUPER_ADMIN));
        return jwt.create(UserPrincipal.from(user));
    }
    private void state(String id,String root,String next,int code) throws Exception {
        mvc.perform(patch("/api/platform/organizations/{id}/status",id).header("Authorization","Bearer "+root)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",next,"reason","Revisión: "+next))))
                .andExpect(status().is(code));
    }
    private void login(JsonNode session,int code) throws Exception {
        String slug=tenants.findById(UUID.fromString(session.get("tenantId").asText())).orElseThrow().getSlug();
        post("/api/auth/login","",Map.of("organizationSlug",slug,"email","admin@example.test","password",PASSWORD),code);
    }

    @Test void fullRegistrationIsRestrictedUntilApprovalAndOldTokenGainsOnlyApprovedAccess() throws Exception {
        var session=register(true);String token=session.get("token").asText(),id=session.get("tenantId").asText();
        mvc.perform(get("/api/institution/status").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_REVIEW")).andExpect(jsonPath("$.active").value(false));
        mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.onboarding").value(true)).andExpect(jsonPath("$.permissions.length()").value(2));
        login(session,200);
        mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        for(String path:List.of("/api/credentials","/api/students","/api/portal/items","/api/access/roles","/api/access/users")) {
            mvc.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            post(path,token,Map.of(),403);
        }
        state(id,token,"ACTIVE",403);
        String root=root();state(id,root,"ACTIVE",200);state(id,root,"ACTIVE",400);
        mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.onboarding").value(false));
        mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isOk());
    }

    @Test void legacyRegistrationNeedsProfileAndObservedCorrectionsRemainRestrictedAndAudited() throws Exception {
        var session=register(false);String token=session.get("token").asText(),id=session.get("tenantId").asText(),root=root();
        login(session,200);state(id,root,"ACTIVE",400);
        post("/api/institution/submit-review",token,Map.of(),400);
        mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(profile()))).andExpect(status().isOk());
        state(id,root,"OBSERVED",200);
        mvc.perform(get("/api/institution/status").header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.reviewNote").value("Revisión: OBSERVED"));
        mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(profile()))).andExpect(status().isOk());
        mvc.perform(get("/api/institution/status").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.status").value("OBSERVED"));
        post("/api/institution/submit-review",token,Map.of(),200);post("/api/institution/submit-review",token,Map.of(),200);
        mvc.perform(get("/api/platform/organizations/{id}/history",id).header("Authorization","Bearer "+root))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/portal/items").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        state(id,root,"ACTIVE",200);
        post("/api/institution/submit-review",token,Map.of(),400);
    }

    @Test void observedEmployeesAreBlockedAndSuspensionOrRejectionInvalidatesExistingAdminAccess() throws Exception {
        var session=register(true);String token=session.get("token").asText(),id=session.get("tenantId").asText(),root=root();
        state(id,root,"ACTIVE",200);
        String role=post("/api/access/roles",token,Map.of("name","Lector","permissions",List.of("ORGANIZATION_VIEW")),201).get("id").asText();
        post("/api/access/users",token,Map.of("fullName","Empleado Prueba","email","staff@example.test","password",PASSWORD,"roleIds",List.of(role)),201);
        String slug=tenants.findById(UUID.fromString(id)).orElseThrow().getSlug();
        Map<String,String> credentials=Map.of("organizationSlug",slug,"email","staff@example.test","password",PASSWORD);
        String staff=post("/api/auth/login","",credentials,200).get("token").asText();
        state(id,root,"PENDING_REVIEW",400);state(id,root,"OBSERVED",200);
        login(session,200);post("/api/auth/login","",credentials,401);
        mvc.perform(get("/api/institution/profile").header("Authorization","Bearer "+staff)).andExpect(status().isForbidden());
        state(id,root,"REJECTED",200);login(session,401);
        mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        state(id,root,"PENDING_REVIEW",200);login(session,200);state(id,root,"ACTIVE",200);
        state(id,root,"SUSPENDED",200);login(session,401);state(id,root,"REJECTED",400);
        mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
    }

    @Test void previouslyActiveOrganizationWithoutGovernanceKeepsItsAccess() throws Exception {
        Tenant tenant=tenants.saveAndFlush(new Tenant("Organización histórica","historical-"+UUID.randomUUID(),InstitutionType.COLEGIO));
        var user=users.saveAndFlush(new AppUser(tenant,"admin@example.test",passwords.encode(PASSWORD),UserRole.TENANT_ADMIN));
        String token=post("/api/auth/login","",Map.of("organizationSlug",tenant.getSlug(),"email",user.getEmail(),"password",PASSWORD),200).get("token").asText();
        mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.onboarding").value(false));
    }
}
