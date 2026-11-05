package bo.edu.uagrm.credenciales.consent;
import bo.edu.uagrm.credenciales.identity.*;
import bo.edu.uagrm.credenciales.tenant.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsentService {
    private final ConsentRepository consents;
    private final IdentityService identities;
    private final TenantRepository tenants;
    public ConsentService(ConsentRepository consents, IdentityService identities, TenantRepository tenants) {
        this.consents=consents; this.identities=identities; this.tenants=tenants;
    }
    public record Access(UUID id,String institutionName,String institutionSlug,Instant expiresAt,
                         Instant usedAt,Instant revokedAt) {}
    public record Generated(String code,Access access) {}
    private Access view(StudentConsent c) {
        return new Access(c.getId(),c.getTenant().getName(),c.getTenant().getSlug(),
            c.getExpiresAt(),c.getUsedAt(),c.getRevokedAt());
    }
    @Transactional
    public Generated generate(UUID studentId,String slug) {
        Tenant tenant=tenants.findBySlug(slug.strip().toLowerCase(Locale.ROOT)).filter(Tenant::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Institución no encontrada. Solicita su identificador de acceso."));
        String code=UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT);
        StudentConsent c=consents.save(new StudentConsent(identities.requireComplete(studentId),tenant,hash(code),Instant.now()));
        return new Generated(code,view(c));
    }
    @Transactional
    public IdentityDtos.Profile redeem(UUID tenantId,UUID userId,String code) {
        if (code.strip().toUpperCase(Locale.ROOT).startsWith("EST-")) {
            return registerBySearchCode(tenantId, userId, code);
        }
        StudentConsent c=consents.findByTokenHashAndTenantId(hash(code.strip().toUpperCase(Locale.ROOT)),tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Código inválido, vencido o no destinado a tu institución."));
        if(!c.redeemable(Instant.now())) throw new IllegalArgumentException("Código inválido, vencido o ya utilizado.");
        c.redeem(userId,Instant.now());
        return IdentityDtos.Profile.from(c.getStudent());
    }
    private IdentityDtos.Profile registerBySearchCode(UUID tenantId, UUID userId, String code) {
        // Lock the account to serialize code rotation and concurrent institutional registrations.
        StudentAccount student = identities.findBySearchCode(code);
        String linkHash = hash(student.getSearchCode() + ":" + tenantId);
        var existing = consents.findByTokenHashAndTenantId(linkHash, tenantId);
        if (existing.isPresent()) {
            if (existing.get().getRevokedAt() != null) {
                throw new IllegalArgumentException("El estudiante retiró esta autorización. Solicita un código nuevo.");
            }
            return IdentityDtos.Profile.from(student);
        }
        Tenant tenant = tenants.findById(tenantId).filter(Tenant::isActive)
            .orElseThrow(() -> new IllegalArgumentException("Institución no disponible."));
        StudentConsent consent = new StudentConsent(student, tenant, linkHash, Instant.now());
        consent.redeem(userId, Instant.now());
        consents.save(consent);
        return IdentityDtos.Profile.from(student);
    }
    @Transactional(readOnly=true)
    public List<Access> history(UUID studentId) {
        return consents.findAllByStudentIdOrderByCreatedAtDesc(studentId).stream().map(this::view).toList();
    }
    @Transactional
    public void revoke(UUID studentId,UUID id) {
        StudentConsent c=consents.findByIdAndStudentId(id,studentId)
            .orElseThrow(() -> new IllegalArgumentException("Autorización no encontrada"));
        c.revoke(Instant.now());
    }
    @Transactional(readOnly=true)
    public List<IdentityDtos.Profile> linked(UUID tenantId) {
        return consents.findAllByTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(tenantId).stream()
            .map(c -> IdentityDtos.Profile.from(c.getStudent())).distinct()
            .sorted(Comparator.comparing(IdentityDtos.Profile::fullName)).toList();
    }
    @Transactional(readOnly=true)
    public StudentAccount authorizedStudent(UUID tenantId,UUID studentId) {
        if(!consents.existsByStudentIdAndTenantIdAndUsedAtIsNotNullAndRevokedAtIsNull(studentId,tenantId))
            throw new IllegalArgumentException("El estudiante no ha autorizado a esta institución");
        return identities.require(studentId);
    }
    private String hash(String code) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
