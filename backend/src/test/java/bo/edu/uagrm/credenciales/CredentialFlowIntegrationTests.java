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
        when(storage.store(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

        String registerJson = """
                {"organizationName":"Universidad Integración","organizationSlug":"integration-university",
                 "email":"admin@integration.edu","password":"StrongPassword123!"}
                """;
        String registerResponse = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(registerJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(registerResponse).get("token").asText();

        String createJson = """
                {"holderName":"María Pérez","holderDocument":"1234567","credentialType":"DIPLOMA",
                 "title":"Ingeniería de Sistemas"}
                """;
        String createResponse = mvc.perform(post("/api/credentials")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(createResponse).get("id").asText();

        MockMultipartFile document = new MockMultipartFile(
                "file", "diploma.pdf", MediaType.APPLICATION_PDF_VALUE, "%PDF-test".getBytes());
        mvc.perform(multipart("/api/credentials/{id}/document", id)
                        .file(document).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentName").value("diploma.pdf"))
                .andExpect(jsonPath("$.documentHash").isNotEmpty());

        String issueResponse = mvc.perform(patch("/api/credentials/{id}/issue", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andReturn().getResponse().getContentAsString();
        JsonNode issued = objectMapper.readTree(issueResponse);

        mvc.perform(get("/api/public/credentials/{code}", issued.get("verificationCode").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organization").value("Universidad Integración"))
                .andExpect(jsonPath("$.holderName").value("María Pérez"))
                .andExpect(jsonPath("$.status").value("ISSUED"));
    }
}
