# Handoff — seguridad y perfil legado

Fecha: 2026-11-14. Rama: codex/rbac-superadmin. Base: 165ac01.

## Resumen

V17 completa reemplazo seguro del autenticador, renovación de respaldos y cancelación
de preparaciones MFA; retención técnica limitada; soporte de rotación JWT/AES y
recifrado transaccional; panel SUPER_ADMIN de mantenimiento. Corrige el 403 erróneo
del perfil institucional inexistente y permite completar datos sin recrear cuentas.
RBAC, tenants e identidad estudiantil se conservan. No se rotaron claves reales.

## Migraciones y endpoints

Solo V17__security_maintenance.sql; V1–V16 intactas. Columnas pending_secret,
used_at, terminal_at y signing_key_id, con índices. Backfill conservador al desplegar.

- POST /api/security/mfa/replacement, /api/security/mfa/recovery-codes.
- DELETE /api/security/mfa/pending; estado/confirm existentes ampliados.
- GET /api/platform/security/maintenance y POST /re-encrypt, /cleanup, solo SUPER_ADMIN.
- GET/PUT /api/institution/details conservan DTO/permisos; error 404 y primer PUT corregidos.

UI: controles en Seguridad de la cuenta, /superadmin/security y perfil institucional.
Contratos: docs/SECURITY_POLISH_CONTRACT.md y docs/KEY_ROTATION_RUNBOOK.md.

## Validación ejecutada

- Maven verify completo contra PostgreSQL 16.15 nuevo: 82 pruebas, cero fallos/
  errores/omitidas; Flyway V1–V17 y Hibernate validate. Build Java correcto.
- Angular ChromeHeadless: 56 correctas y build de producción. Total distinto: 138.
- smoke-security-polish.ps1 correcto dos veces, además de organización, integridad
  documental MinIO, seguridad estudiantil Mailpit y sesiones/MFA/límites.
- Upgrade real V16→V17 y JWT firmado antes de actualizar todavía válido.
- Mantenimiento HTTP real y rechazo 403 para administrador tenant.
- QA visual escritorio/móvil de tres pantallas, 1280×900 y 390×844; seis PNG ficticias.
- Principal localhost:4300/API 8080 UP, bundle actualizado, V17 y cantidades de
  registros de negocio conservadas. Backup privado previo comprobado con pg_restore -l.
- Diff sin secretos privados ni migraciones históricas cambiadas. Logs/builds/env
  y fixtures privados quedan ignorados. Otro checkout intacto.

## Riesgos y continuidad

La rotación exige conservar claves anteriores, ejecutar lotes y esperar la salida
de sesiones JWT antiguas antes de retirarlas. Correos PROCESSING esperan su lease.
La limpieza no borra auditoría ni registros académicos; no constituye retención
legal de bitácoras. No hay recuperación con contraseña sola si se pierden todos
los factores. Custodia/HTTPS/proxy productivo y restauración integral siguen pendientes.
SMTP conserva semántica al menos una vez. T07/T26 generales siguen parciales.
Siguiente módulo funcional: revisión documental de identidad; consultar backlog.

## Commits

- Backend/configuración/contrato: 8bfe29572d8ca954f845e49c5dd87d6a35baf73f.
- Último commit de implementación: 54663ab90eab33c2e97ec2a8db2f32efce727174.
- El commit final de documentación/evidencias que contiene este handoff se obtiene
  con `git log -1 --format=%H -- handoffs/codex-security-polish.md`.
  El SHA final publicado también figura en la descripción del PR #1.
- Rama publicada, sin modificar main. La entrega no es producción ni producto completo.

## Archivos modificados

Inventario completo del incremento, incluidos contratos, pruebas y evidencia:

- .env.example
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditAction.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditTrail.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailCipher.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailOutbox.java
- backend/src/main/java/bo/edu/uagrm/credenciales/mail/MailOutboxStore.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AeadKeyRing.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AuthRateLimitFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/KeySet.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/LoginSession.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaCrypto.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaRecoveryCode.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityMaintenance.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityMaintenanceController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityMaintenanceJob.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SessionRegistry.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/UserMfa.java
- backend/src/main/java/bo/edu/uagrm/credenciales/shared/ApiExceptionHandler.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationOnboardingService.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/db/migration/V17__security_maintenance.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/KeyRotationIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/KeyRotationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/LegacyOrganizationIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/SecurityPolishIntegrationTests.java
- backend/src/test/resources/application.yml
- compose.audit.yml
- docker-compose.yml
- docs/AUDIT_MAIL_DELIVERY.md
- docs/evidence/legacy-profile-desktop.png
- docs/evidence/legacy-profile-mobile.png
- docs/evidence/mfa-recovery-desktop.png
- docs/evidence/mfa-recovery-mobile.png
- docs/evidence/security-maintenance-desktop.png
- docs/evidence/security-maintenance-mobile.png
- docs/KEY_ROTATION_RUNBOOK.md
- docs/PRODUCT_BACKLOG_V3.md
- docs/SECURITY_POLISH_CONTRACT.md
- docs/SECURITY_POLISH_DELIVERY.md
- docs/SPRINT_01_V3.md
- docs/VALIDATION_V3.md
- frontend/src/app/app.routes.ts
- frontend/src/app/app.ts
- frontend/src/app/core/account-security.api.ts
- frontend/src/app/features/account-security/account-security.page.ts
- frontend/src/app/features/account-security/maintenance.page.ts
- frontend/src/app/features/account-security/security-polish.spec.ts
- frontend/src/app/features/audit/audit.api.ts
- frontend/src/app/features/organization/organization-profile.page.ts
- handoffs/codex-security-polish.md
- handoffs/codex.md
- README.md
- scripts/smoke-security-polish.ps1
