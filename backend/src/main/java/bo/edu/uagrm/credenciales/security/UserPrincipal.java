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
        boolean enabled) implements UserDetails {

    public static UserPrincipal from(AppUser user) {
        return new UserPrincipal(user.getId(), user.getTenant().getId(), user.getEmail(),
                user.getPasswordHash(), user.getRole(), user.isActive() && user.getTenant().isActive());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return enabled; }
    @Override public boolean isAccountNonLocked() { return enabled; }
    @Override public boolean isCredentialsNonExpired() { return enabled; }
    @Override public boolean isEnabled() { return enabled; }
}

