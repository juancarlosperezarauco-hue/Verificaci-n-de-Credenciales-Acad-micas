# Product Backlog V3 — estado basado en código

Fecha: 2026-11-13. Fuente: prompt maestro adjunto, auditoría de `0f72612` y el
incremento de integridad documental de esta entrega. Evidencias: `AUDIT_V3.md`,
`VALIDATION_V3.md`, contratos y pruebas del repositorio.

Continuación prioritaria: SEC-01 y SEC-02 implementadas, V14 añadida. Detalle y
límites actuales en PRIORITY_01_DELIVERY.md; el backlog conserva tareas parciales
cuando aún faltan requisitos del módulo completo.

Estados: **Verificada** = alcance de esa tarea comprobado; **Parcial** = hay base
real pero falta alcance; **Pendiente** = no implementada. No hay tarea declarada
bloqueada por una dependencia interna pendiente: la dependencia se muestra aparte.
Las integraciones externas, certificados de firma y publicación Android requerirán
especificaciones/accesos autorizados; su diseño local puede avanzar sin inventarlos.

## Matriz de las 40 tareas

| ID | Nombre | Módulo | Implementación existente | Estado | Dependencias | Riesgo | Prioridad | Próxima acción |
|---|---|---|---|---|---|---|---|---|
| T01 | Auditar arquitectura | Todos | Inventario de paquetes, clientes y contratos en AUDIT_V3. | Verificada | — | Bajo | P0 | Actualizar con cada incremento. |
| T02 | PostgreSQL/Flyway | Transversal | V1–V14 instaladas/validadas; DER conservado, security_version añadido. | Verificada | T01 | Medio | P0 | Mantener migraciones históricas; nuevas solo si necesarias. |
| T03 | Pruebas existentes | Transversal | Baseline reproducido: 11 backend + 24 web; builds correctos. | Verificada | T02 | Bajo | P0 | Conservar baseline y resultados nuevos separados. |
| T04 | Inventario funcional | Todos | 18 módulos y matriz de 40 tareas revisados. | Verificada | T01–03 | Bajo | P0 | Usar esta matriz como estado, no dependencias instaladas. |
| T05 | Aislamiento tenant | M02/M03/M04/M08 | Filtros tenant, FK compuestas RBAC y pruebas de acceso cruzado; incremento documental. | Parcial | T01–03 | Alto | P0 | Completar matriz de recursos/acciones, archivos e historial. |
| T06 | Roles/permisos | M02 | SUPER_ADMIN sin tenant, TENANT_ADMIN, CUSTOM, catálogo, asignación y revocación inmediata. | Verificada | T05 base | Medio | P0 | Añadir permisos nuevos con cada módulo; no delegar administración protegida. |
| T07 | Autenticación | M02 | JWT/BCrypt, onboarding restringido, correo/reset/cambio de credenciales/desactivación y cierre de sesiones por versión. | Parcial | T06 | Alto | P0 | MFA, límites de intentos, sesiones individuales y entrega durable de correo. |
| T08 | Identidad global | M04 | Perfil global, unicidad, correo confirmado, cuenta activa/desactivada y preservación histórica. | Parcial | T07 | Alto | P1 | Revisión documental y procedimiento de disputa de identidades. |
| T09 | Vinculación | M04 | Código EST, historial, retiro/rotación; desactivar retira accesos y bloquea nuevos canjes sin borrar historia. | Parcial | T08 | Alto | P1 | Reforzar consentimiento explícito y prevenir exposición/abuso del buscador. |
| T10 | Expediente institucional | M03/M04/M05 | Identidad y vínculos; entidad students histórica conservada. | Parcial | T09 | Alto | P1 | Agregar expediente tenant y estructuras académicas configurables. |
| T11 | Solicitudes | M06 | No hay agregado de solicitudes/requisitos. | Pendiente | T10 | Medio | P1 | Primer trámite con estados, propietario y adjuntos autorizados. |
| T12 | Aprobaciones | M09 | Solo permiso de emisión directa. | Pendiente | T11/T06 | Alto | P1 | Definir revisión/autoridad y separación de funciones. |
| T13 | Plantillas | M07 | Generador fijo con branding y datos de firmante. | Parcial | T10 | Medio | P1 | Plantilla versionada mínima con campos permitidos y vista previa. |
| T14 | PDF/QR e integridad | M08 | Generación, upload, hash, MinIO; H01/H02 corregidos en este incremento. | Parcial | T05/T09/T13 | Alto | P1/P0 H01 | Validar archivos, QR dentro de PDF, idempotencia y reconciliación. |
| T15 | Firma digital | M08/M09 | Nombre/cargo impresos, sin firma criptográfica. | Pendiente | T12/T14 | Alto | P1 | Contrato de firma, firmante autorizado, claves y validación con certificado de prueba. |
| T16 | Verificación pública | M13 | Consulta de registro y revocación por código; QR generado en web. | Parcial | T14/T15 | Alto | P1 | Separar estado registral, integridad, firma y autorización del emisor. |
| T17 | Revocación/corrección | M10 | Estado REVOKED, fecha y motivo; visible públicamente. | Parcial | T12/T16/T26 | Alto | P1 | Historial de eventos y sustitución sin reescribir el documento emitido. |
| T18 | Decisión blockchain | M11 | Puerto desacoplado, sin ADR de red. | Pendiente | T14–17 | Alto | P2 | Comparar EVM/Fabric, privacidad, gobernanza y operación en ADR. |
| T19 | Integración blockchain | M11 | Adaptador de desarrollo devuelve pending, sin red. | Parcial | T18 | Alto | P2 | Adaptador real local/testnet con outbox, confirmaciones y reintentos. |
| T20 | OCR | M12 | Servicio Python de resumen no extrae documentos. | Pendiente | T14/T07 | Alto | P2 | Extracción local de documento de prueba con permisos y revisión humana. |
| T21 | Anomalías | M12 | Validaciones básicas de formularios y unicidad. | Parcial | T10/T14 | Alto | P2 | Reglas determinísticas explicables, resultados versionados y pruebas. |
| T22 | Panel de anomalías | M12 | Sin alertas persistidas/revisión. | Pendiente | T21/T26 | Medio | P2 | Bandeja tenant con evidencias y decisión humana auditada. |
| T23 | Dashboard global | M15 | Listado/detalle de organizaciones; no indicadores globales. | Pendiente | T26 | Medio | P2 | Agregaciones globales autorizadas, sin datos personales innecesarios. |
| T24 | Dashboard tenant | M15 | Contadores locales y publicaciones/fechas del portal. | Parcial | T10/T14 | Medio | P2 | Agregaciones servidor por periodo/tenant y gráficos reales. |
| T25 | Reportes | M15 | Permiso REPORT_VIEW y OpenPDF, sin reportes/exportación. | Pendiente | T24/T26 | Alto | P2 | Reporte filtrado PDF; justificar dependencia Excel cuando corresponda. |
| T26 | Auditoría | M14 | Eventos de estado institucional y canjes con actor/fecha. | Parcial | T06 | Alto | P1 | Evento transversal de seguridad con acceso por ámbito y sin secretos. |
| T27 | Notificaciones | M16 | Correos de seguridad operativos; sin bandeja ni avisos académicos. | Pendiente | T26/T11/T14 | Medio | P2 | Persistencia, bandeja personal y entrega académica con reintentos. |
| T28 | Base Flutter | M17 | Sin proyecto móvil. | Pendiente | T07/T16 | Medio | P3 | Scaffold Flutter con navegación y configuración de API, sin duplicar backend. |
| T29 | Integración Android | M17 | API web reutilizable; cliente no existe. | Pendiente | T28/T07/T09 | Alto | P3 | Login y expediente real con almacenamiento seguro de sesión. |
| T30 | Android avanzado | M17 | Sin escáner/descargas/push/offline móvil. | Pendiente | T29/T27 | Alto | P3 | Escáner y descarga; offline siempre indica vigencia no actualizada. |
| T31 | APK/AAB | M17 | Sin compilación ni claves de publicación. | Pendiente | T30 | Alto | P3 | Preparar build reproducible y firma autorizada fuera de Git. |
| T32 | Portal Angular | M01 | Landing clara, registro modular y verificador. | Parcial | T07/T16 | Medio | P2 | Accesibilidad y errores funcionales; conservar diseño claro y menú horizontal. |
| T33 | Paneles Angular | M02/M03/M15 | Organizaciones, equipo, roles, perfil y pantallas base. | Parcial | T06/T23/T24 | Medio | P2 | Paginación, filtros, errores y accesos consistentes; no crear paneles vacíos. |
| T34 | Docker | M18 | Compose local operativo, volúmenes y builds multietapa/prebuilt. | Parcial | T02/T03 | Alto | P0/P3 | Health checks de todos los servicios y perfiles de despliegue sin fallbacks inseguros. |
| T35 | CI/CD | M18 | No existe .github/workflows. | Pendiente | T03/T34 | Alto | P3 | CI de build y pruebas PG; mantener despliegue manual hasta preparar producción. |
| T36 | Backups | M18 | Backup y restauración PostgreSQL manual previos; MinIO sin plan integral. | Parcial | T34 | Alto | P0/P3 | Ensayo coordinado PG+objetos en entorno separado, con inventario y checksums. |
| T37 | Producción | M18 | Entorno local HTTP y configuración de desarrollo. | Pendiente | T07/T15/T26/T34–36 | Alto | P3 | Checklist verificado de HTTPS, secretos, monitoreo, restauración y rollback. |
| T38 | Pruebas adicionales | Transversal | Pruebas existentes de permisos/consentimientos/flujo; cinco nuevas de integridad. | Parcial | T05/T14 | Medio | P0 transversal | Expandir cobertura por riesgo, sin repetir pruebas sin motivo. |
| T39 | E2E | Transversal | Smoke HTTP y QA visual previos; sin suite navegador automatizada completa. | Parcial | T11–17/T38 | Medio | P1 transversal | Flujos navegador reales y autorización negativa; no confundir MockMvc con E2E. |
| T40 | Piloto/aceptación | Todos | Sin piloto institucional real documentado. | Pendiente | Núcleo P0/P1/T36/T39 | Alto | P3 | Acordar institución, datos autorizados y escenarios de aceptación. |

T06 se considera verificada para el modelo RBAC actual. Las ampliaciones del
superadmin (parámetros globales, integraciones, indicadores y designación de nuevos
administradores) pertenecen a otras tareas pendientes, no quedan implícitamente hechas.

## Historias iniciales por tarea

Cada fila conserva la épica y prioridad de la matriz. El nombre/descripción y el
beneficio se expresan en la historia; el estado y las dependencias heredan los de
su tarea salvo la excepción indicada. Las estimaciones son **por refinar** con el
alcance técnico de cada historia; no se inventa velocidad, calendario ni capacidad.
Las tareas amplias necesitarán más historias antes de entrar en un sprint.

Resumen de la matriz: **5 verificadas, 19 parciales y 16 pendientes**. No es un
porcentaje de terminación del producto: las tareas difieren en tamaño y alcance.

| Historia | Rol beneficiario y valor funcional | Criterio de aceptación y prueba |
|---|---|---|
| T01.1 | Como mantenedor, necesito reconocer la arquitectura para continuar sin reconstruirla. | Paquetes/contratos y límites documentados con evidencia de rutas y clases. Verificada. |
| T02.1 | Como mantenedor, necesito instalar y validar el esquema real. | PG16 vacío aplica V1–V13, Hibernate validate pasa y DER refleja restricciones. Verificada. |
| T03.1 | Como mantenedor, necesito reproducir el baseline antes de cambiar código. | verify y build web pasan; informe separa 11+24 anteriores y pruebas nuevas. Verificada. |
| T04.1 | Como responsable del producto, necesito saber qué falta realmente. | 18 módulos y 40 tareas clasificados sin llamar real a adaptadores simulados. Verificada. |
| T05.1 | Como emisor, necesito que otro tenant no cambie ni lea mis archivos. | Pruebas negativas de upload/download no invocan almacenamiento; cubierto por este incremento. |
| T06.1 | Como TENANT_ADMIN, necesito asignar roles propios sin escalar a SUPER_ADMIN. | Roles ajenos/permiso desconocido rechazados; JWT vigente pierde permisos revocados. Verificada. |
| T07.1 | Como estudiante, necesito recuperar acceso sin revelar si mi correo existe. | Respuesta uniforme, token hasheado de uso único con vencimiento, replay y cierre de sesiones. Implementada. |
| T08.1 | Como estudiante, necesito que mi cuenta desactivada pierda acceso. | Login/JWT rechazan active=false, se retiran vínculos y se conserva historia. Implementada. |
| T09.1 | Como estudiante, necesito comprender y controlar la vinculación. | Consentimiento visible, alcance mínimo, historial y retiro bloquean nuevos accesos/emisiones sin borrar credenciales. |
| T10.1 | Como TENANT_ADMIN, necesito una unidad académica que se adapte a mi organización. | Nombre/tipo configurables y CRUD limitado al tenant, sin obligar a usar facultades. |
| T11.1 | Como estudiante vinculado, necesito solicitar un certificado. | Solicitud inicial con requisitos y consulta propia; tenant ajeno/otro estudiante no pueden verla. |
| T12.1 | Como autoridad, necesito aprobar solicitudes sin autoaprobar preparación propia cuando la regla lo prohíba. | Transiciones válidas y separación de funciones verificadas mediante pruebas negativas. |
| T13.1 | Como gestor, necesito previsualizar una plantilla versionada. | Vista previa con datos de prueba y versión inmutable al usarse en emisión; campos arbitrarios rechazados. |
| T14.1 | Como titular, necesito conservar los bytes de una credencial emitida. | Cambios terminales no escriben, fallo externo no pisa objeto vigente y carga/emisión concurrente mantienen hash. Implementada en este incremento. |
| T15.1 | Como verificador, necesito validar una firma criptográfica de prueba. | Firma verificable con clave autorizada; PDF modificado falla; la clave privada nunca se publica. |
| T16.1 | Como verificador, necesito distinguir registro, revocación e integridad. | API/UI separan resultados y muestran emisor no habilitado sin declarar automáticamente fraude. |
| T17.1 | Como autoridad, necesito revocar conservando historia. | Evento con actor/motivo, permisos, estado público actualizado y documento original intacto. |
| T18.1 | Como operador técnico, necesito una decisión blockchain sostenible. | ADR compara alternativas, PII, costos, fallos y recuperación; no da por desplegada ninguna red. |
| T19.1 | Como emisor, necesito registrar una prueba criptográfica con reintentos. | Red local/testnet confirma hash sin PII; estado pendiente/fallo explícito y envío duplicado controlado. |
| T20.1 | Como revisor, necesito extraer campos de un documento autorizado. | OCR sobre fixture conocido, confianza/evidencia y corrección humana; sin decidir fraude. |
| T21.1 | Como revisor, necesito una alerta explicable por inconsistencia. | Regla versionada detecta fixture inconsistente y no altera automáticamente credenciales. |
| T22.1 | Como revisor tenant, necesito resolver una alerta de mi organización. | Bandeja con evidencias y decisión auditada; IDs de otro tenant denegados. |
| T23.1 | Como SUPER_ADMIN, necesito contar organizaciones por estado. | Endpoint agregado global restringido a superadmin y gráfico basado en datos reales. |
| T24.1 | Como TENANT_ADMIN, necesito emisiones por periodo de mi institución. | Agregación de servidor con fechas/filtros y prueba con dos tenants. |
| T25.1 | Como auditor autorizado, necesito exportar el mismo conjunto filtrado que consulto. | PDF/Excel coinciden con filtros, respetan ámbito y no ejecutan fórmulas inyectadas. |
| T26.1 | Como auditor, necesito reconstruir cambios críticos sin secretos. | Actor, ámbito, acción, recurso y fecha; permisos por tenant/global y ausencia de tokens/contraseñas. |
| T27.1 | Como estudiante, necesito leer un aviso de emisión propio. | Bandeja persistida, marcar leído solo propietario y correo con reintentos sin duplicados. |
| T28.1 | Como estudiante, necesito una base Android que use la plataforma existente. | Flutter arranca con navegación y API configurable sin secretos ni base principal duplicada. |
| T29.1 | Como estudiante, necesito entrar desde Android y consultar mi expediente. | API real, sesión protegida, logout y aislamiento de cuentas probados. |
| T30.1 | Como verificador móvil, necesito escanear un QR admitido. | Valida formato/enlace y consulta API; sin conexión indica imposibilidad de confirmar vigencia actual. |
| T31.1 | Como responsable de publicación, necesito generar artefactos Android trazables. | APK/AAB del mismo commit, firma fuera de Git, instrucciones y pruebas de instalación. |
| T32.1 | Como visitante, necesito completar formularios con teclado y leer errores. | Labels, foco y errores accesibles; escritorio/móvil; se conserva diseño claro. |
| T33.1 | Como trabajador, necesito ver solo las acciones que puedo ejecutar. | Menús/tablas y API coinciden en permisos, con estado vacío/error y paginación real. |
| T34.1 | Como operador, necesito distinguir proceso iniciado de servicio listo. | Health checks de API/objetos/web y dependencias correctas, sin borrar volúmenes. |
| T35.1 | Como mantenedor, necesito detectar regresiones antes de integrar. | Workflow ejecuta PG/Flyway, backend y web; falla ante una prueba roja y no despliega producción. |
| T36.1 | Como operador, necesito restaurar documentos junto a sus registros. | Ensayo en DB/bucket separados; todo objeto referenciado existe y coincide su hash. |
| T37.1 | Como operador, necesito configuración productiva que falle si faltan secretos. | HTTPS y configuración explícita, rollback y restauración ensayados; sin credenciales de desarrollo. |
| T38.1 | Como mantenedor, necesito reproducir la regresión de integridad. | Nueva prueba falla antes del arreglo y pasa después, junto al baseline. Implementada. |
| T39.1 | Como institución, necesito validar el recorrido completo desde navegador. | Registro, revisión, usuarios, vínculo, emisión/verificación/revocación con capturas y datos aislados. |
| T40.1 | Como responsable institucional, necesito aceptar un piloto con evidencia. | Escenarios acordados ejecutados, incidencias registradas y aceptación explícita de la institución. |

## Cobertura adicional que no debe perderse

- **T10.2:** expediente académico por vínculo, con datos propios del tenant y
  acceso separado de la identidad global; prueba de estudiante compartido entre A/B.
- **T10.3 / M05:** contrato de integración académica y adaptador de prueba, sin
  conexión a sistemas reales hasta obtener autorización/especificación. El backlog
  original no le asigna una tarea independiente; se conserva dentro de T10.
- **T07.2–T07.5:** correo confirmado/cambio de correo, sesiones revocables,
  MFA administrativo y límites de intentos, desarrollados como incrementos distintos.
- **T14.2–T14.4:** validación de archivos, QR incrustado e idempotencia/outbox.
- **T26.2:** eventos de login/roles/usuarios/emisión/consulta; historial de estados
  institucionales ya existente se reutiliza y no se reemplaza.
- Documentación adicional del prompt (manuales, diccionario completo, diagramas de
  secuencia/actividad, plan ASVS/MASVS y operación) se entrega con su módulo, no
  como documentos que anuncien funcionalidades todavía inexistentes.

## Orden de ejecución recomendado

1. Cerrar P0 de integridad, aislamiento, seguridad de cuenta y onboarding restringido.
2. Completar vínculos/expediente, solicitudes/aprobaciones, plantillas, firma y auditoría.
3. Integrar notificaciones/reportes, luego blockchain y OCR/anomalías con revisión humana.
4. Android sobre API estable, CI y recuperación en paralelo al núcleo; producción/piloto
   solo con sus criterios verificados. No se asignan fechas sin capacidad acordada.
