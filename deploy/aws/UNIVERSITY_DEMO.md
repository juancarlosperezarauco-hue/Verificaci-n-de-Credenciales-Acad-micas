# Enlace temporal para la presentación universitaria

El usuario pidió compartir su proyecto académico sin comprar un dominio. Se usa
un Quick Tunnel HTTPS de Cloudflare desde la EC2 existente; no se crea otro servidor.
El enlace dura mientras exista ese proceso y puede cambiar al recrearlo. No es
un despliegue comercial ni requiere una cuenta o dominio de Cloudflare.

## Contrato de configuración antes de implementar

`app.security.allowed-origin` / `ALLOWED_ORIGIN` conservará el comportamiento
existente con un único origen y admitirá además una lista explícita separada
por comas. Cada elemento debe ser un origen HTTP(S) válido y exacto, sin comodines,
credenciales, rutas, consultas ni fragmentos. Se necesita permitir la URL HTTPS
temporal y `http://localhost:4302` a la vez para conservar el acceso privado.
La configuración de producción sigue exigiendo su origen único coincidente con
`APP_FRONTEND_URL`; no se cambian sus validaciones ni requisitos TLS.

`APP_FRONTEND_URL` será el HTTPS temporal para los nuevos QR y enlaces de correo.
No se modifican PDFs ya emitidos. Mailpit seguirá siendo privado; los correos
de la demostración no se entregan a Internet. Las firmas siguen marcadas como prueba.

## Límites de exposición

- Se publica únicamente el frontend y su API autenticada existente.
- PostgreSQL, MinIO/console, Mailpit, OCR, SSH y puertos backend siguen privados.
- El grupo de seguridad continúa sin reglas de entrada. Solo se añade la salida
  requerida por el conector a Cloudflare, registrada para retirarla al cerrar la demo.
- Se conservan usuarios, permisos, datos, claves y volúmenes existentes.
- El conector utiliza una imagen oficial fijada por digest, sin credenciales AWS.
- No se expone el archivo de acceso inicial ni se comparte la contraseña de superadmin.

La aprobación del enlace temporal no cambia el plan FREE ni crea compras de dominio.
El servidor y la transferencia siguen sujetos a su consumo AWS habitual.

## Operación

Desde la revisión desplegada en el servidor, con las claves existentes y Docker
activo, ejecutar `bash deploy/aws/start-university-demo.sh`. Obtiene la imagen
oficial 2026.11.1 y fija su digest; genera el enlace, aplica los orígenes explícitos
y actualiza únicamente backend y resolución del proxy frontend. La metadata queda
en `/etc/credenciales/university-demo.env` con permisos 600, sin copiar claves.

Antes de iniciar se habilitó salida TCP 7844 solo a `198.41.192.0/24` y
`198.41.200.0/24`, usando HTTP/2 e IPv4. Se comprobó que el grupo pertenece a la
instancia del piloto y no está asociado a otro proyecto. Las reglas de entrada
siguen vacías. Los IDs de las dos reglas agregadas se conservan localmente en
`.local-backups/aws-private/university-demo-egress-created.json` para retirarlas
de forma específica al dejar de utilizar esta función.

Para cerrar la publicación y restaurar el origen privado:
`bash deploy/aws/stop-university-demo.sh`. Solo detiene el proyecto del túnel y
reconfigura backend/frontend; no apaga EC2, no elimina datos ni regenera claves.
No ejecutar `down -v`. Reiniciar el proceso del túnel puede cambiar el enlace;
volver a ejecutar el inicio para actualizar también CORS, QR y enlaces nuevos.

## Activación y pruebas del 22/11/2026

Se activó desde la revisión `d0949a7e4aaffcd76eb92f2220a711ea7d11c5aa` en la EC2
existente `i-07ef6f96873de70a4`, región `us-east-2`:

https://attractive-little-foreign-head.trycloudflare.com

Este es un enlace temporal de presentación. La URL vigente y su procedencia se
registran en `.local-backups/aws-private/university-demo.json`, ignorado por Git.
Se creó **Presentar Credenciales** en el escritorio del propietario. Los datos
de acceso siguen en su archivo privado, fuera del repositorio; no se comparten
las credenciales de superadministración junto al enlace.

Comprobaciones realizadas:

- HTTPS público HTTP 200 con certificado validado y contenido de la aplicación.
- Preflight del origen público admitido; origen ajeno rechazado con HTTP 403.
- Cuatro casos de navegador real a través de la URL pública: login, menú lateral,
  logout y navegación anónima en escritorio y vista móvil.
- Once comprobaciones del ciclo académico contra el backend actualizado: instituciones,
  identidad global, permisos, correo Mailpit, emisión/firma de prueba, PDF, alteración,
  reemplazo, revocación y consentimiento; limpieza de sesiones sin errores.
- El enlace público respondió HTTP 200 después de cerrar ambos túneles locales;
  se restauró luego el acceso privado. No depende del PC del propietario.
- Los tests locales de CORS y guardas de producción pasaron (3 + 18). La CI del
  código ejecutable es el run 45093929363; las 12 pruebas de límites del despliegue
  pasaron en el run 45093929291. No hay migraciones nuevas.

Los correos continúan en el buzón interno de prueba y las firmas son de demostración.
Los PDFs antiguos conservan sus URLs originales; los generados durante esta demo
usan el enlace temporal nuevo. No se anuncia como un servicio comercial permanente.

Referencias: [Quick Tunnels](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/)
y [puertos de salida](https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/configure-tunnels/tunnel-with-firewall/).
