import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { StudentsPage } from './students.page';
import { OrganizationApi } from '../organization/organization.api';

describe('Student search code registration', () => {
  const student = {
    id: 'student-1',
    fullName: 'Ana Pérez',
    country: 'BO',
    documentType: 'CI',
    documentNumber: '123',
  };
  let api: { listStudents: jasmine.Spy; redeemStudent: jasmine.Spy };
  beforeEach(async () => {
    api = {
      listStudents: jasmine.createSpy().and.returnValue(of([student])),
      redeemStudent: jasmine.createSpy().and.returnValue(of(student)),
    };
    await TestBed.configureTestingModule({
      imports: [StudentsPage],
      providers: [
        provideRouter([]),
        { provide: OrganizationApi, useValue: { can: () => true } },
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: { current: () => ({ institutionName: 'Colegio' }) } },
      ],
    }).compileComponents();
  });
  it('accepts a pasted permanent code with whitespace and normalizes it', () => {
    const fixture = TestBed.createComponent(StudentsPage);
    fixture.detectChanges();
    const code = 'EST-' + 'A'.repeat(32);
    fixture.componentInstance.form.controls.code.setValue('  ' + code.toLowerCase() + '  ');
    expect(fixture.componentInstance.form.valid).toBeTrue();
    fixture.componentInstance.create();
    expect(api.redeemStudent).toHaveBeenCalledWith(code);
    expect(fixture.componentInstance.students().length).toBe(1);
    expect(fixture.componentInstance.success()).toContain('Ana Pérez');
  });
  it('keeps temporary codes compatible and rejects incomplete search codes', () => {
    const page = TestBed.createComponent(StudentsPage).componentInstance;
    page.form.controls.code.setValue('B'.repeat(32));
    expect(page.form.valid).toBeTrue();
    page.form.controls.code.setValue('EST-123');
    expect(page.form.invalid).toBeTrue();
    page.create();
    expect(api.redeemStudent).not.toHaveBeenCalled();
  });
  it('searches accented names, documents and countries and bounds the visible page', () => {
    const fixture = TestBed.createComponent(StudentsPage);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    page.students.set(
      Array.from({ length: 23 }, (_, i) => ({
        ...student,
        id: String(i),
        fullName: i === 22 ? 'José Pérez' : 'Ana ' + i,
        documentNumber: String(i),
      })),
    );
    expect(page.paged().length).toBe(10);
    page.next();
    page.next();
    page.next();
    expect(page.page()).toBe(2);
    expect(page.paged().length).toBe(3);
    page.query.set('jose');
    page.page.set(0);
    expect(page.filtered().length).toBe(1);
    expect(page.paged()[0].fullName).toBe('José Pérez');
    page.query.set('BO');
    expect(page.filtered().length).toBe(23);
    page.query.set('22');
    expect(page.filtered().length).toBe(1);
  });
  it('reports a failed lookup instead of presenting an empty institution as fact', () => {
    api.listStudents.and.returnValue(throwError(() => ({ status: 503 })));
    const fixture = TestBed.createComponent(StudentsPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'No se pudo cargar',
    );
    expect(fixture.nativeElement.textContent).not.toContain('Aún no hay estudiantes vinculados');
  });
});
