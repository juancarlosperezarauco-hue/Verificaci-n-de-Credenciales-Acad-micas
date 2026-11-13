package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import bo.edu.uagrm.credenciales.identity.StudentAccountRepository;
import com.fasterxml.jackson.databind.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class StudentSecurityIntegrationTests {
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired JdbcTemplate jdbc;
    @Autowired StudentAccountRepository accounts;
    @Autowired org.springframework.context.ApplicationContext context;
    @MockitoBean JavaMailSender mail;
    private final BlockingQueue<SimpleMailMessage> messages=new LinkedBlockingQueue<>();
    private static final String PASSWORD="SecurityPassword123!", NEW_PASSWORD="NewSecurityPassword456!";
    record Account(String email,String token) {}

    @BeforeEach void mailbox() {
        doAnswer(call -> {messages.add(new SimpleMailMessage(call.getArgument(0,SimpleMailMessage.class)));return null;})
                .when(mail).send(any(SimpleMailMessage.class));
    }
    private Account register() throws Exception {
        String email=UUID.randomUUID()+"@student.test";
        return new Account(email,postJson("/api/auth/student/register","",Map.of("email",email,"password",PASSWORD),201).get("token").asText());
    }
    private JsonNode postJson(String path,String token,Object body,int code) throws Exception {
        String response=mvc.perform(post(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return response.isBlank()?json.nullNode():json.readTree(response);
    }
    private String mailToken(String email,String subject) throws Exception {
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<until) {
            var message=messages.poll(100,TimeUnit.MILLISECONDS);
            if(message!=null&&Arrays.asList(message.getTo()).contains(email)&&message.getSubject().equals(subject)) {
                var matcher=Pattern.compile("[?&]token=([A-Za-z0-9_-]+)").matcher(message.getText());
                assertTrue(matcher.find());return matcher.group(1);
            }
        }
        throw new AssertionError("No se recibió el correo de prueba esperado");
    }
    private String resetToken(Account account) throws Exception {
        postJson("/api/auth/student/password-reset/request","",Map.of("email",account.email()),204);
        return mailToken(account.email(),"Recupera tu contraseña");
    }
    private void profile(String token,int code) throws Exception {
        mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+token)).andExpect(status().is(code));
    }
    private JsonNode login(String email,String password,int code) throws Exception {
        return postJson("/api/auth/student/login","",Map.of("email",email,"password",password),code);
    }

    @Test void registrationSendsHashedSingleUseEmailVerificationWithoutExposingToken() throws Exception {
        Account account=register();
        String raw=mailToken(account.email(),"Verifica tu correo");
        String stored=jdbc.queryForObject("select token_hash from student_account_tokens t join student_accounts s on s.id=t.student_id where s.email=?",String.class,account.email());
        assertNotEquals(raw,stored);assertEquals(64,stored.length());
        mvc.perform(get("/api/me/security").header("Authorization","Bearer "+account.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailVerified").value(false)).andExpect(jsonPath("$.token").doesNotExist());
        postJson("/api/auth/student/verify-email","",Map.of("token",raw),204);
        postJson("/api/auth/student/verify-email","",Map.of("token",raw),400);
        mvc.perform(get("/api/me/security").header("Authorization","Bearer "+account.token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test void recoveryIsUniformAndResetRevokesOldSessionsAndRejectsReplay() throws Exception {
        Account account=register();
        postJson("/api/auth/student/password-reset/request","",Map.of("email",UUID.randomUUID()+"@unknown.test"),204);
        String raw=resetToken(account);
        profile(account.token(),200);
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",raw,"newPassword",NEW_PASSWORD),204);
        profile(account.token(),403);
        login(account.email(),PASSWORD,401);
        profile(login(account.email(),NEW_PASSWORD,200).get("token").asText(),200);
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",raw,"newPassword",PASSWORD),400);
    }

    @Test void expiredOrReplacedResetTokensDoNotChangePassword() throws Exception {
        Account account=register();
        String first=resetToken(account), second=resetToken(account);
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",first,"newPassword",NEW_PASSWORD),400);
        jdbc.update("update student_account_tokens set expires_at=? where student_id=? and token_type='PASSWORD_RESET'",
                Timestamp.from(Instant.now().minusSeconds(1)),accounts.findByEmail(account.email()).orElseThrow().getId());
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",second,"newPassword",NEW_PASSWORD),400);
        login(account.email(),PASSWORD,200);profile(account.token(),200);
    }

    @Test void passwordChangeRequiresCurrentPasswordAndInvalidatesSessionsAndPendingTokens() throws Exception {
        Account account=register();String raw=resetToken(account);
        postJson("/api/me/security/password",account.token(),Map.of("currentPassword","IncorrectPassword!","newPassword",NEW_PASSWORD),401);
        postJson("/api/me/security/password",account.token(),Map.of("currentPassword",PASSWORD,"newPassword","á".repeat(40)),400);
        profile(account.token(),200);
        postJson("/api/me/security/password",account.token(),Map.of("currentPassword",PASSWORD,"newPassword",NEW_PASSWORD),204);
        profile(account.token(),403);
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",raw,"newPassword",PASSWORD),400);
        login(account.email(),NEW_PASSWORD,200);
    }

    @Test void emailChangeRequiresNewMailboxConfirmationAndKeepsSameGlobalIdentity() throws Exception {
        Account account=register();String newEmail=UUID.randomUUID()+"@new.test";
        UUID id=accounts.findByEmail(account.email()).orElseThrow().getId();
        postJson("/api/me/security/email-change",account.token(),Map.of("currentPassword",PASSWORD,"newEmail",newEmail),204);
        String raw=mailToken(newEmail,"Confirma tu nuevo correo");
        login(account.email(),PASSWORD,200);assertTrue(accounts.findByEmail(newEmail).isEmpty());
        postJson("/api/auth/student/email-change/confirm","",Map.of("token",raw),204);
        profile(account.token(),403);login(account.email(),PASSWORD,401);
        String token=login(newEmail,PASSWORD,200).get("token").asText();profile(token,200);
        assertEquals(id,accounts.findByEmail(newEmail).orElseThrow().getId());
        assertTrue(accounts.findByEmail(newEmail).orElseThrow().isEmailVerified());
        postJson("/api/auth/student/email-change/confirm","",Map.of("token",raw),400);
    }

    @Test void emailCollisionDoesNotOverwriteAnotherAccountOrConsumeAnUnrelatedSession() throws Exception {
        Account first=register(), second=register();
        postJson("/api/me/security/email-change",first.token(),Map.of("currentPassword",PASSWORD,"newEmail",second.email()),409);
        String target=UUID.randomUUID()+"@race.test";
        postJson("/api/me/security/email-change",first.token(),Map.of("currentPassword",PASSWORD,"newEmail",target),204);
        String raw=mailToken(target,"Confirma tu nuevo correo");
        postJson("/api/auth/student/register","",Map.of("email",target,"password",PASSWORD),201);
        postJson("/api/auth/student/email-change/confirm","",Map.of("token",raw),409);
        profile(first.token(),200);profile(second.token(),200);
        login(first.email(),PASSWORD,200);
    }

    @Test void deactivationIsLogicalAndBlocksLoginTokensAndRecoveryWithoutAffectingOtherAccounts() throws Exception {
        Account first=register(), second=register();String raw=resetToken(first);
        UUID id=accounts.findByEmail(first.email()).orElseThrow().getId();
        mvc.perform(delete("/api/me/security/account").header("Authorization","Bearer "+first.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("currentPassword",PASSWORD))))
                .andExpect(status().isNoContent());
        assertFalse(accounts.findById(id).orElseThrow().isActive());
        profile(first.token(),403);profile(second.token(),200);login(first.email(),PASSWORD,401);
        postJson("/api/auth/student/password-reset/request","",Map.of("email",first.email()),204);
        postJson("/api/auth/student/password-reset/confirm","",Map.of("token",raw,"newPassword",NEW_PASSWORD),400);
    }

    @Test void twoConcurrentResetRequestsCanConsumeTheTokenOnlyOnce() throws Exception {
        Account account=register();String raw=resetToken(account);
        CountDownLatch start=new CountDownLatch(1);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        Callable<Integer> action=() -> {
            assertTrue(start.await(5,TimeUnit.SECONDS));
            return mvc.perform(post("/api/auth/student/password-reset/confirm").contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(Map.of("token",raw,"newPassword",NEW_PASSWORD))))
                    .andReturn().getResponse().getStatus();
        };
        try {
            Future<Integer> first=pool.submit(action),second=pool.submit(action);start.countDown();
            var codes=new ArrayList<>(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS)));
            Collections.sort(codes);assertEquals(List.of(204,400),codes);
            profile(account.token(),403);
        } finally {start.countDown();pool.shutdownNow();assertTrue(pool.awaitTermination(5,TimeUnit.SECONDS));}
    }

    @Test void deactivationWithdrawsInstitutionAccessButPreservesIssuedCredentialAndGlobalIdentity() throws Exception {
        Account account=register();
        mvc.perform(patch("/api/me/profile").header("Authorization","Bearer "+account.token()).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("fullName","Titular Histórico","country","BO","documentType","CI",
                        "documentNumber",UUID.randomUUID().toString(),"dateOfBirth","2000-05-12","department","La Paz"))))
                .andExpect(status().isOk());
        var student=accounts.findByEmail(account.email()).orElseThrow();
        var session=postJson("/api/auth/register","",Map.of("organizationName","Institución Histórica",
                "organizationSlug","history-"+UUID.randomUUID(),"institutionType","COLEGIO","email","admin@example.test","password",PASSWORD),201);
        ApprovedOrganizationFixture.approve(session,context);
        String admin=session.get("token").asText();
        postJson("/api/students/redeem",admin,Map.of("code",student.getSearchCode()),200);
        var tenant=context.getBean(bo.edu.uagrm.credenciales.tenant.TenantRepository.class)
                .findById(UUID.fromString(session.get("tenantId").asText())).orElseThrow();
        var credential=new bo.edu.uagrm.credenciales.credential.Credential(tenant,student,"CERTIFICADO","Logro histórico");
        credential.attachDocument("fixture/document.pdf","document.pdf","application/pdf","a".repeat(64));
        String code="TEST-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        credential.issue(code,"pending:fixture");
        context.getBean(bo.edu.uagrm.credenciales.credential.CredentialRepository.class).saveAndFlush(credential);
        mvc.perform(delete("/api/me/security/account").header("Authorization","Bearer "+account.token())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("currentPassword",PASSWORD))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/students").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        postJson("/api/students/redeem",admin,Map.of("code",student.getSearchCode()),400);
        postJson("/api/credentials",admin,Map.of("studentId",student.getId(),"credentialType","CERTIFICADO","title","No autorizado"),400);
        mvc.perform(get("/api/public/credentials/{code}",code)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED")).andExpect(jsonPath("$.holderName").value("Titular Histórico"));
        assertTrue(accounts.findById(student.getId()).isPresent());
    }
}
