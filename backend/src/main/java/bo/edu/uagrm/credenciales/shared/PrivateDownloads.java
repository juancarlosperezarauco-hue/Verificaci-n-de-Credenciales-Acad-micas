package bo.edu.uagrm.credenciales.shared;
import bo.edu.uagrm.credenciales.credential.CredentialDtos;
import java.nio.charset.StandardCharsets;
import org.springframework.http.*;
public final class PrivateDownloads {
 private PrivateDownloads(){}
 public static ResponseEntity<byte[]> of(CredentialDtos.StoredDocument d){return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header("Referrer-Policy","no-referrer").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(d.name(),StandardCharsets.UTF_8).build().toString()).body(d.content());}
}
