import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi } from '../organization/organization.api';
import { Credential, PortalItem, PortalItemType } from '../../core/models';
import { UiIconComponent } from '../../shared/ui-icon.component';
import { cycleError } from '../academic/academic.api';

interface DashboardSnapshot { linkedStudents:number|null;credentials:number|null;issued:number|null;drafts:number|null;recent:Credential[]|null;generatedAt:string }

@Component({
  imports: [RouterLink, ReactiveFormsModule, DatePipe, UiIconComponent],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.css',
})
export class DashboardPage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly destroy = inject(DestroyRef);
  readonly auth = inject(AuthService);
  readonly access = inject(OrganizationApi);
  readonly credentials = signal<Credential[]>([]);
  private readonly http = inject(HttpClient);
  readonly studentCount = signal<number|null>(null);
  readonly credentialCount = signal<number|null>(null);
  readonly portalItems = signal<PortalItem[]>([]);
  readonly loading = signal(false);
  readonly studentsReady = signal(false);
  readonly credentialsReady = signal(false);
  readonly portalReady = signal(false);
  readonly updatedAt = signal<Date | null>(null);
  readonly loadErrors = signal<string[]>([]);
  readonly publishing = signal(false);
  readonly removing = signal('');
  readonly showEditor = signal(false);
  readonly portalError = signal('');
  readonly issued = signal<number|null>(null);
  readonly drafts = signal<number|null>(null);
  readonly announcements = computed(() =>
    this.portalItems().filter((i) => i.itemType === 'ANNOUNCEMENT'),
  );
  readonly deadlines = computed(() =>
    this.portalItems()
      .filter(
        (i) =>
          i.itemType === 'DEADLINE' && new Date(i.eventAt ?? 0).getTime() >= Date.now() - 86400000,
      )
      .sort((a, b) => new Date(a.eventAt ?? 0).getTime() - new Date(b.eventAt ?? 0).getTime()),
  );
  readonly resources = computed(() => this.portalItems().filter((i) => i.itemType === 'RESOURCE'));
  readonly portalForm = inject(FormBuilder).nonNullable.group({
    itemType: ['ANNOUNCEMENT' as PortalItemType, Validators.required],
    title: ['', [Validators.required, Validators.maxLength(180)]],
    description: ['', Validators.maxLength(1200)],
    eventAt: [''],
    link: ['', Validators.maxLength(500)],
    priority: [0],
  });
  ngOnInit() {
    this.load();
  }
  load() {
    if (this.loading()) return;
    this.loading.set(true);
    this.loadErrors.set([]);
    this.studentsReady.set(false);
    this.credentialsReady.set(false);
    this.portalReady.set(false);
    forkJoin({
      snapshot: this.http.get<DashboardSnapshot>('/api/institution/dashboard').pipe(catchError(this.failed<DashboardSnapshot>('resumen institucional'))),
      portal: this.api
        .listPortalItems()
        .pipe(catchError(this.failed<PortalItem[]>('avisos y agenda'))),
    })
      .pipe(
        takeUntilDestroyed(this.destroy),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((result) => {
        const snapshot=result.snapshot;
        this.studentCount.set(snapshot?.linkedStudents??null);
        this.credentialCount.set(snapshot?.credentials??null);
        this.issued.set(snapshot?.issued??null);
        this.drafts.set(snapshot?.drafts??null);
        this.studentsReady.set(snapshot?.linkedStudents!=null);
        this.credentialsReady.set(snapshot?.credentials!=null);
        this.credentials.set(snapshot?.recent??[]);
        this.portalItems.set(result.portal ?? []);
        this.portalReady.set(result.portal !== null);
        this.updatedAt.set(new Date());
      });
  }
  private failed<T>(label: string) {
    return (_error: HttpErrorResponse) => {
      this.loadErrors.update((errors) => [...errors, label]);
      return of<T | null>(null);
    };
  }
  canManage() {
    return ['ADMIN', 'TENANT_ADMIN'].includes(this.auth.current()?.role ?? '');
  }
  toggleEditor() {
    this.showEditor.update((value) => !value);
  }
  publish() {
    if (this.portalForm.invalid || this.publishing() || !this.canManage()) return;
    const value = this.portalForm.getRawValue();
    if (value.itemType === 'DEADLINE' && !value.eventAt) {
      this.portalError.set('Indica la fecha y hora del evento.');
      return;
    }
    this.publishing.set(true);
    this.portalError.set('');
    this.api
      .createPortalItem({
        ...value,
        eventAt: value.eventAt ? new Date(value.eventAt).toISOString() : undefined,
      })
      .pipe(finalize(() => this.publishing.set(false)))
      .subscribe({
        next: (item) => {
          this.portalItems.update((items) => [item, ...items]);
          this.portalForm.reset({
            itemType: 'ANNOUNCEMENT',
            title: '',
            description: '',
            eventAt: '',
            link: '',
            priority: 0,
          });
          this.showEditor.set(false);
        },
        error: (e) => this.portalError.set(cycleError(e)),
      });
  }
  remove(item: PortalItem) {
    if (
      this.removing() ||
      !this.canManage() ||
      !window.confirm('¿Retirar “' + item.title + '” del portal?')
    )
      return;
    this.removing.set(item.id);
    this.portalError.set('');
    this.api
      .deletePortalItem(item.id)
      .pipe(finalize(() => this.removing.set('')))
      .subscribe({
        next: () => this.portalItems.update((items) => items.filter((i) => i.id !== item.id)),
        error: (e) => this.portalError.set(cycleError(e)),
      });
  }
  statusLabel(status: Credential['status']) {
    return { DRAFT: 'Borrador', ISSUED: 'Emitida', REVOKED: 'Revocada' }[status];
  }
}
