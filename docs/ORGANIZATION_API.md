# Contrato de organizaciones para Antigravity

Actualización del registro: ya no se solicitan niveles educativos en el asistente.
`profile.educationLevels` conserva el contrato de lista para compatibilidad, pero
acepta `[]` al registrar o editar una institución. El frontend envía esa lista
vacía cuando no existen niveles; no se infieren a partir del tipo de institución.
Los valores previamente guardados se conservan y pueden editarse en el perfil.
Registro general: los datos fiscales y de acreditación se pueden completar más
adelante. V13 permite identificadores fiscales y licencias educativas nulos.

Codex asume las tareas backend de DeepSeek. Todo cambio backend pertenece a Codex.
La rama de trabajo sigue siendo codex/rbac-superadmin; migraciones V12/V13.

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
taxId y educationLicense son opcionales. Valores ausentes, vacíos o compuestos solo
de espacios se almacenan como null. Cuando se informan, se normalizan sin espacios
y en mayúsculas y se conserva la unicidad por país.
ministryRegistration/accreditation opcionales.
El correo del representante es la cuenta inicial TENANT_ADMIN.
Los datos de identificación, ubicación/contacto y representante siguen siendo
obligatorios. educationLevels admite lista vacía. Las ocho clases institucionales
comparten el mismo formulario y contrato, incluida OTRO.

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
