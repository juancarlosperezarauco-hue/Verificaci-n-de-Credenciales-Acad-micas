# Entrega Codex: RBAC, superadmin y onboarding

## Resumen
Se implementó superadministración global sin tenant, TENANT_ADMIN, empleados CUSTOM
con roles por organización, 19 permisos, autenticación con consulta de estado/permisos
en cada petición, historial de cambios de estado y registro institucional completo.
Codex retomó el backend asignado a DeepSeek, sin encontrar una entrega publicada suya.
El frontend de administración queda asignado a Antigravity en handoffs/antigravity.md.

## Migraciones
- V11__rbac_and_platform_governance.sql: permisos, roles, asignaciones con claves
  compuestas que impiden cruces de tenant, alcance de usuarios y gobernanza.
- V12__organization_details.sql: datos legales, representante y niveles educativos.
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
- Maven verify: 9 pruebas, 0 fallos, 0 errores (H2).
- Maven verify contra PostgreSQL 16 dedicado con Flyway habilitado y Hibernate
  ddl-auto=validate: 9 pruebas, 0 fallos, 0 errores. Historial Flyway confirma V1–V8,V11,V12.
- Las pruebas cubren permisos retirados con JWT vigente, usuarios desactivados,
  IDs ajenos, estudiantes independientes, suspensión/restauración, documentos legales,
  validación de URL y duplicados de identificación fiscal/licencia por país.
- npm run build: compilación de producción correcta.
- npm test -- --watch=false --browsers=ChromeHeadless: 8 pruebas correctas.
- git diff --check: correcto.

## Riesgos y límites
- Las nuevas pantallas/login superadmin corresponden a Antigravity y no están entregadas.
- ADMIN/ISSUER/AUDITOR históricos conservan compatibilidad; no se duplican estudiantes.
- Los registros nuevos mantienen ACTIVE; aprobación obligatoria previa no fue solicitada
  como condición de registro. Los estados pueden administrarse por superadmin.
- El catálogo IDENTITY_REVIEW/REPORT_VIEW/AUDIT_VIEW no desarrolla esos módulos.
- El bootstrap no tiene credenciales por defecto: usar variables locales documentadas
  y retirarlas tras crear el primer superadmin. Cambios de roles son inmediatos.
- V9/V10 de seguridad del estudiante existen únicamente como cambios locales previos.
  No se publican en esta entrega. Al integrarlos posteriormente en una base que ya
  tenga V11/V12, revisar expresamente el orden Flyway antes de desplegar.
- No se integró ni desplegó sobre main. La aplicación principal conserva su versión.
- No hay avances remotos de DeepSeek/Antigravity que revisar al momento de esta entrega.
- Datos y contenedores de pruebas se mantuvieron separados de los datos del usuario.

## Commit de implementación
5fec54d1c92e6c46357e79fa97f3b3f62b10c42f
Este handoff se publica en un commit posterior de documentación; el HEAD de la rama
incluye ambos y se identifica en la entrega del chat.

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

