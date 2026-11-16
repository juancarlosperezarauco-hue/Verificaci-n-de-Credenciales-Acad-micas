param([string]$ApiBase='http://127.0.0.1:18081')
$ErrorActionPreference='Stop'
if($ApiBase -ne 'http://127.0.0.1:18081'){throw 'Restricted to the isolated audit API.'}
function Api([string]$Method,[string]$Path,$Body,[string]$Token='',[int]$Expected=200){
 $options=@{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true;TimeoutSec=10}
 if($Token){$options.Headers=@{Authorization='Bearer '+$Token}}
 if($null -ne $Body){$options.Body=$Body|ConvertTo-Json -Depth 6}
 $response=Invoke-WebRequest @options
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
$suffix=[guid]::NewGuid().ToString('N');$password='Test-'+[guid]::NewGuid().ToString('N')+'!';$email='polish-'+$suffix+'@example.test';$slug='polish-'+$suffix
$account=Api POST '/api/auth/register' @{organizationName='Academia histórica de prueba';organizationSlug=$slug;institutionType='ACADEMIA';email=$email;password=$password} '' 201
Api GET '/api/institution/details' $null $account.token 404|Out-Null
$profile=@{legalName='Academia histórica de prueba';institutionType='ACADEMIA';educationLevels=@();country='BO';region='La Paz';address='Av. Prueba 100';phone='70000000';contactEmail='contact@example.test';representative=@{fullName='Directora de prueba';documentType='CI';documentNumber=$suffix;position='Directora';email=$email;phone='70000001'}}
Api PUT '/api/institution/details' $profile $account.token|Out-Null
Api GET '/api/institution/details' $null $account.token|Out-Null
Api GET '/api/credentials' $null $account.token 403|Out-Null
$setup=Api POST '/api/security/mfa/setup' @{password=$password} $account.token
$enabled=Api POST '/api/security/mfa/confirm' @{code=(Get-TestTotp $setup.secret)} $account.token
$renewed=Api POST '/api/security/mfa/recovery-codes' @{password=$password;code=$enabled.recoveryCodes[0]} $enabled.session.token
Api GET '/api/security/sessions' $null $enabled.session.token 403|Out-Null
$replacement=Api POST '/api/security/mfa/replacement' @{password=$password;code=$renewed.recoveryCodes[0]} $renewed.session.token
$changed=Api POST '/api/security/mfa/confirm' @{code=(Get-TestTotp $replacement.secret)} $renewed.session.token
Api GET '/api/security/sessions' $null $renewed.session.token 403|Out-Null
$challenge=Api POST '/api/auth/login' @{organizationSlug=$slug;email=$email;password=$password}
Api POST '/api/auth/mfa/verify' @{challengeToken=$challenge.challengeToken;code=$renewed.recoveryCodes[1]} '' 401|Out-Null
$login=Api POST '/api/auth/mfa/verify' @{challengeToken=$challenge.challengeToken;code=$changed.recoveryCodes[0]}
$pending=Api POST '/api/security/mfa/replacement' @{password=$password;code=$changed.recoveryCodes[1]} $login.token
Api DELETE '/api/security/mfa/pending' $null $login.token 204|Out-Null
$status=Api GET '/api/security/mfa' $null $login.token
if(-not $status.enabled -or $status.pendingSetup){throw 'Cancellation changed active MFA or kept a pending setup'}
Api GET '/api/platform/security/maintenance' $null $login.token 403|Out-Null
$script:PolishFixture=@{slug=$slug;email=$email;password=$password;tenantId=$account.tenantId;recoveryCodes=$changed.recoveryCodes}
[pscustomobject]@{result='PASS';tenantId=$account.tenantId;checks='Real HTTP JSON 404, legacy profile completion, review gate, MFA backup renewal, authenticator replacement, session revocation, old backups rejected, cancellation and platform boundary'}
