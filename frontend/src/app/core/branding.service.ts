import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { InstitutionProfile } from './models';

@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly api = inject(ApiService);
  private readonly document = inject(DOCUMENT);
  private readonly state = signal<InstitutionProfile | null>(null);
  readonly profile = this.state.asReadonly();

  load(): void {
    this.api.getInstitutionProfile().subscribe({ next: profile => this.apply(profile), error: () => this.clear() });
  }

  apply(profile: InstitutionProfile): void {
    this.state.set(profile);
    const root = this.document.documentElement;
    root.style.setProperty('--brand-primary', profile.primaryColor);
    root.style.setProperty('--brand-secondary', profile.secondaryColor);
    root.style.setProperty('--brand-accent', profile.accentColor);
    root.dataset['institutionTheme'] = profile.brandingStyle.toLowerCase();
  }

  clear(): void {
    this.state.set(null);
    const root = this.document.documentElement;
    root.style.removeProperty('--brand-primary');
    root.style.removeProperty('--brand-secondary');
    root.style.removeProperty('--brand-accent');
    delete root.dataset['institutionTheme'];
  }
}
