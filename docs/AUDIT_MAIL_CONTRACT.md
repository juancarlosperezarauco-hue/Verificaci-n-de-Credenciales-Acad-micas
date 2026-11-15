# SEC-03 y correo durable — contrato previo

Fecha 2026-11-14. Base c79b963, rama codex/rbac-superadmin.

## Auditoría

V16 añade eventos append-only: id, instante UTC, tenant opcional, actor/tipo/rol,
acción, recurso/tipo y correlación. No contiene contraseñas, tokens, correos,
documentos ni cuerpos de petición. Cambios de permisos guardan códigos del
catálogo; asignaciones guardan UUID de roles y estados institucionales sus enums.
El evento se inserta en la misma transacción que la operación: rollback revierte
ambos. Trigger PostgreSQL rechaza UPDATE/DELETE. No protege frente a un operador
con privilegios para desactivar triggers, truncar o alterar la base.

- GET /api/audit/tenant: tenant del principal, AUDIT_VIEW, sin tenantId aceptado.
- GET /api/platform/audit: SUPER_ADMIN, filtro tenantId opcional y eventos globales.
- Ambos: action, actorId, resourceId, from inclusivo, before exclusivo, page y
  size (1–100); orden occurredAt/id descendente. Respuesta {items,total,page,size}.
- Sin endpoints de modificación/borrado. STUDENT no accede al historial de tenant.
- La correlación se genera en servidor y se devuelve como X-Request-ID.

Cobertura de esta entrega: altas y cambios institucionales, roles/permisos,
usuarios internos/asignaciones/desactivación, borradores/documentos/emisión/
revocación, seguridad estudiantil, MFA y cierre de sesiones. No se presenta como
auditoría de cada lectura, login fallido o evento de infraestructura.

## Correo persistente

V16 añade mail_outbox. La cola se inserta con la transacción que crea el token.
Se conserva el contrato HTTP actual y desaparece el executor con DiscardPolicy.
El contenido completo (destino/asunto/cuerpo) se cifra AES-GCM con una clave
MAIL_ENCRYPTION_KEY independiente, de 32 bytes en base64, fuera de Git.
La fila vincula estudiante y token; no guarda el enlace de acción en claro.

Un worker reclama filas con compare-and-set y lease. Procesa fuera de la
transacción de negocio y confirma usando el identificador de reclamación para
evitar que un worker atrasado sobreescriba al siguiente. Estados: PENDING,
PROCESSING, RETRY, SENT, FAILED y CANCELLED. Cinco intentos por defecto, espera
exponencial desde 30 s hasta 15 min; SMTP tiene timeouts. Lease vencido admite
recuperación después de reiniciar el proceso. El último lease agotado pasa FAILED.

Tokens vencidos/usados/reemplazados o cuentas inactivas cancelan el envío antes
de SMTP. Un enlace ya en tránsito no puede retirarse, pero su canje mantiene las
comprobaciones de vigencia. SENT significa aceptación por SMTP, no lectura ni
entrega final en el buzón. SMTP no ofrece exactamente una vez: un fallo tras
aceptación y antes de confirmar puede duplicar un correo; el token sigue siendo
de un uso. No se cambian las respuestas neutrales de recuperación.

El payload cifrado se retira en estados terminales. Tras agotar intentos se
solicita un enlace nuevo; no se reenvía un secreto expirado ni se ofrece un bypass.
Errores almacenados son categorías estáticas, nunca excepciones del proveedor.

- GET /api/platform/mail: SUPER_ADMIN, state opcional, page/size, metadatos
  {id,studentId,kind,state,attempts,createdAt,nextAttemptAt,sentAt,lastError}.
  No expone destinatario, asunto/cuerpo, token, ciphertext ni clave.
- Sin envío arbitrario, edición o borrado desde la API.
- Interfaz: Auditoría de tenant/global y Correos de plataforma con filtros,
  paginación, estados vacíos, errores y reintento de consulta.

Despliegue: backup antes de V16, configurar clave privada sin cambiar claves
existentes. Pruebas de rollback, aislamiento, permisos, ausencia de secretos,
fallo SMTP, reintento, leases/concurrencia, cancelación y correo real Mailpit.
