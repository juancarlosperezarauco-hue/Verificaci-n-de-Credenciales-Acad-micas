import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { NotificationsPage } from './notifications.page';
describe('Academic inbox', () => {
  let http: HttpTestingController;
  const item = {
    id: 'notification',
    kind: 'REQUEST_APPROVE',
    resourceId: 'request',
    title: 'Tu solicitud fue aprobada',
    createdAt: '2026-11-15T12:00:00Z',
    readAt: null,
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function create() {
    const f = TestBed.createComponent(NotificationsPage);
    http
      .expectOne('/api/me/notifications?page=0&size=20&unreadOnly=false')
      .flush({ items: [item], total: 1, unread: 1, page: 0, size: 20 });
    f.detectChanges();
    return f;
  }
  it('does not mark an event read merely by loading it', () => {
    const f = create();
    expect(f.nativeElement.textContent).toContain('Tu solicitud fue aprobada');
    http.expectNone((r) => r.method === 'PATCH');
  });
  it('refreshes unread counts after explicitly marking an event read and prevents double writes', () => {
    const page = create().componentInstance;
    page.read(item);
    page.read(item);
    http.expectOne('/api/me/notifications/notification/read').flush(null);
    http
      .expectOne('/api/me/notifications?page=0&size=20&unreadOnly=false')
      .flush({
        items: [{ ...item, readAt: '2026-11-15T13:00:00Z' }],
        total: 1,
        unread: 0,
        page: 0,
        size: 20,
      });
    expect(page.inbox().unread).toBe(0);
    expect(page.busy()).toBeFalse();
  });
  it('preserves the inbox when a read request fails', () => {
    const page = create().componentInstance;
    page.read();
    http
      .expectOne('/api/me/notifications/read-all')
      .flush({}, { status: 503, statusText: 'Offline' });
    expect(page.inbox().unread).toBe(1);
    expect(page.error()).toBeTruthy();
  });
});
