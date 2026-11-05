package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.tenant.BrandingStyle;
import bo.edu.uagrm.credenciales.tenant.Tenant;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class CredentialPdfService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.of("America/La_Paz"));
    private final DocumentStoragePort storage;

    public CredentialPdfService(DocumentStoragePort storage) {
        this.storage = storage;
    }

    public byte[] generate(Credential credential) {
        Tenant tenant = credential.getTenant();
        Color primary = Color.decode(tenant.getPrimaryColor());
        Color secondary = Color.decode(tenant.getSecondaryColor());
        Color accent = Color.decode(tenant.getAccentColor());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 46, 46, 42, 42);
        PdfWriter.getInstance(document, output);
        document.open();

        PdfPTable frame = new PdfPTable(1);
        frame.setWidthPercentage(100);
        PdfPCell body = new PdfPCell();
        body.setPadding(tenant.getBrandingStyle() == BrandingStyle.MINIMALISTA ? 28 : 38);
        body.setBorderWidth(tenant.getBrandingStyle() == BrandingStyle.CLASICO ? 5 : 2);
        body.setBorderColor(primary);
        body.setBackgroundColor(Color.WHITE);

        addLogo(body, tenant);
        body.addElement(center(tenant.getName(), 21, Font.BOLD, primary, 8));
        body.addElement(center(credential.getCredentialType(), 34, Font.BOLD, secondary, 18));
        body.addElement(center("Se otorga el presente reconocimiento a", 15, Font.NORMAL, Color.DARK_GRAY, 10));
        body.addElement(center(credential.getHolderName(), 29, Font.BOLD, accent, 12));
        body.addElement(center("por", 13, Font.NORMAL, Color.DARK_GRAY, 4));
        body.addElement(center(credential.getTitle(), 21, Font.BOLD, Color.BLACK, 20));

        PdfPTable footer = new PdfPTable(2);
        footer.setWidthPercentage(88);
        footer.setSpacingBefore(22);
        footer.addCell(footerCell("Fecha\n" + DATE.format(credential.getCreatedAt()), primary));
        String signer = tenant.getSignatoryName() == null ? tenant.getName() : tenant.getSignatoryName();
        String role = tenant.getSignatoryTitle() == null ? "Representante institucional" : tenant.getSignatoryTitle();
        footer.addCell(footerCell(signer + "\n" + role, primary));
        body.addElement(footer);
        body.addElement(center("Código de verificación: " + credential.getVerificationCode(),
                10, Font.NORMAL, Color.GRAY, 16));
        body.addElement(center("Verifique la autenticidad en la plataforma de Credenciales Académicas",
                9, Font.ITALIC, Color.GRAY, 3));
        frame.addCell(body);
        document.add(frame);
        document.close();
        return output.toByteArray();
    }

    private void addLogo(PdfPCell body, Tenant tenant) {
        if (tenant.getLogoObjectKey() == null) return;
        try {
            Image logo = Image.getInstance(storage.load(tenant.getLogoObjectKey()));
            logo.scaleToFit(110, 75);
            logo.setAlignment(Image.ALIGN_CENTER);
            body.addElement(logo);
        } catch (RuntimeException | java.io.IOException ignored) {
            // El certificado sigue siendo válido aunque el archivo de logo esté temporalmente inaccesible.
        }
    }

    private Paragraph center(String text, float size, int style, Color color, float spacingBefore) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, size, style, color);
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(spacingBefore);
        return paragraph;
    }

    private PdfPCell footerCell(String text, Color color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 11, Font.NORMAL)));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(12);
        cell.setBorderWidthTop(2);
        cell.setBorderWidthBottom(0);
        cell.setBorderWidthLeft(0);
        cell.setBorderWidthRight(0);
        cell.setBorderColorTop(color);
        return cell;
    }
}
