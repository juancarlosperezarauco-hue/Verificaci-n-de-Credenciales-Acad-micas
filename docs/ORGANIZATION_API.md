# Contrato de organizaciones para Antigravity

Codex asume las tareas backend de DeepSeek. Todo cambio backend pertenece a Codex.
La rama de trabajo sigue siendo codex/rbac-superadmin; migración V12.

POST /api/auth/organizations/register (público):
{organizationSlug,password,profile:{legalName,institutionType,educationLevels:[],
country,region,address,phone,contactEmail,website,domain,taxId,educationLicense,
ministryRegistration,accreditation,representative:{fullName,documentType,
documentNumber,position,email,phone}}}.
Respuesta: AuthResponse existente con TENANT_ADMIN y tenantId.

institutionType conserva los códigos existentes: UNIVERSIDAD, COLEGIO, INSTITUTO,
ACADEMIA, CENTRO_FORMACION, OTRO y añade ESCUELA, INSTITUTO_TECNICO.
educationLevels: INICIAL, PRIMARIA, SECUNDARIA, SUPERIOR, TECNICO.
country: código ISO de dos letras; obligatorio.
website opcional, solo http/https. domain opcional, nombre de dominio sin protocolo.
taxId y educationLicense se normalizan sin espacios y en mayúsculas; unicidad por país.
ministryRegistration/accreditation opcionales.
El correo del representante es la cuenta inicial TENANT_ADMIN.
Todos los otros campos y al menos un nivel son obligatorios.

GET /api/institution/details: detalle legal; 404 si perfil histórico incompleto.
GET /api/institution/status: nombre, slug, tipo y estado de la organización propia.
PUT /api/institution/details: mismo objeto profile; valida unicidad y actualiza.
Permisos ORGANIZATION_VIEW/ORGANIZATION_UPDATE respectivamente.
No cambia la cuenta de acceso al editar el correo del representante.
GET /api/platform/organizations/{id}/details: detalle para SUPER_ADMIN.

El registro anterior /api/auth/register sigue disponible por compatibilidad.
No se impone aprobación previa al registro en este incremento (estado ACTIVE).
La superadministración puede observar/suspender/desactivar posteriormente.

Errores: 400 validación, 401 login, 403 permisos, 404 recurso ajeno o no existente,
409 identificación fiscal o licencia duplicada. Nunca exponer hashes de contraseña.
