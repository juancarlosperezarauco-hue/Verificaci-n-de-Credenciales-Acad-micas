# Handoff — barra horizontal de módulos

Fecha 2026-11-15, base 773b6b4, rama codex/rbac-superadmin.

Se corrige la barra que cortaba el primer módulo y superponía el nombre institucional.
La marca queda a la izquierda, con ancho limitado/ellipsis y nombre completo en title;
los módulos quedan alineados a la derecha. Se elimina overflow-x-auto de la navegación.

Accesos principales visibles; Organización agrupa revisión de identidad, perfil,
imagen, equipo/roles y auditoría según permisos. SUPER_ADMIN usa Administración.
Cuenta reúne seguridad/sesiones y cierre de sesión. Menos de 1100 px: botón Menú
con todas las opciones autorizadas, sin franja horizontal ni nombre superpuesto.
Se conservan los colores de cada pantalla y no se añade botón Inicio.

La navegación se separa en componente y modelo; App conserva branding/logout.
Disclosure con aria-expanded/controls, links normales, foco visible, Escape con
retorno de foco, cierre por clic exterior/navegación y limpieza de suscripción.
No cambia la arquitectura backend, RBAC, contratos API ni migraciones V1–V18.

Validación: 69 pruebas Angular correctas y build production. Tres nuevas comprueban
Escape/foco, clic exterior/cambio de ruta y permisos retirados con menú abierto.
Backend sin cambios; última suite 96 correcta, total de referencia 165.
Principal frontend reconstruido/recreado, bundle verificado en localhost:4300;
API 18080 permanece UP y no se modifica ningún registro/volumen ni otro proyecto.

QA visual no ejecutada: herramienta integrada falla al iniciar con `failed to write
kernel assets`/ruta no encontrada. No se presentan tests como captura visual.

Archivos: frontend/src/app/app.ts, core/navigation/navigation.model.ts,
navigation.component.ts, navigation.component.spec.ts; README.md,
docs/PRODUCT_BACKLOG_V3.md, docs/VALIDATION_V3.md y handoffs/codex*.md.
Logs navigation-build.log, frontend/navigation-test.log y navigation-runtime.log
ignorados; no se incluyen secretos. Sin migraciones ni endpoints nuevos.

Commit final: `git log -1 --format=%H -- handoffs/codex-navigation.md` y descripción
del PR #1. Publicar solo codex/rbac-superadmin; no integrar automáticamente a main.
