package bo.edu.uagrm.credenciales.identityreview;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import bo.edu.uagrm.credenciales.audit.AuditTrail;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.user.UserRole;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;

class IdentityAnalysisServiceTests {
    HttpServer server;
    IdentityReviewService review=mock(IdentityReviewService.class);
    AuditTrail audit=mock(AuditTrail.class);
    UUID id=UUID.randomUUID(),student=UUID.randomUUID(),tenant=UUID.randomUUID();
    UserPrincipal user=new UserPrincipal(UUID.randomUUID(),tenant,"reviewer@example.test","",UserRole.CUSTOM,true,Set.of("IDENTITY_REVIEW"));
    final String token="a-disposable-test-service-key-with-32-characters";
    IdentityReviewDtos.Item item(boolean changed){
        return new IdentityReviewDtos.Item(id,student,tenant,"Instituto","PENDING","PENDING",
                new IdentityReviewDtos.Snapshot("Ana Perez","BO","CI","12345",LocalDate.of(2000,1,1),"La Paz"),
                changed,true,false,Instant.now(),null,null,null,true);
    }
    @BeforeEach void setup()throws Exception{
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.start();
        when(review.detail(user,id)).thenReturn(item(false));
        when(review.institutionFile(user,id,"FRONT")).thenReturn(new byte[]{1,2});
        when(review.institutionFile(user,id,"BACK")).thenReturn(new byte[]{3,4});
    }
    @AfterEach void stop(){server.stop(0);}
    IdentityAnalysisService service(boolean enabled){return new IdentityAnalysisService(review,audit,"http://127.0.0.1:"+server.getAddress().getPort(),token,enabled);}
    void reply(String body,int status){
        server.createContext("/identity-analysis",exchange->{
            assertEquals(token,exchange.getRequestHeaders().getFirst("X-Service-Token"));
            String input=new String(exchange.getRequestBody().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(input.contains("expected_document"));assertTrue(input.contains("12345"));
            byte[] bytes=body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");exchange.sendResponseHeaders(status,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });
    }
    @Test void authenticatedTransportReturnsAssistanceAndDoesNotSaveAnIdentityDecision(){
        reply("{\"text\":\"ANA PEREZ 12345\",\"confidence\":0.9,\"signals\":[],\"human_review_required\":true,\"engine\":\"tesseract\"}",200);
        var result=service(true).analyze(user,id);
        assertTrue(result.humanReviewRequired());assertEquals(0.9,result.confidence());
        verify(review,never()).decide(any(),any(),any());
        verify(audit).record(bo.edu.uagrm.credenciales.audit.AuditAction.IDENTITY_ANALYSIS_REQUESTED,tenant,AuditTrail.Resource.IDENTITY_REVIEW,id);
    }
    @Test void disabledIntegrationDoesNotReadImagesOrContactTheService(){
        assertEquals(503,assertThrows(ResponseStatusException.class,()->service(false).analyze(user,id)).getStatusCode().value());
        verify(review,never()).institutionFile(any(),any(),any());
    }
    @Test void staleProfileAndMissingAuthorizationAreCheckedBeforeSendingDocuments(){
        when(review.detail(user,id)).thenReturn(item(true));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service(true).analyze(user,id)).getStatusCode().value());
        verify(review,never()).institutionFile(any(),any(),any());
        when(review.detail(user,id)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->service(true).analyze(user,id)).getStatusCode().value());
    }
    @Test void providerFailureAndAutomaticIdentityVerdictsAreRejected(){
        reply("{\"text\":\"12345\",\"confidence\":1.0,\"signals\":[],\"human_review_required\":false,\"engine\":\"tesseract\"}",200);
        assertEquals(503,assertThrows(ResponseStatusException.class,()->service(true).analyze(user,id)).getStatusCode().value());
        verifyNoInteractions(audit);
    }
}
