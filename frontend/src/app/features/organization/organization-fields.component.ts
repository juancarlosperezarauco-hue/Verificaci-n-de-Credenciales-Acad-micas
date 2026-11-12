import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { OrgField } from './organization.form';
@Component({
 selector:'app-organization-fields', imports:[ReactiveFormsModule],
 template:`
 <div [formGroup]="form()" class="grid gap-5 sm:grid-cols-2">
 @for (field of fields(); track field.key) {
  <label class="org-label" [for]="prefix()+field.key">{{field.label}} @if(field.optional){<span class="font-normal text-slate-500">(opcional)</span>}
   @if(field.options) {
    <select class="org-input" [id]="prefix()+field.key" [formControlName]="field.key" [multiple]="field.type==='multiple'" [attr.size]="field.type==='multiple'?5:null" [attr.aria-describedby]="prefix()+field.key+'-hint'">
     @if(field.type!=='multiple'){<option value="">Selecciona una opción</option>}
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
 </div>`
})
export class OrganizationFieldsComponent {
 readonly form=input.required<FormGroup>(); readonly fields=input.required<OrgField[]>(); readonly prefix=input('org-');
}
