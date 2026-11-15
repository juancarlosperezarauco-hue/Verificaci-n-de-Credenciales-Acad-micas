package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.rbac.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuditIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired org.springframework.context.ApplicationContext context;
 @Autowired AccessManagementService access;@Autowired PlatformTransactionManager transactions;@Autowired AppUserRepository users;@Autowired JwtService jwt;
 @MockitoBean DocumentStoragePort storage;@MockitoBean BlockchainAnchorPort blockchain;
 final String password="AuditPassword123!";
 record Tenant(String token,UUID id){}
 JsonNode postJson(String path,String token,Object body,int code)throws Exception{return json.readTree(mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse().getContentAsString());}
 Tenant tenant()throws Exception{var r=postJson("/api/auth/register","",Map.of("organizationName","Auditoría","organizationSlug","audit-"+UUID.randomUUID(),"institutionType","ACADEMIA","email","admin@example.test","password",password),201);ApprovedOrganizationFixture.approve(r,context);return new Tenant(r.get("token").asText(),UUID.fromString(r.get("tenantId").asText()));}
 String root(){var u=users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test","test-only",bo.edu.uagrm.credenciales.user.UserRole.SUPER_ADMIN));return jwt.create(UserPrincipal.from(u));}
 JsonNode events(String token,String path,UUID resource)throws Exception{return json.readTree(mvc.perform(get(path).param("resourceId",resource.toString()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void roleChangesKeepActorCorrelationAndPermissionDiffWithoutSensitiveFields()throws Exception{
  var a=tenant();var response=mvc.perform(post("/api/access/roles").header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Private role name","permissions",List.of("AUDIT_VIEW"))))).andExpect(status().isCreated()).andExpect(header().exists("X-Request-ID")).andReturn().getResponse();
  UUID id=UUID.fromString(json.readTree(response.getContentAsString()).get("id").asText());var row=events(a.token,"/api/audit/tenant",id).get("items").get(0);
  assertEquals("ROLE_CREATED",row.get("action").asText());assertEquals("TENANT_ADMIN",row.get("actorRole").asText());assertEquals(response.getHeader("X-Request-ID"),row.get("correlationId").asText());assertTrue(row.get("changes").asText().contains("AUDIT_VIEW"));assertFalse(row.toString().contains(password));assertFalse(row.toString().contains("admin@example.test"));assertFalse(row.toString().contains("Private role name"));
  mvc.perform(put("/api/access/roles/{id}",id).header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Changed","permissions",List.of("CREDENTIAL_VIEW"))))).andExpect(status().isOk());
  var changed=events(a.token,"/api/audit/tenant",id);assertEquals(2,changed.get("total").asInt());assertTrue(changed.toString().contains("ROLE_UPDATED"));assertTrue(changed.toString().contains("CREDENTIAL_VIEW"));
 }
 @Test void tenantCannotSelectAnotherTenantOrSeeGlobalStudentSecurityEvents()throws Exception{
  var a=tenant();var b=tenant();var role=postJson("/api/access/roles",b.token,Map.of("name","Other","permissions",List.of()),201);UUID id=UUID.fromString(role.get("id").asText());
  mvc.perform(get("/api/audit/tenant").param("tenantId",b.id.toString()).param("resourceId",id.toString()).header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
  assertEquals(1,events(root(),"/api/platform/audit",id).get("total").asInt());
  String email=UUID.randomUUID()+"@student.test";var student=postJson("/api/auth/student/register","",Map.of("email",email,"password",password),201);
  UUID sid=jdbc.queryForObject("select id from student_accounts where email=?",UUID.class,email);assertEquals(0,events(a.token,"/api/audit/tenant",sid).get("total").asInt());var global=events(root(),"/api/platform/audit",sid);assertTrue(global.toString().contains("STUDENT_REGISTERED"));assertFalse(global.toString().contains(email));
  mvc.perform(get("/api/audit/tenant").header("Authorization","Bearer "+student.get("token").asText())).andExpect(status().isForbidden());mvc.perform(get("/api/platform/audit").header("Authorization","Bearer "+a.token)).andExpect(status().isForbidden());
 }
 @Test void permissionGrantAndRemovalApplyToAnExistingEmployeeSession()throws Exception{
  var a=tenant();var role=postJson("/api/access/roles",a.token,Map.of("name","Auditor","permissions",List.of()),201);String rid=role.get("id").asText();String email=UUID.randomUUID()+"@staff.test";
  postJson("/api/access/users",a.token,Map.of("fullName","Test","email",email,"password",password,"roleIds",List.of(rid)),201);
  String slug=jdbc.queryForObject("select slug from tenants where id=?",String.class,a.id);String staff=postJson("/api/auth/login","",Map.of("organizationSlug",slug,"email",email,"password",password),200).get("token").asText();
  mvc.perform(get("/api/audit/tenant").header("Authorization","Bearer "+staff)).andExpect(status().isForbidden());
  for(var grants:List.of(List.of("AUDIT_VIEW"),List.<String>of())){
   mvc.perform(put("/api/access/roles/{id}",rid).header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Auditor","permissions",grants)))).andExpect(status().isOk());
   mvc.perform(get("/api/audit/tenant").header("Authorization","Bearer "+staff)).andExpect(status().is(grants.isEmpty()?403:200));
  }
 }
 @Test void businessRollbackRemovesItsAuditEventAndForeignMutationDoesNotCreateOne()throws Exception{
  var a=tenant();AtomicReference<UUID> id=new AtomicReference<>();new TransactionTemplate(transactions).executeWithoutResult(tx->{id.set(access.saveRole(a.id,null,new AccessDtos.RoleRequest("Rollback-"+UUID.randomUUID(),Set.of())).id());tx.setRollbackOnly();});
  assertEquals(0,jdbc.queryForObject("select count(*) from tenant_roles where id=?",Integer.class,id.get()));assertEquals(0,jdbc.queryForObject("select count(*) from audit_events where resource_id=?",Integer.class,id.get()));
  var b=tenant();UUID foreign=UUID.fromString(postJson("/api/access/roles",b.token,Map.of("name","Foreign","permissions",List.of()),201).get("id").asText());
  mvc.perform(delete("/api/access/roles/{id}",foreign).header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());assertEquals(1,events(b.token,"/api/audit/tenant",foreign).get("total").asInt());
 }
 @Test void issuanceRevocationAndDocumentEventsAreAtomicAndDoNotLogTheDocument()throws Exception{
  var a=tenant();when(storage.store(any(),any(),any())).thenAnswer(call->call.getArgument(0));when(blockchain.anchor(any(),any(),any())).thenReturn("pending:audit");
  var s=postJson("/api/auth/student/register","",Map.of("fullName","Private Student","country","BO","documentType","CI","documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2001-05-12","department","La Paz","email",UUID.randomUUID()+"@test.example","password",password),201);
  var profile=json.readTree(mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+s.get("token").asText())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());postJson("/api/students/redeem",a.token,Map.of("code",profile.get("searchCode").asText()),200);
  UUID id=UUID.fromString(postJson("/api/credentials",a.token,Map.of("studentId",profile.get("id").asText(),"credentialType","CERTIFICADO","title","Private Title"),201).get("id").asText());postJson("/api/credentials/"+id+"/generate-document",a.token,Map.of(),200);
  mvc.perform(patch("/api/credentials/{id}/issue",id).header("Authorization","Bearer "+a.token)).andExpect(status().isOk());mvc.perform(patch("/api/credentials/{id}/issue",id).header("Authorization","Bearer "+a.token)).andExpect(status().isBadRequest());
  mvc.perform(patch("/api/credentials/{id}/revoke",id).header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Private reason\"}")).andExpect(status().isOk());
  var history=events(a.token,"/api/audit/tenant",id);assertEquals(4,history.get("total").asInt());for(String event:List.of("CREDENTIAL_CREATED","DOCUMENT_GENERATED","CREDENTIAL_ISSUED","CREDENTIAL_REVOKED"))assertTrue(history.toString().contains(event));assertFalse(history.toString().contains("Private"));assertFalse(history.toString().contains("pending:audit"));
 }
 @Test void filtersAndPaginationRejectInvalidBounds()throws Exception{
  String token=root();for(String size:List.of("0","101"))mvc.perform(get("/api/platform/audit").param("size",size).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());mvc.perform(get("/api/platform/audit").param("from","2026-12-01T00:00:00Z").param("before","2026-01-01T00:00:00Z").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
 }
 @Test void postgresRejectsAuditUpdatesAndDeletes()throws Exception{
  try(var connection=jdbc.getDataSource().getConnection()){org.junit.jupiter.api.Assumptions.assumeTrue(connection.getMetaData().getDatabaseProductName().equals("PostgreSQL"));}
  var a=tenant();assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("update audit_events set action='ROLE_DELETED' where tenant_id=?",a.id));assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.update("delete from audit_events where tenant_id=?",a.id));assertTrue(jdbc.queryForObject("select count(*) from audit_events where tenant_id=?",Integer.class,a.id)>0);
 }
}
