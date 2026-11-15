import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, KeyValuePipe } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { AuditApi, MailItem, MAIL_LABELS, MAIL_KINDS, MAIL_ERRORS } from './audit.api';
import { requestError } from '../organization/organization.api';
@Component({imports:[DatePipe,KeyValuePipe,ReactiveFormsModule],template:`
 <section class="org-page max-w-6xl"><p class="org-eyebrow">Operación de la plataforma</p><h1 class="org-title">Seguimiento de correos</h1><p class="org-description">Consulta el estado de los correos de seguridad y sus reintentos. La aceptación por SMTP no confirma que el mensaje haya llegado al buzón.</p>
 <div class="org-card mt-7 flex flex-wrap items-end gap-4"><label class="org-label min-w-56">Estado<select class="org-input" [formControl]="state"><option value="">Todos los estados</option>@for(option of labels|keyvalue;track option.key){<option [value]="option.key">{{option.value}}</option>}</select></label><button class="org-primary" [disabled]="loading()" (click)="apply()">Consultar</button><button class="org-secondary" [disabled]="loading()" (click)="load()">Actualizar</button></div>
 @if(error()){<p role="alert" class="org-error mt-6">{{error()}}</p><button class="org-secondary mt-3" (click)="load()">Reintentar consulta</button>}
 @if(loading()){<p role="status" class="mt-6 text-slate-600">Consultando cola…</p>}
 @if(ready()){<p class="mt-6 text-sm text-slate-600">{{total()}} registros · Página {{page()+1}}</p>
 @for(item of items();track item.id){<article class="org-card mt-4"><div class="flex flex-wrap items-center justify-between gap-3"><h2 class="font-bold">{{kinds[item.kind]||item.kind}}</h2><span class="rounded-full bg-slate-100 px-3 py-1 text-sm font-medium">{{labels[item.state]||item.state}}</span></div><dl class="mt-4 grid gap-4 text-sm sm:grid-cols-2"><div><dt class="text-slate-500">Referencia del envío</dt><dd class="mt-1 break-all font-mono">{{item.id}}</dd></div><div><dt class="text-slate-500">Identificador del estudiante</dt><dd class="mt-1 break-all font-mono">{{item.studentId}}</dd></div><div><dt class="text-slate-500">Creación · intentos</dt><dd class="mt-1">{{item.createdAt|date:'dd/MM/yyyy HH:mm:ss'}} · {{item.attempts}}</dd></div>@if(item.state==='PENDING'||item.state==='RETRY'){<div><dt class="text-slate-500">Próximo intento</dt><dd class="mt-1">{{item.nextAttemptAt|date:'dd/MM/yyyy HH:mm:ss'}}</dd></div>}@if(item.sentAt){<div><dt class="text-slate-500">Aceptación SMTP</dt><dd class="mt-1">{{item.sentAt|date:'dd/MM/yyyy HH:mm:ss'}}</dd></div>}</dl>@if(item.lastError){<p class="mt-4 text-sm text-slate-600">{{errors[item.lastError]||'No se completó el envío.'}}</p>}@if(item.state==='FAILED'){<p class="mt-3 text-sm text-amber-800">Se agotaron los intentos. El estudiante puede solicitar un enlace nuevo.</p>}</article>}
 @if(!items().length){<div class="org-card mt-4 text-slate-600">No hay correos con este filtro.</div>}
 <div class="mt-6 flex justify-between gap-4"><button class="org-secondary" [disabled]="loading()||page()===0" (click)="move(-1)">Anterior</button><button class="org-secondary" [disabled]="loading()||(page()+1)*20>=total()" (click)="move(1)">Siguiente</button></div>}
 </section>`})
export class MailPage implements OnInit{
 private readonly api=inject(AuditApi);readonly labels=MAIL_LABELS;readonly kinds=MAIL_KINDS;readonly errors=MAIL_ERRORS;readonly state=new FormControl('',{nonNullable:true});private filter='';
 readonly items=signal<MailItem[]>([]);readonly total=signal(0);readonly page=signal(0);readonly loading=signal(false);readonly ready=signal(false);readonly error=signal('');
 ngOnInit(){this.load();}
 apply(){this.filter=this.state.value;this.page.set(0);this.load();}
 move(delta:number){if(this.loading()||this.page()+delta<0)return;this.page.update(p=>p+delta);this.load();}
 load(){if(this.loading())return;this.loading.set(true);this.ready.set(false);this.error.set('');this.api.mail({state:this.filter,page:this.page(),size:20}).pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.items.set(r.items);this.total.set(r.total);this.ready.set(true);},error:e=>{this.items.set([]);this.error.set(requestError(e));}});}
}
