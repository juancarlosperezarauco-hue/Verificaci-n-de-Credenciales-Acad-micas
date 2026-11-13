# Trabajo entre dos IAs

## Continuación auditada V3 (2026-11-13)

Codex revisó el prompt maestro contra la arquitectura existente y reprodujo las
35 pruebas. La corrección de integridad documental añade cinco casos: 16 backend
+ 24 frontend, además de smokes reales aislados. V1–V13 sin modificaciones.
Estado y siguiente sprint: docs/PRODUCT_BACKLOG_V3.md y docs/SPRINT_01_V3.md.
Handoff: handoffs/codex-v3.md. No se han iniciado agentes adicionales ni integrado
trabajo pendiente del otro checkout. Docker principal sin nuevo despliegue.

## Reparto histórico

Codex: backend completo, seguridad, RBAC, superadmin, registro institucional,
validaciones, migraciones, pruebas e integración. Rama codex/rbac-superadmin.

Antigravity: frontend Angular, formularios institucionales, panel superadmin,
usuarios/roles y pruebas visuales. Rama antigravity/organization-ui.

DeepSeek quedó detenido. No hay rama remota ni handoff suyo en los checkouts
disponibles al revisar origin; si aparece un parche se revisará antes de integrarlo.

Ambos agentes deben leer docs/RBAC_CONTRACT.md y docs/ORGANIZATION_API.md.
Codex es propietario de todos los archivos backend y migraciones V11/V12.
Antigravity es propietario de nuevas pantallas frontend. Cambios existentes mínimos
de compatibilidad TENANT_ADMIN en dashboard/institution ya pertenecen a Codex.
Entregar commits, pruebas y handoffs; no integrar directamente en main.
Estado actualizado: Antigravity también se detuvo antes de implementar pantallas.
Codex completó su alcance frontend y lo conectó al backend real.
Validación: 11 pruebas backend PostgreSQL, 24 Angular, recorrido HTTP completo con
PDF real en MinIO y verificación de suspensión/restauración.
La versión integrada está ejecutándose en Docker local (web 4300).
La revisión visual se completó en escritorio y móvil; evidencias y correcciones
documentadas en docs/UI_DELIVERY.md y docs/evidence/.
La rama de entrega sigue siendo codex/rbac-superadmin; main no se modifica.
