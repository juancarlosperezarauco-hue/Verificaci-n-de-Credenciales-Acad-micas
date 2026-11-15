param([string]$ApiBase='http://127.0.0.1:18081')
# PowerShell 7. Creates disposable accounts in the isolated audit stack only.
$ErrorActionPreference='Stop'
if($ApiBase -ne 'http://127.0.0.1:18081'){throw 'This smoke is restricted to the isolated audit API.'}
function Call-Access([string]$Method,[string]$Path,$Body,[string]$Token='',[int]$Expected=200){
    $request=@{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true}
    if($Token){$request.Headers=@{Authorization='Bearer '+$Token}}
    if($null -ne $Body){$request.Body=$Body|ConvertTo-Json -Depth 5}
    $response=Invoke-WebRequest @request
    if([int]$response.StatusCode -ne $Expected){throw ('Unexpected HTTP '+$response.StatusCode+' at '+$Path)}
    if($response.Content){return $response.Content|ConvertFrom-Json}
}
function Get-TestTotp([string]$Seed,[long]$EpochSeconds=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()){
    $alphabet='ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';$bytes=[Collections.Generic.List[byte]]::new();$buffer=0;$bits=0
    foreach($c in $Seed.ToCharArray()){
        $buffer=($buffer -shl 5) -bor $alphabet.IndexOf($c);$bits+=5
        if($bits -ge 8){$bits-=8;$bytes.Add([byte](($buffer -shr $bits) -band 255));$buffer=$buffer -band ((1 -shl $bits)-1)}
    }
    $counter=[long][Math]::Floor($EpochSeconds/30)
    $message=[BitConverter]::GetBytes($counter);if([BitConverter]::IsLittleEndian){[Array]::Reverse($message)}
    $hmac=[Security.Cryptography.HMACSHA1]::new($bytes.ToArray())
    try{$digest=$hmac.ComputeHash($message)}finally{$hmac.Dispose()}
    $offset=$digest[19] -band 15
    $number=(([int]$digest[$offset] -band 127) -shl 24) -bor ([int]$digest[$offset+1] -shl 16) -bor ([int]$digest[$offset+2] -shl 8) -bor [int]$digest[$offset+3]
    return ($number%1000000).ToString('D6')
}
$suffix=[guid]::NewGuid().ToString('N');$password='Test-'+[guid]::NewGuid().ToString('N')+'!'
$studentEmail='sessions-'+$suffix+'@example.test'
$first=Call-Access POST '/api/auth/student/register' @{email=$studentEmail;password=$password} '' 201
$second=Call-Access POST '/api/auth/student/login' @{email=$studentEmail;password=$password}
$devices=@(Call-Access GET '/api/security/sessions' $null $second.token)
if($devices.Count -ne 2){throw 'Expected two device sessions'}
Call-Access POST '/api/security/sessions/revoke-others' @{} $second.token 204|Out-Null
Call-Access GET '/api/me/profile' $null $first.token 403|Out-Null
Call-Access DELETE '/api/security/sessions/current' $null $second.token 204|Out-Null
Call-Access GET '/api/me/profile' $null $second.token 403|Out-Null
$slug='mfa-'+$suffix;$email='admin-'+$suffix+'@example.test'
$admin=Call-Access POST '/api/auth/register' @{organizationName='Disposable MFA test';organizationSlug=$slug;institutionType='ACADEMIA';email=$email;password=$password} '' 201
$prepared=Call-Access POST '/api/security/mfa/setup' @{password=$password} $admin.token
$enabled=Call-Access POST '/api/security/mfa/confirm' @{code=(Get-TestTotp $prepared.secret)} $admin.token
Call-Access GET '/api/access/me' $null $admin.token 403|Out-Null
$challenge=Call-Access POST '/api/auth/login' @{organizationSlug=$slug;email=$email;password=$password}
if(-not $challenge.mfaRequired -or $challenge.token){throw 'MFA login issued an early JWT'}
Call-Access POST '/api/auth/mfa/verify' @{challengeToken=$challenge.challengeToken;code='invalid'} '' 401|Out-Null
$verified=Call-Access POST '/api/auth/mfa/verify' @{challengeToken=$challenge.challengeToken;code=$enabled.recoveryCodes[0]}
Call-Access GET '/api/access/me' $null $verified.token|Out-Null
$next=Call-Access POST '/api/auth/login' @{organizationSlug=$slug;email=$email;password=$password}
Call-Access POST '/api/auth/mfa/verify' @{challengeToken=$next.challengeToken;code=$enabled.recoveryCodes[0]} '' 401|Out-Null
$verified=Call-Access POST '/api/auth/mfa/verify' @{challengeToken=$next.challengeToken;code=$enabled.recoveryCodes[1]}
Call-Access DELETE '/api/security/mfa' @{password=$password;code=$enabled.recoveryCodes[2]} $verified.token 204|Out-Null
Call-Access GET '/api/access/me' $null $verified.token 403|Out-Null
$limited='limit-'+$suffix+'@example.test'
for($i=0;$i -lt 10;$i++){Call-Access POST '/api/auth/student/login' @{email=$limited;password='invalid'} '' 401|Out-Null}
Call-Access POST '/api/auth/student/login' @{email=$limited;password='invalid'} '' 429|Out-Null
[pscustomobject]@{result='PASS';checks='Device revocation and logout; MFA enrollment; no early JWT; recovery-code replay blocked; disable revokes sessions; login rate limit 429'}
