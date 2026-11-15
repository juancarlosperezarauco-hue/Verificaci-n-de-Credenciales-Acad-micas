import { Component, Input, OnChanges, OnDestroy, inject, output, signal } from '@angular/core';
import { Subscription, forkJoin } from 'rxjs';
import { IdentityReviewApi, ImagePart } from './identity-review.api';

@Component({selector:'app-identity-preview',template:`
 <section class="org-card mt-5" aria-label="Documentos privados de identidad">
  <div class="flex flex-wrap items-center justify-between gap-3"><h3 class="font-bold">Documentos compartidos</h3><button type="button" class="org-secondary" (click)="close.emit()">Cerrar imágenes</button></div>
  <p class="mt-3 text-sm text-slate-600">Contenido privado para esta revisión. Evita compartirlo fuera del trámite.</p>
  @if(loading()){<p role="status" class="mt-4">Cargando documentos…</p>}
  @if(error()){<p role="alert" class="org-error mt-4">{{error()}}</p>}
  <div class="mt-5 grid gap-5 md:grid-cols-2">@for(image of images();track image.part){<figure class="min-w-0 rounded-xl border border-slate-200 p-3"><figcaption class="mb-3 text-sm font-semibold">{{image.label}}</figcaption><img [src]="image.url" [alt]="image.label" class="max-h-96 w-full rounded-lg object-contain" referrerpolicy="no-referrer"></figure>}</div>
 </section>`})
export class IdentityPreviewComponent implements OnChanges,OnDestroy {
 @Input({required:true}) submissionId='';@Input() own=false;@Input() photoPresent=false;
 readonly close=output<void>();readonly images=signal<{part:ImagePart;label:string;url:string}[]>([]);
 readonly loading=signal(false);readonly error=signal('');private readonly api=inject(IdentityReviewApi);private pending?:Subscription;
 ngOnChanges(){this.clear();this.error.set('');this.loading.set(true);const parts:ImagePart[]=this.photoPresent?['FRONT','BACK','PHOTO']:['FRONT','BACK'];this.pending=forkJoin(parts.map(part=>this.api.file(this.own,this.submissionId,part))).subscribe({next:blobs=>{this.images.set(blobs.map((blob,i)=>({part:parts[i],label:parts[i]==='FRONT'?'Frente del documento':parts[i]==='BACK'?'Reverso del documento':'Fotografía opcional',url:URL.createObjectURL(blob)})));this.loading.set(false);},error:()=>{this.clear();this.loading.set(false);this.error.set('No se pudieron abrir los documentos. Comprueba que la autorización siga vigente.');}});}
 private clear(){this.pending?.unsubscribe();this.images().forEach(i=>URL.revokeObjectURL(i.url));this.images.set([]);}
 ngOnDestroy(){this.clear();}
}
