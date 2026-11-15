# Seguridad 02 — MFA, sesiones e intentos

Fecha: 2026-11-14. Base ed42991, rama codex/rbac-superadmin.

## Comportamiento entregado

- Mis sesiones (`/account/security`) muestra dispositivos, última actividad,
  vencimiento y sesión actual. Permite cerrar una sesión propia o las demás.
- Salir revoca la sesión en el servidor. Si falla la conexión, limpia el acceso
  local y avisa que el cierre remoto no pudo confirmarse.
- SUPER_ADMIN y administradores institucionales pueden habilitar MFA desde esa
  página, confirmando contraseña, escaneo QR local y TOTP. Ninguna cuenta real
  se inscribe automáticamente. CUSTOM y STUDENT solo administran sus sesiones.
- Una cuenta con MFA recibe un desafío temporal después de la contraseña;
  todavía no recibe JWT. Un TOTP válido o código de recuperación de un uso
  completa el acceso. Hay diez códigos de respaldo, visibles una sola vez.
- Confirmar/deshabilitar MFA revoca las sesiones previas. Deshabilitar exige
  contraseña y segundo factor. No existe una ruta de desactivación administrativa
  que evite ese requisito.
- Los intentos se limitan mediante contadores compartidos en base de datos.
  Recuperación conserva 204 uniforme al limitar por correo; login/códigos usan
  429 y Retry-After. Se ignora X-Forwarded-For no confiable.

## Migración y compatibilidad

V15__sessions_mfa_and_auth_limits.sql añade login_sessions, user_mfa,
mfa_challenges, mfa_recovery_codes y auth_rate_limits. V1–V14 conservan su
contenido. No se eliminan ni duplican identidades o credenciales.

Cada JWT nuevo tiene jti asociado a una sesión. Los JWT anteriores requieren
volver a ingresar una vez. Se conservan la versión de seguridad estudiantil,
los permisos dinámicos, el aislamiento por tenant y el onboarding restringido.
El login sin MFA conserva AuthResponse; el login con MFA añade la respuesta
alternativa documentada en SECURITY_02_API.md.

MFA_ENCRYPTION_KEY es obligatoria: exactamente 32 bytes aleatorios en base64,
independientes de JWT_SECRET. La semilla se cifra con AES-GCM y usuario como AAD.
Guardar la clave junto a respaldos seguros, fuera de Git. Perderla impide validar
el autenticador existente. La rotación de esta clave requiere un procedimiento
de recifrado que no está incluido en este incremento.

## Validación

- Maven verify: 51 pruebas correctas, cero fallos/errores/omitidas, sobre una base
  nueva PostgreSQL 16.15, Flyway V1–V15 y Hibernate validate.
- Angular: build de producción y 43 pruebas Jasmine/Karma/ChromeHeadless correctas.
- Total de pruebas distintas: 94. Incluye 21 nuevas backend y siete nuevas web.
- Vectores RFC 6238, ventana/replay, cifrado manipulado o de otro propietario,
  cinco intentos persistidos, expiración, respaldo de un uso y consumo concurrente.
- Tenant suspendido entre factores no completa acceso. Un desafío anterior a
  reinscribir el autenticador tampoco acepta códigos de la configuración nueva.
- Sesiones ajenas dan 404; revocar tiene efecto inmediato; cerrar las demás
  conserva la actual. JWT sin jti y sesiones vencidas se rechazan.
- Límites: normalización de correo, respuesta neutral, expiración sin esperar,
  concurrencia sin perder incrementos, IP falsificada y JSON excesivo.
- La interfaz no guarda un desafío como sesión; borra contraseña/código, retira
  semilla/QR tras confirmar y permite ocultar los respaldos.

Comandos: `backend/mvnw.cmd -B verify` con datasource de prueba PG16, Flyway
habilitado y ddl-auto=validate; `npm test -- --watch=false --browsers=ChromeHeadless`
y `npm run build`. No ejecutar los fixtures sobre una base con datos de usuario.
El perfil test por defecto usa H2 y no valida migraciones.

Smoke HTTP reproducible: `scripts/smoke-sessions-mfa.ps1`, restringido a la API
aislada 127.0.0.1:18081. Solo crea cuentas ficticias y no imprime tokens/semillas.

Resultados con servicios reales en Compose aislado:

- Upgrade de la base existente V14 a V15 correcto; Hibernate validate y health UP.
- Smoke de sesiones/MFA/429 correcto, incluido TOTP calculado en PowerShell y
  contrastado con un vector público RFC antes del ensayo.
- Regresiones HTTP correctas de organizaciones, aprobación, roles, vínculo,
  expediente, PDF real en MinIO, emisión, verificación, revocación y suspensión.
- Regresión de integridad documental correcta: rechazos sin alterar bytes/hash.
- Seguridad estudiantil con Mailpit correcto: confirmación, recuperación, cambio
  de correo, rechazo del JWT anterior, identidad conservada y desactivación.
- QA navegador 1280×900 y 390×844: MFA sin activar, dispositivos, cierre de otra
  sesión y Salir regresando a login. Sin desbordamiento horizontal de la página.
  Capturas ficticias en `docs/evidence/security-sessions-{desktop,mobile}.png`.

Entorno principal actualizado en localhost:4300, API localhost:8080 con health UP,
esquema V15 y bundle de esta compilación. Backup previo privado en
`.local-backups/credentials-before-security02-20261114-205410.dump`; archivo
comprobado con pg_restore -l. Las cantidades de organizaciones, estudiantes y
credenciales coinciden antes/después de actualizar. No se borraron datos/volúmenes.
No se declara un ensayo de restauración completo por haber listado el backup.

Docker arrancó tras intervención del usuario. Una eliminación propuesta del
socket temporal fue rechazada automáticamente por política y no se ejecutó.
El primer build de auditoría creó ambas imágenes pero falló al renombrar un
metadato temporal de Compose en Windows; up --no-build usó las imágenes nuevas
comprobadas. El build y arranque del entorno principal terminaron correctamente.

## Límites y siguiente prioridad

- Falta outbox de correo con reintentos y auditoría transversal de seguridad.
- Las sesiones, desafíos y respaldos usados todavía requieren política de
  retención/limpieza; el contador de intentos sí tiene limpieza programada.
- Tras un proxy todas las peticiones comparten IP de socket. Antes de producción
  hay que definir proxies confiables y límites de gateway, sin confiar en cabeceras
  arbitrarias. La ventana fija puede ser conservadora bajo concurrencia.
- No hay renovación de sesión/refresh token ni recuperación manual privilegiada
  de MFA. El usuario debe conservar códigos de respaldo. HTTPS es obligatorio
  para el despliegue productivo; el entorno actual sigue siendo desarrollo local.
- T07 sigue parcial. Firma digital, auditoría integral, blockchain real, IA/OCR y
  Android conservan el estado pendiente/parcial del backlog.
- Hallazgo previo fuera de este incremento: una organización creada por la ruta
  antigua `/api/auth/register` no tiene OrganizationProfile detallado hasta un
  PUT; la consulta de detalles inicial puede terminar en 403 por el dispatch de
  error. El alta web actual usa `/api/auth/organizations/register` y sí crea ese
  perfil. Corregir la experiencia de organizaciones históricas en una entrega
  específica; el fixture visual antiguo permitió comprobar este caso.

La siguiente prioridad es SEC-03, bitácora de acciones críticas por ámbito, y
la entrega durable de correos; después, validación documental de identidad y
flujo académico de solicitudes/aprobaciones.
