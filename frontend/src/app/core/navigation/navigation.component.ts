import { Component, DestroyRef, Input, OnDestroy, inject, output, signal } from '@angular/core';
import { Dialog, DialogModule, DialogRef } from '@angular/cdk/dialog';
import { Overlay } from '@angular/cdk/overlay';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationDrawerComponent } from './navigation-drawer.component';

/** Left-side drawer on desktop and mobile, with CDK focus/scroll management. */
@Component({selector:'app-navigation',imports:[DialogModule],host:{'[class.nav-light]':'light'},template:`
 <button type="button" class="menu-toggle" [attr.aria-expanded]="opened()" aria-haspopup="dialog" aria-label="Abrir menú lateral" (click)="open()">
  <svg aria-hidden="true" width="21" height="21" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M4 6h16M4 12h16M4 18h16"/></svg><span>Menú</span>
 </button>`,styles:`
 :host{display:block;flex-shrink:0;color:#e2e8f0;}
 :host.nav-light{color:#334155;}
 .menu-toggle{display:inline-flex;align-items:center;gap:9px;min-height:42px;padding:10px 13px;border:1px solid #94a3b844;border-radius:10px;background:transparent;color:inherit;font:inherit;font-size:.875rem;font-weight:600;}
 .menu-toggle:hover{background:#94a3b81a;}
 .menu-toggle:focus-visible{outline:2px solid var(--brand-primary);outline-offset:3px;}
 @media(max-width:479px){.menu-toggle{padding:10px;}.menu-toggle span{display:none;}}
 `})
export class NavigationComponent implements OnDestroy {
 @Input() light=false;readonly logout=output<void>();readonly opened=signal(false);
 private readonly dialogs=inject(Dialog);private readonly overlay=inject(Overlay);private readonly router=inject(Router);private readonly destroyRef=inject(DestroyRef);
 private ref?:DialogRef<'logout',NavigationDrawerComponent>;
 constructor(){this.router.events.pipe(takeUntilDestroyed()).subscribe(event=>{if(event instanceof NavigationEnd)this.ref?.close();});}
 open(){
  if(this.ref)return;
  this.opened.set(true);
  this.ref=this.dialogs.open<'logout',undefined,NavigationDrawerComponent>(NavigationDrawerComponent,{
   positionStrategy:this.overlay.position().global().left('0').top('0'),width:'min(320px, calc(100vw - 24px))',maxWidth:'none',height:'100dvh',
   ariaLabel:'Menú lateral principal',autoFocus:'first-tabbable',restoreFocus:true,closeOnNavigation:true,
  });
  this.ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(result=>{this.ref=undefined;this.opened.set(false);if(result==='logout')this.logout.emit();});
 }
 ngOnDestroy(){this.ref?.close();}
}
