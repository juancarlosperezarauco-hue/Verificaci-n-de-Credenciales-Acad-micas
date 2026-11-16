# Handoff — auditoría V3 e integridad de credenciales

Fecha: 2026-11-13. Rama: `codex/rbac-superadmin`.
Commit de implementación: `a9d9a2afa2db9e296748b48a8f75ca81823ded2c`.
Este handoff y el inventario se incorporan en el commit posterior de documentación;
el HEAD final se consulta en el historial de la rama/PR. No se modifica main.

## Resumen

Se auditó el estado existente frente al prompt maestro, se reprodujo el baseline de
35 pruebas y se validaron V1–V13 en PG16. La matriz clasifica las 40 tareas en
5 verificadas (auditoría/inventario y RBAC actual), 19 parciales y 16 pendientes.
No significa que cinco módulos completos del producto estén terminados.

Se corrigió una regresión de integridad: una carga rechazada podía modificar MinIO
antes de validar el estado de una credencial. Ahora se valida DRAFT antes de I/O,
se bloquea la fila al mutar y cada carga/generación usa un objeto nuevo. El reintento
de emisión rechazado no repite anclaje externo.

## Archivos

- backend/src/main/java/bo/edu/uagrm/credenciales/credential/CredentialService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/credential/CredentialRepository.java
- backend/src/test/java/bo/edu/uagrm/credenciales/CredentialDocumentIntegrityIntegrationTests.java
- compose.audit.yml
- scripts/smoke-document-integrity.ps1
- docs/CREDENTIAL_DOCUMENT_CONTRACT.md
- docs/AUDIT_V3.md
- docs/PRODUCT_BACKLOG_V3.md
- docs/SPRINT_01_V3.md
- docs/VALIDATION_V3.md
- README.md, AI_WORKPLAN.md, handoffs/codex.md y este handoff.

## Migraciones, endpoints y permisos

Sin migraciones nuevas ni cambios históricos. Sin cambios de entidades o DTO.
Se conservan `/api/credentials/{id}/document`, `/generate-document`, `/issue` y
`/revoke`, con los permisos existentes. La consulta bloqueante incluye tenant + id.
El contrato se documentó antes del arreglo en CREDENTIAL_DOCUMENT_CONTRACT.md.

## Pruebas

- Baseline: 11 backend PostgreSQL + 24 frontend, ambos builds correctos.
- Prueba roja de reemplazo posterior a emisión antes del arreglo: fallo reproducido.
- Suite final PG16: 16 casos, 0 fallos/errores/omitidos; Flyway V1–V13 y validate.
- Suite H2 por defecto: los mismos 16 casos aprobados; no duplica el total.
- Frontend: 24 casos aprobados y build producción. **Total distinto: 40**.
- Dos smokes HTTP PASS en API/PostgreSQL/MinIO reales separados: recorrido completo
  y conservación de bytes/hash tras rechazo en ISSUED y REVOKED.
- Parser PowerShell y git diff --check correctos.

## Riesgos y continuidad

- T05/T14/T38 siguen parciales: esto cierra historias concretas de integridad.
- Pendientes prioritarios: aprobación inicial de organizaciones, seguridad de cuentas,
  validación real de archivos, verificación del emisor y auditoría transversal.
- V9 no equivale a seguridad estudiantil implementada; revisar los cambios ajenos
  pendientes sin sobrescribir su checkout ni crear tablas duplicadas.
- El bloqueo cubre I/O externo: revisar timeouts/carga. Un rollback puede dejar un
  objeto nuevo huérfano; outbox/reconciliación siguen pendientes.
- Blockchain es adaptador de desarrollo, no hay firma criptográfica, OCR/anomalías
  ni Flutter. No se anuncian como reales.
- No se borró ni alteró la base/MinIO del usuario. Los contenedores auxiliares
  quedaron detenidos, con sus datos separados para revisión.
- El Docker principal conserva su versión anterior; no se desplegó este incremento
  allí ni en producción. Siguiente sprint y aceptación: docs/SPRINT_01_V3.md.
