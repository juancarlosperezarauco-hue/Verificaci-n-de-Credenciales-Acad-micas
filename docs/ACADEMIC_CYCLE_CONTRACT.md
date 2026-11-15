# Contrato del ciclo académico: puntos 1–9

Fecha 2026-11-15; base cbe2989, rama codex/rbac-superadmin. Alcance autorizado:
implementar los nueve puntos para revisión funcional del usuario, sin reconstruir
roles, identidad global, consentimientos ni menú lateral.

## 1. Expediente institucional

Programas/cursos configurables por tenant (tipo, periodo, requisitos informativos).
Matrículas/expedientes por programa + estudiante global, fechas/estado/resultado.
ACADEMIC_VIEW/ACADEMIC_MANAGE, sin visibilidad cruzada. Crear/editar requiere vínculo
vigente; historial propio estudiantil se conserva al retirar consentimiento.

## 2–3. Solicitudes y aprobaciones

El estudiante vinculado solicita DIPLOMA/CERTIFICADO/TITULO/RECONOCIMIENTO/CONSTANCIA
desde su expediente. Documentos de requisitos privados y limitados; motivos/eventos.
SUBMITTED → IN_REVIEW → REVIEWED → APPROVED → FULFILLED; OBSERVED permite subsanar
y reenviar; REJECTED/WITHDRAWN terminales. El revisor puede observar/recomendar/rechazar.
El aprobador debe ser una persona diferente al revisor, incluso TENANT_ADMIN.
La aprobación crea un solo borrador vinculado; emitir cumple la solicitud.
Permisos REQUEST_VIEW/REQUEST_REVIEW/REQUEST_APPROVE. Acceso estudiantil solo propio.
El flujo anterior de emisión directa sigue disponible; solicitudes aprobadas no
pueden saltar sus reglas mediante endpoints de documentos existentes.

## 4–5. Plantillas, PDF, QR y almacenamiento

Plantillas versionadas, texto plano con variables permitidas, orientación, tipo y
requisitos; previsualización con marca MUESTRA. Versiones usadas no se reescriben.
TEMPLATE_VIEW/TEMPLATE_MANAGE; un borrador fija su versión. QR incrustado con código
aleatorio y URL configurada. Upload valida PDF real, tamaño/páginas y contenido
activo; no se confía en extensión/MIME. Emitidas/revocadas siguen inmutables.
Idempotency-Key opcional en emisión: misma clave no repite efectos; sin clave se
conserva rechazo histórico al replay. Hash e integridad comprobados al emitir/descargar.

Objetos nuevos registrados antes del I/O, claves nuevas, sin sobrescritura del
documento confirmado. Cola/reconciliación acotada solo para objetos gestionados por
esta versión, con revalidación de referencias y periodo de seguridad. No se borra
contenido arbitrario del bucket ni se presume transacción distribuida con MinIO.

## 6–7. Firma y verificación

Clave institucional PKCS12 RSA >=2048, certificado vigente y uso digitalSignature;
solo SIGNING_MANAGE puede configurarla. Clave privada cifrada con clave de entorno
independiente, nunca exportada por API. CREDENTIAL_SIGN firma PDF DRAFT con CMS
detached SHA-256/RSA y preserva bytes anteriores. Emitir verifica hash/firma.
Firma de prueba local opcional, deshabilitada por defecto fuera de configuración
explícita; certificado autofirmado y UI/verificador lo indican. No supone confianza
de una autoridad, validez legal, timestamp TSA, OCSP/CRL ni PAdES-LTV.

Consulta pública separa registro/revocación, habilitación actual del emisor,
integridad del archivo y firma. GET no afirma haber comprobado un archivo del
visitante. POST de archivo compara SHA-256 y comprueba CMS/cobertura/fingerprint
del firmante registrado, sin descargar claves ni acceder a URLs del PDF.
Rate limit y límites de upload; CI/documentos de identidad nunca son públicos.

## 8. Correcciones/sustituciones

Solicitud propia referida a credencial emitida; misma revisión/aprobación separada.
El borrador nuevo conserva el vínculo al original. Solo al emitir el reemplazo se
revoca y enlaza el anterior atómicamente en BD; sus bytes no se alteran. Motivo
público genérico, detalles privados en el trámite. No dos correcciones activas
simultáneas sobre el mismo documento; replay y acceso cruzado rechazados.

## 9. Identidad y ciclo de evidencia

Avatar opcional propio autenticado, independiente de imágenes institucionales.
Casos de disputa propios con evidencia y revisión SUPER_ADMIN: observación/rechazo
o corrección explícita del perfil reclamante si no colisiona con otra identidad.
No se fusionan cuentas ni se reasigna un documento ya ocupado de forma automática.
Toda resolución real es una decisión humana, con motivo, estados e historial.

Política configurable de retención de evidencia retirada/rechazada/obsoleta, con
metadatos de revisión preservados. Primero se retiran referencias en una transacción;
la cola elimina después los objetos sin referencias y reintenta fallos. Evidencia
pendiente o verificada vigente no se elimina. Contenido privado nuevo cifrado antes
de enviarlo a MinIO con clave independiente; no se anuncia como KMS/HSM.

## Compatibilidad y entrega

Migraciones nuevas V19 académico/trámites; V20 plantillas/documentos/firma/objetos;
V21 identidad/evidencia. V1–V18 intactas. Permisos nuevos solo se conceden implícitamente
a administradores tenant por su modelo existente; CUSTOM requiere asignación expresa.
SUPER_ADMIN no obtiene acceso automático a expedientes ni CI institucionales.

Antes de actualizar principal: suites backend PG16/Angular, pruebas negativas de
tenant/estudiante/permisos, concurrencia/transiciones/replay, QR decodificable, firma
real con certificado de prueba, archivo alterado rechazado, preservación del original,
retención/fallos MinIO y smoke integrado. Backup y claves privadas preservados.
Publicar rama/PR, manual de pruebas del usuario y handoff con límites comprobados.

Fuentes técnicas primarias: OpenPDF instalado 1.3.43 y su código/API;
https://github.com/LibrePDF/OpenPDF ; https://www.bouncycastle.org/download/bouncy-castle-java/
y https://github.com/zxing/zxing/releases (ZXing 3.5.4). No se cambia Angular/Spring.
