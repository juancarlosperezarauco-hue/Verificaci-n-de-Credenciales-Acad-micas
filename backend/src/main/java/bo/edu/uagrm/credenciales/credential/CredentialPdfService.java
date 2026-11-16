package bo.edu.uagrm.credenciales.credential;

import bo.edu.uagrm.credenciales.storage.DocumentStoragePort;
import bo.edu.uagrm.credenciales.template.*;
import bo.edu.uagrm.credenciales.tenant.Tenant;

import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.imageio.ImageIO;

@Service
public class CredentialPdfService {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/La_Paz"));
    private final DocumentStoragePort storage;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public CredentialPdfService(DocumentStoragePort storage) {
        this.storage = storage;
    }

    public byte[] generate(Credential credential) {
        return generate(credential, null, credential.getTitle(), false);
    }

    public byte[] generate(
            Credential c, DocumentTemplate template, String program, boolean preview) {
        Tenant t = c.getTenant();
        Color primary = Color.decode(t.getPrimaryColor()),
                accent = Color.decode(t.getAccentColor());
        var page =
                template != null && template.getOrientation().equals("PORTRAIT")
                        ? PageSize.A4
                        : PageSize.A4.rotate();
        var output = new ByteArrayOutputStream();
        var document = new Document(page, 36, 36, 30, 30);
        var writer = PdfWriter.getInstance(document, output);
        document.open();
        var values =
                Map.of(
                        "studentName",
                        c.getHolderName(),
                        "institutionName",
                        t.getName(),
                        "programName",
                        program,
                        "documentTitle",
                        c.getTitle(),
                        "issueDate",
                        DATE.format(c.getCreatedAt()),
                        "verificationCode",
                        c.getVerificationCode());
        var frame = new PdfPTable(1);
        frame.setTotalWidth(page.getWidth() - 72);
        frame.setLockedWidth(true);
        var body = new PdfPCell();
        body.setPadding(20);
        body.setBorderWidth(2);
        body.setBorderColor(primary);
        if (t.getLogoObjectKey() != null)
            try {
                var logo = Image.getInstance(storage.load(t.getLogoObjectKey()));
                logo.scaleToFit(80, 45);
                logo.setAlignment(Image.ALIGN_CENTER);
                body.addElement(logo);
            } catch (Exception ignored) {
            }
        body.addElement(center(t.getName(), 18, Font.BOLD, primary, 4));
        body.addElement(
                center(
                        template == null
                                ? c.getCredentialType()
                                : TemplateService.render(template.getHeading(), values),
                        24,
                        Font.BOLD,
                        primary,
                        8));
        body.addElement(
                center("Se otorga el presente documento a", 12, Font.NORMAL, Color.DARK_GRAY, 8));
        body.addElement(center(c.getHolderName(), 22, Font.BOLD, accent, 7));
        body.addElement(
                center(
                        template == null
                                ? "Por " + c.getTitle()
                                : TemplateService.render(template.getBodyText(), values),
                        12,
                        Font.NORMAL,
                        Color.DARK_GRAY,
                        8));
        var footer = new PdfPTable(new float[] {1, 2, 1});
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(16);
        footer.addCell(footerCell("Fecha\n" + DATE.format(c.getCreatedAt())));
        footer.addCell(
                footerCell(
                        (t.getSignatoryName() == null ? t.getName() : t.getSignatoryName())
                                + "\n"
                                + (t.getSignatoryTitle() == null
                                        ? "Representante institucional"
                                        : t.getSignatoryTitle())));
        var qr = new PdfPCell();
        qr.setBorder(Rectangle.NO_BORDER);
        qr.setHorizontalAlignment(Element.ALIGN_CENTER);
        try {
            var matrix =
                    new QRCodeWriter()
                            .encode(
                                    frontendUrl.replaceAll("/$", "")
                                            + "/verify/"
                                            + c.getVerificationCode(),
                                    BarcodeFormat.QR_CODE,
                                    180,
                                    180,
                                    Map.of(EncodeHintType.MARGIN, 2));
            var image =
                    new BufferedImage(
                            matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++)
                for (int y = 0; y < matrix.getHeight(); y++)
                    image.setRGB(x, y, matrix.get(x, y) ? 0xff000000 : 0xffffffff);
            var png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            var qrImage = Image.getInstance(png.toByteArray());
            qrImage.scaleToFit(62, 62);
            qrImage.setAlignment(Image.ALIGN_CENTER);
            qr.addElement(qrImage);
            image.flush();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el QR del PDF.");
        }
        footer.addCell(qr);
        body.addElement(footer);
        body.addElement(
                center(
                        "Código de verificación: " + c.getVerificationCode(),
                        9,
                        Font.NORMAL,
                        Color.GRAY,
                        6));
        if (preview) body.addElement(center("MUESTRA - SIN VALIDEZ", 13, Font.BOLD, Color.RED, 6));
        frame.addCell(body);
        if (frame.getTotalHeight() > page.getHeight() - 60) {
            throw new IllegalArgumentException(
                    "El contenido no cabe en una página. Reduce el texto de la plantilla y previsualiza otra vez.");
        }
        document.add(frame);
        document.close();
        return output.toByteArray();
    }

    private Paragraph center(String text, float size, int style, Color color, float before) {
        var p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA, size, style, color));
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingBefore(before);
        return p;
    }

    private PdfPCell footerCell(String text) {
        var c = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 10)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(8);
        return c;
    }
}
