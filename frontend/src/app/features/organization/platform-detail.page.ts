import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Dialog, DialogModule } from '@angular/cdk/dialog';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { OrganizationApi, requestError } from './organization.api';
import { OrganizationDetails, OrganizationStatus, OrganizationSummary, STATUS_LABELS, StatusEvent } from './organization.models';
import { buildDetailsForm, DETAIL_SECTIONS, fillDetails } from './organization.form';
import { ConfirmDialogComponent } from './confirm-dialog.component';
@Component({imports:[RouterLink,ReactiveFormsModule,DatePipe,DialogModule],template:`
 <section class="org-page"><a routerLink="/superadmin/organizations" class="org-link">← Todas las organizaciones</a>
 @if(loading()){<p role="status" class="org-card mt-8">Cargando detalle…</p>}
 @if(error()){<p role="alert" class="org-error mt-6">{{error()}} <button class="org-link" (click)="load()">Reintentar</button></p>}
 @if(organization();as org){<div class="my-7 flex flex-wrap items-center justify-between gap-4"><div><p class="org-eyebrow">Expediente institucional</p><h1 class="org-title">{{org.name}}</h1><p class="mt-2 text-slate-500">{{org.slug}} · {{org.institutionType.replaceAll('_',' ')}}</p></div><span class="org-badge" [attr.data-status]="org.status">{{labels[org.status]}}</span></div>
 <div class="grid gap-6 xl:grid-cols-[1.4fr_1fr]"><div>
 @if(details()){ @for(section of sections;track section.title){<section class="org-card mb-5"><h2 class="mb-5 text-lg font-bold">{{section.title}}</h2><dl class="grid gap-5 sm:grid-cols-2">@for(field of section.fields;track field.key){<div><dt class="text-xs text-slate-500">{{field.label}}</dt><dd class="mt-1 break-words text-sm">{{display(field.key)}}</dd></div>}</dl></section>}}
 @else{<section class="org-card"><h2 class="font-semibold">Datos legales pendientes</h2><p class="mt-3 text-sm text-slate-500">Esta institución se registró con el formulario anterior y todavía no completó su información legal y de representante.</p></section>}
 </div><div class="space-y-5">
 <form class="org-card" [formGroup]="statusForm" (ngSubmit)="confirmStatus()"><h2 class="text-lg font-bold">Administrar estado</h2><p class="mt-2 text-sm text-slate-500">En revisión u observada, solo el administrador puede completar el perfil. La aprobación habilita las operaciones; suspensión, rechazo o desactivación bloquean el acceso.</p>
 <label class="org-label mt-5">Nuevo estado<select class="org-input" formControlName="status"><option [value]="org.status">{{labels[org.status]}} (actual)</option>@for(s of org.allowedTransitions??states;track s){@if(s!==org.status){<option [value]="s">{{labels[s]}}</option>}}</select></label>
 <label class="org-label mt-4">Motivo<textarea class="org-input min-h-28" formControlName="reason" maxlength="500" placeholder="Explica la revisión o decisión"></textarea></label>
 @if(statusForm.controls.reason.invalid&&statusForm.controls.reason.touched){<p class="mt-2 text-sm text-red-700">Escribe un motivo para registrar la decisión.</p>}
 @if(notice()){<p class="org-success mt-4" role="status">{{notice()}}</p>}
 <button class="org-primary mt-5 w-full" [disabled]="busy()||statusForm.controls.status.value===org.status">{{busy()?'Actualizando…':'Aplicar cambio'}}</button>
 </form>
 <section class="org-card"><h2 class="mb-5 text-lg font-bold">Historial de revisiones</h2>
 @if(!history().length){<p class="text-sm text-slate-500">Todavía no hay cambios de estado registrados.</p>}
 @for(event of history();track event.id){<article class="border-b border-slate-200 py-4 last:border-0"><p class="text-sm font-semibold">{{labels[event.previousStatus]}} → {{labels[event.status]}}</p><p class="mt-2 break-words text-sm text-slate-600">{{event.reason}}</p><time class="mt-2 block text-xs text-slate-500">{{event.createdAt|date:'dd/MM/yyyy HH:mm'}}</time></article>}
 </section></div></div>}
 </section>`})
export class PlatformDetailPage {
 private readonly api=inject(OrganizationApi);private readonly dialog=inject(Dialog);private readonly fb=inject(FormBuilder);
 private readonly id=inject(ActivatedRoute).snapshot.paramMap.get('id')!;
 readonly organization=signal<OrganizationSummary|null>(null);readonly details=signal<OrganizationDetails|null>(null);readonly history=signal<StatusEvent[]>([]);
 readonly loading=signal(true);readonly busy=signal(false);readonly error=signal('');readonly notice=signal('');readonly labels=STATUS_LABELS;
 readonly states=Object.keys(STATUS_LABELS) as OrganizationStatus[];readonly sections=DETAIL_SECTIONS;private readonly detailForm=buildDetailsForm();
 readonly statusForm=this.fb.nonNullable.group({status:['ACTIVE' as OrganizationStatus,Validators.required],reason:['',[Validators.required,Validators.maxLength(500),Validators.pattern(/\S/)] ]});
 constructor(){this.load();}
 display(key:string){const v=this.detailForm.get(key)?.value;const f=this.sections.flatMap(s=>s.fields).find(f=>f.key===key);const label=(x:string)=>f?.options?.find(o=>o.value===x)?.label??x;return Array.isArray(v)?v.map(label).join(', '):label(v||'Sin especificar');}
 load(){this.loading.set(true);this.error.set('');forkJoin({organization:this.api.organization(this.id),history:this.api.history(this.id),details:this.api.organizationDetails(this.id).pipe(catchError(e=>{if(e instanceof HttpErrorResponse&&e.status===404)return of(null);throw e;}))}).pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.organization.set(r.organization);this.details.set(r.details);this.history.set(r.history);if(r.details)fillDetails(this.detailForm,r.details);this.statusForm.controls.status.setValue(r.organization.status);},error:e=>this.error.set(requestError(e))});}
 confirmStatus(){this.statusForm.markAllAsTouched();if(this.statusForm.invalid||this.busy())return;const v=this.statusForm.getRawValue();
 this.dialog.open<boolean>(ConfirmDialogComponent,{data:{title:'Cambiar estado de la organización',message:'Se cambiará el estado a '+this.labels[v.status]+'. La decisión quedará registrada en el historial.'},ariaLabelledBy:'confirm-title',autoFocus:'first-tabbable',restoreFocus:true}).closed.subscribe(ok=>{if(!ok)return;this.busy.set(true);this.error.set('');this.notice.set('');this.api.changeStatus(this.id,v.status,v.reason.trim()).pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.statusForm.controls.reason.reset();this.notice.set('Estado actualizado.');this.load();},error:e=>this.error.set(requestError(e))});});}
}
