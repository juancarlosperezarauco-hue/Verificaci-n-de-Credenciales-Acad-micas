# Estado de los 15 puntos

Rama codex/rbac-superadmin. Recursos declarados por el usuario: todavía sin
proveedores externos, dominio ni institución piloto. No se marca el proyecto
completo por tener configuración o pruebas ficticias.

| Punto | Desarrollo local | Para cerrarlo |
| --- | --- | --- |
| 1. Pruebas integradas y visuales | Suites PostgreSQL/Angular y navegador escritorio/móvil | 8 pruebas del navegador final aprobadas; revisión del usuario pendiente |
| 2. Docker | Límites de recursos, readiness/liveness y arranque aislado | Verificación final principal y capacidad del servidor |
| 3. Notificaciones | Bandeja privada, lectura y outbox cifrada/reintentos | SMTP real y destinatarios autorizados |
| 4. Reportes | PDF y Excel filtrados, descarga y permisos | Aceptación de formatos de la institución |
| 5. Estadísticas | Agregados institucionales/globales y panel acotado | Prueba de carga con volumen esperado |
| 6. Expediente | Paginación y filtros institución/año/tipo en servidor | Piloto con historial real autorizado |
| 7. Integración académica | CSV, preview, lote de 24 horas y aplicación idempotente | Conector del sistema académico que elija la institución |
| 8. Firma productiva | CMS institucional existente y respaldo probado de bóveda | CA, OCSP/CRL, TSA y custodia productiva todavía pendientes |
| 9. Seguridad productiva | Perfil estricto, TLS preparado y secretos independientes | Dominio/servidor, revisión de seguridad y pérdida total de factores |
| 10. Recuperación | Snapshot cifrado y restauración de BD/MinIO/firma/evidencia comprobados | Ensayo en otro host y custodia externa de recuperación |
| 11. CI y operación | Workflow backend/frontend/OCR/navegador/Android y chequeo JSON de salud | Resultado CI, alertas y despliegue con entornos protegidos |
| 12. Blockchain real | Se conserva el puerto de integración; pending no representa una transacción | Selección de red/RPC/wallet, implementación y validación reales |
| 13. OCR/IA | OCR privado con alertas explicadas y decisión humana; Gemini opcional | Repetir smoke HTTP tras ajuste HTTP/1.1; cuenta Gemini si se usarán resúmenes |
| 14. Android | Cliente Flutter de estudiante, QR y guardado nativo de PDF | Análisis y 5 pruebas Flutter aprobados; APK CI, teléfono, dominio y firma pendientes |
| 15. Piloto | Matriz y procedimiento en ACCEPTANCE_PILOT.md | Institución, responsables y aceptación efectiva |

RECOVERY_AND_EXTERNAL_SERVICES.md detalla qué recursos hacen falta. No pegar
contraseñas, semillas, claves privadas ni service accounts en el chat.


Publicación solicitada en main el 16/11/2026. El usuario revisará los fallos después.
La ejecución completa de CI de las últimas correcciones sigue pendiente de confirmar;
no se debe interpretar la publicación como aprobación de producción.
