import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Dialog, DialogModule } from '@angular/cdk/dialog';
import { finalize, forkJoin, Observable } from 'rxjs';
import { OrganizationApi, requestError } from './organization.api';
import { PERMISSION_LABELS, StaffUser, TenantRole } from './organization.models';
import { ConfirmDialogComponent } from './confirm-dialog.component';
@Component({imports:[ReactiveFormsModule,RouterLink,DialogModule],template:`
 <section class="org-page"><a routerLink="/organization" class="org-link">← Organización</a><p class="org-eyebrow mt-7">Tu equipo, sus responsabilidades</p><h1 class="org-title">Usuarios y permisos</h1><p class="org-description">Crea roles y asígnalos a los encargados de tu institución. Los estudiantes tienen cuentas personales separadas.</p>
 <div class="my-7 flex gap-2" aria-label="Secciones"><button class="org-secondary" [class.org-selected]="tab()==='users'" (click)="tab.set('users')">Usuarios internos</button><button class="org-secondary" [class.org-selected]="tab()==='roles'" (click)="tab.set('roles')">Roles y permisos</button></div>
 @if(error()){<p class="org-error mb-5" role="alert">{{error()}} <button class="org-link" (click)="load()">Reintentar</button></p>}
 @if(notice()){<p class="org-success mb-5" role="status">{{notice()}}</p>}
 @if(loading()){<p role="status" class="org-card">Cargando equipo…</p>}
 @else if(tab()==='users'){
 <div class="grid gap-6 xl:grid-cols-[1.3fr_1fr]">
 <section class="org-card"><h2 class="mb-5 text-xl font-bold">Equipo institucional <span class="text-sm font-normal text-slate-500">({{users().length}})</span></h2>
 @for(user of users();track user.id){<article class="border-b border-slate-200 py-5 last:border-0"><div class="flex flex-wrap items-start justify-between gap-3"><div><h3 class="font-semibold">{{user.fullName||'Administrador institucional'}}</h3><p class="break-all text-sm text-slate-500">{{user.email}}</p><p class="mt-2 text-sm text-slate-600">{{user.role==='CUSTOM'?roleNames(user):'Administrador del tenant'}}</p></div><span class="org-badge" [attr.data-status]="user.active?'ACTIVE':'INACTIVE'">{{user.active?'Activo':'Desactivado'}}</span></div>
 @if(user.role==='CUSTOM'&&user.active){<div class="mt-3 flex gap-4"><button class="org-link" (click)="editUser(user)" [disabled]="busy()">Editar usuario</button><button class="text-sm font-semibold text-red-700" (click)="disable(user)" [disabled]="busy()">Desactivar</button></div>}</article>}
 </section>
 <form class="org-card self-start" [formGroup]="userForm" (ngSubmit)="saveUser()"><h2 class="mb-5 text-xl font-bold">{{editingUser()?'Editar usuario':'Crear usuario'}}</h2>
 <label class="org-label">Nombre completo<input class="org-input" formControlName="fullName" maxlength="180"></label>
 @if(!editingUser()){<label class="org-label mt-4">Correo electrónico<input class="org-input" type="email" formControlName="email" maxlength="180" autocomplete="off"></label><label class="org-label mt-4">Contraseña inicial<input class="org-input" type="password" formControlName="password" maxlength="72" autocomplete="new-password"><span class="org-hint">Entre 10 y 72 caracteres. Compártela directamente con el encargado.</span></label>}
 <label class="org-label mt-4">Roles asignados<select class="org-input" multiple size="5" formControlName="roleIds">@for(role of roles();track role.id){<option [value]="role.id">{{role.name}}</option>}</select><span class="org-hint">Puedes seleccionar varios. Un usuario sin roles no tiene permisos institucionales.</span></label>
 @if(!roles().length){<p class="org-info mt-4">Crea primero un rol en “Roles y permisos”.</p>}
 @if(userForm.invalid&&userForm.touched){<p class="mt-4 text-sm text-red-700">Revisa el nombre, correo y contraseña.</p>}
 <div class="mt-6 flex flex-wrap gap-3"><button class="org-primary" [disabled]="busy()">{{busy()?'Guardando…':editingUser()?'Guardar cambios':'Crear usuario'}}</button>@if(editingUser()){<button type="button" class="org-secondary" (click)="resetUser()">Cancelar</button>}</div>
 </form></div>
 } @else {
 <div class="grid gap-6 xl:grid-cols-[1fr_1.2fr]"><section class="org-card"><h2 class="mb-5 text-xl font-bold">Roles de la organización</h2>
 @if(!roles().length){<p class="py-8 text-slate-500">Aún no tienes roles personalizados. Crea uno para comenzar.</p>}
 @for(role of roles();track role.id){<article class="border-b border-slate-200 py-5 last:border-0"><h3 class="font-semibold">{{role.name}}</h3><p class="mt-1 text-sm text-slate-500">{{role.permissions.length}} permisos asignados</p><div class="mt-3 flex gap-4"><button class="org-link" [disabled]="busy()" (click)="editRole(role)">Editar rol</button><button class="text-sm font-semibold text-red-700" [disabled]="busy()" (click)="removeRole(role)">Eliminar rol</button></div></article>}
 </section>
 <form class="org-card" [formGroup]="roleForm" (ngSubmit)="saveRole()"><h2 class="mb-5 text-xl font-bold">{{editingRole()?'Editar rol':'Nuevo rol'}}</h2><label class="org-label">Nombre del rol<input class="org-input" formControlName="name" maxlength="100" placeholder="Ejemplo: Emisor de certificados"></label>
 <fieldset class="mt-6"><legend class="mb-4 font-semibold">Permisos disponibles</legend><div class="grid gap-3 sm:grid-cols-2">
 @for(permission of permissions();track permission){<label class="flex items-start gap-3 rounded-lg border border-slate-200 p-3 text-sm"><input type="checkbox" class="mt-1" [checked]="selectedPermissions().includes(permission)" (change)="togglePermission(permission,$any($event.target).checked)">{{permissionLabels[permission]||permission}}</label>}
 </div></fieldset><p class="org-hint mt-4">Los permisos administrativos de usuarios y roles están reservados al administrador. Identidad, reportes y auditoría se habilitarán al incorporar sus módulos.</p>
 @if(roleForm.invalid&&roleForm.touched){<p class="mt-4 text-sm text-red-700">Escribe el nombre del rol.</p>}
 <div class="mt-6 flex flex-wrap gap-3"><button class="org-primary" [disabled]="busy()">{{busy()?'Guardando…':editingRole()?'Guardar rol':'Crear rol'}}</button>@if(editingRole()){<button type="button" class="org-secondary" (click)="resetRole()">Cancelar</button>}</div>
 </form></div>
 }
 </section>`})
export class OrganizationTeamPage {
 private readonly api=inject(OrganizationApi);private readonly fb=inject(FormBuilder);private readonly dialog=inject(Dialog);
 readonly tab=signal<'users'|'roles'>('users');readonly users=signal<StaffUser[]>([]);readonly roles=signal<TenantRole[]>([]);
 readonly permissions=signal<string[]>([]);readonly permissionLabels=PERMISSION_LABELS;readonly selectedPermissions=signal<string[]>([]);
 readonly loading=signal(true);readonly busy=signal(false);readonly error=signal('');readonly notice=signal('');readonly editingUser=signal<string|null>(null);readonly editingRole=signal<string|null>(null);
 readonly userForm=this.fb.nonNullable.group({fullName:['',[Validators.required,Validators.maxLength(180)]],email:['',[Validators.required,Validators.email,Validators.maxLength(180)]],password:['',[Validators.required,Validators.minLength(10),Validators.maxLength(72)]],roleIds:[[] as string[]]});
 readonly roleForm=this.fb.nonNullable.group({name:['',[Validators.required,Validators.maxLength(100)]]});
 constructor(){this.load();}
 load(){this.loading.set(true);this.error.set('');forkJoin({users:this.api.users(),roles:this.api.roles(),permissions:this.api.permissions()}).pipe(finalize(()=>this.loading.set(false))).subscribe({next:r=>{this.users.set(r.users);this.roles.set(r.roles);this.permissions.set(r.permissions);},error:e=>this.error.set(requestError(e))});}
 roleNames(user:StaffUser){return user.roleIds.map(id=>this.roles().find(r=>r.id===id)?.name).filter(Boolean).join(', ')||'Sin roles asignados';}
 editUser(u:StaffUser){this.resetUser();this.editingUser.set(u.id);this.userForm.controls.email.clearValidators();this.userForm.controls.password.clearValidators();this.userForm.patchValue({fullName:u.fullName??'',roleIds:u.roleIds});this.userForm.controls.email.updateValueAndValidity();this.userForm.controls.password.updateValueAndValidity();}
 resetUser(){this.editingUser.set(null);this.userForm.reset({roleIds:[]});this.userForm.controls.email.setValidators([Validators.required,Validators.email]);this.userForm.controls.password.setValidators([Validators.required,Validators.minLength(10),Validators.maxLength(72)]);this.userForm.controls.email.updateValueAndValidity();this.userForm.controls.password.updateValueAndValidity();}
 saveUser(){this.userForm.markAllAsTouched();if(this.userForm.invalid||this.busy())return;const v=this.userForm.getRawValue();const id=this.editingUser();this.run(id?this.api.updateUser(id,{fullName:v.fullName,roleIds:v.roleIds}):this.api.createUser(v),'Usuario guardado.',()=>this.resetUser());}
 togglePermission(p:string,on:boolean){this.selectedPermissions.update(v=>on?[...v,p]:v.filter(x=>x!==p));}
 editRole(r:TenantRole){this.editingRole.set(r.id);this.roleForm.setValue({name:r.name});this.selectedPermissions.set([...r.permissions]);}
 resetRole(){this.editingRole.set(null);this.roleForm.reset();this.selectedPermissions.set([]);}
 saveRole(){this.roleForm.markAllAsTouched();if(this.roleForm.invalid||this.busy())return;this.run(this.api.saveRole(this.editingRole(),{name:this.roleForm.getRawValue().name,permissions:this.selectedPermissions()}),'Rol guardado. Los permisos ya están actualizados.',()=>this.resetRole());}
 private confirm(title:string,message:string,action:()=>void){this.dialog.open<boolean>(ConfirmDialogComponent,{data:{title,message},ariaLabelledBy:'confirm-title',autoFocus:'first-tabbable',restoreFocus:true}).closed.subscribe(confirmed=>{if(confirmed)action();});}
 disable(u:StaffUser){this.confirm('Desactivar usuario','Se bloqueará el acceso de '+(u.fullName||u.email)+'.',()=>this.run(this.api.disableUser(u.id),'Usuario desactivado.'));}
 removeRole(r:TenantRole){this.confirm('Eliminar rol','Se retirará el rol '+r.name+' de todos los usuarios que lo tienen asignado.',()=>this.run(this.api.deleteRole(r.id),'Rol eliminado.',()=>this.resetRole()));}
 private run<T>(request:Observable<T>,message:string,done:()=>void=()=>{}){if(this.busy())return;this.busy.set(true);this.error.set('');this.notice.set('');request.pipe(finalize(()=>this.busy.set(false))).subscribe({next:()=>{done();this.notice.set(message);this.load();},error:e=>this.error.set(requestError(e))});}
}

