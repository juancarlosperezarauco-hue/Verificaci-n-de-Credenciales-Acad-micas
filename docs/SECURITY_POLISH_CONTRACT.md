# Seguridad y registro histórico — contrato previo

Fecha 2026-11-14, base 165ac01, rama codex/rbac-superadmin.

## Recuperación y mantenimiento de MFA

No se añade recuperación con contraseña sola ni bypass administrativo. Los códigos
de respaldo ya permiten login. Después de ingresar se podrá cambiar autenticador
o renovar respaldos con contraseña y segundo factor/código de respaldo vigente.

- POST /api/security/mfa/replacement {password,code}: prepara una semilla nueva
  por diez minutos; el autenticador actual sigue activo hasta confirmar.
- POST /api/security/mfa/confirm {code}: confirma semilla pendiente, cambia la
  generación, revoca sesiones/desafíos anteriores y devuelve sesión + diez respaldos.
- POST /api/security/mfa/recovery-codes {password,code}: renueva respaldos de una
  vez, invalida los anteriores y devuelve sesión nueva. Cancela preparaciones previas.
- DELETE /api/security/mfa/pending: cancela preparación propia, conserva MFA activo.
- GET estado añade pendingSetup/pendingExpiresAt, nunca devuelve semilla guardada.
- Acceso solo administrativo, con controles existentes de sesión/MFA/rate limit.

## Rotación sin pérdida de datos

Las claves se configuran fuera de la UI/Git. Identificadores públicos de 1–40
letras, números o guiones; valor por defecto legacy. Variables *_KEY_ID y
*_PREVIOUS_KEYS para JWT, MFA y MAIL; previous es id=valor;id=valor, sin imprimir.

JWT nuevos incluyen kid y se verifica exclusivamente contra claves configuradas.
JWT anteriores sin kid usan legacy; se conservan controles de sesión y tenant.
No se renuevan firmas de tokens que ya están en el cliente: conservar clave
anterior hasta que no haya sesiones vigentes firmadas con ella.

AES-GCM admite ciphertext legado y prefijo K1.id.payload para claves nuevas;
el AAD/propietario se conserva. Recifrado por lotes y bajo bloqueo, sin cambiar
secretos, usuarios ni enlaces. Correo PROCESSING se excluye para respetar leases.
Los fallos revierten el lote, sin retirar claves ni cambiar variables reales.

- GET /api/platform/security/maintenance: política, identificadores y pendientes
  de recifrado/sesiones por firmante; no expone material criptográfico.
- POST /api/platform/security/maintenance/re-encrypt: recifra hasta 50 filas de
  MFA y 50 correos; auditado; solo SUPER_ADMIN.
- No se rotan automáticamente claves de cuentas reales en esta entrega.

## Retención segura

V17 añade timestamps de respaldo usado/fin de correo, semilla pendiente separada
y firmante de sesión. Timestamps históricos desconocidos parten del despliegue.
Limpieza limitada a 500 filas por categoría y ejecución, con retención mínima de
siete días (30 por defecto): sesiones revocadas/vencidas, desafíos expirados,
respaldos usados, metadatos terminales de correo y tokens sin referencias.
Se limpian semillas pendientes vencidas. Se conservan datos académicos, usuarios,
documentos, eventos de auditoría, sesiones vigentes, respaldos utilizables y correos
pendientes/en procesamiento. Tests desactivan scheduler y controlan el reloj.

- POST /api/platform/security/maintenance/cleanup: ejecuta política configurada,
  sin parámetros para eliminar más datos; solo SUPER_ADMIN; resumen auditado.
- Limpieza horaria configurable; excluye siempre auditoría append-only.

## Registro institucional antiguo

GET /api/institution/details sin perfil responde 404 JSON seguro, sin forwarding
que lo convierta en 403. No crea un perfil vacío ni altera el estado institucional.
La UI permite completar campos, precarga únicamente nombre/tipo ya conocidos y
conserva campos escritos por el usuario. PUT serializa por tenant y mantiene la
misma organización/cuenta. Un tenant no puede leer/editar otro. No se inventan
país, CI ni datos del representante para aprobar automáticamente.

Pruebas: recuperación/replay/reemplazo, rotación compatible y clave desconocida,
recifrado con rollback, retención con datos vigentes, acceso por ámbito y registro
histórico vía HTTP real. Migraciones V1–V16 intactas; backup antes de V17.
