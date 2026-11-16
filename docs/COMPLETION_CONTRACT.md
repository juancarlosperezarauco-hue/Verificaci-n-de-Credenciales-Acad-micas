# Continuación de los 15 puntos

Base: 7e1cae8, rama codex/rbac-superadmin. No se reemplazan RBAC, identidad global,
consentimientos, firmas ni migraciones V1–V21. El usuario no tiene proveedores,
dominio ni institución piloto; las conexiones reales y la aceptación quedan
pendientes de esos recursos. No se crean cuentas de pago ni se envían mensajes a
destinatarios reales.

## Contratos nuevos (aditivos)

- GET /api/me/credentials/page: page/size (máximo 50), institutionId, year y type;
  resultados propios emitidos/revocados. GET histórico anterior conserva su forma.
- GET /api/me/credentials/filters: instituciones y tipos del expediente propio.
- GET /api/me/notifications: paginación y unreadOnly. PATCH /{id}/read solo propio;
  POST /read-all. Eventos de solicitudes observadas/aprobadas/rechazadas y emisión/
  revocación guardados en la misma transacción del negocio. Correo genérico sin
  CI, adjuntos ni motivos; solo correo verificado y todavía vigente al despachar.
  Reutiliza outbox cifrada con reintentos. Entrega SMTP al menos una vez.
- GET /api/institution/reports/summary y /credentials: REPORT_VIEW y tenant activo;
  filtros year/type/status, exportación /credentials.pdf y /credentials.xlsx.
  Se limita el tamaño de exportación; no se mezclan organizaciones ni CI.
- GET /api/platform/statistics: SUPER_ADMIN, solo totales de plataforma.

## Migración y comprobación

V22 añade notificaciones e índices; token_id de outbox pasa a opcional exclusivamente
para correos académicos. Las notificaciones son privadas y no sustituyen decisiones
humanas. Se prueban aislamiento, filtros, validación, descargas, lectura y rollback.
Las pruebas de PostgreSQL usan base nueva y las integradas usan entorno audit.

## Criterio de entrega de los 15 puntos

Cada punto se registra con evidencia ejecutada y limitaciones. Configuración no es
conexión real; testnet no es producción; certificado de prueba no es confianza
legal; simulación de piloto no es aceptación institucional. No se marca terminado
el conjunto hasta probar restauración, E2E, integraciones configuradas, Android y
aceptación con responsables reales. No se publican secretos.


## Importación académica V23

POST /api/institution/academic/imports/preview recibe 1–100 matrículas JSON con
studentId global ya vinculado, programId, registrationCode y enrolledOn. Valida
programas propios activos y duplicados antes de guardar un lote de 24 horas.
POST /{id}/apply revalida vínculos y conflictos; aplica todo en una transacción.
La repetición devuelve los mismos IDs, sin duplicar matrículas ni crear cuentas.
No se importan CI ni identidades nuevas. ACADEMIC_MANAGE y tenant activo en ambas
operaciones. Los cambios posteriores al preview pueden impedir la aplicación.


## Asistencia OCR y resúmenes

POST /api/institution/identity-verifications/{id}/analysis exige IDENTITY_REVIEW,
vínculo vigente, documentos disponibles y perfil sin cambios. Lee evidencia privada
cifrada a través del servicio existente y la envía solo al servicio OCR configurado.
La asistencia no cambia estados de identidad. Devuelve texto, confianza de lectura
y señales explicadas (nombre/documento no encontrado, baja confianza); no afirma
fraude ni autenticidad. La respuesta no se almacena, no se cachea y su acceso queda
auditable. La revisión continúa siendo humana.

El servicio FastAPI autentica X-Service-Token (32+ caracteres), limita cuerpo a
9 MiB, dos imágenes JPEG/PNG a 3 MiB/8 MP cada una y una lectura simultánea con
8 segundos por imagen. Tesseract español/inglés funciona sin cuentas externas.
Gemini se usa únicamente en /summaries, con el token interno y GEMINI_API_KEY;
no recibe imágenes de identidad. Se ocultan inputs privados en errores 422.
AI_ANALYSIS_ENABLED=false hasta configurar el servicio privado. Su token no debe
publicarse en frontend, Android, Git ni el chat.


GET /api/institution/dashboard devuelve agregados del servidor y las cinco últimas
credenciales. ORGANIZATION_VIEW es obligatorio; totales de estudiantes solo con
STUDENT_VIEW y documentos solo con CREDENTIAL_VIEW. Los datos sin permiso son null,
no cero. El panel principal ya no descarga padrones completos para calcular métricas.
