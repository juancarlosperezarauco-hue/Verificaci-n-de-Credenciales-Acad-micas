# Verificación de Credenciales Académicas

Plataforma multitenant para que instituciones educativas creen, emitan, consulten y revoquen credenciales académicas verificables mediante código público y QR.

## Flujo funcional

1. Se registra una institución como universidad, colegio, instituto, academia, centro de formación u otro tipo.
2. El administrador ingresa al espacio privado de su institución.
3. El estudiante crea una cuenta personal global en `/student/register` y accede en `/student/login`.
4. El estudiante comparte su código de búsqueda `EST-…`. La institución lo pega en Estudiantes → Buscar y registrar, consulta sus datos básicos y lo vincula a su padrón para emitirle documentos.
5. Selecciona uno o varios estudiantes, genera el PDF con logo y colores institucionales o adjunta un PDF propio.
6. Emite la credencial y comparte su código o QR de verificación.

## Organización modular

- `auth` y `tenant`: registro, autenticación y aislamiento de instituciones.
- `identity`: cuenta e identidad global del estudiante, independientes de las instituciones.
- `consent`: autorización por institución, códigos de un solo uso, vencimiento y registro de acceso.
- `student`: consulta de estudiantes que han autorizado a la institución; entidades anteriores conservadas para compatibilidad histórica.
- `credential`: creación, emisión, revocación y verificación de documentos vinculados a estudiantes.
- `storage`: almacenamiento de archivos mediante MinIO/S3.
- `blockchain`: puerto de anclaje desacoplado de la emisión.
- Frontend por funcionalidades: `register`, `login`, `students`, `credentials`, `dashboard` y `verify`.

Las cuentas de estudiantes son globales. Las credenciales y autorizaciones mantienen su institución propietaria.
Cada estudiante tiene un código de búsqueda aleatorio reutilizable, distinto de su identificador interno.
Compartirlo permite a una institución autenticada registrarlo; no revela su correo ni historial académico.
El código permanece estable hasta que el estudiante lo cambie. Al cambiarlo se invalidan búsquedas futuras con el código anterior,
pero se conservan los vínculos y documentos existentes. Solo el estudiante puede ver o cambiar su propio código.
El registro repetido en la misma institución es idempotente. Una autorización retirada no se reactiva con el mismo código.
Cada nueva vinculación registra institución, usuario institucional y fecha en el historial de autorizaciones.
También se mantiene la alternativa de generar códigos temporales destinados a una institución específica.
El endpoint de canje solo entrega identificador interno, nombre, país, tipo y número de documento.
Los códigos usan UUID aleatorio (122 bits de aleatoriedad), se guardan como SHA-256, caducan en 15 minutos y se consumen con bloqueo transaccional.
El vínculo usado permanece activo hasta que el estudiante retire esa autorización; la expiración limita el canje inicial.
Retirarla bloquea nuevas consultas y emisiones, pero no borra las credenciales emitidas ni información previamente consultada.
El historial personal muestra documentos emitidos y revocados de todas las instituciones, excluyendo borradores.
Las tablas anteriores se conservan en V3; no se fusionan personas por nombre ni se asignan documentos históricos a cuentas sin comprobación.
La unicidad se aplica a país + tipo + número de documento normalizados y al correo. La identidad es declarada por el usuario:
la comprobación documental, recuperación de cuenta y resolución de identidades duplicadas con diferentes documentos requieren un flujo posterior.

## Primera entrega funcional

- Registro de una organización con su administrador.
- Inicio de sesión con JWT y control de roles `ADMIN`, `ISSUER`, `AUDITOR` y `STUDENT`.
- Separación de usuarios y credenciales por organización.
- Creación de credenciales en borrador.
- Perfil institucional con logo, tres colores, estilo moderno/clásico/minimalista y datos del firmante.
- Documento de respaldo institucional en MinIO con huella SHA-256.
- Emisión por lotes para hasta 100 estudiantes y generación automática de certificados PDF con OpenPDF.
- Carga del documento PDF en MinIO y cálculo de su hash SHA-256.
- Emisión y anclaje mediante un puerto blockchain; la implementación actual es un adaptador de desarrollo reemplazable.
- Generación de QR, consulta pública y revocación con motivo.
- Panel web responsive, API documentada con OpenAPI y migraciones Flyway.
- Microservicio FastAPI preparado para funciones con Google Gemini.

## Tecnologías incluidas

- Angular 20, TypeScript 5.8, Tailwind CSS 4 y RxJS.
- Java 21, Spring Boot 3.5, Spring Security, JPA/Hibernate y JWT.
- PostgreSQL 16 y Flyway.
- MinIO para documentos, Mailpit para correo local y FastAPI para IA.
- Docker Compose para el entorno de desarrollo completo.

> El Compose fija una compilación comunitaria de MinIO basada en la última versión corregida de Community Edition, ya que el proyecto oficial dejó de distribuir imágenes precompiladas.

## Ejecución con Docker

1. Copia `.env.example` como `.env` y cambia los secretos.
2. Inicia Docker Desktop.
3. Ejecuta:

```powershell
docker compose up --build
```

Servicios disponibles:

- Aplicación: http://localhost:4200
- API y Swagger: http://localhost:8080/swagger-ui.html
- Consola MinIO: http://localhost:9001
- Mailpit: http://localhost:8025
- Servicio IA: http://localhost:8000/docs

### Despliegue con compilación local

Para reutilizar Java y Node instalados y construir imágenes pequeñas:

```powershell
cd backend
.\mvnw.cmd verify
cd ../frontend
npm ci
npm run build
cd ..
docker compose -f docker-compose.yml -f compose.prebuilt.yml up --build -d
```

Los puertos alternativos de este equipo están en el archivo local `.env` (excluido de Git).
En este equipo: web 4300, MinIO 9100/9101 y Mailpit 1125/8125.
Las copias de seguridad locales se excluyen de Git en `.local-backups/`.

## Crear la primera organización

Con los contenedores iniciados, ejecuta en PowerShell:

```powershell
$body = @{
  organizationName = "Universidad Demo"
  organizationSlug = "universidad-demo"
  institutionType = "UNIVERSIDAD"
  email = "admin@universidad.edu"
  password = "Cambiar123!"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/auth/register" `
  -ContentType "application/json" `
  -Body $body
```

Después ingresa en la aplicación con el slug, correo y contraseña anteriores.

## Ejecución sin contenedores de aplicación

Se requiere PostgreSQL y MinIO activos. Luego:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

En otra terminal:

```powershell
cd frontend
npm install
npm start
```

## Estructura

```text
backend/       API REST y reglas de negocio
frontend/      Aplicación web Angular
ai-service/    Microservicio FastAPI/Gemini
docker-compose.yml
```

## Próximos módulos

1. Gestión de usuarios, invitaciones y permisos por organización.
2. Editor avanzado de plantillas con posiciones, tipografías y firmas digitales configurables.
3. Integración blockchain de producción y explorador de transacciones.
4. Notificaciones por correo y Firebase.
5. Reportes Excel, Word y PDF, dashboard estadístico y auditoría.
6. Aplicación móvil Flutter y planes de suscripción con Stripe.
