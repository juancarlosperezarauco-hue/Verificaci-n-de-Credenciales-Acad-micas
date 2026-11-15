import { Component, computed, inject } from '@angular/core';
import { DialogRef } from '@angular/cdk/dialog';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../auth.service';
import { OrganizationApi } from '../../features/organization/organization.api';
import { navigationFor } from './navigation.model';

@Component({imports:[RouterLink,RouterLinkActive],template:`
 <section class="drawer">
  <header class="drawer-header"><div><p class="eyebrow">Credenciales académicas</p><h2>Menú principal</h2></div><button type="button" class="close-button" aria-label="Cerrar menú lateral" (click)="ref.close()"><svg aria-hidden="true" width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="m6 6 12 12M18 6 6 18"/></svg></button></header>
  <nav class="drawer-content" aria-label="Módulos de la plataforma">
   <p class="section-label">Módulos</p>
   @for(item of model().primary;track item.path){<a class="drawer-link" [routerLink]="item.path" routerLinkActive="drawer-active" ariaCurrentWhenActive="page" (click)="ref.close()"><span class="item-marker" aria-hidden="true"></span>{{item.label}}</a>}
   @if(model().organization.length){<div class="divider"></div><p class="section-label">{{model().organizationLabel}}</p>@for(item of model().organization;track item.path){<a class="drawer-link" [routerLink]="item.path" [routerLinkActiveOptions]="{exact:item.path==='/organization'}" routerLinkActive="drawer-active" ariaCurrentWhenActive="page" (click)="ref.close()"><span class="item-marker" aria-hidden="true"></span>{{item.label}}</a>}}
   @if(auth.isAuthenticated()){<div class="divider"></div><p class="section-label">Mi cuenta</p>@for(item of model().account;track item.path){<a class="drawer-link" [routerLink]="item.path" routerLinkActive="drawer-active" ariaCurrentWhenActive="page" (click)="ref.close()"><span class="item-marker" aria-hidden="true"></span>{{item.label}}</a>}}@else{<div class="divider"></div><a class="drawer-link" routerLink="/login" (click)="ref.close()"><span class="item-marker" aria-hidden="true"></span>Ingresar al portal académico</a>}
  </nav>
  <footer class="drawer-footer">@if(auth.isAuthenticated()){<button type="button" class="sign-out" (click)="ref.close('logout')"><svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M9 5H4v14h5m5-14 7 7-7 7M8 12h13"/></svg>Cerrar sesión</button>}<p>Tu espacio académico</p></footer>
 </section>`,styles:`
 :host{display:block;height:100%;width:100%;color-scheme:light;color:#334155;font-size:.875rem;}
 .drawer{display:flex;flex-direction:column;width:100%;height:100%;min-height:0;background:#fff;border-right:1px solid #e2e8f0;box-shadow:12px 0 40px #0f172a26;}
 .drawer-header{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:24px 20px 20px;border-bottom:1px solid #e2e8f0;}
 .eyebrow{margin:0 0 7px;color:#64748b;font-size:.65rem;letter-spacing:.1em;text-transform:uppercase;font-weight:700;}
 h2{margin:0;color:#0f172a;font-size:1.2rem;font-weight:700;}
 .close-button{display:grid;place-items:center;flex-shrink:0;width:38px;height:38px;border:1px solid #e2e8f0;border-radius:10px;background:#fff;color:#475569;}
 .drawer-content{flex:1;min-height:0;overflow-y:auto;overscroll-behavior:contain;padding:16px 12px 24px;}
 .section-label{margin:0;padding:12px 14px 9px;color:#64748b;font-size:.68rem;font-weight:700;text-transform:uppercase;letter-spacing:.09em;}
 .drawer-link{display:flex;align-items:center;gap:12px;min-height:44px;margin:3px 0;padding:12px 14px;border-radius:10px;line-height:1.4;text-decoration:none;color:#334155;overflow-wrap:anywhere;}
 .item-marker{flex-shrink:0;width:7px;height:7px;border:1.5px solid #94a3b8;border-radius:2px;}
 .drawer-link:hover,.close-button:hover{background:#f1f5f9;}
 .drawer-active{background:#eff6ff;color:#1d4ed8;font-weight:650;}
 .drawer-active .item-marker{background:#2563eb;border-color:#2563eb;}
 .divider{height:1px;margin:15px 10px 5px;background:#e2e8f0;}
 .drawer-footer{flex-shrink:0;padding:16px 20px;border-top:1px solid #e2e8f0;background:#f8fafc;}
 .drawer-footer p{margin:12px 0 0;font-size:.7rem;color:#64748b;}
 .sign-out{display:flex;align-items:center;justify-content:center;gap:10px;width:100%;min-height:42px;border:1px solid #e2e8f0;border-radius:10px;background:#fff;color:#be123c;font:inherit;font-weight:600;}
 .sign-out:hover{background:#fff1f2;}
 a:focus-visible,button:focus-visible{outline:2px solid #2563eb;outline-offset:2px;}
 `})
export class NavigationDrawerComponent {
 readonly ref=inject(DialogRef<'logout'>);readonly auth=inject(AuthService);private readonly access=inject(OrganizationApi);
 readonly model=computed(()=>navigationFor(this.auth.current()?.role??null,this.access.access()?.permissions??[],this.access.access()?.onboarding??false));
}
