package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class LegacyOrganizationIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
 JsonNode legacy()throws Exception{return json.readTree(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("organizationName","Academia Histórica","organizationSlug","old-"+UUID.randomUUID(),"institutionType","ACADEMIA","email","admin@example.test","password","LegacyPassword123!")))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
 Map<String,Object> details(){return Map.ofEntries(Map.entry("legalName","Academia Histórica"),Map.entry("institutionType","ACADEMIA"),Map.entry("educationLevels",List.of()),Map.entry("country","BO"),Map.entry("region","La Paz"),Map.entry("address","Av. Prueba 100"),Map.entry("phone","70000000"),Map.entry("contactEmail","contact@example.test"),Map.entry("representative",Map.of("fullName","Directora de prueba","documentType","CI","documentNumber",UUID.randomUUID().toString(),"position","Directora","email","admin@example.test","phone","70000001")));}
 @Test void missingLegacyProfileReturnsJson404WithoutCreatingOrApprovingAnything()throws Exception{
  var a=legacy();String token=a.get("token").asText();UUID id=UUID.fromString(a.get("tenantId").asText());mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token)).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.message").exists());
  assertEquals(0,jdbc.queryForObject("select count(*) from organization_profiles where tenant_id=?",Integer.class,id));assertEquals("PENDING_REVIEW",jdbc.queryForObject("select status from organization_governance where tenant_id=?",String.class,id));
 }
 @Test void savingLegacyProfileKeepsItsTenantAccountAndTheReviewGate()throws Exception{
  var a=legacy();var other=legacy();String token=a.get("token").asText();UUID id=UUID.fromString(a.get("tenantId").asText());UUID user=jdbc.queryForObject("select id from app_users where tenant_id=?",UUID.class,id);
  for(int i=0;i<2;i++)mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(details()))).andExpect(status().isOk());
  mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.legalName").value("Academia Histórica"));mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+other.get("token").asText())).andExpect(status().isNotFound());mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  assertEquals(user,jdbc.queryForObject("select id from app_users where tenant_id=?",UUID.class,id));assertEquals(1,jdbc.queryForObject("select count(*) from organization_profiles where tenant_id=?",Integer.class,id));assertEquals("PENDING_REVIEW",jdbc.queryForObject("select status from organization_governance where tenant_id=?",String.class,id));
 }
}
