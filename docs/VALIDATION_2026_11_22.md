# Continuación y comprobaciones del 22/11/2026

## Punto de partida

Main contenía el merge 6cf0233 de PR #1. Se comprobó su CI 43807182050:
backend, frontend, AI y navegador aprobados; Android fallaba en el script Kotlin
por el uso del DSL antiguo y la lambda de taskGraph.whenReady.

## Cambios

- Se usa ApplicationExtension de AGP 9, DSL nuevo y Kotlin integrado.
- mobile/platform/android/build.gradle.kts es la fuente mantenida que copia
  setup-mobile.ps1. Las tareas release dependen de validateCredentialSigning.
  La publicación exige key.properties/keystore privados y no usa la firma debug.
- Flutter create regeneraba el lockfile incluso con --no-pub. El script restaura
  los manifiestos antes de pub get --enforce-lockfile. Se comprobó su repetición
  comparando las huellas de pubspec.yaml y pubspec.lock, sin cambios en lib/.
- Se rechazan señales OCR sin código con 503, evitando un NullPointerException.
  Tres casos de proveedor malformado comprueban ausencia de auditoría de éxito
  y de decisión automática.
- La prueba integrada genera una imagen ficticia en Chromium, la sube como
  evidencia privada y llama al OCR desde Spring. Comprueba texto leído por
  Tesseract, respuesta 200, rango de confianza, revisión humana, estado PENDING
  y rechazo 403 del estudiante como revisor. Retira la solicitud al terminar.
  No publica tokens ni imágenes de identidad en trazas.
- No hay migraciones ni contratos de API nuevos. Se conservan V1–V23.

## Evidencia

[CI de referencia 44990243503](https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas/actions/runs/44990243503)
aprobó los cinco jobs: backend 138, Angular 114, OCR nativo 11, Flutter 5 y
navegador/API 9. El caso de OCR duplicado en el proyecto móvil se omite porque
prueba el mismo servicio; las ocho pruebas de interfaz sí cubren ambos tamaños.
El APK debug se generó como artefacto android-debug-apk.

Tras esa referencia se agregaron la conservación estricta del lockfile, la
comprobación de firma ausente y tres casos de error OCR. Las siete pruebas
locales de IdentityAnalysisServiceTests aprobaron. La suite completa contiene
ahora 141 casos backend; sus resultados y los del APK con dependencias fijadas
se consultan en [las ejecuciones de la rama](https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas/actions?query=branch%3Acodex%2Fcomplete-validation).
Código final: d75fdea14023d51ceebd4e340240691fbc42b708; la documentación posterior
no modifica ese código. No se incluyen credenciales, claves ni respaldos.

## Comprobación local y límites

El motor Docker local no era accesible por dockerDesktopLinuxEngine al comprobarlo,
aunque el usuario indicó haber abierto Docker Desktop. El lanzamiento automático
anterior fue bloqueado por la política de ejecución sin un motivo más específico.
No se confirma el arranque local ni se borran contenedores/volúmenes para resolverlo.
La CI usa Docker PostgreSQL/MinIO/AI aislados y datos ficticios.

El APK debug usa http://10.0.2.2:18080 para el emulador: no es una versión para
Play Store ni valida cámara/selector de PDF en un teléfono real. Un teléfono requiere
una API HTTPS accesible y una compilación con esa URL. Los recursos externos de
COMPLETION_STATUS.md y RECOVERY_AND_EXTERNAL_SERVICES.md siguen pendientes:
servidor/dominio, SMTP, CA/OCSP/CRL/TSA/custodia, blockchain real, Gemini/Firebase
cuando se usen, recuperación en otro host y piloto institucional.

## Referencias del cambio Android

[ApplicationExtension, Android Developers](https://developer.android.com/reference/tools/gradle-api/9.0/com/android/build/api/dsl/ApplicationExtension)
y [API de TaskExecutionGraph, Gradle](https://docs.gradle.org/current/javadoc/org/gradle/api/execution/TaskExecutionGraph.html).
También se revisó el código de FlutterPlugin/AgpCommonExtensionWrapper del SDK
Flutter 3.50.0 instalado para comprobar la compatibilidad del DSL nuevo.
