import { Component, computed, inject } from '@angular/core';
import { DialogRef } from '@angular/cdk/dialog';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth.service';
import { OrganizationApi } from '../../features/organization/organization.api';
import { navigationFor, navigationIcon } from './navigation.model';
import { UiIconComponent } from '../../shared/ui-icon.component';

@Component({
  imports: [RouterLink, RouterLinkActive, UiIconComponent],
  templateUrl: './navigation-drawer.component.html',
  styleUrl: './navigation-drawer.component.css',
})
export class NavigationDrawerComponent {
  readonly icon = navigationIcon;
  readonly ref = inject(DialogRef<'logout'>);
  readonly auth = inject(AuthService);
  private readonly access = inject(OrganizationApi);
  readonly model = computed(() =>
    navigationFor(
      this.auth.current()?.role ?? null,
      this.access.access()?.permissions ?? [],
      this.access.access()?.onboarding ?? false,
    ),
  );
}
