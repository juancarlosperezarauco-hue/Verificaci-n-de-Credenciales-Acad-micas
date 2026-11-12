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
Estado: Codex completó backend y validación (9 pruebas PostgreSQL, 8 Angular).
Frontend administrativo pendiente de entrega de Antigravity. Ver handoffs/codex.md.
