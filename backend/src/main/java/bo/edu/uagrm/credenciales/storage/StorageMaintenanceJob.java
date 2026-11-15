package bo.edu.uagrm.credenciales.storage;
import bo.edu.uagrm.credenciales.identityreview.EvidenceLifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
/** The reference retirement commits before physical deletion starts. Do not wrap in one transaction. */
@Component
class StorageMaintenanceJob {
 private final EvidenceLifecycle lifecycle;private final StorageMaintenance storage;private final boolean enabled;
 StorageMaintenanceJob(EvidenceLifecycle l,StorageMaintenance s,@Value("${app.evidence.cleanup-enabled:false}") boolean enabled){lifecycle=l;storage=s;this.enabled=enabled;}
 @Scheduled(initialDelay=60000,fixedDelay=3600000) public void run(){if(enabled){lifecycle.purgeExpired();storage.cleanup();}}
}
