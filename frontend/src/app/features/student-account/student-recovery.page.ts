import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { StudentAccountApi } from './student-account.api';

@Component({imports:[ReactiveFormsModule,RouterLink],template:`
 <section class="org-page max-w-xl"><p class="org-eyebrow">Cuenta del estudiante</p>
 <h1 class="org-title">{{token?'Crea una contraseña nueva':'Recupera tu contraseña'}}</h1>
 <p class="org-description">{{token?'Usa entre 10 y 72 caracteres. Al guardar se cerrarán las sesiones anteriores.':'Escribe tu correo. Si corresponde a una cuenta activa, recibirás un enlace válido durante 30 minutos.'}}</p>
 @if(message()){<p class="org-success mt-6" role="status">{{message()}}</p>}
 @if(error()){<p class="org-error mt-6" role="alert">{{error()}}</p>}
 @if(!completed()){
 <form [formGroup]="form" (ngSubmit)="submit()" class="org-card mt-7 space-y-5">
 @if(token){<label class="org-label">Contraseña nueva<input class="org-input" type="password" formControlName="password" autocomplete="new-password" maxlength="72"></label><label class="org-label">Repite la contraseña<input class="org-input" type="password" formControlName="confirm" autocomplete="new-password" maxlength="72"></label>}
 @else{<label class="org-label">Correo electrónico<input class="org-input" type="email" formControlName="email" autocomplete="email" maxlength="180"></label>}
 <button class="org-primary w-full" [disabled]="busy()||form.invalid">{{busy()?'Procesando…':token?'Guardar contraseña':'Solicitar enlace'}}</button></form>}
 <a routerLink="/student/login" class="org-link mt-6 inline-block">Volver a iniciar sesión</a>
 </section>`})
export class StudentRecoveryPage implements OnInit {
 private readonly api=inject(StudentAccountApi);private readonly auth=inject(AuthService);private readonly router=inject(Router);private readonly route=inject(ActivatedRoute);
 readonly token=this.route.snapshot.queryParamMap.get('token')??'';
 readonly busy=signal(false);readonly completed=signal(false);readonly message=signal('');readonly error=signal('');
 readonly form=inject(FormBuilder).nonNullable.group({email:['',this.token?[]:[Validators.required,Validators.email]],password:['',this.token?[Validators.required,Validators.minLength(10),Validators.maxLength(72)]:[]],confirm:['',this.token?[Validators.required,Validators.maxLength(72)]:[]]});
 ngOnInit(){if(this.token)void this.router.navigate([],{relativeTo:this.route,queryParams:{token:null},queryParamsHandling:'merge',replaceUrl:true});}
 submit(){if(this.busy())return;this.error.set('');this.message.set('');this.form.markAllAsTouched();if(this.form.invalid){this.error.set('Revisa los datos del formulario.');return;}const value=this.form.getRawValue();
 if(this.token&&value.password!==value.confirm){this.error.set('Las contraseñas no coinciden.');return;}
 this.busy.set(true);const request=this.token?this.api.confirmPasswordReset(this.token,value.password):this.api.requestPasswordReset(value.email);
 request.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{if(this.token){this.completed.set(true);this.form.reset();if(this.auth.current()?.role==='STUDENT')this.auth.logout();}this.message.set(this.token?'Contraseña actualizada. Inicia sesión con tu nueva contraseña.':'Solicitud recibida. Si existe una cuenta activa con ese correo, recibirás las instrucciones.');},error:e=>this.error.set(e.error?.message??'No se pudo completar la solicitud. Intenta nuevamente.')});}
}
