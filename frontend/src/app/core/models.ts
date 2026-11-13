export type InstitutionType = 'ESCUELA' | 'INSTITUTO_TECNICO' | 'UNIVERSIDAD' | 'COLEGIO' | 'INSTITUTO' | 'ACADEMIA' | 'CENTRO_FORMACION' | 'OTRO';
export type BrandingStyle = 'MODERNO' | 'CLASICO' | 'MINIMALISTA';

export interface InstitutionProfile {
  id: string;
  name: string;
  slug: string;
  institutionType: InstitutionType;
  primaryColor: string;
  secondaryColor: string;
  accentColor: string;
  brandingStyle: BrandingStyle;
  logoName?: string;
  logoUrl?: string;
  verificationDocumentName?: string;
  verificationDocumentHash?: string;
  signatoryName?: string;
  signatoryTitle?: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  role: string;
  tenantId: string | null;
  institutionName: string | null;
  institutionType: InstitutionType | null;
}

export interface Student {
  id: string;
  fullName: string;
  documentNumber: string;
  country: string;
  documentType: string;
}

export type CredentialStatus = 'DRAFT' | 'ISSUED' | 'REVOKED';

export interface Credential {
  id: string;
  studentId: string;
  holderName: string;
  credentialType: string;
  title: string;
  status: CredentialStatus;
  verificationCode: string;
  documentName?: string;
  documentHash?: string;
  blockchainTxId?: string;
  issuedAt?: string;
  createdAt: string;
}

export interface PublicCredential {
  organization: string;
  institutionType: InstitutionType;
  institutionLogoUrl?: string;
  primaryColor?: string;
  secondaryColor?: string;
  holderName: string;
  credentialType: string;
  title: string;
  status: CredentialStatus;
  verificationCode: string;
  documentHash?: string;
  issuedAt?: string;
  revokedAt?: string;
  revocationReason?: string;
}

export type PortalItemType = 'ANNOUNCEMENT' | 'DEADLINE' | 'RESOURCE';

export interface PortalItem {
  id: string;
  itemType: PortalItemType;
  title: string;
  description?: string;
  eventAt?: string;
  link?: string;
  priority: number;
  createdAt: string;
}
