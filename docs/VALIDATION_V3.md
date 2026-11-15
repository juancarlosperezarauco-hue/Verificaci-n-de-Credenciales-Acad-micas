# Validación del incremento V3

Fecha: 2026-11-13, America/La_Paz. Rama: `codex/rbac-superadmin`.
Base auditada: `0f72612`. No se modifica main, ninguna migración V1–V13 ni datos
de la aplicación principal. Los cambios de código fueron probados en entorno
aislado; el Docker principal conserva la versión anterior hasta actualizarlo.

## Resultados

| Verificación | Resultado |
|---|---|
| Baseline backend PostgreSQL 16 + Flyway + Hibernate validate | 11 pruebas, 0 fallos/errores/omitidas; Maven verify y empaquetado correctos. |
| Baseline frontend Angular/Karma/ChromeHeadless | 24 pruebas aprobadas. |
| Compilación producción Angular | Correcta. |
| Instalación de base nueva | V1–V13 aplicadas; contextos posteriores validan las 13 migraciones. |
| Historial de base operativa, consulta de solo lectura | Versiones 1–13 exitosas. |
| Regresión antes del arreglo | 1 prueba falla porque hay escritura de almacenamiento después de emitir. |
| Backend PostgreSQL después del arreglo | 16 pruebas, 0 fallos/errores/omitidas; BUILD SUCCESS. |
| Compatibilidad con perfil test H2 por defecto | 16 pruebas, 0 fallos/errores/omitidas; Maven verify correcto. |
| Smoke HTTP con API + PostgreSQL + MinIO reales | PASS: organización, rol, trabajador, estudiante, vínculo, PDF, descarga, emisión, expediente, consulta, revocación y suspensión/restauración. |
| Smoke documental con MinIO real | PASS: PDF propio cargado y emitido; mismo nombre reenviado se rechaza en ISSUED/REVOKED; bytes y hash original conservados. |
| Migraciones históricas modificadas | Ninguna; no se crea V14. |
| git diff --check | Correcto. |

Total de casos distintos: **40 = 16 backend + 24 frontend**. Las comprobaciones
HTTP manuales no se suman como casos JUnit/Jasmine. La repetición de los 16 casos
backend en H2 tampoco duplica el total de pruebas distintas.

Los cinco casos nuevos están en `CredentialDocumentIntegrityIntegrationTests`:

1. Documento emitido/revocado rechaza reemplazo antes del almacenamiento.
2. Tenant ajeno, estudiante y trabajador lector no pueden reemplazarlo; tenant ajeno
   tampoco puede descargarlo.
3. Fallo simulado después de escribir no altera el objeto previamente confirmado.
4. Reintento de emisión rechazado no repite la invocación externa.
5. Carga y emisión concurrentes se serializan y conservan hash/documento coherentes.

La primera ejecución ampliada tuvo dos errores en la configuración de mocks de los
casos nuevos (restubbing invocaba el comportamiento anterior). Se corrigió usando
`doAnswer`; la suite completa se volvió a ejecutar con resultado 16/16 correcto.

## Entornos y evidencia

- Suite PG: contenedor dedicado `codex-rbac-postgres-check`, PostgreSQL 16.15,
  puerto 15432, base nueva con prefijo `audit_final_` y sufijo de fecha UTC generado
  por el comando de prueba; no se utiliza la base `credentials` del usuario.
- Smoke real: proyecto Compose `credentials-v3-audit`, base `credentials_audit`,
  almacenamiento en MinIO propio y bucket `audit-credentials`. API ligada a
  `127.0.0.1:18081`. No comparte volúmenes con el proyecto principal.
- Credencial del smoke completo: `fd972ff1-2827-446d-96dd-b378d08c13b4`.
- Credencial de integridad: `4ddf4de9-9f7e-4e1a-a8fa-30bcb1cc93ce`.
- Logs locales ignorados: `backend/audit-baseline.log`,
  `backend/audit-regression-before.log`, `backend/audit-final.log`,
  `backend/audit-h2.log`, `frontend/audit-baseline.log`, `frontend/audit-build.log`.
- Los secretos de prueba se generaron localmente en
  `.local-backups/audit-compose.env` (ignorado), nunca se incluyeron en código o Git.
- Al acabar se detienen los tres contenedores del proyecto de auditoría y se
  devuelve `codex-rbac-postgres-check` a su estado previo detenido. Se conservan
  los recursos de prueba para revisar evidencia, separados de datos del usuario.

## Comandos usados

Desde `frontend/`:

```powershell
$env:CHROME_BIN = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
npm test -- --watch=false --browsers=ChromeHeadless
npm run build
```

Desde `backend/`, con variables de datasource apuntando a una **base nueva de prueba**:

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:15432/BASE_NUEVA_DE_PRUEBA'
# SPRING_DATASOURCE_USERNAME/PASSWORD provienen del contenedor de pruebas, sin imprimirlos.
$env:SPRING_FLYWAY_ENABLED = 'true'
$env:SPRING_JPA_HIBERNATE_DDL_AUTO = 'validate'
.\mvnw.cmd verify
```

No ejecutar esta suite contra una base con datos de usuario: sus fixtures crean
organizaciones, cuentas, vínculos y credenciales. Por defecto el perfil test usa
H2 con Flyway deshabilitado y create-drop; esa configuración por sí sola no valida
las migraciones PostgreSQL.

Desde la raíz, para el smoke aislado (Maven verify debe generar primero el JAR):

```powershell
docker compose --env-file .local-backups/audit-compose.env -p credentials-v3-audit -f compose.audit.yml up -d --build
# Esperar /actuator/health en http://127.0.0.1:18081.
# PowerShell 7; credenciales solo en variables del proceso:
$smoke = . .\scripts\smoke-organization.ps1 -ApiBase 'http://127.0.0.1:18081' -SuperEmail $auditEmail -SuperPassword $auditPassword
.\scripts\smoke-document-integrity.ps1 -ApiBase 'http://127.0.0.1:18081' -Token $admin.token -StudentId $studentProfile.id
docker compose --env-file .local-backups/audit-compose.env -p credentials-v3-audit -f compose.audit.yml stop
```

El primer script se invoca con dot sourcing para conservar el fixture de estudiante
y el token de administrador en ese proceso. No imprimir esas variables. El archivo
de entorno exige POSTGRES_USER/PASSWORD, MINIO_ROOT_USER/PASSWORD y JWT_SECRET;
el escenario de suspensión añade BOOTSTRAP_SUPERADMIN_EMAIL/PASSWORD. Usar valores
aleatorios locales y no confirmar ese archivo en Git.

## Entrega T14.1

- **Problema:** almacenamiento externo se modificaba antes del rechazo por estado.
- **Solución:** comprobar DRAFT antes de efectos externos, bloqueo por id/tenant
  para mutaciones y objeto nuevo por carga/generación.
- **Entidades/esquema:** sin cambios. Se añade consulta bloqueante al repositorio.
- **API/permisos:** sin rutas ni DTO nuevos. CREDENTIAL_CREATE/VIEW/ISSUE/REVOKE
  mantienen su contrato. Errores de estado/propiedad conservan HTTP 400 actual.
- **Pantallas:** sin cambios visuales. No se necesita QA visual de este incremento.
- **Archivos de implementación:** CredentialService.java, CredentialRepository.java,
  CredentialDocumentIntegrityIntegrationTests.java, compose.audit.yml y
  scripts/smoke-document-integrity.ps1. Contrato previo en CREDENTIAL_DOCUMENT_CONTRACT.md.
- **Riesgo:** bloqueo durante I/O; objetos nuevos pueden quedar huérfanos ante un
  rollback. No hay firma digital ni transacción distribuida ni blockchain real.
- **Siguiente trabajo:** onboarding con revisión previa y seguridad estudiantil,
  según SPRINT_01_V3.md. Las 40 tareas no están terminadas.

## Continuación Seguridad 02 — 2026-11-14

Resultados actuales: 51 pruebas backend PostgreSQL 16.15 y 43 web correctas,
builds correctos, Flyway V1–V15 y upgrade V14→V15 comprobados. Smokes HTTP de
MFA/sesiones/límites, organizaciones/roles/credenciales, integridad documental y
seguridad estudiantil Mailpit correctos. QA 1280×900 y 390×844 sin overflow.
El entorno principal sirve la versión nueva en localhost:4300 y API health UP;
se conservan datos/volúmenes y hay backup previo. Detalles, comandos y límites
en SECURITY_02_API.md, SECURITY_02_DELIVERY.md y handoffs/codex-security-02.md.
## SEC-03 y correo durable — 2026-11-14

Estado actual: 66 pruebas backend PG16 + 49 Angular = 115, cero fallos/errores/
omitidas en la ejecución PostgreSQL. Builds correctos, V1–V16 en base nueva y
upgrade V15→V16 verificados. Trigger de auditoría rechaza UPDATE/DELETE. Outbox
probado con fallo SMTP, RETRY persistido, reinicio del backend y SENT en intento 2;
correo real en Mailpit y enlace de un uso. Scripts públicos smoke-audit-mail.ps1
y smoke-mail-recovery.ps1, más regresiones anteriores, correctos.

QA de escritorio/móvil de auditoría/monitor, filtros y acceso por ámbito. Principal
actualizado con backup previo y cantidades de registros conservadas. Contrato,
resultados, límites y handoff: AUDIT_MAIL_CONTRACT.md, AUDIT_MAIL_DELIVERY.md y
handoffs/codex-audit-mail.md. Las entregas anteriores conservan contexto histórico.

## Seguridad y perfil legado — 2026-11-14

- Maven verify completo: **82 pruebas**, cero fallos/errores/omitidas, PostgreSQL
  16.15 en base nueva `polish_final_20261114222509`, Flyway V1–V17 y Hibernate validate.
- Angular ChromeHeadless: **56 pruebas**, cero fallos; build de producción correcto.
- Total distinto: **138**, frente a 115 anteriores. Las nuevas cubren rotación
  compatible, rechazo de claves/firmas inválidas, recifrado con rollback, retención
  sin borrar registros vigentes, MFA/replay/reemplazo y perfil institucional legado.
- Upgrade real V16→V17: el JWT emitido antes del upgrade sigue autenticando;
  mantenimiento SUPER_ADMIN funciona y el administrador tenant recibe 403.
- `smoke-security-polish.ps1`: perfil faltante 404 JSON, completar sin duplicar,
  acceso operativo pendiente bloqueado, MFA/respaldos/reemplazo/cancelación correctos.
- Regresiones HTTP correctas: `smoke-organization.ps1`,
  `smoke-document-integrity.ps1`, `smoke-student-security.ps1`,
  `smoke-sessions-mfa.ps1`; PostgreSQL/MinIO/Mailpit reales, datos ficticios aislados.
- QA visual 1280×900 y 390×844: perfil legado, recuperación MFA y mantenimiento;
  formularios exclusivos, campos sin datos inventados, sin claves privadas expuestas
  ni desbordamiento horizontal. Evidencias en `docs/evidence/`.
- Docker principal: web 4300 con bundle nuevo, API 8080 UP, Flyway 17 y cantidades
  de tenants/estudiantes/credenciales/usuarios conservadas. Backup verificado con
  `pg_restore -l`; esto no sustituye un ensayo integral de restauración.

Contrato, límites, configuración y comandos: SECURITY_POLISH_CONTRACT.md,
KEY_ROTATION_RUNBOOK.md y SECURITY_POLISH_DELIVERY.md. V1–V16 intactas.

## Revisión documental institucional — 2026-11-15

- Maven verify: 96 pruebas, cero fallos/errores/omitidas; PostgreSQL 16.15 nuevo,
  identity_final_20261115141442, Flyway V1–V18 y Hibernate validate. JAR correcto.
- Angular: 66 pruebas ChromeHeadless y build production correctos. Total distinto 162.
- Se agregan 12 casos de revisión/autorización/concurrencia, dos de rate limit
  multipart y diez de frontend (consentimiento, envío/errores, retirada, decisiones,
  blobs privados/liberación). Se conservan las 138 pruebas anteriores.
- smoke-identity-review.ps1 correcto con PostgreSQL/MinIO/Mailpit reales: carga
  3.6 MB por Nginx, imágenes normalizadas, permisos, acceso cruzado, SUPER_ADMIN
  denegado, rechazo, replay, aprobación, retirada y reenvío. Incluye regresión
  completa del script de organizaciones/roles/vínculo/emisión/verificación/revocación.
- Upgrade aislado/principal V17→V18; web 4300 con bundle nuevo y API 18080 UP;
  cantidades de registros académicos conservadas y respaldo previo comprobado.
- QA visual **no ejecutada**: las dos herramientas de interfaz no arrancan por
  fallo `failed to write kernel assets`/ruta no encontrada. No hay capturas nuevas.
  No se presenta la prueba de componentes como QA visual ni E2E de navegador.

Detalles/límites: IDENTITY_REVIEW_CONTRACT.md, IDENTITY_REVIEW_DELIVERY.md y
handoffs/codex-identity-review.md. Migraciones V1–V17 intactas.

## Navegación horizontal — 2026-11-15

Barra superior con nombre institucional acotado a la izquierda y módulos a la
derecha. Organización/Administración y Cuenta agrupan opciones; en anchos menores
de 1100 px hay un menú desplegable sin scroll horizontal. Componente y modelo
de navegación separados de App. Se mantienen roles, permisos, rutas y logout.

Angular: 69 pruebas correctas (tres nuevas de Escape/foco, clic exterior/cambio
de ruta y retirada de permisos con menú abierto); build de producción correcto.
Backend/API/BD sin cambios; su última suite conserva 96 aprobadas, V18. Total 165.
Frontend principal actualizado y bundle comprobado por HTTP. Herramienta visual
vuelve a fallar al arrancar por kernel assets/ruta ausente; QA visual manual pendiente.
