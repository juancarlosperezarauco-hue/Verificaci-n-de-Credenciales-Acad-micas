import { Component, computed, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from './core/auth.service';
import { BrandingService } from './core/branding.service';
import { OrganizationApi } from './features/organization/organization.api';
import { AccountSecurityApi } from './core/account-security.api';
import { NavigationComponent } from './core/navigation/navigation.component';
import { navigationFor } from './core/navigation/navigation.model';
import { UiIconComponent } from './shared/ui-icon.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, NavigationComponent, UiIconComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  readonly auth = inject(AuthService);
  readonly branding = inject(BrandingService);
  readonly access = inject(OrganizationApi);
  private readonly security = inject(AccountSecurityApi);
  private readonly router = inject(Router);
  readonly currentUrl = signal(this.router.url);
  readonly currentModule = computed(() => {
    const session = this.auth.current();
    const model = navigationFor(
      session?.role ?? null,
      this.access.access()?.permissions ?? [],
      this.access.access()?.onboarding ?? false,
    );
    return (
      [...model.primary, ...model.organization, ...model.account]
        .sort((a, b) => b.path.length - a.path.length)
        .find(
          (item) =>
            this.currentUrl().split('?')[0] === item.path ||
            this.currentUrl().startsWith(item.path + '/'),
        )?.label ?? 'Área de trabajo'
    );
  });
  readonly roleLabel = computed(
    () =>
      (
        ({
          SUPER_ADMIN: 'Administración global',
          TENANT_ADMIN: 'Administración institucional',
          ADMIN: 'Administración institucional',
          STUDENT: 'Estudiante',
          CUSTOM: 'Equipo institucional',
        }) as Record<string, string>
      )[this.auth.current()?.role ?? ''] ?? 'Mi cuenta',
  );
  readonly initials = computed(() =>
    (this.auth.current()?.email ?? 'VC').slice(0, 2).toUpperCase(),
  );

  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationEnd) this.currentUrl.set(event.urlAfterRedirects);
    });
    effect((onCleanup) => {
      const session = this.auth.current();
      this.access.access.set(null);
      this.branding.clear();
      if (session?.token && session.role !== 'STUDENT') {
        const request = this.access.me().subscribe({
          next: (p) => {
            if (p.permissions.includes('ORGANIZATION_VIEW')) this.branding.load();
          },
          error: () => this.access.access.set(null),
        });
        onCleanup(() => request.unsubscribe());
      }
    });
  }
  logout(): void {
    const role = this.auth.current()?.role;
    const finish = (unconfirmed: boolean) => {
      this.auth.logout();
      this.access.access.set(null);
      void this.router.navigate(
        [
          role === 'STUDENT'
            ? '/student/login'
            : role === 'SUPER_ADMIN'
              ? '/superadmin/login'
              : '/login',
        ],
        { queryParams: unconfirmed ? { logoutUnconfirmed: true } : {} },
      );
    };
    this.security.logout().subscribe({ next: () => finish(false), error: () => finish(true) });
  }
}
