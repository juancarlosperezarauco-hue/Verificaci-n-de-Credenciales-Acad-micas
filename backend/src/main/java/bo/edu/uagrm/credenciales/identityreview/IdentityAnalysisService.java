package bo.edu.uagrm.credenciales.identityreview;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

@Service
public class IdentityAnalysisService {
    private final IdentityReviewService review;
    private final AuditTrail audit;
    private final RestClient client;
    private final boolean configured;
    private final Semaphore worker = new Semaphore(1);

    public IdentityAnalysisService(
            IdentityReviewService review,
            AuditTrail audit,
            @Value("${app.ai.url:http://ai-service:8000}") String url,
            @Value("${app.ai.token:}") String token,
            @Value("${app.ai.enabled:false}") boolean enabled) {
        this.review = review;
        this.audit = audit;
        URI uri = URI.create(url);
        if (enabled
                && (!Set.of("http", "https").contains(uri.getScheme())
                        || uri.getHost() == null
                        || uri.getUserInfo() != null))
            throw new IllegalArgumentException("Configura una URL válida del servicio OCR.");
        configured = enabled && token.length() >= 32;
        var factory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(25));
        client =
                RestClient.builder()
                        .baseUrl(url)
                        .requestFactory(factory)
                        .defaultHeader("X-Service-Token", token)
                        .build();
    }

    public record Signal(String code, String explanation) {}

    public record Result(
            String text,
            double confidence,
            List<Signal> signals,
            @JsonProperty("human_review_required") boolean humanReviewRequired,
            String engine) {
        @Override
        public String toString() {
            return "IdentityAnalysis[private]";
        }
    }

    public record Availability(boolean configured) {}

    public Availability availability() {
        return new Availability(configured);
    }

    @Transactional
    public Result analyze(UserPrincipal p, UUID id) {
        var item = review.detail(p, id);
        if (!configured)
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "El servicio OCR todavía no está configurado.");
        if (item.profileChanged() || !item.documentsAvailable())
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        if (!worker.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        try {
            var images =
                    List.of(
                            Base64.getEncoder()
                                    .encodeToString(review.institutionFile(p, id, "FRONT")),
                            Base64.getEncoder()
                                    .encodeToString(review.institutionFile(p, id, "BACK")));
            Result result;
            try {
                result =
                        client.post()
                                .uri("/identity-analysis")
                                .body(
                                        Map.of(
                                                "images",
                                                images,
                                                "expected_name",
                                                item.profile().fullName(),
                                                "expected_document",
                                                item.profile().documentNumber()))
                                .retrieve()
                                .body(Result.class);
            } catch (org.springframework.web.client.RestClientException failed) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "No se pudo obtener la asistencia OCR.");
            }
            if (result == null
                    || result.text() == null
                    || result.text().length() > 24001
                    || !Double.isFinite(result.confidence())
                    || result.confidence() < 0
                    || result.confidence() > 1
                    || !result.humanReviewRequired()
                    || !"tesseract".equals(result.engine())
                    || result.signals() == null
                    || result.signals().size() > 10
                    || result.signals().stream()
                            .anyMatch(s ->
                                    s == null
                                            || s.explanation() == null
                                            || s.explanation().length() > 500
                                            || !Set.of(
                                                            "LOW_OCR_CONFIDENCE",
                                                            "DOCUMENT_NOT_FOUND",
                                                            "NAME_NOT_FOUND")
                                                    .contains(s.code())))
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE, "La respuesta OCR no es válida.");
            audit.record(
                    AuditAction.IDENTITY_ANALYSIS_REQUESTED,
                    p.tenantId(),
                    AuditTrail.Resource.IDENTITY_REVIEW,
                    id);
            return result;
        } finally {
            worker.release();
        }
    }
}
