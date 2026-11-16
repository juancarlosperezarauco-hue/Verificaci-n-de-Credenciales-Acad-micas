package bo.edu.uagrm.credenciales;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
class IdentityUploadRateLimitTests {
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 void authenticate(){var p=new UserPrincipal(UUID.randomUUID(),null,"test@example.test","unused",UserRole.STUDENT,true);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,p.getAuthorities()));}
 @Test void multipartIsPassedIntactAndUsesSeparateOwnerAndIpBudgets()throws Exception{
  var limiter=mock(AuthRateLimiter.class);when(limiter.enabled()).thenReturn(true);when(limiter.take(anyString(),anyString(),anyInt(),anyInt())).thenReturn(new AuthRateLimiter.Decision(true,1));authenticate();
  var request=new MockHttpServletRequest("POST","/api/me/identity-verifications");request.setContentType("multipart/form-data; boundary=test");request.setContent(new byte[20000]);var chain=new MockFilterChain();new AuthRateLimitFilter(limiter,new ObjectMapper()).doFilter(request,new MockHttpServletResponse(),chain);
  assertSame(request,chain.getRequest());assertEquals(20000,request.getContentLength());verify(limiter).take(eq("identity-upload"),anyString(),eq(6),eq(3600));verify(limiter).take(eq("identity-upload-ip"),anyString(),eq(30),eq(60));
 }
 @Test void exhaustedUploadBudgetReturns429BeforeReadingFiles()throws Exception{
  var limiter=mock(AuthRateLimiter.class);when(limiter.enabled()).thenReturn(true);when(limiter.take(anyString(),anyString(),anyInt(),anyInt())).thenReturn(new AuthRateLimiter.Decision(false,123));authenticate();
  var response=new MockHttpServletResponse();var chain=new MockFilterChain();new AuthRateLimitFilter(limiter,new ObjectMapper()).doFilter(new MockHttpServletRequest("POST","/api/me/identity-verifications"),response,chain);
  assertEquals(429,response.getStatus());assertEquals("123",response.getHeader("Retry-After"));assertNull(chain.getRequest());
 }
}
