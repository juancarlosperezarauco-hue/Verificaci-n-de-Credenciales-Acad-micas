import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { authGuard, studentGuard } from './auth.guard';

describe('Access separated by account type', () => {
  let role: string | null;
  beforeEach(() => {
    role = null;
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: {
        isAuthenticated: () => role !== null,
        current: () => role ? { role } : null,
      }}],
    });
  });
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;
  it('sends anonymous users to the corresponding login', () => {
    TestBed.runInInjectionContext(() => {
      expect(String(authGuard(route,state))).toBe('/login');
      expect(String(studentGuard(route,state))).toBe('/student/login');
    });
  });
  it('keeps a student out of institution management', () => {
    role = 'STUDENT';
    TestBed.runInInjectionContext(() => {
      expect(studentGuard(route,state)).toBeTrue();
      expect(String(authGuard(route,state))).toBe('/student/account');
    });
  });
  it('keeps institutional staff out of the personal student account', () => {
    role = 'ADMIN';
    TestBed.runInInjectionContext(() => {
      expect(authGuard(route,state)).toBeTrue();
      expect(String(studentGuard(route,state))).toBe('/dashboard');
    });
  });
});
