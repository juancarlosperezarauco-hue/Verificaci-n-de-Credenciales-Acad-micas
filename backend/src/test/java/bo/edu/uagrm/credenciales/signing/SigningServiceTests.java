package bo.edu.uagrm.credenciales.signing;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import bo.edu.uagrm.credenciales.audit.AuditTrail;
import bo.edu.uagrm.credenciales.shared.TenantAccess;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.user.UserRole;
import bo.edu.uagrm.credenciales.tenant.*;
import bo.edu.uagrm.credenciales.identity.StudentAccount;
import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.template.TemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
class SigningServiceTests {
 record Fixture(SigningService signer,SignerRepository repository,UserPrincipal principal,Map<UUID,InstitutionalSigner> rows){}
 Fixture fixture(){var rows=new HashMap<UUID,InstitutionalSigner>();var repo=mock(SignerRepository.class);when(repo.findById(any())).thenAnswer(i->Optional.ofNullable(rows.get(i.getArgument(0))));when(repo.saveAndFlush(any())).thenAnswer(i->{InstitutionalSigner row=i.getArgument(0);rows.put(row.tenantId,row);return row;});var access=mock(TenantAccess.class);var p=new UserPrincipal(UUID.randomUUID(),UUID.randomUUID(),"demo@example.test","unused",UserRole.TENANT_ADMIN,true);when(access.require(any(),eq("SIGNING_MANAGE"))).thenReturn(p.tenantId());return new Fixture(new SigningService(repo,access,mock(AuditTrail.class),Clock.systemUTC(),new ObjectMapper(),"AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM=",true),repo,p,rows);}
 Credential credential(){var t=new Tenant("Institución de prueba","demo",InstitutionType.UNIVERSIDAD);var student=new StudentAccount("Estudiante Prueba","BO","CI","DEMO",LocalDate.of(2000,1,1),"La Paz","student@example.test","unused");var c=new Credential(t,student,"CERTIFICADO","Programa de prueba");c.prepareVerificationCode("CRED-"+UUID.randomUUID().toString().replace("-",""));return c;}
 byte[] pdf(Credential c){var service=new CredentialPdfService(mock(DocumentStoragePort.class));ReflectionTestUtils.setField(service,"frontendUrl","http://localhost:4300");return service.generate(c);}
 @Test void actualCmsSignatureValidatesAndAnUncoveredAppendIsRejected()throws Exception{var f=fixture();var status=f.signer.createDemo(f.principal,true);assertTrue(status.testOnly());assertTrue(status.required());var result=f.signer.sign(f.principal.tenantId(),pdf(credential()));var check=f.signer.verify(result.bytes(),result.fingerprint());assertEquals("VALID",check.status());assertTrue(check.coversWholeDocument());assertTrue(check.signerMatches());try(var reader=new PdfReader(result.bytes())){assertTrue(new com.lowagie.text.pdf.parser.PdfTextExtractor(reader).getTextFromPage(1).contains("FIRMA DE PRUEBA LOCAL"));}assertFalse(f.rows.get(f.principal.tenantId()).encryptedKey.contains("privateKey"));byte[] changed=Arrays.copyOf(result.bytes(),result.bytes().length+12);System.arraycopy("\n% changed\n".getBytes(),0,changed,result.bytes().length,11);assertEquals("INVALID",f.signer.verify(changed,result.fingerprint()).status());assertFalse(f.signer.verify(result.bytes(),"another-certificate").signerMatches());}
 @Test void generatedPdfContainsARealDecodableQrAndOnePage()throws Exception{var c=credential();byte[] bytes=pdf(c);new PdfSafety().validate(bytes);try(var reader=new PdfReader(bytes)){assertEquals(1,reader.getNumberOfPages());var objects=reader.getPageN(1).getAsDict(PdfName.RESOURCES).getAsDict(PdfName.XOBJECT);boolean found=false;for(var name:objects.getKeys()){var object=PdfReader.getPdfObject(objects.get(name));if(object instanceof PRStream stream&&PdfName.IMAGE.equals(stream.getAsName(PdfName.SUBTYPE))){int w=stream.getAsNumber(PdfName.WIDTH).intValue(),h=stream.getAsNumber(PdfName.HEIGHT).intValue();byte[] rgb=PdfReader.getStreamBytes(stream);int[] pixels=new int[w*h];for(int i=0;i<pixels.length;i++)pixels[i]=0xff000000|((rgb[i*3]&255)<<16)|((rgb[i*3+1]&255)<<8)|(rgb[i*3+2]&255);String value=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(w,h,pixels)))).getText();assertEquals("http://localhost:4300/verify/"+c.getVerificationCode(),value);found=true;}}assertTrue(found,"No se encontró un QR decodificable");}}
 @Test void invalidPdfAndUnsupportedTemplateVariablesAreRejected(){assertThrows(IllegalArgumentException.class,()->new PdfSafety().validate("%PDF-not-a-real-file".getBytes()));assertThrows(IllegalArgumentException.class,()->TemplateService.validateText("Documento {{arbitraryField}}"));assertThrows(IllegalArgumentException.class,()->TemplateService.validateText("{{studentName"));assertEquals("Hola Persona",TemplateService.render("Hola {{studentName}}",Map.of("studentName","Persona")));}
 @Test void pkcs12ImportChecksPasswordAndCertificateValidityWithoutOverwritingOnFailure()throws Exception{
  var f=fixture();var initial=f.signer.createDemo(f.principal,true);var row=f.rows.get(f.principal.tenantId());String secret="AwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwM=";
  var material=new ObjectMapper().readTree(new bo.edu.uagrm.credenciales.security.AeadKeyRing("legacy",secret,"").decrypt(f.principal.tenantId().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),row.encryptedKey));
  var key=java.security.KeyFactory.getInstance("RSA").generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(Base64.getDecoder().decode(material.get("privateKey").asText())));
  var cert=SigningService.certificate(row.certificateDer);var store=java.security.KeyStore.getInstance("PKCS12");store.load(null,null);store.setKeyEntry("signer",key,"test-password".toCharArray(),new java.security.cert.Certificate[]{cert});var out=new java.io.ByteArrayOutputStream();store.store(out,"test-password".toCharArray());var file=new org.springframework.mock.web.MockMultipartFile("file","test.p12","application/x-pkcs12",out.toByteArray());
  assertThrows(IllegalArgumentException.class,()->f.signer.configure(f.principal,file,"incorrect",false));assertEquals(initial.fingerprint(),f.signer.status(f.principal.tenantId()).fingerprint());assertTrue(f.signer.configure(f.principal,file,"test-password",true).testOnly());
  var access=mock(TenantAccess.class);when(access.require(any(),anyString())).thenReturn(f.principal.tenantId());var future=new SigningService(f.repository,access,mock(AuditTrail.class),Clock.fixed(Instant.now().plus(Duration.ofDays(370)),ZoneOffset.UTC),new ObjectMapper(),secret,false);assertThrows(IllegalArgumentException.class,()->future.configure(f.principal,file,"test-password",true));
 }
 @Test void activePdfContentAndExcessiveNestingAreRejected()throws Exception{
  byte[] original=pdf(credential());var out=new java.io.ByteArrayOutputStream();try(var reader=new PdfReader(original)){var stamp=new PdfStamper(reader,out);stamp.addJavaScript("app.alert('test');");stamp.close();}assertThrows(IllegalArgumentException.class,()->new PdfSafety().validate(out.toByteArray()));
 }
}
