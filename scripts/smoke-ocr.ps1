param([Parameter(Mandatory)][string]$FixtureFile,[Parameter(Mandatory)][string]$ImageFile)
$ErrorActionPreference='Stop'
# Only fictional accounts in the isolated audit environment, never the main application.
$base='http://127.0.0.1:18081'
$fixture=Get-Content -LiteralPath (Resolve-Path -LiteralPath $FixtureFile).Path -Raw|ConvertFrom-Json
if($fixture.studentEmail -notmatch '@[^@]+\.test$' -or $fixture.adminEmail -notmatch '@[^@]+\.test$'){throw 'Usa únicamente cuentas ficticias .test.'}
function Call([string]$Method,[string]$Path,$Body,[string]$Token=''){
 $args=@{Method=$Method;Uri=$base+$Path;Headers=@{Authorization='Bearer '+$Token};ContentType='application/json';TimeoutSec=40}
 if($null -ne $Body){$args.Body=$Body|ConvertTo-Json -Depth 5}
 Invoke-RestMethod @args
}
$student=Call POST '/api/auth/student/login' @{email=$fixture.studentEmail;password=$fixture.password}
$admin=Call POST '/api/auth/login' @{organizationSlug=$fixture.slug;email=$fixture.adminEmail;password=$fixture.password}
$available=Call GET '/api/institution/identity-analysis/status' $null $admin.token
if(!$available.configured){throw 'No está habilitado el OCR de audit.'}
$own=Call GET '/api/me/identity-verifications' $null $student.token
$tenant=$own.institutions|Where-Object {$_.slug -eq $fixture.slug}|Select-Object -First 1
if(!$tenant){throw 'El estudiante ficticio no está vinculado.'}
foreach($pending in @($own.submissions|Where-Object {$_.tenantId -eq $tenant.id -and $_.status -eq 'PENDING' -and !$_.withdrawnAt})){
 $null=Call POST ('/api/me/identity-verifications/'+$pending.id+'/withdraw') @{} $student.token
}
$file=Get-Item -LiteralPath (Resolve-Path -LiteralPath $ImageFile).Path
$backPath=Join-Path ([IO.Path]::GetTempPath()) ('ocr-back-'+[guid]::NewGuid().ToString('N')+$file.Extension)
Copy-Item -LiteralPath $file.FullName -Destination $backPath
try {
$submission=Invoke-RestMethod ($base+'/api/me/identity-verifications') -Method Post -Headers @{Authorization='Bearer '+$student.token} -Form @{tenantId=$tenant.id;accepted='true';front=$file;back=(Get-Item -LiteralPath $backPath)} -TimeoutSec 40
$analysis=Call POST ('/api/institution/identity-verifications/'+$submission.id+'/analysis') @{} $admin.token
if(!$analysis.human_review_required -or $analysis.engine -ne 'tesseract' -or $analysis.confidence -lt 0 -or $analysis.confidence -gt 1){throw 'Respuesta OCR inválida.'}
$after=Call GET '/api/me/identity-verifications' $null $student.token
$record=$after.submissions|Where-Object {$_.id -eq $submission.id}|Select-Object -First 1
if($record.status -ne 'PENDING'){throw 'El OCR modificó indebidamente el estado de identidad.'}
$denied=Invoke-WebRequest ($base+'/api/institution/identity-verifications/'+$submission.id+'/analysis') -Method Post -Headers @{Authorization='Bearer '+$student.token} -ContentType application/json -Body '{}' -SkipHttpErrorCheck -TimeoutSec 20
if([int]$denied.StatusCode -ne 403){throw 'El endpoint OCR debe rechazar al estudiante como revisor.'}
[pscustomobject]@{Result='PASS';Engine='tesseract';HumanDecisionPreserved=$true;StudentReviewDenied=$true}

}finally{if(Test-Path -LiteralPath $backPath){Remove-Item -LiteralPath $backPath}}
