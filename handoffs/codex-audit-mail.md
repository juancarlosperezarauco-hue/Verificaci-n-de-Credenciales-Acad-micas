# Handoff Codex — SEC-03 y correo durable

Fecha: 2026-11-14. Rama codex/rbac-superadmin. Base c79b963.

## Resumen

Se completan los puntos 1 y 2 de la continuación: auditoría de acciones críticas
y correo de seguridad persistente con reintentos. Se conserva la arquitectura
Angular/Spring/PostgreSQL/MinIO y la identidad global estudiantil.

La bitácora se inserta con cada transacción, con actor real del contexto o SYSTEM
para solicitudes públicas. El acceso es por AUDIT_VIEW dentro del tenant o por
SUPER_ADMIN global. Se registran diferencias permitidas de permisos/roles/estado,
sin cuerpos, nombres, correos, documentos, contraseñas ni tokens.

La cola cifrada se guarda con el token y se procesa con reclamaciones atómicas,
lease, backoff y presupuesto de intentos. Se cancela si el token/cuenta no siguen
vigentes. El monitor global solo muestra metadatos seguros. Se retiró el executor
con DiscardPolicy; las pantallas tienen filtros, paginación, errores y estado vacío.

## Migración y endpoints

- V16__audit_and_mail_outbox.sql: audit_events con protección UPDATE/DELETE,
  mail_outbox y FK token/estudiante. V1–V15 intactas.
- GET /api/audit/tenant: tenant del principal y AUDIT_VIEW; ignora selector ajeno.
- GET /api/platform/audit: SUPER_ADMIN, filtros globales/por tenant.
- GET /api/platform/mail: SUPER_ADMIN, filtro state y paginación.
- Sin endpoints de mutación de bitácora/cola ni envío arbitrario.
- UI: /organization/audit, /superadmin/audit, /superadmin/mail.
- MAIL_ENCRYPTION_KEY obligatoria: clave independiente de 32 bytes en base64,
  privada y conservada con backups; no reemplaza ni rota MFA/JWT.

## Validación

- Maven verify: 66/66 en PostgreSQL 16.15 nuevo, Flyway V1–V16, Hibernate validate.
- Build Angular y 49/49 Jasmine/Karma/ChromeHeadless. Total distinto: 115.
- Rollback, aislamiento, permisos dinámicos, datos sensibles ausentes, límites
  de filtros, actor/correlación, emisión/revocación y rechazo SQL de UPDATE/DELETE.
- Correo cifrado, fallo SMTP/reintentos/backoff/máximo de intentos, payload retirado,
  tokens/cuentas inválidos, concurrencia, lease recuperado y worker atrasado rechazado.
- Upgrade real V15→V16 en Compose aislado. Mailpit detenido produce RETRY;
  tras reiniciar backend y recuperar Mailpit la fila pasa SENT en intento 2.
  El correo llega a Mailpit y su enlace funciona una vez. Script público ejecutado:
  scripts/smoke-mail-recovery.ps1; smoke base: scripts/smoke-audit-mail.ps1.
- Regresiones HTTP reales de organizaciones, equipo, PDF/MinIO, credenciales,
  seguridad estudiantil/Mailpit, sesiones, MFA y rate limiting correctas.
- QA 1280×900/390×844, sin overflow: filtros/cambios, monitor/estado vacío,
  auditoría propia y rechazo de Correos global desde una institución.
- Principal localhost:4300, API UP, esquema V16 y bundle nuevo comprobados.
  Cantidades de organizaciones/estudiantes/documentos/usuarios conservadas.
- Backup privado previo, archivo comprobado con pg_restore -l; no se declara
  restauración completa. Configuración privada y fixtures no se publican.

## Riesgos y siguientes pasos

- SMTP confirma aceptación, no entrega final. Puede duplicarse un correo si el
  proceso falla después de SMTP y antes de confirmar la fila; token de un uso.
- Tras agotar intentos se solicita enlace nuevo; no se reenvían secretos expirados.
- Falta retención/custodia externa de auditoría y metadatos de cola, alertas
  operativas y rotación con recifrado de claves. No hay historial retroactivo.
- No se auditan aún todas las lecturas, logins fallidos o infraestructura. El
  trigger no protege frente al administrador capaz de desactivarlo o alterar BD.
- Bandeja de notificaciones/avisos académicos, firma criptográfica, validación
  documental de identidad, blockchain real, IA/OCR y Android siguen pendientes.
- El siguiente bloque es pulir seguridad/recuperación/retención y el registro
  institucional antiguo, seguido de la revisión documental estudiantil.

## Commits

- Backend/migración/contrato/smokes: d9cfc513f063ea0e3781fd317627ae352ab62efe.
- Commit final de implementación: 53348ac5035cb267b386664825ea956e2bc81838.
- El hash del commit de esta documentación se obtiene con
  `git log -1 --format=%H -- handoffs/codex-audit-mail.md`.
- Publicación en codex/rbac-superadmin y PR #1. main sin modificar.

## Archivos modificados en este incremento

- .env.example
- README.md
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditAction.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditEvent.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditQuery.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditTrail.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/RequestCorrelationFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/credential/CredentialService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccountToken.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentMailConfiguration.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentMailDelivery.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentSecurityService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailCipher.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailMonitoringController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailOutbox.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailOutboxStore.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailOutboxWorker.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailState.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessManagementService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityConfig.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SessionRegistry.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationOnboardingService.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/db/migration/V16__audit_and_mail_outbox.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/AuditIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/MailOutboxIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/StudentSecurityIntegrationTests.java
- backend/src/test/resources/application.yml
- compose.audit.yml
- docker-compose.yml
- docs/AUDIT_MAIL_CONTRACT.md
- docs/AUDIT_MAIL_DELIVERY.md
- docs/PRODUCT_BACKLOG_V3.md
- docs/SPRINT_01_V3.md
- docs/VALIDATION_V3.md
- docs/evidence/audit-global-desktop.png
- docs/evidence/audit-global-mobile.png
- docs/evidence/audit-tenant-desktop.png
- docs/evidence/mail-monitor-desktop.png
- docs/evidence/mail-monitor-mobile.png
- frontend/src/app/app.routes.ts
- frontend/src/app/app.ts
- frontend/src/app/features/audit/audit.api.ts
- frontend/src/app/features/audit/audit.page.ts
- frontend/src/app/features/audit/audit.spec.ts
- frontend/src/app/features/audit/mail.page.ts
- frontend/src/app/features/organization/organization-team.page.ts
- handoffs/codex-audit-mail.md
- handoffs/codex.md
- scripts/smoke-audit-mail.ps1
- scripts/smoke-mail-recovery.ps1
