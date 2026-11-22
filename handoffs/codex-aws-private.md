# Preparación AWS privada

**Estado actualizado:** entorno activado tras autorización del importe corregido.
Ver `handoffs/aws-activation-2026-11-22.md` para recurso, versión, pruebas reales,
acceso privado y pendientes del piloto. Las notas siguientes registran la preparación.

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
Pruebas Compose locales: 3 aprobadas. Las siete pruebas, incluidas las cuatro de
permisos/secretos POSIX, pasaron en Linux en el run 45043833506 (revisión 6923365).
Sintaxis Bash y PowerShell comprobada localmente y en CI. Dry-run RunInstances
devolvió DryRunOperation: permiso básico comprobado sin crear servidor; no valida
la creación del rol ni PassRole. La suite completa de la plataforma se comprueba
antes de integrar.

No hay migraciones ni endpoints de negocio nuevos. No se copian datos locales ni claves.

## Riesgos y pendientes

- AWS rechazó t3.medium al intentar activar: tipo no elegible para FREE, pese al dry-run.
  Se preparó c7i-flex.large de 4 GiB, elegible, a USD 68.75/mes estimados; requiere
  autorización del aumento. Se incorporó comprobación explícita de elegibilidad,
  precio regional y límite autorizado antes de crear recursos.
- Complemento oficial Session Manager instalado en Windows, versión 1.2.896.0;
  instalador con firma Authenticode válida de Amazon.
- Activación, compilación y ciclo completo de aceptación en AWS aún no ejecutados.
- El volumen se conserva al terminar EC2 y sigue generando costo.
- Las firmas y el correo son de prueba; no es entorno público/comercial.
- Backup externo, dominio, SMTP real y APK para servidor público quedan pendientes.

Commit inicial de implementación: `60b2fa8`. CI del entorno:
https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas/actions/runs/45043833506
