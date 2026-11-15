param(
 [string]$ApiBase='http://127.0.0.1:18081',
 [Parameter(Mandatory)][string]$SuperEmail,
 [Parameter(Mandatory)][string]$SuperPassword
)
# PowerShell 7. Audit stack and fictitious account only. No provider credentials printed.
$ErrorActionPreference='Stop'
if($ApiBase -ne 'http://127.0.0.1:18081'){throw 'This smoke is restricted to the isolated audit API.'}
function Call-AuditMail([string]$Method,[string]$Path,$Body,[string]$Token='',[int]$Expected=200){
 $request=@{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true;TimeoutSec=5}
 if($Token){$request.Headers=@{Authorization='Bearer '+$Token}}
 if($null -ne $Body){$request.Body=$Body|ConvertTo-Json -Depth 5}
 $response=Invoke-WebRequest @request
 if([int]$response.StatusCode -ne $Expected){throw ('Unexpected HTTP '+$response.StatusCode+' at '+$Path)}
 if($response.Content){return $response.Content|ConvertFrom-Json}
}
$root=Call-AuditMail POST '/api/auth/platform/login' @{email=$SuperEmail;password=$SuperPassword}
if(-not $root.token){throw 'Use the isolated audit superadmin without MFA for this smoke.'}
$email='durable-'+[guid]::NewGuid().ToString('N')+'@example.test'
$password='Test-'+[guid]::NewGuid().ToString('N')+'!'
$account=Call-AuditMail POST '/api/auth/student/register' @{email=$email;password=$password} '' 201
$profile=Call-AuditMail GET '/api/me/profile' $null $account.token
$script:AuditMailFixture=@{rootToken=$root.token;studentId=$profile.id;studentToken=$account.token;email=$email}
$events=Call-AuditMail GET ('/api/platform/audit?resourceId='+$profile.id) $null $root.token
if(-not ($events.items|Where-Object {$_.action -eq 'STUDENT_REGISTERED'})){throw 'Missing student registration audit event'}
$metadata=Call-AuditMail GET '/api/platform/mail?size=100' $null $root.token
$entry=$metadata.items|Where-Object {$_.studentId -eq $profile.id}|Select-Object -First 1
if(-not $entry){throw 'Missing durable mail row'}
$serialized=$metadata|ConvertTo-Json -Depth 5
if($serialized.Contains($email) -or $serialized.Contains('encryptedPayload') -or $serialized.Contains('tokenId')){throw 'Mail monitor exposed private payload'}
Call-AuditMail GET '/api/platform/mail' $null $account.token 403|Out-Null
$script:AuditMailFixture.mailId=$entry.id
[pscustomobject]@{result='PASS';studentId=$profile.id;mailId=$entry.id;checks='Registration audit, durable queue metadata and platform-only monitoring'}
