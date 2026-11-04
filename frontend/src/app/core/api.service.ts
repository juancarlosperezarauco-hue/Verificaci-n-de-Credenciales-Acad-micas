import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { AuthResponse, Credential, PublicCredential } from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  login(body: { organizationSlug: string; email: string; password: string }) {
    return this.http.post<AuthResponse>('/api/auth/login', body);
  }

  listCredentials() { return this.http.get<Credential[]>('/api/credentials'); }

  createCredential(body: { holderName: string; holderDocument: string; credentialType: string; title: string }) {
    return this.http.post<Credential>('/api/credentials', body);
  }

  issueCredential(id: string) {
    return this.http.patch<Credential>(`/api/credentials/${id}/issue`, {});
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
}
