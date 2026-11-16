# Handoff — revisión documental institucional

Fecha 2026-11-15. Base c3daf33. Rama codex/rbac-superadmin; no publicar en main.

## Resumen

Módulo identityreview: envíos privados por institución/consentimiento del estudiante
global, snapshot/versionado de perfil, documentos normalizados, revisión con
IDENTITY_REVIEW, motivo de rechazo, historial, retirada y reenvío. Interfaces web
claras para estudiante y encargado; lecturas y decisiones auditadas sin PII.
No duplica cuentas ni impone un requisito nuevo a la emisión ya existente.

## Migración y API

V18__identity_review.sql: identity_revision y tabla identity_review_submissions
con consentimiento exacto, estados, instantánea/huella, archivos/huellas, revisor,
retiro e índices. V1–V17 sin cambios. Columnas globales reservadas V9 conservadas.

- GET/POST /api/me/identity-verifications; POST /{id}/withdraw; GET /{id}/files/{part}.
- GET /api/institution/identity-verifications (estado/paginación), GET /{id},
  POST /{id}/decision y GET /{id}/files/{part}, solo IDENTITY_REVIEW del tenant.
- Sin consulta documental pública ni implícita para SUPER_ADMIN.
- UI: /student/identity y /organization/identity. Nginx/Spring admiten 16 MiB
  por envío, imágenes hasta 5 MiB cada una. Sin claves/dependencias nuevas.

## Pruebas ejecutadas

- Maven verify completo PostgreSQL 16.15 nuevo, Flyway V1–V18/Hibernate validate:
  96 pruebas, cero fallos/errores/omitidas; JAR correcto.
- Angular ChromeHeadless: 66 correctas y build production. Total distinto 162.
- 12 nuevas de revisión/aislamiento/permisos/replay/concurrencia/fallo MinIO;
  dos de rate limit multipart y diez de componentes/formularios/blobs.
- smoke-identity-review.ps1 correcto con PG/MinIO/Mailpit reales. Carga 3.6 MB
  por Nginx, acceso privado, rechazo/aprobación/retirada y reenvío. Incluye el
  smoke previo de organizaciones/roles/vínculo/PDF/emisión/verificación/revocación.
- V17→V18 real aislado/principal; API 18080 UP, web 4300 con bundle nuevo,
  cantidades académicas conservadas. Backup privado previo comprobado.
- QA visual no ejecutada: ambas herramientas de interfaz fallaron al iniciar
  (`failed to write kernel assets`, ruta no encontrada). No hay capturas nuevas.
  Esta limitación no se oculta tras los tests de componentes.

## Operación y riesgos

BACKEND_PORT local cambió a 18080 porque HRcore ya ocupa 8080; se preservan
otros proyectos. Las claves/volúmenes/datos de negocio y otro checkout se conservan.
Backup .local-backups/credentials-before-identity-20261115-141659.dump, con
pg_restore -l; no sustituye restauración integral. Fixtures y logs ignorados.
Al terminar se detienen solo el stack credentials-v3-audit y codex-rbac-postgres-check;
principal permanece iniciado y los contenedores de HRcore no se tocan.

La decisión es institucional/humana, no validación estatal/biométrica. T08 sigue
parcial por disputa de identidades, retención/borrado de imágenes y avatar global.
MinIO privado no implica KMS en reposo. Un rollback tras subir puede dejar objetos
huérfanos; no sobrescribe anteriores. Bloqueos durante I/O requieren seguimiento
de rendimiento. Historial propio sin paginación; inbox institucional paginada.
Retirada no elimina copias previamente consultadas. Falta QA visual manual.
Siguiente prioridad funcional: expediente institucional y solicitudes/aprobaciones.

## Commits

- Backend, contrato y smoke: 99ef3d1b888756e072ba150773fdec8f893887b6.
- Última implementación web: bffed74a0712e2037a8bc7e2ac66cc6b0ffc26cb.
- SHA final de documentación: `git log -1 --format=%H -- handoffs/codex-identity-review.md`.
  El SHA publicado se registra también en el PR #1.

## Archivos modificados

- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditAction.java
- backend/src/main/java/bo/edu/uagrm/credenciales/audit/AuditTrail.java
- backend/src/main/java/bo/edu/uagrm/credenciales/consent/ConsentRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccount.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentityImages.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentityReviewController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentityReviewDtos.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentityReviewService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentitySubmission.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identityreview/IdentitySubmissionRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AuthRateLimitFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/shared/ApiExceptionHandler.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/db/migration/V18__identity_review.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/IdentityReviewIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/IdentityUploadRateLimitTests.java
- docs/IDENTITY_REVIEW_CONTRACT.md
- docs/IDENTITY_REVIEW_DELIVERY.md
- docs/PRODUCT_BACKLOG_V3.md
- docs/SPRINT_01_V3.md
- docs/VALIDATION_V3.md
- frontend/nginx.conf
- frontend/src/app/app.routes.ts
- frontend/src/app/app.ts
- frontend/src/app/features/audit/audit.api.ts
- frontend/src/app/features/identity-review/identity-preview.component.ts
- frontend/src/app/features/identity-review/identity-review.api.ts
- frontend/src/app/features/identity-review/identity-review.spec.ts
- frontend/src/app/features/identity-review/institution-identity.page.ts
- frontend/src/app/features/identity-review/student-identity.page.ts
- frontend/src/styles.css
- handoffs/codex-identity-review.md
- handoffs/codex.md
- README.md
- scripts/smoke-identity-review.ps1
