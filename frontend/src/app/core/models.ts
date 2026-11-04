export interface AuthResponse {
  token: string;
  tokenType: string;
  email: string;
  role: string;
  tenantId: string;
}

export type CredentialStatus = 'DRAFT' | 'ISSUED' | 'REVOKED';

export interface Credential {
  id: string;
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
