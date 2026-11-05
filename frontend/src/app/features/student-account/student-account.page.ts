import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import QRCode from 'qrcode';
import { PublicCredential } from '../../core/models';
import { Consent, GeneratedConsent, IdentityProfile, StudentAccountApi } from './student-account.api';
@Component({
 imports:[ReactiveFormsModule,DatePipe,RouterLink],
 template:`
 <section class="mx-auto max-w-6xl px-6 py-12">
 <p class="text-cyan-300">Mi identidad académica</p><h1 class="mt-3 text-3xl font-bold">{{profile()?.fullName || 'Construye tu perfil estudiantil'}}</h1>
 @if(profile(); as person){<p class="mt-3 text-slate-400">Administra tus datos personales y decide qué instituciones pueden vincularte.</p><p class="mt-2 break-all text-xs text-slate-500">Identificador permanente: {{person.id}}</p>}
 @if(error()){<p role="alert" class="mt-4 rounded-xl bg-red-500/10 p-4 text-red-300">{{error()}}</p>}
 @if(profile(); as person){
 @if(!person.profileComplete || editingProfile()){
 <section class="mt-8 overflow-hidden rounded-3xl border border-cyan-400/25 bg-slate-900">
  <div class="border-b border-white/10 p-6 sm:p-8">
   <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><p class="text-sm font-bold uppercase tracking-[.2em] text-cyan-300">{{person.profileComplete ? 'Editar expediente' : 'Configuración inicial'}}</p><h2 class="mt-2 text-2xl font-bold">{{person.profileComplete ? 'Actualiza tu perfil' : 'Completa tu perfil estudiantil'}}</h2></div><span class="w-fit rounded-full bg-cyan-400/10 px-4 py-2 text-sm font-bold text-cyan-300">Paso {{profileStep()}} de 3</span></div>
   <div class="mt-6 grid grid-cols-3 gap-2"><span [class]="profileStep() >= 1 ? 'h-2 rounded-full bg-cyan-400' : 'h-2 rounded-full bg-slate-700'"></span><span [class]="profileStep() >= 2 ? 'h-2 rounded-full bg-cyan-400' : 'h-2 rounded-full bg-slate-700'"></span><span [class]="profileStep() >= 3 ? 'h-2 rounded-full bg-cyan-400' : 'h-2 rounded-full bg-slate-700'"></span></div>
  </div>
  <form [formGroup]="profileForm" (ngSubmit)="submitProfileStep()" class="p-6 sm:p-8">
   @if(profileStep() === 1){
    <p class="text-sm text-slate-400">Paso 1 · Datos personales</p>
    <div class="mt-5 grid gap-5 sm:grid-cols-2">
     <label class="block">Nombre completo<input class="field" formControlName="fullName" maxlength="180" autocomplete="name" placeholder="Tal como aparece en tu CI"></label>
     <label class="block">Fecha de nacimiento<input class="field" type="date" formControlName="dateOfBirth" [max]="maxBirthDate" autocomplete="bday"></label>
    </div>
    <p class="mt-3 text-sm text-slate-400">Usaremos estos datos para identificarte correctamente en tus diplomas y certificados.</p>
    <div class="mt-6 flex flex-wrap gap-3">@if(person.profileComplete){<button type="button" (click)="cancelProfileEdit()" class="rounded-xl border border-white/20 px-5 py-3">Cancelar</button>}<button class="rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950">Continuar</button></div>
   } @else if(profileStep() === 2){
    <p class="text-sm text-slate-400">Paso 2 · Identificación</p>
    <label class="mt-5 block max-w-xl">Cédula de identidad (CI)<input class="field" formControlName="documentNumber" maxlength="80" autocomplete="off" placeholder="Ingresa tu número de CI"></label>
    <p class="mt-3 text-sm text-slate-400">Tu CI evita que se cree otro perfil con la misma identidad. No se mostrará públicamente.</p>
    <div class="mt-6 flex flex-wrap gap-3"><button type="button" (click)="profileStep.set(1)" class="rounded-xl border border-white/20 px-5 py-3">Volver</button>@if(person.profileComplete){<button type="button" (click)="cancelProfileEdit()" class="rounded-xl border border-white/20 px-5 py-3">Cancelar</button>}<button class="rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950">Continuar</button></div>
   } @else {
    <p class="text-sm text-slate-400">Paso 3 · Ubicación</p>
    <div class="mt-5 grid gap-5 sm:grid-cols-2">
     <label class="block">País<select class="field" formControlName="country"><option value="BO">Bolivia</option><option value="AR">Argentina</option><option value="BR">Brasil</option><option value="CL">Chile</option><option value="CO">Colombia</option><option value="EC">Ecuador</option><option value="ES">España</option><option value="MX">México</option><option value="PE">Perú</option><option value="PY">Paraguay</option><option value="UY">Uruguay</option><option value="VE">Venezuela</option></select></label>
     <label class="block">Departamento o región<input class="field" formControlName="department" maxlength="120" autocomplete="address-level1" placeholder="Ej. Santa Cruz"></label>
    </div>
    <p class="mt-3 text-sm text-slate-400">Podrás regresar y actualizar estos datos cuando cambien.</p>
    <div class="mt-6 flex flex-wrap gap-3"><button type="button" (click)="profileStep.set(2)" class="rounded-xl border border-white/20 px-5 py-3">Volver</button>@if(person.profileComplete){<button type="button" (click)="cancelProfileEdit()" class="rounded-xl border border-white/20 px-5 py-3">Cancelar</button>}<button [disabled]="busy() || profileForm.invalid" class="rounded-xl bg-cyan-400 px-5 py-3 font-bold text-slate-950 disabled:opacity-50">{{busy() ? 'Guardando…' : person.profileComplete ? 'Guardar cambios' : 'Completar perfil'}}</button></div>
   }
  </form>
 </section>
 } @else {
 <section class="mt-8 rounded-2xl border border-white/10 bg-slate-900 p-6">
  <div class="flex flex-col justify-between gap-4 sm:flex-row sm:items-center"><div><p class="text-sm font-bold uppercase tracking-[.18em] text-cyan-300">Perfil completado</p><h2 class="mt-2 text-2xl font-bold">Mis datos personales</h2></div><button type="button" (click)="startProfileEdit()" class="w-fit rounded-xl border border-cyan-400/40 px-4 py-3 font-semibold text-cyan-300">Editar perfil</button></div>
  <dl class="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4"><div class="rounded-xl bg-slate-950/60 p-4"><dt class="text-xs uppercase tracking-wider text-slate-500">Nacimiento</dt><dd class="mt-2 font-semibold">{{person.dateOfBirth | date:'dd/MM/yyyy'}}</dd></div><div class="rounded-xl bg-slate-950/60 p-4"><dt class="text-xs uppercase tracking-wider text-slate-500">Cédula de identidad</dt><dd class="mt-2 font-semibold">{{person.documentNumber}}</dd></div><div class="rounded-xl bg-slate-950/60 p-4"><dt class="text-xs uppercase tracking-wider text-slate-500">País</dt><dd class="mt-2 font-semibold">{{countryName(person.country)}}</dd></div><div class="rounded-xl bg-slate-950/60 p-4"><dt class="text-xs uppercase tracking-wider text-slate-500">Departamento</dt><dd class="mt-2 font-semibold">{{person.department}}</dd></div></dl>
 </section>
 <section class="mt-8 rounded-2xl border border-cyan-400/30 bg-cyan-400/5 p-6">
 <h2 class="text-2xl font-bold">Mi código de búsqueda</h2>
 <p class="mt-3 text-slate-300">Entrega este código a tu colegio, universidad o instituto. Podrán pegarlo en Estudiantes para consultar tu nombre y documento, registrarte en su padrón y emitirte reconocimientos.</p>
 <code class="mt-5 block break-all select-all rounded-xl bg-slate-950 p-4 text-lg text-cyan-300">{{person.searchCode}}</code>
 <div class="mt-4 flex flex-wrap gap-3">
 <button type="button" (click)="copySearchCode()" class="rounded-xl bg-cyan-400 px-4 py-3 font-bold text-slate-950">Copiar código</button>
 <button type="button" [disabled]="busy()" (click)="confirmRotation.set(true)" class="rounded-xl border border-white/20 px-4 py-3">Cambiar código</button>
 </div>
 @if(copyMessage()){<p role="status" class="mt-3 text-sm text-cyan-300">{{copyMessage()}}</p>}
 @if(searchQr()){<img [src]="searchQr()" alt="QR del código de búsqueda del estudiante" class="mt-5 size-48">}
 <p class="mt-4 text-sm text-slate-400">Es reutilizable y no vence. Compártelo solo con las instituciones que quieras autorizar. Ellas no verán tus credenciales de otras instituciones. Puedes retirar cada vínculo en Autorizaciones y accesos.</p>
 @if(confirmRotation()){
 <div class="mt-4 rounded-xl bg-slate-900 p-4">
 <p>El código anterior dejará de funcionar. Tus vínculos existentes y reconocimientos se conservarán.</p>
 <button type="button" [disabled]="busy()" (click)="rotateSearchCode()" class="mt-3 mr-4 text-cyan-300">Confirmar cambio</button>
 <button type="button" (click)="confirmRotation.set(false)">Cancelar</button>
 </div>}
 </section>}}
 @if(profile()?.profileComplete && !editingProfile()){
 <div class="mt-8 grid gap-8 lg:grid-cols-2">
 <form [formGroup]="consentForm" (ngSubmit)="generate()" class="rounded-2xl border border-white/10 bg-slate-900 p-6">
 <h2 class="text-xl font-bold">Código temporal opcional</h2>
 <p class="mt-3 text-sm text-slate-400">Compartirás tu nombre y documento. La institución podrá vincularte y emitir reconocimientos hasta que retires la autorización. No tendrá acceso a tu historial completo.</p>
 <label class="mt-5 block">Identificador de la institución<input class="field" formControlName="institutionSlug" placeholder="universidad-demo"></label>
 <button [disabled]="busy() || consentForm.invalid" class="mt-5 rounded-xl bg-cyan-400 p-3 font-bold text-slate-950 disabled:opacity-50">Generar código de un solo uso</button>
 @if(generated(); as result){
 <div class="mt-6 rounded-xl border border-cyan-400/30 p-4">
 <p class="font-bold">Para {{result.access.institutionName}}</p>
 <p class="mt-2 text-sm">Vence: {{result.access.expiresAt | date:'short'}}</p>
 <code class="mt-3 block break-all select-all text-cyan-300">{{result.code}}</code>
 @if(qr()){<img [src]="qr()" alt="Código QR de autorización temporal" class="mt-4 size-48">}
 <p class="mt-3 text-xs text-slate-400">Entrega este código solo a la institución indicada. El QR contiene el mismo código.</p>
 </div>}
 </form>
 <div class="rounded-2xl border border-white/10 bg-slate-900 p-6">
 <h2 class="text-xl font-bold">Autorizaciones y accesos</h2>
 @if(loading()){<p class="mt-4">Cargando…</p>}
 @for(c of consents();track c.id){
 <article class="border-b border-white/10 py-4">
 <p class="font-bold">{{c.institutionName}}</p>
 <p class="mt-1 text-sm text-slate-400">{{c.revokedAt ? 'Retirada' : c.usedAt ? 'Utilizada · vínculo activo' : expired(c) ? 'Código vencido' : 'Pendiente de uso'}}</p>
 @if(c.usedAt){<p class="text-sm">Consultó tus datos: {{c.usedAt | date:'short'}}</p>}
 @if(!c.revokedAt && (c.usedAt || !expired(c))){<button type="button" [disabled]="busy()" (click)="revoke(c)" class="mt-3 text-sm text-red-300">Retirar autorización</button>}
 </article>
 }@empty{<p class="mt-4 text-slate-400">Todavía no has compartido tus datos.</p>}
 </div>
 </div>
 <div class="mt-8 rounded-2xl border border-white/10 bg-slate-900 p-6">
 <h2 class="text-xl font-bold">Mis diplomas y certificados</h2>
 @for(c of credentials();track c.verificationCode){
 <article class="border-b border-white/10 py-5">
 <h3 class="font-semibold">{{c.title}}</h3><p class="text-cyan-300">{{c.organization}} · {{c.credentialType}}</p>
 <p class="text-sm text-slate-400">{{c.status === 'REVOKED' ? 'Revocado' : 'Emitido'}} · {{c.issuedAt | date:'shortDate'}}</p>
 <a class="mt-2 inline-block text-sm underline" [routerLink]="['/verify',c.verificationCode]">Verificar credencial</a>
 </article>
 }@empty{<p class="mt-4 text-slate-400">Aquí aparecerán los documentos que las instituciones te emitan.</p>}
 </div>
 }
 </section>`
})
export class StudentAccountPage implements OnInit {
 private readonly api=inject(StudentAccountApi);
 readonly profile=signal<IdentityProfile|null>(null); readonly consents=signal<Consent[]>([]);
 readonly credentials=signal<PublicCredential[]>([]); readonly generated=signal<GeneratedConsent|null>(null);
 readonly qr=signal('');readonly busy=signal(false);readonly loading=signal(true);readonly error=signal('');
 readonly searchQr=signal('');readonly copyMessage=signal('');readonly confirmRotation=signal(false);
 readonly profileStep=signal(1);readonly editingProfile=signal(false);
 readonly maxBirthDate=new Date(Date.now()-86_400_000).toISOString().slice(0,10);
 readonly profileForm=inject(FormBuilder).nonNullable.group({
   fullName:['',[Validators.required,Validators.maxLength(180)]],
   dateOfBirth:['',[Validators.required]],
   country:['BO',[Validators.required,Validators.pattern('[A-Za-z]{2}')]],
   documentType:['CI',[Validators.required]],
   documentNumber:['',[Validators.required,Validators.maxLength(80)]],
   department:['',[Validators.required,Validators.maxLength(120)]]
 });
 readonly consentForm=inject(FormBuilder).nonNullable.group({institutionSlug:['',[Validators.required,Validators.maxLength(80)]]});
 ngOnInit(){this.reload();}
 submitProfileStep(){
   if(this.profileStep()===1){
     const controls=[this.profileForm.controls.fullName,this.profileForm.controls.dateOfBirth];
     controls.forEach(control=>control.markAsTouched());
     if(controls.some(control=>control.invalid))return;
     this.profileStep.set(2);return;
   }
   if(this.profileStep()===2){
     const document=this.profileForm.controls.documentNumber;
     document.markAsTouched();
     if(document.invalid)return;
     this.profileStep.set(3);return;
   }
   this.saveProfile();
 }
 private saveProfile(){
   if(this.profileForm.invalid || this.busy())return;
   this.busy.set(true);this.error.set('');
   this.api.updateProfile(this.profileForm.getRawValue()).pipe(finalize(()=>this.busy.set(false))).subscribe({
     next:r=>{this.setProfile(r);this.profileStep.set(1);this.editingProfile.set(false);},
     error:e=>this.error.set(e.error?.message ?? 'No se pudo completar el perfil.')
   });
 }
 startProfileEdit(){
   const person=this.profile();if(!person)return;
   this.profileForm.patchValue({fullName:person.fullName??'',dateOfBirth:person.dateOfBirth??'',country:person.country??'BO',documentType:'CI',documentNumber:person.documentNumber??'',department:person.department??''});
   this.profileStep.set(1);this.editingProfile.set(true);this.error.set('');
 }
 cancelProfileEdit(){this.profileStep.set(1);this.editingProfile.set(false);this.error.set('');}
 countryName(code:string|null){return ({BO:'Bolivia',AR:'Argentina',BR:'Brasil',CL:'Chile',CO:'Colombia',EC:'Ecuador',ES:'España',MX:'México',PE:'Perú',PY:'Paraguay',UY:'Uruguay',VE:'Venezuela'} as Record<string,string>)[code??'']??code??'—';}
 async copySearchCode(){
   const code=this.profile()?.searchCode;
   if(!code)return;
   try { await navigator.clipboard.writeText(code);this.copyMessage.set('Código copiado. Ya puedes enviarlo a tu institución.'); }
   catch {this.copyMessage.set('Selecciona el código y cópialo manualmente.');}
 }
 rotateSearchCode(){
   if(this.busy())return;
   this.busy.set(true);this.error.set('');
   this.api.rotateSearchCode().pipe(finalize(()=>this.busy.set(false))).subscribe({
     next:r=>{this.setProfile(r);this.confirmRotation.set(false);this.copyMessage.set('Código actualizado. El anterior ya no funciona.');},
     error:e=>this.error.set(e.error?.message ?? 'No se pudo cambiar el código.')
   });
 }
 private setProfile(profile:IdentityProfile){
   this.profile.set(profile);this.searchQr.set('');
   this.profileForm.patchValue({fullName:profile.fullName??'',dateOfBirth:profile.dateOfBirth??'',country:profile.country??'BO',documentType:'CI',documentNumber:profile.documentNumber??'',department:profile.department??''});
   if(!profile.profileComplete)return;
   void QRCode.toDataURL(profile.searchCode,{width:240}).then(url=>{
     if(this.profile()?.searchCode===profile.searchCode)this.searchQr.set(url);
   }).catch(()=>{});
 }
 expired(c:Consent){return new Date(c.expiresAt).getTime()<=Date.now();}
 generate(){
 if(this.consentForm.invalid || this.busy())return;
 this.busy.set(true);this.error.set('');
 this.api.generate(this.consentForm.getRawValue().institutionSlug).pipe(finalize(()=>this.busy.set(false))).subscribe({
 next:r=>{this.generated.set(r);this.consents.update(items=>[r.access,...items]);this.qr.set('');void QRCode.toDataURL(r.code,{width:240}).then(url=>this.qr.set(url)).catch(()=>{});},
 error:e=>this.error.set(e.error?.message ?? 'No se pudo generar el código.')
 });
 }
 revoke(c:Consent){
 this.busy.set(true);this.error.set('');
 this.api.revoke(c.id).pipe(finalize(()=>this.busy.set(false))).subscribe({
 next:()=>{if(this.generated()?.access.id===c.id){this.generated.set(null);this.qr.set('');}this.reload();},
 error:e=>this.error.set(e.error?.message ?? 'No se pudo retirar la autorización.')
 });
 }
 private reload(){
 this.loading.set(true);
 forkJoin({profile:this.api.profile(),consents:this.api.consents(),credentials:this.api.history()})
 .pipe(finalize(()=>this.loading.set(false))).subscribe({
 next:r=>{this.setProfile(r.profile);this.consents.set(r.consents);this.credentials.set(r.credentials);},
 error:()=>this.error.set('No se pudo cargar la cuenta. Intenta iniciar sesión nuevamente.')
 });
 }
}
