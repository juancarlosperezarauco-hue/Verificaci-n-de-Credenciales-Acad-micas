package bo.edu.uagrm.credenciales.report;

import bo.edu.uagrm.credenciales.audit.*;
import bo.edu.uagrm.credenciales.credential.*;
import bo.edu.uagrm.credenciales.security.UserPrincipal;
import bo.edu.uagrm.credenciales.shared.*;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private static final int EXPORT_LIMIT = 1000;
    private final JdbcTemplate jdbc;
    private final TenantAccess access;
    private final AuditTrail audit;
    private final Clock clock;

    public ReportService(JdbcTemplate jdbc, TenantAccess access, AuditTrail audit, Clock clock) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    public record Row(
            UUID id,
            String holderName,
            String type,
            String title,
            String status,
            String code,
            Instant issuedAt) {}

    public record Group(String label, long total) {}

    public record Summary(
            long linkedStudents,
            long credentials,
            long issued,
            long drafts,
            long revoked,
            List<Group> byType,
            List<Group> byMonth,
            Instant generatedAt) {}

    public record Platform(
            long organizations, long students, long credentials, List<Group> byStatus) {}

    private record Query(String where, List<Object> args) {}

    private Query query(UUID tenant, CredentialFilters filters) {
        var args = new ArrayList<Object>();
        args.add(tenant);
        var sql = new StringBuilder(" where c.tenant_id=?");
        if (filters.year() != null) {
            sql.append(" and c.issued_at>=? and c.issued_at<?");
            args.add(java.sql.Timestamp.from(filters.start()));
            args.add(java.sql.Timestamp.from(filters.end()));
        }
        if (filters.type() != null) {
            sql.append(" and c.credential_type=?");
            args.add(filters.type());
        }
        if (filters.status() != null) {
            sql.append(" and c.status=?");
            args.add(filters.status().name());
        }
        return new Query(sql.toString(), args);
    }

    private List<Row> rows(Query q, int offset, int limit) {
        var args = new ArrayList<>(q.args());
        args.add(limit);
        args.add(offset);
        return jdbc.query(
                "select c.id,c.holder_name,c.credential_type,c.title,c.status,c.verification_code,c.issued_at from credentials c"
                        + q.where()
                        + " order by c.created_at desc,c.id desc limit ? offset ?",
                (r, n) ->
                        new Row(
                                r.getObject(1, UUID.class),
                                r.getString(2),
                                r.getString(3),
                                r.getString(4),
                                r.getString(5),
                                r.getString(6),
                                r.getTimestamp(7) == null ? null : r.getTimestamp(7).toInstant()),
                args.toArray());
    }

    public ResultPage<Row> page(UserPrincipal p, CredentialFilters filters, int page, int size) {
        ResultPage.validate(page, size);
        var q = query(access.require(p, "REPORT_VIEW"), filters);
        long total =
                jdbc.queryForObject(
                        "select count(*) from credentials c" + q.where(),
                        Long.class,
                        q.args().toArray());
        return new ResultPage<>(rows(q, Math.multiplyExact(page, size), size), total, page, size);
    }

    public Summary summary(UserPrincipal p, CredentialFilters filters) {
        UUID tenant = access.require(p, "REPORT_VIEW");
        var q = query(tenant, filters);
        var counts =
                jdbc.queryForList(
                        "select c.status,count(*) as total from credentials c"
                                + q.where()
                                + " group by c.status",
                        q.args().toArray());
        var totals = new HashMap<String, Long>();
        counts.forEach(
                r -> totals.put((String) r.get("status"), ((Number) r.get("total")).longValue()));
        long students =
                jdbc.queryForObject(
                        "select count(distinct student_id) from student_consents where tenant_id=? and used_at is not null and revoked_at is null",
                        Long.class,
                        tenant);
        var types =
                jdbc.query(
                        "select c.credential_type,count(*) from credentials c"
                                + q.where()
                                + " group by c.credential_type order by c.credential_type",
                        (r, n) -> new Group(r.getString(1), r.getLong(2)),
                        q.args().toArray());
        // EXTRACT works on both PostgreSQL and the test database. Return aggregated rows, not
        // identities.
        var months =
                jdbc.query(
                        "select extract(year from c.issued_at at time zone 'UTC') as y,extract(month from c.issued_at at time zone 'UTC') as m,count(*) from credentials c"
                                + q.where()
                                + " and c.issued_at is not null group by extract(year from c.issued_at at time zone 'UTC'),extract(month from c.issued_at at time zone 'UTC') order by y,m",
                        (r, n) ->
                                new Group(
                                        String.format("%04d-%02d", r.getInt(1), r.getInt(2)),
                                        r.getLong(3)),
                        q.args().toArray());
        return new Summary(
                students,
                totals.values().stream().mapToLong(Long::longValue).sum(),
                totals.getOrDefault("ISSUED", 0L),
                totals.getOrDefault("DRAFT", 0L),
                totals.getOrDefault("REVOKED", 0L),
                types,
                months,
                clock.instant());
    }

    public Platform platform() {
        return new Platform(
                jdbc.queryForObject("select count(*) from tenants", Long.class),
                jdbc.queryForObject("select count(*) from student_accounts", Long.class),
                jdbc.queryForObject("select count(*) from credentials", Long.class),
                jdbc.query(
                        "select status,count(*) from organization_governance group by status order by status",
                        (r, n) -> new Group(r.getString(1), r.getLong(2))));
    }

    @Transactional
    public CredentialDtos.StoredDocument export(
            UserPrincipal p, CredentialFilters filters, boolean excel) {
        UUID tenant = access.require(p, "REPORT_VIEW");
        var q = query(tenant, filters);
        var rows = rows(q, 0, EXPORT_LIMIT + 1);
        if (rows.size() > EXPORT_LIMIT)
            throw new IllegalArgumentException(
                    "Hay más de 1000 resultados. Aplica filtros antes de exportar.");
        String institution = access.active(tenant).getName();
        byte[] bytes = excel ? excel(rows, institution, filters) : pdf(rows, institution, filters);
        audit.record(AuditAction.REPORT_EXPORTED, tenant, AuditTrail.Resource.ORGANIZATION, tenant);
        return new CredentialDtos.StoredDocument(
                bytes,
                excel
                        ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        : "application/pdf",
                "credenciales." + (excel ? "xlsx" : "pdf"));
    }

    private static final String[] HEADERS = {
        "Estudiante", "Tipo", "Título", "Estado", "Código de verificación", "Emisión UTC"
    };

    private String[] cells(Row r) {
        return new String[] {
            r.holderName(),
            r.type(),
            r.title(),
            r.status(),
            r.code() == null ? "" : r.code(),
            r.issuedAt() == null ? "" : r.issuedAt().toString()
        };
    }

    private String filterLabel(CredentialFilters f) {
        return "Año de emisión UTC: "
                + (f.year() == null ? "Todos" : f.year())
                + " · Tipo: "
                + (f.type() == null ? "Todos" : f.type())
                + " · Estado: "
                + (f.status() == null ? "Todos" : f.status());
    }

    private byte[] excel(List<Row> rows, String institution, CredentialFilters filters) {
        try (var book = new XSSFWorkbook();
                var out = new ByteArrayOutputStream()) {
            var sheet = book.createSheet("Credenciales");
            sheet.createRow(0).createCell(0).setCellValue(institution);
            sheet.createRow(1).createCell(0).setCellValue(filterLabel(filters));
            sheet.createRow(2)
                    .createCell(0)
                    .setCellValue(
                            "Generado UTC: " + clock.instant() + " · Registros: " + rows.size());
            var style = book.createCellStyle();
            var font = book.createFont();
            font.setBold(true);
            style.setFont(font);
            style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var header = sheet.createRow(4);
            for (int i = 0; i < HEADERS.length; i++) {
                var cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, (i == 2 ? 48 : 26) * 256);
            }
            int number = 5;
            for (var row : rows) {
                var line = sheet.createRow(number++);
                var values = cells(row);
                // Explicit string cells keep user-controlled values such as =HYPERLINK(...) inert.
                for (int i = 0; i < values.length; i++)
                    line.createCell(i, CellType.STRING).setCellValue(values[i]);
            }
            sheet.createFreezePane(0, 5);
            sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, number - 1), 0, 5));
            book.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel.", e);
        }
    }

    private byte[] pdf(List<Row> rows, String institution, CredentialFilters filters) {
        var out = new ByteArrayOutputStream();
        var document = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(
                new Paragraph(institution, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15)));
        document.add(
                new Paragraph(
                        "Reporte de credenciales",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        document.add(
                new Paragraph(filterLabel(filters), FontFactory.getFont(FontFactory.HELVETICA, 9)));
        document.add(
                new Paragraph(
                        "Generado UTC: " + clock.instant() + " · Registros: " + rows.size(),
                        FontFactory.getFont(FontFactory.HELVETICA, 9)));
        document.add(Chunk.NEWLINE);
        var table = new PdfPTable(new float[] {2, 1.2f, 2.7f, 1.1f, 2, 1.5f});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String h : HEADERS) {
            var c = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            c.setBackgroundColor(new java.awt.Color(226, 236, 251));
            c.setPadding(5);
            table.addCell(c);
        }
        for (var row : rows)
            for (String value : cells(row)) {
                var c =
                        new PdfPCell(
                                new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 8)));
                c.setPadding(5);
                table.addCell(c);
            }
        if (rows.isEmpty())
            document.add(new Paragraph("No hay resultados con los filtros seleccionados."));
        else document.add(table);
        document.close();
        return out.toByteArray();
    }
}
