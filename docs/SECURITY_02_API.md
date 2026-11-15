# Seguridad 02: contrato previo a implementación

Fecha 2026-11-14. Continuación de ed42991, sin modificar V1–V14 ni cuentas ajenas.

## Sesiones por dispositivo

Todo JWT nuevo tiene jti con una sesión persistida. Los JWT anteriores sin jti
requieren iniciar sesión de nuevo al actualizar. Se conserva security_version
estudiantil y la revisión dinámica de usuario/roles/tenant. La sesión conserva
propietario (usuario O estudiante), ámbito, vencimiento, revocación, etiqueta de
navegador y última actividad. No conserva token completo ni dirección IP.

- GET /api/security/sessions: sesiones propias vigentes, actual marcada.
- DELETE /api/security/sessions/{id}: revoca solo una sesión propia; 404 si ajena.
- DELETE /api/security/sessions/current: cierre de sesión real en servidor.
- POST /api/security/sessions/revoke-others: cierra las demás sesiones propias.

Acceso también durante onboarding, exclusivamente a las sesiones propias.

## MFA administrativo optativo

Solo SUPER_ADMIN, TENANT_ADMIN y ADMIN. TOTP RFC 6238, 6 dígitos, paso 30 s,
ventana ±1 paso y prevención de reutilización. No se activa automáticamente en
cuentas reales. Semilla cifrada AES-GCM con MFA_ENCRYPTION_KEY de 32 bytes base64,
independiente de JWT_SECRET; guardar esa clave fuera de Git y junto a backups seguros.

- GET /api/security/mfa: estado y cantidad de códigos de recuperación disponibles.
- POST /api/security/mfa/setup {password}: semilla/otpauth URI, solo en la respuesta
  de preparación (10 minutos); no habilita MFA todavía.
- POST /api/security/mfa/confirm {code}: confirma posesión, habilita MFA, revoca
  sesiones previas y devuelve {session:AuthResponse,recoveryCodes:[...]} una vez.
- DELETE /api/security/mfa {password,code}: requiere contraseña y TOTP o código de
  recuperación; deshabilita MFA y cierra sesiones. Nunca basta la contraseña sola.
- POST /api/auth/mfa/verify {challengeToken,code}: completa login pendiente.

El login conserva AuthResponse para cuentas sin MFA. Con MFA retorna
{mfaRequired:true,challengeToken,expiresAt}, SIN JWT ni acceso a la aplicación.
El desafío aleatorio se almacena hasheado, caduca en 5 minutos, admite 5 intentos
y se consume una sola vez; las denegaciones se confirman sin revertir contadores.
Los códigos de recuperación son aleatorios, hasheados y de un uso. Al confirmar
MFA, las sesiones anteriores dejan de funcionar, incluida la de preparación.
Cada desafío pertenece a una generación del autenticador. Deshabilitar o volver
a habilitar MFA invalida los desafíos previos, incluso con un código nuevo correcto.
El segundo factor vuelve a consultar el estado del usuario y de su organización.

Algoritmo y pruebas de vectores: [RFC 6238](https://www.rfc-editor.org/rfc/rfc6238),
[RFC 4226](https://www.rfc-editor.org/rfc/rfc4226). No se envía la semilla a terceros.

## Límites de intentos

Contadores compartidos en PostgreSQL, no solo memoria de una instancia. Se usan
claves HMAC para no almacenar correos/IP en claro. Login/códigos excesivos devuelven
429 con Retry-After; recuperación limitada por identidad conserva respuesta 204
uniforme exista o no cuenta. Se limita tamaño del JSON y no se registran cuerpos.
La IP se toma del socket, sin confiar en X-Forwarded-For enviado por el cliente.
Un proxy comparte ese límite IP; se debe configurar un proxy confiable/gateway
antes de producción. Los límites específicos por cuenta siguen siendo independientes.

Límites iniciales: IP 120/minuto; login por ruta + slug + correo normalizados
10/15 minutos; recuperación 3/15 minutos por correo; registros 30/15 minutos por
IP; códigos/acciones sensibles 10/5 minutos por propietario o IP. Tamaño máximo
del JSON de autenticación: 8192 bytes. Los intentos exitosos también cuentan.
La ventana es fija; cada hora se limpian contadores vencidos hace más de 24 horas.
Bajo concurrencia pueden
denegarse solicitudes anticipadamente; no se pierden incrementos.

V15 añade sesiones, configuración/desafíos/códigos MFA y contadores de intentos.
No borra registros ni convierte estudiantes en trabajadores. Los controles se
verifican mediante pruebas de acceso cruzado, replay, concurrencia y expiración.
