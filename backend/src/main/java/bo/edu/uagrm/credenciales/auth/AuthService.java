package bo.edu.uagrm.credenciales.auth;

import bo.edu.uagrm.credenciales.security.JwtService;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import bo.edu.uagrm.credenciales.tenant.TenantRepository;
import bo.edu.uagrm.credenciales.user.AppUser;
import bo.edu.uagrm.credenciales.user.AppUserRepository;
import bo.edu.uagrm.credenciales.user.UserRole;
import java.util.Locale;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final bo.edu.uagrm.credenciales.rbac.AccessService access;
    private final bo.edu.uagrm.credenciales.platform.GovernanceRepository governance;

    public AuthService(TenantRepository tenantRepository, AppUserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       bo.edu.uagrm.credenciales.rbac.AccessService access,
                       bo.edu.uagrm.credenciales.platform.GovernanceRepository governance) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.access = access;
        this.governance = governance;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String slug = normalizeSlug(request.organizationSlug());
        if (tenantRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("La organización ya existe");
        }
        Tenant tenant = tenantRepository.save(new Tenant(request.organizationName().trim(), slug,
                request.institutionType()));
        tenant.setActive(false);
        governance.save(new bo.edu.uagrm.credenciales.platform.OrganizationGovernance(tenant.getId(),
                bo.edu.uagrm.credenciales.platform.OrganizationStatus.PENDING_REVIEW));
        AppUser user = userRepository.save(new AppUser(tenant, request.email().trim(),
                passwordEncoder.encode(request.password()), UserRole.TENANT_ADMIN));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(normalizeSlug(request.organizationSlug()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        AppUser user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.email().trim().toLowerCase(Locale.ROOT))
                .filter(AppUser::isActive)
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!access.principal(user).enabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return response(user);
    }

    private AuthDtos.AuthResponse response(AppUser user) {
        UserPrincipal principal = access.principal(user);
        return new AuthDtos.AuthResponse(jwtService.create(principal), "Bearer", user.getEmail(),
                user.getRole().name(), user.getTenant().getId().toString(), user.getTenant().getName(),
                user.getTenant().getInstitutionType());
    }

    private String normalizeSlug(String value) {
        String slug = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-").replaceAll("(^-|-$)", "");
        if (slug.length() < 3) throw new IllegalArgumentException("El identificador de organización es inválido");
        return slug;
    }
}

