package bo.edu.uagrm.credenciales.identity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "student_accounts", uniqueConstraints = @UniqueConstraint(columnNames = {"country", "document_type", "document_number"}))
public class StudentAccount {
    @Id private UUID id;
    @Column(name="full_name", length=180) private String fullName;
    @Column(length=2) private String country;
    @Column(name="document_type", length=30) private String documentType;
    @Column(name="document_number", length=80) private String documentNumber;
    @Column(nullable=false, unique=true, length=180) private String email;
    @Column(name="password_hash", nullable=false, length=100) private String passwordHash;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="search_code", nullable=false, unique=true, length=36) private String searchCode;
    protected StudentAccount() {}
    public StudentAccount(String name, String country, String type, String document, String email, String hash) {
        id = UUID.randomUUID(); fullName=name; this.country=country; documentType=type;
        documentNumber=document; this.email=email; passwordHash=hash; createdAt=Instant.now();
        rotateSearchCode();
    }
    public StudentAccount(String email, String hash) {
        this(null, null, null, null, email, hash);
    }
    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public String getCountry() { return country; }
    public String getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getSearchCode() { return searchCode; }
    public boolean isProfileComplete() {
        return fullName != null && country != null && documentType != null && documentNumber != null;
    }
    public void updateProfile(String name, String country, String type, String document) {
        fullName = name;
        this.country = country;
        documentType = type;
        documentNumber = document;
    }
    public void rotateSearchCode() {
        searchCode = "EST-" + UUID.randomUUID().toString().replace("-", "").toUpperCase(java.util.Locale.ROOT);
    }
}
