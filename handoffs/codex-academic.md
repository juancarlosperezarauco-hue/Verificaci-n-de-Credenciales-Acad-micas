# Entrega: ciclo académico 1–9

Rama codex/rbac-superadmin; base cbe2989. PR existente #1, sin merge a main.
Contrato acordado antes del código: docs/ACADEMIC_CYCLE_CONTRACT.md.
Guía funcional y límites: docs/ACADEMIC_CYCLE_DELIVERY.md.

## Resumen

Se conserva el modelo SUPER_ADMIN global, TENANT_ADMIN, trabajadores CUSTOM y
STUDENT global. Se añaden programas/matrículas, solicitudes y adjuntos, revisión
separada de aprobación, plantillas versionadas, PDF con QR real, firma CMS,
comprobación pública de archivos, reemplazos e identidad/evidencia.

Una aprobación crea un borrador único. Emitir verifica la revisión del perfil,
el consentimiento, los bytes y la firma exigida; una corrección enlaza/revoca
atómicamente la anterior y conserva sus bytes. Se serializa con cambios del perfil.
Reintentos con la misma Idempotency-Key no repiten el anclaje de desarrollo.
Los objetos nuevos se cifran y registran independientemente de la transacción
de negocio para reconciliar fallos sin borrar objetos todavía referenciados.

## Archivos y migraciones

Inventario exacto: codex-academic-files.txt.
Paquetes nuevos: academic, workflow, template y signing.
Se amplían credential, storage, identityreview, identity, consent, shared,
audit y los guards/límites existentes. Frontend añade funcionalidades separadas,
conservando el drawer izquierdo.

- V19: programas, matrículas, solicitudes, eventos, requisitos y permisos.
- V20: plantillas, configuración cifrada de firma, metadatos documentales,
  reemplazos y registro de objetos gestionados.
- V21: disputas/eventos y retirada de evidencia; referencias de imágenes anulables.

V1–V18 no se modifican. La migración no elimina objetos físicos ni datos académicos.
Solo tenants activos y vínculos autorizados permiten gestión institucional.

## Endpoints

- /api/institution/academic/programs GET/POST y PUT /{id}.
- /api/institution/academic/records GET/POST y PATCH /{id}.
- /api/me/academic-records GET y /{id}/templates GET.
- /api/me/credential-requests GET/POST, PATCH /{id}, POST /{id}/actions,
  POST /{id}/attachments, DELETE/GET /{id}/attachments/{attachment}.
- /api/institution/credential-requests GET, GET /{id},
  POST /{id}/actions y GET /{id}/attachments/{attachment}.
- /api/me/credentials/{id}/corrections POST y /{id}/document GET.
- /api/institution/templates GET/POST, PATCH /{id}/active, GET /{id}/preview.
- /api/institution/signing-key GET/POST multipart, POST /demo, PATCH /policy.
- /api/credentials/{id}/sign POST, generate-document acepta plantilla opcional;
  issue admite Idempotency-Key conservando los clientes anteriores.
- /api/public/credentials/{code} GET; POST /{code}/check-document multipart.
- /api/me/avatar/status GET; /api/me/avatar GET/POST/DELETE.
- /api/me/identity-disputes GET/POST, POST /{id}/withdraw o /resubmit,
  GET /{id}/files/{part}.
- /api/platform/identity-disputes GET, POST /{id}/resolution,
  GET /{id}/files/{part}, solo SUPER_ADMIN.
- /api/platform/storage/maintenance GET, POST /retire-evidence, /cleanup,
  /re-encrypt, solo SUPER_ADMIN.

## Pruebas

- PostgreSQL 16.15, base de prueba nueva: Maven verify, 120 correctas,
  cero fallos/errores/omitidas; Flyway V1–V21 y Hibernate validate.
- H2: 119 correctas y una omitida específica del trigger PostgreSQL.
- Angular: 88 Jasmine/Karma/ChromeHeadless correctas y build de producción.
- 24 pruebas backend nuevas y 18 web nuevas: total distinto 208.
- Firma real, importación PKCS12/contraseña/vigencia, sello visible de prueba,
  modificación posterior, firmante ajeno, PDF activo/falso y QR decodificado.
- Dos tenants, estudiante global, observación/subsanación, archivos privados,
  autoaprobación denegada, aprobación concurrente, consentimiento retirado,
  edición de perfil concurrente, reemplazo y bytes originales preservados.
- Avatar, disputa privada/observación/corrección, cierre de sesiones,
  CI ocupado protegido, retención de evidencia vigente, archivos huérfanos
  tras rollback y reintento de eliminación.
- Frontend: controles por rol, confirmaciones, limpieza de contraseña PKCS12,
  firma de prueba, reintento de emisión, cancelación de consultas obsoletas,
  consentimiento y liberación de URLs privadas.

Smoke real PASS: scripts/smoke-academic-cycle.ps1, nginx, PostgreSQL, MinIO y
Mailpit. Incluye regresión de organizaciones/identidad, requisito privado,
versiones/muestra, revisión, firma real, detección de alteración, descarga propia,
reemplazo conservando bytes y resolución de disputa. Upgrade auditado V18→V21.
Logs de trabajo ignorados por Git: cycle-*.log.

Docker perdió conexión con su VM durante el primer intento de smoke. El reinicio
normal agotó el plazo; se cerraron únicamente procesos de Docker y se reinició
su distribución docker-desktop, conservando volúmenes. Ubuntu/Fedora permanecieron
detenidos. Se restablecieron los contenedores previamente activos; luego el smoke
completo terminó correctamente. No hubo prune, down -v ni eliminación de datos.

Principal actualizado a V21: web4300/API18080 UP, bundle main-EXG6TBBS.js comprobado,
conteos de organizaciones/estudiantes/credenciales/consentimientos conservados.
El login SUPER_ADMIN y el estado de cifrado se comprobaron por HTTP.
Backup privado credentials-before-cycle-20261115-183151.dump con 179 entradas
en pg_restore -l, y copia privada de .env con las claves nuevas. No es un ensayo
de restauración. El smoke solo creó datos en auditoría. Contenedores de auditoría
y codex-rbac-postgres-check detenidos al cerrar; principal y HRcore restablecidos.

## Riesgos y límites

Piloto local HTTP. Firma CMS no implica confianza legal: sin CA, OCSP/CRL, TSA
o PAdES-LTV. Certificados de prueba llevan marca visible. La custodia productiva
y autorización del firmante requieren definición institucional.
SIGNING_ENCRYPTION_KEY no tiene rotación automática; conservarla para restaurar.
EVIDENCE_ENCRYPTION_KEY tiene identificador/anillo anterior y recifrado por lotes.
No se regeneran las claves JWT/MFA/correo existentes.

Retención 90 días, limpieza automática apagada. SUPER_ADMIN confirma retirada y
limpieza por separado; no se fusionan cuentas ni se transfiere un CI ocupado.
El registro de objetos no importa ni elimina automáticamente todo el bucket legado.
No es un antivirus ni una certificación de identidad gubernamental.
Las operaciones con objetos usan I/O dentro de transacciones acotadas; para
una red blockchain real falta outbox/confirmación/reintentos (T19).
Historial propio no paginado; plantillas de texto/orientación, sin editor libre.
QA visual manual pendiente por fallo de kernel assets; tests DOM no la sustituyen.

## Commits

- 811338f: contrato previo.
- 15cfa8b: esquema V19–V21 y permisos.
- 769bd25: dominio/API, firma y pruebas.
- 0fe3bbd: interfaces por módulo y pruebas Angular.
- c6e317a: configuración y smoke HTTP.

Commit final de implementación: c6e317a. El cierre documental queda en HEAD
de codex/rbac-superadmin; verificar con git rev-parse HEAD o con el head del PR #1.
