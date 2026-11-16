# Revisión documental de identidad — contrato previo

Fecha 2026-11-15. Base c3daf33; rama codex/rbac-superadmin. Incremento T08.

La cuenta estudiantil es global. La revisión de una institución no acredita la
identidad ante las demás ni constituye validación gubernamental/biométrica.
No se cambia RBAC ni se condiciona retrospectivamente la emisión ya existente.

## Consentimiento y datos

El estudiante con perfil completo y correo confirmado elige una institución activa
con la que ya mantiene vínculo vigente. Acepta explícitamente compartir frente y
reverso del documento, más fotografía opcional, para esa revisión. Conocer el
código EST o tener STUDENT_VIEW no permite consultar imágenes.

Cada envío crea una versión nueva con instantánea de los campos personales y
huella del perfil. Una revisión solo es vigente para esos datos. Si cambian, la
UI muestra Sin verificar y se requiere nuevo envío; una decisión sobre datos
desactualizados se rechaza. No se bloquea la edición del perfil global.

Estados: UNVERIFIED (sin envío o datos cambiados/permiso retirado), PENDING,
VERIFIED, REJECTED. Historial conserva envío, decisión, institución y motivo.
Rechazar requiere motivo de 10–1000 caracteres; no se incluye en la bitácora.
Solo se decide una vez un envío pendiente; cambios concurrentes devuelven 409.
No se acepta otro envío pendiente/vigente ya aprobado sin cambio de datos.
Se permite reenviar tras rechazo, retirada o cambio de perfil.
La huella incluye una revisión monotónica: volver a escribir los datos anteriores
no restaura una aprobación vieja. Cada envío queda ligado al consentimiento exacto;
revocar y volver a vincular no reabre las imágenes del consentimiento anterior.

El estudiante puede retirar el acceso a cada envío. La retirada del vínculo, la
desactivación de cuenta o suspensión institucional también bloquean acceso/revisión.
No se borran documentos académicos. La retirada no elimina datos ya descargados.

## Archivos

Solo JPEG/PNG decodificables: hasta 5 MiB por archivo y 12 megapíxeles, dimensiones
máximas 6000 por lado. Frente/reverso obligatorios; foto opcional. Se valida el
contenido real y se recodifica como JPEG sin metadatos antes de guardar. No SVG,
PDF ni archivos arbitrarios. Originales no se guardan; claves de objetos nuevas
por envío, privadas y sin URLs públicas. Hash del contenido recodificado.
Descarga autenticada con Cache-Control no-store, nosniff y nombre fijo seguro.
Subidas: seis intentos por estudiante/hora y treinta por IP/minuto, persistidos en
PostgreSQL con 429/Retry-After; el filtro no consume el cuerpo multipart. Nginx
permite 16 MiB solo en esta ruta; Spring limita el envío completo a 16 MiB.

No hay transacción distribuida MinIO/BD; un rollback tras subir puede dejar objetos
huérfanos, nunca reemplaza documentos vigentes. Retención/borrado físico e imagen
de avatar global quedan fuera de este incremento y deben definirse aparte.

## API

- GET /api/me/identity-verifications: destinos vinculados activos + historial propio.
- POST /api/me/identity-verifications (multipart): tenantId, accepted=true,
  front, back, photo opcional; 201. Requiere perfil completo/correo confirmado.
- POST /api/me/identity-verifications/{id}/withdraw: retira acceso propio.
- GET /api/me/identity-verifications/{id}/files/{FRONT|BACK|PHOTO}: archivo propio.
- GET /api/institution/identity-verifications?status=PENDING&page=0&size=20:
  bandeja paginada por tenant; IDENTITY_REVIEW.
- GET /api/institution/identity-verifications/{id}: detalle propio autorizado.
- GET /api/institution/identity-verifications/{id}/files/{part}: lectura auditada.
- POST /api/institution/identity-verifications/{id}/decision:
  {status:VERIFIED|REJECTED, reason}; IDENTITY_REVIEW; motivo obligatorio al rechazar.

Tenant se obtiene del principal en rutas institucionales, nunca de un parámetro.
Sin permiso: 403. ID/archivo ajeno, vínculo retirado o envío retirado: 404 seguro.
No hay consulta de imágenes para público ni SUPER_ADMIN global. Eventos de envío,
retirada, lectura y decisión solo contienen IDs/estado, nunca documentos o motivos.

## Verificación requerida

Migración nueva V18, V1–V17 intactas; imagen falsa/grande, email/perfil incompletos,
aislamiento de dos tenants/estudiantes, permiso revocado, consentimiento retirado,
perfil cambiado, replay/concurrencia de decisión y fallo MinIO sin corrupción.
Frontend: selección/consentimiento, estados/errores, subida/retirada, bandeja/rechazo,
previsualización autenticada con liberación de URLs blob. Builds, suites completas,
smoke PostgreSQL/MinIO y QA escritorio/móvil antes de publicar y actualizar principal.

Actualización de ejecución: tests y smoke reales comprobados; QA visual pendiente
por fallo de infraestructura de ambas herramientas de interfaz (`failed to write
kernel assets`, ruta no encontrada). La limitación se registra en la entrega;
no se presenta la comprobación de componentes como una revisión visual.

V9 contiene columnas globales de identidad/foto sin flujo implementado en esta
rama. No se borran ni se reinterpretan como aprobación de todas las instituciones.
V18 añade historial por institución/consentimiento, necesario para no mezclar accesos.
