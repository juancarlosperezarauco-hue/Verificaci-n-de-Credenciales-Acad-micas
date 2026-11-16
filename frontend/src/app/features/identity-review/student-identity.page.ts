import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { IdentityReview, IdentityReviewApi, IdentityState, ImagePart, REVIEW_LABELS } from './identity-review.api';
import { IdentityPreviewComponent } from './identity-preview.component';

@Component({imports:[DatePipe,ReactiveFormsModule,RouterLink,IdentityPreviewComponent],template:`
 <section class="org-page max-w-5xl">
  <p class="org-eyebrow">Mi identidad</p><h1 class="org-title">Verifica tus datos con tu institución</h1>
  <p class="org-description">Envía tus documentos solo a una institución que hayas vinculado. Cada organización revisa sus propios envíos; tu cuenta sigue siendo la misma.</p>
  @if(error()){<p role="alert" class="org-error mt-6">{{error()}}</p>}
  @if(notice()){<p role="status" class="org-success mt-6">{{notice()}}</p>}
  @if(loading()){<p role="status" class="mt-6">Cargando tus verificaciones…</p>}
  @if(state();as data){
   @if(!data.profileComplete||!data.emailVerified){<div class="org-info mt-6"><p>Antes de enviar documentos, completa tu perfil y confirma tu correo electrónico.</p><div class="mt-3 flex flex-wrap gap-4"><a routerLink="/student/account" class="org-link">Completar perfil</a><a routerLink="/student/security" class="org-link">Confirmar correo</a></div></div>}
   @if(!data.institutions.length){<div class="org-card mt-6"><h2 class="text-xl font-bold">Todavía no tienes una institución disponible</h2><p class="mt-3 text-slate-600">Comparte tu código EST para que una institución aprobada te vincule. Ese vínculo no comparte tus imágenes automáticamente.</p><a routerLink="/student/account" class="org-link mt-4 inline-block">Ver mi código y autorizaciones</a></div>}
   @if(data.institutions.length&&data.profileComplete&&data.emailVerified){
    <form #uploadForm class="org-card mt-8" [formGroup]="form" (ngSubmit)="submit()">
     <h2 class="text-xl font-bold">Nuevo envío</h2>
     <label class="org-label mt-5">Institución destinataria<select class="org-input" formControlName="tenantId"><option value="">Selecciona una institución</option>@for(target of data.institutions;track target.id){<option [value]="target.id">{{target.name}}</option>}</select></label>
     <p class="org-hint">Se revisarán los datos actuales de tu perfil. Si luego cambian, tendrás que enviar una nueva revisión.</p>
     <fieldset [disabled]="busy()" class="mt-5 grid gap-5 sm:grid-cols-2"><legend class="mb-3 text-sm font-semibold">Imágenes del documento</legend>
      <label class="org-label">Frente del documento<input type="file" class="org-input" accept="image/jpeg,image/png" (change)="choose('FRONT',$event)" required><span class="org-hint">Obligatorio, legible y con todos los bordes visibles.</span></label>
      <label class="org-label">Reverso del documento<input type="file" class="org-input" accept="image/jpeg,image/png" (change)="choose('BACK',$event)" required><span class="org-hint">Obligatorio. No incluyas documentos de otras personas.</span></label>
      <label class="org-label sm:col-span-2">Fotografía (opcional)<input type="file" class="org-input" accept="image/jpeg,image/png" (change)="choose('PHOTO',$event)"></label>
     </fieldset>
     <p class="org-hint">JPEG o PNG, hasta 5 MiB por imagen, 12 megapíxeles y 6000 píxeles por lado. La fotografía no se publicará como avatar.</p>
     <label class="mt-6 flex items-start gap-3 rounded-xl border border-blue-200 bg-blue-50 p-4 text-sm leading-relaxed"><input type="checkbox" class="mt-1 shrink-0" formControlName="accepted"><span>Autorizo a la institución seleccionada a consultar estos documentos y mis datos personales para revisar mi identidad. Puedo retirar el acceso desde el historial. La retirada no elimina copias ya consultadas.</span></label>
     <button type="submit" class="org-primary mt-5" [disabled]="busy()||form.invalid||!files.FRONT||!files.BACK">{{busy()?'Enviando…':'Enviar a revisión'}}</button>
    </form>
   }
   <section class="mt-10"><h2 class="text-2xl font-bold">Mis envíos</h2><p class="mt-2 text-sm text-slate-600">La verificación es por institución y para los datos que enviaste. No es una acreditación gubernamental.</p>
   @for(item of data.submissions;track item.id){<article class="org-card mt-5">
    <div class="flex flex-wrap items-start justify-between gap-3"><div><h3 class="font-bold">{{item.institutionName}}</h3><p class="mt-2 text-sm text-slate-500">Enviado el {{item.submittedAt|date:'dd/MM/yyyy HH:mm'}}</p></div><span class="org-badge" [attr.data-status]="item.effectiveStatus">{{labels[item.effectiveStatus]}}</span></div>
    @if(item.profileChanged){<p class="org-info mt-4">Tu perfil cambió después de este envío. Envía documentos actualizados para volver a verificarlo.</p>}
    @if(!item.sharingActive){<p class="mt-4 text-sm text-slate-600">La institución ya no tiene acceso a este envío.</p>}
    @if(item.reason){<p class="mt-4 whitespace-pre-wrap rounded-xl bg-red-50 p-4 text-sm text-red-800"><strong>Motivo de rechazo:</strong> {{item.reason}}</p>}
    @if(item.reviewedAt){<p class="mt-3 text-sm text-slate-500">Revisado el {{item.reviewedAt|date:'dd/MM/yyyy HH:mm'}}</p>}
    <div class="mt-5 flex flex-wrap gap-3">@if(item.documentsAvailable!==false){<button type="button" class="org-secondary" (click)="preview.set(item)">Ver mis imágenes</button>}@else{<p class="text-sm text-slate-500">Evidencia retirada según el plazo de conservación.</p>}@if(item.sharingActive){<button type="button" class="org-secondary" [disabled]="busy()" (click)="withdrawing.set(item)">Retirar acceso</button>}</div>
   </article>}@empty{<p class="org-card mt-5 text-slate-600">Sin verificar. Todavía no has enviado documentos.</p>}
   </section>
  }
  @if(withdrawing();as item){<section class="org-card mt-6 border-red-200" role="region" aria-label="Confirmar retirada"><h2 class="font-bold">¿Retirar el acceso de {{item.institutionName}}?</h2><p class="mt-3 text-sm text-slate-600">No podrá seguir consultando ni revisando este envío. Tus credenciales académicas se conservan.</p><div class="mt-5 flex flex-wrap gap-3"><button class="org-secondary" [disabled]="busy()" (click)="withdrawing.set(null)">Cancelar</button><button class="org-danger" [disabled]="busy()" (click)="withdraw()">Confirmar retirada</button></div></section>}
  @if(preview();as item){<app-identity-preview [submissionId]="item.id" [own]="true" [photoPresent]="item.photoPresent" (close)="preview.set(null)"/>}
 </section>`})
export class StudentIdentityPage {
 readonly api=inject(IdentityReviewApi);readonly state=signal<IdentityState|null>(null);readonly loading=signal(false);readonly busy=signal(false);
 readonly error=signal('');readonly notice=signal('');readonly preview=signal<IdentityReview|null>(null);readonly withdrawing=signal<IdentityReview|null>(null);readonly labels=REVIEW_LABELS;
 readonly form=inject(FormBuilder).nonNullable.group({tenantId:['',Validators.required],accepted:[false,Validators.requiredTrue]});
 @ViewChild('uploadForm') uploadForm?:ElementRef<HTMLFormElement>;
 files:Record<ImagePart,File|null>={FRONT:null,BACK:null,PHOTO:null};
 constructor(){this.load();this.form.controls.tenantId.valueChanges.subscribe(()=>this.form.controls.accepted.setValue(false));}
 load(){this.loading.set(true);this.api.own().pipe(finalize(()=>this.loading.set(false))).subscribe({next:data=>this.state.set(data),error:()=>this.error.set('No se pudo cargar el historial. Recarga la página para intentarlo de nuevo.')});}
 choose(part:ImagePart,event:Event){this.error.set('');const input=event.target as HTMLInputElement,file=input.files?.[0]??null;this.files[part]=null;if(file&&(file.size>5*1024*1024||!file.size||!['image/jpeg','image/png'].includes(file.type))){input.value='';this.error.set('Selecciona una imagen JPEG o PNG de hasta 5 MiB.');return;}this.files[part]=file;}
 submit(){if(this.busy()||this.form.invalid||!this.files.FRONT||!this.files.BACK)return;this.busy.set(true);this.error.set('');this.notice.set('');this.api.submit(this.form.controls.tenantId.value,this.files.FRONT,this.files.BACK,this.files.PHOTO).pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.files={FRONT:null,BACK:null,PHOTO:null};this.uploadForm?.nativeElement.reset();this.form.reset();this.notice.set('Documentos enviados. Puedes seguir la revisión en el historial.');this.load();},error:e=>this.error.set(e.status===409?'Ya hay una revisión pendiente o vigente para estos datos. Revisa tu historial.':e.error?.message??'No se pudo enviar. Comprueba las imágenes y vuelve a intentarlo.')});}
 withdraw(){const item=this.withdrawing();if(!item||this.busy())return;this.busy.set(true);this.error.set('');this.notice.set('');this.api.withdraw(item.id).pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.preview.set(null);this.withdrawing.set(null);this.notice.set('Acceso retirado.');this.load();},error:()=>this.error.set('No se pudo retirar el acceso. Inténtalo de nuevo.')});}
}
