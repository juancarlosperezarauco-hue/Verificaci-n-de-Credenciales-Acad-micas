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
