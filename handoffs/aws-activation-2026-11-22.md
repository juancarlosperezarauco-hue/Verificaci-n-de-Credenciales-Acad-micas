# Entorno privado AWS activado — 22/11/2026

El usuario autorizó activar c7i-flex.large después de conocer la estimación corregida
de USD 68.75/mes a 730 horas, usando sus créditos y conservando el plan gratuito.
No se amplió el plan ni se abrieron puertos de entrada.

## Instalación ejecutada

- Perfil local: `credenciales`; cuenta esperada `098453331970`; región `us-east-2`.
- Instancia: `i-07ef6f96873de70a4`, etiqueta Deployment=`credenciales-private`.
- Ubuntu 24.04 oficial, c7i-flex.large, 2 vCPU / 4 GiB, raíz gp3 40 GiB cifrada.
- Código fijado en `0aa7f856b1d6b87e562d4153c4240f3b2e9a8df5`, integrado en main
  mediante PR #6. Ruta `/opt/credenciales/releases/0aa7f856b1d6b87e562d4153c4240f3b2e9a8df5`.
- Cloud-init completado sin errores. Docker y Compose instalados; imágenes de
  backend, frontend y OCR compiladas en el servidor.
- PostgreSQL, MinIO, Mailpit, backend, frontend y OCR activos. Los cinco servicios
  con healthcheck configurado reportan healthy; MinIO está en ejecución.
- Las 23 migraciones V1–V23 aplicadas correctamente; readiness del backend `UP`.
- Claves generadas en el host y conservadas con permisos 600. Password de bootstrap
  retirado del entorno del backend después de crear el administrador inicial.
- La instalación local, sus datos y otros proyectos no se modificaron.

## Acceso y comprobaciones

- Software: `http://localhost:4302/`, mediante túnel SSM al servidor AWS.
- Superadministración: `http://localhost:4302/superadmin/login`.
- Correo ficticio: `http://localhost:8126/`, mediante un segundo túnel SSM.
- Ambos túneles escuchan en `127.0.0.1` del equipo; no son URLs públicas compartibles.
- Complemento Session Manager de Windows instalado y verificado (1.2.896.0).
- Login de superadministración y consulta protegida: correctos. Cierre de sesión
  HTTP 204 y reutilización posterior del token rechazada con HTTP 403.
- Pruebas públicas Playwright ejecutadas contra AWS mediante el túnel: 2 aprobadas
  (escritorio y móvil, navegación, FAQ y anchos 320/412/1440, sin errores JavaScript).
- CI de la revisión desplegada: suite completa aprobada en run 45046367589;
  validación privada aprobada en run 45046367711, incluidas las 9 pruebas de claves
  y límites, serialización AWS y bloqueo de incrementos no autorizados de costo.

Los datos iniciales se entregaron en un archivo local privado fuera del repositorio,
con acceso limitado al propietario de Windows y SYSTEM. La transferencia utilizó
RSA-OAEP-SHA256; SSM devolvió solo ciphertext y el material de transferencia se retiró.
No incluir valores de acceso en Git, incidencias, logs o mensajes.

Los túneles se iniciaron como procesos independientes ocultos en Windows. Su estado
local se guarda en `.local-backups/aws-private/`, ignorado por Git. Si se cierran,
ejecutar `scripts/connect-aws-private.ps1` y, para correo, el mismo script con
`-Service Mail`. Se necesita una sesión AWS activa; el servidor continúa consumiendo
créditos aunque el túnel esté cerrado.

## Pendientes del piloto

- El propietario debe cambiar el password inicial y activar MFA.
- Aceptación con dos organizaciones, estudiantes y ciclo académico completo en AWS.
- Firma y correo siguen siendo de demostración; no es un entorno comercial público.
- Copia externa coordinada y restauración probada antes de almacenar datos reales.
- Dominio, HTTPS público, SMTP y firma institucional real para la etapa comercial.
- El APK existente sigue dirigido al emulador; no se cambió su API a este túnel.

No terminar instancias, eliminar volúmenes ni regenerar claves al actualizar.
El volumen raíz se conserva si se termina EC2 y continúa facturándose. Consultar
`deploy/aws/README.md` para operación y costos.
