package bo.edu.uagrm.credenciales;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import bo.edu.uagrm.credenciales.blockchain.BlockchainAnchorPort;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CredentialDocumentIntegrityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockitoBean DocumentStoragePort storage;
    @MockitoBean BlockchainAnchorPort blockchain;

    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private static final byte[] ORIGINAL = "%PDF-original-document".getBytes(StandardCharsets.UTF_8);
    private static final byte[] REPLACEMENT = "%PDF-replacement-document".getBytes(StandardCharsets.UTF_8);
    private static final String PASSWORD = "IntegrityTestPassword123!";

    @BeforeEach
    void storageAndAnchor() {
        doAnswer(call -> {
            String key = call.getArgument(0);
            files.put(key, call.getArgument(1));
            return key;
        }).when(storage).store(any(), any(), any());
        when(storage.load(any())).thenAnswer(call -> files.get(call.<String>getArgument(0)));
        when(blockchain.anchor(any(), any(), any())).thenReturn("pending:integrity-test");
    }

    record Fixture(String token, String studentToken, String credentialId) {}

    private String organization() throws Exception {
        return postJson("/api/auth/register", "", Map.of(
                "organizationName", "Organización integridad", "organizationSlug", "integrity-" + UUID.randomUUID(),
                "institutionType", "ACADEMIA", "email", "admin@example.test", "password", PASSWORD), 201)
                .get("token").asText();
    }

    private Fixture draft() throws Exception {
        String owner = organization();
        String student = postJson("/api/auth/student/register", "", Map.of(
                "fullName", "Estudiante Integridad", "country", "BO", "documentType", "CI",
                "documentNumber", UUID.randomUUID().toString(), "dateOfBirth", "2001-05-12",
                "department", "Santa Cruz", "email", UUID.randomUUID() + "@example.test", "password", PASSWORD), 201)
                .get("token").asText();
        JsonNode profile = json.readTree(mvc.perform(get("/api/me/profile")
                .header("Authorization", "Bearer " + student)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        postJson("/api/students/redeem", owner, Map.of("code", profile.get("searchCode").asText()), 200);
        String id = postJson("/api/credentials", owner, Map.of("studentId", profile.get("id").asText(),
                "credentialType", "CERTIFICADO", "title", "Integridad documental"), 201).get("id").asText();
        upload(id, owner, ORIGINAL, 200);
        return new Fixture(owner, student, id);
    }

    private JsonNode postJson(String path, String token, Object body, int statusCode) throws Exception {
        return json.readTree(mvc.perform(post(path).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().is(statusCode)).andReturn().getResponse().getContentAsString());
    }

    private JsonNode upload(String id, String token, byte[] bytes, int statusCode) throws Exception {
        return json.readTree(mvc.perform(multipart("/api/credentials/{id}/document", id)
                .file(new MockMultipartFile("file", "certificado.pdf", "application/pdf", bytes))
                .header("Authorization", "Bearer " + token)).andExpect(status().is(statusCode))
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode issue(Fixture f, int statusCode) throws Exception {
        return json.readTree(mvc.perform(patch("/api/credentials/{id}/issue", f.credentialId())
                .header("Authorization", "Bearer " + f.token())).andExpect(status().is(statusCode))
                .andReturn().getResponse().getContentAsString());
    }

    private void originalCanBeDownloaded(Fixture f) throws Exception {
        byte[] downloaded = mvc.perform(get("/api/credentials/{id}/document", f.credentialId())
                .header("Authorization", "Bearer " + f.token())).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(ORIGINAL, downloaded, "Un rechazo no debe alterar el objeto confirmado");
    }

    @Test
    void issuedAndRevokedDocumentsRejectReplacementBeforeStorage() throws Exception {
        Fixture f = draft();
        JsonNode issued = issue(f, 200);
        clearInvocations(storage);
        upload(f.credentialId(), f.token(), REPLACEMENT, 400);
        verifyNoInteractions(storage);
        originalCanBeDownloaded(f);
        mvc.perform(patch("/api/credentials/{id}/revoke", f.credentialId())
                .header("Authorization", "Bearer " + f.token()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Prueba de integridad\"}")).andExpect(status().isOk());
        clearInvocations(storage);
        upload(f.credentialId(), f.token(), REPLACEMENT, 400);
        postJson("/api/credentials/" + f.credentialId() + "/generate-document", f.token(), Map.of(), 400);
        verifyNoInteractions(storage);
        originalCanBeDownloaded(f);
        mvc.perform(get("/api/public/credentials/{code}", issued.get("verificationCode").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REVOKED"))
                .andExpect(jsonPath("$.documentHash").value(issued.get("documentHash").asText()));
    }

    @Test
    void otherTenantStudentsAndReadersCannotReadOrReplaceDocuments() throws Exception {
        Fixture f = draft();
        String other = organization();
        String role = postJson("/api/access/roles", f.token(), Map.of("name", "Lector",
                "permissions", java.util.List.of("CREDENTIAL_VIEW")), 201).get("id").asText();
        postJson("/api/access/users", f.token(), Map.of("fullName", "Lector Prueba", "email", "reader@example.test",
                "password", PASSWORD, "roleIds", java.util.List.of(role)), 201);
        String slug = mvc.perform(get("/api/institution/profile").header("Authorization", "Bearer " + f.token()))
                .andReturn().getResponse().getContentAsString();
        String reader = postJson("/api/auth/login", "", Map.of("organizationSlug", json.readTree(slug).get("slug").asText(),
                "email", "reader@example.test", "password", PASSWORD), 200).get("token").asText();
        clearInvocations(storage);
        upload(f.credentialId(), other, REPLACEMENT, 400);
        upload(f.credentialId(), f.studentToken(), REPLACEMENT, 403);
        upload(f.credentialId(), reader, REPLACEMENT, 403);
        mvc.perform(get("/api/credentials/{id}/document", f.credentialId()).header("Authorization", "Bearer " + other))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
        originalCanBeDownloaded(f);
    }

    @Test
    void storageWriteThenFailureCannotOverwritePreviouslyCommittedDocument() throws Exception {
        Fixture f = draft();
        String originalKey = files.keySet().iterator().next();
        doAnswer(call -> {
            files.put(call.getArgument(0), call.getArgument(1));
            throw new IllegalStateException("Fallo simulado después de escribir");
        }).when(storage).store(any(), any(), any());
        upload(f.credentialId(), f.token(), REPLACEMENT, 400);
        assertEquals(2, files.size(), "La carga debe escribir un objeto nuevo incluso con el mismo nombre");
        assertArrayEquals(ORIGINAL, files.get(originalKey));
        originalCanBeDownloaded(f);
    }

    @Test
    void rejectedIssueRetryDoesNotCallExternalAnchorAgain() throws Exception {
        Fixture f = draft();
        issue(f, 200);
        issue(f, 400);
        verify(blockchain, times(1)).anchor(any(), any(), any());
    }

    @Test
    void concurrentUploadAndIssueSerializeDocumentAndHash() throws Exception {
        Fixture f = draft();
        CountDownLatch stored = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch issueStarted = new CountDownLatch(1);
        doAnswer(call -> {
            String key = call.getArgument(0);
            files.put(key, call.getArgument(1));
            stored.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Tiempo de espera de prueba");
            return key;
        }).when(storage).store(any(), any(), any());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<JsonNode> uploaded = pool.submit(() -> upload(f.credentialId(), f.token(), REPLACEMENT, 200));
            assertTrue(stored.await(10, TimeUnit.SECONDS));
            Future<MvcResult> issuance = pool.submit(() -> {
                issueStarted.countDown();
                return mvc.perform(patch("/api/credentials/{id}/issue", f.credentialId())
                        .header("Authorization", "Bearer " + f.token())).andReturn();
            });
            assertTrue(issueStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> issuance.get(500, TimeUnit.MILLISECONDS),
                    "La emisión debe esperar a que la carga confirme su hash");
            release.countDown();
            JsonNode updated = uploaded.get(10, TimeUnit.SECONDS);
            MvcResult emitted = issuance.get(10, TimeUnit.SECONDS);
            assertEquals(200, emitted.getResponse().getStatus());
            JsonNode issued = json.readTree(emitted.getResponse().getContentAsString());
            assertEquals(updated.get("documentHash"), issued.get("documentHash"));
            mvc.perform(get("/api/public/credentials/{code}", issued.get("verificationCode").asText()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ISSUED"))
                    .andExpect(jsonPath("$.documentHash").value(updated.get("documentHash").asText()));
            byte[] bytes = mvc.perform(get("/api/credentials/{id}/document", f.credentialId())
                    .header("Authorization", "Bearer " + f.token())).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            assertArrayEquals(REPLACEMENT, bytes);
        } finally {
            release.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(15, TimeUnit.SECONDS));
        }
    }
}
