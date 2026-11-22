# Landing clara y profesional — 22/11/2026

Rama: codex/landing-refresh, desde main 2d80150. Cambios de presentación pública;
no se cambian API, migraciones, permisos ni el panel autenticado.

## Resultado

- Portada clara con mensajes directos, contraste correcto y accesos a institución,
  estudiante y consulta de credenciales.
- Certificado ilustrativo identificado como ejemplo; no se presentan clientes,
  cifras, identidades verificadas ni certificaciones reales inventadas.
- Recorridos por audiencia, proceso de cuatro pasos, explicación de consulta
  registral/comprobación de PDF y preguntas frecuentes nativas con teclado.
- Componentes separados para la vista de certificado y FAQ. Se conserva el menú
  lateral y la navegación existentes.
- Ajuste de alineación, contraste del logo y tamaño de acciones del encabezado
  público. Las reglas nuevas se limitan a public-shell.

## Archivos

frontend/src/app/features/landing/landing.page.{ts,html,css};
credential-preview.component.{ts,css}; landing-faq.component.{ts,css};
frontend/src/app/app.css; e2e/tests/public-landing.spec.ts; e2e/public.config.ts.

## Verificación

Las 114 pruebas del frontend aprobaron. El build productivo conserva los límites
de tamaño existentes. La prueba de navegador comprueba los tres accesos, apertura
de FAQ con teclado y ausencia de desbordamiento a 320, 412 y 1440 píxeles.
Las capturas se guardan en los resultados ignorados de Playwright y se revisan
como imágenes. La vista previa integrada no respondió; esa comprobación manual
no se atribuye como realizada.

    cd e2e
    npx playwright test --config public.config.ts

Esta configuración solo ejecuta la prueba pública anónima contra localhost:4300.
La configuración audit original mantiene su origen restringido y ejecuta también
esa prueba en CI. No se generan cuentas ni se modifica la base principal.

El frontend local se actualiza mediante compose.prebuilt.yml, únicamente para el
servicio frontend, conservando backend, datos y claves.

## Resultado de entrega

Las dos pruebas públicas locales aprobaron: navegación, FAQ con teclado y capturas
sin desbordamiento en 320, 412 y 1440 px. Se revisaron las capturas resultantes.
Build productivo sin advertencias de tamaño. Frontend local actualizado y saludable.

La CI 45037797928 aprobó los cinco jobs del commit
902e914a044c00657fadd0c8e4c1e37c122f3d4e: 156 PostgreSQL + 114 Angular +
11 OCR nativo + 14 pruebas integradas + 5 Flutter = 300 aprobadas;
dos duplicados móviles de servicio omitidos. Builds/análisis/APK aprobados.
La actualización posterior de este documento no modifica el código probado.

https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas/actions/runs/45037797928
