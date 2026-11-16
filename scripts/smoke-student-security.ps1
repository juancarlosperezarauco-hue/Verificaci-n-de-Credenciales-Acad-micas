param(
    [string]$ApiBase = 'http://127.0.0.1:18081',
    [string]$MailpitBase = 'http://127.0.0.1:18125'
)
# PowerShell 7. Test-only account and Mailpit inbox; no real mail provider.
$ErrorActionPreference = 'Stop'
function Call-SecurityApi([string]$Method,[string]$Path,$Body,[string]$Token='',[int]$Expected=200) {
    $request = @{Method=$Method;Uri=$ApiBase+$Path;ContentType='application/json';SkipHttpErrorCheck=$true}
    if($Token){$request.Headers=@{Authorization='Bearer '+$Token}}
    if($null -ne $Body){$request.Body=$Body | ConvertTo-Json -Depth 5}
    $response=Invoke-WebRequest @request
    if([int]$response.StatusCode -ne $Expected){throw ('Estado inesperado en '+$Path+': '+$response.StatusCode)}
    if($response.Content){return $response.Content | ConvertFrom-Json}
}
function Read-TestMailToken([string]$Address,[string]$Subject) {
    $timer=[System.Diagnostics.Stopwatch]::StartNew()
    while($timer.Elapsed.TotalSeconds -lt 10){
        $inbox=Invoke-RestMethod -Uri ($MailpitBase+'/api/v1/messages')
        $item=$inbox.messages | Where-Object {
            $recipients=@($_.To | ForEach-Object {$_.Address})
            $_.Subject -eq $Subject -and $recipients -contains $Address
        } | Select-Object -First 1
        if($item){
            $message=Invoke-RestMethod -Uri ($MailpitBase+'/api/v1/message/'+$item.ID)
            if($message.Text -match '[?&]token=([A-Za-z0-9_-]+)'){return $Matches[1]}
        }
        Start-Sleep -Milliseconds 200
    }
    throw 'No llegó el correo de prueba a Mailpit'
}
$email='security-'+[guid]::NewGuid().ToString('N')+'@example.test'
$password='Test-'+[guid]::NewGuid().ToString('N')+'!'
$newPassword='Reset-'+[guid]::NewGuid().ToString('N')+'!'
$account=Call-SecurityApi POST '/api/auth/student/register' @{email=$email;password=$password} '' 201
$profile=Call-SecurityApi GET '/api/me/profile' $null $account.token
$verify=Read-TestMailToken $email 'Verifica tu correo'
Call-SecurityApi POST '/api/auth/student/verify-email' @{token=$verify} '' 204 | Out-Null
Call-SecurityApi POST '/api/auth/student/verify-email' @{token=$verify} '' 400 | Out-Null
$snapshot=Call-SecurityApi GET '/api/me/security' $null $account.token
if(-not $snapshot.emailVerified){throw 'No se confirmó el correo'}
Call-SecurityApi POST '/api/auth/student/password-reset/request' @{email='unknown-'+[guid]::NewGuid().ToString('N')+'@example.test'} '' 204 | Out-Null
Call-SecurityApi POST '/api/auth/student/password-reset/request' @{email=$email} '' 204 | Out-Null
$reset=Read-TestMailToken $email 'Recupera tu contraseña'
Call-SecurityApi POST '/api/auth/student/password-reset/confirm' @{token=$reset;newPassword=$newPassword} '' 204 | Out-Null
Call-SecurityApi GET '/api/me/profile' $null $account.token 403 | Out-Null
Call-SecurityApi POST '/api/auth/student/password-reset/confirm' @{token=$reset;newPassword=$password} '' 400 | Out-Null
$fresh=Call-SecurityApi POST '/api/auth/student/login' @{email=$email;password=$newPassword}
$newEmail='changed-'+[guid]::NewGuid().ToString('N')+'@example.test'
Call-SecurityApi POST '/api/me/security/email-change' @{currentPassword=$newPassword;newEmail=$newEmail} $fresh.token 204 | Out-Null
$change=Read-TestMailToken $newEmail 'Confirma tu nuevo correo'
Call-SecurityApi POST '/api/auth/student/email-change/confirm' @{token=$change} '' 204 | Out-Null
Call-SecurityApi GET '/api/me/profile' $null $fresh.token 403 | Out-Null
$changed=Call-SecurityApi POST '/api/auth/student/login' @{email=$newEmail;password=$newPassword}
$same=Call-SecurityApi GET '/api/me/profile' $null $changed.token
if($same.id -ne $profile.id){throw 'El cambio de correo duplicó la identidad'}
Call-SecurityApi DELETE '/api/me/security/account' @{currentPassword=$newPassword} $changed.token 204 | Out-Null
Call-SecurityApi GET '/api/me/profile' $null $changed.token 403 | Out-Null
Call-SecurityApi POST '/api/auth/student/login' @{email=$newEmail;password=$newPassword} '' 401 | Out-Null
[pscustomobject]@{result='PASS';studentId=$profile.id;checks='Correo real Mailpit, token de un uso, recuperación uniforme, reset, JWT anterior rechazado, cambio de correo, misma identidad y desactivación lógica'}
