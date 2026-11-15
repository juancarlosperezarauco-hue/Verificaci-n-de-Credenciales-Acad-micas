package bo.edu.uagrm.credenciales.security;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
class SecurityMaintenanceJob {
 private final SecurityMaintenance service;private final boolean enabled;
 SecurityMaintenanceJob(SecurityMaintenance service,@Value("${app.security.maintenance-enabled:true}") boolean enabled){this.service=service;this.enabled=enabled;}
 @Scheduled(fixedDelay=3600000,initialDelay=60000)
 public void clean(){if(enabled)service.cleanup();}
}
