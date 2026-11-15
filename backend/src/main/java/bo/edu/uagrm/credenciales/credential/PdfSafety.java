package bo.edu.uagrm.credenciales.credential;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class PdfSafety {
 public static final int MAX_BYTES=10*1024*1024;
 private static final Set<String> FORBIDDEN=Set.of("JavaScript","JS","OpenAction","AA","EmbeddedFiles","EF","XFA","RichMedia","Launch","SubmitForm","ImportData","GoToR","Rendition");
 public void validate(byte[] bytes){
  if(bytes==null||bytes.length<8||bytes.length>MAX_BYTES||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F')throw new IllegalArgumentException("Selecciona un PDF real de hasta 10 MiB.");
  try(var reader=new PdfReader(bytes)){
   if(reader.isEncrypted()||reader.getNumberOfPages()<1||reader.getNumberOfPages()>30||reader.getXrefSize()>50000)throw new IllegalArgumentException("PDF cifrado, demasiado grande o con más de 30 páginas.");
   for(int i=0;i<reader.getXrefSize();i++)inspect(reader.getPdfObject(i),0);
  }catch(IllegalArgumentException bad){throw bad;}catch(Exception invalid){throw new IllegalArgumentException("No se pudo validar el PDF. Comprueba el archivo.");}
 }
 private void inspect(PdfObject object,int depth){
  if(depth>20)throw new IllegalArgumentException("La estructura del PDF es demasiado compleja.");
  if(object==null||object.isIndirect())return;
  if(object instanceof PdfDictionary dict){for(PdfName key:dict.getKeys()){String name=PdfName.decodeName(key.toString());if(FORBIDDEN.contains(name))throw new IllegalArgumentException("El PDF contiene acciones o adjuntos no permitidos.");inspect(dict.get(key),depth+1);}var action=dict.getAsName(PdfName.S);if(action!=null&&Set.of("JavaScript","Launch","SubmitForm","ImportData","GoToR","URI").contains(PdfName.decodeName(action.toString())))throw new IllegalArgumentException("El PDF contiene enlaces o acciones externas no permitidos.");}
  else if(object instanceof PdfArray array){for(int i=0;i<array.size();i++)inspect(array.getPdfObject(i),depth+1);}
 }
}
