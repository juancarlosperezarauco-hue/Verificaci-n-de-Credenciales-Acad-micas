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
- Backend: 11 pruebas con PostgreSQL real, Flyway habilitado y Hibernate validate.
  Las trece migraciones se instalaron correctamente en una base de prueba nueva.
- Frontend: compilación de producción y 24 pruebas Jasmine/Karma/ChromeHeadless.
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

## Revisión visual completada
Se recuperó la herramienta de navegador y se revisó la aplicación real contra una
base PostgreSQL dedicada, con datos ficticios. Se comprobaron el asistente de
registro, los dos accesos, el listado/búsqueda y expediente del superadmin, su
historial, el equipo, el catálogo de permisos y el guardado del perfil institucional.
Se revisaron escritorio (1440 × 1000) y móvil (390 × 844); el contenido de registro,
expediente y roles no desborda horizontalmente el área disponible.

Correcciones del cierre:
- Niveles educativos mediante casillas independientes, utilizables en móvil.
- El tipo institucional conserva el valor guardado y envía un valor escalar.
- Los correos e identificadores largos se ajustan dentro del expediente móvil.
- El error de credenciales inválidas indica cómo revisar los datos de acceso.
- Los empleados con ORGANIZATION_UPDATE pueden editar la imagen institucional.
- No se muestran formularios de equipo si falla la carga inicial; se permite reintentar.

Capturas con datos de prueba:
- [Registro en escritorio](evidence/register-desktop.jpg).
- [Registro en móvil](evidence/register-mobile.jpg).
- [Panel del superadmin](evidence/superadmin-desktop.jpg).
- [Expediente en móvil](evidence/organization-mobile.jpg).
- [Roles y permisos](evidence/roles-desktop.jpg).
- [Confirmación de perfil guardado](evidence/profile-saved.jpg).

Las modificaciones de permisos, suspensión y emisión se validaron mediante las
pruebas de API; la revisión visual no cambió permisos de cuentas reales.
Los servidores temporales de revisión se detuvieron al finalizar.

## Ajuste del registro: selección de documentación
- Tipo de documento mediante desplegable: CI, DNI, pasaporte, extranjería u otro.
- Cargo mediante desplegable con funciones institucionales habituales.
- País con nombres en español; el contrato sigue enviando el código de dos letras.
- Al elegir Bolivia, departamento mediante desplegable de sus nueve departamentos.
  Para otros países se permite escribir la región. Cambiar el país limpia la región.
- Nombre del representante y correo ocupan filas completas; tipo y número de
  documento se presentan juntos. Los valores históricos fuera del catálogo se
  conservan como opciones identificadas, sin sobrescribirlos.
- Tres pruebas nuevas cubren selección/documento enviado, país/departamento y
  compatibilidad con valores guardados. Sin cambios de API ni migraciones.
- Se verificó en escritorio y móvil, sin enviar el registro ni crear datos en la base.
  [Captura del formulario corregido](evidence/register-document-dropdown.jpg).

## Simplificación de identificación
Se quitó el bloque de niveles educativos del registro y de su revisión final.
La primera pantalla solicita únicamente el nombre legal y el tipo de institución.
El registro envía niveles vacíos; su validación en la API ya permite esa lista.
En el perfil institucional siguen disponibles como información opcional, conservando
los valores históricos. No requiere migraciones.

## Registro general de organizaciones educativas
Las ocho clases institucionales utilizan el mismo asistente: escuela, colegio,
universidad, instituto, instituto técnico, academia, centro de formación/capacitación
y otra organización educativa. La identificación solicita nombre y tipo.
La información fiscal, licencia, registro ministerial y acreditación es opcional;
el asistente permite continuar sin esos datos y completarlos después.
V13 permite guardar NIT/licencia ausentes como NULL y conserva las restricciones
de unicidad para los identificadores informados. Las pruebas de PostgreSQL cubren
alta y edición de los ocho tipos sin identificadores ni niveles, además de los
rechazos por identificadores duplicados cuando se suministran.
Se comprobó en la web Docker que se avanza sin niveles y con el paso legal vacío.
La vista móvil conserva su ancho disponible. No se enviaron registros de prueba
a la base local. [Captura del registro general](evidence/register-general.jpg).

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
