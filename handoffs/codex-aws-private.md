# Preparación AWS privada

Se prepara una instalación independiente de pruebas sin dominio, mediante EC2 y
túneles SSM, manteniendo Angular / Spring Boot / PostgreSQL / MinIO y V1–V23.
La producción existente conserva sus controles y requisitos. No se activan recursos
con coste al ejecutar el preparador sin `-Apply`.

## Archivos

- `compose.aws-private.yml`: seis servicios limitados, solo dos puertos en loopback.
- `scripts/prepare-aws-private.ps1`: plan de lectura por defecto; activación explícita,
  cuenta y región fijadas, commit publicado, recursos con propiedad comprobada.
- `scripts/connect-aws-private.ps1`: túnel de aplicación o correo.
- `deploy/aws/bootstrap-host.sh`, `init-private.sh`, `start-private.sh`: instalación,
  claves privadas compatibles, arranque con salud y retiro del bootstrap del entorno.
- `deploy/aws/README.md`: arquitectura, coste, operación y aceptación pendiente.
- `deploy/aws/tests/test_private_deploy.py`: límites de exposición, claves obligatorias,
  permisos, compatibilidad AES y conservación de secretos.
- `.github/workflows/aws-private.yml`: comprobaciones aisladas sin recursos AWS.
- Dockerfiles de frontend/backend: ARG opcional para limitar memoria al compilar.

## Validación

La cuenta y región coinciden con las esperadas; no se encontraron EC2 ni nodos SSM.
Plan de lectura ejecutado contra AWS: VPC/subred con salida, Ubuntu oficial y
disponibilidad t3.medium. No verifica RunInstances, PassRole ni creación IAM.
Pruebas Compose locales: 3 aprobadas. Cuatro pruebas POSIX requieren Linux en CI.
Sintaxis Bash y PowerShell comprobada localmente. Ver resultados CI antes de integrar.

No hay migraciones ni endpoints de negocio nuevos. No se copian datos locales ni claves.

## Riesgos y pendientes

- Autorizar el costo base aproximado de USD 37.22/mes a 730 h para activar EC2.
- El complemento Session Manager todavía debe instalarse en Windows para túneles CLI.
- Activación, compilación y ciclo completo de aceptación en AWS aún no ejecutados.
- El volumen se conserva al terminar EC2 y sigue generando costo.
- Las firmas y el correo son de prueba; no es entorno público/comercial.
- Backup externo, dominio, SMTP real y APK para servidor público quedan pendientes.

El commit final y los enlaces de CI se anotarán después de ejecutar las comprobaciones.
