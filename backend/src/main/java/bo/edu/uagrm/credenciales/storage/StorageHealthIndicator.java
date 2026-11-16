package bo.edu.uagrm.credenciales.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;

/** Readiness probes the service without creating buckets or disclosing object names. */
@Component("objectStorage")
@ConditionalOnProperty(name = "app.storage.health-enabled", havingValue = "true")
public class StorageHealthIndicator implements HealthIndicator {
    private final URI uri;
    private final HttpClient client =
            HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

    public StorageHealthIndicator(@Value("${app.storage.endpoint}") String endpoint) {
        uri = URI.create(endpoint.replaceAll("/+$", "") + "/minio/health/ready");
    }

    @Override
    public Health health() {
        try {
            var response =
                    client.send(
                            HttpRequest.newBuilder(uri)
                                    .timeout(Duration.ofSeconds(3))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.discarding());
            return (response.statusCode() == 200 ? Health.up() : Health.down()).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Health.down().build();
        } catch (Exception e) {
            return Health.down().build();
        }
    }
}
