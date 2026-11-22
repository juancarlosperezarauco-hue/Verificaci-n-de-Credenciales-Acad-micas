# Prioridades: arranque, aceptación y seguridad

Continuación del 22/11/2026 desde main 234e428, en codex/priority-readiness.
No hay nuevas migraciones ni contratos de API. Se conservan V1–V23.

## Arranque local comprobado

Docker Desktop pudo iniciarse y los seis servicios principales quedaron activos.
Se verificaron HTTP de readiness, liveness, frontend y AI; PostgreSQL conserva una
organización y un estudiante, con las 23 migraciones aprobadas. No se crearon datos
de prueba en la base principal ni se cambiaron sus claves. HRcore quedó activo.

scripts/start-local.ps1 ofrece un arranque repetible, comprueba motor Linux local,
rechaza contextos remotos y .env ausente, espera readiness y consulta los puertos
reales. Solo opera el proyecto verificacion-credenciales. No contiene down, prune,
eliminación de volúmenes ni generación automática de secretos.

Se corrigió check-health.ps1: PowerShell puede devolver bytes para el MIME vendor
JSON de Actuator. Se decodifica UTF-8 antes de comprobar status, evitando falsos
fallos HTTP 200. CI ejecuta este script contra el entorno aislado.

## Aceptación integrada

El seed deja de aprobar instituciones y confirmar correos mediante SQL. Usa registro
institucional completo, aprobación real por SUPER_ADMIN, registro de estudiante
con correo/contraseña, perfil posterior y token recibido en Mailpit.
El bootstrap y las identidades son ficticios .test del proyecto audit.
Los secretos se capturan en memoria y el fixture privado está excluido de Git.

La prueba academic-cycle crea sus propias organizaciones y estudiante. Comprueba
una identidad global vinculada a dos tenants, rechazo de accesos cruzados,
rol personalizado de revisor, observación/corrección/reenvío, aprobación separada,
firma CMS de prueba, emisión idempotente, descarga privada, detección de alteración,
reemplazo con original preservado/revocado y retirada de consentimiento manteniendo
el historial personal. El mismo flujo de servicio se ejecuta una vez; la navegación
y la prueba de cabeceras cubren escritorio y móvil.

El seed por API/Mailpit aprobó localmente. El primer intento E2E coincidió con
tiempos de espera y errores 500 del motor Docker al ejecutar tres proyectos.
Se interrumpió ese intento y el usuario autorizó detener temporalmente los seis
contenedores de HRcore, sin borrar datos. Con memoria liberada se repitió el ciclo
académico completo: aprobado localmente en 20.2 segundos. Después se detiene audit;
credenciales queda disponible y HRcore permanece detenido temporalmente por esa
autorización. No se reinició Docker/WSL ni se eliminaron volúmenes.

La [CI 45034361749](https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas/actions/runs/45034361749)
aprobó los cinco jobs del commit 2d387f9e15e11b56a4f422f8944419e0557ac87f:
156 backend PostgreSQL/Flyway, 114 Angular, 11 OCR nativo, 12 pruebas integradas y
5 Flutter, total 298. Se omiten dos duplicados de flujos de servicio del proyecto
móvil; las pruebas de interfaz y CSP sí cubren ambos tamaños. Builds, análisis
Flutter y APK debug también aprobaron. La documentación posterior no cambia código.

## Seguridad aplicada

- La configuración productiva se valida mediante BeanFactoryPostProcessor antes
  de instanciar conexiones. Las 18 pruebas de configuración aprobaron localmente.
- Se analizan los parámetros JDBC completos, se exige verify-full y la CA montada
  en /run/certs/postgres-root.crt. Se rechazan parámetros duplicados, credenciales
  incrustadas, falsa coincidencia en otro parámetro, factories no validadoras,
  hostname verifiers personalizados y sobrescrituras TLS mediante Hikari.
  No se imprime la URL ni los valores privados al rechazarla. Por ahora se admite
  una dirección PostgreSQL en DB_URL; HA requiere endpoint gestionado o contrato nuevo.
- Nginx aplica CSP, protección de marcos, nosniff, no-referrer y permisos limitados.
  Recuperación y confirmación conservan no-store y todas las cabeceras, pese a
  las reglas de herencia de nginx. La cámara sigue disponible para el propio sitio.
- Angular compila sin inlineCritical para eliminar el evento onload inline y poder
  bloquear scripts inline y eval. Los estilos inline siguen permitidos por el uso
  actual de Angular/CDK; una política con nonces requiere trabajo adicional.
- Producción y audit limitan privilegios/procesos del backend y OCR. El backend
  tiene raíz de solo lectura y /tmp temporal acotado. Las pruebas CI del ciclo
  académico y OCR ejercitan estas restricciones.

## Medición acotada

Con el entorno audit preparado y E2E_ALLOW_FIXTURES=isolated-audit:

    node scripts/load-audit.mjs --requests 40 --concurrency 4 --p95-ms 3000

Solo acepta el origen loopback fijo de audit y cuentas ficticias. Alterna consulta
pública y expediente autenticado, valida JSON y cierra la sesión al terminar.
Límites: 80 peticiones, 8 clientes y timeout de 5 segundos. No cambia documentos.
Escribe e2e/test-results/load-summary.json, sin tokens ni datos personales, y falla
si hay errores o se supera el presupuesto p95. CI conserva este informe.
En esa CI se midieron 40 peticiones/4 clientes, sin errores: p95 20 ms en consulta
pública y 25 ms en expediente. Esta medición pequeña con datos ficticios no
representa la capacidad para el volumen de una institución ni el equipo local.

## Despliegue que sigue pendiente

Servidor/dominio, SMTP real, CA/OCSP/CRL/TSA/custodia, blockchain real, teléfono,
recuperación en otro host y piloto institucional siguen sin recursos designados.
No se ha desplegado un servicio público ni se ha atribuido confianza legal a las
firmas ficticias. Ver COMPLETION_STATUS.md y RECOVERY_AND_EXTERNAL_SERVICES.md.

## Fuentes técnicas

- [pgJDBC: SSL](https://jdbc.postgresql.org/documentation/ssl/).
- [Angular 20: configuración de compilación](https://v20.angular.dev/reference/configs/workspace-config).
- [Angular: CSP y scripts inline](https://angular.dev/best-practices/security).
- [Docker Compose: espera de servicios](https://docs.docker.com/reference/cli/docker/compose/up/).
