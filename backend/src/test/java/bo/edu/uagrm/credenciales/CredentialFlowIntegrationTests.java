package bo.edu.uagrm.credenciales;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CredentialFlowIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean DocumentStoragePort storage;

    @Test
    void registerUploadIssueAndPubliclyVerifyCredential() throws Exception {
        Map<String, byte[]> files = new ConcurrentHashMap<>();
        when(storage.store(any(), any(), any())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            files.put(key, invocation.getArgument(1));
            return key;
        });
        when(storage.load(any())).thenAnswer(invocation -> files.get(invocation.<String>getArgument(0)));

        String registerJson = """
                {"organizationName":"Universidad Integración","organizationSlug":"integration-university","institutionType":"UNIVERSIDAD",
                 "email":"admin@integration.edu","password":"StrongPassword123!"}
                """;
        String registerResponse = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        mvc.perform(post("/api/portal/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"itemType":"ANNOUNCEMENT","title":"Inicio de inscripciones",
                                 "description":"La convocatoria ya está disponible.","priority":5}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Inicio de inscripciones"));
        mvc.perform(get("/api/portal/items").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].itemType").value("ANNOUNCEMENT"));

        mvc.perform(patch("/api/institution/profile").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"primaryColor":"#123456","secondaryColor":"#345678","accentColor":"#9A34BC",
                                 "brandingStyle":"CLASICO","signatoryName":"Dra. Rectora","signatoryTitle":"Rectora"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brandingStyle").value("CLASICO"));

        byte[] logoBytes = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");
        MockMultipartFile logo = new MockMultipartFile("file", "escudo.png", "image/png", logoBytes);
        mvc.perform(multipart("/api/institution/profile/logo").file(logo)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.logoName").value("escudo.png"));
        MockMultipartFile backing = new MockMultipartFile("file", "resolucion.pdf",
                MediaType.APPLICATION_PDF_VALUE, "%PDF-respaldo".getBytes());
        mvc.perform(multipart("/api/institution/profile/verification-document").file(backing)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationDocumentName").value("resolucion.pdf"))
                .andExpect(jsonPath("$.verificationDocumentHash").isNotEmpty());

        String studentJson = """
                {"fullName":"María Pérez","country":"BO","documentType":"CI","documentNumber":"1234567",
                 "email":"maria@example.com","password":"StrongPassword123!"}
                """;
        String studentResponse = mvc.perform(post("/api/auth/student/register")
                        .contentType(MediaType.APPLICATION_JSON).content(studentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn().getResponse().getContentAsString();
        String studentToken = objectMapper.readTree(studentResponse).get("token").asText();
        mvc.perform(post("/api/portal/items").header("Authorization", "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemType\":\"ANNOUNCEMENT\",\"title\":\"No permitido\",\"priority\":0}"))
                .andExpect(status().isForbidden());
        String profile = mvc.perform(get("/api/me/profile").header("Authorization","Bearer "+studentToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String studentId = objectMapper.readTree(profile).get("id").asText();
        String grant = mvc.perform(post("/api/me/consents").header("Authorization","Bearer "+studentToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"institutionSlug\":\"integration-university\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String code = objectMapper.readTree(grant).get("code").asText();
        mvc.perform(post("/api/students/redeem").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\""+code+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("María Pérez"))
                .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());

        String createJson = "{\"studentIds\":[\"" + studentId
                + "\"],\"credentialType\":\"DIPLOMA\",\"title\":\"Ingeniería de Sistemas\"}";
        String createResponse = mvc.perform(post("/api/credentials/batch")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get(0).get("id").asText();

        String generatedResponse = mvc.perform(post("/api/credentials/{id}/generate-document", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentName").value(org.hamcrest.Matchers.startsWith("diploma-")))
                .andExpect(jsonPath("$.documentHash").isNotEmpty())
                .andExpect(jsonPath("$.verificationCode").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String preparedCode = objectMapper.readTree(generatedResponse).get("verificationCode").asText();
        mvc.perform(get("/api/public/credentials/{code}", preparedCode)).andExpect(status().isBadRequest());
        byte[] generatedPdf = mvc.perform(get("/api/credentials/{id}/document", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        org.junit.jupiter.api.Assertions.assertEquals("%PDF", new String(generatedPdf, 0, 4));

        String issueResponse = mvc.perform(patch("/api/credentials/{id}/issue", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode issued = objectMapper.readTree(issueResponse);

        mvc.perform(get("/api/public/credentials/{code}", issued.get("verificationCode").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization").value("Universidad Integración"))
                .andExpect(jsonPath("$.institutionLogoUrl").isNotEmpty())
                .andExpect(jsonPath("$.primaryColor").value("#123456"))
                .andExpect(jsonPath("$.holderName").value("María Pérez"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
        mvc.perform(get("/api/me/credentials").header("Authorization","Bearer "+studentToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].organization").value("Universidad Integración"));
    }
}
