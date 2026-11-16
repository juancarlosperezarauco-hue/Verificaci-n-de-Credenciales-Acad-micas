package bo.edu.uagrm.credenciales.security;

import com.fasterxml.jackson.databind.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> LOGIN=Set.of("/api/auth/login","/api/auth/platform/login","/api/auth/student/login");
    private static final Set<String> REGISTER=Set.of("/api/auth/register","/api/auth/organizations/register","/api/auth/student/register");
    private final AuthRateLimiter limiter;private final ObjectMapper json;
    public AuthRateLimitFilter(AuthRateLimiter limiter,ObjectMapper json){this.limiter=limiter;this.json=json;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException {
        String path=request.getRequestURI().substring(request.getContextPath().length());
        if(path.startsWith("/api/public/credentials/")&&limiter.enabled()){
            var budget=limiter.take("public-document:"+request.getMethod(),request.getRemoteAddr(),request.getMethod().equals("POST")?10:60,request.getMethod().equals("POST")?300:60);
            if(!budget.allowed()){deny(response,budget.retryAfter());return;}chain.doFilter(request,response);return;
        }
        if(path.equals("/api/me/identity-verifications")&&request.getMethod().equals("POST")&&limiter.enabled()){
            var authentication=SecurityContextHolder.getContext().getAuthentication();
            if(authentication!=null&&authentication.getPrincipal() instanceof UserPrincipal p&&p.role()==bo.edu.uagrm.credenciales.user.UserRole.STUDENT){
                var upload=limiter.take("identity-upload",p.userId().toString(),6,3600);
                if(!upload.allowed()){deny(response,upload.retryAfter());return;}
                var source=limiter.take("identity-upload-ip",request.getRemoteAddr(),30,60);
                if(!source.allowed()){deny(response,source.retryAfter());return;}
            }
            // Multipart must reach Spring intact; never buffer it as the JSON security bodies below.
            chain.doFilter(request,response);return;
        }
        boolean reset=path.equals("/api/auth/student/password-reset/request");
        if(limiter.enabled()&&request.getMethod().equals("POST")&&request.getContentType()!=null&&request.getContentType().startsWith("multipart/")){
            var authentication=SecurityContextHolder.getContext().getAuthentication();String owner=authentication!=null&&authentication.getPrincipal() instanceof UserPrincipal p?p.userId().toString():request.getRemoteAddr();
            var budget=limiter.take("private-upload",owner,20,3600);if(!budget.allowed()){deny(response,budget.retryAfter());return;}chain.doFilter(request,response);return;
        }
        boolean sensitive=LOGIN.contains(path)||REGISTER.contains(path)||path.startsWith("/api/auth/mfa/")
                ||path.startsWith("/api/security/mfa")||path.startsWith("/api/auth/student/password-reset/")
                ||path.equals("/api/auth/student/verify-email")||path.equals("/api/auth/student/email-change/confirm")
                ||path.startsWith("/api/platform/security/")||path.startsWith("/api/me/security/")||path.startsWith("/api/institution/signing-key")||path.startsWith("/api/platform/storage/")||path.startsWith("/api/platform/identity-disputes/")||path.startsWith("/api/me/credential-requests")||path.startsWith("/api/me/credentials/");
        if(!limiter.enabled()||!Set.of("POST","DELETE").contains(request.getMethod())||!sensitive){chain.doFilter(request,response);return;}
        var ip=limiter.take("ip",request.getRemoteAddr(),120,60);
        if(!ip.allowed()){deny(response,ip.retryAfter());return;}
        if(request.getContentLengthLong()>8192){response.sendError(413);return;}
        byte[] body=request.getInputStream().readNBytes(8193);
        if(body.length>8192){response.sendError(413);return;}
        JsonNode data;
        try{data=json.readTree(body);}catch(IOException invalid){data=null;}
        String email=data==null?"":data.path("email").asText("").strip().toLowerCase(Locale.ROOT);
        String slug=data==null?"":data.path("organizationSlug").asText("").strip().toLowerCase(Locale.ROOT);
        var auth=SecurityContextHolder.getContext().getAuthentication();
        String owner=auth!=null&&auth.getPrincipal() instanceof UserPrincipal p?p.userId().toString():request.getRemoteAddr();
        AuthRateLimiter.Decision account;
        if(LOGIN.contains(path))account=limiter.take(path,slug+":"+email,10,900);
        else if(reset)account=limiter.take("reset",email,3,900);
        else if(REGISTER.contains(path))account=limiter.take("register",request.getRemoteAddr(),30,900);
        else account=limiter.take("code:"+path,owner,10,300);
        if(!account.allowed()){
            if(reset)response.setStatus(204);else deny(response,account.retryAfter());
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public ServletInputStream getInputStream(){ByteArrayInputStream input=new ByteArrayInputStream(body);return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};}
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
        },response);
    }
    private void deny(HttpServletResponse response,long seconds)throws IOException{response.setStatus(429);response.setHeader("Retry-After",Long.toString(seconds));response.setContentType("application/json;charset=UTF-8");response.getWriter().write("{\"message\":\"Demasiados intentos. Intenta nuevamente más tarde.\"}");}
}
