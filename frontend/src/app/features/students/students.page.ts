import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Student } from '../../core/models';
import { OrganizationApi } from '../organization/organization.api';
import { UiIconComponent } from '../../shared/ui-icon.component';

@Component({
  imports: [ReactiveFormsModule, FormsModule, RouterLink, UiIconComponent],
  templateUrl: './students.page.html',
})
export class StudentsPage implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly access = inject(OrganizationApi);
  readonly students = signal<Student[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly loadError = signal('');
  readonly success = signal('');
  readonly query = signal('');
  readonly page = signal(0);
  readonly filtered = computed(() => {
    const query = this.query()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .trim()
      .toLowerCase();
    return this.students().filter((student) =>
      [student.fullName, student.documentNumber, student.documentType, student.country]
        .join(' ')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase()
        .includes(query),
    );
  });
  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / 10)));
  readonly paged = computed(() => this.filtered().slice(this.page() * 10, this.page() * 10 + 10));
  readonly lastItem = computed(() => Math.min((this.page() + 1) * 10, this.filtered().length));
  readonly form = inject(FormBuilder).nonNullable.group({
    code: [
      '',
      [Validators.required, Validators.pattern('\\s*([Ee][Ss][Tt]-)?[a-fA-F0-9]{32}\\s*')],
    ],
  });
  ngOnInit() {
    this.reload();
  }
  initials(name: string) {
    return name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((word) => word[0] ?? '')
      .join('')
      .toUpperCase();
  }
  previous() {
    this.page.update((page) => Math.max(0, page - 1));
  }
  next() {
    this.page.update((page) => Math.min(this.pageCount() - 1, page + 1));
  }
  create() {
    if (this.form.invalid || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.success.set('');
    this.api
      .redeemStudent(this.form.getRawValue().code.trim().toUpperCase())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (student) => {
          const students = [...this.students().filter((s) => s.id !== student.id), student].sort(
            (a, b) => a.fullName.localeCompare(b.fullName),
          );
          this.students.set(students);
          this.query.set('');
          this.page.set(Math.floor(students.findIndex((s) => s.id === student.id) / 10));
          this.form.reset();
          this.loadError.set('');
          this.success.set(student.fullName + ' ya está registrado en tu institución.');
        },
        error: (e) => this.error.set(e.error?.message ?? 'No se pudo consultar el código.'),
      });
  }
  reload() {
    this.loading.set(true);
    this.loadError.set('');
    this.page.set(0);
    this.api
      .listStudents()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (students) => this.students.set(students),
        error: () => {
          this.students.set([]);
          this.loadError.set(
            'No se pudo cargar el padrón institucional. Vuelve a intentarlo con Actualizar.',
          );
        },
      });
  }
}
