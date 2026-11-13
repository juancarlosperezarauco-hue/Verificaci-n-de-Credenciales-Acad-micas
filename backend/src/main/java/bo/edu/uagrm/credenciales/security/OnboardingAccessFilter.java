package bo.edu.uagrm.credenciales.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Deny operational routes even if they share a profile permission. */
public class OnboardingAccessFilter extends OncePerRequestFilter {
    private static final Set<String> READ = Set.of("/api/access/me", "/api/institution/status",
            "/api/institution/details", "/api/institution/profile", "/api/institution/profile/verification-document");
    private static final Set<String> POST = Set.of("/api/institution/submit-review",
            "/api/institution/profile/logo", "/api/institution/profile/verification-document");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.onboarding() && !allowed(request.getMethod(), request.getRequestURI().substring(request.getContextPath().length()))) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Tu organización debe ser aprobada antes de realizar esta operación.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allowed(String method, String path) {
        if (path.startsWith("/api/auth/")) return true;
        if (method.equals("GET") && (path.startsWith("/api/public/") || path.equals("/actuator/health"))) return true;
        return switch (method) {
            case "GET" -> READ.contains(path);
            case "PUT" -> path.equals("/api/institution/details");
            case "PATCH" -> path.equals("/api/institution/profile");
            case "POST" -> POST.contains(path);
            default -> false;
        };
    }
}
