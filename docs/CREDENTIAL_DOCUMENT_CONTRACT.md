# Contrato de integridad documental — T05 / T14

Fecha: 2026-11-13. Se conservan rutas, DTO, permisos y esquema V1–V13.

## Comportamiento requerido

- `POST /api/credentials/{id}/document` exige `CREDENTIAL_CREATE` y propiedad del tenant.
- Solo un borrador admite adjuntos. Emitidas y revocadas devuelven HTTP 400, como
  antes, pero deben rechazarse **antes** de invocar almacenamiento.
- `POST /api/credentials/{id}/generate-document` mantiene la misma restricción.
- Carga, generación, emisión y revocación serializan cambios de la misma credencial
  mediante bloqueo de fila, conservando el filtro `id + tenant_id`.
- Cada carga/generación usa una clave de objeto nueva bajo el prefijo tenant/credencial.
  Un fallo posterior al almacenamiento no debe sobrescribir el objeto al que apunta
  una transacción ya confirmada. Las claves son internas; no cambia la respuesta API.
- Un reintento de emisión de una credencial emitida/revocada devuelve HTTP 400
  antes de invocar el adaptador blockchain. No se introduce emisión idempotente.
- La descarga mantiene `CREDENTIAL_VIEW` y propiedad institucional.

## Fallo encontrado

Antes del cambio, `attachDocument` ejecutaba `storage.store` sobre una clave derivada
del nombre original y luego llamaba `Credential.attachDocument`. La entidad rechazaba
el estado emitido, pero la transacción PostgreSQL no revertía la escritura MinIO.
Reenviar el mismo nombre podía alterar bytes publicados conservando el hash anterior.

## Límites

Esta corrección no implementa firma digital, validación estructural/antivirus de PDF,
outbox, recolección de objetos huérfanos, inmutabilidad administrativa de MinIO ni
transacciones distribuidas. Una escritura externa completada y luego abortada puede
dejar un objeto nuevo sin referencia; nunca debe reemplazar el objeto vigente.
El bloqueo se mantiene durante PDF/almacenamiento/anclaje: evaluar tiempos de espera
y procesamiento asíncrono antes de producción. El adaptador blockchain sigue siendo
de desarrollo (`pending:`), sin evidencia de red real.

## Verificación

Pruebas HTTP con PostgreSQL/Flyway y almacenamiento simulado con efectos reales en
memoria: rechazos sin escritura, descarga original tras intento de reemplazo,
acceso cruzado/por permisos, fallo después de escribir y concurrencia carga/emisión.
Prueba adicional contra API/PostgreSQL/MinIO reales en base y bucket dedicados.
