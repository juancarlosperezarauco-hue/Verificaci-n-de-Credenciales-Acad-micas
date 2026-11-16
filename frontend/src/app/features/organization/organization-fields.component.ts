import { Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';
import { OrgField } from './organization.form';
import { BOLIVIA_DEPARTMENTS } from './organization-options';
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
  <label [class]="field.fullWidth?'org-label sm:col-span-2':'org-label'" [for]="prefix()+field.key">{{field.label}} @if(field.optional){<span class="font-normal text-slate-500">(opcional)</span>}
   @if(options(field);as choices) {
    <select class="org-input" [id]="prefix()+field.key" [formControlName]="field.key" [attr.aria-describedby]="prefix()+field.key+'-hint'" [attr.aria-invalid]="invalid(field)" (change)="selectionChanged(field)">
     <option value="">{{field.placeholder||'Selecciona una opción'}}</option>
     @for(option of choices;track option.value){<option [value]="option.value">{{option.label}}</option>}
     @if(legacyValue(field,choices);as value){<option [value]="value">{{value}} (valor guardado)</option>}
    </select>
   } @else {
    <input class="org-input" [id]="prefix()+field.key" [type]="field.type??'text'" [formControlName]="field.key"
     [attr.maxlength]="field.max??(field.type==='password'?72:null)" [attr.autocomplete]="field.type==='password'?'new-password':null"
     [attr.placeholder]="field.placeholder??null"
     [attr.aria-invalid]="form().get(field.key)?.invalid && form().get(field.key)?.touched"
     [attr.aria-describedby]="prefix()+field.key+'-hint'">
   }
   <span class="mt-1 block text-xs font-normal text-slate-500" [id]="prefix()+field.key+'-hint'">{{field.hint}}</span>
   @if(invalid(field)){<span class="mt-1 block text-xs text-red-700">{{options(field)?'Selecciona una opción para continuar.':'Completa este campo con un valor válido.'}}</span>}
  </label>
  }
 }
 </div>`
})
export class OrganizationFieldsComponent {
 readonly form=input.required<FormGroup>(); readonly fields=input.required<OrgField[]>(); readonly prefix=input('org-');
 options(field:OrgField){return field.key==='region'&&this.form().get('country')?.value?.toUpperCase()==='BO'?BOLIVIA_DEPARTMENTS:field.options;}
 invalid(field:OrgField){const control=this.form().get(field.key);return !!(control?.invalid&&control.touched);}
 legacyValue(field:OrgField,choices:{value:string;label:string}[]){const value=this.form().get(field.key)?.value as string;return value&&!choices.some(option=>option.value===value)?value:null;}
 selectionChanged(field:OrgField){if(field.key==='country'){this.form().get('region')?.reset('');}}
 toggle(key:string,value:string,checked:boolean){
  const control=this.form().get(key);const values=control?.value as string[];
  control?.setValue(checked?[...new Set([...values,value])]:values.filter(v=>v!==value));
  control?.markAsTouched();control?.markAsDirty();
 }
}
