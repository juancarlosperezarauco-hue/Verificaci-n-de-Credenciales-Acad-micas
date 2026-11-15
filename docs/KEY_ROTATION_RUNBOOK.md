# Rotación de claves con transición compatible

Fecha 2026-11-14. Identificadores públicos; secretos solo en entorno privado.

La aplicación conserva compatibilidad de lecturas durante una rotación y permite
recifrar por lotes. No cambia claves reales por sí sola. Las claves anteriores
deben permanecer disponibles hasta terminar la transición. Probar primero en una
copia aislada y respaldar base de datos, objetos y configuración privada.

## Variables

| Uso | Clave actual | Identificador actual | Claves anteriores |
|---|---|---|---|
| Firma JWT | JWT_SECRET | JWT_KEY_ID | JWT_PREVIOUS_KEYS |
| Semillas MFA | MFA_ENCRYPTION_KEY | MFA_KEY_ID | MFA_PREVIOUS_KEYS |
| Correo pendiente | MAIL_ENCRYPTION_KEY | MAIL_KEY_ID | MAIL_PREVIOUS_KEYS |

KEY_ID acepta 1–40 letras, números o guiones, sin espacios. El identificador
inicial es legacy; los JWT sin kid y ciphertext antiguos usan ese identificador.
PREVIOUS_KEYS tiene formato `id=valor;otro-id=valor`, sin repetir el actual.
El valor de MFA/MAIL son exactamente 32 bytes aleatorios en base64. JWT requiere
al menos 32 bytes UTF-8 de secreto aleatorio; conservar el valor exacto usado.
No guardar los valores en comandos compartidos, documentación, capturas o Git.

## Procedimiento

1. Realizar y comprobar un respaldo. Conservar también las claves antiguas:
   la base cifrada por sí sola no permite reconstruirlas.
2. Generar una nueva clave privada y un identificador nuevo. En el entorno
   privado, mover el identificador/valor anterior a PREVIOUS_KEYS y configurar
   KEY_ID/clave actual nuevos. Cambiar un tipo de clave por vez facilita comprobarlo.
   No reutilizar un identificador para otro valor ni compartir claves entre tipos.
3. Recrear el backend con la configuración nueva. En varias instancias, coordinar
   la configuración y conservar las verificaciones anteriores durante el despliegue.
   Los nuevos JWT/ciphertexts usan la clave actual; los antiguos se siguen leyendo
   con la clave anterior configurada. El rate limit debe usar una configuración
   coherente entre instancias; no se presenta este entorno local como despliegue HA.
4. Ingresar como SUPER_ADMIN y abrir **Mantenimiento**. Verificar identificadores
   actuales y pendientes. No se muestran ni introducen valores secretos en la UI.
5. Ejecutar **Recifrar datos pendientes** hasta que MFA/correos pendientes lleguen
   a cero. Cada ejecución bloquea y recifra como máximo 50 filas por categoría en
   una transacción. Un error revierte ese lote. Revisar claves/configuración ante
   un error; no eliminar filas para ocultarlo. El contenido y su propietario no cambian.
6. Los correos PROCESSING esperan a finalizar/reintentar para no alterar el payload
   que está usando otro worker. Revisar el monitor y esperar al lease si se interrumpió.
7. Para JWT, esperar a que **Sesiones por clave de firma** muestre cero accesos
   vigentes bajo la clave anterior. No se puede cambiar la firma de un JWT que ya
   está en el navegador. Los cierres/vencimientos se reflejan en ese recuento.
8. Comprobar login normal, MFA, recuperación y envío de un correo. Solo entonces
   retirar una clave anterior sin referencias vigentes y recrear el backend.
   Conservar los respaldos cifrados con sus claves correspondientes para restauración.

## Límites

- El recifrado no habilita/deshabilita MFA ni renueva respaldos; esas operaciones
  requieren contraseña y segundo factor de la propia cuenta.
- Sin las claves antiguas, los datos que todavía las necesitan no se pueden leer.
  La aplicación falla sin sustituirlos por contenido inventado ni omitir MFA.
- Las claves se validan al iniciar. Un identificador desconocido de JWT se rechaza;
  no se descargan claves desde URLs del token ni se prueban firmas arbitrarias.
- La recuperación sin autenticador ni respaldos requiere un procedimiento humano
  de identidad separado; no se añade una ruta de restablecimiento con contraseña sola.
- La custodia en un gestor de secretos, HTTPS y procedimientos de producción/DR
  siguen siendo trabajo del despliegue productivo. No rotar claves solo para probar
  en la base del usuario: usar el entorno aislado y cuentas ficticias.
