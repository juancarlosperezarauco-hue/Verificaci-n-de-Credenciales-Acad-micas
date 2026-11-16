package bo.edu.uagrm.credenciales.audit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;

public class RequestCorrelationFilter extends OncePerRequestFilter {
 static final String ATTRIBUTE="audit.correlation";
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
  UUID id=UUID.randomUUID();request.setAttribute(ATTRIBUTE,id);response.setHeader("X-Request-ID",id.toString());chain.doFilter(request,response);
 }
}
