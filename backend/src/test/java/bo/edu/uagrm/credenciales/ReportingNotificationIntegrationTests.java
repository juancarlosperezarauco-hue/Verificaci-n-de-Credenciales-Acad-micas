package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.notification.NotificationService;
import bo.edu.uagrm.credenciales.mail.MailOutboxStore;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.CellType;
import org.junit.jupiter.api.*;
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
class ReportingNotificationIntegrationTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.context.ApplicationContext context;
    @Autowired NotificationService notifications; @Autowired MailOutboxStore outbox;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean DocumentStoragePort storage; @MockitoBean BlockchainAnchorPort blockchain;
    final Map<String,byte[]> blobs=new ConcurrentHashMap<>();
    static final String PASSWORD="ReportTestPassword123!";
    record Org(String token,UUID id,String slug){}
    record Student(String token,UUID id,String code){}
    @BeforeEach void setup(){
        when(storage.store(anyString(),any(),anyString())).thenAnswer(i->{blobs.put(i.getArgument(0),i.getArgument(1));return i.getArgument(0);});
        when(storage.load(anyString())).thenAnswer(i->blobs.get(i.<String>getArgument(0)));
        when(blockchain.anchor(any(),any(),any())).thenReturn("pending:report");
    }
    JsonNode call(String method,String path,String token,Object body,int expected)throws Exception{
        var b=request(org.springframework.http.HttpMethod.valueOf(method),path).header("Authorization","Bearer "+token);
        if(body!=null)b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        var response=mvc.perform(b).andExpect(status().is(expected)).andReturn().getResponse();
        return response.getContentAsString().isEmpty()?json.createObjectNode():json.readTree(response.getContentAsString());
    }
    Org org()throws Exception{
        String slug="reports-"+UUID.randomUUID();
        var r=call("POST","/api/auth/register","",Map.of("organizationName","Organización de reportes","organizationSlug",slug,"institutionType","UNIVERSIDAD","email","admin@example.test","password",PASSWORD),201);
        ApprovedOrganizationFixture.approve(r,context);
        return new Org(r.get("token").asText(),UUID.fromString(r.get("tenantId").asText()),slug);
    }
    Student student(boolean verified)throws Exception{
        var r=call("POST","/api/auth/student/register","",Map.of("email",UUID.randomUUID()+"@example.test","password",PASSWORD,"fullName","Estudiante de reportes","country","BO","documentType","CI","documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2000-05-12","department","La Paz"),201);
        String token=r.get("token").asText();var profile=call("GET","/api/me/profile",token,null,200);UUID id=UUID.fromString(profile.get("id").asText());
        if(verified)jdbc.update("update student_accounts set email_verified=true where id=?",id);
        return new Student(token,id,profile.get("searchCode").asText());
    }
    void link(Org o,Student s)throws Exception{call("POST","/api/students/redeem",o.token,Map.of("code",s.code),200);}
    JsonNode draft(Org o,Student s,String title)throws Exception{return call("POST","/api/credentials",o.token,Map.of("studentId",s.id,"credentialType","CERTIFICADO","title",title),201);}
    JsonNode issue(Org o,Student s,String title)throws Exception{
        var c=draft(o,s,title);String id=c.get("id").asText();call("POST","/api/credentials/"+id+"/generate-document",o.token,Map.of(),200);
        return call("PATCH","/api/credentials/"+id+"/issue",o.token,Map.of(),200);
    }
    void publish(Student s,Org o,String key){
        new TransactionTemplate(transactions).executeWithoutResult(tx->notifications.publish(s.id,o.id,key,"REQUEST_APPROVE",UUID.randomUUID(),"Tu solicitud fue aprobada"));
    }
    @Test void historyIsPagedFilteredAndPrivateAcrossInstitutions()throws Exception{
        var a=org();var b=org();var s=student(true);var other=student(true);link(a,s);link(b,s);
        var first=issue(a,s,"Documento de A");var second=issue(b,s,"Documento de B");draft(a,s,"Borrador invisible");
        jdbc.update("update credentials set issued_at=? where id=?",java.sql.Timestamp.from(java.time.Instant.parse("2025-12-31T23:59:59Z")),UUID.fromString(first.get("id").asText()));
        jdbc.update("update credentials set issued_at=? where id=?",java.sql.Timestamp.from(java.time.Instant.parse("2026-01-01T00:00:00Z")),UUID.fromString(second.get("id").asText()));
        var page=call("GET","/api/me/credentials/page?size=1",s.token,null,200);
        assertEquals(2,page.get("total").asInt());assertEquals(1,page.get("items").size());assertEquals("Documento de B",page.get("items").get(0).get("title").asText());
        assertEquals(1,call("GET","/api/me/credentials/page?institutionId="+a.id+"&year=2025&type=CERTIFICADO",s.token,null,200).get("total").asInt());
        assertEquals(0,call("GET","/api/me/credentials/page?institutionId="+a.id+"&year=2026",s.token,null,200).get("total").asInt());
        assertEquals(0,call("GET","/api/me/credentials/page",other.token,null,200).get("total").asInt());
        var filters=call("GET","/api/me/credentials/filters",s.token,null,200);assertEquals(2,filters.get("institutions").size());assertEquals(2,filters.get("years").size());
        call("GET","/api/me/credentials/page?size=51",s.token,null,400);
        call("GET","/api/me/credentials/page?year=0",s.token,null,400);
        call("GET","/api/me/credentials/page?type=%27%20OR%201=1",s.token,null,400);
        call("GET","/api/me/credentials/page",a.token,null,403);
        assertEquals(2,call("GET","/api/me/credentials",s.token,null,200).size());
    }
    @Test void notificationCreationIsAtomicIdempotentAndRequiresVerifiedRecipient()throws Exception{
        var o=org();var verified=student(true);var pending=student(false);
        String key="dedup:"+UUID.randomUUID();publish(verified,o,key);publish(verified,o,key);publish(pending,o,key);
        assertEquals(1,jdbc.queryForObject("select count(*) from academic_notifications where student_id=?",Long.class,verified.id));
        assertEquals(1,jdbc.queryForObject("select count(*) from mail_outbox where student_id=? and kind='ACADEMIC'",Long.class,verified.id));
        assertEquals(0,jdbc.queryForObject("select count(*) from mail_outbox where student_id=? and kind='ACADEMIC'",Long.class,pending.id));
        new TransactionTemplate(transactions).executeWithoutResult(tx->{notifications.publish(verified.id,o.id,"rollback","REQUEST_APPROVE",UUID.randomUUID(),"Aviso de prueba");tx.setRollbackOnly();});
        assertEquals(1,call("GET","/api/me/notifications",verified.token,null,200).get("total").asInt());
        String payload=jdbc.queryForObject("select encrypted_payload from mail_outbox where student_id=? and kind='ACADEMIC'",String.class,verified.id);
        assertFalse(payload.contains("@example.test"));assertFalse(payload.contains("/student/notifications"));
    }
    @Test void notificationReadCannotBeUsedByAnotherStudentOrTenant()throws Exception{
        var o=org();var s=student(true);var other=student(true);publish(s,o,"read");
        var inbox=call("GET","/api/me/notifications",s.token,null,200);String id=inbox.get("items").get(0).get("id").asText();
        call("PATCH","/api/me/notifications/"+id+"/read",other.token,Map.of(),404);
        call("GET","/api/me/notifications",o.token,null,403);
        call("PATCH","/api/me/notifications/"+id+"/read",s.token,Map.of(),204);
        call("PATCH","/api/me/notifications/"+id+"/read",s.token,Map.of(),204);
        assertEquals(0,call("GET","/api/me/notifications?unreadOnly=true",s.token,null,200).get("total").asInt());
        publish(s,o,"read2");call("POST","/api/me/notifications/read-all",other.token,Map.of(),204);
        assertEquals(1,call("GET","/api/me/notifications",s.token,null,200).get("unread").asInt());
        call("POST","/api/me/notifications/read-all",s.token,Map.of(),204);
        assertEquals(0,call("GET","/api/me/notifications",s.token,null,200).get("unread").asInt());
    }
    @Test void changingTheEmailCancelsAcademicDeliveryToThePreviousAddress()throws Exception{
        var o=org();var s=student(true);publish(s,o,"email");
        UUID id=jdbc.queryForObject("select id from mail_outbox where student_id=? and kind='ACADEMIC'",UUID.class,s.id);
        var delivery=outbox.claim(id).orElseThrow();assertTrue(outbox.valid(delivery));
        jdbc.update("update student_accounts set email=? where id=?",UUID.randomUUID()+"@example.test",s.id);
        assertFalse(outbox.valid(delivery));
    }
    @Test void actualIssueReplayAndRevokeProduceExactlyOneNotificationEach()throws Exception{
        var o=org();var s=student(true);link(o,s);var c=draft(o,s,"Prueba de avisos");String id=c.get("id").asText();
        call("POST","/api/credentials/"+id+"/generate-document",o.token,Map.of(),200);
        for(int i=0;i<2;i++)mvc.perform(patch("/api/credentials/"+id+"/issue").header("Authorization","Bearer "+o.token).header("Idempotency-Key","report-notification-key")).andExpect(status().isOk());
        call("PATCH","/api/credentials/"+id+"/revoke",o.token,Map.of("reason","Revocación para prueba"),200);
        var inbox=call("GET","/api/me/notifications",s.token,null,200);assertEquals(2,inbox.get("total").asInt());
    }
    @Test void reportsStayWithinTenantAndExportSafeReadableFiles()throws Exception{
        var a=org();var b=org();var s=student(true);link(a,s);link(b,s);
        issue(a,s,"=HYPERLINK(\"https://example.test\")");draft(a,s,"Borrador A");issue(b,s,"Documento privado B");
        var stats=call("GET","/api/institution/reports/summary",a.token,null,200);
        assertEquals(2,stats.get("credentials").asInt());assertEquals(1,stats.get("issued").asInt());assertEquals(1,stats.get("drafts").asInt());assertEquals(1,stats.get("linkedStudents").asInt());
        var rows=call("GET","/api/institution/reports/credentials?status=ISSUED&type=CERTIFICADO",a.token,null,200);assertEquals(1,rows.get("total").asInt());
        var xlsx=mvc.perform(get("/api/institution/reports/credentials.xlsx?status=ISSUED").header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store"))).andReturn().getResponse().getContentAsByteArray();
        try(var book=new XSSFWorkbook(new java.io.ByteArrayInputStream(xlsx))){var cell=book.getSheetAt(0).getRow(5).getCell(2);assertEquals(CellType.STRING,cell.getCellType());assertTrue(cell.getStringCellValue().startsWith("=HYPERLINK"));}
        var pdf=mvc.perform(get("/api/institution/reports/credentials.pdf").header("Authorization","Bearer "+a.token)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        var reader=new com.lowagie.text.pdf.PdfReader(pdf);
        try{String text=new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1);assertTrue(text.contains("Borrador A"));assertFalse(text.contains("Documento privado B"));}finally{reader.close();}
        assertEquals(2,jdbc.queryForObject("select count(*) from audit_events where tenant_id=? and action='REPORT_EXPORTED'",Long.class,a.id));
        call("GET","/api/institution/reports/credentials",s.token,null,403);
        call("GET","/api/institution/reports/summary?year=3000",a.token,null,400);
        call("GET","/api/platform/statistics",a.token,null,403);
    }
    @Test void importsPreviewApplyAndReplayWithoutCreatingStudentIdentities()throws Exception{
        var a=org();var b=org();var s=student(true);link(a,s);
        var program=call("POST","/api/institution/academic/programs",a.token,Map.of("name","Curso de importación","kind","Curso","period","2026","active",true),201);
        var row=Map.of("studentId",s.id,"programId",program.get("id").asText(),"registrationCode","IMPORT-001","enrolledOn","2026-01-01");
        long before=jdbc.queryForObject("select count(*) from student_accounts",Long.class);
        var preview=call("POST","/api/institution/academic/imports/preview",a.token,Map.of("rows",List.of(row)),200);
        String batch=preview.get("batchId").asText();
        assertEquals(0,preview.get("problems").size());
        assertEquals(0,jdbc.queryForObject("select count(*) from academic_records where tenant_id=?",Long.class,a.id));
        call("POST","/api/institution/academic/imports/"+batch+"/apply",b.token,Map.of(),404);
        var first=call("POST","/api/institution/academic/imports/"+batch+"/apply",a.token,Map.of(),200);
        assertEquals(first,call("POST","/api/institution/academic/imports/"+batch+"/apply",a.token,Map.of(),200));
        assertEquals(1,jdbc.queryForObject("select count(*) from academic_records where tenant_id=?",Long.class,a.id));
        assertEquals(before,jdbc.queryForObject("select count(*) from student_accounts",Long.class));
        var duplicate=call("POST","/api/institution/academic/imports/preview",a.token,Map.of("rows",List.of(row,row)),200);
        assertTrue(duplicate.get("batchId").isNull());assertTrue(duplicate.get("problems").size()>=2);
    }
    @Test void importRevalidatesConsentAndRejectsTheWholeBatch()throws Exception{
        var a=org();var s=student(true);var other=student(true);link(a,s);link(a,other);
        var program=call("POST","/api/institution/academic/programs",a.token,Map.of("name","Curso de importación","kind","Curso","period","2026","active",true),201);
        var rows=List.of(Map.of("studentId",s.id,"programId",program.get("id").asText(),"registrationCode","IMPORT-001","enrolledOn","2026-01-01"),
                Map.of("studentId",other.id,"programId",program.get("id").asText(),"registrationCode","IMPORT-002","enrolledOn","2026-01-01"));
        var p=call("POST","/api/institution/academic/imports/preview",a.token,Map.of("rows",rows),200);
        jdbc.update("update student_consents set revoked_at=current_timestamp where student_id=? and tenant_id=?",other.id,a.id);
        call("POST","/api/institution/academic/imports/"+p.get("batchId").asText()+"/apply",a.token,Map.of(),404);
        assertEquals(0,jdbc.queryForObject("select count(*) from academic_records where tenant_id=?",Long.class,a.id));
        call("POST","/api/institution/academic/imports/preview",s.token,Map.of("rows",rows),403);
    }
    @Test void dashboardUsesTenantAggregatesAndOnlyFiveRecentDocuments()throws Exception{
        var a=org();var b=org();var s=student(true);link(a,s);link(b,s);
        for(int i=0;i<6;i++)draft(a,s,"Borrador "+i);
        issue(a,s,"Documento emitido");draft(b,s,"Documento ajeno");
        var snapshot=call("GET","/api/institution/dashboard",a.token,null,200);
        assertEquals(7,snapshot.get("credentials").asInt());assertEquals(1,snapshot.get("linkedStudents").asInt());
        assertEquals(1,snapshot.get("issued").asInt());assertEquals(6,snapshot.get("drafts").asInt());
        assertEquals(5,snapshot.get("recent").size());assertFalse(snapshot.toString().contains("Documento ajeno"));
        call("GET","/api/institution/dashboard",s.token,null,403);
        var role=call("POST","/api/access/roles",a.token,Map.of("name","Panel de documentos","permissions",List.of("ORGANIZATION_VIEW","CREDENTIAL_VIEW")),201);
        call("POST","/api/access/users",a.token,Map.of("fullName","Consulta panel","email","dashboard@example.test","password",PASSWORD,"roleIds",List.of(role.get("id").asText())),201);
        String token=call("POST","/api/auth/login","",Map.of("organizationSlug",a.slug,"email","dashboard@example.test","password",PASSWORD),200).get("token").asText();
        var limited=call("GET","/api/institution/dashboard",token,null,200);
        assertTrue(limited.get("linkedStudents").isNull());assertEquals(7,limited.get("credentials").asInt());
        call("GET","/api/institution/reports/summary",token,null,403);
    }
    @Test void globalStatisticsReturnAggregatesWithoutPrivateExpedients()throws Exception{
        var response=mvc.perform(get("/api/platform/statistics").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("root@example.test").roles("SUPER_ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse();
        var body=json.readTree(response.getContentAsString());
        assertEquals(jdbc.queryForObject("select count(*) from tenants",Long.class).longValue(),body.get("organizations").asLong());
        assertTrue(body.get("students").isNumber());assertTrue(body.get("credentials").isNumber());
        assertFalse(body.has("items"));assertFalse(body.has("email"));assertFalse(body.has("documentNumber"));
    }
    @Test void customRoleNeedsExplicitReportPermission()throws Exception{
        var o=org();
        var role=call("POST","/api/access/roles",o.token,Map.of("name","Solo consulta","permissions",List.of("ORGANIZATION_VIEW")),201);
        call("POST","/api/access/users",o.token,Map.of("fullName","Usuario restringido","email","reader@example.test","password",PASSWORD,"roleIds",List.of(role.get("id").asText())),201);
        String token=call("POST","/api/auth/login","",Map.of("organizationSlug",o.slug,"email","reader@example.test","password",PASSWORD),200).get("token").asText();
        call("GET","/api/institution/reports/summary",token,null,403);
        call("GET","/api/institution/reports/credentials.xlsx",token,null,403);
    }
}
