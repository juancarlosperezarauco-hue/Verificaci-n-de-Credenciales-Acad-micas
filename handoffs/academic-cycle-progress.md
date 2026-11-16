# Trabajo autorizado en curso: puntos 1–9

Base cbe2989, rama codex/rbac-superadmin; contrato docs/ACADEMIC_CYCLE_CONTRACT.md.
No modificar otro checkout ni main. No delegar agentes sin petición explícita.

Estado inicial: 96 pruebas backend +70 web, V18. Árbol limpio.

- [x] 1 Expediente institucional backend/web/pruebas.
- [x] 2 Solicitudes propias/requisitos backend/web/pruebas.
- [x] 3 Revisión/aprobación separadas y emisión vinculada.
- [x] 4 Plantillas versionadas/previsualización.
- [x] 5 Validación PDF, QR, idempotencia y reconciliación.
- [x] 6 Firma CMS real y configuración cifrada/prueba local explícita.
- [x] 7 Verificador registral/integridad/firma/emisor.
- [x] 8 Correcciones con sustitución y original preservado.
- [x] 9 Avatar, disputa revisable y retención/borrado seguro de evidencia.
- [x] Suites completas, smoke integrado, backup/actualización principal.
- [x] Manual de prueba, handoff, commits/push/PR.

Entorno previo: repo en Credenciales-codex-rbac; web4300/API18080; HRcore usa8080
y no se toca. Docker CLI en AppData/Local/Programs/DockerDesktop/resources/bin.
Principal Compose verificacion-credenciales con override privado security02-runtime.yml;
auditoría credentials-v3-audit/compose.audit.yml con env privado audit-compose.env,
API18081/web4301/Mailpit18125. PG de tests codex-rbac-postgres-check:15432.
Herramientas de navegador/escritorio fallaban al arrancar por kernel assets ausentes;
no inventar QA visual ni usar su resultado como excusa para omitir pruebas funcionales.

Cierre: puntos 1–9 implementados y probados. 120 backend PostgreSQL y 88 web correctas; H2 119 correctas y una omitida propia de PostgreSQL. Smoke HTTP real PASS, upgrade V18→V21, principal actualizado en web4300/API18080 con datos conservados. Backup PG/configuración privado; no ensayo de restauración integral. Ver docs/ACADEMIC_CYCLE_DELIVERY.md y handoffs/codex-academic.md. QA visual manual pendiente. Código publicado hasta c6e317a; cierre documental en HEAD.
