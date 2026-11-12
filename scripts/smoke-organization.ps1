param(
 [string]$ApiBase = 'http://127.0.0.1:18081',
 [string]$SuperEmail = $env:REVIEW_SUPERADMIN_EMAIL,
 [string]$SuperPassword = $env:REVIEW_SUPERADMIN_PASSWORD
)
$ErrorActionPreference = 'Stop'
function Invoke-Api([string]$Method,[string]$Path,$Body=$null,[string]$Token='') {
 $request = @{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json'}
 if($Token){$request.Headers=@{Authorization='Bearer '+$Token}}
 if($null -ne $Body){$request.Body=$Body|ConvertTo-Json -Depth 12}
 Invoke-RestMethod @request
}
function Assert-Value([bool]$Condition,[string]$Message){if(-not $Condition){throw $Message}}
$suffix=[guid]::NewGuid().ToString('N')
$password='Smoke-'+[guid]::NewGuid().ToString('N')
$slug='smoke-'+$suffix
$profile=@{
 legalName='Instituto de prueba '+$suffix.Substring(0,6);institutionType='INSTITUTO_TECNICO'
 educationLevels=@('TECNICO');country='BO';region='Santa Cruz';address='Av. Educación 100'
 phone='70000000';contactEmail='contacto-'+$suffix+'@example.test';website='https://instituto.example.test'
 domain='instituto.example.test';taxId=$suffix;educationLicense='EDU-'+$suffix
 ministryRegistration='REG-'+$suffix;accreditation='Registro de prueba local'
 representative=@{fullName='Administradora de prueba';documentType='CI';documentNumber=$suffix
 position='Directora';email='admin-'+$suffix+'@example.test';phone='70000001'}
}
$admin=Invoke-Api POST '/api/auth/organizations/register' @{organizationSlug=$slug;password=$password;profile=$profile}
Assert-Value ($admin.role -eq 'TENANT_ADMIN') 'No se creó el administrador institucional'
$own=Invoke-Api GET '/api/institution/status' $null $admin.token
Assert-Value ($own.id -eq $admin.tenantId) 'El estado no pertenece al tenant autenticado'
$role=Invoke-Api POST '/api/access/roles' @{name='Emisor de prueba';permissions=@('ORGANIZATION_VIEW','STUDENT_VIEW','STUDENT_LINK','CREDENTIAL_VIEW','CREDENTIAL_CREATE','CREDENTIAL_ISSUE')} $admin.token
$employeeEmail='docente-'+$suffix+'@example.test'
$employee=Invoke-Api POST '/api/access/users' @{fullName='Docente de prueba';email=$employeeEmail;password=$password;roleIds=@($role.id)} $admin.token
$staff=Invoke-Api POST '/api/auth/login' @{organizationSlug=$slug;email=$employeeEmail;password=$password}
$student=Invoke-Api POST '/api/auth/student/register' @{email='estudiante-'+$suffix+'@example.test';password=$password}
$studentProfile=Invoke-Api PATCH '/api/me/profile' @{fullName='Estudiante de prueba';country='BO';department='Santa Cruz';documentType='CI';documentNumber=$suffix;dateOfBirth='2001-05-12'} $student.token
$studentProfile=Invoke-Api GET '/api/me/profile' $null $student.token
$linked=Invoke-Api POST '/api/students/redeem' @{code=$studentProfile.searchCode} $staff.token
Assert-Value ($linked.id -eq $studentProfile.id) 'La vinculación no corresponde al estudiante'
$batch=Invoke-Api POST '/api/credentials/batch' @{studentIds=@($studentProfile.id);credentialType='CERTIFICADO';title='Formación académica de prueba'} $staff.token
$credential=@($batch)[0]
$generated=Invoke-Api POST ('/api/credentials/'+$credential.id+'/generate-document') @{} $staff.token
Assert-Value ([bool]$generated.documentHash) 'No se generó el PDF en almacenamiento real'
$download=Invoke-WebRequest -Uri ($ApiBase+'/api/credentials/'+$credential.id+'/document') -Headers @{Authorization='Bearer '+$staff.token}
Assert-Value ($download.StatusCode -eq 200) 'No se descargó el certificado'
$issued=Invoke-Api PATCH ('/api/credentials/'+$credential.id+'/issue') @{} $staff.token
$verified=Invoke-Api GET ('/api/public/credentials/'+$issued.verificationCode)
Assert-Value ($verified.status -eq 'ISSUED') 'La verificación pública no reconoce el certificado emitido'
$history=Invoke-Api GET '/api/me/credentials' $null $student.token
Assert-Value (@($history).Count -ge 1) 'El certificado no aparece en el expediente global'
Invoke-Api PATCH ('/api/credentials/'+$credential.id+'/revoke') @{reason='Fin de prueba de integración'} $admin.token | Out-Null
$revoked=Invoke-Api GET ('/api/public/credentials/'+$issued.verificationCode)
Assert-Value ($revoked.status -eq 'REVOKED') 'La revocación no se refleja públicamente'
if($SuperEmail -and $SuperPassword){
 $root=Invoke-Api POST '/api/auth/platform/login' @{email=$SuperEmail;password=$SuperPassword}
 Invoke-Api PATCH ('/api/platform/organizations/'+$admin.tenantId+'/status') @{status='SUSPENDED';reason='Prueba de suspensión'} $root.token | Out-Null
 $blocked=$false
 try {Invoke-Api GET '/api/credentials' $null $staff.token | Out-Null}catch{if([int]$_.Exception.Response.StatusCode -eq 403){$blocked=$true}else{throw}}
 Assert-Value $blocked 'Un token existente continuó operando tras suspender la organización'
 Invoke-Api PATCH ('/api/platform/organizations/'+$admin.tenantId+'/status') @{status='ACTIVE';reason='Restauración después de prueba'} $root.token | Out-Null
 Invoke-Api GET '/api/credentials' $null $staff.token | Out-Null
}
[pscustomobject]@{result='PASS';organizationId=$admin.tenantId;organizationSlug=$slug;studentId=$studentProfile.id;credentialId=$credential.id;verificationCode=$issued.verificationCode;checks='Registro, roles, usuario, estudiante, vinculación, PDF real, emisión, expediente, verificación y revocación'}
