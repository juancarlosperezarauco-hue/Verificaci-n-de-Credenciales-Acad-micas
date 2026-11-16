package bo.edu.uagrm.credenciales;
public final class TestPdfs {
 private TestPdfs(){}
 public static byte[] create(String label){var out=new java.io.ByteArrayOutputStream();var doc=new com.lowagie.text.Document();com.lowagie.text.pdf.PdfWriter.getInstance(doc,out);doc.open();doc.add(new com.lowagie.text.Paragraph(label));doc.close();return out.toByteArray();}
}
