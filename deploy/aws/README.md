# Evaluación privada en AWS

Este entorno permite probar la plataforma sin dominio y sin publicar ningún puerto
de entrada. Es una instalación nueva, con datos ficticios, independiente de Docker
local. **No es el despliegue comercial de `compose.production.yml`.** Esa configuración
conserva sus requisitos de HTTPS, PostgreSQL TLS, correo real y firma sin certificados
de demostración.

## Configuración preparada

- Cuenta esperada: `098453331970`, perfil `credenciales`, región `us-east-2`.
- Una EC2 Ubuntu 24.04 LTS oficial, x86_64, `c7i-flex.large`, 2 vCPU / 4 GiB RAM.
- Disco raíz gp3 de 40 GiB cifrado. Se conserva al terminar la instancia;
  sigue generando costo hasta su eliminación explícita.
- Instancia Flex, sin la configuración de créditos de CPU de la familia T3.
- VPC existente; no crea NAT, balanceador, IP elástica ni bases administradas.
- IP pública automática para descargas y conexión saliente a SSM. **Ninguna regla
  de entrada**, incluidos SSH/HTTP/HTTPS. Salidas TCP 80/443 para paquetes,
  repositorios, imágenes y SSM; el DNS lo proporciona el resolvedor de la VPC.
- IMDSv2 obligatorio, salto 1: el agente del host usa su rol; los contenedores
  no necesitan credenciales AWS ni acceso a metadatos.
- Rol EC2 con `AmazonSSMManagedInstanceCore`; sin permisos de administrador.
- PostgreSQL 16, MinIO, Mailpit, backend, frontend y OCR local en contenedores.
  Solo frontend `127.0.0.1:4302` y Mailpit `127.0.0.1:8126` publican puertos de loopback.
- Firma de demostración habilitada **solo en esta evaluación privada**. No acredita
  validez oficial. Gemini, Firebase, blockchain real y SMTP externo no se activan.
- Claves independientes generadas dentro del host; AES-256 de 32 bytes en Base64.
  Nunca se incluyen en Git, user data, planes ni salidas de los comandos.

## Coste orientativo

Estimación consultada el 22/11/2026 para Ohio, Linux On-Demand, 730 horas/mes:

| Recurso | Cálculo | USD/mes aproximado |
| --- | --- | ---: |
| c7i-flex.large | 0.08479 × 730 | 61.90 |
| IPv4 pública | 0.005 × 730 | 3.65 |
| gp3 40 GiB | 0.08 × 40 | 3.20 |
| Total base | | **68.75** |

La primera propuesta t3.medium (USD 37.22/mes) fue rechazada por AWS al activar:
no es elegible en este plan FREE. El dry-run de EC2 no detectó esa restricción.
Se verifica ahora explícitamente `FreeTierEligible` tanto en el tipo como en la
AMI y se obtiene el precio EC2 del catálogo regional antes de crear recursos.
El límite de costo autorizado conserva por defecto los USD 37.22 iniciales:
la alternativa requiere autorización expresa del nuevo importe.

No incluye impuestos, transferencia facturable, snapshots ni otros recursos.
Los créditos se aplican según elegibilidad y saldo AWS; no se promete costo cero.
Apagar EC2 reduce cómputo y libera su IP automática; el disco sigue facturándose.
No se cambia el plan de cuenta ni se habilitan funciones avanzadas automáticamente.

Fuentes: [catálogo regional AWS EC2](https://b0.p.awsstatic.com/pricing/2.0/meteredUnitMaps/ec2/USD/current/ec2-ondemand-without-sec-sel/US%20East%20(Ohio)/Linux/index.json),
[IPv4](https://aws.amazon.com/vpc/pricing/), [EBS](https://aws.amazon.com/ebs/pricing/).

## Preparar y activar

Desde la raíz del repositorio, en PowerShell:

```powershell
./scripts/prepare-aws-private.ps1
```

El modo predeterminado consulta identidad, red, rutas, AMI oficial, elegibilidad y disponibilidad
del tipo de instancia; escribe `.local-backups/aws-private/plan.json`, ignorado por
Git. **No crea ni modifica recursos AWS.** No prueba las autorizaciones de creación.

Después de revisar el plan y autorizar su consumo de saldo:

```powershell
./scripts/prepare-aws-private.ps1 -Apply -ApprovedMonthlyCost 68.75
```

La rama debe estar limpia y su HEAD publicado en el repositorio esperado. El servidor
descarga ese commit exacto; no ejecuta el `main` mutable. El comando comprueba la
cuenta, presupuesto estimado autorizado y propiedad de los recursos; una instancia existente no se duplica ni
se sustituye. Los errores de SCP se reportan sin intentar eludirlos. No se configura
una clave de acceso AWS permanente.

El arranque instala Docker y Compose de Ubuntu, conserva SSM, genera secretos en
`/etc/credenciales/private.env` (600), compila secuencialmente, aplica las migraciones
existentes y espera salud. No crea migraciones nuevas. Al quedar listo elimina
el password de bootstrap del entorno del backend y lo recrea. La contraseña inicial
se conserva en `/etc/credenciales/bootstrap-admin.txt` (600) para que el administrador
la consulte **en su sesión privada**, la cambie y retire ese archivo después.
Usuario inicial de prueba: `superadmin@pruebas.invalid`.

EC2 en estado running no significa que la aplicación esté lista. Comprueba SSM Online,
el fin de cloud-init y `docker compose ps` antes de realizar pruebas. Si un paso
falla, no se borran recursos ni datos automáticamente; pueden quedar un rol,
perfil o grupo propio, o un disco retenido. Revisa antes de repetir. La región,
permisos IAM/PassRole, cuotas y conectividad pueden impedir la creación pese a
que el plan de lectura sea válido.

## Acceso desde Windows

Se necesita AWS CLI v2 con sesión activa y el [complemento oficial de Session
Manager](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html).
La preparación no instala herramientas nuevas en el equipo local.

Para uso normal, abrir los dos túneles con reconexión y entrar al navegador:

```powershell
./scripts/open-aws-private.ps1 -OpenBrowser
```

El lanzador conserva sus procesos en segundo plano y reutiliza los existentes,
sin crear servidores. `connect-aws-private.ps1 -Reconnect` restablece las sesiones
que AWS cierre por inactividad. La reconexión puede tardar unos segundos; no
modifica el tiempo de sesión de la cuenta ni sus permisos. Si caduca la sesión
AWS o cambia de cuenta, se detiene y requiere que el propietario inicie sesión
de nuevo. No autoriza por sí mismo un nuevo login de navegador.

Para cerrar únicamente el acceso de este equipo:

```powershell
./scripts/close-aws-private.ps1
```

Cerrar los túneles no apaga EC2 ni detiene su consumo de créditos. El lanzador
comprueba la propiedad del proceso antes de cerrarlo y no toca otros proyectos.

Para una conexión manual única, que termina cuando AWS cierre esa sesión:

```powershell
./scripts/connect-aws-private.ps1
```

Mantén abierta la terminal y visita `http://localhost:4302`. Así no colisiona con
la instalación local en 4300. En otra terminal, para leer los correos ficticios:

```powershell
./scripts/connect-aws-private.ps1 -Service Mail
```

Visita `http://localhost:8126`. Los mensajes no salen a Internet. Los enlaces de
confirmación requieren también el túnel de la aplicación. Cada probador necesita
su propia sesión AWS autorizada; no se comparten las credenciales AWS.

Para revisar el host, usa EC2 → Conectar → Session Manager, o `aws ssm start-session`
con el ID de la instancia. El agente se ejecuta con privilegios administrativos;
no pegues salidas con claves en incidencias o en el chat.

## Pruebas de aceptación en el servidor

La comprobación explícita del piloto usa únicamente destinatarios `.test` y crea
dos organizaciones marcadas `PRUEBA AWS`, un estudiante ficticio y credenciales
de demostración. Conserva los registros para revisión, suspende sus organizaciones
al finalizar y cierra las sesiones creadas por la propia prueba. No modifica
organizaciones o estudiantes preexistentes. Usa el archivo de acceso inicial
privado del propietario; si cambió la contraseña o habilitó MFA, se debe preparar
un acceso de prueba adecuado, sin eludir esa protección.

```powershell
node scripts/verify-aws-private.mjs --run-fictional-flow
```

Resultado sin claves: `.local-backups/aws-private/acceptance-latest.json`.
No se ejecuta en CI ni reutiliza/relaja las restricciones de los scripts audit.
Para comprobar navegación pública, login, menú y logout en navegadores reales:

```powershell
# Desde e2e; requiere Chrome local o Chromium de Playwright.
$env:AWS_PRIVATE_BROWSER_CHECK='1'
$env:E2E_CHROME_PATH='C:/Program Files/Google/Chrome/Application/chrome.exe'
npx playwright test --config aws-private/playwright.config.ts
```

La configuración privada no genera trazas, videos ni capturas de las pantallas
autenticadas para proteger los datos de acceso. El archivo de prueba pública
puede guardar capturas de la landing anónima.

La aceptación automática ejecutada el 22/11/2026 está documentada en
`handoffs/aws-verification-2026-11-22.md`. La revisión operativa del propietario
y el piloto con usuarios reales siguen siendo pasos separados.

1. Confirmar cloud-init completado, los seis servicios saludables/activos, puertos
   enlazados a loopback y migraciones V1–V23 aplicadas.
2. Entrar con superadmin; cambiar la contraseña y verificar MFA/sesiones.
3. Registrar una organización y aprobarla; personalizarla y crear un rol limitado.
4. Registrar un estudiante, confirmar correo en Mailpit, completar perfil y enlazar
   por su código; comprobar aislamiento entre dos organizaciones ficticias.
5. Revisar identidad y ciclo académico con actores distintos; emitir un certificado
   de demostración, descargarlo, verificar QR y archivo, revocarlo y corregirlo.
6. Probar recuperación de contraseña, notificaciones y reapertura del túnel.
7. Reiniciar únicamente los contenedores propios y comprobar persistencia de datos
   y claves. Documentar los resultados antes de considerar terminado el piloto.

El APK actual apunta al emulador local: **no se modifica ni se anuncia compatible
con este túnel desde un teléfono físico**. El APK con HTTPS público se prepara al
tener dominio y entorno adecuado.

## Datos, actualizaciones y producción

No ejecutar `down -v`, `docker system prune --volumes` ni terminar el servidor para
actualizar. Guardar un respaldo coordinado de PostgreSQL, MinIO y las claves antes
de cualquier actualización; las claves son imprescindibles para restaurar datos
cifrados. El disco retenido y un solo servidor no sustituyen una copia externa ni
alta disponibilidad. Esta primera preparación no configura backups automáticos
ni actualizaciones automáticas de la aplicación.

Para pasar a servicio público: dominio, HTTPS, correo real, certificados institucionales,
almacenamiento y base TLS según `compose.production.yml`, copia externa probada y
aceptación del piloto. Abrir el grupo de seguridad de esta evaluación no es el
procedimiento para publicarla.
