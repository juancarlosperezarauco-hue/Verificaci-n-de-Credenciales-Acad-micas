# Panel claro y limpieza de servicios

Base: 3604a3f. Rama: codex/rbac-superadmin. Alcance: presentación del panel y
mantenibilidad del backend, conservando los módulos y contratos del ciclo académico.

## Interfaz

- Cabecera clara y fija, identidad institucional, módulo actual, rol y acceso a
  seguridad de la cuenta. Menú lateral izquierdo desplegable conservado.
- Iconos SVG compartidos, enlaces con etiquetas y foco visible. Enlace para saltar
  directamente al contenido y soporte de movimiento reducido.
- Fondo claro, tipografía y tamaños consistentes, tarjetas discretas, formularios
  blancos y tablas con estados y acciones legibles.
- Resumen institucional orientado al trabajo: indicadores reales, accesos por
  permisos, últimos documentos, publicaciones y agenda. No se añadieron estadísticas
  ficticias ni se presentan errores de carga como un cero.
- Padrón con búsqueda por nombre/documento/país, búsqueda sin acentos, paginación
  local de diez registros, estados vacíos y reintento ante errores.
- Perfil/acceso del estudiante, imagen institucional y estilos públicos adaptados
  al esquema claro. El contenido y los pasos de registro se conservan.
- Contraste del texto sobre el color de cada institución. Al salir de la cuenta
  se cancela la carga pendiente del tema para evitar aplicar datos de otro tenant.
- Plantillas HTML, estilos CSS y lógica TypeScript separados en cabecera, menú,
  resumen y padrón. Iconos pequeños permanecen en un componente compartido.

## Backend

Se separan responsabilidades del servicio de credenciales:

| Servicio | Responsabilidad |
|---|---|
| CredentialService | Comandos: crear borradores, generar, firmar, emitir y revocar; consentimiento, revisión, bloqueo y auditoría. |
| CredentialDocumentService | Validación PDF, objetos nuevos gestionados, lectura y comprobación de bytes/hash. |
| CredentialVerificationService | Consultas públicas, expediente personal, estado del emisor y comparación del archivo/firma. |

La fachada conserva los métodos usados por los controladores. DocumentCheck pasa
al contenedor de DTO; los campos JSON se conservan. Se eliminan rutas/variables
muertas y duplicación de creación de borradores. La marca temporal de firma utiliza
el Clock ya configurado por la aplicación.

IdentityService y ConsentService declaran todas sus dependencias en el constructor,
incluido el publicador de eventos; no dependen de inyección mutable de ese campo.
Se conserva el orden de bloqueo estudiante → credencial, la política de aprobación,
idempotencia, aislamiento, cifrado, firmas y sustituciones. No se cambian endpoints,
roles, permisos ni migraciones V1–V21.

El formato de 63 clases de los módulos académicos/documentales se puede comprobar:

    ./scripts/format-academic-backend.ps1 -Check

Sin -Check aplica el formato. Usa Google Java Format 2.0.0, descargado de su
[repositorio oficial](https://github.com/google/google-java-format), en una caché
local ignorada por Git. No añade dependencias de producción.
La interfaz modificada se formateó con Prettier 3.6.2 y la configuración ya presente
en frontend/package.json; no se instaló una dependencia en el node_modules compartido.

## Validación

- Maven verify: 120 pruebas correctas en PostgreSQL 16.15, Flyway V1–V21 y
  Hibernate validate; sin fallos, errores u omitidas.
- Angular: 99 pruebas correctas y build de producción sin advertencias de presupuesto.
- Total distinto: 219. Once pruebas web nuevas cubren permisos, datos parcialmente
  disponibles, fallos de publicación/eliminación, búsqueda/páginas y límites del tema.
- Verificación de formato de las 63 clases correcta.
- Evidencia local ignorada: backend/polish-tests.log, frontend/polish-tests.log,
  frontend/polish-build.log, polish-smoke.log.

La herramienta visual sigue sin iniciar por un error de kernel assets; no se
afirma una inspección visual automatizada. El preview local y las pruebas de
componentes permiten al usuario revisar el diseño en escritorio y móvil.

## Recursos locales

La VM Docker tiene aproximadamente 3,5 GiB. Al ejecutar auditoría y los sistemas
principales juntos se observó memoria disponible casi agotada y pérdida de
respuesta del motor. Se limita únicamente compose.audit.yml:

- Backend: heap Java de 320 MiB, límite de contenedor 768 MiB.
- PostgreSQL: límite 256 MiB, shared_buffers 32 MiB y 30 conexiones.
- MinIO: límite 256 MiB y objetivo de memoria Go 192 MiB.

Estos límites son del entorno de prueba. No se modifican las claves, los volúmenes,
los datos ni la configuración de memoria de los otros proyectos.
La instancia de auditoría debe detenerse al finalizar.

El smoke HTTP completo de este incremento no se pudo cerrar: el motor perdió
respuesta por presión de memoria al ejecutar ambos entornos. No se cuenta como
aprobado. Los 120 casos PostgreSQL y los 99 casos de interfaz sí están aprobados.
Para repetir el smoke en este equipo, ejecutar auditoría con los servicios
principales propios detenidos o utilizar un equipo con más memoria.

## Instalación comprobada

Principal actualizado y comprobado por HTTP: web localhost:4300 y API 18080 UP.
El bundle servido es main-XYRD3N2Y.js y el SHA-256 del JAR dentro del contenedor
coincide con el artefacto de Maven verificado:
ce14d7ded800cc8a9f09487fd1e8fd0c2b67bb9e2b22e7e6da1e7c19f37cd39c.
Las cantidades de organizaciones, estudiantes, credenciales y consentimientos
se conservan; el esquema sigue en V21.

Se reutilizaron las imágenes ya construidas al fallar el estado de BuildKit
después del reinicio; se verificaron el JAR y el bundle de la instalación final.
El override privado del equipo limita el heap del backend principal a 512 MiB;
se guardó una copia del override previo. Los contenedores de pruebas quedan
detenidos y los servicios principales y HRcore restablecidos.

Commits de implementación: ebe3d27 (formato), 3502362 (servicios) y
fbe1bf7 (interfaz). El cierre documental queda en HEAD de la rama.
