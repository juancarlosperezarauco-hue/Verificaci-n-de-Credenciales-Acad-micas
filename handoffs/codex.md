# Entrega Codex: RBAC, superadmin y onboarding

## Entrega prioritaria más reciente

SEC-01/SEC-02 y V14 están documentadas en
[codex-priority-01.md](codex-priority-01.md): 30 backend + 36 frontend aprobadas,
smokes y QA visual. El Docker principal ya ejecuta esta versión. Las secciones
anteriores de V3 conservan su contexto histórico y no sustituyen esta actualización.

## Continuación V3

La auditoría y el incremento posterior de integridad están en
[codex-v3.md](codex-v3.md). Se reprodujo el baseline descrito abajo; ahora hay
16 pruebas backend + 24 frontend aprobadas. Se mantienen V1–V13. El nuevo código
se probó en Docker aislado; el stack principal sigue con la versión anterior.
Inventario actualizado: docs/AUDIT_V3.md y docs/PRODUCT_BACKLOG_V3.md.

## Resumen
Se implementó superadministración global sin tenant, TENANT_ADMIN, empleados CUSTOM
con roles por organización, 19 permisos, autenticación con consulta de estado/permisos
en cada petición, historial de cambios de estado y registro institucional completo.
Codex retomó el backend asignado a DeepSeek, sin encontrar una entrega publicada suya.
Codex también completó el frontend al detenerse Antigravity. Ver docs/UI_DELIVERY.md.

## Migraciones
- V11__rbac_and_platform_governance.sql: permisos, roles, asignaciones con claves
  compuestas que impiden cruces de tenant, alcance de usuarios y gobernanza.
- V12__organization_details.sql: datos legales, representante y niveles educativos.
- V13__optional_organization_legal_identifiers.sql: NIT/licencia opcionales,
  normalización de vacíos a NULL y conservación de unicidad cuando se informan.
No se modificó ninguna migración anterior.

## Endpoints
El detalle de cuerpos y respuestas está en docs/RBAC_CONTRACT.md y docs/ORGANIZATION_API.md.
- POST /api/auth/platform/login
- GET /api/access/me, /api/access/permissions
- GET/POST /api/access/roles; PUT/DELETE /api/access/roles/{id}
- GET/POST /api/access/users; PUT /api/access/users/{id}
- PATCH /api/access/users/{id}/disable
- GET /api/platform/organizations, /api/platform/organizations/{id}
- PATCH /api/platform/organizations/{id}/status
- GET /api/platform/organizations/{id}/history
- POST /api/auth/organizations/register
- GET/PUT /api/institution/details
- GET /api/platform/organizations/{id}/details

## Validación ejecutada
- Maven verify contra PostgreSQL 16 dedicado con Flyway habilitado y Hibernate
  ddl-auto=validate: 11 pruebas, 0 fallos, 0 errores. La entrega integrada valida V1–V13.
- Las pruebas cubren permisos retirados con JWT vigente, usuarios desactivados,
  IDs ajenos, estudiantes independientes, suspensión/restauración, documentos legales,
  validación de URL y duplicados de identificación fiscal/licencia por país.
- npm run build: compilación de producción correcta.
- npm test -- --watch=false --browsers=ChromeHeadless: 24 pruebas correctas.
- scripts/smoke-organization.ps1: PASS contra PostgreSQL/MinIO/API reales,
  incluyendo emisión, descarga, verificación, revocación y suspensión/restauración.
- git diff --check: correcto.

## Riesgos y límites
- Las nuevas pantallas/login superadmin están implementadas por Codex y revisadas
  visualmente en escritorio y móvil. Evidencias y recorrido: docs/UI_DELIVERY.md.
- ADMIN/ISSUER/AUDITOR históricos conservan compatibilidad; no se duplican estudiantes.
- Los registros nuevos mantienen ACTIVE; aprobación obligatoria previa no fue solicitada
  como condición de registro. Los estados pueden administrarse por superadmin.
- El catálogo IDENTITY_REVIEW/REPORT_VIEW/AUDIT_VIEW no desarrolla esos módulos.
- El bootstrap no tiene credenciales por defecto: usar variables locales documentadas
  y retirarlas tras crear el primer superadmin. Cambios de roles son inmediatos.
- V9/V10 se incorporaron con el contenido previo, sin alterar su checksum, para
  validar el historial ya aplicado en la base local y permitir instalar desde cero.
  Esto no publica el código local pendiente del módulo de seguridad del estudiante.
- main permanece sin modificar. Docker local ahora ejecuta la versión integrada de esta rama.
- No hay avances remotos de DeepSeek/Antigravity que revisar al momento de esta entrega.
- Datos y contenedores de pruebas se mantuvieron separados de los datos del usuario.

## Commit de implementación base
fec11e8f6d7c07540af79f882f60a62b816187dd
Este handoff se publica en un commit posterior de documentación; el HEAD de la rama
incluye ambos y se identifica en la entrega del chat.

Actualización posterior del registro: selectores de documento, cargo, país y
departamento de Bolivia; nueva distribución del representante y tres pruebas
adicionales. Archivos: organization-options.ts, organization.form.ts,
organization-fields.component.ts y organization.spec.ts dentro de
frontend/src/app/features/organization/. Sin cambios de API ni migraciones.
El commit de esta actualización se identifica en el historial de la rama y el PR.

Registro general: se quitaron los niveles educativos del asistente; NIT/licencia
son opcionales. V13 adapta esas columnas y el backend normaliza vacíos a NULL.
Los ocho tipos institucionales se probaron registrando y editando sin esos datos.

## Cierre de revisión
Se corrigieron el selector de tipo institucional, la selección de niveles en móvil,
el desbordamiento de identificadores largos, el error de acceso inválido, la edición
de imagen institucional por permiso y la recuperación de carga fallida del equipo.
Se agregaron cinco pruebas de regresión y seis capturas con datos ficticios.
No se añadieron migraciones ni endpoints durante este cierre.

Archivos del cierre:
- frontend/src/app/features/institution/institution.page.ts
- frontend/src/app/features/organization/access-login.page.ts
- frontend/src/app/features/organization/organization-fields.component.ts
- frontend/src/app/features/organization/organization-team.page.ts
- frontend/src/app/features/organization/organization.form.ts
- frontend/src/app/features/organization/organization.spec.ts
- frontend/src/styles.css
- docs/UI_DELIVERY.md y docs/evidence/
- AI_WORKPLAN.md y handoffs/antigravity.md

## Archivos de implementación
- .env.example
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/credential/CredentialController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/GovernanceRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/OrganizationGovernance.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/OrganizationStatus.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/OrganizationStatusEvent.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/StatusEventRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/SuperAdminBootstrap.java
- backend/src/main/java/bo/edu/uagrm/credenciales/portal/PortalController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessDtos.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessManagementService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/Permission.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/PermissionCode.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/PermissionRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/Role.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/RolePermission.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/RolePermissionRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/RoleRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/UserRole.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/UserRoleRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtAuthenticationFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityConfig.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/UserPrincipal.java
- backend/src/main/java/bo/edu/uagrm/credenciales/student/StudentController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/EducationLevel.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/InstitutionController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/InstitutionType.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationDtos.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationOnboardingController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationOnboardingService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationProfile.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/OrganizationProfileRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/Tenant.java
- backend/src/main/java/bo/edu/uagrm/credenciales/user/AppUser.java
- backend/src/main/java/bo/edu/uagrm/credenciales/user/AppUserRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/user/UserRole.java
- backend/src/main/resources/db/migration/V11__rbac_and_platform_governance.sql
- backend/src/main/resources/db/migration/V12__organization_details.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/RbacIntegrationTests.java
- docker-compose.yml
- frontend/src/app/features/dashboard/dashboard.page.ts
- frontend/src/app/features/institution/institution.page.ts

