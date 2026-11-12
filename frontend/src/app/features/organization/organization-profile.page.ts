import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { OrganizationApi, requestError } from './organization.api';
import { buildDetailsForm, DETAIL_SECTIONS, fillDetails, toDetails } from './organization.form';
import { OrganizationFieldsComponent } from './organization-fields.component';
import { STATUS_LABELS, OrganizationSummary } from './organization.models';
@Component({imports:[ReactiveFormsModule,RouterLink,OrganizationFieldsComponent],template:`
 <section class="org-page max-w-5xl"><div class="flex flex-wrap items-start justify-between gap-4"><div><p class="org-eyebrow">Administración institucional</p><h1 class="org-title">Tu organización</h1><p class="org-description">Mantén actualizados sus datos legales y de contacto.</p></div>
 @if(isAdmin()){<a class="org-secondary" routerLink="/organization/team">Usuarios y roles →</a>}</div>
 @if(organization();as org){<div class="org-card my-6 flex flex-wrap items-center justify-between gap-3"><div><h2 class="font-semibold">{{org.name}}</h2><p class="text-sm text-slate-500">Acceso: {{org.slug}}</p></div><span class="org-badge" [attr.data-status]="org.status">{{labels[org.status]}}</span></div>}
 @if(loading()){<p role="status" class="org-card mt-6">Cargando organización…</p>}
 @if(missing()){<p class="org-info mt-6">Tu organización aún no completó sus datos legales. Completa el formulario para registrarlos.</p>}
 @if(error()){<p class="org-error mt-6" role="alert">{{error()}} <button class="org-link" (click)="load()">Reintentar</button></p>}
 @if(!loading() && !failed()){
 <form [formGroup]="form" (ngSubmit)="save()" class="mt-6"><fieldset [disabled]="!api.can('ORGANIZATION_UPDATE')||saving()">
 @for(section of sections;track section.title;let i=$index){<section class="org-card mb-5"><p class="org-eyebrow">Paso {{i+1}}</p><h2 class="mb-6 mt-2 text-xl font-bold">{{section.title}}</h2><app-organization-fields [form]="form" [fields]="section.fields" [prefix]="'profile-'+i+'-'"/></section>}
 </fieldset>
 <p class="mb-5 text-sm text-slate-500">Cambiar el correo del representante no modifica el correo de inicio de sesión.</p>
 @if(notice()){<p role="status" class="org-success mb-4">{{notice()}}</p>}
 @if(api.can('ORGANIZATION_UPDATE')){<button type="submit" class="org-primary" [disabled]="saving()">{{saving()?'Guardando…':'Guardar datos institucionales'}}</button>}
 </form>}
 </section>`})
export class OrganizationProfilePage {
 readonly api=inject(OrganizationApi);readonly sections=DETAIL_SECTIONS;readonly labels=STATUS_LABELS;readonly form=buildDetailsForm();
 readonly loading=signal(true);readonly saving=signal(false);readonly missing=signal(false);readonly failed=signal(false);readonly error=signal('');readonly notice=signal('');
 readonly organization=signal<OrganizationSummary|null>(null);
 isAdmin(){return ['TENANT_ADMIN','ADMIN'].includes(this.api.access()?.role??'');}
 constructor(){this.load();}
 load(){this.loading.set(true);this.error.set('');this.failed.set(false);this.missing.set(false);
 this.api.myOrganization().subscribe({next:o=>this.organization.set(o),error:e=>this.error.set(requestError(e))});
 this.api.details().pipe(finalize(()=>this.loading.set(false))).subscribe({next:d=>fillDetails(this.form,d),error:e=>{
 if(e instanceof HttpErrorResponse && e.status===404){this.missing.set(true);}else{this.error.set(requestError(e));this.failed.set(true);}}});}
 save(){this.form.markAllAsTouched();if(this.form.invalid||this.saving())return;this.saving.set(true);this.error.set('');this.notice.set('');
 this.api.saveDetails(toDetails(this.form)).pipe(finalize(()=>this.saving.set(false))).subscribe({
 next:d=>{fillDetails(this.form,d);this.notice.set('Datos institucionales guardados.');this.api.myOrganization().subscribe(o=>this.organization.set(o));},error:e=>this.error.set(requestError(e))});}
}
