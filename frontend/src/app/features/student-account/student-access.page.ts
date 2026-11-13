import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { StudentAccountApi } from './student-account.api';
@Component({
 imports:[ReactiveFormsModule,RouterLink],
 template:`
 <section class="mx-auto max-w-2xl px-6 py-12">
  <p class="text-cyan-300">Cuenta personal del estudiante</p>
  <h1 class="mt-3 text-3xl font-bold">{{registering ? 'Crea tu cuenta' : 'Ingresa a tu cuenta'}}</h1>
  <p class="mt-3 text-slate-400">{{registering ? 'Comienza con tu correo y contraseña. Completarás tu perfil dentro de tu cuenta.' : 'Accede a tu perfil y a tus reconocimientos académicos.'}}</p>
  @if(passwordChanged){<p role="status" class="mt-5 rounded-xl bg-emerald-400/10 p-4 text-emerald-300">Tu contraseña fue actualizada. Inicia sesión con la nueva contraseña.</p>}
  <form [formGroup]="form" (ngSubmit)="submit()" class="mt-8 rounded-2xl border border-white/10 bg-slate-900 p-6 space-y-5">
   <label class="block">Correo electrónico<input class="field" formControlName="email" type="email" autocomplete="email"></label>
   <label class="block">Contraseña<input class="field" formControlName="password" type="password" [autocomplete]="registering ? 'new-password' : 'current-password'"></label>
   @if(registering){<p class="text-sm text-slate-400">Usa por lo menos 10 caracteres. Después podrás completar tus datos personales en pasos separados.</p>}
   @if(error()){<p role="alert" class="text-red-300">{{error()}}</p>}
   <button class="w-full rounded-xl bg-cyan-400 p-3 font-bold text-slate-950 disabled:opacity-50" [disabled]="busy() || form.invalid">{{busy() ? 'Procesando…' : registering ? 'Crear mi cuenta' : 'Ingresar'}}</button>
  </form>
  @if(!registering){<a routerLink="/student/recover" class="mt-5 inline-block text-cyan-300">Olvidé mi contraseña</a>}
  <p class="mt-6">@if(registering){<a routerLink="/student/login" class="text-cyan-300">Ya tengo cuenta</a>}@else{<a routerLink="/student/register" class="text-cyan-300">Registrarme como estudiante</a>}</p>
  <a routerLink="/login" class="mt-5 block text-sm text-slate-400">Acceso de instituciones</a>
 </section>`
})
export class StudentAccessPage {
 private readonly api=inject(StudentAccountApi);
 private readonly auth=inject(AuthService);
 private readonly router=inject(Router);
 readonly registering=inject(ActivatedRoute).snapshot.data['registering']===true;
 readonly passwordChanged=inject(ActivatedRoute).snapshot.queryParamMap.get('passwordChanged')==='true';
 readonly busy=signal(false); readonly error=signal('');
 readonly form=inject(FormBuilder).nonNullable.group({
 email:['',[Validators.required,Validators.email]],password:['',[Validators.required,Validators.minLength(this.registering ? 10 : 1),Validators.maxLength(72)]]
 });
 submit(){
  if(this.form.invalid || this.busy())return;
  this.busy.set(true);this.error.set('');
  const request=this.registering ? this.api.register(this.form.getRawValue()) : this.api.login(this.form.getRawValue());
  request.pipe(finalize(()=>this.busy.set(false))).subscribe({
   next:r=>{this.auth.save(r);void this.router.navigate(['/student/account']);},
   error:e=>this.error.set(e.error?.message ?? 'No se pudo completar la solicitud.')
  });
 }
}
