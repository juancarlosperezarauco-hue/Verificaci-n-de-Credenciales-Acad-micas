package bo.edu.uagrm.credenciales.storage;
import bo.edu.uagrm.credenciales.identityreview.EvidenceLifecycle;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/platform/storage/maintenance") @PreAuthorize("hasRole('SUPER_ADMIN')")
public class StorageMaintenanceController {
 private final StorageMaintenance storage;private final EvidenceLifecycle lifecycle;
 public StorageMaintenanceController(StorageMaintenance s,EvidenceLifecycle l){storage=s;lifecycle=l;}
 @GetMapping StorageMaintenance.Status status(){return storage.status();}
 @PostMapping("/retire-evidence") Map<String,Integer> retire(){return Map.of("retired",lifecycle.purgeExpired());}
 @PostMapping("/cleanup") StorageMaintenance.Cleanup cleanup(){return storage.cleanup();}
 @PostMapping("/re-encrypt") Map<String,Integer> reencrypt(){return Map.of("reencrypted",storage.reencrypt());}
}
