import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { OrgField } from './organization.form';
@Component({
 selector:'app-organization-fields', imports:[ReactiveFormsModule],
 template:`
 <div [formGroup]="form()" class="grid gap-5 sm:grid-cols-2">
 @for (field of fields(); track field.key) {
  @if(field.type==='multiple') {
   <fieldset class="org-label sm:col-span-2" [attr.aria-describedby]="prefix()+field.key+'-hint'">
    <legend>{{field.label}}</legend>
    <div class="mt-2 grid gap-2 sm:grid-cols-3">
     @for(option of field.options;track option.value){
      <label class="flex cursor-pointer items-center gap-3 rounded-xl border border-slate-200 p-3 font-normal">
       <input type="checkbox" [checked]="form().get(field.key)?.value.includes(option.value)"
        (change)="toggle(field.key,option.value,$any($event.target).checked)">{{option.label}}
      </label>
     }
    </div>
    <span class="org-hint" [id]="prefix()+field.key+'-hint'">{{field.hint}}</span>
    @if(form().get(field.key)?.invalid && form().get(field.key)?.touched){<span class="mt-1 block text-xs text-red-700">Selecciona al menos un nivel educativo.</span>}
   </fieldset>
  } @else {
  <label class="org-label" [for]="prefix()+field.key">{{field.label}} @if(field.optional){<span class="font-normal text-slate-500">(opcional)</span>}
   @if(field.options) {
    <select class="org-input" [id]="prefix()+field.key" [formControlName]="field.key" [attr.aria-describedby]="prefix()+field.key+'-hint'">
     <option value="">Selecciona una opción</option>
     @for(option of field.options;track option.value){<option [value]="option.value">{{option.label}}</option>}
    </select>
   } @else {
    <input class="org-input" [id]="prefix()+field.key" [type]="field.type??'text'" [formControlName]="field.key"
     [attr.maxlength]="field.max??(field.type==='password'?72:null)" [attr.autocomplete]="field.type==='password'?'new-password':null"
     [attr.aria-invalid]="form().get(field.key)?.invalid && form().get(field.key)?.touched"
     [attr.aria-describedby]="prefix()+field.key+'-hint'">
   }
   <span class="mt-1 block text-xs font-normal text-slate-500" [id]="prefix()+field.key+'-hint'">{{field.hint}}</span>
   @if(form().get(field.key)?.invalid && form().get(field.key)?.touched){<span class="mt-1 block text-xs text-red-700">Completa este campo con un valor válido.</span>}
  </label>
  }
 }
 </div>`
})
export class OrganizationFieldsComponent {
 readonly form=input.required<FormGroup>(); readonly fields=input.required<OrgField[]>(); readonly prefix=input('org-');
 toggle(key:string,value:string,checked:boolean){
  const control=this.form().get(key);const values=control?.value as string[];
  control?.setValue(checked?[...new Set([...values,value])]:values.filter(v=>v!==value));
  control?.markAsTouched();control?.markAsDirty();
 }
}
