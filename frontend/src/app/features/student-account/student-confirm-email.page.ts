import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { StudentAccountApi } from './student-account.api';

@Component({imports:[RouterLink],template:`
 <section class="org-page max-w-xl"><p class="org-eyebrow">Seguridad de tu cuenta</p><h1 class="org-title">{{success()?'Correo confirmado':type==='change'?'Confirma tu nuevo correo':'Verifica tu correo'}}</h1>
 <div class="org-card mt-7"><p class="text-slate-600">{{success()?'La confirmación se completó. Puedes acceder a tu cuenta.':type==='change'?'Confirma que deseas utilizar este nuevo correo. Tus sesiones anteriores se cerrarán.':'Confirma que tienes acceso al correo registrado para tu cuenta de estudiante.'}}</p>
 @if(error()){<p class="org-error mt-5" role="alert">{{error()}}</p>}
 @if(!success()){<button class="org-primary mt-6" [disabled]="busy()||!token" (click)="confirm()">{{busy()?'Confirmando…':'Confirmar correo'}}</button>}
 <a routerLink="/student/login" class="org-link mt-6 block">Ir a iniciar sesión</a></div></section>`})
export class StudentConfirmEmailPage implements OnInit {
 private readonly api=inject(StudentAccountApi);private readonly auth=inject(AuthService);private readonly route=inject(ActivatedRoute);private readonly router=inject(Router);
 readonly token=this.route.snapshot.queryParamMap.get('token')??'';readonly type=this.route.snapshot.queryParamMap.get('type')==='change'?'change':'verification';
 readonly busy=signal(false);readonly success=signal(false);readonly error=signal('');
 ngOnInit(){if(!this.token)this.error.set('El enlace está incompleto. Solicita uno nuevo desde la seguridad de tu cuenta.');else void this.router.navigate([],{relativeTo:this.route,queryParams:{token:null,type:null},replaceUrl:true});}
 confirm(){if(!this.token||this.busy()||this.success())return;this.busy.set(true);this.error.set('');this.api.confirmEmail(this.token,this.type).pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{this.success.set(true);if(this.type==='change'&&this.auth.current()?.role==='STUDENT')this.auth.logout();},error:e=>this.error.set(e.error?.message??'El enlace no es válido o ya venció.')});}
}
