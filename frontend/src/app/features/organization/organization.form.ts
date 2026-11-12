import { FormControl, FormGroup, Validators } from '@angular/forms';
import { OrganizationDetails } from './organization.models';
import { InstitutionType } from '../../core/models';
export interface OrgField { key:string; label:string; type?:string; hint?:string; optional?:boolean; max?:number; options?:{value:string;label:string}[]; }
export const DETAIL_SECTIONS:{title:string; fields:OrgField[]}[] = [
 {title:'Identificación',fields:[
 {key:'legalName',label:'Nombre legal o razón social',max:160},
 {key:'institutionType',label:'Tipo de institución',options:[{value:'ESCUELA',label:'Escuela'},{value:'COLEGIO',label:'Colegio'},{value:'UNIVERSIDAD',label:'Universidad'},{value:'INSTITUTO',label:'Instituto'},{value:'INSTITUTO_TECNICO',label:'Instituto técnico'},{value:'ACADEMIA',label:'Academia'},{value:'CENTRO_FORMACION',label:'Centro de formación'},{value:'OTRO',label:'Otro'}]},
 {key:'educationLevels',label:'Niveles educativos',type:'multiple',hint:'Puedes seleccionar varios niveles con Ctrl o Command.',options:[{value:'INICIAL',label:'Inicial'},{value:'PRIMARIA',label:'Primaria'},{value:'SECUNDARIA',label:'Secundaria'},{value:'SUPERIOR',label:'Superior'},{value:'TECNICO',label:'Técnico'}]}]},
 {title:'Ubicación y contacto',fields:[
 {key:'country',label:'País (código de dos letras)',hint:'Ejemplos: BO Bolivia, PE Perú, AR Argentina.',max:2},
 {key:'region',label:'Departamento o región',max:120},{key:'address',label:'Dirección física completa',max:500},
 {key:'phone',label:'Teléfono de contacto',type:'tel',max:40},{key:'contactEmail',label:'Correo de contacto',type:'email',max:180},
 {key:'website',label:'Sitio web',type:'url',optional:true,hint:'Incluye https://',max:255},{key:'domain',label:'Dominio institucional',optional:true,hint:'Ejemplo: colegio.edu.bo, sin https://',max:180}]},
 {title:'Información legal',fields:[
 {key:'taxId',label:'Identificación fiscal (NIT / RUC / CUIT)',max:80},
 {key:'educationLicense',label:'Número de licencia educativa',max:120},
 {key:'ministryRegistration',label:'Registro ante el Ministerio de Educación',optional:true,max:180},
 {key:'accreditation',label:'Acreditación oficial',optional:true,max:500}]},
 {title:'Representante',fields:[
 {key:'representativeName',label:'Nombre completo del representante',max:180},
 {key:'documentType',label:'Tipo de documento',max:30},{key:'documentNumber',label:'Número de documento',max:80},
 {key:'position',label:'Cargo',hint:'Director, rector, administrador…',max:100},
 {key:'representativeEmail',label:'Correo institucional del administrador',type:'email',max:180},
 {key:'representativePhone',label:'Teléfono del representante',type:'tel',max:40}]}
];
export const ACCOUNT_FIELDS:OrgField[]=[
 {key:'organizationSlug',label:'Identificador de acceso de la organización',hint:'Letras minúsculas, números y guiones. Ejemplo: universidad-demo',max:80},
 {key:'password',label:'Contraseña del administrador',type:'password',hint:'Entre 10 y 72 caracteres.'},
 {key:'confirmPassword',label:'Repite la contraseña',type:'password'}
];
export function buildDetailsForm(registration=false):FormGroup {
 const controls:Record<string,FormControl>={};
 for(const f of [...DETAIL_SECTIONS.flatMap(s=>s.fields),...(registration?ACCOUNT_FIELDS:[])]) {
  const validators = f.optional ? [] : [Validators.required];
  if(f.max) validators.push(Validators.maxLength(f.max));
  if(f.type==='email') validators.push(Validators.email);
  if(f.key==='country') validators.push(Validators.pattern(/^[a-z]{2}$/i));
  if(f.key==='website') validators.push(Validators.pattern(/^$|https?:\/\/[^\s]+$/));
  if(f.key==='domain') validators.push(Validators.pattern(/^$|[a-z0-9-]+(\.[a-z0-9-]+)+$/i));
  if(f.key==='organizationSlug') validators.push(Validators.pattern(/^[a-z0-9][a-z0-9-]{2,79}$/));
  if(f.type==='password') validators.push(Validators.minLength(10),Validators.maxLength(72));
  controls[f.key]=new FormControl(f.type==='multiple'?[]:'', {nonNullable:true, validators});
 }
 return new FormGroup(controls);
}
export function toDetails(form:FormGroup):OrganizationDetails {
 const v=form.getRawValue();return {
 legalName:v.legalName,institutionType:v.institutionType as InstitutionType,educationLevels:v.educationLevels,
 country:v.country.toUpperCase(),region:v.region,address:v.address,phone:v.phone,contactEmail:v.contactEmail,
 website:v.website,domain:v.domain,taxId:v.taxId,educationLicense:v.educationLicense,
 ministryRegistration:v.ministryRegistration,accreditation:v.accreditation,
 representative:{fullName:v.representativeName,documentType:v.documentType,documentNumber:v.documentNumber,position:v.position,email:v.representativeEmail,phone:v.representativePhone}
 };
}
export function fillDetails(form:FormGroup,d:OrganizationDetails) {
 form.patchValue({...d,representativeName:d.representative.fullName,documentType:d.representative.documentType,
 documentNumber:d.representative.documentNumber,position:d.representative.position,representativeEmail:d.representative.email,
 representativePhone:d.representative.phone});
}

