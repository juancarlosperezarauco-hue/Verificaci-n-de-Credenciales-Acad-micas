import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize, Observable } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { StudentAccountApi, StudentSecurity } from './student-account.api';

@Component({imports:[ReactiveFormsModule,RouterLink],template:`
 <section class="org-page max-w-5xl"><a routerLink="/student/account" class="org-link">← Mi expediente</a><p class="org-eyebrow mt-7">Cuenta personal</p><h1 class="org-title">Seguridad de mi cuenta</h1><p class="org-description">Confirma tu correo y administra tus datos de acceso.</p>
 @if(loading()){<p class="org-card mt-7" role="status">Cargando cuenta…</p>}
 @if(error()){<p class="org-error mt-6" role="alert">{{error()}} <button type="button" class="org-link" (click)="load()">Volver a cargar</button></p>}
 @if(message()){<p class="org-success mt-6" role="status">{{message()}}</p>}
 @if(account();as data){<div class="mt-7 grid items-start gap-6 md:grid-cols-2">
 <section class="org-card"><h2 class="text-xl font-bold">Correo electrónico</h2><p class="mt-3 break-all font-medium">{{data.email}}</p><p class="mt-2 text-sm" [class.text-emerald-700]="data.emailVerified" [class.text-amber-700]="!data.emailVerified">{{data.emailVerified?'Correo verificado':'Correo pendiente de verificación'}}</p>
 @if(!data.emailVerified){<button type="button" class="org-secondary mt-5" [disabled]="busy()" (click)="resend()">Reenviar verificación</button>}
 <form [formGroup]="emailForm" (ngSubmit)="changeEmail()" class="mt-7 space-y-4 border-t border-slate-200 pt-6"><h3 class="font-semibold">Cambiar correo</h3><label class="org-label">Nuevo correo<input class="org-input" formControlName="newEmail" type="email" autocomplete="email" maxlength="180"></label><label class="org-label">Contraseña actual<input class="org-input" formControlName="currentPassword" type="password" autocomplete="current-password"></label><p class="text-sm text-slate-500">El cambio se hará al confirmar el enlace enviado al nuevo correo.</p><button class="org-primary" [disabled]="busy()||emailForm.invalid">Solicitar cambio</button></form></section>
 <form class="org-card space-y-4" [formGroup]="passwordForm" (ngSubmit)="changePassword()"><h2 class="text-xl font-bold">Cambiar contraseña</h2><label class="org-label">Contraseña actual<input class="org-input" type="password" autocomplete="current-password" formControlName="currentPassword"></label><label class="org-label">Contraseña nueva<input class="org-input" type="password" autocomplete="new-password" formControlName="newPassword" maxlength="72"></label><label class="org-label">Repite la contraseña nueva<input class="org-input" type="password" autocomplete="new-password" formControlName="confirmPassword" maxlength="72"></label><p class="text-sm text-slate-500">Usa entre 10 y 72 caracteres. Al guardar se cerrarán tus sesiones anteriores, incluida esta.</p><button class="org-primary" [disabled]="busy()||passwordForm.invalid">Guardar y volver a iniciar sesión</button></form>
 </div>
 <section class="org-card mt-6 border-red-200"><h2 class="text-xl font-bold">Desactivar mi cuenta</h2><p class="mt-3 text-slate-600">Se cerrarán tus sesiones y tu código dejará de funcionar. Tus credenciales emitidas y su historial se conservarán.</p>
 @if(!confirmDeactivation()){<button type="button" class="org-secondary mt-5 text-red-700" [disabled]="busy()" (click)="confirmDeactivation.set(true)">Desactivar cuenta</button>}
 @else{<form [formGroup]="deactivateForm" (ngSubmit)="deactivate()" class="mt-5 max-w-md space-y-4"><p class="text-sm font-medium text-red-700">Confirma la desactivación con tu contraseña actual.</p><label class="org-label">Contraseña actual<input class="org-input" type="password" formControlName="currentPassword" autocomplete="current-password"></label><div class="flex flex-wrap gap-3"><button class="org-primary" [disabled]="busy()||deactivateForm.invalid">Confirmar desactivación</button><button type="button" class="org-secondary" [disabled]="busy()" (click)="cancelDeactivation()">Cancelar</button></div></form>}
 </section>}
 </section>`})
export class StudentSecurityPage implements OnInit {
 private readonly api=inject(StudentAccountApi);private readonly auth=inject(AuthService);private readonly router=inject(Router);private readonly fb=inject(FormBuilder);
 readonly account=signal<StudentSecurity|null>(null);readonly loading=signal(true);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly confirmDeactivation=signal(false);
 readonly emailForm=this.fb.nonNullable.group({newEmail:['',[Validators.required,Validators.email,Validators.maxLength(180)]],currentPassword:['',Validators.required]});
 readonly passwordForm=this.fb.nonNullable.group({currentPassword:['',Validators.required],newPassword:['',[Validators.required,Validators.minLength(10),Validators.maxLength(72)]],confirmPassword:['',Validators.required]});
 readonly deactivateForm=this.fb.nonNullable.group({currentPassword:['',Validators.required]});
 ngOnInit(){this.load();}
 load(){this.loading.set(true);this.error.set('');this.api.security().pipe(finalize(()=>this.loading.set(false))).subscribe({next:v=>this.account.set(v),error:()=>this.error.set('No se pudo consultar la cuenta. Si tu sesión terminó, vuelve a iniciar sesión.')});}
 resend(){this.run(this.api.resendVerification(),()=>this.message.set('Solicitud recibida. Revisa tu correo; si no llega el enlace, puedes solicitar otro.'));}
 changeEmail(){if(this.emailForm.invalid)return;this.run(this.api.requestEmailChange(this.emailForm.getRawValue()),()=>{this.emailForm.reset();this.message.set('Revisa el nuevo correo para confirmar el cambio.');});}
 changePassword(){if(this.passwordForm.invalid)return;const value=this.passwordForm.getRawValue();if(value.newPassword!==value.confirmPassword){this.error.set('Las contraseñas nuevas no coinciden.');return;}this.run(this.api.changePassword({currentPassword:value.currentPassword,newPassword:value.newPassword}),()=>{this.passwordForm.reset();this.auth.logout();void this.router.navigate(['/student/login'],{queryParams:{passwordChanged:true}});});}
 cancelDeactivation(){this.confirmDeactivation.set(false);this.deactivateForm.reset();}
 deactivate(){if(!this.confirmDeactivation()||this.deactivateForm.invalid)return;this.run(this.api.deactivate(this.deactivateForm.controls.currentPassword.value),()=>{this.auth.logout();void this.router.navigate(['/student/login']);});}
 private run(request:Observable<void>,done:()=>void){if(this.busy())return;this.busy.set(true);this.error.set('');this.message.set('');request.pipe(finalize(()=>this.busy.set(false))).subscribe({next:done,error:e=>this.error.set(e.error?.message??'No se pudo completar la operación. Intenta nuevamente.')});}
}
