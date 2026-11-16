# Piloto y aceptación

Estado: sin institución ni responsables designados. Este documento prepara el
ensayo; no representa aprobación institucional. Los datos de las pruebas actuales
son ficticios, en un entorno audit separado.

## Responsables que faltan

Una persona responsable de plataforma, un administrador de la institución, un
revisor distinto al aprobador y estudiantes que consientan el piloto. Se acuerdan
dominio, correo de prueba, tipos de documento, certificado institucional, custodia
de claves, conservación y procedimiento de soporte antes de usar datos reales.

## Escenarios de aceptación

| Escenario | Resultado esperado |
| --- | --- |
| Registrar estudiante solo con correo/contraseña | El perfil se completa después; la identidad es global |
| Vincularlo a colegio y universidad | Una cuenta, dos vínculos; cada institución ve solo su expediente |
| Retirar autorización | Se impide acceso institucional; se conserva el historial personal |
| Crear rol personalizado | Acceso limitado al tenant, módulo y acción asignados |
| Usar otro tenant o estudiante | Rechazo sin divulgar documentos ni datos personales |
| Solicitar, observar, corregir y aprobar | Historial de estados y separación entre revisor y aprobador |
| Emitir dos veces con la misma clave | Un solo efecto, un documento y una notificación de emisión |
| Cambiar perfil durante un trámite | El trámite desactualizado no se emite |
| PDF con QR y firma | QR resoluble; firma de prueba visible si corresponde |
| Consultar código sin PDF | Registro consultado; archivo y firma sin comprobar |
| Alterar el PDF | La comparación detecta diferencia; no valida el documento alterado |
| Corregir credencial emitida | Reemplazo aprobado; original preservado y revocado al emitir |
| OCR | Texto y alertas de apoyo; ninguna decisión automática de identidad |
| Notificación privada | Solo destinatario; marcar lectura es una acción explícita |
| Reporte y Excel | Filtros aplicados, tenant aislado y fórmulas de entrada inertes |
| Importar CSV | Preview sin matrículas; aplicación atómica, sin nuevas identidades |
| Menú y móvil | Navegación vertical, foco correcto, sin desbordamiento de página |
| Android | Login, token seguro, filtros, QR y PDF guardado donde el usuario elija |
| Restauración | BD, archivos y claves recuperables en entorno aislado |
| Incidente o pérdida de factores | Procedimiento humano, responsables y evidencias acordados |

## Evidencia que se recoge

Fecha, escenario, resultado esperado/obtenido, captura sin datos sensibles, defecto,
responsable de corrección y repetición. Se registran tiempos con el volumen real
esperado, conectividad del teléfono y errores de entrega del correo. No se promete
un rendimiento sin medirlo.

La aceptación se firma únicamente cuando los responsables prueben estos escenarios,
se resuelvan defectos críticos y se hayan configurado servicios reales, recuperación
fuera del equipo y distribución Android. No se atribuye validez legal a la firma
de demostración ni al estado administrativo de una organización.
