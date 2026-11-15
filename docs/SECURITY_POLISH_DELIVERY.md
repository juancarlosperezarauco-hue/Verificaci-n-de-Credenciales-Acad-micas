# Seguridad y perfil institucional histórico — entrega V17

Fecha: 2026-11-14 (America/La_Paz). Base: 165ac01. Rama: codex/rbac-superadmin.
Se continúa Angular 20, Spring Boot 3.5, PostgreSQL 16 y MinIO sin reconstruir RBAC,
identidad global ni credenciales. Contrato previo: SECURITY_POLISH_CONTRACT.md.

## Comportamiento entregado

- Un administrador que ingresa con respaldo puede cambiar su autenticador o
  renovar sus diez respaldos con contraseña y un segundo factor vigente. Preparar
  otro autenticador conserva el actual hasta confirmar el nuevo código. Confirmar
  o renovar invalida sesiones, desafíos y respaldos anteriores; devuelve una sesión
  nueva y muestra los códigos una vez. Cancelar solo elimina la preparación.
- SUPER_ADMIN dispone de Mantenimiento: política de retención, identificadores
  públicos de claves, pendientes de recifrado y sesiones por firmante. Las acciones
  requieren confirmación y no aceptan secretos ni parámetros de borrado arbitrario.
- Soporte de claves actual/anterior para JWT y AES-GCM MFA/correo, con selección
  por ID permitido. JWT antiguos sin kid y ciphertext sin prefijo siguen usando
  legacy. IDs desconocidos y firmas inválidas se rechazan; no se consultan URLs.
- Recifrado transaccional de hasta 50 MFA y 50 correos por ejecución. Conserva
  contenido/AAD/propietario, excluye correos PROCESSING y revierte el lote al fallar.
- Retención predeterminada de 30 días, configurable entre 7 y 365. Limpieza horaria
  o manual: hasta 500 filas por categoría de sesiones terminales, desafíos vencidos,
  respaldos usados, correo terminal sin payload y tokens vencidos sin referencias.
  Elimina preparaciones MFA vencidas conservando el factor activo. No extiende los
  timestamps de respaldos ya usados ni sesiones ya revocadas al renovar credenciales.
  Conserva cuentas, instituciones, documentos, auditoría, sesiones vigentes,
  respaldos utilizables y correo pendiente/en proceso.
- Organización creada con la ruta histórica: el perfil inexistente devuelve 404
  JSON real en lugar de terminar como 403. La pantalla ofrece completarlo y precarga
  solo nombre/tipo conocidos, sin pisar campos editados ni inventar país o CI.
  PUT serializa por tenant y conserva la cuenta; completar no aprueba la institución.
  El correo del representante se identifica como contacto, separado del login.

## Migración y configuración

Solo se añade V17__security_maintenance.sql; V1–V16 no se modifican. Añade
pending_secret, used_at, terminal_at, signing_key_id e índices. Los timestamps
históricos desconocidos parten del despliegue; no se inventa una fecha pasada.

Variables nuevas, con valores compatibles por defecto:

- JWT_KEY_ID, MFA_KEY_ID, MAIL_KEY_ID: legacy.
- JWT_PREVIOUS_KEYS, MFA_PREVIOUS_KEYS, MAIL_PREVIOUS_KEYS: vacías; formato en runbook.
- SECURITY_RETENTION_DAYS: 30; SECURITY_MAINTENANCE_ENABLED: true (solo limpieza automática).

Compose principal y aislado transmiten estas variables. Las claves reales existentes
se conservan: **esta entrega no las ha rotado**. Procedimiento: KEY_ROTATION_RUNBOOK.md.

## API y UI

| Método/ruta | Acceso y resultado |
|---|---|
| POST /api/security/mfa/replacement | Administrador propio, password/code; prepara semilla nueva. |
| POST /api/security/mfa/recovery-codes | Administrador propio, password/code; nueva sesión y respaldos. |
| DELETE /api/security/mfa/pending | Administrador propio; cancela preparación, 204. |
| POST /api/security/mfa/confirm | Contrato existente; ahora confirma también reemplazo. |
| GET /api/security/mfa | Estado extendido con pendingSetup/pendingExpiresAt, sin semilla. |
| GET /api/platform/security/maintenance | Solo SUPER_ADMIN; política/metadatos públicos. |
| POST /api/platform/security/maintenance/re-encrypt | Solo SUPER_ADMIN; ejecuta lote auditado. |
| POST /api/platform/security/maintenance/cleanup | Solo SUPER_ADMIN; ejecuta política auditada. |
| GET/PUT /api/institution/details | Mismos DTO/permisos; 404 y primer guardado legado corregidos. |

UI nueva: /superadmin/security y menú Mantenimiento. Seguridad de la cuenta añade
cambio de autenticador, renovación y cancelación. Los eventos nuevos aparecen en
la bitácora sin material criptográfico ni documentos personales.

## Verificación ejecutada

**82 backend + 56 Angular = 138 pruebas distintas**, cero fallos. Maven verify
contra PostgreSQL 16.15 nuevo con Flyway/Hibernate validate; ChromeHeadless y build
production correctos. Nuevas clases: KeyRotationTests (4), KeyRotationIntegrationTests
(3), LegacyOrganizationIntegrationTests (2), SecurityPolishIntegrationTests (7),
security-polish.spec.ts (7). Baseline anterior 66+49 conservado.

Smoke público aislado, solo API 127.0.0.1:18081:

```powershell
.\scripts\smoke-security-polish.ps1
```

Probado dos veces. Regresiones organización/roles/emisión/revocación, integridad
PDF MinIO, seguridad estudiantil Mailpit y sesiones/MFA también correctas. Se
comprobaron upgrade V16→V17, JWT previo vigente y endpoints reales de mantenimiento.
QA escritorio/móvil 1280×900 y 390×844, con cuentas ficticias y sin modificar las
credenciales desde la UI. Se verificaron formularios exclusivos y anchos de página.

Evidencias: legacy-profile-{desktop,mobile}.png, mfa-recovery-{desktop,mobile}.png
y security-maintenance-{desktop,mobile}.png en docs/evidence/.

## Entorno actualizado

Docker principal operativo en http://localhost:4300; API health UP en 8080.
Bundle coincide con build local y Flyway está en 17. Cantidades académicas antes
y después coinciden; no se borraron registros/volúmenes ni se modificó el otro checkout.

Backup privado previo: `.local-backups/credentials-before-security-polish-20261114-224149.dump`.
Se comprobó con pg_restore -l; no equivale a una restauración integral ensayada.
Logs privados ignorados: backend/polish-postgres-final.log, frontend/polish-test.log,
frontend/polish-build.log, polish-regressions.log y polish-local-build.log.
Recursos Docker de QA se detienen al terminar; el principal permanece activo.

## Límites y siguiente prioridad

La recuperación sin autenticador ni respaldos necesita verificación humana de
identidad; no hay bypass con contraseña sola. No se ha desplegado gestor de secretos,
HTTPS/proxy confiable, rotación real de las claves del usuario ni custodia externa
de auditoría. Retención técnica no sustituye una política legal de auditoría.
SMTP sigue siendo al menos una vez, no garantía de recepción/lectura.

T07/T26 integrales siguen parciales. La siguiente prioridad funcional es la revisión
documental de identidad estudiantil; luego expediente, solicitudes/aprobaciones y
plantillas. Firma criptográfica, blockchain real, OCR/IA y Android siguen pendientes
según PRODUCT_BACKLOG_V3.md. No se declara el proyecto completo por este incremento.
