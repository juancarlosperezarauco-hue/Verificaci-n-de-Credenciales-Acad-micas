# Operación y recursos externos

## Respaldo coordinado

Se detienen los escritores del proyecto y MinIO, se guarda pg_dump y se copia el
almacenamiento detenido, junto con el archivo de claves. El paquete incorpora un
manifiesto SHA-256 y se cifra con CMS AES-256-GCM usando un certificado de
recuperación. La clave privada de recuperación debe custodiarse por separado.
No se eliminan volúmenes del proyecto.

PowerShell 7, Python 3.12+, Docker, tar y OpenSSL 3 son necesarios:

    ./scripts/init-backup-recipient.ps1 -Directory C:/boveda/backup
    ./scripts/backup-project.ps1 -RecipientCertificate C:/boveda/backup/recovery-certificate.pem
    ./scripts/rehearse-restore.ps1 -Snapshot C:/ruta/snapshot.cms -RecoveryKey C:/boveda/backup/recovery-key.pem

La frase se introduce fuera del chat como CREDENTIAL_BACKUP_PASSPHRASE. No se
imprime ni se envía como argumento de proceso. El ensayo restaura en volúmenes
nuevos credentials-restore-*, verifica manifiesto y conteos y comprueba MinIO.
El payload descifrado se conserva temporalmente en .local-backups/restores para
la comprobación de aplicación; contiene claves y no debe compartirse. Después:

    ./scripts/verify-restored-application.ps1 -RestoreReport C:/ruta/restore-report.json -FixtureFile C:/ruta/cuentas-ficticias.json

La comprobación admite únicamente cuentas ficticias .test, deshabilita el envío
de correos y no usa la base original. Comprueba login, expediente, bóveda de firma,
PDF, huella, CMS y evidencia cifrada si el fixture identifica una disputa. Al
aprobar, detiene el ensayo y elimina su payload descifrado. Conserva el informe
y los volúmenes detenidos para inspección. No reutilizar el mismo ensayo como
un backup nuevo: la comprobación genera documentos ficticios en la copia.

Evidencia local 2026-11-15: snapshot de audit, 73 archivos; restauración con
25 organizaciones, 25 estudiantes, 19 credenciales, 243 eventos y 21 objetos
gestionados. Se comprobaron login, expediente, una nueva firma con la bóveda
restaurada, descarga MinIO, SHA-256, CMS y descifrado de evidencia. El avatar no
existía en ese fixture. El cifrado también rechazó una alteración del paquete.
Esto no demuestra recuperación en un host independiente ni custodia externa.

## Preparación para producción

El usuario todavía no tiene proveedores ni institución piloto. No se han creado
cuentas, contratado servidores, enviado mensajes reales ni desplegado un dominio.
deploy/production.env.example describe los valores que deben configurarse fuera
de Git. compose.production.yml ofrece TLS con Caddy, sin publicar puertos de API,
OCR ni base de datos. No se debe ejecutar con valores de ejemplo.

El perfil Spring production rechaza claves repetidas/de desarrollo, URLs HTTP,
PostgreSQL sin sslmode=verify-full y firma demo. SMTP requiere autenticación y
STARTTLS. Swagger se deshabilita. Estas comprobaciones no sustituyen auditoría,
prueba de carga, configuración del firewall ni evaluación de recuperación total.

| Recurso | Para qué se necesita | Dato que se puede compartir |
| --- | --- | --- |
| Dominio propio y servidor | HTTPS, QR públicos, despliegue y alertas | Nombre de dominio y proveedor |
| PostgreSQL 16 con TLS | Persistencia productiva y recuperación externa | Proveedor, región y CA pública |
| S3 o MinIO con HTTPS | PDFs y evidencia privada | Proveedor y endpoint |
| SMTP con dominio verificado | Correo real y entregabilidad | Proveedor y remitente autorizado |
| Certificado institucional y custodia | Confianza CA, revocación, TSA y claves de producción | Autoridad, política y proveedor; nunca clave privada |
| Red/RPC/wallet blockchain | Publicación real, confirmaciones y costes | Red y proveedor; nunca frase semilla |
| Proyecto Gemini | Resúmenes externos opcionales | Proyecto; clave mediante secret manager |
| Firebase | Push Android posterior | Nombre de proyecto |
| Keystore y distribución Android | Firma de publicación, teléfono y Play Store | Responsable y canal de pruebas |
| Institución y responsables | Piloto y aceptación de administradores/revisores/estudiantes | Institución y roles de prueba |

El OCR Tesseract no requiere cuenta externa y no envía CI a Gemini. Las claves
internas AI_SERVICE_TOKEN son exclusivamente del servidor.

## Verificación y supervisión

.github/workflows/verify.yml comprueba PostgreSQL/Flyway, Angular, OCR nativo y
navegador en escritorio/móvil. Los datos de CI son ficticios. scripts/check-health.ps1
produce JSON y código de salida no cero al fallar readiness, liveness o frontend.
La entrega de alertas y despliegue automático queda pendiente del servidor, el
destinatario autorizado y la protección de entornos/ramas. Nunca se despliega
directamente en main desde estas herramientas.

Fuentes primarias: https://docs.openssl.org/3.5/man1/openssl-cms/ ;
https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html ;
https://playwright.dev/docs/test-projects .
