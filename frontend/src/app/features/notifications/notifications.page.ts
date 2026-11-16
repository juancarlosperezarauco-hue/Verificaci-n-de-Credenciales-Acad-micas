import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { cycleError } from '../academic/academic.api';
interface Notification {
  id: string;
  kind: string;
  resourceId: string;
  title: string;
  createdAt: string;
  readAt: string | null;
}
interface Inbox {
  items: Notification[];
  total: number;
  unread: number;
  page: number;
  size: number;
}
@Component({
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './notifications.page.html',
})
export class NotificationsPage {
  private readonly http = inject(HttpClient);
  private readonly destroy = inject(DestroyRef);
  readonly inbox = signal<Inbox>({ items: [], total: 0, unread: 0, page: 0, size: 20 });
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly ready = signal(false);
  unreadOnly = false;
  constructor() {
    this.load();
  }
  load(page = 0) {
    if (this.loading()) return;
    this.loading.set(true);
    this.ready.set(false);
    this.error.set('');
    this.http
      .get<Inbox>('/api/me/notifications', {
        params: { page, size: 20, unreadOnly: this.unreadOnly },
      })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe({
        next: (r) => {
          this.inbox.set(r);
          this.ready.set(true);
        },
        error: (e) => this.error.set(cycleError(e)),
      });
  }
  read(item?: Notification) {
    if (this.busy() || this.loading()) return;
    this.busy.set(true);
    this.error.set('');
    const request = item
      ? this.http.patch<void>('/api/me/notifications/' + item.id + '/read', {})
      : this.http.post<void>('/api/me/notifications/read-all', {});
    request
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.busy.set(false)),
      )
      .subscribe({
        next: () => this.load(this.unreadOnly ? 0 : this.inbox().page),
        error: (e) => this.error.set(cycleError(e)),
      });
  }
}
