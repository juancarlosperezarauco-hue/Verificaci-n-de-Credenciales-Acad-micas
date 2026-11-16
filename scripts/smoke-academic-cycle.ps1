param([string]$SuperEmail=$env:REVIEW_SUPERADMIN_EMAIL,[string]$SuperPassword=$env:REVIEW_SUPERADMIN_PASSWORD)
# Fictional data, fixed isolated audit ports. Never run against the main application.
$ErrorActionPreference='Stop'
$PSDefaultParameterValues['Invoke-RestMethod:TimeoutSec']=30
$PSDefaultParameterValues['Invoke-WebRequest:TimeoutSec']=30
$ApiBase='http://127.0.0.1:18081';$FrontendBase='http://127.0.0.1:4301';$MailpitBase='http://127.0.0.1:18125'
. (Join-Path $PSScriptRoot 'smoke-identity-review.ps1') -ApiBase $ApiBase -FrontendBase $FrontendBase -MailpitBase $MailpitBase -SuperEmail $SuperEmail -SuperPassword $SuperPassword | Out-Null
function Cycle-Call([string]$Method,[string]$Path,$Body,[string]$Token,[int]$Expected=200,[hashtable]$Extra=@{}){
 $headers=@{Authorization='Bearer '+$Token};foreach($name in $Extra.Keys){$headers[$name]=$Extra[$name]}
 $args=@{Method=$Method;Uri=$FrontendBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true;Headers=$headers}
 if($null -ne $Body){$args.Body=$Body|ConvertTo-Json -Depth 12}
 $response=Invoke-WebRequest @args
 if([int]$response.StatusCode -ne $Expected){throw ('HTTP '+$response.StatusCode+' en '+$Path)}
 if($response.Content){return $response.Content|ConvertFrom-Json}
}
function Cycle-Multipart([string]$Path,[hashtable]$Fields,[hashtable]$Files,[string]$Token,[int]$Expected=200){
 $client=[Net.Http.HttpClient]::new();$client.Timeout=[TimeSpan]::FromSeconds(30);$content=[Net.Http.MultipartFormDataContent]::new();$response=$null
 try{
  if($Token){$client.DefaultRequestHeaders.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$Token)}
  foreach($name in $Fields.Keys){$content.Add([Net.Http.StringContent]::new([string]$Fields[$name]),$name)}
  foreach($name in $Files.Keys){$part=[Net.Http.ByteArrayContent]::new([IO.File]::ReadAllBytes($Files[$name]));$content.Add($part,$name,[IO.Path]::GetFileName($Files[$name]))}
  $response=$client.PostAsync($FrontendBase+$Path,$content).GetAwaiter().GetResult()
  if([int]$response.StatusCode -ne $Expected){throw ('Multipart HTTP '+[int]$response.StatusCode+' en '+$Path)}
  $text=$response.Content.ReadAsStringAsync().GetAwaiter().GetResult();if($text){return $text|ConvertFrom-Json}
 }finally{if($response){$response.Dispose()};$content.Dispose();$client.Dispose()}
}
function Cycle-Download([string]$Path,[string]$Token,[string]$File){Invoke-WebRequest ($FrontendBase+$Path) -Headers @{Authorization='Bearer '+$Token} -OutFile $File}
function Cycle-Approve([string]$Id){$path='/api/institution/credential-requests/'+$Id+'/actions';foreach($action in @('START','RECOMMEND')){$null=Cycle-Call POST $path @{action=$action} $review.token};return Cycle-Call POST $path @{action='APPROVE'} $admin.token}
$temp=Join-Path ([IO.Path]::GetTempPath()) ('academic-cycle-'+[guid]::NewGuid().ToString('N'));$null=New-Item -ItemType Directory -Path $temp
$pdf=Join-Path $temp 'certificate.pdf';$changed=Join-Path $temp 'changed.pdf';$ownPdf=Join-Path $temp 'own.pdf';$oldPdf=Join-Path $temp 'old.pdf';$image=Join-Path $temp 'blank-test.png'
try{
 $program=Cycle-Call POST '/api/institution/academic/programs' @{name='Curso ficticio de pruebas';kind='Curso';period='Gestión 2026';requirements='Constancia ficticia';active=$true} $admin.token 201
 $record=Cycle-Call POST '/api/institution/academic/records' @{studentId=$studentProfile.id;programId=$program.id;registrationCode='MAT-'+[guid]::NewGuid().ToString('N');enrolledOn='2026-01-10'} $admin.token 201
 $null=Cycle-Call PATCH ('/api/institution/academic/records/'+$record.id) @{status='COMPLETED';completedOn='2026-11-01';result='Aprobado';notes='Registro ficticio para pruebas'} $admin.token
 $null=Cycle-Call PATCH ('/api/institution/academic/records/'+$record.id) @{status='WITHDRAWN'} $other.token 404
 $v1=Cycle-Call POST '/api/institution/templates' @{name='Modelo ficticio';credentialType='CERTIFICADO';heading='Certificado de prueba';bodyText='Por {{documentTitle}}, correspondiente a {{programName}}.';orientation='LANDSCAPE'} $admin.token 201
 $v2=Cycle-Call POST '/api/institution/templates' @{previousVersionId=$v1.id;name='Modelo ficticio';credentialType='CERTIFICADO';heading='Certificado de prueba';bodyText='Se certifica a {{studentName}} por {{documentTitle}}.';orientation='LANDSCAPE'} $admin.token 201
 if($v2.version -ne 2){throw 'No se creó la segunda versión'}
 $role=Cycle-Call POST '/api/access/roles' @{name='Revisor de solicitudes';permissions=@('REQUEST_VIEW','REQUEST_REVIEW')} $admin.token 201
 $reviewEmail='review-'+[guid]::NewGuid().ToString('N')+'@example.test'
 $null=Cycle-Call POST '/api/access/users' @{fullName='Revisor ficticio';email=$reviewEmail;password=$password;roleIds=@($role.id)} $admin.token 201
 $review=Cycle-Call POST '/api/auth/login' @{organizationSlug=$slug;email=$reviewEmail;password=$password} '' 200
 $request=Cycle-Call POST '/api/me/credential-requests' @{recordId=$record.id;credentialType='CERTIFICADO';title='Participación en curso ficticio';templateVersionId=$v2.id} $student.token 201
 Cycle-Download ('/api/credentials/'+$credential.id+'/document') $admin.token $pdf
 $attachment=Cycle-Multipart ('/api/me/credential-requests/'+$request.id+'/attachments') @{} @{file=$pdf} $student.token
 Cycle-Download ('/api/me/credential-requests/'+$request.id+'/attachments/'+$attachment.id) $student.token $ownPdf
 if((Get-FileHash $pdf).Hash -ne (Get-FileHash $ownPdf).Hash){throw 'El adjunto del trámite no conservó sus bytes'}
 Cycle-Download ('/api/institution/templates/'+$v2.id+'/preview') $admin.token $ownPdf
 $path='/api/institution/credential-requests/'+$request.id+'/actions'
 $null=Cycle-Call POST $path @{action='START'} $review.token
 $null=Cycle-Call POST $path @{action='OBSERVE';reason='Corrige el título solicitado para el curso ficticio.'} $review.token
 $null=Cycle-Call PATCH ('/api/me/credential-requests/'+$request.id) @{title='Curso ficticio completado';message='Datos corregidos';templateVersionId=$v2.id} $student.token
 $null=Cycle-Call POST ('/api/me/credential-requests/'+$request.id+'/actions') @{action='RESUBMIT'} $student.token
 $approved=Cycle-Approve $request.id
 $null=Cycle-Call POST $path @{action='APPROVE'} $review.token 403
 $documentPath='/api/credentials/'+$approved.credentialId
 $signer=Cycle-Call POST '/api/institution/signing-key/demo' @{required=$true} $admin.token
 if(!$signer.testOnly){throw 'Falta la marca de certificado de prueba'}
 $null=Cycle-Call POST ($documentPath+'/generate-document') @{} $admin.token
 $null=Cycle-Call PATCH ($documentPath+'/issue') @{} $admin.token 400
 $null=Cycle-Call POST ($documentPath+'/sign') @{} $admin.token
 $operation=[guid]::NewGuid().ToString();$issued=Cycle-Call PATCH ($documentPath+'/issue') @{} $admin.token 200 @{'Idempotency-Key'=$operation}
 $replay=Cycle-Call PATCH ($documentPath+'/issue') @{} $admin.token 200 @{'Idempotency-Key'=$operation}
 if($replay.id -ne $issued.id){throw 'El reintento no es idempotente'}
 Cycle-Download ($documentPath+'/document') $admin.token $pdf
 Cycle-Download ('/api/me/credentials/'+$issued.id+'/document') $student.token $ownPdf
 if((Get-FileHash $pdf).Hash -ne (Get-FileHash $ownPdf).Hash){throw 'El expediente personal entregó otros bytes'}
 $check=Cycle-Multipart ('/api/public/credentials/'+$issued.verificationCode+'/check-document') @{} @{file=$pdf} ''
 if(!$check.hashMatches -or $check.signature.status -ne 'VALID' -or !$check.testSignature){throw 'No se verificó la firma real del PDF de prueba'}
 $bytes=[IO.File]::ReadAllBytes($pdf);[IO.File]::WriteAllBytes($changed,($bytes+[Text.Encoding]::ASCII.GetBytes([Environment]::NewLine+'% changed'+[Environment]::NewLine)))
 $mismatch=Cycle-Multipart ('/api/public/credentials/'+$issued.verificationCode+'/check-document') @{} @{file=$changed} ''
 if($mismatch.hashMatches -or $mismatch.signature.status -eq 'VALID'){throw 'No se detectó la alteración'}
 $public=Cycle-Call GET ('/api/public/credentials/'+$issued.verificationCode) $null ''
 if($public.integrityStatus -ne 'NOT_CHECKED' -or $public.signatureStatus -ne 'REGISTERED'){throw 'La consulta registral anuncia una comprobación que no realizó'}
 $correction=Cycle-Call POST ('/api/me/credentials/'+$issued.id+'/corrections') @{recordId=$record.id;title='Título corregido de prueba';reason='Corrección solicitada por el estudiante';templateVersionId=$v2.id} $student.token 201
 $replacement=Cycle-Approve $correction.id;$replacementPath='/api/credentials/'+$replacement.credentialId
 $null=Cycle-Call POST ($replacementPath+'/generate-document') @{} $admin.token
 $null=Cycle-Call POST ($replacementPath+'/sign') @{} $admin.token
 $new=Cycle-Call PATCH ($replacementPath+'/issue') @{} $admin.token
 $previous=Cycle-Call GET ('/api/public/credentials/'+$issued.verificationCode) $null ''
 if($previous.status -ne 'REVOKED' -or $previous.replacedByCode -ne $new.verificationCode){throw 'No se publicó el reemplazo'}
 Cycle-Download ($documentPath+'/document') $admin.token $oldPdf
 if((Get-FileHash $pdf).Hash -ne (Get-FileHash $oldPdf).Hash){throw 'Se alteraron los bytes históricos'}
 Add-Type -AssemblyName System.Drawing;$bitmap=[Drawing.Bitmap]::new(100,80);try{$bitmap.Save($image,[Drawing.Imaging.ImageFormat]::Png)}finally{$bitmap.Dispose()}
 $stream=[IO.File]::OpenWrite($image);try{$stream.SetLength(1200000)}finally{$stream.Dispose()}
 $null=Cycle-Multipart '/api/me/avatar' @{} @{file=$image} $student.token 204
 $avatar=Invoke-WebRequest ($FrontendBase+'/api/me/avatar') -Headers @{Authorization='Bearer '+$student.token}
 if($avatar.Headers['Cache-Control'] -notcontains 'no-store' -or $avatar.RawContentLength -ge 1200000){throw 'La fotografía no está normalizada o es cacheable'}
 $null=Cycle-Call DELETE '/api/me/avatar' $null $student.token 204
 $number='CLAIM-'+[guid]::NewGuid().ToString('N')
 $case=Cycle-Multipart '/api/me/identity-disputes' @{country='BO';documentType='CI';documentNumber=$number;reason='Caso ficticio de corrección de identidad para pruebas'} @{front=$image;back=$image} $student.token 201
 $null=Cycle-Call GET '/api/platform/identity-disputes' $null $admin.token 403
 $null=Cycle-Call POST ('/api/platform/identity-disputes/'+$case.id+'/resolution') @{action='OBSERVE';reason='Adjunta imágenes corregidas del caso de prueba.'} $root.token
 $null=Cycle-Multipart ('/api/me/identity-disputes/'+$case.id+'/resubmit') @{explanation='Adjunto las imágenes corregidas de prueba.'} @{front=$image;back=$image} $student.token
 $current=Cycle-Call GET '/api/me/profile' $null $student.token
 $corrected=@{fullName=$current.fullName;country='BO';documentType='CI';documentNumber=$number;dateOfBirth=$current.dateOfBirth;department=$current.department}
 $null=Cycle-Call POST ('/api/platform/identity-disputes/'+$case.id+'/resolution') @{action='CORRECT_PROFILE';reason='Se comprobó el caso ficticio mediante revisión humana de prueba.';profile=$corrected} $root.token
 $null=Cycle-Call GET '/api/me/profile' $null $student.token 403
 $student=Cycle-Call POST '/api/auth/student/login' @{email=$student.email;password=$password} ''
 $status=Cycle-Call GET '/api/platform/storage/maintenance' $null $root.token
 if(!$status.encryptionConfigured){throw 'No está configurado el cifrado de archivos'}
 $script:AcademicFixture=@{studentEmail=$student.email;password=$password;adminEmail=$profile.representative.email;slug=$slug;reviewerEmail=$reviewEmail;credentialId=$issued.id;replacementId=$new.id;verificationCode=$new.verificationCode;caseId=$case.id;recordId=$record.id}
 [pscustomobject]@{result='PASS';checks='Expediente, revisión separada, observación, versiones, firma CMS, idempotencia, alteración, descarga privada, reemplazo, avatar y disputa; cargas nginx >1MiB';credentialId=$issued.id;replacementId=$new.id}
}finally{
 foreach($file in @($pdf,$changed,$ownPdf,$oldPdf,$image)){if(Test-Path -LiteralPath $file){Remove-Item -LiteralPath $file}}
 if(Test-Path -LiteralPath $temp){Remove-Item -LiteralPath $temp}
}
