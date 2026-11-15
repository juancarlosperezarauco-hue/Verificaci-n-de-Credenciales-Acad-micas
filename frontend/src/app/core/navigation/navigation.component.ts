import { Component, DestroyRef, Input, OnDestroy, inject, output, signal } from '@angular/core';
import { Dialog, DialogModule, DialogRef } from '@angular/cdk/dialog';
import { Overlay } from '@angular/cdk/overlay';
import { NavigationEnd, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationDrawerComponent } from './navigation-drawer.component';

/** Left-side drawer on desktop and mobile, with CDK focus/scroll management. */
@Component({
  selector: 'app-navigation',
  imports: [DialogModule],
  host: { '[class.nav-light]': 'light' },
  templateUrl: './navigation.component.html',
  styleUrl: './navigation.component.css',
})
export class NavigationComponent implements OnDestroy {
  @Input() light = false;
  readonly logout = output<void>();
  readonly opened = signal(false);
  private readonly dialogs = inject(Dialog);
  private readonly overlay = inject(Overlay);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private ref?: DialogRef<'logout', NavigationDrawerComponent>;
  constructor() {
    this.router.events.pipe(takeUntilDestroyed()).subscribe((event) => {
      if (event instanceof NavigationEnd) this.ref?.close();
    });
  }
  open() {
    if (this.ref) return;
    this.opened.set(true);
    this.ref = this.dialogs.open<'logout', undefined, NavigationDrawerComponent>(
      NavigationDrawerComponent,
      {
        positionStrategy: this.overlay.position().global().left('0').top('0'),
        width: 'min(320px, calc(100vw - 24px))',
        maxWidth: 'none',
        height: '100dvh',
        ariaLabel: 'Menú lateral principal',
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        closeOnNavigation: true,
      },
    );
    this.ref.closed.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((result) => {
      this.ref = undefined;
      this.opened.set(false);
      if (result === 'logout') this.logout.emit();
    });
  }
  ngOnDestroy() {
    this.ref?.close();
  }
}
