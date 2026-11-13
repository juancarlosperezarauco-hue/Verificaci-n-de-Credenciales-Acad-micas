# Handoff — prioridad 1 (SEC-01 / SEC-02)

Fecha: 2026-11-13. Rama codex/rbac-superadmin.
Base: eb894bf. Commits funcionales: 26f7240 (onboarding), 2e504fd (seguridad)
y fabe793c0157b32d5fd510a59de12d142635f274 (web e integración).
El commit posterior de documentación contiene este handoff y correcciones de
espacios finales; el HEAD completo se identifica en el historial del PR.
No se modifica main ni el checkout ajeno con trabajo pendiente.

## Resumen

Nuevas organizaciones pendientes de aprobación, con acceso limitado de su administrador
para completar/corregir perfil y respaldo. Se cubren ambas rutas de alta, reenvío de
observaciones, transiciones auditadas, bloqueo de trabajadores en revisión y efecto
inmediato sobre tokens existentes.

Cuenta estudiantil global: confirmación de correo, recuperación/cambio de contraseña,
cambio de correo confirmado y desactivación lógica. Reutiliza V9/V10 y las entidades
revisadas del trabajo previo, con bloqueo por cuenta/token, envío posterior al commit
y cierre de sesiones por security_version. Al desactivar se retiran vínculos activos
y se conservan credenciales históricas. No se integran fotos/CI sin completar su revisión.

## Migraciones y endpoints

V14__student_security_version.sql añade una columna con default 0.
V1–V13 sin cambios. Sin tablas duplicadas ni dependencias adicionales.
Contratos previos: docs/ORGANIZATION_API.md y docs/STUDENT_SECURITY_API.md.

- GET /api/access/me añade onboarding.
- GET /api/institution/status y resúmenes globales añaden reviewNote/allowedTransitions.
- POST /api/institution/submit-review.
- GET /api/me/security.
- POST /api/me/security/email-verification, /password y /email-change.
- DELETE /api/me/security/account.
- POST /api/auth/student/verify-email.
- POST /api/auth/student/password-reset/request y /password-reset/confirm.
- POST /api/auth/student/email-change/confirm.

Se conservan registro/login, DTO, roles y permisos de catálogo existentes.

## Validación

- Maven verify: **30 pruebas PG16**, 0 fallos/errores/omitidas, Flyway V1–V14 y
  Hibernate validate. Log local ignorado: backend/priority-release.log.
- Angular: build producción correcto; **36 Jasmine/Karma/ChromeHeadless** aprobadas,
  log local frontend/priority-tests-final.log. Total distinto: **66**.
- Smokes de organización/MinIO e integridad documental: PASS en Compose aislado.
- Smoke con Mailpit real: PASS para confirmación, replay, recuperación uniforme,
  cierre de sesión antigua, cambio de email con misma identidad y desactivación.
- Nginx: marcador de token ausente del log; no-store/no-referrer y página 200.
- QA escritorio 1280×900/móvil 390×844, reenvío observado→pendiente y guard de
  /credentials confirmados. Se cerró la pestaña y se restableció el viewport.
- git diff contra eb894bf --check correcto tras normalizar EOF de entidades copiadas.

## Ejecución y datos

Docker principal actualizado (web 4300, API 8080, Mailpit 8125); API UP,
login SUPER_ADMIN y Flyway 14 comprobados. No se crearon fixtures en esa base.
Backup previo en .local-backups/credentials-before-priority-20261113-052750.dump,
45 591 bytes; catálogo pg_restore --list legible. No se borraron volúmenes ni datos.

JWT_SECRET de ejemplo se reemplazó por valor aleatorio en .env ignorado; hay que
iniciar sesión de nuevo. Se deshabilita el usuario automático de Spring y no aparece
su contraseña en logs. No se incluyen secretos en commits. Correos locales llegan
a Mailpit; no se utilizó un proveedor externo. Entorno audit y PostgreSQL dedicado
detenidos al finalizar.

## Riesgos y siguiente incremento

MFA administrativo, rate limiting, sesiones individuales y outbox durable siguen
pendientes. La cola de correo asíncrona puede perder entregas; hay reenvío y respuesta
neutral, sin afirmar entrega garantizada. Verificación documental, auditoría integral
y preparación de producción no se declaran completadas. Blockchain/IA/Android
mantienen el alcance previo, sin nuevas integraciones simuladas.

Las tareas amplias T07/T08/T09 siguen parciales aunque estas historias estén resueltas.
Evidencias, pruebas y límites: docs/PRIORITY_01_DELIVERY.md.

## Archivos modificados o creados

- .env.example
- AI_WORKPLAN.md
- README.md
- backend/src/main/java/bo/edu/uagrm/credenciales/CredentialApiApplication.java
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/consent/ConsentRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/IdentityService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccount.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccountRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccountToken.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentAccountTokenRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentMailConfiguration.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentMailDelivery.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentPublicSecurityController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentSecurityController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentSecurityDtos.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentSecurityService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentTokenType.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/OrganizationGovernance.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/StatusEventRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/rbac/AccessService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtAuthenticationFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/OnboardingAccessFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityConfig.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/UserPrincipal.java
- backend/src/main/java/bo/edu/uagrm/credenciales/tenant/TenantRepository.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/db/migration/V14__student_security_version.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/ApprovedOrganizationFixture.java
- backend/src/test/java/bo/edu/uagrm/credenciales/CredentialDocumentIntegrityIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/CredentialFlowIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/JwtConfigurationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/OrganizationOnboardingIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/RbacIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/StudentConsentIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/StudentSecurityIntegrationTests.java
- compose.audit.yml
- docker-compose.yml
- docs/AUDIT_V3.md
- docs/ORGANIZATION_API.md
- docs/PRIORITY_01_DELIVERY.md
- docs/PRODUCT_BACKLOG_V3.md
- docs/SPRINT_01_V3.md
- docs/STUDENT_SECURITY_API.md
- docs/evidence/priority-onboarding-desktop.jpg
- docs/evidence/priority-onboarding-mobile.jpg
- docs/evidence/priority-recovery-desktop.jpg
- docs/evidence/priority-recovery-mobile.jpg
- docs/evidence/priority-student-security-desktop.jpg
- docs/evidence/priority-student-security-mobile.jpg
- frontend/nginx.conf
- frontend/src/app/app.routes.ts
- frontend/src/app/app.ts
- frontend/src/app/core/auth.guard.ts
- frontend/src/app/features/organization/access-login.page.ts
- frontend/src/app/features/organization/organization-onboarding.spec.ts
- frontend/src/app/features/organization/organization-profile.page.ts
- frontend/src/app/features/organization/organization-register.page.ts
- frontend/src/app/features/organization/organization.api.ts
- frontend/src/app/features/organization/organization.models.ts
- frontend/src/app/features/organization/platform-detail.page.ts
- frontend/src/app/features/student-account/student-access.page.ts
- frontend/src/app/features/student-account/student-account.api.ts
- frontend/src/app/features/student-account/student-account.page.ts
- frontend/src/app/features/student-account/student-confirm-email.page.ts
- frontend/src/app/features/student-account/student-recovery.page.ts
- frontend/src/app/features/student-account/student-security.page.ts
- frontend/src/app/features/student-account/student-security.spec.ts
- handoffs/codex-priority-01.md
- handoffs/codex.md
- scripts/smoke-organization.ps1
- scripts/smoke-student-security.ps1
