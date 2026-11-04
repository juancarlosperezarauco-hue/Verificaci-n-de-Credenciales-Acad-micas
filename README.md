# Verificación de Credenciales Académicas

Plataforma multitenant para que instituciones educativas creen, emitan, consulten y revoquen credenciales académicas verificables mediante código público y QR.

## Primera entrega funcional

- Registro de una organización con su administrador.
- Inicio de sesión con JWT y control de roles `ADMIN`, `ISSUER` y `VIEWER`.
- Separación de usuarios y credenciales por organización.
- Creación de credenciales en borrador.
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

## Crear la primera organización

Con los contenedores iniciados, ejecuta en PowerShell:

```powershell
$body = @{
  organizationName = "Universidad Demo"
  organizationSlug = "universidad-demo"
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
2. Plantillas visuales y generación de PDF institucional.
3. Integración blockchain de producción y explorador de transacciones.
4. Notificaciones por correo y Firebase.
5. Reportes Excel, Word y PDF, dashboard estadístico y auditoría.
6. Aplicación móvil Flutter y planes de suscripción con Stripe.
