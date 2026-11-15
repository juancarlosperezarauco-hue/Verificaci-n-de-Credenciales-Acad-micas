import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';
import { ACTION_LABELS, AuditApi, AuditItem } from './audit.api';
import { requestError } from '../organization/organization.api';
const uuid=/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
@Component({imports:[DatePipe,KeyValuePipe,ReactiveFormsModule],template:`
 <section class="org-page max-w-6xl"><p class="org-eyebrow">{{global?'Superadministración':'Historial institucional'}}</p><h1 class="org-title">Auditoría de acciones</h1><p class="org-description">{{global?'Consulta acciones globales y de las organizaciones.':'Consulta únicamente las acciones de tu organización.'}} Los registros conservan quién realizó cada operación y cuándo ocurrió.</p>
 <form class="org-card mt-7 grid gap-4 sm:grid-cols-2 lg:grid-cols-3" [formGroup]="form" (ngSubmit)="apply()">
 <label class="org-label">Acción<select class="org-input" formControlName="action"><option value="">Todas las acciones</option>@for(option of labels|keyvalue;track option.key){<option [value]="option.key">{{option.value}}</option>}</select></label>
 <label class="org-label">Desde<input type="date" class="org-input" formControlName="from"></label><label class="org-label">Hasta<input type="date" class="org-input" formControlName="to"></label>
 <label class="org-label">Identificador del actor<input class="org-input" formControlName="actorId" placeholder="UUID del usuario" maxlength="36"></label><label class="org-label">Identificador del recurso<input class="org-input" formControlName="resourceId" placeholder="UUID del recurso" maxlength="36"></label>
 @if(global){<label class="org-label">Identificador de organización<input class="org-input" formControlName="tenantId" placeholder="UUID de la organización" maxlength="36"></label>}
 <div class="flex items-end gap-3"><button class="org-primary" [disabled]="loading()">Aplicar filtros</button><button type="button" class="org-secondary" [disabled]="loading()" (click)="clear()">Limpiar</button></div></form>
 @if(error()){<p role="alert" class="org-error mt-6">{{error()}}</p><button class="org-secondary mt-3" [disabled]="loading()" (click)="load()">Reintentar consulta</button>}
 @if(loading()){<p role="status" class="mt-6 text-slate-600">Consultando registros…</p>}
 @if(ready()){
 <p class="mt-6 text-sm text-slate-600">{{total()}} registros · Página {{page()+1}}</p>
 @for(item of items();track item.id){<article class="org-card mt-4"><div class="flex flex-wrap items-start justify-between gap-3"><h2 class="font-bold">{{labels[item.action]||item.action}}</h2><time class="text-sm text-slate-500">{{item.occurredAt|date:'dd/MM/yyyy HH:mm:ss'}}</time></div><dl class="mt-4 grid gap-4 text-sm sm:grid-cols-2"><div><dt class="text-slate-500">Actor · {{item.actorRole}}</dt><dd class="mt-1 break-all font-mono">{{item.actorId||'Proceso del sistema / solicitud pública'}}</dd></div><div><dt class="text-slate-500">Recurso · {{item.resourceType}}</dt><dd class="mt-1 break-all font-mono">{{item.resourceId}}</dd></div>@if(global){<div><dt class="text-slate-500">Organización</dt><dd class="mt-1 break-all font-mono">{{item.tenantId||'Ámbito global'}}</dd></div>}<div><dt class="text-slate-500">Referencia de la operación</dt><dd class="mt-1 break-all font-mono">{{item.correlationId}}</dd></div></dl>@if(item.changes){<details class="mt-4"><summary class="org-link cursor-pointer">Ver cambios registrados</summary><p class="mt-3 whitespace-pre-wrap break-all rounded-lg bg-slate-50 p-3 font-mono text-xs">{{item.changes}}</p></details>}</article>}
 @if(!items().length){<div class="org-card mt-4 text-slate-600">No hay registros con estos filtros.</div>}
 <div class="mt-6 flex justify-between gap-4"><button class="org-secondary" [disabled]="loading()||page()===0" (click)="move(-1)">Anterior</button><button class="org-secondary" [disabled]="loading()||(page()+1)*20>=total()" (click)="move(1)">Siguiente</button></div>}
 </section>`})
export class AuditPage implements OnInit{
 private readonly api=inject(AuditApi);readonly global=inject(ActivatedRoute).snapshot.data['global']===true;readonly labels=ACTION_LABELS;
 readonly form=inject(FormBuilder).nonNullable.group({action:[''],from:[''],to:[''],actorId:['',Validators.pattern(uuid)],resourceId:['',Validators.pattern(uuid)],tenantId:['',Validators.pattern(uuid)]});
 readonly items=signal<AuditItem[]>([]);readonly total=signal(0);readonly page=signal(0);readonly loading=signal(false);readonly ready=signal(false);readonly error=signal('');
 private filters:Record<string,string|number>={};
 ngOnInit(){this.load();}
 apply(){this.error.set('');if(this.form.invalid){this.error.set('Los identificadores deben ser UUID válidos.');return;}const v=this.form.getRawValue();if(v.from&&v.to&&v.from>v.to){this.error.set('La fecha inicial no puede ser posterior a la final.');return;}const before=v.to?new Date(v.to+'T00:00:00'):null;if(before)before.setDate(before.getDate()+1);this.filters={action:v.action,actorId:v.actorId,resourceId:v.resourceId,from:v.from?new Date(v.from+'T00:00:00').toISOString():'',before:before?.toISOString()??''};if(this.global)this.filters['tenantId']=v.tenantId;this.page.set(0);this.load();}
 clear(){this.form.reset();this.filters={};this.page.set(0);this.load();}
 move(delta:number){if(this.loading()||this.page()+delta<0)return;this.page.update(p=>p+delta);this.load();}
 load(){if(this.loading())return;this.loading.set(true);this.ready.set(false);this.error.set('');this.api.events(this.global,{...this.filters,page:this.page(),size:20}).pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.items.set(r.items);this.total.set(r.total);this.ready.set(true);},error:e=>{this.items.set([]);this.error.set(requestError(e));}});}
}
