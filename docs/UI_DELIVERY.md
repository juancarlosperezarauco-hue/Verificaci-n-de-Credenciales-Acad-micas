# Entrega integrada de organizaciones y acceso

Codex retomó el frontend de Antigravity. No existía código nuevo en su rama local,
solo un plan y dependencias. Se implementó directamente contra los contratos reales.

## Páginas
- /register (alias /org/register): asistente de cinco pasos con identificación,
  contacto, información legal, representante/cuenta y revisión con confirmación.
- /login: acceso institucional, con destino acorde a los permisos del empleado.
- /superadmin/login: acceso global separado.
- /organization (alias /org/admin): datos legales y representante, edición o lectura
  según permisos, estado institucional y formulario para registros históricos.
- /organization/team: alta/edición/desactivación de empleados y CRUD de roles;
  catálogo de 19 permisos y asignación de múltiples roles.
- /superadmin/organizations: métricas, búsqueda, filtro de estado y paginación local.
- /superadmin/organizations/:id: expediente, representante, cambio de estado con motivo
  y confirmación e historial.
- /access-denied: salida accesible para cuentas sin permisos.

## Integración
Se preservaron los módulos de estudiantes y certificados. La navegación, los guards
y los botones institucionales ahora respetan los permisos cargados por /api/access/me.
El dashboard evita consultar módulos que la cuenta no puede leer.
Los formularios gestionan validación, carga, 400/401/403/404/409 y reintentos.
Se agregó GET /api/institution/status, limitado al tenant autenticado.
Diseño claro con layouts adaptables, etiquetas, ayudas y diálogos CDK con foco.

## Validación
- Backend: 9 pruebas con PostgreSQL real, Flyway habilitado y Hibernate validate.
  Las doce migraciones se instalaron correctamente en una base de prueba nueva.
- Frontend: compilación de producción y 15 pruebas Jasmine/Karma/ChromeHeadless.
  Casos nuevos: pasos y contrato de registro, contraseñas, validación, permisos,
  alta de empleados y filtros del superadmin.
- Prueba de servicios reales con scripts/smoke-organization.ps1: PASS.
  Registro institucional → rol → empleado → cuenta estudiante → perfil →
  vinculación por código → certificado → PDF MinIO → descarga → emisión →
  verificación pública → expediente → revocación → suspensión/restauración.
  Solo datos de prueba en PostgreSQL dedicado; los PDFs de prueba se almacenaron
  bajo el tenant de prueba en MinIO local.
- Docker local: frontend/API responden 200; login de superadmin, /api/access/me y
  listado de organizaciones verificados a través del proxy del frontend.
  La base local pasó de V10 a V12, conservando los datos existentes.

## Limitación de revisión visual
cua_repl falló al inicializar dos veces con "failed to write kernel assets".
No se obtuvieron capturas ni revisión visual manual de escritorio/móvil.
Las pruebas de componentes se ejecutaron en ChromeHeadless.

## Acceso local
Web: http://localhost:4300
Registro: http://localhost:4300/register
Superadmin: http://localhost:4300/superadmin/login
El acceso inicial del superadmin está configurado únicamente en el .env ignorado
del checkout Credenciales-codex-rbac. No hay contraseña real en Git.
Correo local: superadmin@credenciales.local. Contraseña: valor local de
BOOTSTRAP_SUPERADMIN_PASSWORD. Las instalaciones nuevas deben elegir sus propios
valores BOOTSTRAP_SUPERADMIN_EMAIL/PASSWORD antes del primer arranque.

## Ejecución
Compilar backend con mvnw verify y frontend con npm run build. Luego:
docker compose -p verificacion-credenciales -f docker-compose.yml -f compose.prebuilt.yml up --build -d

La entrega se publica en codex/rbac-superadmin; main se conserva.
Los módulos de revisión documental, reportes/auditoría avanzada y los cambios locales
de seguridad del estudiante ajenos a esta rama siguen fuera de este incremento.
