# Revisión documental de identidad — entrega V18

Fecha: 2026-11-15 (America/La_Paz). Base c3daf33; rama codex/rbac-superadmin.
Contrato previo en IDENTITY_REVIEW_CONTRACT.md. Se mantiene el monolito modular,
RBAC existente, identidad global y aislamiento institucional.

## Alcance implementado

- Nueva página estudiantil /student/identity y menú Mi identidad. El estudiante
  elige una institución activa/vinculada, confirma el envío y adjunta frente/reverso
  y fotografía opcional. Perfil completo y correo confirmado son obligatorios.
- Historial con estados Sin verificar, En revisión, Verificado y Rechazado; motivos,
  fechas, consulta de imágenes propias, retirada y nuevo envío tras correcciones.
  La foto forma parte del envío privado: no es un avatar global.
- Bandeja /organization/identity, solo IDENTITY_REVIEW: filtro/paginación, instantánea
  de datos, imágenes autenticadas, aprobación y rechazo con motivo de 10–1000 caracteres.
  Confirmación antes de decidir, estados vacíos/error y liberación de URLs blob.
- La verificación pertenece a una institución y una versión del perfil. Una
  aprobación no valida globalmente al estudiante, no sustituye comprobación estatal
  o biométrica y no crea otra cuenta. El reviewer realiza evaluación humana.
- Editar datos personales invalida su vigencia; una revisión monotónica evita
  restaurarla al escribir los datos antiguos. No se decide sobre datos cambiados.
- Frente/reverso obligatorios; fotografía opcional. JPEG/PNG reales, hasta 5 MiB
  por archivo, 12 MP y 6000 px por lado. Todas las imágenes se validan antes de
  escribir; se recodifican como JPEG sin metadatos/originales, con SHA-256.
- Claves privadas nuevas por envío en MinIO, sin URLs públicas ni claves de objeto
  en DTO. Respuestas autenticadas no-store/nosniff y nombre fijo. La lectura
  institucional comprueba integridad y genera evento de auditoría sin contenido.
- Envío, retirada, verificación/rechazo y lectura institucional auditados en la
  misma transacción; motivo/documento/nombre/CI no se copian a audit_events.
- Acceso institucional solo al tenant del principal y al consentimiento exacto.
  Retirar el vínculo, envío o cuenta bloquea acceso; volver a vincular no reabre
  archivos antiguos. SUPER_ADMIN global y STUDENT_VIEW no permiten ver imágenes.
- Bloqueo por estudiante serializa envíos, cambios de perfil, retirada y decisiones.
  Una decisión pendiente se aplica una vez; concurrencia/replay devuelve 409.
- Límite persistido de seis intentos de subida por estudiante/hora y treinta por
  IP/minuto; 429/Retry-After sin leer ni truncar el multipart.

## Migración, contratos y compatibilidad

V18__identity_review.sql añade identity_revision a student_accounts y tabla
identity_review_submissions, snapshot/huella, archivos/huellas, consentimiento,
estados/revisión/retirada e índices. Un índice parcial impide dos pendientes activos
para estudiante/tenant. V1–V17 intactas; no elimina columnas globales históricas V9.
Estas columnas no se usan como aprobación general ni se migran archivos ajenos.

No cambia los DTO de login, perfil global ni credenciales. No bloquea la emisión
existente exigiendo retroactivamente revisión; esa política necesita definición
funcional aparte. Los envíos son versiones nuevas y no modifican PDFs académicos.

API nueva, detalles y permisos en el contrato:

- GET/POST /api/me/identity-verifications.
- POST /api/me/identity-verifications/{id}/withdraw.
- GET /api/me/identity-verifications/{id}/files/{FRONT|BACK|PHOTO}.
- GET /api/institution/identity-verifications?status=PENDING&page=0&size=20.
- GET /api/institution/identity-verifications/{id}.
- POST /api/institution/identity-verifications/{id}/decision.
- GET /api/institution/identity-verifications/{id}/files/{part}.

Nginx permite 16 MiB solo para esta ruta de subida; el máximo global de multipart
Spring pasa de 12 a 16 MiB. No hay dependencias, claves ni secretos nuevos.

## Verificación ejecutada

- Maven verify completo: **96 pruebas**, cero fallos/errores/omitidas; PostgreSQL
  16.15 nuevo, base identity_final_20261115141442, Flyway V1–V18 y Hibernate validate.
- Angular: **66 pruebas** ChromeHeadless y build de producción correctos.
- Total distinto: **162**, frente a 138 anteriores. Se añaden 12 pruebas de flujo/
  autorización/concurrencia y dos de límite multipart, más diez de componentes web.
- Casos: dos instituciones y estudiantes, permisos retirados con JWT vigente,
  SUPER_ADMIN sin acceso, archivos corruptos/falsos/grandes, perfil/correo incompletos,
  snapshot/revisión monotónica, rechazo/motivo, replay, retirada/revinculación,
  fallo de almacenamiento sin reemplazar historial y concurrencia de decisión/subida.
- Smoke con PostgreSQL/MinIO/Mailpit reales y documentos deliberadamente ficticios:
  carga 3.6 MB por Nginx, normalización JPEG, consulta privada, rechazo/aprobación,
  replay, retirada y reenvío. Incluye regresión de organización/roles/vínculo/
  generación PDF/emisión/expediente/verificación/revocación/suspensión.
- Instalación nueva V1–V18 y actualización real V17→V18 en stack aislado y principal.

Comando aislado (credenciales suministradas desde entorno privado, no imprimir):

```powershell
.\scripts\smoke-identity-review.ps1 -SuperEmail $auditEmail -SuperPassword $auditPassword
```

**QA visual pendiente:** navegador integrado y herramienta de escritorio fallaron
al iniciar con `failed to write kernel assets: ... ruta ... no encontrada (os error 3)`.
No se tomaron capturas nuevas ni se afirma revisión manual de escritorio/móvil.
Los componentes/formularios/blobs están cubiertos por tests; esto no equivale a QA visual.

## Entorno local

Web http://localhost:4300. API http://localhost:18080 (8080 está ocupado por HRcore,
que se dejó intacto). Se actualizó solo BACKEND_PORT en .env privado; el proxy web
usa el nombre interno del backend. PostgreSQL, MinIO, Mailpit y sus volúmenes se conservan.
No se cambian las claves configuradas ni se modifica el otro checkout.

Backup previo: .local-backups/credentials-before-identity-20261115-141659.dump,
comprobado con pg_restore -l; no equivale a restauración integral ensayada.
Cantidades de tenants, student_accounts, credentials y app_users conservadas.
Logs ignorados: backend/identity-postgres-final.log, frontend/identity-test.log,
frontend/identity-build.log, identity-smoke.log e identity-main-build.log.
Credenciales ficticias de QA en .local-backups/identity-qa.json, excluidas de Git.

## Límites y siguiente trabajo

- No hay integración con registro civil, OCR, biometría ni resolución de disputa
  de identidades. T08 general sigue parcial. Tampoco hay avatar global independiente.
- No hay política final de retención/eliminación física de imágenes o KMS para
  objetos. MinIO se usa con acceso privado; esto no significa cifrado KMS en reposo.
- Retirar acceso no elimina copias ya consultadas/descargadas ni historia académica.
- Un rollback tras escribir MinIO puede dejar objetos huérfanos; no se sobrescribe
  ningún objeto vigente. Falta reconciliación/retención de archivos.
- El historial propio aún no está paginado; bandeja institucional sí, máximo 50.
- Sigue pendiente QA visual por infraestructura, además de preparación productiva
  (HTTPS, custodia de secretos, monitoreo/restauración integral).
- Siguiente módulo funcional: expediente institucional y solicitudes/aprobaciones,
  sin duplicar al estudiante global. Consultar PRODUCT_BACKLOG_V3.md.
