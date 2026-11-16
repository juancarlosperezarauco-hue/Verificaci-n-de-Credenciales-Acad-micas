package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import com.fasterxml.jackson.databind.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class IdentityReviewIntegrationTests {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.context.ApplicationContext context;
    @MockitoBean DocumentStoragePort storage;
    final Map<String,byte[]> objects=new ConcurrentHashMap<>();
    static final String PASSWORD="IdentityReviewTest123!";
    record Org(String token,String id,String slug) {}
    record Student(String token,String id,String code,Map<String,Object> profile) {}
    @BeforeEach void files(){
        objects.clear();when(storage.store(anyString(),any(),anyString())).thenAnswer(i->{objects.put(i.getArgument(0),i.getArgument(1));return i.getArgument(0);});
        when(storage.load(anyString())).thenAnswer(i->{byte[] bytes=objects.get(i.<String>getArgument(0));if(bytes==null)throw new IllegalStateException("missing");return bytes;});
    }
    JsonNode post(String path,String token,Object body,int expected)throws Exception {
        var r=mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andExpect(status().is(expected)).andReturn();
        return r.getResponse().getContentAsString().isEmpty()?json.createObjectNode():json.readTree(r.getResponse().getContentAsString());
    }
    Org org()throws Exception {
        String slug="identity-"+UUID.randomUUID();var s=post("/api/auth/register","",Map.of("organizationName","Instituto de prueba","organizationSlug",slug,"institutionType","COLEGIO","email","admin@example.test","password",PASSWORD),201);
        ApprovedOrganizationFixture.approve(s,context);return new Org(s.get("token").asText(),s.get("tenantId").asText(),slug);
    }
    Student student(boolean complete,boolean verified)throws Exception {
        var body=new HashMap<String,Object>(Map.of("email",UUID.randomUUID()+"@example.test","password",PASSWORD));
        var profile=Map.<String,Object>of("fullName","Estudiante de prueba","country","BO","documentType","CI","documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2000-04-20","department","La Paz");
        if(complete)body.putAll(profile);var s=post("/api/auth/student/register","",body,201);String token=s.get("token").asText();
        var own=json.readTree(mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+token)).andReturn().getResponse().getContentAsString());
        if(verified)jdbc.update("update student_accounts set email_verified=true where id=?",UUID.fromString(own.get("id").asText()));
        return new Student(token,own.get("id").asText(),own.get("searchCode").asText(),profile);
    }
    void link(Student s,Org o)throws Exception{post("/api/students/redeem",o.token,Map.of("code",s.code),200);}
    static byte[] png(int width,int height)throws Exception {var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);image.flush();return out.toByteArray();}
    MockMultipartFile image(String field)throws Exception{return new MockMultipartFile(field,"private-name.png","image/png",png(30,20));}
    ResultActions upload(Student s,Org o,boolean accepted,MockMultipartFile front)throws Exception {
        return mvc.perform(multipart("/api/me/identity-verifications").file(front).file(image("back")).param("tenantId",o.id).param("accepted",Boolean.toString(accepted)).header("Authorization","Bearer "+s.token));
    }
    JsonNode submit(Student s,Org o)throws Exception{return json.readTree(upload(s,o,true,image("front")).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());}
    String own(Student s)throws Exception{return mvc.perform(get("/api/me/identity-verifications").header("Authorization","Bearer "+s.token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();}
    String path(JsonNode row){return "/api/institution/identity-verifications/"+row.get("id").asText();}
    void edit(Student s,Map<String,Object> profile)throws Exception {mvc.perform(patch("/api/me/profile").header("Authorization","Bearer "+s.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(profile))).andExpect(status().isOk());}

    @Test void approvalIsScopedToInstitutionAndNeverDuplicatesGlobalIdentity()throws Exception {
        var a=org();var b=org();var s=student(true,true);link(s,a);link(s,b);var row=submit(s,a);
        assertFalse(row.has("frontKey"));assertFalse(row.has("profileHash"));assertEquals("PENDING",row.get("effectiveStatus").asText());
        mvc.perform(get("/api/institution/identity-verifications").header("Authorization","Bearer "+b.token)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        post(path(row)+"/decision",a.token,Map.of("status","VERIFIED"),200);
        assertEquals("VERIFIED",json.readTree(own(s)).get("submissions").get(0).get("effectiveStatus").asText());
        var second=submit(s,b);assertEquals("PENDING",second.get("effectiveStatus").asText());
        assertEquals(1,jdbc.queryForObject("select count(*) from student_accounts where id=?",Integer.class,UUID.fromString(s.id)));
        upload(s,a,true,image("front")).andExpect(status().isConflict());
        var bytes=mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(content().contentType(MediaType.IMAGE_JPEG)).andReturn().getResponse().getContentAsByteArray();
        assertEquals(0xff,bytes[0]&255);assertEquals(0xd8,bytes[1]&255);
        assertEquals(1,jdbc.queryForObject("select count(*) from audit_events where action='IDENTITY_DOCUMENT_VIEWED' and resource_id=?",Integer.class,UUID.fromString(row.get("id").asText())));
    }
    @Test void studentsTenantsAndPublicCannotReadEachOthersImages()throws Exception {
        var a=org();var b=org();var s=student(true,true);var other=student(true,true);link(s,a);link(s,b);var row=submit(s,a);String id=row.get("id").asText();
        for(String suffix:List.of("","/files/FRONT"))mvc.perform(get(path(row)+suffix).header("Authorization","Bearer "+b.token)).andExpect(status().isNotFound());
        post(path(row)+"/decision",b.token,Map.of("status","VERIFIED"),404);
        mvc.perform(get("/api/me/identity-verifications/"+id+"/files/FRONT").header("Authorization","Bearer "+other.token)).andExpect(status().isNotFound());
        post("/api/me/identity-verifications/"+id+"/withdraw",other.token,Map.of(),404);
        mvc.perform(get(path(row)+"/files/FRONT")).andExpect(status().isForbidden());
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+s.token)).andExpect(status().isForbidden());
        mvc.perform(get(path(row)+"/files/UNKNOWN").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
        String key=jdbc.queryForObject("select front_key from identity_review_submissions where id=?",String.class,UUID.fromString(id));objects.put(key,"tampered".getBytes());
        String part="FRONT";
        mvc.perform(get(path(row)+"/files/"+part).header("Authorization","Bearer "+a.token)).andExpect(status().isServiceUnavailable());
    }
    @Test void permissionChangesApplyToExistingReviewerToken()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);
        String role=post("/api/access/roles",a.token,Map.of("name","Revisor","permissions",List.of("STUDENT_VIEW")),201).get("id").asText();
        post("/api/access/users",a.token,Map.of("fullName","Revisor Prueba","email","reviewer@example.test","password",PASSWORD,"roleIds",List.of(role)),201);
        String token=post("/api/auth/login","",Map.of("organizationSlug",a.slug,"email","reviewer@example.test","password",PASSWORD),200).get("token").asText();
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        for(boolean grant:List.of(true,false)){
            mvc.perform(put("/api/access/roles/"+role).header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("name","Revisor","permissions",grant?List.of("IDENTITY_REVIEW"):List.of("STUDENT_VIEW"))))).andExpect(status().isOk());
            mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+token)).andExpect(status().is(grant?200:403));
        }
    }
    @Test void profileEmailExplicitConsentLinkAndRealImagesAreRequiredBeforeStorage()throws Exception {
        var a=org();var b=org();var s=student(true,false);link(s,a);
        upload(s,a,true,image("front")).andExpect(status().isBadRequest());
        jdbc.update("update student_accounts set email_verified=true where id=?",UUID.fromString(s.id));
        upload(s,a,false,image("front")).andExpect(status().isBadRequest());upload(s,b,true,image("front")).andExpect(status().isNotFound());
        upload(s,a,true,new MockMultipartFile("front","ci.png","image/png","<svg>fake</svg>".getBytes())).andExpect(status().isBadRequest());
        upload(s,a,true,new MockMultipartFile("front","big.jpg","image/jpeg",new byte[5*1024*1024+1])).andExpect(status().isBadRequest());
        upload(s,a,true,new MockMultipartFile("front","wide.png","image/png",png(6001,1))).andExpect(status().isBadRequest());
        var incomplete=student(false,true);upload(incomplete,a,true,image("front")).andExpect(status().isBadRequest());
        verify(storage,never()).store(anyString(),any(),anyString());
    }
    @Test void rejectionNeedsReasonAndAllowsNewVersionWithoutRewritingHistory()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);
        post(path(row)+"/decision",a.token,Map.of("status","REJECTED","reason","corto"),400);
        post(path(row)+"/decision",a.token,Map.of("status","PENDING"),400);
        String reason="El documento no es legible, vuelve a enviarlo.";
        post(path(row)+"/decision",a.token,Map.of("status","REJECTED","reason",reason),200);
        post(path(row)+"/decision",a.token,Map.of("status","VERIFIED"),409);
        var newer=submit(s,a);assertNotEquals(row.get("id"),newer.get("id"));
        var history=json.readTree(own(s)).get("submissions");assertEquals(2,history.size());assertTrue(history.toString().contains(reason));
        var changes=jdbc.queryForList("select changes from audit_events where resource_id=?",String.class,UUID.fromString(row.get("id").asText()));assertTrue(changes.stream().allMatch(Objects::isNull));
    }
    @Test void profileRevisionInvalidatesReviewEvenIfValuesAreChangedBack()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);post(path(row)+"/decision",a.token,Map.of("status","VERIFIED"),200);
        var updated=new HashMap<>(s.profile);updated.put("fullName","Nombre actualizado");edit(s,updated);edit(s,s.profile);
        var old=json.readTree(own(s)).get("submissions").get(0);assertEquals("UNVERIFIED",old.get("effectiveStatus").asText());assertTrue(old.get("profileChanged").asBoolean());
        var pending=submit(s,a);updated.put("fullName","Otro nombre actualizado");edit(s,updated);
        post(path(pending)+"/decision",a.token,Map.of("status","VERIFIED"),409);
        var last=submit(s,a);assertEquals("PENDING",last.get("status").asText());
        mvc.perform(get(path(pending)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
    }
    @Test void withdrawalAndConsentRevocationDoNotReopenOldDocumentsAfterRelinking()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);
        post("/api/me/identity-verifications/"+row.get("id").asText()+"/withdraw",s.token,Map.of(),204);
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
        var next=submit(s,a);
        UUID consent=jdbc.queryForObject("select id from student_consents where student_id=? and tenant_id=? and revoked_at is null",UUID.class,UUID.fromString(s.id),UUID.fromString(a.id));
        mvc.perform(delete("/api/me/consents/"+consent).header("Authorization","Bearer "+s.token)).andExpect(status().isOk());
        mvc.perform(get(path(next)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
        var rotated=post("/api/me/search-code/rotate",s.token,Map.of(),200);
        post("/api/students/redeem",a.token,Map.of("code",rotated.get("searchCode").asText()),200);
        mvc.perform(get(path(next)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/me/identity-verifications/"+next.get("id").asText()+"/files/FRONT").header("Authorization","Bearer "+s.token)).andExpect(status().isOk());
        assertEquals("PENDING",submit(s,a).get("effectiveStatus").asText());
    }
    @Test void storageFailurePreservesPreviousRequestAndNeverCommitsHalfSubmission()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);
        var updated=new HashMap<>(s.profile);updated.put("fullName","Nombre actualizado");edit(s,updated);
        var calls=new java.util.concurrent.atomic.AtomicInteger();doAnswer(i->{if(calls.incrementAndGet()==2)throw new IllegalStateException("private storage details");objects.put(i.getArgument(0),i.getArgument(1));return i.getArgument(0);}).when(storage).store(anyString(),any(),anyString());
        upload(s,a,true,image("front")).andExpect(status().isServiceUnavailable());
        assertEquals(1,jdbc.queryForObject("select count(*) from identity_review_submissions where student_id=?",Integer.class,UUID.fromString(s.id)));
        assertNull(jdbc.queryForObject("select withdrawn_at from identity_review_submissions where id=?",Object.class,UUID.fromString(row.get("id").asText())));
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isOk());
    }
    @Test void concurrentDecisionsHaveExactlyOneWinner()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);var start=new CountDownLatch(1);
        var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> decide=()->{start.await();return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path(row)+"/decision").header("Authorization","Bearer "+a.token).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}")).andReturn().getResponse().getStatus();};
            var first=pool.submit(decide);var second=pool.submit(decide);start.countDown();var results=new ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);
            assertEquals(1,jdbc.queryForObject("select count(*) from audit_events where resource_id=? and action='IDENTITY_VERIFIED'",Integer.class,UUID.fromString(row.get("id").asText())));
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentSubmissionsCreateOnlyOnePendingVersion()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> upload=()->{start.await();return upload(s,a,true,image("front")).andReturn().getResponse().getStatus();};
            var first=pool.submit(upload);var second=pool.submit(upload);start.countDown();var results=new ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(201,409),results);
            assertEquals(1,jdbc.queryForObject("select count(*) from identity_review_submissions where student_id=?",Integer.class,UUID.fromString(s.id)));
        } finally {pool.shutdownNow();}
    }
    @Test void globalSuperAdminHasNoImplicitAccessToPrivateIdentityFiles()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);String email=UUID.randomUUID()+"@platform.test";
        var users=context.getBean(bo.edu.uagrm.credenciales.user.AppUserRepository.class);var passwords=context.getBean(org.springframework.security.crypto.password.PasswordEncoder.class);
        users.saveAndFlush(new bo.edu.uagrm.credenciales.user.AppUser(null,email,passwords.encode(PASSWORD),bo.edu.uagrm.credenciales.user.UserRole.SUPER_ADMIN));
        String root=post("/api/auth/platform/login","",Map.of("email",email,"password",PASSWORD),200).get("token").asText();
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+root)).andExpect(status().isForbidden());
        mvc.perform(get("/api/me/identity-verifications/"+row.get("id").asText()+"/files/FRONT").header("Authorization","Bearer "+root)).andExpect(status().isForbidden());
    }
    @Test void deactivatedStudentAndSuspendedInstitutionCannotContinueReview()throws Exception {
        var a=org();var s=student(true,true);link(s,a);var row=submit(s,a);
        jdbc.update("update tenants set active=false where id=?",UUID.fromString(a.id));jdbc.update("update organization_governance set status='SUSPENDED' where tenant_id=?",UUID.fromString(a.id));
        assertEquals(0,json.readTree(own(s)).get("institutions").size());
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isForbidden());
        jdbc.update("update tenants set active=true where id=?",UUID.fromString(a.id));jdbc.update("update organization_governance set status='ACTIVE' where tenant_id=?",UUID.fromString(a.id));
        jdbc.update("update student_accounts set active=false where id=?",UUID.fromString(s.id));
        mvc.perform(get(path(row)+"/files/FRONT").header("Authorization","Bearer "+a.token)).andExpect(status().isNotFound());
        mvc.perform(get("/api/institution/identity-verifications").header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
