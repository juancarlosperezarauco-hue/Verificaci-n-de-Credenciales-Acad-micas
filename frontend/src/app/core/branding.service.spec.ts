import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BrandingService } from './branding.service';
import { InstitutionProfile } from './models';

describe('Institutional branding boundaries', () => {
  let service: BrandingService;
  let http: HttpTestingController;
  const profile: InstitutionProfile = {
    id: 'tenant',
    name: 'Instituto',
    slug: 'instituto',
    institutionType: 'INSTITUTO',
    primaryColor: '#ffffff',
    secondaryColor: '#2563eb',
    accentColor: '#0d9488',
    brandingStyle: 'MODERNO',
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BrandingService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => {
    service.clear();
    http.verify();
  });
  it('uses dark foregrounds on pale institution colors and readable links on white', () => {
    service.apply(profile);
    const style = TestBed.inject(DOCUMENT).documentElement.style;
    expect(style.getPropertyValue('--brand-on-primary')).toBe('#0f172a');
    expect(style.getPropertyValue('--brand-ink')).toBe('#1d4ed8');
  });
  it('uses light foregrounds on dark colors and resets all tenant variables on exit', () => {
    service.apply({ ...profile, primaryColor: '#15345c' });
    const root = TestBed.inject(DOCUMENT).documentElement;
    expect(root.style.getPropertyValue('--brand-on-primary')).toBe('#ffffff');
    service.clear();
    expect(root.style.getPropertyValue('--brand-on-primary')).toBe('');
    expect(root.style.getPropertyValue('--brand-ink')).toBe('');
    expect(root.dataset['institutionTheme']).toBeUndefined();
  });
  it('cancels the previous tenant request before clearing or fetching another institution', () => {
    service.load();
    const old = http.expectOne('/api/institution/profile');
    service.clear();
    expect(old.cancelled).toBeTrue();
    service.load();
    const next = http.expectOne('/api/institution/profile');
    next.flush({ ...profile, id: 'another', name: 'Otra institución' });
    expect(service.profile()?.id).toBe('another');
  });
});
