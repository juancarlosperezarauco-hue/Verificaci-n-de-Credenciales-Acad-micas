# Handoff Codex — Seguridad 02

Fecha: 2026-11-14. Rama: codex/rbac-superadmin. Base: ed42991.

## Resumen

Se implementaron sesiones persistidas y revocables por dispositivo, MFA TOTP
optativo para administradores, códigos de respaldo y límites de intentos
compartidos en PostgreSQL. Angular incluye login en dos factores y Mis sesiones.
STUDENT mantiene identidad global; CUSTOM no se convierte en administrador.
El entorno principal ejecuta esta versión en http://localhost:4300.

## Migración

- V15__sessions_mfa_and_auth_limits.sql: cinco tablas nuevas para sesiones,
  configuración/desafíos/respaldos MFA y contadores de intentos.
- V1–V14 intactas. Upgrade V14→V15 y esquema nuevo V1→V15 comprobados en PG16.
- Añadir MFA_ENCRYPTION_KEY independiente, 32 bytes en base64, fuera de Git.
  Conservarla junto al respaldo de base de datos para poder restaurar MFA.
- JWT anteriores sin jti requieren nuevo login; no se modifica su firma para
  admitirlos sin registro de sesión.

## Endpoints

- GET /api/security/sessions
- DELETE /api/security/sessions/current
- DELETE /api/security/sessions/{id}
- POST /api/security/sessions/revoke-others
- GET /api/security/mfa
- POST /api/security/mfa/setup
- POST /api/security/mfa/confirm
- DELETE /api/security/mfa
- POST /api/auth/mfa/verify
- Login institucional y de plataforma devuelven AuthResponse sin MFA, o
  MfaChallengeResponse sin JWT cuando requieren segundo factor.

Contratos, límites y respuestas en docs/SECURITY_02_API.md.

## Validación ejecutada

- Maven verify: 51/51 sobre PostgreSQL 16.15 nuevo, Flyway y Hibernate validate.
- Angular: build productivo y 43/43 Jasmine/Karma/ChromeHeadless. Total: 94.
- Concurrencia, replay, expiración, aislamiento, revocación, cifrado/AAD,
  vectores RFC, tenant suspendido entre factores y reinscripción de MFA.
- Smokes HTTP reales en Compose aislado: sesiones/MFA/429; organizaciones,
  roles, emisión/verificación/revocación; integridad de PDF/MinIO; recuperación,
  cambios y desactivación estudiantil con Mailpit.
- QA 1280×900 y 390×844, sin overflow de página; cierre de otra sesión y Salir
  confirmado en navegador y DELETE /current 204 en access log.
- API principal UP, web 200 con bundle nuevo, esquema V15, cantidades de
  tenants/student_accounts/credentials sin cambios tras el upgrade.
- Backup privado anterior a la actualización y listado pg_restore correcto.
  No se presenta ese listado como restauración completa.

## Riesgos y trabajo pendiente

- Entrega durable de correo y auditoría transversal SEC-03 pendientes.
- Retención de sesiones/desafíos/respaldos y rotación con recifrado de clave MFA
  pendientes. Guardar respaldos de recuperación; no existe bypass administrativo.
- Proxy confiable/gateway y HTTPS pendientes para producción; el límite IP usa
  socket, no X-Forwarded-For arbitrario. Ventanas fijas pueden denegar antes bajo
  concurrencia.
- La ruta legacy de alta no crea OrganizationProfile detallado; revisar su
  experiencia inicial aparte. La ruta actual del formulario sí lo crea.
- Firma digital, revisión documental, blockchain real, IA/OCR y Android conservan
  su estado del backlog. Este incremento no termina las 40 tareas.
- Docker inició manualmente; la revisión automática bloqueó eliminar un socket
  temporal y esa eliminación no se ejecutó. Los datos y volúmenes se conservaron.
- El checkout ajeno con cambios pendientes no fue modificado.

## Commits

- Backend, migración, contrato y smoke: e3bcd5b396dc4dac21b82268922adbcc662e8288.
- Commit final de implementación validada: 733fda0413376e729b5b281d07ed85dfb2d81bba.
- Este handoff y las evidencias se entregan en el commit de documentación
  posterior; su hash exacto se consulta con `git log -1 --format=%H -- handoffs/codex-security-02.md`.
- Publicación en la rama de desarrollo y PR #1; main se conserva.

## Archivos modificados en este incremento

- .env.example
- README.md
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthDtos.java
- backend/src/main/java/bo/edu/uagrm/credenciales/auth/AuthService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/IdentityService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/identity/StudentSecurityService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/platform/PlatformController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AuthRateBucket.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AuthRateLimitFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/AuthRateLimiter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtAuthenticationFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/JwtService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/LoginSession.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/LoginSessionRepository.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaChallenge.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaCrypto.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaRecoveryCode.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaRepositories.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaService.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/MfaTotp.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/OnboardingAccessFilter.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SecurityConfig.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SessionController.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/SessionRegistry.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/UserMfa.java
- backend/src/main/java/bo/edu/uagrm/credenciales/security/UserPrincipal.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/db/migration/V15__sessions_mfa_and_auth_limits.sql
- backend/src/test/java/bo/edu/uagrm/credenciales/AuthRateIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/MfaCryptoTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/MfaIntegrationTests.java
- backend/src/test/java/bo/edu/uagrm/credenciales/SessionIntegrationTests.java
- backend/src/test/resources/application.yml
- compose.audit.yml
- docker-compose.yml
- docs/PRODUCT_BACKLOG_V3.md
- docs/SECURITY_02_API.md
- docs/SECURITY_02_DELIVERY.md
- docs/SPRINT_01_V3.md
- docs/VALIDATION_V3.md
- docs/evidence/security-sessions-desktop.png
- docs/evidence/security-sessions-mobile.png
- frontend/src/app/app.routes.ts
- frontend/src/app/app.ts
- frontend/src/app/core/account-security.api.ts
- frontend/src/app/core/api.service.ts
- frontend/src/app/core/models.ts
- frontend/src/app/features/account-security/account-security.page.ts
- frontend/src/app/features/account-security/account-security.spec.ts
- frontend/src/app/features/login/login.page.ts
- frontend/src/app/features/organization/access-login.page.ts
- frontend/src/app/features/organization/organization.api.ts
- frontend/src/app/features/student-account/student-access.page.ts
- handoffs/codex-security-02.md
- handoffs/codex.md
- scripts/smoke-sessions-mfa.ps1
