import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { LandingPage } from './landing.page';

describe('LandingPage', () => {
  let fixture: ComponentFixture<LandingPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPage],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(LandingPage);
    fixture.detectChanges();
  });

  it('explains the product and its process', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Qué hacemos');
    expect(text).toContain('Lo que ofrecemos');
    expect(text).toContain('Cómo funciona');
    expect(text).toContain('Verificar credencial');
  });
});
