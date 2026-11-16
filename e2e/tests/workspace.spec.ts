import { test,expect,Page } from '@playwright/test';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
const fixture=JSON.parse(readFileSync(resolve(import.meta.dirname,'../test-results/fixture.json'),'utf8'));
async function login(page:Page,student=false){
  await page.goto(student?'/student/login':'/login');
  if(!student)await page.getByLabel('Identificador de la organización').fill(fixture.slug);
  await page.getByLabel('Correo electrónico',{exact:true}).fill(student?fixture.studentEmail:fixture.adminEmail);
  await page.getByLabel('Contraseña',{exact:true}).fill(fixture.password);
  await page.getByRole('button',{name:'Ingresar',exact:true}).click();
  await expect(page).toHaveURL(student?/\/student\/account$/:/\/dashboard$/);
}
async function noOverflow(page:Page){
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBeTruthy();
}
test('left drawer opens vertically, restores focus and navigates to permitted reports',async({page},info)=>{
  await login(page);
  const trigger=page.getByRole('button',{name:'Abrir menú lateral'});
  await trigger.click();
  const drawer=page.locator('.drawer');
  await expect(drawer).toBeVisible();expect((await drawer.boundingBox())?.x).toBe(0);
  const links=drawer.locator('.drawer-link');const a=await links.nth(0).boundingBox();const b=await links.nth(1).boundingBox();
  expect(b!.y).toBeGreaterThanOrEqual(a!.y+a!.height);
  await page.keyboard.press('Escape');await expect(drawer).not.toBeVisible();await expect(trigger).toBeFocused();
  await trigger.click();await page.getByRole('link',{name:'Reportes y estadísticas',exact:true}).click();
  await expect(page.getByRole('heading',{name:'Reportes y estadísticas'})).toBeVisible();
  await expect(drawer).not.toBeVisible();await noOverflow(page);
  await page.screenshot({path:info.outputPath('reports.png'),fullPage:true});
});
test('reports apply filters and download readable PDF and Excel',async({page})=>{
  await login(page);await page.goto('/organization/reports');
  await page.getByRole('combobox',{name:'Estado',exact:true}).selectOption('ISSUED');
  await page.getByRole('button',{name:'Aplicar filtros'}).click();
  await expect(page.getByText('Certificado ficticio de prueba E2E',{exact:true})).toBeVisible();
  for(const [name,extension] of [['Descargar PDF','.pdf'],['Descargar Excel','.xlsx']]){
    const downloading=page.waitForEvent('download');await page.getByRole('button',{name,exact:true}).click();
    const file=await downloading;expect(file.suggestedFilename().endsWith(extension)).toBeTruthy();expect(await file.failure()).toBeNull();
    const stream=await file.createReadStream();const chunks=[];for await(const c of stream!)chunks.push(c);
    const bytes=Buffer.concat(chunks);expect(bytes.length).toBeGreaterThan(100);
    expect(bytes.subarray(0,extension==='.pdf'?4:2).toString()).toBe(extension==='.pdf'?'%PDF':'PK');
  }
});
test('student sees own paginated documents and can explicitly read notifications',async({page},info)=>{
  await login(page,true);
  const section=page.getByRole('region',{name:'Mis diplomas y certificados'});
  await expect(section.getByText('Certificado ficticio de prueba E2E',{exact:true})).toBeVisible();
  await section.getByRole('combobox',{name:'Institución',exact:true}).selectOption(fixture.tenantId);
  await section.getByRole('combobox',{name:'Tipo de documento',exact:true}).selectOption('CERTIFICADO');
  await section.getByRole('button',{name:'Aplicar filtros'}).click();
  await expect(section.getByText('1 documentos encontrados',{exact:true})).toBeVisible();
  await noOverflow(page);await page.screenshot({path:info.outputPath('student.png'),fullPage:true});
  await page.goto('/student/notifications');await expect(page.getByRole('heading',{name:'Notificaciones',exact:true})).toBeVisible();
  await expect(page.getByText('Tu solicitud necesita correcciones',{exact:true})).toBeVisible();
  const mark=page.getByRole('button',{name:'Marcar todas como leídas'});
  if(await mark.isEnabled())await mark.click();
  await expect(page.getByText('0 sin leer',{exact:true})).toBeVisible();await noOverflow(page);
});
test('public verification stays available while institutional routes require a session',async({page})=>{
  await page.goto('/organization/reports');await expect(page).not.toHaveURL(/\/organization\/reports$/);
  await page.goto('/verify/'+fixture.verificationCode);await expect(page.getByText('Certificado ficticio de prueba E2E · CERTIFICADO',{exact:true})).toBeVisible();
  await noOverflow(page);
});
