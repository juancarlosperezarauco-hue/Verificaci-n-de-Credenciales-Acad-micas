import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { OrganizationApi, requestError } from './organization.api';
import { OrganizationSummary, OrganizationStatus, STATUS_LABELS } from './organization.models';
@Component({imports:[FormsModule,RouterLink],template:`
 <section class="org-page"><div class="flex flex-wrap items-start justify-between gap-4"><div><p class="org-eyebrow">Superadministración</p><h1 class="org-title">Organizaciones</h1><p class="org-description">Consulta las instituciones registradas y administra su estado de acceso.</p></div><button class="org-secondary" (click)="load()" [disabled]="loading()">Actualizar</button></div>
 <div class="my-8 grid gap-4 sm:grid-cols-3"><div class="org-card"><p class="text-sm text-slate-500">Instituciones registradas</p><p class="mt-3 text-3xl font-bold">{{organizations().length}}</p></div><div class="org-card"><p class="text-sm text-slate-500">Activas</p><p class="mt-3 text-3xl font-bold text-emerald-700">{{activeCount()}}</p></div><div class="org-card"><p class="text-sm text-slate-500">Pendientes de revisión</p><p class="mt-3 text-3xl font-bold text-amber-700">{{pendingCount()}}</p></div></div>
 <section class="org-card"><div class="mb-6 grid gap-4 sm:grid-cols-[2fr_1fr]"><label class="org-label">Buscar organización<input class="org-input" placeholder="Nombre, identificador o tipo" [ngModel]="query()" (ngModelChange)="query.set($event);page.set(0)"></label><label class="org-label">Estado<select class="org-input" [ngModel]="status()" (ngModelChange)="status.set($event);page.set(0)"><option value="">Todos los estados</option>@for(s of states;track s){<option [value]="s">{{labels[s]}}</option>}</select></label></div>
 @if(error()){<p class="org-error" role="alert">{{error()}}</p>}
 @if(loading()){<p role="status" class="py-12 text-center text-slate-500">Cargando organizaciones…</p>}@else{
 @if(!filtered().length){<div class="py-14 text-center"><h2 class="font-semibold">No se encontraron organizaciones</h2><p class="mt-2 text-sm text-slate-500">Prueba con otro nombre o estado.</p></div>}@else{
 <div class="overflow-x-auto"><table class="org-table"><thead><tr><th>Organización</th><th>Tipo</th><th>Estado</th><th><span class="sr-only">Acciones</span></th></tr></thead><tbody>@for(org of paged();track org.id){<tr><td><strong>{{org.name}}</strong><p class="text-xs text-slate-500">{{org.slug}}</p></td><td>{{org.institutionType.replaceAll('_',' ')}}</td><td><span class="org-badge" [attr.data-status]="org.status">{{labels[org.status]}}</span></td><td><a class="org-link whitespace-nowrap" [routerLink]="['/superadmin/organizations',org.id]">Ver detalle →</a></td></tr>}</tbody></table></div>
 <div class="mt-5 flex flex-wrap items-center justify-between gap-3 text-sm"><p class="text-slate-500">{{filtered().length}} resultados · Página {{page()+1}} de {{pageCount()}}</p><div class="flex gap-2"><button class="org-secondary" [disabled]="page()===0" (click)="page.update(decrement)">Anterior</button><button class="org-secondary" [disabled]="page()+1>=pageCount()" (click)="page.update(increment)">Siguiente</button></div></div>
 }}
 </section></section>`})
export class PlatformOrganizationsPage {
 private readonly api=inject(OrganizationApi);readonly organizations=signal<OrganizationSummary[]>([]);readonly loading=signal(true);readonly error=signal('');
 readonly labels=STATUS_LABELS;readonly states=Object.keys(STATUS_LABELS) as OrganizationStatus[];readonly query=signal('');readonly status=signal('');readonly page=signal(0);
 readonly filtered=computed(()=>this.organizations().filter(o=>(!this.status()||o.status===this.status())&&[o.name,o.slug,o.institutionType].join(' ').toLowerCase().includes(this.query().toLowerCase().trim())));
 readonly pageCount=computed(()=>Math.max(1,Math.ceil(this.filtered().length/10)));readonly paged=computed(()=>this.filtered().slice(this.page()*10,this.page()*10+10));
 readonly activeCount=computed(()=>this.organizations().filter(o=>o.active).length);readonly pendingCount=computed(()=>this.organizations().filter(o=>o.status==='PENDING_REVIEW').length);
 readonly increment=(n:number)=>n+1;readonly decrement=(n:number)=>n-1;
 constructor(){this.load();}
 load(){this.loading.set(true);this.error.set('');this.api.organizations().pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.organizations.set(r);this.page.set(0);},error:e=>this.error.set(requestError(e))});}
}
