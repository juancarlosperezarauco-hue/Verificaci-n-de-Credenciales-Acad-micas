# Handoff — menú lateral izquierdo

Fecha 2026-11-15, base c5fe166, rama codex/rbac-superadmin.

La navegación actual es un panel lateral izquierdo con opciones en lista vertical.
Sustituye la barra horizontal del incremento anterior tras aclaración del usuario.
Botón Menú antes del logo/nombre institucional; panel blanco de hasta 320 px,
altura de ventana, secciones Módulos/Organización o Administración/Mi cuenta.
El contenido tiene scroll vertical independiente y Cerrar sesión queda en el pie.

Mismo comportamiento en escritorio y móvil: abrir/cerrar con botón, X, Escape,
fondo exterior o al navegar. Angular CDK Dialog gestiona foco atrapado/restaurado,
bloqueo de scroll del fondo y capa modal. Se conserva modelo de permisos y logout.
El nombre institucional queda acotado; no se añade botón Inicio.

Validación: 70 pruebas Angular correctas y build production. Cuatro casos de drawer
verifican posición izquierda real (x=0) y distribución vertical en ChromeHeadless,
foco/Escape, clic exterior/navegación, permisos dinámicos y logout solo explícito.
Backend sin cambios; última suite 96 correcta (total de referencia 166), V18.
Frontend principal actualizado en localhost:4300, bundle comprobado; API18080 UP.
Sin cambios de datos/volúmenes, migraciones, endpoints ni otros proyectos.

No hay capturas de revisión visual manual: las herramientas de interfaz fallaron
al iniciar en intentos previos. No se equiparan las pruebas de geometría/DOM a QA visual.

Archivos: frontend/src/app/app.ts, core/navigation/navigation.component.ts,
navigation-drawer.component.ts, navigation.component.spec.ts; README.md,
docs/PRODUCT_BACKLOG_V3.md, docs/VALIDATION_V3.md y handoffs/codex*.md.
Logs sidebar-test.log, sidebar-build.log y sidebar-runtime.log ignorados.
Commit final: git log -1 --format=%H -- handoffs/codex-navigation.md y PR #1.
Publicar únicamente codex/rbac-superadmin, sin integrar automáticamente a main.