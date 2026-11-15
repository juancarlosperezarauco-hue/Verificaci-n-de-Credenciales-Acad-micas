# SEC-03 y correo durable — entrega

Fecha: 2026-11-14. Base c79b963. Rama codex/rbac-superadmin.

## Qué cambia

Auditoría de acciones institucionales, roles/permisos, usuarios, documentos,
emisión/revocación, seguridad estudiantil, MFA y cierre de sesiones. Cada evento
se guarda con la operación y desaparece si esta hace rollback. La consulta exige
AUDIT_VIEW dentro del tenant o SUPER_ADMIN en la vista global. Hay filtros por
acción, actor, recurso y fechas, con paginación. El actor se identifica por UUID,
tipo y rol; no se guardan nombres, correos, documentos ni secretos en la bitácora.
Las solicitudes públicas se identifican como SYSTEM, sin atribuir una identidad
autenticada que no se comprobó. Los cambios de permisos/roles y estados conservan
sus valores anteriores y nuevos mediante datos permitidos.

El correo de seguridad dejó de depender de un executor con DiscardPolicy y de
un listener AFTER_COMMIT sin persistencia. Ahora se encola cifrado dentro de la
transacción que crea el token. Un worker con lease y reclamación atómica permite
reintentos y recuperación después de reiniciar el backend. No se bloquea la
transacción del estudiante esperando SMTP. El monitor global muestra únicamente
metadatos seguros y categorías de error, con estados vacíos/errores y paginación.

## Migración, contratos y configuración

- V16__audit_and_mail_outbox.sql: audit_events con trigger que rechaza UPDATE y
  DELETE, mail_outbox y FK compuesta que vincula token/estudiante.
- V1–V15 intactas; se validaron instalación nueva y actualización V15→V16.
- GET /api/audit/tenant, GET /api/platform/audit y GET /api/platform/mail.
  Contrato previo completo en AUDIT_MAIL_CONTRACT.md. Sin API para editar/borrar
  auditoría, reenviar enlaces vencidos ni mandar correos arbitrarios.
- Frontend: /organization/audit, /superadmin/audit y /superadmin/mail, con guards
  y menú según permisos. Los trabajadores con AUDIT_VIEW solo ven su tenant.
- MAIL_ENCRYPTION_KEY obligatoria e independiente de MFA/JWT: 32 bytes aleatorios
  en base64, fuera de Git. AES-GCM liga cada payload al UUID de la fila.
- La clave debe conservarse con los respaldos seguros; no se regenera durante
  reinicios. Su rotación con recifrado sigue pendiente. Los tests usan otra clave
  sintética expresamente limitada al perfil de prueba.
- Valores iniciales: cinco intentos, backoff desde 30 s hasta 15 min, lease de
  120 s, lotes de cinco y consulta cada segundo. SMTP tiene timeouts de dos segundos.
  Los tests deshabilitan el scheduler y ejecutan el worker de forma determinista.

## Pruebas ejecutadas

- Maven verify: **66 pruebas correctas**, cero fallos/errores/omitidas, en
  PostgreSQL 16.15 nuevo, Flyway V1–V16 y Hibernate validate.
- Angular: build productivo y **49 pruebas correctas** Jasmine/Karma/ChromeHeadless.
- Total distinto: **115**. Son 15 nuevas backend y seis nuevas web sobre el
  incremento anterior de 94; las nueve regresiones de seguridad estudiantil
  ahora procesan la cola persistida para recoger sus mensajes de prueba.
- Auditoría: aislamiento incluso con tenantId ajeno en query, estudiante denegado,
  permisos dinámicos de empleado, actor/correlación, diferencias de permisos,
  rollback, mutación ajena rechazada sin evento, documento/emisión/revocación,
  ausencia de datos sensibles y paginación inválida.
- En PostgreSQL, UPDATE y DELETE de eventos fallan por el trigger. Este ensayo
  requiere PostgreSQL; el perfil H2 por defecto omite expresamente esa prueba.
- Correo: cola y token revierten juntos, contenido cifrado, error SMTP sin detalles
  privados, backoff, máximo de intentos, borrado del payload en estados terminales,
  cancelación por enlace/cuenta inválidos, consumo concurrente, recuperación de
  lease, worker obsoleto rechazado y cifrado resistente a manipulación/otro UUID.
- Prueba real aislada: Mailpit detenido → RETRY persistido → reinicio del backend
  → Mailpit recuperado → SENT en el segundo intento. El correo llegó a Mailpit y
  su enlace de verificación funcionó una vez; el segundo canje falló.
- Regresiones HTTP con servicios reales: organización/aprobación/roles, estudiante,
  vínculo, PDF/MinIO, emisión, expediente, verificación, revocación, suspensión,
  integridad documental, recuperación/cambio/desactivación, MFA y sesiones.
- QA navegador 1280×900 y 390×844: filtros de auditoría, diferencias de permisos,
  estados de correo y estado vacío, vista propia institucional y rechazo del
  acceso institucional a Correos global. Anchura de página no supera viewport.
  Evidencias ficticias en docs/evidence/audit-*.png y mail-monitor-*.png.

## Reproducción

La suite backend debe apuntar a una base nueva de pruebas con Flyway habilitado y
ddl-auto=validate. No ejecutarla sobre datos del usuario. Ver VALIDATION_V3.md.

Smokes PowerShell 7, exclusivos de 127.0.0.1:18081 y Compose credentials-v3-audit:

```powershell
# Variables del proceso cargadas desde el entorno privado; no imprimirlas.
.\scripts\smoke-audit-mail.ps1 -SuperEmail $auditEmail -SuperPassword $auditPassword
.\scripts\smoke-mail-recovery.ps1 -SuperEmail $auditEmail -SuperPassword $auditPassword
```

El segundo detiene/restaura solamente Mailpit de auditoría y reinicia su backend.
El escenario y el script reutilizable se ejecutaron durante esta entrega,
sin rutas de credenciales locales ni secretos incluidos. Si docker
no está en PATH, pasar -DockerCommand con la ruta de su ejecutable.

## Entorno local actualizado

El principal sirve esta versión en http://localhost:4300 y API health UP en
localhost:8080, con V16 y bundle main-PYCXMPUH.js. Las cantidades de tenants,
student_accounts, credentials y app_users coinciden antes/después de actualizar.
No se borraron registros ni volúmenes y el otro checkout permanece intacto.

Backup privado previo: .local-backups/credentials-before-audit-mail-20261114-213836.dump.
Se comprobó el archivo con pg_restore -l; no se confunde con un ensayo completo
de restauración. Las tres claves privadas y fixtures de QA permanecen ignorados.
Los recursos de auditoría y PostgreSQL de pruebas se detienen al finalizar,
conservando sus datos de prueba y dejando activo el proyecto principal.

## Límites

- SENT confirma aceptación por SMTP, no entrega final ni lectura. Un fallo entre
  aceptación SMTP y confirmación en la base puede duplicar el mensaje; el token
  continúa siendo de un uso. No existe garantía exactamente una vez en SMTP.
- Si un token deja de estar vigente, la cola cancela antes de enviar. Un correo
  ya en tránsito no puede retirarse; el endpoint de canje sigue rechazando el token.
- Tras agotar intentos se elimina el payload y se solicita un enlace nuevo. No
  hay botón que reenvíe un secreto perdido/vencido ni bypass de autenticación.
- La bitácora comienza con operaciones nuevas; no se inventan eventos históricos.
  No audita todavía todas las lecturas, intentos fallidos de acceso o infraestructura.
- Un administrador con capacidad de desactivar triggers, truncar o alterar BD
  puede evadir la protección. Falta custodia externa/retención operativa de auditoría.
- Faltan política de retención de metadatos de correo, alertas operativas de cola,
  reconciliación de archivos, backups integrales ensayados y preparación productiva.
- Las claves necesitan copia segura y procedimiento de rotación; sin MAIL key se
  pierden los correos pendientes cifrados. Los payloads terminales ya se eliminan.
- La bandeja de notificaciones y los avisos académicos siguen pendientes. T26 y
  T07 integrales permanecen parciales aunque este alcance de SEC-03 esté terminado.
