package bo.edu.uagrm.credenciales.signing;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.identityreview.IdentityImages;
import bo.edu.uagrm.credenciales.security.*;
import bo.edu.uagrm.credenciales.shared.TenantAccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.*;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class SigningService {
    public record Status(
            boolean configured,
            boolean vaultAvailable,
            boolean demoAvailable,
            boolean required,
            boolean testOnly,
            String fingerprint,
            String subject,
            Instant expiresAt) {}

    public record Signed(byte[] bytes, String fingerprint, String certificate, boolean testOnly) {}

    public record Check(
            String status,
            boolean cryptographicValid,
            boolean coversWholeDocument,
            boolean signerMatches,
            String fingerprint,
            boolean certificateCurrentlyValid) {}

    private record Material(String privateKey, List<String> certificates) {
        @Override
        public String toString() {
            return "SigningMaterial[redacted]";
        }
    }

    private final SignerRepository signers;
    private final TenantAccess access;
    private final AuditTrail audit;
    private final Clock clock;
    private final ObjectMapper json;
    private final AeadKeyRing vault;
    private final boolean demo;

    public SigningService(
            SignerRepository signers,
            TenantAccess access,
            AuditTrail audit,
            Clock clock,
            ObjectMapper json,
            @Value("${app.signing.encryption-key:}") String secret,
            @Value("${app.signing.allow-demo:false}") boolean demo) {
        this.signers = signers;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
        this.json = json;
        this.vault = secret.isBlank() ? null : new AeadKeyRing("legacy", secret, "");
        this.demo = demo;
        if (Security.getProvider("BC") == null) Security.addProvider(new BouncyCastleProvider());
    }

    @Transactional(readOnly = true)
    public Status status(UUID tenant) {
        var s = signers.findById(tenant);
        return s.map(r ->
                        new Status(
                                true,
                                vault != null,
                                demo && vault != null,
                                r.required,
                                r.testOnly,
                                r.fingerprint,
                                r.subject,
                                r.expiresAt))
                .orElseGet(() ->
                        new Status(
                                false,
                                vault != null,
                                demo && vault != null,
                                false,
                                false,
                                null,
                                null,
                                null));
    }

    public Status configure(
            UserPrincipal p, MultipartFile file, String password, boolean required) {
        UUID tenant = access.require(p, "SIGNING_MANAGE");
        if (vault == null)
            throw new IllegalStateException("Configura la clave del almacén de firma.");
        if (file.isEmpty()
                || file.getSize() > 1024 * 1024
                || password == null
                || password.length() > 200)
            throw new IllegalArgumentException("PKCS12 inválido (máximo 1 MiB).");
        char[] chars = password.toCharArray();
        try (var input = file.getInputStream()) {
            var ks = KeyStore.getInstance("PKCS12");
            ks.load(input, chars);
            var aliases =
                    Collections.list(ks.aliases()).stream()
                            .filter(a -> {
                                try {
                                    return ks.isKeyEntry(a);
                                } catch (Exception e) {
                                    return false;
                                }
                            })
                            .toList();
            if (aliases.size() != 1)
                throw new IllegalArgumentException(
                        "El archivo debe contener una sola clave de firma.");
            var key = ks.getKey(aliases.getFirst(), chars);
            var chain = ks.getCertificateChain(aliases.getFirst());
            if (!(key instanceof RSAPrivateKey rsa)
                    || rsa.getModulus().bitLength() < 2048
                    || chain == null
                    || chain.length < 1)
                throw new IllegalArgumentException(
                        "Se requiere una clave RSA de al menos 2048 bits y su certificado.");
            var cert = (X509Certificate) chain[0];
            cert.checkValidity(Date.from(clock.instant()));
            if (cert.getBasicConstraints() >= 0
                    || cert.getKeyUsage() != null && !cert.getKeyUsage()[0])
                throw new IllegalArgumentException(
                        "El certificado no está habilitado como firmante final.");
            boolean self = cert.getSubjectX500Principal().equals(cert.getIssuerX500Principal());
            return save(
                    tenant,
                    (PrivateKey) key,
                    Arrays.stream(chain).map(c -> (X509Certificate) c).toList(),
                    required,
                    self);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "No se pudo abrir el PKCS12. Comprueba clave, certificado y contraseña.");
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    public Status createDemo(UserPrincipal p, boolean required) {
        UUID tenant = access.require(p, "SIGNING_MANAGE");
        if (!demo || vault == null)
            throw new IllegalArgumentException(
                    "La firma de prueba no está habilitada en este entorno.");
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            var pair = generator.generateKeyPair();
            var name = new X500Name("CN=PRUEBA LOCAL - " + tenant);
            var now = clock.instant();
            var builder =
                    new JcaX509v3CertificateBuilder(
                            name,
                            new BigInteger(128, new SecureRandom()).abs(),
                            Date.from(now.minusSeconds(60)),
                            Date.from(now.plus(Duration.ofDays(365))),
                            name,
                            pair.getPublic());
            builder.addExtension(
                    org.bouncycastle.asn1.x509.Extension.basicConstraints,
                    true,
                    new BasicConstraints(false));
            builder.addExtension(
                    org.bouncycastle.asn1.x509.Extension.keyUsage,
                    true,
                    new KeyUsage(KeyUsage.digitalSignature));
            var cert =
                    new JcaX509CertificateConverter()
                            .setProvider("BC")
                            .getCertificate(
                                    builder.build(
                                            new JcaContentSignerBuilder("SHA256withRSA")
                                                    .setProvider("BC")
                                                    .build(pair.getPrivate())));
            return save(tenant, pair.getPrivate(), List.of(cert), required, true);
        } catch (Exception failure) {
            throw new IllegalStateException("No se pudo crear el certificado de prueba.");
        }
    }

    public Status policy(UserPrincipal p, boolean required) {
        var row =
                signers.findById(access.require(p, "SIGNING_MANAGE"))
                        .orElseThrow(TenantAccess::missing);
        if (required && !row.expiresAt.isAfter(clock.instant()))
            throw new IllegalArgumentException("El certificado está vencido.");
        row.required = required;
        audit.record(
                AuditAction.SIGNING_KEY_CONFIGURED,
                row.tenantId,
                AuditTrail.Resource.SIGNER,
                row.tenantId);
        return status(row.tenantId);
    }

    private Status save(
            UUID tenant,
            PrivateKey key,
            List<X509Certificate> certificates,
            boolean required,
            boolean test) {
        try {
            var cert = certificates.getFirst();
            byte[] challenge = "credential-key-proof".getBytes(StandardCharsets.UTF_8);
            var signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(key);
            signer.update(challenge);
            byte[] sig = signer.sign();
            signer.initVerify(cert);
            signer.update(challenge);
            if (!signer.verify(sig))
                throw new IllegalArgumentException("La clave no corresponde al certificado.");
            var material =
                    new Material(
                            Base64.getEncoder().encodeToString(key.getEncoded()),
                            certificates.stream()
                                    .map(c -> {
                                        try {
                                            return Base64.getEncoder()
                                                    .encodeToString(c.getEncoded());
                                        } catch (Exception e) {
                                            throw new IllegalStateException();
                                        }
                                    })
                                    .toList());
            var row = signers.findById(tenant).orElseGet(InstitutionalSigner::new);
            row.tenantId = tenant;
            row.encryptedKey =
                    vault.encrypt(
                            tenant.toString().getBytes(StandardCharsets.UTF_8),
                            json.writeValueAsBytes(material));
            row.certificateDer = material.certificates().getFirst();
            row.fingerprint = IdentityImages.hash(cert.getEncoded());
            row.subject = cert.getSubjectX500Principal().getName();
            if (row.subject.length() > 500) row.subject = row.subject.substring(0, 500);
            row.expiresAt = cert.getNotAfter().toInstant();
            row.required = required;
            row.testOnly = test;
            row.configuredAt = clock.instant();
            signers.saveAndFlush(row);
            audit.record(
                    AuditAction.SIGNING_KEY_CONFIGURED, tenant, AuditTrail.Resource.SIGNER, tenant);
            return status(tenant);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo proteger la configuración de firma.");
        }
    }

    public Signed sign(UUID tenant, byte[] pdf) {
        var config =
                signers.findById(tenant)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Primero configura el certificado institucional."));
        if (vault == null || !config.expiresAt.isAfter(clock.instant()))
            throw new IllegalArgumentException(
                    "La clave no está disponible o el certificado está vencido.");
        try {
            var material =
                    json.readValue(
                            vault.decrypt(
                                    tenant.toString().getBytes(StandardCharsets.UTF_8),
                                    config.encryptedKey),
                            Material.class);
            var key =
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(
                                    new PKCS8EncodedKeySpec(
                                            Base64.getDecoder().decode(material.privateKey())));
            var chain = new ArrayList<X509Certificate>();
            for (var encoded : material.certificates()) chain.add(certificate(encoded));
            var output = new ByteArrayOutputStream();
            try (var reader = new PdfReader(pdf)) {
                var stamper = PdfStamper.createSignature(reader, output, '\0', null, true);
                if (config.testOnly) {
                    var page = reader.getPageSize(1);
                    ColumnText.showTextAligned(
                            stamper.getOverContent(1),
                            com.lowagie.text.Element.ALIGN_CENTER,
                            new com.lowagie.text.Phrase(
                                    "FIRMA DE PRUEBA LOCAL - SIN CONFIANZA DE AUTORIDAD",
                                    com.lowagie.text.FontFactory.getFont(
                                            com.lowagie.text.FontFactory.HELVETICA,
                                            8,
                                            com.lowagie.text.Font.BOLD,
                                            java.awt.Color.RED)),
                            page.getWidth() / 2,
                            14,
                            0);
                }
                var appearance = stamper.getSignatureAppearance();
                var date = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                date.setTime(Date.from(clock.instant()));
                appearance.setSignDate(date);
                appearance.setReason(
                        config.testOnly
                                ? "PRUEBA LOCAL - sin confianza de autoridad"
                                : "Emisión institucional");
                var dictionary =
                        new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
                dictionary.setDate(new PdfDate(date));
                dictionary.setReason(appearance.getReason());
                appearance.setCryptoDictionary(dictionary);
                int reserved = 32768;
                appearance.preClose(Map.of(PdfName.CONTENTS, reserved * 2 + 2));
                byte[] range;
                try (var stream = appearance.getRangeStream()) {
                    range = stream.readAllBytes();
                }
                var cms = new CMSSignedDataGenerator();
                cms.addSignerInfoGenerator(
                        new JcaSignerInfoGeneratorBuilder(
                                        new JcaDigestCalculatorProviderBuilder()
                                                .setProvider("BC")
                                                .build())
                                .build(
                                        new JcaContentSignerBuilder("SHA256withRSA")
                                                .setProvider("BC")
                                                .build(key),
                                        chain.getFirst()));
                cms.addCertificates(new JcaCertStore(chain));
                byte[] signature =
                        cms.generate(new CMSProcessableByteArray(range), false).getEncoded();
                if (signature.length > reserved)
                    throw new IllegalArgumentException(
                            "La cadena del certificado es demasiado grande.");
                byte[] padded = new byte[reserved];
                System.arraycopy(signature, 0, padded, 0, signature.length);
                var update = new PdfDictionary();
                update.put(PdfName.CONTENTS, new PdfString(padded).setHexWriting(true));
                appearance.close(update);
            }
            byte[] result = output.toByteArray();
            var check = verify(result, config.fingerprint);
            if (!check.cryptographicValid()
                    || !check.coversWholeDocument()
                    || !check.signerMatches())
                throw new IllegalStateException(
                        "La comprobación de la firma recién generada falló.");
            return new Signed(result, config.fingerprint, config.certificateDer, config.testOnly);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception failed) {
            throw new IllegalStateException("No se pudo firmar el PDF.");
        }
    }

    public Check verify(byte[] pdf, String expected) {
        try (var reader = new PdfReader(pdf)) {
            var fields = reader.getAcroFields();
            var names = fields.getSignatureNames();
            if (names.isEmpty()) return new Check("UNSIGNED", false, false, false, null, false);
            String name = names.getLast();
            var signature = fields.verifySignature(name, "BC");
            boolean valid = signature.verify(), whole = fields.signatureCoversWholeDocument(name);
            var cert = signature.getSigningCertificate();
            String fingerprint = IdentityImages.hash(cert.getEncoded());
            boolean current = true;
            try {
                cert.checkValidity(Date.from(clock.instant()));
            } catch (Exception e) {
                current = false;
            }
            boolean matches = expected != null && expected.equals(fingerprint);
            return new Check(
                    valid && whole && matches ? "VALID" : "INVALID",
                    valid,
                    whole,
                    matches,
                    fingerprint,
                    current);
        } catch (Exception failed) {
            return new Check("INVALID", false, false, false, null, false);
        }
    }

    public static X509Certificate certificate(String der) throws CertificateException {
        return (X509Certificate)
                CertificateFactory.getInstance("X.509")
                        .generateCertificate(
                                new ByteArrayInputStream(Base64.getDecoder().decode(der)));
    }
}
