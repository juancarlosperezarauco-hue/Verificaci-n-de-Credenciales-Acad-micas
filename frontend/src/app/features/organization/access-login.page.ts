import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { OrganizationApi, requestError } from './organization.api';
import { AccountSecurityApi } from '../../core/account-security.api';
import { AuthResponse, MfaChallenge, requiresMfa } from '../../core/models';
@Component({imports:[ReactiveFormsModule,RouterLink],template:`
 <section class="org-page grid min-h-[75vh] max-w-5xl items-center gap-10 lg:grid-cols-2">
 <div><p class="org-eyebrow">{{platform?'Administración de la plataforma':'Portal institucional'}}</p>
 <h1 class="org-title">{{platform?'Supervisa las organizaciones.':'Tu institución, conectada a sus logros.'}}</h1>
 <p class="org-description">{{platform?'Consulta organizaciones, revisa su información y administra sus estados de acceso.':'Gestiona tu equipo, asigna permisos y entrega credenciales verificables a tus estudiantes.'}}</p></div>
 @if(!challenge()){
 <form [formGroup]="form" (ngSubmit)="submit()" class="org-card"><h2 class="mb-6 text-2xl font-bold">Iniciar sesión</h2>
 @if(logoutUnconfirmed){<p class="org-info mb-5" role="status">La sesión local se cerró, pero no se pudo confirmar el cierre en el servidor. Puedes revisar tus sesiones al volver a ingresar.</p>}
 @if(!platform){<label class="org-label">Identificador de la organización<input class="org-input" formControlName="organizationSlug" placeholder="universidad-demo" autocomplete="organization"></label>}
 <label class="org-label mt-4">Correo electrónico<input class="org-input" type="email" formControlName="email" autocomplete="username"></label>
 <label class="org-label mt-4">Contraseña<input class="org-input" type="password" formControlName="password" autocomplete="current-password"></label>
 @if(error()){<p class="org-error mt-5" role="alert">{{error()}}</p>}
 <button class="org-primary mt-6 w-full" [disabled]="form.invalid||loading()">{{loading()?'Ingresando…':'Ingresar'}}</button>
 @if(!platform){<p class="mt-6 text-sm text-slate-600">¿Primera vez? <a routerLink="/register" class="org-link">Registra tu institución</a></p><a routerLink="/superadmin/login" class="org-link mt-5 inline-block text-xs">Acceso de superadministración</a>}
 @else{<a routerLink="/login" class="org-link mt-5 inline-block">Volver al acceso institucional</a>}
 </form>}@else{
 <form [formGroup]="codeForm" (ngSubmit)="verifyMfa()" class="org-card"><p class="org-eyebrow">Segundo factor</p><h2 class="mt-3 text-2xl font-bold">Confirma tu acceso</h2><p class="mt-4 text-slate-600">Introduce un código nuevo de tu aplicación autenticadora o uno de tus códigos de recuperación.</p>
 <label class="org-label mt-5">Código de autenticación o recuperación<input class="org-input" formControlName="code" autocomplete="one-time-code" maxlength="64"></label>
 @if(error()){<p class="org-error mt-5" role="alert">{{error()}}</p>}<button class="org-primary mt-6 w-full" [disabled]="loading()||codeForm.invalid">{{loading()?'Verificando…':'Verificar y entrar'}}</button><button type="button" class="org-link mt-5" [disabled]="loading()" (click)="cancelMfa()">Volver al inicio de sesión</button></form>}
 </section>`})
export class AccessLoginPage {
 private readonly fb=inject(FormBuilder);private readonly api=inject(OrganizationApi);private readonly institution=inject(ApiService);private readonly auth=inject(AuthService);private readonly router=inject(Router);
 readonly platform=inject(ActivatedRoute).snapshot.data['platform']===true;
 readonly logoutUnconfirmed=inject(ActivatedRoute).snapshot.queryParamMap.get('logoutUnconfirmed')==='true';
 readonly form=this.fb.nonNullable.group({organizationSlug:['',this.platform?[]:[Validators.required]],email:['',[Validators.required,Validators.email]],password:['',Validators.required]});
 readonly loading=signal(false);readonly error=signal('');
 private readonly security=inject(AccountSecurityApi);readonly challenge=signal<MfaChallenge|null>(null);
 readonly codeForm=this.fb.nonNullable.group({code:['',[Validators.required,Validators.maxLength(64)]]});
 submit(){if(this.form.invalid||this.loading())return;this.error.set('');this.loading.set(true);const v=this.form.getRawValue();
 (this.platform?this.api.platformLogin({email:v.email,password:v.password}):this.institution.login(v)).pipe(finalize(()=>this.loading.set(false))).subscribe({
 next:r=>{this.form.controls.password.reset();if(requiresMfa(r)){this.challenge.set(r);return;}this.enter(r);},error:e=>this.error.set(e instanceof HttpErrorResponse && e.status===401
 ? 'Los datos de acceso no son correctos. Revisa el correo, la contraseña y, si corresponde, el identificador de la organización.'
 : requestError(e))});}
 private enter(r:AuthResponse){this.auth.save(r);this.challenge.set(null);this.api.me().subscribe({next:p=>{
 const destination=p.role==='SUPER_ADMIN'?'/superadmin/organizations':p.onboarding?'/organization':p.permissions.includes('ORGANIZATION_VIEW')?'/dashboard':p.permissions.includes('STUDENT_VIEW')?'/students':p.permissions.includes('CREDENTIAL_VIEW')?'/credentials':'/access-denied';
 void this.router.navigate([destination]);},error:e=>this.error.set(requestError(e))});}
 verifyMfa(){const challenge=this.challenge();if(!challenge||this.loading()||this.codeForm.invalid)return;this.loading.set(true);this.error.set('');this.security.verify(challenge.challengeToken,this.codeForm.controls.code.value.trim()).pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.codeForm.reset();this.enter(r);},error:e=>{this.codeForm.reset();this.error.set(e.status===401?'El código no es válido, venció o ya fue utilizado. Usa un código nuevo.':requestError(e));}});}
 cancelMfa(){this.challenge.set(null);this.codeForm.reset();this.error.set('');}
}
