param(
 [Parameter(Mandatory)][string]$SuperEmail,
 [Parameter(Mandatory)][string]$SuperPassword,
 [string]$DockerCommand='docker'
)
# Audit stack only. Stops its Mailpit, persists a retry, restarts its backend,
# restores Mailpit and validates delivery and the one-use verification link.
$ErrorActionPreference='Stop'
$docker=$DockerCommand
if((Invoke-RestMethod -Uri 'http://127.0.0.1:18081/actuator/health' -TimeoutSec 5).status -ne 'UP'){throw 'Audit API not ready'}
try{
 & $docker stop credentials-v3-audit-mailpit-1|Out-Null
 $proof=. .\scripts\smoke-audit-mail.ps1 -SuperEmail $SuperEmail -SuperPassword $SuperPassword
 $proof|Format-List
 function MailEntry {
  $rows=Invoke-RestMethod -Uri 'http://127.0.0.1:18081/api/platform/mail?size=100' -Headers @{Authorization='Bearer '+$AuditMailFixture.rootToken} -TimeoutSec 5
  return $rows.items|Where-Object {$_.id -eq $AuditMailFixture.mailId}|Select-Object -First 1
 }
 $timer=[Diagnostics.Stopwatch]::StartNew();$entry=$null
 while($timer.Elapsed.TotalSeconds -lt 15){$entry=MailEntry;if($entry.state -eq 'RETRY'){break};Start-Sleep -Milliseconds 300}
 if($entry.state -ne 'RETRY'){throw 'SMTP outage did not enter RETRY'}
 Write-Output 'SMTP outage: RETRY persisted'
 & $docker start credentials-v3-audit-mailpit-1|Out-Null
 & $docker restart credentials-v3-audit-backend-1|Out-Null
 Write-Output 'Audit backend restarted; checking persisted queue'
 $timer.Restart();$healthy=$false
 while($timer.Elapsed.TotalSeconds -lt 45){try{$healthy=(Invoke-RestMethod -Uri 'http://127.0.0.1:18081/actuator/health' -TimeoutSec 2).status -eq 'UP'}catch{};if($healthy){break};Start-Sleep -Milliseconds 500}
 if(-not $healthy){throw 'Audit backend did not recover'}
 $timer.Restart()
 while($timer.Elapsed.TotalSeconds -lt 45){$entry=MailEntry;if($entry.state -eq 'SENT'){break};Start-Sleep -Milliseconds 500}
 if($entry.state -ne 'SENT' -or $entry.attempts -lt 2){throw 'Durable retry was not accepted by SMTP'}
 $inbox=Invoke-RestMethod -Uri 'http://127.0.0.1:18125/api/v1/messages'
 $message=$inbox.messages|Where-Object {@($_.To|ForEach-Object {$_.Address}) -contains $AuditMailFixture.email}|Select-Object -First 1
 if(-not $message){throw 'Retried message is absent from Mailpit'}
 $body=Invoke-RestMethod -Uri ('http://127.0.0.1:18125/api/v1/message/'+$message.ID)
 if($body.Text -notmatch '[?&]token=([A-Za-z0-9_-]+)'){throw 'Missing valid verification link'}
 $raw=$Matches[1]
 Call-AuditMail POST '/api/auth/student/verify-email' @{token=$raw} '' 204|Out-Null
 Call-AuditMail POST '/api/auth/student/verify-email' @{token=$raw} '' 400|Out-Null
 [pscustomobject]@{result='PASS';state=$entry.state;attempts=$entry.attempts;checks='SMTP outage, persistent RETRY, backend restart, same queue delivered to Mailpit and verification link single use'}|Format-List
}finally{& $docker start credentials-v3-audit-mailpit-1|Out-Null}