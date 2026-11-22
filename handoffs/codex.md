# Prioridades de arranque y aceptación — 22/11/2026

- Rama: codex/priority-readiness, desde main 234e428.
- Commits de implementación: d25f888 (arranque/salud), 6878905 (TLS/CSP/runtime),
  1a0f1f5bcc5c8bf2a2a3bd6fac08413db838334f (aceptación y baseline).
- Resumen: arranque local sin borrado de datos, corrección del JSON Actuator,
  validación TLS temprana, CSP compatible con Angular, límites de contenedores,
  seed por APIs y Mailpit, ciclo académico con dos instituciones y medición acotada.
- Archivos: scripts/start-local.ps1, scripts/check-health.ps1,
  scripts/load-audit.mjs, ProductionConfiguration.java, PostgresTlsPolicy.java y
  ProductionConfigurationTests.java; frontend/Dockerfile*, angular.json,
  nginx.conf y security-headers.conf; compose.audit.yml, compose.production.yml;
  e2e/seed.mjs, support/audit-api.mjs, tests/academic-cycle.spec.ts,
  tests/security-headers.spec.ts, .github/workflows/verify.yml, README.md y
  docs/PRIORITY_READINESS.md, docs/COMPLETION_STATUS.md.
- Migraciones y endpoints nuevos: ninguno. V1–V23 y contratos conservados.
- Pruebas locales: 18 casos de configuración productiva aprobados; compilaciones
  backend y frontend aprobadas; arranque principal/readiness/liveness/web/AI
  aprobados, una organización y un estudiante conservados; seed por API/Mailpit
  aprobado. La suite E2E local se interrumpió por timeouts bajo presión de tres
  stacks; no se atribuye aprobación funcional a ese intento.
- CI ejecuta PostgreSQL/Flyway, Angular, OCR, Android, nuevas pruebas integradas,
  salud HTTP y baseline acotado. Resultado final en la ejecución de la rama y PR;
  la referencia previa de 280 pruebas corresponde al main de partida.
- Riesgos: incidente local del motor Docker al acumular stacks; HRcore pertenece
  a otro proyecto y no se detiene sin autorización. Pendientes servidor/dominio,
  SMTP, confianza/custodia de firma, blockchain, teléfono y piloto institucional.
- No se publican .env, fixtures privados, tokens, claves ni datos de usuarios.

Detalle: [PRIORITY_READINESS.md](../docs/PRIORITY_READINESS.md).
Se conserva el historial a continuación.

---

# Continuación y validación — 22/11/2026

- Rama: codex/complete-validation; código final de esta continuación:
  d75fdea14023d51ceebd4e340240691fbc42b708 (precedido por ed4c8fc).
- Resumen: corregidos los errores del DSL de Android/AGP 9 y el control de
  firma release; configuración Gradle mantenida como fuente. La preparación
  conserva pubspec.yaml y el lockfile y exige resolver versiones fijadas.
  OCR incompleto devuelve 503 y mantiene la decisión humana.
- Archivos: mobile/android/app/build.gradle.kts, mobile/android/gradle.properties,
  mobile/platform/android/build.gradle.kts, mobile/README.md,
  scripts/setup-mobile.ps1, .github/workflows/verify.yml,
  e2e/tests/identity-ocr.spec.ts, IdentityAnalysisService.java y sus pruebas,
  docs/COMPLETION_STATUS.md, docs/COMPLETION_FILES.txt y
  docs/VALIDATION_2026_11_22.md.
- Migraciones y endpoints nuevos: ninguno; se conservan V1–V23 y contratos.
- Pruebas: CI 44990243503 aprobó los cinco jobs: backend (138), Angular (114),
  OCR nativo (11), navegador/API (9 aprobadas, 1 duplicado móvil omitido) y
  Flutter (5), análisis, compilaciones y APK debug.
  Después, las 7 pruebas locales de IdentityAnalysisServiceTests aprobaron,
  incluidas tres respuestas malformadas; se comprobó que repetir setup-mobile
  conserva los manifiestos y fuentes de la aplicación.
- La CI del código final comprueba además que el grafo release exige validar
  la clave de publicación y que la falta de clave se rechaza. Consultar la
  ejecución de la rama para el resultado final; la evidencia inicial no prueba
  por sí sola cambios posteriores.
- Riesgos: APK configurado para emulador local, sin validación en teléfono;
  servicios externos, CA/TSA, blockchain y piloto siguen pendientes. El motor
  Docker local no fue accesible desde esta sesión al revisar el arranque;
  no se alteraron volúmenes ni secretos para intentar recuperarlo.

Evidencias y enlaces: [VALIDATION_2026_11_22.md](../docs/VALIDATION_2026_11_22.md).
El historial de entregas anteriores se conserva a continuación.

---

# Entrega para integración en main — 16/11/2026

El usuario pidió guardar todos los cambios y publicarlos en main; indicó que revisará
los fallos después. Esta entrega no declara completo el alcance productivo de 15 puntos.

- Commit de código de esta entrega: 0af0a4d332b185e90ccfa837dc522a5d1da854d5.
- Rama: codex/rbac-superadmin; integración mediante PR #1, conservando historial.
- Incremento: notificaciones académicas y outbox cifrada, reportes PDF/Excel,
  estadísticas en servidor, historial paginado, importación CSV con preview/lotes,
  OCR privado de apoyo humano, configuración productiva y recuperación cifrada,
  CI y cliente Flutter/Android con guardado nativo de PDFs.
- Archivos: docs/COMPLETION_FILES.txt. Contratos: docs/COMPLETION_CONTRACT.md.
- Migraciones nuevas: V22 (notificaciones/índices/outbox) y V23 (lotes de importación).
  V1–V21 se conservan.
- Endpoints: /api/me/notifications; /api/me/credentials/page y /filters;
  /api/institution/reports/*; /api/institution/dashboard;
  /api/platform/statistics; /api/institution/academic/imports/*;
  /api/institution/identity-analysis/status y /identity-verifications/{id}/analysis.
- Evidencia ejecutada: 138 pruebas PostgreSQL, 114 Angular, 11 OCR nativo,
  8 Playwright escritorio/móvil y 5 Flutter; build backend/frontend y análisis
  Flutter sin incidencias. Restauración local de BD/MinIO/firma/evidencia comprobada.
- Estado de CI: la primera ejecución falló por un fixture de correo PROCESSING y
  advertencias Flutter. Se publicaron correcciones, pero la nueva ejecución no se
  considera aprobada hasta comprobar su resultado. APK Android aún sin confirmar.
- OCR HTTP: se corrigió el multipart del script y el transporte JDK a HTTP/1.1
  para Uvicorn. Falta repetir el smoke tras reiniciar Docker, que dejó de responder.
- Límites: dominio/servidor/SMTP reales, firma CA/OCSP/CRL/TSA/custodia, blockchain
  real, alertas, conector académico concreto y piloto institucional siguen pendientes.
  No hay proveedores ni institución designados por el usuario.
- No se incluyen .env privados, claves, respaldos ni credenciales reales en Git.

Estado completo y recursos necesarios: docs/COMPLETION_STATUS.md,
docs/RECOVERY_AND_EXTERNAL_SERVICES.md y docs/ACCEPTANCE_PILOT.md.

---

# Entrega Codex: RBAC, superadmin y onboarding

## Entrega prioritaria más reciente

Presentación y mantenibilidad: [WORKSPACE_POLISH.md](../docs/WORKSPACE_POLISH.md).
Panel claro, iconos compartidos, resumen por permisos, búsqueda/páginas de
estudiantes y separación de emisión/documentos/consulta pública.
120 PostgreSQL + 99 Angular correctas (219); V21 sin migraciones nuevas.
Principal actualizado y comprobado; smoke ampliado no completado por memoria
del entorno y QA visual manual pendiente. Auditoría detenida al cerrar.

Ciclo académico 1–9: [codex-academic.md](codex-academic.md).
120 pruebas PostgreSQL + 88 Angular correctas (208), Flyway V1–V21.
Expedientes, solicitudes/aprobación separada, plantillas, QR/firma CMS,
verificación de archivos, reemplazos y ciclo de evidencia/identidad.
Guía: [ACADEMIC_CYCLE_DELIVERY.md](../docs/ACADEMIC_CYCLE_DELIVERY.md).
El historial de entregas anteriores se conserva a continuación.

Menú lateral izquierdo con listas verticales: [codex-navigation.md](codex-navigation.md).
70 pruebas web correctas y build; backend sin cambios (última suite: 96), V18.
Principal actualizado; QA visual pendiente por fallo de herramientas de interfaz.

Revisión documental por institución, V18: [codex-identity-review.md](codex-identity-review.md).
96 backend PostgreSQL 16 + 66 Angular (162), smoke PostgreSQL/MinIO/Mailpit real.
Web 4300/API 18080 con datos conservados. QA visual pendiente por fallo de herramientas.

Seguridad y perfil legado, V17: [codex-security-polish.md](codex-security-polish.md).
82 backend PostgreSQL 16 + 56 web aprobadas (138), smokes HTTP, upgrade compatible
y QA escritorio/móvil. Docker principal actualizado con datos y claves conservados.
Las siguientes secciones documentan entregas históricas.

SEC-03 y correo durable, V16: [codex-audit-mail.md](codex-audit-mail.md).
66 backend PostgreSQL 16 + 49 web aprobadas (115), smokes de fallo SMTP/reinicio
y QA de auditoría/monitor. El Docker principal ejecuta esta versión.

Seguridad 02 y V15: [codex-security-02.md](codex-security-02.md). MFA optativo,
sesiones individuales y límites de intentos; 51 backend PostgreSQL 16 + 43 web
aprobadas, smokes reales y QA visual. El Docker principal ejecuta esta versión.
El detalle anterior se conserva como historial:

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

