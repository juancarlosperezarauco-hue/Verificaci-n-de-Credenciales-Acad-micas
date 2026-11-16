import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuthResponse, LoginResponse, BrandingStyle, Credential, InstitutionProfile, InstitutionType, PortalItem, PortalItemType, PublicCredential, Student } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  login(body: { organizationSlug: string; email: string; password: string }) {
    return this.http.post<LoginResponse>('/api/auth/login', body);
  }

  registerInstitution(body: { organizationName: string; organizationSlug: string; institutionType: InstitutionType; email: string; password: string }) {
    return this.http.post<AuthResponse>('/api/auth/register', body);
  }

  getInstitutionProfile() { return this.http.get<InstitutionProfile>('/api/institution/profile'); }

  updateInstitutionProfile(body: { primaryColor: string; secondaryColor: string; accentColor: string; brandingStyle: BrandingStyle; signatoryName: string; signatoryTitle: string }) {
    return this.http.patch<InstitutionProfile>('/api/institution/profile', body);
  }

  uploadInstitutionLogo(file: File) {
    const body = new FormData(); body.append('file', file);
    return this.http.post<InstitutionProfile>('/api/institution/profile/logo', body);
  }

  uploadInstitutionVerificationDocument(file: File) {
    const body = new FormData(); body.append('file', file);
    return this.http.post<InstitutionProfile>('/api/institution/profile/verification-document', body);
  }

  downloadInstitutionVerificationDocument() {
    return this.http.get('/api/institution/profile/verification-document', { responseType: 'blob', observe: 'response' });
  }

  listStudents() { return this.http.get<Student[]>('/api/students'); }

  redeemStudent(code: string) {
    return this.http.post<Student>('/api/students/redeem', {code});
  }

  listCredentials() { return this.http.get<Credential[]>('/api/credentials'); }

  listPortalItems() { return this.http.get<PortalItem[]>('/api/portal/items'); }

  createPortalItem(body: { itemType: PortalItemType; title: string; description: string; eventAt?: string; link: string; priority: number }) {
    return this.http.post<PortalItem>('/api/portal/items', body);
  }

  deletePortalItem(id: string) { return this.http.delete<void>(`/api/portal/items/${id}`); }

  createCredential(body: { studentId: string; credentialType: string; title: string }) {
    return this.http.post<Credential>('/api/credentials', body);
  }

  createCredentials(body: { studentIds: string[]; credentialType: string; title: string }) {
    return this.http.post<Credential[]>('/api/credentials/batch', body);
  }

  generateCredentialDocument(id: string, templateVersionId?: string) {
    return this.http.post<Credential>(`/api/credentials/${id}/generate-document`, templateVersionId ? {templateVersionId} : {});
  }

  downloadCredentialDocument(id: string) {
    return this.http.get(`/api/credentials/${id}/document`, { responseType: 'blob', observe: 'response' });
  }

  signCredential(id: string) {
    return this.http.post<Credential>(`/api/credentials/${id}/sign`, {});
  }

  issueCredential(id: string, operationKey?: string) {
    return this.http.patch<Credential>(`/api/credentials/${id}/issue`, {}, operationKey ? {headers:{'Idempotency-Key':operationKey}} : {});
  }

  uploadDocument(id: string, file: File) {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<Credential>(`/api/credentials/${id}/document`, body);
  }

  revokeCredential(id: string, reason: string) {
    return this.http.patch<Credential>(`/api/credentials/${id}/revoke`, { reason });
  }

  verifyCredential(code: string) {
    return this.http.get<PublicCredential>(`/api/public/credentials/${encodeURIComponent(code)}`);
  }

  checkCredentialFile(code:string,file:File) {
    const body=new FormData();body.append('file',file);
    return this.http.post<DocumentCheck>(`/api/public/credentials/${encodeURIComponent(code)}/check-document`,body);
  }
}

export interface DocumentCheck {
  hashMatches:boolean; registryStatus:string; issuerStatus:string; testSignature:boolean;
  signature:{status:string;coversWholeDocument:boolean;cryptographicValid:boolean;signerMatches:boolean;certificateCurrentlyValid:boolean;fingerprint:string|null};
}
