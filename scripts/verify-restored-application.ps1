param([Parameter(Mandatory)][string]$RestoreReport,[Parameter(Mandatory)][string]$FixtureFile,[string]$Docker='docker')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $root
$compose=$null;$verified=$false;$payload=$null
try{
 $reportPath=(Resolve-Path -LiteralPath $RestoreReport).Path
 $folder=Split-Path $reportPath
 $allowed=[IO.Path]::GetFullPath((Join-Path $root '.local-backups/restores'))+[IO.Path]::DirectorySeparatorChar
 if(!$folder.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase)){throw 'El informe debe pertenecer a un ensayo local.'}
 $report=Get-Content -LiteralPath $reportPath -Raw|ConvertFrom-Json
 if($report.project -notmatch '^credentials-restore-[0-9a-f]{12}$' -or $report.result -ne 'PASS'){throw 'El ensayo previo no es válido.'}
 $fixture=Get-Content -LiteralPath (Resolve-Path -LiteralPath $FixtureFile).Path -Raw|ConvertFrom-Json
 if($fixture.studentEmail -notmatch '@[^@]+\.test$' -or $fixture.adminEmail -notmatch '@[^@]+\.test$'){throw 'La comprobación requiere cuentas ficticias .test.'}
 $payload=Join-Path $folder 'payload';$keys=Join-Path $payload 'keys.env'
 $compose=@('compose','-p',$report.project,'--env-file',$keys,'-f','compose.restore.yml','-f','compose.restore-app.yml')
 & $Docker @compose up -d --no-build --wait --wait-timeout 180 | Out-Null
 if($LASTEXITCODE -ne 0){throw 'No arrancó la aplicación restaurada.'}
 $port=(& $Docker @compose port backend 8080).Trim()
 if($port -notmatch '^127\.0\.0\.1:[0-9]+$'){throw 'El ensayo debe estar ligado a localhost.'}
 $base='http://'+$port
 function Call([string]$Method,[string]$Path,$Body,[string]$Token=''){
  $args=@{Method=$Method;Uri=$base+$Path;ContentType='application/json';TimeoutSec=30;Headers=@{Authorization='Bearer '+$Token}}
  if($null -ne $Body){$args.Body=$Body|ConvertTo-Json -Depth 5}
  Invoke-RestMethod @args
 }
 $student=Call POST '/api/auth/student/login' @{email=$fixture.studentEmail;password=$fixture.password}
 $profile=Call GET '/api/me/profile' $null $student.token
 $history=Call GET '/api/me/credentials' $null $student.token
 if(!($history|Where-Object {$_.verificationCode -eq $fixture.verificationCode})){throw 'No se recuperó el expediente esperado.'}
 $admin=Call POST '/api/auth/login' @{organizationSlug=$fixture.slug;email=$fixture.adminEmail;password=$fixture.password}
 $signer=Call GET '/api/institution/signing-key' $null $admin.token
 if(!$signer.configured -or !$signer.vaultAvailable){throw 'No se recuperó la bóveda institucional.'}
 $draft=Call POST '/api/credentials' @{studentId=$profile.id;credentialType='CERTIFICADO';title='Comprobación ficticia de restauración'} $admin.token
 $null=Call POST ('/api/credentials/'+$draft.id+'/generate-document') @{} $admin.token
 $signed=Call POST ('/api/credentials/'+$draft.id+'/sign') @{} $admin.token
 if(!$signed.signedAt){throw 'La clave institucional restaurada no pudo firmar.'}
 $issued=Call PATCH ('/api/credentials/'+$draft.id+'/issue') @{} $admin.token
 $pdf=Join-Path $folder 'restore-proof.pdf'
 Invoke-WebRequest ($base+'/api/me/credentials/'+$issued.id+'/document') -Headers @{Authorization='Bearer '+$student.token} -OutFile $pdf -TimeoutSec 30
 $check=Invoke-RestMethod ($base+'/api/public/credentials/'+$issued.verificationCode+'/check-document') -Method Post -Form @{file=Get-Item -LiteralPath $pdf} -TimeoutSec 30
 if(!$check.hashMatches -or !$check.signature.cryptographicValid -or !$check.signature.coversWholeDocument -or !$check.signature.signerMatches){throw 'No se pudo comprobar la firma y huella del archivo restaurado.'}
 $avatar=Call GET '/api/me/avatar/status' $null $student.token
 $encryptedEvidence=$false
 if($avatar.present){
  $image=Invoke-WebRequest ($base+'/api/me/avatar') -Headers @{Authorization='Bearer '+$student.token} -TimeoutSec 30
  if($image.RawContentLength -lt 10){throw 'No se pudo descifrar el avatar restaurado.'};$encryptedEvidence=$true
 }
 $evidence=$false
 if($fixture.caseId){
  $front=Invoke-WebRequest ($base+'/api/me/identity-disputes/'+$fixture.caseId+'/files/FRONT') -Headers @{Authorization='Bearer '+$student.token} -TimeoutSec 30
  if($front.RawContentLength -lt 10){throw 'No se pudo descifrar la evidencia restaurada.'}
  $evidence=$true
 }
 $report|Add-Member applicationChecks @{login=$true;history=$true;vaultSigning=$true;minioDownload=$true;sha256=$true;cmsSignature=$true;encryptedAvatar=$encryptedEvidence;encryptedEvidence=$evidence} -Force
 $report.limits='Restauración local con datos ficticios. Falta prueba en un host independiente y custodia externa de las claves de recuperación.'
 $report|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $reportPath
 $verified=$true
 [pscustomobject]@{Result='PASS';Report=$reportPath;Checks='Login, expediente, firma con bóveda restaurada, MinIO, huella, CMS y evidencia cifrada'}
}finally{
 if($compose){& $Docker @compose stop | Out-Null}
 if($verified -and $payload){
  $resolved=[IO.Path]::GetFullPath($payload)
  $allowed=[IO.Path]::GetFullPath((Join-Path $root '.local-backups/restores'))+[IO.Path]::DirectorySeparatorChar
  if(!$resolved.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase) -or (Split-Path $resolved -Leaf) -ne 'payload'){throw 'Ruta de limpieza rechazada.'}
  Remove-Item -LiteralPath $resolved -Recurse
 }
 Pop-Location
}
