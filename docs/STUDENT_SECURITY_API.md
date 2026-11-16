# SEC-02 — seguridad de la cuenta global

Contrato previo a implementación. Se reutilizan student_accounts y los tokens de
V9/V10, revisados desde el trabajo local pendiente sin modificar aquel checkout.
V14 añade únicamente security_version BIGINT NOT NULL DEFAULT 0 a student_accounts.

## API

Públicos bajo /api/auth/student:

- POST /password-reset/request {email}: 204 uniforme exista o no cuenta activa.
- POST /password-reset/confirm {token,newPassword}: 204; enlace válido una vez/30 min.
- POST /verify-email {token}: 204; enlace válido una vez/24 h.
- POST /email-change/confirm {token}: 204; enlace válido una vez/30 min.

Solo STUDENT, bajo /api/me/security:

- GET: {email,emailVerified,active}; nunca devuelve hashes, tokens ni documentos.
- POST /email-verification: 204, reenvía al correo actual si aún no está confirmado.
- POST /password {currentPassword,newPassword}: 204, invalida sesiones previas.
- POST /email-change {currentPassword,newEmail}: 204, confirma en el nuevo correo;
  no modifica email hasta confirmar. Colisiones usan 409 sin exponer otra cuenta.
- DELETE /account {currentPassword}: 204, desactivación lógica y cierre de sesiones;
  preserva identidad, vínculos y credenciales históricas.

Tokens aleatorios de 256 bits: solo SHA-256 en base; no salen en respuestas/logs.
Las operaciones serializan por cuenta y consumen tokens bajo bloqueo para impedir
replay. Un cambio/recuperación de contraseña o correo invalida tokens pendientes e
incrementa security_version. JWT incluye esa versión y se compara con la cuenta
actual en cada petición. Tokens estudiantiles anteriores sin claim equivalen a 0.
Los trabajadores siguen su autorización RBAC; nunca se mezclan con estas cuentas.

Correo: envío después del commit y asíncrono, con tiempos de espera y cola acotados;
Mailpit en local. La recuperación devuelve el mismo resultado aunque no se pueda
entregar correo; el usuario puede solicitar otro enlace. No existe garantía durable
de entrega hasta implementar outbox. No se envían enlaces/token al log.

La contraseña usa 10–72 caracteres y como máximo 72 bytes UTF-8 por BCrypt.
La verificación de correo no duplica cuentas ni altera la identidad documental.
El correo sin confirmar se muestra en la interfaz, sin introducir en este incremento
un bloqueo retroactivo de expedientes existentes. MFA, lista de sesiones y límites
distribuidos de intentos siguen siendo incrementos distintos; no se declaran hechos.

JWT_SECRET es obligatorio y se rechazan las claves públicas de ejemplo anteriores
y valores de menos de 32 bytes. El usuario automático de Spring Boot se deshabilita:
solo se utilizan las cuentas y autenticación de la plataforma, sin contraseñas
automáticas escritas en logs. Las claves reales se configuran localmente fuera de Git.
Nginx excluye los enlaces de recuperación/confirmación del log de acceso y aplica
no-store/no-referrer; la interfaz retira el token de la URL al cargar.
