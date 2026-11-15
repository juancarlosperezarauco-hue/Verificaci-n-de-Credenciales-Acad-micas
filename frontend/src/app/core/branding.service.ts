import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from './api.service';
import { InstitutionProfile } from './models';
import { Subscription } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly api = inject(ApiService);
  private readonly document = inject(DOCUMENT);
  private readonly state = signal<InstitutionProfile | null>(null);
  readonly profile = this.state.asReadonly();
  private pending?: Subscription;

  load(): void {
    this.pending?.unsubscribe();
    this.pending = this.api
      .getInstitutionProfile()
      .subscribe({ next: (profile) => this.apply(profile), error: () => this.clear() });
  }

  apply(profile: InstitutionProfile): void {
    this.state.set(profile);
    const root = this.document.documentElement;
    root.style.setProperty('--brand-primary', profile.primaryColor);
    const rgb = profile.primaryColor
      .replace('#', '')
      .match(/.{2}/g)
      ?.map((part) => parseInt(part, 16) / 255) ?? [0, 0, 0];
    const linear = rgb.map((value) =>
      value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4,
    );
    const luminance = 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2];
    root.style.setProperty('--brand-on-primary', luminance > 0.179 ? '#0f172a' : '#ffffff');
    root.style.setProperty(
      '--brand-ink',
      1.05 / (luminance + 0.05) >= 4.5 ? profile.primaryColor : '#1d4ed8',
    );
    root.style.setProperty('--brand-secondary', profile.secondaryColor);
    root.style.setProperty('--brand-accent', profile.accentColor);
    root.dataset['institutionTheme'] = profile.brandingStyle.toLowerCase();
  }

  clear(): void {
    this.pending?.unsubscribe();
    this.pending = undefined;
    this.state.set(null);
    const root = this.document.documentElement;
    root.style.removeProperty('--brand-primary');
    root.style.removeProperty('--brand-on-primary');
    root.style.removeProperty('--brand-ink');
    root.style.removeProperty('--brand-secondary');
    root.style.removeProperty('--brand-accent');
    delete root.dataset['institutionTheme'];
  }
}
