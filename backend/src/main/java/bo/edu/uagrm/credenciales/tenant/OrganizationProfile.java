package bo.edu.uagrm.credenciales.tenant;
import jakarta.persistence.*;
import java.util.*;
@Entity @Table(name="organization_profiles",uniqueConstraints={
 @UniqueConstraint(columnNames={"country","tax_id"}),@UniqueConstraint(columnNames={"country","education_license"})})
public class OrganizationProfile {
 @Id @Column(name="tenant_id") private UUID tenantId;
 @Column(name="legal_name",length=160) private String legalName;
 @Column(name="country",length=2) private String country;
 @Column(name="region",length=120) private String region;
 @Column(name="address",length=500) private String address;
 @Column(name="phone",length=40) private String phone;
 @Column(name="contact_email",length=180) private String contactEmail;
 @Column(name="website",length=255) private String website;
 @Column(name="domain",length=180) private String domain;
 @Column(name="tax_id",length=80) private String taxId;
 @Column(name="education_license",length=120) private String educationLicense;
 @Column(name="ministry_registration",length=180) private String ministryRegistration;
 @Column(name="accreditation",length=500) private String accreditation;
 @Enumerated(EnumType.STRING) @Column(name="institution_type",nullable=false,length=40) private InstitutionType institutionType;
 @ElementCollection @CollectionTable(name="organization_education_levels",joinColumns=@JoinColumn(name="tenant_id"))
 @Enumerated(EnumType.STRING) @Column(name="level",length=30) private Set<EducationLevel> educationLevels=new HashSet<>();
 @Column(name="representative_name",length=180) private String representativeName;
 @Column(name="representative_document_type",length=30) private String representativeDocumentType;
 @Column(name="representative_document_number",length=80) private String representativeDocumentNumber;
 @Column(name="representative_position",length=100) private String representativePosition;
 @Column(name="representative_email",length=180) private String representativeEmail;
 @Column(name="representative_phone",length=40) private String representativePhone;
 protected OrganizationProfile(){}
 public OrganizationProfile(UUID tenantId){this.tenantId=tenantId;}
 private static String text(String value){return value==null?null:value.trim();}
 private static String key(String value){return value.replaceAll("\\s+","").toUpperCase(Locale.ROOT);}
 private static String optionalKey(String value){if(value==null)return null;String normalized=key(value);return normalized.isEmpty()?null:normalized;}
 public void update(OrganizationDtos.Details d){
 legalName=text(d.legalName());
 country=key(d.country());
 region=text(d.region());
 address=text(d.address());
 phone=text(d.phone());
 contactEmail=d.contactEmail()==null?null:d.contactEmail().trim().toLowerCase(Locale.ROOT);
 website=text(d.website());
 domain=d.domain()==null?null:d.domain().trim().toLowerCase(Locale.ROOT);
 taxId=optionalKey(d.taxId());
 educationLicense=optionalKey(d.educationLicense());
 ministryRegistration=text(d.ministryRegistration());
 accreditation=text(d.accreditation());
 institutionType=d.institutionType();educationLevels.clear();educationLevels.addAll(d.educationLevels());
 var p=d.representative();representativeName=text(p.fullName());representativeDocumentType=key(p.documentType());
 representativeDocumentNumber=key(p.documentNumber());representativePosition=text(p.position());
 representativeEmail=p.email().trim().toLowerCase(Locale.ROOT);representativePhone=text(p.phone());
 }
 public OrganizationDtos.Details details(){return new OrganizationDtos.Details(legalName,country,region,address,phone,contactEmail,website,domain,taxId,educationLicense,ministryRegistration,accreditation,institutionType,Set.copyOf(educationLevels),
 new OrganizationDtos.Representative(representativeName,representativeDocumentType,representativeDocumentNumber,representativePosition,representativeEmail,representativePhone));}
}
