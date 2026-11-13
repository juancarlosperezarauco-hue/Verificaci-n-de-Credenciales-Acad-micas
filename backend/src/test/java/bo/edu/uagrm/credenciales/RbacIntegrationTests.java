package bo.edu.uagrm.credenciales;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.user.*;
import bo.edu.uagrm.credenciales.tenant.*;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class RbacIntegrationTests {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AppUserRepository users;
 @Autowired TenantRepository tenants; @Autowired PasswordEncoder encoder;
 @MockitoBean DocumentStoragePort storage;
 private final String password="RbacTestPassword123!";
 private JsonNode postJson(String path,String token,Object body,int code)throws Exception{
  return json.readTree(mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
   .andExpect(status().is(code)).andReturn().getResponse().getContentAsString());
 }
 private JsonNode register()throws Exception{
  return postJson("/api/auth/register","",Map.of("organizationName","Institución RBAC","organizationSlug","rbac-"+UUID.randomUUID(),
   "institutionType","UNIVERSIDAD","email","admin@example.edu","password",password),201);
 }
 @Test void tenantIsolationAndLivePermissions()throws Exception{
  var first=register();var second=register();String a=first.get("token").asText(),b=second.get("token").asText();
  String role=postJson("/api/access/roles",a,Map.of("name","Revisor","permissions",List.of("CREDENTIAL_VIEW")),201).get("id").asText();
  String id=postJson("/api/access/users",a,Map.of("fullName","Docente Uno","email","teacher@example.edu","password",password,"roleIds",List.of(role)),201).get("id").asText();
  String slug=tenants.findById(UUID.fromString(first.get("tenantId").asText())).orElseThrow().getSlug();
  String token=postJson("/api/auth/login","",Map.of("organizationSlug",slug,"email","teacher@example.edu","password",password),200).get("token").asText();
  mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(get("/api/students").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/access/roles").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(put("/api/access/roles/"+role).header("Authorization","Bearer "+b).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("name","Ajeno","permissions",List.of())))).andExpect(status().isNotFound());
  postJson("/api/access/users",b,Map.of("fullName","Ajeno","email","cross@example.edu","password",password,"roleIds",List.of(role)),404);
  mvc.perform(patch("/api/access/users/"+id+"/disable").header("Authorization","Bearer "+b)).andExpect(status().isNotFound());
  mvc.perform(put("/api/access/roles/"+role).header("Authorization","Bearer "+a).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("name","Revisor","permissions",List.of("STUDENT_VIEW"))))).andExpect(status().isOk());
  mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/students").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(delete("/api/access/roles/"+role).header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
  mvc.perform(get("/api/students").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(patch("/api/access/users/"+id+"/disable").header("Authorization","Bearer "+a)).andExpect(status().isNoContent());
  mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
 }
 @Test void superAdminSuspendsAndRestoresWithoutTenantAccess()throws Exception{
  var tenant=register();String token=tenant.get("token").asText(),id=tenant.get("tenantId").asText();
  String email=UUID.randomUUID()+"@platform.example";
  users.save(new AppUser(null,email,encoder.encode(password),UserRole.SUPER_ADMIN));
  String root=postJson("/api/auth/platform/login","",Map.of("email",email,"password",password),200).get("token").asText();
  mvc.perform(get("/api/institution/status").header("Authorization","Bearer "+token))
   .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(get("/api/institution/status").header("Authorization","Bearer "+root)).andExpect(status().isForbidden());
  mvc.perform(get("/api/platform/organizations").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+root)).andExpect(status().isForbidden());
  mvc.perform(get("/api/credentials").header("Authorization","Bearer "+root)).andExpect(status().isForbidden());
  for(String state:List.of("SUSPENDED","ACTIVE")){
   mvc.perform(patch("/api/platform/organizations/"+id+"/status").header("Authorization","Bearer "+root)
    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("status",state,"reason","Revisión institucional"))))
    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state));
   mvc.perform(get("/api/credentials").header("Authorization","Bearer "+token)).andExpect(status().is(state.equals("ACTIVE")?200:403));
  }
  mvc.perform(get("/api/platform/organizations/"+id+"/history").header("Authorization","Bearer "+root))
   .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
 }
 private Map<String,Object> profile(String tax,String license){
  return Map.ofEntries(Map.entry("legalName","Instituto Pruebas"),Map.entry("institutionType","INSTITUTO_TECNICO"),
   Map.entry("educationLevels",List.of("TECNICO")),Map.entry("country","bo"),Map.entry("region","Santa Cruz"),
   Map.entry("address","Av. Principal 100"),Map.entry("phone","70000000"),Map.entry("contactEmail","contacto@example.edu"),
   Map.entry("website","https://instituto.edu.bo"),Map.entry("domain","instituto.edu.bo"),Map.entry("taxId",tax),
   Map.entry("educationLicense",license),Map.entry("representative",Map.of("fullName","María Directora",
    "documentType","CI","documentNumber","123456","position","Directora","email","directora@example.edu","phone","70000001")));
 }
 @Test void completeOrganizationRegistrationAndDuplicates()throws Exception{
  String tax=UUID.randomUUID().toString(),license=UUID.randomUUID().toString();
  var session=postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),
   "password",password,"profile",profile(tax,license)),201);
  String token=session.get("token").asText();
  mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token))
   .andExpect(status().isOk()).andExpect(jsonPath("$.country").value("BO"))
   .andExpect(jsonPath("$.domain").value("instituto.edu.bo")).andExpect(jsonPath("$.representative.fullName").value("María Directora"));
  postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),
   "password",password,"profile",profile(tax.toUpperCase(),UUID.randomUUID().toString())),409);
  postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),
   "password",password,"profile",profile(UUID.randomUUID().toString(),license.toUpperCase())),409);
  mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(profile(tax,license)))).andExpect(status().isOk());
  var invalid=new HashMap<>(profile("X","Y"));invalid.put("website","javascript:alert(1)");
  postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),"password",password,"profile",invalid),400);
 }
 @Test void organizationCanRegisterAndUpdateWithoutEducationLevels()throws Exception{
  var details=new HashMap<>(profile(UUID.randomUUID().toString(),UUID.randomUUID().toString()));
  details.put("educationLevels",List.of());
  var session=postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),
   "password",password,"profile",details),201);
  String token=session.get("token").asText();
  mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token))
   .andExpect(status().isOk()).andExpect(jsonPath("$.educationLevels").isEmpty());
  mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token)
   .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(details)))
   .andExpect(status().isOk()).andExpect(jsonPath("$.educationLevels").isEmpty());
 }
 @Test void allEducationalOrganizationTypesCanRegisterWithoutLegalIdentifiers()throws Exception{
  for(var type:InstitutionType.values()){
   var details=new HashMap<>(profile("unused","unused"));
   details.put("institutionType",type.name());details.put("educationLevels",List.of());
   details.remove("taxId");details.put("educationLicense","   ");
   var session=postJson("/api/auth/organizations/register","",Map.of("organizationSlug","org-"+UUID.randomUUID(),
    "password",password,"profile",details),201);
   String token=session.get("token").asText();
   mvc.perform(get("/api/institution/details").header("Authorization","Bearer "+token))
    .andExpect(status().isOk()).andExpect(jsonPath("$.institutionType").value(type.name()))
    .andExpect(jsonPath("$.taxId").isEmpty()).andExpect(jsonPath("$.educationLicense").isEmpty());
   details.put("taxId","");details.put("educationLicense",null);
   mvc.perform(put("/api/institution/details").header("Authorization","Bearer "+token)
    .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(details)))
    .andExpect(status().isOk()).andExpect(jsonPath("$.taxId").isEmpty())
    .andExpect(jsonPath("$.educationLicense").isEmpty());
  }
 }
 @Test void studentAndUnknownPermissionsCannotEscalate()throws Exception{
  String token=postJson("/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@student.example","password",password),201).get("token").asText();
  mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+token)).andExpect(status().isOk());
  mvc.perform(get("/api/access/users").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/platform/organizations").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/access/me").header("Authorization","Bearer "+token)).andExpect(jsonPath("$.permissions").isEmpty());
  String admin=register().get("token").asText();
  mvc.perform(post("/api/access/roles").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
   .content(json.writeValueAsString(Map.of("name","Elevado","permissions",List.of("SUPER_ADMIN"))))).andExpect(status().isBadRequest());
 }
}
