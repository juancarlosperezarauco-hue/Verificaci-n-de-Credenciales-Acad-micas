# Trabajo entre dos IAs

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
Validación: 9 pruebas backend PostgreSQL, 15 Angular, recorrido HTTP completo con
PDF real en MinIO y verificación de suspensión/restauración.
La versión integrada está ejecutándose en Docker local (web 4300).
La herramienta de revisión visual no inició: no se entregan capturas ni se afirma QA visual.
La rama de entrega sigue siendo codex/rbac-superadmin; main no se modifica.
