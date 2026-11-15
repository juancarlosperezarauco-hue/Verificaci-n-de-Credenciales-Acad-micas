# Ciclo académico: puntos 1–9

Rama codex/rbac-superadmin; base cbe2989. Contrato previo:
[ACADEMIC_CYCLE_CONTRACT.md](ACADEMIC_CYCLE_CONTRACT.md).
Alcance: piloto local funcional. Blockchain real, OCR/IA, Android, pagos y
preparación de producción siguen fuera de estos nueve puntos.

## Funciones entregadas

| Punto | Función |
|---|---|
| 1 | Programas por periodo, requisitos informativos, matrícula, resultado, observaciones y estado. Expediente personal entre instituciones sin duplicar la identidad global. |
| 2 | Solicitudes, selección de plantilla, hasta cinco requisitos PDF/JPEG/PNG de 5 MiB, historial y subsanación. |
| 3 | Revisión asignada y aprobación por otra persona, incluso siendo administrador. La aprobación crea un borrador; la emisión completa la solicitud. |
| 4 | Plantillas de texto/orientación, versiones inmutables, variables permitidas, archivo/activación y muestra PDF descargable. |
| 5 | Análisis real de PDF hasta 10 MiB/30 páginas, rechazo de contenido activo, QR embebido al generar PDF, hash de bytes, Idempotency-Key y reconciliación de objetos gestionados. |
| 6 | Importación de PKCS12 RSA, clave privada cifrada, firma CMS SHA256withRSA y política de firma obligatoria. Certificado local de prueba marcado en el PDF y la interfaz. |
| 7 | Verificador con registro, estado del emisor, comparación del archivo y firma separados. Consultar un código no comprueba un PDF. |
| 8 | Corrección por el titular, nueva revisión y reemplazo. Solo al emitir se revoca y enlaza el anterior, conservando sus bytes. |
| 9 | Avatar privado opcional, disputas de identidad revisadas por SUPER_ADMIN, corrección humana que cierra sesiones y conservación/borrado seguro de evidencia vencida. |

Las operaciones institucionales nuevas requieren vínculo/consentimiento activo.
Retirarlo oculta sus expedientes y solicitudes en las bandejas de esa institución;
el titular conserva su historial. Cambiar el perfil invalida las solicitudes
aprobadas sobre su revisión anterior: retirar y presentar una nueva.
Los trabajadores del tenant siguen separados de STUDENT.

Los requisitos se revisan por una persona. No hay comprobación automática de
notas, elegibilidad, registros ministeriales o identidad gubernamental.
Las plantillas no son un editor gráfico libre. Un PDF propio se comprueba y puede
firmarse; el QR se incorpora al generar desde una plantilla de la plataforma.

## Prueba manual

Web principal: http://localhost:4300. API: http://localhost:18080.
Correo local: http://localhost:8125. Usa el menú lateral izquierdo y cuentas
distintas, en perfiles de navegador separados o cerrando sesión.

1. Registra estudiante, confirma correo desde Mailpit y completa perfil.
   Registra institución y apruébala como SUPER_ADMIN. El TENANT_ADMIN pega
   el código EST del estudiante en Estudiantes para vincularlo.
2. En Equipo y roles crea un revisor con REQUEST_VIEW y REQUEST_REVIEW y su
   usuario. TENANT_ADMIN aprobará; alternativamente delega REQUEST_APPROVE
   a un segundo trabajador. Deben ser personas con cuentas distintas.
3. En Expedientes académicos crea un programa/periodo y matrícula al estudiante.
   Edita resultado y finalización. En su cuenta comprueba programa y observaciones.
4. Crea una plantilla, descarga su muestra y copia una segunda versión.
   Ejemplo de texto: Por {{documentTitle}}, correspondiente a {{programName}}.
   La versión anterior debe permanecer.
5. Como estudiante, en Expediente académico selecciona el programa, solicita
   certificado y adjunta un requisito ficticio. Comprueba Mis solicitudes.
   Intenta duplicar una solicitud mientras siga abierta.
6. Como revisor inicia y observa con motivo. El estudiante corrige y reenvía.
   El revisor inicia de nuevo y termina su revisión. No debe poder aprobarla;
   el administrador sí puede hacerlo y aparece un borrador en Credenciales.
7. En Firma institucional importa un PKCS12 autorizado o acepta generar
   un certificado de prueba local. Activa firma obligatoria. Genera y descarga
   el PDF del borrador; revisa datos, texto y QR. Emitir sin firma debe fallar.
   Firma, emite y descarga desde el expediente estudiantil: mismos bytes.
8. Abre código/QR sin sesión: registro emitido y archivo sin comprobar.
   Carga el PDF: huella coincidente y firma íntegra. Otro PDF debe mostrar
   que no coincide. Una firma autofirmada mantiene la advertencia PRUEBA.
9. En Expediente académico cambia a Corrección de credencial, elige la del
   mismo emisor, explica el error y repite revisión/emisión. El código anterior
   queda revocado con enlace al nuevo; su descarga conserva los bytes originales.
10. En Foto y asistencia de identidad sube y quita avatar. Abre caso con imágenes
    ficticias. SUPER_ADMIN observa, estudiante subsana y SUPER_ADMIN decide.
    Una corrección de perfil cierra sus sesiones. Un CI ocupado por otra cuenta
    debe rechazarse sin transferir datos.
11. Usa otra institución con el mismo estudiante: expedientes, solicitudes,
    roles, plantillas y firma separados. Retira consentimiento y comprueba
    que la primera ya no pueda revisar sus solicitudes.
12. Prueba PDF falso/grande, plantilla incompatible, autoaprobación, doble clic
    y sesión vencida. Registra rol, módulo, acción, hora, resultado esperado y
    mensaje observado. Usa datos ficticios y evita compartir claves o CI reales.

Los roles personalizados existentes no reciben permisos nuevos automáticamente.
El administrador los asigna. Matrícula web usa ACADEMIC_VIEW/MANAGE y STUDENT_VIEW;
trámites usan REQUEST_VIEW y REVIEW o APPROVE. Emisión delega CREDENTIAL_VIEW,
CREATE, SIGN e ISSUE según responsabilidad. TEMPLATE_VIEW/MANAGE y
SIGNING_MANAGE se administran de forma independiente.

## Conservación, claves y firma

Archivos nuevos: EVIDENCE_ENCRYPTION_KEY independiente de JWT, MFA, correo y
SIGNING_ENCRYPTION_KEY. Se leen objetos históricos sin reescritura silenciosa.
La firma histórica usa su huella registrada aunque después cambie el firmante.

No se valida confianza de CA, OCSP/CRL, TSA, PAdES-LTV ni validez legal.
Antes de usar certificados reales se deben definir autorización, custodia,
cadena de confianza, revocación y sellado de tiempo. El servidor necesita
la clave del almacén para usar las claves privadas cifradas de cada institución.

No regeneres claves de un entorno existente. Respalda PostgreSQL, objetos,
.env y claves de forma coordinada y privada. EVIDENCE_KEY_ID y
EVIDENCE_PREVIOUS_KEYS permiten recifrar por lotes; conserva claves anteriores
durante el recifrado y la vida de los backups. La clave del almacén de firma
no tiene rotación automática en esta entrega.

Retención predeterminada: 90 días desde retirada, rechazo, cambio de
perfil/consentimiento o cierre del caso. Se conservan hashes/metadatos/historial.
La revisión vigente/autorizada y el caso abierto están protegidos. V21 inicia
un plazo conservador para evidencia histórica V18; migrar no elimina archivos.

SUPER_ADMIN, en Archivos y conservación, puede retirar referencias vencidas,
limpiar hasta 50 objetos sin referencias o recifrar hasta 50 archivos. La retirada
confirma su transacción antes del borrado físico. Se revalidan referencias y
se reintentan fallos de MinIO. Solo se consideran objetos gestionados o rutas
históricas de identidad estrictamente identificadas; no hay barrido del bucket.
Limpieza automática desactivada por defecto y en el piloto:
EVIDENCE_CLEANUP_ENABLED=false.
Las disputas no fusionan cuentas ni transfieren CI, certificados o matrículas.

## Validación

Maven verify: 120 pruebas correctas en PostgreSQL 16.15, Flyway V1–V21 y
Hibernate validate. Perfil H2: 119 correctas y una omitida del trigger exclusivo
de PostgreSQL. Angular: 88 pruebas correctas y build de producción. Total distinto: 208.
Logs y credenciales ficticias locales están ignorados por Git:

Smoke real aprobado por nginx con PostgreSQL/MinIO/Mailpit: flujo institucional
previo y nueve puntos, requisitos privados, firma CMS comprobada sobre los bytes,
alteración detectada, descarga propia, reemplazo conservando el PDF anterior,
avatar y observación/subsanación/corrección de disputa. Cargas mayores a 1 MiB.
Upgrade de la instancia aislada V18 a V21 comprobado.
Se hizo copia PostgreSQL del principal y configuración/claves privadas;
pg_restore -l enumeró el archivo. Esto no es un ensayo de restauración integral.
Principal actualizado: API 18080 UP, frontend 4300 con el bundle de producción
comprobado y esquema V21. Las cantidades de organizaciones, estudiantes,
credenciales y consentimientos coinciden con las previas. No se cargaron los
fixtures de auditoría en el principal. El mantenimiento SUPER_ADMIN confirma
el cifrado configurado. Auditoría y PostgreSQL de pruebas quedan detenidos.

- backend/cycle-full-pg.log: PostgreSQL, Flyway V1–V21 y Hibernate validate.
- backend/cycle-full-h2.log: compatibilidad del perfil test local.
- frontend/cycle-tests.log y cycle-build.log: ChromeHeadless y build Angular.
- scripts/smoke-academic-cycle.ps1: HTTP real con nginx, PostgreSQL, MinIO
  y Mailpit, restringido a la instancia de auditoría.

Las herramientas de navegador/escritorio no iniciaron por un fallo de kernel
assets. Las pruebas de componentes incluyen menú izquierdo, permisos,
formularios y confirmaciones en ChromeHeadless. No sustituyen una revisión
visual manual del diseño en escritorio y móvil.
