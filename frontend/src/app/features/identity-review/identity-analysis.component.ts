import { Component,DestroyRef,inject,input,signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
interface Analysis {text:string;confidence:number;signals:{code:string;explanation:string}[];human_review_required:true;engine:string}
@Component({
  selector:'app-identity-analysis',
  imports:[DecimalPipe],
  template:`<section class="mt-5 rounded-xl border border-blue-100 bg-blue-50/40 p-4"><h3 class="font-semibold">Asistencia de lectura</h3>
  <p class="mt-2 text-sm text-slate-600">Extrae texto del CI en el servicio privado de OCR. La decisión de identidad sigue a cargo del revisor. Estas imágenes no se envían a Gemini.</p>
  @if(available()){<button type="button" class="org-secondary mt-4" [disabled]="loading()" (click)="analyze()">{{loading()?'Leyendo documentos…':'Extraer texto y revisar alertas'}}</button>}
  @else{<p class="mt-3 text-sm text-slate-500">La asistencia OCR no está habilitada. Puedes continuar con la revisión manual.</p>}
  @if(error()){<p role="alert" class="org-error mt-3">{{error()}}</p>}
  @if(result();as r){<p class="mt-4 text-sm">Confianza de lectura: {{r.confidence*100|number:'1.0-0'}} %. Este valor no representa autenticidad.</p>
    @for(s of r.signals;track s.code){<p class="org-info mt-3">{{s.explanation}}</p>}@empty{<p class="org-info mt-3">No se detectaron diferencias en estas comprobaciones. Confirma siempre los documentos originales.</p>}
    <details class="mt-4"><summary class="cursor-pointer text-sm font-semibold">Ver texto extraído (privado)</summary><p class="mt-3 whitespace-pre-wrap break-words text-sm">{{r.text}}</p></details>}
  </section>`
})
export class IdentityAnalysisComponent {
  readonly submissionId=input.required<string>();
  private readonly http=inject(HttpClient);private readonly destroy=inject(DestroyRef);
  readonly available=signal(false);readonly loading=signal(false);readonly error=signal('');readonly result=signal<Analysis|null>(null);
  constructor(){this.http.get<{configured:boolean}>('/api/institution/identity-analysis/status').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:r=>this.available.set(r.configured),error:()=>this.available.set(false)});}
  analyze(){
    if(!this.available()||this.loading())return;this.loading.set(true);this.error.set('');this.result.set(null);
    this.http.post<Analysis>('/api/institution/identity-verifications/'+this.submissionId()+'/analysis',{})
      .pipe(takeUntilDestroyed(this.destroy),finalize(()=>this.loading.set(false)))
      .subscribe({next:r=>this.result.set(r),error:e=>this.error.set(e.status===409?'El envío cambió. Actualiza la bandeja antes de analizar.':'No se pudo leer el documento. Puedes continuar con la revisión manual.')});
  }
}
