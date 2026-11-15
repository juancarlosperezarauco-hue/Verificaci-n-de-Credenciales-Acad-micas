import { Component, ElementRef, HostListener, Input, computed, inject, output, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../auth.service';
import { OrganizationApi } from '../../features/organization/organization.api';
import { navigationFor } from './navigation.model';

type OpenMenu='organization'|'account'|'mobile'|null;
@Component({selector:'app-navigation',imports:[RouterLink,RouterLinkActive],host:{'[class.nav-light]':'light'},template:`
 <nav class="desktop-nav" aria-label="Navegación principal">
  @for(item of model().primary;track item.path){<a class="nav-link" [routerLink]="item.path" routerLinkActive="nav-active" ariaCurrentWhenActive="page">{{item.label}}</a>}
  @if(model().organization.length){<div class="menu-wrap"><button type="button" class="nav-link menu-trigger" [class.nav-active]="groupActive()" aria-controls="organization-navigation" [attr.aria-expanded]="openMenu()==='organization'" (click)="toggle('organization',$event)">{{model().organizationLabel}}<span class="chevron" aria-hidden="true"></span></button>
   @if(openMenu()==='organization'){<div class="popup" id="organization-navigation"><p class="popup-label">{{model().organizationLabel}}</p>@for(item of model().organization;track item.path){<a class="popup-link" [routerLink]="item.path" [routerLinkActiveOptions]="{exact:item.path==='/organization'}" routerLinkActive="nav-active" ariaCurrentWhenActive="page" (click)="close()">{{item.label}}</a>}</div>}
  </div>}
  @if(auth.isAuthenticated()){<div class="menu-wrap account-wrap"><button type="button" class="nav-link menu-trigger account-trigger" aria-controls="account-navigation" [attr.aria-expanded]="openMenu()==='account'" (click)="toggle('account',$event)"><svg aria-hidden="true" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/></svg>Cuenta<span class="chevron" aria-hidden="true"></span></button>
   @if(openMenu()==='account'){<div class="popup" id="account-navigation"><p class="popup-label">Mi cuenta</p>@for(item of model().account;track item.path){<a class="popup-link" [routerLink]="item.path" routerLinkActive="nav-active" ariaCurrentWhenActive="page" (click)="close()">{{item.label}}</a>}<div class="menu-divider"></div><button type="button" class="popup-link sign-out" (click)="signOut()">Cerrar sesión</button></div>}
  </div>}@else{<a class="login-link" routerLink="/login">Ingresar</a>}
 </nav>
 <div class="mobile-nav"><button type="button" class="nav-link mobile-trigger" aria-controls="mobile-navigation" [attr.aria-expanded]="openMenu()==='mobile'" (click)="toggle('mobile',$event)"><svg aria-hidden="true" width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 6h16M4 12h16M4 18h16"/></svg>Menú</button>
  @if(openMenu()==='mobile'){<nav class="popup mobile-popup" id="mobile-navigation" aria-label="Módulos de la plataforma"><p class="popup-label">Módulos</p>@for(item of model().primary;track item.path){<a class="popup-link" [routerLink]="item.path" routerLinkActive="nav-active" ariaCurrentWhenActive="page" (click)="close()">{{item.label}}</a>}
   @if(model().organization.length){<div class="menu-divider"></div><p class="popup-label">{{model().organizationLabel}}</p>@for(item of model().organization;track item.path){<a class="popup-link" [routerLink]="item.path" [routerLinkActiveOptions]="{exact:item.path==='/organization'}" routerLinkActive="nav-active" ariaCurrentWhenActive="page" (click)="close()">{{item.label}}</a>}}
   @if(auth.isAuthenticated()){<div class="menu-divider"></div><p class="popup-label">Mi cuenta</p>@for(item of model().account;track item.path){<a class="popup-link" [routerLink]="item.path" routerLinkActive="nav-active" ariaCurrentWhenActive="page" (click)="close()">{{item.label}}</a>}<button type="button" class="popup-link sign-out" (click)="signOut()">Cerrar sesión</button>}@else{<div class="menu-divider"></div><a class="popup-link" routerLink="/login" (click)="close()">Ingresar al portal académico</a>}
  </nav>}
 </div>`,styles:`
 :host{position:relative;display:block;flex-shrink:0;--nav-text:#e2e8f0;--nav-muted:#94a3b8;--nav-panel:#0f172a;--nav-border:#334155;--nav-hover:#1e293b;--nav-danger:#fda4af;color:var(--nav-text);font-size:.875rem;}
 :host.nav-light{--nav-text:#334155;--nav-muted:#64748b;--nav-panel:#fff;--nav-border:#e2e8f0;--nav-hover:#f1f5f9;--nav-danger:#be123c;}
 .desktop-nav{display:none;align-items:center;justify-content:flex-end;gap:4px;}
 .mobile-nav,.menu-wrap{position:relative;}
 .nav-link{display:inline-flex;align-items:center;justify-content:center;gap:9px;min-height:42px;padding:10px 12px;white-space:nowrap;border:0;border-radius:10px;background:transparent;color:inherit;font:inherit;font-weight:500;text-decoration:none;}
 .nav-link:hover,.popup-link:hover{background:var(--nav-hover);}
 .nav-link:focus-visible,.popup-link:focus-visible,.login-link:focus-visible{outline:2px solid var(--brand-primary);outline-offset:3px;}
 .nav-active{background:var(--nav-hover);font-weight:650;}
 .account-wrap{margin-left:8px;padding-left:12px;border-left:1px solid var(--nav-border);}
 .account-trigger,.mobile-trigger{border:1px solid var(--nav-border);}
 .chevron{width:6px;height:6px;border-right:1.5px solid currentColor;border-bottom:1.5px solid currentColor;transform:translateY(-2px) rotate(45deg);margin-left:2px;}
 [aria-expanded="true"] .chevron{transform:translateY(1px) rotate(225deg);}
 .popup{position:absolute;right:0;top:calc(100% + 14px);z-index:60;width:min(280px,calc(100vw - 32px));padding:8px;border:1px solid var(--nav-border);border-radius:14px;background:var(--nav-panel);box-shadow:0 16px 48px #0003;max-height:calc(100dvh - 110px);overflow-y:auto;overscroll-behavior:contain;}
 .popup-label{margin:0;padding:9px 12px;font-size:.7rem;line-height:1.5;font-weight:700;text-transform:uppercase;letter-spacing:.08em;color:var(--nav-muted);}
 .popup-link{display:block;width:100%;border:0;border-radius:8px;padding:11px 12px;text-align:left;white-space:normal;overflow-wrap:anywhere;font:inherit;line-height:1.4;color:inherit;background:transparent;text-decoration:none;}
 .menu-divider{height:1px;background:var(--nav-border);margin:6px 4px;}
 .sign-out{color:var(--nav-danger);}
 .login-link{margin-left:10px;display:inline-flex;padding:11px 18px;min-height:42px;border-radius:10px;font-weight:650;background:var(--brand-primary);color:#020617;text-decoration:none;}
 .mobile-popup{width:min(320px,calc(100vw - 32px));}
 @media(min-width:1100px){.desktop-nav{display:flex;}.mobile-nav{display:none;}}
 `})
export class NavigationComponent {
 @Input() light=false;readonly logout=output<void>();readonly auth=inject(AuthService);
 private readonly access=inject(OrganizationApi);private readonly element=inject(ElementRef<HTMLElement>);private readonly router=inject(Router);
 readonly model=computed(()=>navigationFor(this.auth.current()?.role??null,this.access.access()?.permissions??[],this.access.access()?.onboarding??false));
 readonly openMenu=signal<OpenMenu>(null);private readonly currentUrl=signal(this.router.url);private trigger?:HTMLButtonElement;
 constructor(){this.router.events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd){this.currentUrl.set(event.urlAfterRedirects);this.close();}});}
 groupActive(){const path=this.currentUrl().split('?')[0];return this.model().organization.some(item=>path===item.path||(item.path!=='/organization'&&path.startsWith(item.path+'/')));}
 toggle(menu:OpenMenu,event:MouseEvent){this.trigger=event.currentTarget as HTMLButtonElement;this.openMenu.set(this.openMenu()===menu?null:menu);}
 close(restoreFocus=false){this.openMenu.set(null);if(restoreFocus)this.trigger?.focus();}
 signOut(){this.close();this.logout.emit();}
 @HostListener('document:click',['$event']) outside(event:MouseEvent){if(this.openMenu()&&event.target instanceof Node&&!this.element.nativeElement.contains(event.target))this.close();}
 @HostListener('document:keydown.escape',['$event']) escape(event:Event){if(this.openMenu()){event.preventDefault();this.close(true);}}
}
