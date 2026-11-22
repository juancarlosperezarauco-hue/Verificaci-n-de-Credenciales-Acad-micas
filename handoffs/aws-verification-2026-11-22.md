# Verificación real de AWS — 22/11/2026

El usuario pidió comprobar si el software funcionaba realmente. Se trabajó contra
la EC2 existente y sus túneles; no se creó infraestructura adicional ni se cambió
el código del backend, frontend, las migraciones o los datos preexistentes.

## Fallo encontrado y corregido

EC2 estaba running y SSM Online, pero el enlace local rechazaba la conexión:
AWS había terminado los túneles por inactividad. Los logs locales y el historial
SSM confirmaron el motivo. El software no se había apagado.

`connect-aws-private.ps1` incorpora reconexión opcional, vuelve a comprobar la
identidad AWS antes de reabrir y se detiene si cambia/caduca. No modifica políticas
de cuenta, tiempos globales ni abre puertos públicos. `open-aws-private.ps1`
inicia/reutiliza ambos procesos y comprueba HTTP; `close-aws-private.ps1` cierra
solo los procesos propios. Se creó el acceso directo **Credenciales AWS** en el
escritorio del propietario para que pueda volver a abrir el acceso sin depender
de esta conversación.

La recuperación se probó terminando explícitamente en AWS únicamente la sesión
del túnel de aplicación identificado en su propio log. Se observó una sesión
nueva y HTTP 200 después del cierre. Prueba local idempotente: abrir el lanzador
de nuevo conservó los mismos procesos y no creó duplicados. Cuatro casos sin
AWS cubren conexión única, reconexión, identidad cambiada y cuenta inicial ajena.

## Aceptación real del ciclo académico

`scripts/verify-aws-private.mjs --run-fictional-flow` pasó las 11 comprobaciones:

1. Login de superadministración y consulta protegida.
2. Registro de dos instituciones, bloqueo mientras estaban pendientes y aprobación.
3. Registro del estudiante, perfil posterior y confirmación de correo en Mailpit.
4. Vinculación del mismo estudiante global a ambas instituciones por su código.
5. Programa, matrícula, conclusión y rechazo de acceso desde otro tenant.
6. Rol revisor, observación, corrección y aprobación con separación de permisos.
7. PDF, firma de demostración, emisión y reintento idempotente.
8. Descarga desde MinIO, firma/hash válidos y rechazo del archivo modificado.
9. Reemplazo, revocación del original e historial con bytes originales conservados.
10. Revocación explícita y consulta pública por código.
11. Retiro del consentimiento, bloqueo institucional y expediente personal conservado.

Resultado: success=true y cleanupErrors=0. Las dos organizaciones ficticias
quedaron SUSPENDED; los documentos de prueba quedaron revocados y las sesiones
creadas por el script se cerraron. Se conservó un estudiante ficticio claramente
etiquetado para trazabilidad; no se borraron datos ni cuentas reales.
El informe local sin contraseñas está en
`.local-backups/aws-private/acceptance-latest.json`.

## Navegador real contra AWS

Cuatro casos Playwright aprobados: login de superadministrador, menú lateral y
cierre de sesión tanto en escritorio como en móvil, y navegación pública en ambos.
Anchos de landing comprobados: 320, 412 y 1440. Sin errores JavaScript en la
navegación pública. Se utiliza una configuración separada de la suite audit,
sin trazas, videos ni capturas de acceso autenticado; los errores al rellenar
credenciales se sustituyen por mensajes genéricos. Ninguna clave queda en Git.

## Límites prácticos

Esto verifica el piloto privado con datos ficticios y firma/correo de prueba.
No certifica capacidad de producción, entrega de correo a Internet, firma oficial,
blockchain, proveedores de IA externos, teléfonos físicos ni todos los trámites
de una institución real. El dominio y acceso público siguen pendientes.
El túnel necesita una sesión AWS válida y el servidor continúa consumiendo
créditos mientras esté encendido aunque el acceso local se cierre.
