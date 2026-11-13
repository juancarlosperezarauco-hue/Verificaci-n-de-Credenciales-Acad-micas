# Sprint 01 V3 — integridad y acceso seguro

## Objetivo

Conservar documentos emitidos y cerrar los controles de acceso prioritarios antes
de ampliar el ciclo académico. Se mantiene el monolito modular, la identidad global
y los contratos existentes. Sin fechas ni velocidad inventadas.

## Incremento implementado: T14.1 / T05.1 / T38.1

Como titular de una credencial, necesito que un intento rechazado no modifique su
documento. Como institución, necesito impedir el acceso de otro tenant y coordinar
las acciones simultáneas sobre una credencial.

- **Prioridad:** P0, por integridad de datos.
- **Módulos:** M08, M10 y autorización transversal.
- **Estado:** implementado y probado; forma parte de T14/T05/T38, que siguen parciales.
- **Dependencias:** modelo y permisos existentes; no añade librerías ni migraciones.
- **Estimación:** incremento acotado a servicio/repositorio y pruebas; no se asignan
  puntos retrospectivos.

Criterios comprobados:

1. Emitidas/revocadas rechazan adjuntos antes de cualquier escritura de almacenamiento.
2. Otro tenant, un estudiante o un lector sin CREDENTIAL_CREATE no pueden reemplazarlos.
3. Un fallo después de escribir deja el objeto previamente confirmado intacto.
4. Carga y emisión concurrentes mantienen un único documento/hash coherente.
5. Una emisión repetida rechazada no vuelve a llamar al adaptador externo.
6. El baseline sigue pasando y Flyway valida V1–V13.

Subtareas completadas: documentar contrato; reproducir prueba roja; bloqueo de fila
tenant/id para mutaciones; rechazo previo; clave nueva por carga/generación;
cinco regresiones; suite completa y prueba de almacenamiento real (ver VALIDATION_V3).

Entregables: `CREDENTIAL_DOCUMENT_CONTRACT.md`, cambios en `CredentialService` y
`CredentialRepository`, `CredentialDocumentIntegrityIntegrationTests` y evidencias.

Límites: los objetos nuevos sin referencia después de un fallo necesitan una futura
reconciliación. El adaptador blockchain continúa en desarrollo; no se implementa
firma criptográfica. No hay cambios visuales en este incremento.

## Historias siguientes del sprint: planificadas, no implementadas

### SEC-01 — Onboarding institucional restringido (T07 / M03)

Como solicitante, necesito completar mi organización mientras se revisa, sin poder
emitir credenciales antes de la aprobación del SUPER_ADMIN.

- **Valor:** evita que un alta pública se habilite como emisor automáticamente.
- **Prioridad:** P0. **Estado:** pendiente. **Estimación:** por refinar tras contrato.
- **Dependencias:** definir acceso de onboarding para PENDING_REVIEW/OBSERVED y
  política de compatibilidad de `/api/auth/register`; conservar organizaciones existentes.
- **Subtareas:** contrato documentado primero; pruebas de las dos rutas de alta;
  sesión restringida; transición auditada y UI con estados/observaciones.
- **Aceptación:** alta nueva pendiente, puede completar/corregir perfil, no puede
  emitir; superadmin aprueba; acceso operativo se habilita sin crear otra identidad;
  suspensión/rechazo bloquean acciones y el token anterior no conserva privilegios.
- **Pruebas:** dos tenants, dos rutas de alta, transición permitida/denegada,
  usuario CUSTOM y estudiante; migración solo si los datos nuevos la requieren.
- **Entrega:** API y pantallas funcionales, contrato y resultados verificables.

No basta cambiar `active=true` por false: el login actual también quedaría bloqueado
y se impediría al representante corregir observaciones. Esta historia requiere un
flujo restringido completo, no un cambio aislado del booleano.

### SEC-02 — Estado activo y recuperación estudiantil (T07.1 / T08.1)

Como estudiante, necesito recuperar mi cuenta y que una cuenta desactivada pierda
acceso, manteniendo la identidad global y mis credenciales históricas.

- **Prioridad:** P0. **Estado:** pendiente. **Estimación:** por refinar.
- **Dependencias:** revisar, sin sobrescribir, los cambios locales de seguridad
  de estudiante en el otro checkout; V9/V10 y contratos existentes.
- **Subtareas:** comparar parche/entidades con V9, proteger estado activo en login/JWT,
  respuesta uniforme de recuperación, token hasheado/expirable/de uso único y
  revocación de sesiones; correo de prueba en Mailpit; UI de recuperación.
- **Aceptación:** activo=false deniega login y JWT existente; recuperación no enumera
  cuentas; token vencido/reutilizado falla; contraseña nueva funciona y las sesiones
  anteriores se invalidan; no se duplica student_accounts.
- **Pruebas:** replay, expiración, cuenta inexistente/inactiva, cambio de contraseña,
  aislamiento de cuentas y regresiones de vínculo/expediente.
- **Entrega:** incremento integrado desde cambios revisados, sin duplicar tablas/token.

### SEC-03 — Bitácora mínima de acciones críticas (T26.1)

Como auditor autorizado, necesito reconstruir cambios de permisos y emisiones sin
acceder a secretos ni a registros privados de otra organización.

- **Prioridad:** P1. **Estado:** pendiente. **Estimación:** por refinar.
- **Dependencias:** T06 y definición de eventos/información permitida.
- **Subtareas:** catálogo de eventos; persistencia append-only desde la aplicación;
  actor/tenant/recurso/fecha/correlación; consulta paginada por ámbito; permisos y UI.
- **Aceptación:** eventos de roles/usuarios/emisión/revocación; sin tokens, contraseñas
  ni contenido documental; tenant A no consulta eventos B; auditor no modifica eventos.
- **Pruebas:** autorización positiva/negativa, datos sensibles ausentes y rollback.
- **Entrega:** migración siguiente disponible (confirmar antes), contrato y vista real.

## Condición para ampliar a blockchain/IA/Android

El siguiente incremento debe elegirse a partir de los controles anteriores y sus
pruebas. Blockchain se diseña mediante ADR; OCR usa documentos autorizados y revisión
humana; Android reutiliza API y reglas existentes. Ninguno puede declararse activo
por tener un paquete instalado o una pantalla decorativa.
