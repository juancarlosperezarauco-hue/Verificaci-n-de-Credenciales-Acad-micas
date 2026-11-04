import { Injectable, computed, signal } from '@angular/core';
import { AuthResponse } from './models';

const STORAGE_KEY = 'credential_session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly session = signal<AuthResponse | null>(this.readSession());
  readonly current = this.session.asReadonly();
  readonly isAuthenticated = computed(() => Boolean(this.session()?.token));

  save(response: AuthResponse): void {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(response));
    this.session.set(response);
  }

  token(): string | null { return this.session()?.token ?? null; }

  logout(): void {
    sessionStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }

  private readSession(): AuthResponse | null {
    try {
      const value = sessionStorage.getItem(STORAGE_KEY);
      return value ? JSON.parse(value) as AuthResponse : null;
    } catch {
      return null;
    }
  }
}
