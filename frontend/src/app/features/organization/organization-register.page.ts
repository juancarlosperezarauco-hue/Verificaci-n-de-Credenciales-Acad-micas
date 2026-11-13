import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi, requestError } from './organization.api';
import { ACCOUNT_FIELDS, buildDetailsForm, DETAIL_SECTIONS, toDetails } from './organization.form';
import { OrganizationFieldsComponent } from './organization-fields.component';
@Component({
 imports:[ReactiveFormsModule,RouterLink,OrganizationFieldsComponent],
 template:`
 <section class="org-page max-w-5xl">
 <div class="mb-8"><p class="org-eyebrow">Un espacio para cada organización educativa</p><h1 class="org-title">Registra tu organización educativa</h1>
 <p class="org-description">Para escuelas, colegios, universidades, institutos, academias y centros de formación. Completa los datos de tu organización y crea la cuenta de su administrador.</p></div>
 <ol class="mb-8 grid grid-cols-2 gap-2 sm:grid-cols-5" aria-label="Pasos del registro">
 @for(label of steps;track label;let i=$index){<li class="rounded-xl border p-3 text-sm" [class]="step()===i?'border-blue-600 bg-blue-50 text-blue-800':'border-slate-200 bg-white text-slate-500'" [attr.aria-current]="step()===i?'step':null"><span class="mr-2 font-bold">{{i+1}}</span>{{label}}</li>}
 </ol>
 <form [formGroup]="form" (ngSubmit)="submit()" class="org-card">
 @if(step()<4){
  <h2 class="mb-6 text-xl font-bold">{{sections[step()].title}}</h2>
  @if(step()===2){<p class="org-info mb-6">Estos datos son opcionales. Puedes continuar y completarlos más adelante desde el perfil de tu organización.</p>}
  <app-organization-fields [form]="form" [fields]="sections[step()].fields"/>
  @if(step()===3){<hr class="my-7 border-slate-200"><h3 class="mb-5 font-semibold">Acceso de la organización</h3><app-organization-fields [form]="form" [fields]="accountFields"/><p class="mt-4 text-sm text-slate-500">El correo del representante será el correo de inicio de sesión.</p>}
 } @else {
  <h2 class="text-xl font-bold">Revisa tus datos antes de enviar</h2>
  @for(section of sections;track section.title;let i=$index){
   <div class="mt-6 border-b border-slate-200 pb-5"><div class="mb-3 flex justify-between gap-3"><h3 class="font-semibold">{{section.title}}</h3><button type="button" class="org-link" (click)="step.set(i)">Editar {{section.title.toLowerCase()}}</button></div>
   <dl class="grid gap-3 sm:grid-cols-2">@for(field of section.fields;track field.key){<div><dt class="text-xs text-slate-500">{{field.label}}</dt><dd class="break-words text-sm">{{display(field.key)}}</dd></div>}</dl></div>
  }
  <p class="mt-5 text-sm">Identificador de acceso: <strong>{{form.get('organizationSlug')?.value}}</strong></p>
  <label class="mt-5 flex items-start gap-3 text-sm"><input type="checkbox" class="mt-1" [checked]="confirmed()" (change)="confirmed.set($any($event.target).checked)">Confirmo que los datos de la organización son correctos y que estoy autorizado para registrarla.</label>
 }
 @if(error()){<p class="org-error mt-5" role="alert">{{error()}}</p>}
 <div class="mt-8 flex flex-wrap justify-between gap-3">
 @if(step()>0){<button type="button" class="org-secondary" (click)="back()" [disabled]="loading()">Anterior</button>}@else{<a routerLink="/login" class="org-link self-center">Ya tengo una cuenta</a>}
 @if(step()<4){<button type="button" class="org-primary" (click)="next()">Continuar</button>}@else{<button type="submit" class="org-primary" [disabled]="loading() || !confirmed()">{{loading()?'Registrando…':'Crear organización'}}</button>}
 </div>
 </form></section>`
})
export class OrganizationRegisterPage {
 private readonly api=inject(OrganizationApi);private readonly auth=inject(AuthService);private readonly router=inject(Router);
 readonly sections=DETAIL_SECTIONS.map(section=>({...section,fields:section.fields.filter(field=>field.key!=='educationLevels')}));readonly accountFields=ACCOUNT_FIELDS;readonly steps=['Identificación','Contacto','Información legal','Representante','Revisión'];
 readonly form=buildDetailsForm(true);readonly step=signal(0);readonly loading=signal(false);readonly error=signal('');readonly confirmed=signal(false);
 display(key:string):string {const field=this.sections.flatMap(s=>s.fields).find(f=>f.key===key);const v=this.form.get(key)?.value;
 const label=(x:string)=>field?.options?.find(o=>o.value===x)?.label??x;return Array.isArray(v)?v.map(label).join(', '):label(v||'Sin especificar');}
 next(){this.error.set('');const fields=[...this.sections[this.step()].fields,...(this.step()===3?this.accountFields:[])];
 fields.forEach(f=>this.form.get(f.key)?.markAsTouched());if(fields.some(f=>this.form.get(f.key)?.invalid)){this.error.set('Revisa los campos marcados para continuar.');return;}
 if(this.step()===3 && this.form.get('password')?.value!==this.form.get('confirmPassword')?.value){this.error.set('Las contraseñas no coinciden.');return;}this.step.update(i=>i+1);}
 back(){this.step.update(i=>i-1);this.error.set('');}
 submit(){if(this.step()!==4||this.form.invalid||!this.confirmed()||this.loading())return;
 this.loading.set(true);this.error.set('');const v=this.form.getRawValue();
 this.api.register({organizationSlug:v.organizationSlug,password:v.password,profile:toDetails(this.form)}).pipe(finalize(()=>this.loading.set(false))).subscribe({
 next:r=>{this.auth.save(r);this.form.get('password')?.reset();this.form.get('confirmPassword')?.reset();void this.router.navigate(['/organization']);},
 error:e=>this.error.set(requestError(e))});}
}
