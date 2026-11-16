package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.storage.*;
import bo.edu.uagrm.credenciales.identityreview.EvidenceLifecycle;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.*;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.sql.Timestamp;
import java.time.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class IdentityLifecycleIntegrationTests {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired org.springframework.context.ApplicationContext context;@Autowired AppUserRepository users;@Autowired JwtService jwt;@Autowired StorageMaintenance maintenance;@Autowired EvidenceLifecycle lifecycle;@Autowired ManagedStorage managed;@Autowired PlatformTransactionManager transactions;
 @MockitoBean DocumentStoragePort storage;final Map<String,byte[]> blobs=new ConcurrentHashMap<>();static final String PASSWORD="LifecycleTestPassword123!";
 record Org(String token,String id){}record Student(String token,String id,String code,Map<String,Object> profile){}
 @BeforeEach void mockFiles(){when(storage.store(anyString(),any(),anyString())).thenAnswer(i->{blobs.put(i.getArgument(0),i.getArgument(1));return i.getArgument(0);});when(storage.load(anyString())).thenAnswer(i->{var b=blobs.get(i.<String>getArgument(0));if(b==null)throw new IllegalStateException("missing");return b;});doAnswer(i->{blobs.remove(i.<String>getArgument(0));return null;}).when(storage).delete(anyString());}
 JsonNode call(String method,String path,String token,Object body,int expected)throws Exception{var b=org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(org.springframework.http.HttpMethod.valueOf(method),path).header("Authorization","Bearer "+token);if(body!=null)b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));var r=mvc.perform(b).andExpect(status().is(expected)).andReturn().getResponse();return r.getContentAsString().isEmpty()?json.createObjectNode():json.readTree(r.getContentAsString());}
 String root(){return jwt.create(UserPrincipal.from(users.saveAndFlush(new AppUser(null,UUID.randomUUID()+"@root.test","test-only",UserRole.SUPER_ADMIN))));}
 Student student()throws Exception{var profile=Map.<String,Object>of("fullName","Estudiante prueba","country","BO","documentType","CI","documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2000-01-12","department","La Paz");var body=new HashMap<String,Object>(profile);body.put("email",UUID.randomUUID()+"@example.test");body.put("password",PASSWORD);String token=call("POST","/api/auth/student/register","",body,201).get("token").asText();var own=call("GET","/api/me/profile",token,null,200);jdbc.update("update student_accounts set email_verified=true where id=?",UUID.fromString(own.get("id").asText()));return new Student(token,own.get("id").asText(),own.get("searchCode").asText(),profile);}
 Org org()throws Exception{var o=call("POST","/api/auth/register","",Map.of("organizationName","Institución prueba","organizationSlug","life-"+UUID.randomUUID(),"institutionType","INSTITUTO","email","admin@example.test","password",PASSWORD),201);ApprovedOrganizationFixture.approve(o,context);return new Org(o.get("token").asText(),o.get("tenantId").asText());}
 MockMultipartFile image(String name)throws Exception{var source=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(source,"png",out);source.flush();return new MockMultipartFile(name,"prueba.png","image/png",out.toByteArray());}
 JsonNode dispute(Student s,String number)throws Exception{return json.readTree(mvc.perform(multipart("/api/me/identity-disputes").file(image("front")).file(image("back")).param("country","BO").param("documentType","CI").param("documentNumber",number).param("reason","Necesito una revisión por conflicto de identidad.").header("Authorization","Bearer "+s.token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
 JsonNode proof(Student s,Org o)throws Exception{call("POST","/api/students/redeem",o.token,Map.of("code",s.code),200);return json.readTree(mvc.perform(multipart("/api/me/identity-verifications").file(image("front")).file(image("back")).param("tenantId",o.id).param("accepted","true").header("Authorization","Bearer "+s.token)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
 String key(String table,String id,String column){return jdbc.queryForObject("select "+column+" from "+table+" where id=?",String.class,UUID.fromString(id));}
 void age(String table,String id){jdbc.update("update "+table+" set retired_at=? where id=?",Timestamp.from(Instant.now().minus(Duration.ofDays(100))),UUID.fromString(id));}
 @Test void avatarIsPrivateEncryptedBoundedAndOnlyUnreferencedObjectsAreRemoved()throws Exception{
  var s=student();var other=student();String root=root();
  mvc.perform(multipart("/api/me/avatar").file(image("file")).header("Authorization","Bearer "+s.token)).andExpect(status().isNoContent());
  String key=key("student_accounts",s.id,"photo_object_key");byte[] bytes=mvc.perform(get("/api/me/avatar").header("Authorization","Bearer "+s.token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsByteArray();
  var decoded=ImageIO.read(new ByteArrayInputStream(bytes));assertEquals(512,decoded.getWidth());assertFalse(Arrays.equals(bytes,blobs.get(key)));
  call("GET","/api/me/avatar/status",other.token,null,200);mvc.perform(get("/api/me/avatar").header("Authorization","Bearer "+other.token)).andExpect(status().isNotFound());call("GET","/api/me/avatar/status",root,null,403);mvc.perform(get("/api/me/avatar")).andExpect(status().isForbidden());
  jdbc.update("update managed_objects set delete_requested_at=CURRENT_TIMESTAMP where object_key=?",key);maintenance.cleanup();assertNotNull(blobs.get(key),"Una referencia activa siempre protege el objeto");
  call("DELETE","/api/me/avatar",s.token,null,204);maintenance.cleanup();assertNull(blobs.get(key));assertEquals("REMOVED",jdbc.queryForObject("select state from managed_objects where object_key=?",String.class,key));
 }
 @Test void disputesArePrivateAndObservationRequiresNewEvidenceBeforeResolution()throws Exception{
  var s=student();var other=student();var o=org();String root=root();String id=dispute(s,"NEW-"+UUID.randomUUID()).get("id").asText(),path="/api/platform/identity-disputes/"+id;
  mvc.perform(get("/api/me/identity-disputes/"+id+"/files/FRONT").header("Authorization","Bearer "+other.token)).andExpect(status().isNotFound());
  call("GET","/api/platform/identity-disputes",o.token,null,403);call("GET","/api/platform/identity-disputes",s.token,null,403);mvc.perform(get(path+"/files/FRONT").header("Authorization","Bearer "+root)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  call("POST",path+"/resolution",root,Map.of("action","OBSERVE","reason","Aclara los datos del documento."),200);
  var profile=new HashMap<String,Object>(s.profile);profile.put("documentNumber","NEW-"+UUID.randomUUID());
  call("POST",path+"/resolution",root,Map.of("action","CORRECT_PROFILE","reason","Revisión administrativa completada.","profile",profile),400);
  mvc.perform(multipart("/api/me/identity-disputes/"+id+"/resubmit").file(image("front")).file(image("back")).param("explanation","Adjunto evidencia actualizada.").header("Authorization","Bearer "+s.token)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OPEN"));
  call("POST",path+"/resolution",root,Map.of("action","REJECT","reason","La reclamación no corresponde."),200);assertEquals(4,call("GET","/api/me/identity-disputes",s.token,null,200).get(0).get("events").size());
 }
 @Test void humanCorrectionUpdatesOnlyClaimantAndInvalidatesSessionsAndInstitutionalEvidence()throws Exception{
  var s=student();var o=org();var proof=proof(s,o);String number="FIX-"+UUID.randomUUID(),id=dispute(s,number).get("id").asText();var profile=new HashMap<String,Object>(s.profile);profile.put("documentNumber",number);
  call("POST","/api/platform/identity-disputes/"+id+"/resolution",root(),Map.of("action","CORRECT_PROFILE","reason","Se verificó la corrección con evidencia.","profile",profile),200);
  assertEquals(number.toUpperCase(Locale.ROOT),key("student_accounts",s.id,"document_number"));assertNotNull(jdbc.queryForObject("select retired_at from identity_review_submissions where id=?",Timestamp.class,UUID.fromString(proof.get("id").asText())));
  call("GET","/api/me/profile",s.token,null,403);assertEquals("RESOLVED",key("identity_disputes",id,"status"));
 }
 @Test void occupiedDocumentCannotBeTakenOverOrDiscloseTheOtherAccount()throws Exception{
  var a=student();var b=student();String id=dispute(a,b.profile.get("documentNumber").toString()).get("id").asText();var profile=new HashMap<String,Object>(a.profile);profile.put("documentNumber",b.profile.get("documentNumber"));
  call("POST","/api/platform/identity-disputes/"+id+"/resolution",root(),Map.of("action","CORRECT_PROFILE","reason","Se solicita corrección de documento.","profile",profile),400);
  assertEquals(a.profile.get("documentNumber").toString().toUpperCase(Locale.ROOT),key("student_accounts",a.id,"document_number"));assertEquals(b.profile.get("documentNumber").toString().toUpperCase(Locale.ROOT),key("student_accounts",b.id,"document_number"));assertEquals("OPEN",key("identity_disputes",id,"status"));
 }
 @Test void retentionPreservesLiveEvidenceAndRemovesOnlyExpiredRetiredFiles()throws Exception{
  var s=student();var o=org();String id=proof(s,o).get("id").asText(),front=key("identity_review_submissions",id,"front_key"),hash=key("identity_review_submissions",id,"front_hash");age("identity_review_submissions",id);lifecycle.purgeExpired();assertNotNull(key("identity_review_submissions",id,"front_key"));
  call("POST","/api/me/identity-verifications/"+id+"/withdraw",s.token,Map.of(),204);age("identity_review_submissions",id);assertTrue(lifecycle.purgeExpired()>0);assertNull(key("identity_review_submissions",id,"front_key"));assertEquals(hash,key("identity_review_submissions",id,"front_hash"));assertNotNull(blobs.get(front),"Retirar referencias confirma una transacción antes de borrar bytes");maintenance.cleanup();assertNull(blobs.get(front));
  assertEquals(1,jdbc.queryForObject("select count(*) from student_accounts where id=?",Integer.class,UUID.fromString(s.id)));assertEquals(1,jdbc.queryForObject("select count(*) from tenants where id=?",Integer.class,UUID.fromString(o.id)));
 }
 @Test void openDisputeIsProtectedThenWithdrawalAndRetentionAllowPurging()throws Exception{
  var s=student();String id=dispute(s,"CLAIM-"+UUID.randomUUID()).get("id").asText();age("identity_disputes",id);lifecycle.purgeExpired();assertNotNull(key("identity_disputes",id,"front_key"));
  call("POST","/api/me/identity-disputes/"+id+"/withdraw",s.token,Map.of(),200);age("identity_disputes",id);lifecycle.purgeExpired();assertFalse(call("GET","/api/me/identity-disputes",s.token,null,200).get(0).get("documentsAvailable").asBoolean());assertEquals(2,jdbc.queryForObject("select count(*) from identity_dispute_events where dispute_id=?",Integer.class,UUID.fromString(id)));
 }
 @Test void rolledBackUploadRemainsReconcilableAndDeletionFailuresCanBeRetried(){
  String[] key={null};assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).execute(s->{key[0]=managed.store(null,UUID.randomUUID(),"AVATAR",new byte[]{1,2,3},"image/jpeg");throw new IllegalStateException("Simulated database failure");}));
  assertEquals("ABANDONED",jdbc.queryForObject("select state from managed_objects where object_key=?",String.class,key[0]));doThrow(new IllegalStateException("offline")).when(storage).delete(key[0]);assertTrue(maintenance.cleanup().failures()>0);assertNotNull(blobs.get(key[0]));
  doAnswer(i->{blobs.remove(i.<String>getArgument(0));return null;}).when(storage).delete(key[0]);maintenance.cleanup();assertNull(blobs.get(key[0]));
 }
 @Test void rootStorageEndpointsNeverAcceptTenantOrStudentAccess()throws Exception{var o=org();var s=student();call("GET","/api/platform/storage/maintenance",o.token,null,403);call("POST","/api/platform/storage/maintenance/cleanup",s.token,Map.of(),403);assertTrue(call("GET","/api/platform/storage/maintenance",root(),null,200).get("encryptionConfigured").asBoolean());}
}
