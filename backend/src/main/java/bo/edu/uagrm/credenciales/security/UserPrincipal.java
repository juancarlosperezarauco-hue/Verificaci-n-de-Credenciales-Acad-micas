package bo.edu.uagrm.credenciales.security;

import bo.edu.uagrm.credenciales.user.AppUser;
import bo.edu.uagrm.credenciales.user.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record UserPrincipal(
        UUID userId,
        UUID tenantId,
        String email,
        String password,
        UserRole role,
        boolean enabled,
        java.util.Set<String> permissions) implements UserDetails {

    public UserPrincipal(UUID userId, UUID tenantId, String email, String password, UserRole role, boolean enabled) {
        this(userId, tenantId, email, password, role, enabled, defaults(role));
    }

    public static java.util.Set<String> defaults(UserRole role) {
        return switch (role) {
            case ADMIN, TENANT_ADMIN -> java.util.Arrays.stream(bo.edu.uagrm.credenciales.rbac.PermissionCode.values())
                    .map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet());
            case ISSUER -> java.util.Set.of("ORGANIZATION_VIEW", "STUDENT_VIEW", "STUDENT_LINK",
                    "CREDENTIAL_VIEW", "CREDENTIAL_CREATE", "CREDENTIAL_ISSUE");
            case AUDITOR -> java.util.Set.of("ORGANIZATION_VIEW", "CREDENTIAL_VIEW", "REPORT_VIEW", "AUDIT_VIEW");
            default -> java.util.Set.of();
        };
    }

    public static UserPrincipal from(AppUser user) {
        return new UserPrincipal(user.getId(), user.getTenant() == null ? null : user.getTenant().getId(), user.getEmail(),
                user.getPasswordHash(), user.getRole(), user.isActive() &&
                (user.getRole() == UserRole.SUPER_ADMIN ? user.getTenant() == null :
                    user.getTenant() != null && user.getTenant().isActive()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        java.util.Set<GrantedAuthority> authorities = new java.util.HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        return authorities;
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return enabled; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return enabled; }
    @Override public boolean isEnabled() { return enabled; }
}

