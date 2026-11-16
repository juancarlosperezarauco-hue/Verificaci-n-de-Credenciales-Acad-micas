import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi } from './organization.api';
@Component({imports:[RouterLink],template:`
 <section class="org-page max-w-2xl py-20"><div class="org-card"><p class="org-eyebrow">Acceso restringido</p><h1 class="org-title">Tu cuenta no tiene este permiso</h1><p class="org-description">Consulta al administrador de tu organización para revisar los roles asignados.</p>
 <div class="mt-7 flex flex-wrap gap-3">@if(api.can('ORGANIZATION_VIEW')){<a class="org-primary" routerLink="/organization">Ir a mi organización</a>}<a class="org-secondary" routerLink="/verify">Verificar certificado</a><button class="org-secondary" (click)="logout()">Cambiar de cuenta</button></div></div></section>`})
export class AccessDeniedPage {readonly api=inject(OrganizationApi);private readonly auth=inject(AuthService);private readonly router=inject(Router);logout(){this.auth.logout();this.api.access.set(null);void this.router.navigate(['/login']);}}
