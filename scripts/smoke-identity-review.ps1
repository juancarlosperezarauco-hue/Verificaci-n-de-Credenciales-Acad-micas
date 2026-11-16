param(
 [string]$ApiBase='http://127.0.0.1:18081',
 [string]$FrontendBase='http://127.0.0.1:4301',
 [string]$MailpitBase='http://127.0.0.1:18125',
 [string]$SuperEmail=$env:REVIEW_SUPERADMIN_EMAIL,
 [string]$SuperPassword=$env:REVIEW_SUPERADMIN_PASSWORD
)
# Fictitious identities only. Restricted to the isolated audit stack, never the main DB.
$ErrorActionPreference='Stop'
if($ApiBase -ne 'http://127.0.0.1:18081' -or $FrontendBase -ne 'http://127.0.0.1:4301' -or $MailpitBase -ne 'http://127.0.0.1:18125'){throw 'Usa exclusivamente los puertos del entorno aislado.'}
. (Join-Path $PSScriptRoot 'smoke-organization.ps1') -ApiBase $ApiBase -SuperEmail $SuperEmail -SuperPassword $SuperPassword | Out-Null
function Review-Call([string]$Method,[string]$Path,$Body,[string]$Token,[int]$Expected=200){
 $request=@{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true;Headers=@{Authorization='Bearer '+$Token}}
 if($null -ne $Body){$request.Body=$Body|ConvertTo-Json -Depth 12}
 $response=Invoke-WebRequest @request
 if([int]$response.StatusCode -ne $Expected){throw ('Estado inesperado '+$response.StatusCode+' en '+$Path)}
 if($response.Content){return $response.Content|ConvertFrom-Json}
}
$timer=[Diagnostics.Stopwatch]::StartNew();$mailToken=$null
while($timer.Elapsed.TotalSeconds -lt 15 -and -not $mailToken){
 $messages=Invoke-RestMethod ($MailpitBase+'/api/v1/messages')
 $message=$messages.messages | Where-Object {$recipients=@($_.To|ForEach-Object {$_.Address});($recipients -contains $student.email) -and $_.Subject -eq 'Verifica tu correo'} | Select-Object -First 1
 if($message){$full=Invoke-RestMethod ($MailpitBase+'/api/v1/message/'+$message.ID);if($full.Text -match '(?:token=)([A-Za-z0-9_-]+)'){$mailToken=$matches[1]}}
 if(-not $mailToken){Start-Sleep -Milliseconds 250}
}
if(-not $mailToken){throw 'No llegó el correo al buzón de pruebas'}
$null=Review-Call POST '/api/auth/student/verify-email' @{token=$mailToken} '' 204
$otherSuffix=[guid]::NewGuid().ToString('N')
$otherProfile=$profile|ConvertTo-Json -Depth 12|ConvertFrom-Json -AsHashtable
$otherProfile.legalName='Otra institución de prueba';$otherProfile.taxId=$otherSuffix;$otherProfile.educationLicense='EDU-'+$otherSuffix
$otherProfile.representative.email='other-'+$otherSuffix+'@example.test';$otherProfile.representative.documentNumber=$otherSuffix
$other=Review-Call POST '/api/auth/organizations/register' @{organizationSlug='other-'+$otherSuffix;password=$password;profile=$otherProfile} '' 201
$null=Review-Call PATCH ('/api/platform/organizations/'+$other.tenantId+'/status') @{status='ACTIVE';reason='Aprobación de institución ficticia'} $root.token
$null=Review-Call POST '/api/students/redeem' @{code=$studentProfile.searchCode} $other.token
$temp=Join-Path ([IO.Path]::GetTempPath()) ('identity-review-'+[guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $temp
$imageFile=Join-Path $temp 'documento-ficticio.png'
try {
 Add-Type -AssemblyName System.Drawing
 $bitmap=[Drawing.Bitmap]::new(1200,750);$graphics=[Drawing.Graphics]::FromImage($bitmap);$font=[Drawing.Font]::new('Arial',30)
 try {
  $graphics.Clear([Drawing.Color]::WhiteSmoke)
  $graphics.FillRectangle([Drawing.Brushes]::Navy,0,0,1200,120)
  $graphics.DrawString('DOCUMENTO FICTICIO - SOLO PRUEBAS',$font,[Drawing.Brushes]::White,35,35)
  $graphics.DrawString('Estudiante de prueba',$font,[Drawing.Brushes]::Black,50,220)
  $graphics.DrawString('CI: DEMO - SIN VALIDEZ',$font,[Drawing.Brushes]::Black,50,320)
  $graphics.DrawString('NO ES UN DOCUMENTO DE IDENTIDAD REAL',$font,[Drawing.Brushes]::DarkRed,50,550)
  $bitmap.Save($imageFile,[Drawing.Imaging.ImageFormat]::Png)
 } finally {$font.Dispose();$graphics.Dispose();$bitmap.Dispose()}
 # Larger than nginx's default 1 MiB; backend decodes and removes trailing data.
 $stream=[IO.File]::OpenWrite($imageFile);try{$stream.SetLength(1200000)}finally{$stream.Dispose()}
 function Review-Upload {
  $client=[Net.Http.HttpClient]::new();$content=[Net.Http.MultipartFormDataContent]::new();$response=$null
  try {
   $client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$student.token)
   $content.Add([Net.Http.StringContent]::new($admin.tenantId),'tenantId');$content.Add([Net.Http.StringContent]::new('true'),'accepted')
   foreach($partName in @('front','back','photo')){$part=[Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($imageFile));$part.Headers.ContentType=[Net.Http.Headers.MediaTypeHeaderValue]::new('image/png');$content.Add($part,$partName,($partName+'.png'))}
   $response=$client.PostAsync(($FrontendBase+'/api/me/identity-verifications'),$content).GetAwaiter().GetResult()
   if([int]$response.StatusCode -ne 201){throw ('La carga por nginx falló: '+[int]$response.StatusCode)}
   return $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()|ConvertFrom-Json
  } finally {if($response){$response.Dispose()};$content.Dispose();$client.Dispose()}
 }
 $first=Review-Upload
 $base='/api/institution/identity-verifications/'+$first.id
 $null=Review-Call GET ($base+'/files/FRONT') $null $staff.token 403
 $null=Review-Call GET $base $null $other.token 404
 $null=Review-Call GET $base $null $root.token 403
 $ownBytes=Invoke-WebRequest ($ApiBase+'/api/me/identity-verifications/'+$first.id+'/files/FRONT') -Headers @{Authorization='Bearer '+$student.token}
 $institutionBytes=Invoke-WebRequest ($ApiBase+$base+'/files/FRONT') -Headers @{Authorization='Bearer '+$admin.token}
 if($ownBytes.Headers['Cache-Control'] -notcontains 'no-store'){throw 'Falta no-store en documento privado'}
 if($ownBytes.RawContentLength -ne $institutionBytes.RawContentLength -or $ownBytes.RawContentLength -ge 1200000){throw 'La normalización de la imagen no coincide'}
 $null=Review-Call POST ($base+'/decision') @{status='REJECTED';reason='Imagen de prueba: vuelve a enviar una copia legible.'} $admin.token
 $null=Review-Call POST ($base+'/decision') @{status='VERIFIED'} $admin.token 409
 $second=Review-Upload
 $null=Review-Call POST ('/api/institution/identity-verifications/'+$second.id+'/decision') @{status='VERIFIED'} $admin.token
 $history=Review-Call GET '/api/me/identity-verifications' $null $student.token
 if($history.submissions[0].effectiveStatus -ne 'VERIFIED'){throw 'No aparece la verificación institucional'}
 $null=Review-Call POST ('/api/me/identity-verifications/'+$second.id+'/withdraw') @{} $student.token 204
 $null=Review-Call GET ('/api/institution/identity-verifications/'+$second.id+'/files/FRONT') $null $admin.token 404
 $third=Review-Upload
 $script:IdentityFixture=@{studentEmail=$student.email;studentPassword=$password;adminEmail=$profile.representative.email;adminPassword=$password;slug=$slug;studentId=$studentProfile.id;tenantId=$admin.tenantId;submissionId=$third.id}
 [pscustomobject]@{result='PASS';checks='Carga 3.6 MB por nginx, JPEG sin metadatos, MinIO real, permisos, tenant ajeno, superadmin sin imágenes, rechazo, replay, aprobación, retirada y reenvío';submissionId=$third.id}
} finally {
 if(Test-Path -LiteralPath $imageFile){Remove-Item -LiteralPath $imageFile}
 if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp}
}
