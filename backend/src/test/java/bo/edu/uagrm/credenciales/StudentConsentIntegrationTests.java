package bo.edu.uagrm.credenciales;

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
class StudentConsentIntegrationTests {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired JdbcTemplate jdbc;
 @Autowired org.springframework.context.ApplicationContext context;
 String postJson(String path,String token,Object body,int status) throws Exception {
  return mvc.perform(post(path).header("Authorization",token==null?"":"Bearer "+token)
    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
    .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
 }
 JsonNode node(String s)throws Exception{return json.readTree(s);}
 String institution(String slug)throws Exception{
  var session=node(postJson("/api/auth/register",null,Map.of("organizationName",slug,"organizationSlug",slug,
    "institutionType","COLEGIO","email","admin@example.com","password","StrongPassword123!"),201));
  ApprovedOrganizationFixture.approve(session,context);return session.get("token").asText();
 }
 JsonNode grant(String token,String slug)throws Exception{
  return node(postJson("/api/me/consents",token,Map.of("institutionSlug",slug),200));
 }

 @Test void studentRegistersWithCredentialsThenCompletesProfileInSeparateSteps() throws Exception {
  String slug="onboarding-"+UUID.randomUUID(), institution=institution(slug);
  String email=UUID.randomUUID()+"@example.com";
  String token=node(postJson("/api/auth/student/register",null,
      Map.of("email",email,"password","StrongPassword123!"),201)).get("token").asText();
  String initial=mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+token))
      .andExpect(status().isOk()).andExpect(jsonPath("$.profileComplete").value(false))
      .andExpect(jsonPath("$.fullName").doesNotExist()).andReturn().getResponse().getContentAsString();
  String searchCode=node(initial).get("searchCode").asText();
  postJson("/api/students/redeem",institution,Map.of("code",searchCode),400);
  postJson("/api/me/consents",token,Map.of("institutionSlug",slug),400);
  mvc.perform(patch("/api/me/profile").header("Authorization","Bearer "+token)
      .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
          "fullName","Perfil Completo","country","BO","documentType","CI",
          "documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2001-05-12",
          "department","Santa Cruz"))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.profileComplete").value(true))
      .andExpect(jsonPath("$.fullName").value("Perfil Completo"));
  postJson("/api/students/redeem",institution,Map.of("code",searchCode),200);
 }

 @Test void permanentSearchCodeRegistersAcrossInstitutionsWithoutDuplicates() throws Exception {
  String slugA="search-a-"+UUID.randomUUID(),slugB="search-b-"+UUID.randomUUID();
  String a=institution(slugA),b=institution(slugB);
  String student=node(postJson("/api/auth/student/register",null,Map.of(
      "fullName","Estudiante Código","country","BO","documentType","CI",
      "documentNumber",UUID.randomUUID().toString(),"email",UUID.randomUUID()+"@example.com",
      "password","StrongPassword123!","dateOfBirth","2000-08-21","department","La Paz"),201)).get("token").asText();
  String own=mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+student))
      .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String code=node(own).get("searchCode").asText(), id=node(own).get("id").asText();
  org.junit.jupiter.api.Assertions.assertTrue(code.matches("EST-[A-F0-9]{32}"));
  var payload=Map.of("code",code);
  postJson("/api/students/redeem",student,payload,403);
  JsonNode first=node(postJson("/api/students/redeem",a,payload,200));
  org.junit.jupiter.api.Assertions.assertEquals(id,first.get("id").asText());
  org.junit.jupiter.api.Assertions.assertFalse(first.has("searchCode"));
  org.junit.jupiter.api.Assertions.assertFalse(first.has("email"));
  postJson("/api/students/redeem",a,payload,200);
  postJson("/api/students/redeem",b,Map.of("code",code.toLowerCase(Locale.ROOT)),200);
  mvc.perform(get("/api/students").header("Authorization","Bearer "+a))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
  mvc.perform(get("/api/students").header("Authorization","Bearer "+b))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
  String history=mvc.perform(get("/api/me/consents").header("Authorization","Bearer "+student))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
      .andReturn().getResponse().getContentAsString();
  String grantId="";
  for(JsonNode grant:node(history)) {
      if(grant.get("institutionSlug").asText().equals(slugA)) grantId=grant.get("id").asText();
  }
  mvc.perform(delete("/api/me/consents/"+grantId).header("Authorization","Bearer "+student))
      .andExpect(status().isOk());
  postJson("/api/students/redeem",a,payload,400);
  postJson("/api/me/search-code/rotate",a,Map.of(),403);
  JsonNode rotated=node(postJson("/api/me/search-code/rotate",student,Map.of(),200));
  org.junit.jupiter.api.Assertions.assertNotEquals(code,rotated.get("searchCode").asText());
  org.junit.jupiter.api.Assertions.assertEquals(id,rotated.get("id").asText());
  postJson("/api/students/redeem",b,payload,400);
  mvc.perform(get("/api/students").header("Authorization","Bearer "+b))
      .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
  postJson("/api/students/redeem",a,Map.of("code",rotated.get("searchCode").asText()),200);
  mvc.perform(get("/api/students").header("Authorization","Bearer "+a))
      .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
 }
 @Test void identityConsentAndIsolation()throws Exception {
  String slugA="school-"+UUID.randomUUID(),slugB="university-"+UUID.randomUUID();
  String a=institution(slugA),b=institution(slugB);
  String email=UUID.randomUUID()+"@example.com",document=UUID.randomUUID().toString();
  var registration=Map.of("fullName","Ana Prueba","country","BO","documentType","CI",
    "documentNumber",document,"email",email,"password","StrongPassword123!",
    "dateOfBirth","1999-02-15","department","Cochabamba");
  String student=node(postJson("/api/auth/student/register",null,registration,201)).get("token").asText();
  postJson("/api/auth/student/register",null,registration,400);
  var duplicate=new HashMap<String,String>(registration);duplicate.put("email","other-"+email);
  postJson("/api/auth/student/register",null,duplicate,400);
  postJson("/api/auth/student/login",null,Map.of("email",email,"password","incorrect"),401);
  postJson("/api/auth/student/login",null,Map.of("email",email,"password","StrongPassword123!"),200);
  String profile=mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+student))
    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String id=node(profile).get("id").asText();
  mvc.perform(get("/api/students").header("Authorization","Bearer "+student)).andExpect(status().isForbidden());
  mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+a)).andExpect(status().isForbidden());
  var credential=Map.of("studentId",id,"credentialType","CERTIFICADO","title","Participación");
  postJson("/api/credentials",a,credential,400);
  postJson("/api/students",a,Map.of("fullName","Legacy"),405);
  JsonNode g=grant(student,slugA);
  var code=Map.of("code",g.get("code").asText());
  postJson("/api/students/redeem",b,code,400);
  JsonNode viewed=node(postJson("/api/students/redeem",a,code,200));
  org.junit.jupiter.api.Assertions.assertEquals(Set.of("id","fullName","country","documentType","documentNumber",
      "dateOfBirth","department"),
    new HashSet<>(viewed.properties().stream().map(Map.Entry::getKey).toList()));
  postJson("/api/students/redeem",a,code,400);
  String draft=node(postJson("/api/credentials",a,credential,201)).get("id").asText();
  mvc.perform(get("/api/students").header("Authorization","Bearer "+b)).andExpect(jsonPath("$.length()").value(0));
  postJson("/api/credentials",b,credential,400);
  JsonNode second=grant(student,slugB);
  postJson("/api/students/redeem",b,Map.of("code",second.get("code").asText()),200);
  mvc.perform(get("/api/students").header("Authorization","Bearer "+b)).andExpect(jsonPath("$[0].id").value(id));
  mvc.perform(get("/api/credentials").header("Authorization","Bearer "+b)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(get("/api/me/credentials").header("Authorization","Bearer "+student)).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(delete("/api/me/consents/"+g.get("access").get("id").asText()).header("Authorization","Bearer "+student)).andExpect(status().isOk());
  postJson("/api/credentials",a,credential,400);
  mvc.perform(patch("/api/credentials/"+draft+"/issue").header("Authorization","Bearer "+a)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/students").header("Authorization","Bearer "+a)).andExpect(jsonPath("$.length()").value(0));
  JsonNode expired=grant(student,slugA);
  jdbc.update("UPDATE student_consents SET expires_at = ? WHERE id = ?",
      java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(1)),UUID.fromString(expired.get("access").get("id").asText()));
  postJson("/api/students/redeem",a,Map.of("code",expired.get("code").asText()),400);
  JsonNode cancelled=grant(student,slugA);
  mvc.perform(delete("/api/me/consents/"+cancelled.get("access").get("id").asText()).header("Authorization","Bearer "+student)).andExpect(status().isOk());
  postJson("/api/students/redeem",a,Map.of("code",cancelled.get("code").asText()),400);
 }
}
