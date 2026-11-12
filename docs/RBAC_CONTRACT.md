# Contrato RBAC y superadministración

Base: origin/main a53fc14. Rama propietaria: codex/rbac-superadmin.

Codex posee security, auth, user, rbac, platform y migración V11.
V9/V10 están reservadas a seguridad de estudiantes existente localmente.
DeepSeek debe usar V12 o posterior para onboarding y coordinar cambios de auth/Tenant.
Antigravity consume este contrato sin crear endpoints alternativos.

- STUDENT: identidad global en student_accounts.
- SUPER_ADMIN: app_users sin tenant; login POST /api/auth/platform/login.
- TENANT_ADMIN: administrador institucional. ADMIN permanece como alias histórico.
- CUSTOM: empleado con roles propios del tenant, nunca administrador implícito.
- ISSUER/AUDITOR históricos conservan permisos equivalentes.
- JWT identifica usuario/tenant; permisos y estado se recargan de BD por petición.
- Superadmin administra organizaciones por /api/platform/organizations; no hereda permisos institucionales.

Endpoints:
- GET /api/access/me: userId, tenantId, role, permissions.
- GET /api/access/permissions: catálogo.
- GET/POST /api/access/roles; PUT/DELETE /api/access/roles/{id}.
  Escritura: {name, permissions:[códigos]}. Nombres únicos ignorando mayúsculas por tenant.
- GET/POST /api/access/users; PUT /api/access/users/{id}; PATCH /api/access/users/{id}/disable.
  Alta: {fullName,email,password,roleIds:[]}; edición: {fullName,roleIds:[]}.
  Los endpoints gestionan empleados CUSTOM; no permiten crear/elegir SUPER_ADMIN/TENANT_ADMIN.
- GET /api/platform/organizations; GET /api/platform/organizations/{id}.
- PATCH /api/platform/organizations/{id}/status: {status,reason}.
  Estados ACTIVE, PENDING_REVIEW, OBSERVED, SUSPENDED, REJECTED, INACTIVE.
  Cambiar estado registra actor y fecha; solo ACTIVE habilita acceso institucional.
- GET /api/platform/organizations/{id}/history: historial de cambios de estado.

Inicialización de superadmin: variables BOOTSTRAP_SUPERADMIN_EMAIL y
BOOTSTRAP_SUPERADMIN_PASSWORD (mínimo 12 caracteres); ninguna credencial por defecto.
Quitar esas variables después del primer arranque. No hay registro público de superadmin.

Los registros existentes y el registro actual mantienen ACTIVE por compatibilidad.
DeepSeek puede introducir PENDING_REVIEW en su flujo de onboarding coordinándolo con Codex.
Roles eliminados y permisos modificados surten efecto en la siguiente petición.
Los IDs de usuarios/roles recibidos se resuelven siempre dentro del tenant autenticado.

Validación: Maven verify, pruebas RBAC, Angular build/test y PostgreSQL/Flyway.
La UI de gestión corresponde a Antigravity. Los permisos IDENTITY_REVIEW,
REPORT_VIEW y AUDIT_VIEW son catálogo para módulos posteriores.
