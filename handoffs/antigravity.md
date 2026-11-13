# Asignación actualizada: dos IAs

Actualización: Codex retomó y completó esta asignación al agotarse los tokens
de Antigravity. Ver docs/UI_DELIVERY.md. El contenido siguiente queda como
registro histórico de la distribución de tareas, no como trabajo pendiente.
La revisión visual posterior también se completó. Las capturas con datos ficticios,
las rutas, los contratos usados y las pruebas finales están en docs/UI_DELIVERY.md.
Commit final de código integrado: fec11e8f6d7c07540af79f882f60a62b816187dd.

Repositorio: https://github.com/juancarlosperezarauco-hue/Verificaci-n-de-Credenciales-Acad-micas

Codex terminó el backend de RBAC, superadmin y onboarding que correspondía a DeepSeek.
No continúes suponiendo un backend mock: consume los contratos reales.

1. Ejecuta git fetch origin.
2. Crea tu worktree y rama antigravity/organization-ui tomando como base
   origin/codex/rbac-superadmin (o integra esa rama si ya empezaste desde main).
3. Lee docs/RBAC_CONTRACT.md, docs/ORGANIZATION_API.md y AI_WORKPLAN.md.
4. Desarrolla solo frontend:
   - login superadmin por /api/auth/platform/login;
   - listado, detalle e historial de organizaciones y acciones de estado;
   - asistente de registro completo y edición de datos legales/representante;
   - usuarios internos y roles con permisos;
   - menús, rutas y botones según /api/access/me.
5. Mantén el estudiante global separado de los usuarios del tenant.
6. Maneja 400/401/403/404/409 y los estados de carga.
7. Compila Angular, ejecuta pruebas y revisa escritorio/móvil.
8. Publica tu rama y reemplaza este archivo con tu entrega:
   archivos, rutas, contratos usados, pruebas, capturas, pendientes y commit.

Los formularios envían únicamente los campos documentados. Las modificaciones del
correo del representante no cambian automáticamente el correo de inicio de sesión.
Los registros históricos sin detalles reciben 404 y deben mostrar el formulario.

Codex conserva la integración mediante pull request; no publica directamente en main.
