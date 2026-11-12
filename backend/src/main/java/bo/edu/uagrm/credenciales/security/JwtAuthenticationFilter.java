package bo.edu.uagrm.credenciales.security;

import bo.edu.uagrm.credenciales.user.AppUserRepository;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final bo.edu.uagrm.credenciales.rbac.AccessService access;
    private final bo.edu.uagrm.credenciales.identity.StudentAccountRepository accounts;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository userRepository,
            bo.edu.uagrm.credenciales.identity.StudentAccountRepository accounts,
            bo.edu.uagrm.credenciales.rbac.AccessService access) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.accounts = accounts;
        this.access = access;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.JwtIdentity identity = jwtService.verify(header.substring(7));
                (identity.tenantId() == null && !identity.userAccount()
                    ? accounts.findById(identity.userId()).map(bo.edu.uagrm.credenciales.identity.IdentityService::principal)
                    : (identity.tenantId() == null ? userRepository.findByIdAndTenantIsNull(identity.userId())
                        : userRepository.findByIdAndTenantId(identity.userId(), identity.tenantId())).map(access::principal))
                        .filter(UserPrincipal::enabled)
                        .ifPresent(principal -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())));
            } catch (JWTVerificationException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}

