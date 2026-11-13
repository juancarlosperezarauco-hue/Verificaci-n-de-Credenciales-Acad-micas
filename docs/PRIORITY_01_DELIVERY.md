# Prioridad 1 — revisión institucional y seguridad de cuentas

Fecha: 2026-11-13. Continuación de `eb894bf` en `codex/rbac-superadmin`.
Historias: SEC-01 y SEC-02 de SPRINT_01_V3. Módulos M02/M03/M04.

## Resultado funcional

### Revisión institucional

Las dos rutas públicas de registro crean una organización PENDING_REVIEW, con
active=false. El TENANT_ADMIN puede ingresar para completar datos, logo y respaldo,
pero no puede gestionar estudiantes, emitir, publicar, crear roles o empleados.
El backend aplica una lista cerrada de rutas de onboarding además de los permisos;
compartir ORGANIZATION_UPDATE con el portal académico no habilita publicaciones.

El SUPER_ADMIN aprueba, observa o rechaza desde su panel existente. La organización
observada ve el motivo, guarda sus correcciones y las reenvía a revisión. El reenvío
se registra con actor/fecha y es idempotente si ya está pendiente. Las transiciones
se validan en servidor y la interfaz utiliza la lista permitida que devuelve la API.

La aprobación requiere el perfil institucional completo, con los identificadores
legales opcionales acordados. Los registros históricos ACTIVE conservan su acceso.
Si una organización histórica suspendida tiene un perfil incompleto, el superadmin
puede pasarla a OBSERVED para que su administrador complete los datos antes de
reactivarla. Guardar datos nunca aprueba ni reactiva automáticamente.

Los JWT se revisan contra el estado actual: un token pendiente obtiene acceso
operativo tras aprobar y lo pierde al observar/suspender. Durante revisión solo
el administrador ingresa; los trabajadores CUSTOM permanecen bloqueados.

### Cuenta global del estudiante

- Confirmación de correo y reenvío del enlace.
- Recuperación con respuesta pública uniforme y enlace de 30 minutos.
- Cambio de contraseña con contraseña actual y cierre de sesiones anteriores.
- Cambio de correo confirmado en el nuevo buzón, conservando la misma identidad.
- Desactivación lógica con contraseña y confirmación visual; se retiran los vínculos
  activos, se conserva el estudiante y permanecen las credenciales históricas.
- Tokens aleatorios de uso único, vencimiento y solo SHA-256 en PostgreSQL.
- Serialización por cuenta/token y security_version en JWT, comparada en cada petición.
- Envío de correo después del commit, asíncrono y con cola/tiempos de espera acotados.

Se revisó el trabajo pendiente del checkout `Verificacion-Credenciales`, sin escribir
en él. Se reutilizan las entidades de token de V9/V10 y sus nombres de rutas; no se
copió íntegramente el módulo inacabado de fotografía/CI ni se duplicaron tablas.
Se corrigieron sus carencias de invalidación de sesiones, bloqueo concurrente,
envío de correo y protección de enlaces/logs antes de integrar.

## Datos y contratos

- Nueva migración **V14__student_security_version.sql**: añade security_version,
  valor inicial 0, sin borrar registros. V1–V13 no se modifican.
- Se activan los campos active/email_verified/deactivated_at y la tabla de tokens
  ya existentes en V9/V10. No se convierte al estudiante en trabajador institucional.
- Contratos documentados antes del código en ORGANIZATION_API.md y STUDENT_SECURITY_API.md.
- `GET /api/access/me` añade onboarding; resúmenes institucionales añaden reviewNote
  y allowedTransitions. DTO de registro/login conserva compatibilidad.
- Nuevo `POST /api/institution/submit-review`.
- Nuevas rutas estudiantiles detalladas en STUDENT_SECURITY_API.md.
- No se agregan permisos globales ni dependencias de terceros.

## Seguridad y límites

JWT_SECRET es obligatorio y se rechazan las claves públicas anteriores. La clave
local se sustituyó por un valor aleatorio en .env, excluido de Git; esto exige
iniciar sesión de nuevo al actualizar el contenedor. Se elimina la autoconfiguración
de usuario de Spring para evitar credenciales automáticas en logs.

Los enlaces de correo se retiran de la URL al cargar la página. Nginx no los incluye
en access logs y aplica no-store/no-referrer. Se comprobó con un marcador de prueba.
La confirmación de correo requiere una acción explícita en la página; un GET no
consume el token. No se probaron cambios de contraseña desde el navegador: esas
operaciones se validaron mediante fixtures HTTP/JUnit en el entorno aislado.

**Queda pendiente:** MFA administrativo, listado/revocación de sesiones individuales,
rate limiting de autenticación/recuperación, outbox durable para correos y protección
operativa de producción. El envío asíncrono puede perderse si el proceso cae o la
cola se llena; la interfaz permite solicitar otro enlace y no garantiza entrega.
El correo no confirmado se indica en pantalla sin bloquear retroactivamente los
expedientes existentes. La verificación documental de identidad es otra historia.

## Evidencia visual

Se revisaron escritorio 1280×900 y móvil 390×844 con datos ficticios en Docker aparte:

- `docs/evidence/priority-onboarding-desktop.jpg`: observación visible y menú restringido.
- `docs/evidence/priority-onboarding-mobile.jpg`: reenvío confirmado como PENDING_REVIEW.
- `docs/evidence/priority-student-security-desktop.jpg` y `-mobile.jpg`: formularios de cuenta.
- `docs/evidence/priority-recovery-desktop.jpg` y `-mobile.jpg`: recuperación y respuesta uniforme.

No hubo desbordamiento horizontal del contenido. El acceso directo a /credentials
se redirigió a /organization para el solicitante. Se restableció el viewport y se
cerró la pestaña temporal. No se modificaron permisos ni contraseñas de usuarios reales.

## Reproducción

```powershell
# Backend: base PostgreSQL 16 NUEVA y separada, variables datasource de prueba;
# SPRING_FLYWAY_ENABLED=true y SPRING_JPA_HIBERNATE_DDL_AUTO=validate.
cd backend
.\mvnw.cmd verify
cd ../frontend
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

Con el JAR y dist ya generados, desde la raíz y un archivo de entorno de prueba ignorado:

```powershell
docker compose --env-file .local-backups/audit-compose.env -p credentials-v3-audit -f compose.audit.yml up -d --build
# En variables del proceso: SUPER_ADMIN exclusivo del entorno de prueba.
.\scripts\smoke-organization.ps1 -SuperEmail $testEmail -SuperPassword $testPassword
.\scripts\smoke-student-security.ps1
# El smoke documental requiere token institucional y estudiante vinculado del fixture.
.\scripts\smoke-document-integrity.ps1 -Token $testToken -StudentId $testStudent
docker compose --env-file .local-backups/audit-compose.env -p credentials-v3-audit -f compose.audit.yml stop
```

PowerShell 7. El Compose aislado agrega frontend 4301, API 18081 y Mailpit 18125,
todos ligados a loopback; DB/MinIO propios. No ejecutar smokes sobre datos de usuario.
Los resultados finales de pruebas y ejecución local se registran en el handoff de
esta entrega. Logs locales `priority-*.log` se mantienen fuera de Git.

## Verificación final y ejecución local

| Comprobación | Resultado |
|---|---|
| Maven verify, PostgreSQL 16 nuevo, Flyway habilitado y Hibernate validate | **30 pruebas**, 0 fallos/errores/omitidas; BUILD SUCCESS. |
| Flyway | V1–V14 desde cero; upgrade V13→V14 en Compose aislado y stack principal. |
| Angular producción | Build correcto. |
| Jasmine/Karma/ChromeHeadless | **36 pruebas** aprobadas. |
| Total distinto | **66 pruebas**; no se suman smokes manuales. |
| Smoke institucional PostgreSQL/MinIO | PASS, incluyendo bloqueo previo y aprobación con el mismo JWT. |
| Smoke de integridad documental | PASS, documentos originales conservados tras rechazos. |
| Smoke de seguridad con Mailpit real | PASS: verificación, replay, recuperación, sesiones previas, cambio de email e identidad conservada. |
| Marcador de enlace en logs Nginx | No aparece; HTTP 200, no-store/no-referrer. |
| QA visual | Escritorio y móvil; sin desbordamiento; reenvío y redirección restringida comprobados. |

La prueba adicional de desactivación verifica que se retiran accesos institucionales
sin eliminar el estudiante ni invalidar una credencial histórica emitida. Durante
su preparación se corrigió el código de verificación del fixture a mayúsculas;
la última suite completa pasó 30/30. Se corrigió asimismo la enumeración de
destinatarios de Mailpit en el script PowerShell antes de su resultado PASS.

El stack principal ya se actualizó con imágenes de backend/frontend de esta entrega:
web **http://localhost:4300**, recuperación `/student/recover`, seguridad
`/student/security`, revisión global `/superadmin/organizations` y Mailpit
**http://localhost:8125**. API UP, login SUPER_ADMIN comprobado y Flyway 14 exitoso.
No se enviaron correos a proveedores externos: el entorno usa Mailpit.

Antes de migrar se obtuvo el respaldo local ignorado
`.local-backups/credentials-before-priority-20261113-052750.dump` (45 591 bytes),
legible mediante `pg_restore --list`. No se afirma un ensayo completo de restauración
de este nuevo respaldo. No se borraron bases, volúmenes ni documentos.

La clave JWT de ejemplo se sustituyó localmente; se comprobó clave privada configurada
y ausencia del mensaje de contraseña automática en el backend. Las sesiones
anteriores deben iniciar de nuevo. El Compose de auditoría y el PostgreSQL dedicado
quedaron detenidos; los seis servicios principales continúan activos.
