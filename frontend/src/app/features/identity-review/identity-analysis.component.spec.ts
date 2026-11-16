import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController,provideHttpClientTesting } from '@angular/common/http/testing';
import { IdentityAnalysisComponent } from './identity-analysis.component';
describe('Human identity review assistance',()=>{
  let http:HttpTestingController;
  beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});http=TestBed.inject(HttpTestingController);});
  afterEach(()=>http.verify());
  function create(enabled=true){const f=TestBed.createComponent(IdentityAnalysisComponent);f.componentRef.setInput('submissionId','submission');http.expectOne('/api/institution/identity-analysis/status').flush({configured:enabled});f.detectChanges();return f;}
  it('leaves manual review available when OCR has not been configured',()=>{const f=create(false);expect(f.nativeElement.textContent).toContain('revisión manual');f.componentInstance.analyze();http.expectNone(r=>r.method==='POST');});
  it('requests analysis only after the reviewer acts and never posts a decision',()=>{
    const f=create();http.expectNone(r=>r.method==='POST');f.componentInstance.analyze();f.componentInstance.analyze();
    http.expectOne('/api/institution/identity-verifications/submission/analysis').flush({text:'Lectura ficticia',confidence:0.8,signals:[],human_review_required:true,engine:'tesseract'});
    f.detectChanges();expect(f.nativeElement.textContent).toContain('no representa autenticidad');http.expectNone(r=>r.url.endsWith('/decision'));
  });
  it('does not keep a previous analysis when reanalysis fails',()=>{
    const f=create();f.componentInstance.analyze();http.expectOne('/api/institution/identity-verifications/submission/analysis').flush({text:'Lectura ficticia',confidence:0.9,signals:[],human_review_required:true,engine:'tesseract'});
    f.componentInstance.analyze();http.expectOne('/api/institution/identity-verifications/submission/analysis').flush({},{status:503,statusText:'Unavailable'});
    expect(f.componentInstance.result()).toBeNull();expect(f.componentInstance.error()).toContain('revisión manual');
  });
});
