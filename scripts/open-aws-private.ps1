param([switch]$OpenBrowser)
$ErrorActionPreference='Stop'
if (!$IsWindows) { throw 'Este lanzador es para Windows. Usa connect-aws-private.ps1 en una terminal.' }
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$connector=Join-Path $PSScriptRoot 'connect-aws-private.ps1'
$state=Join-Path $root '.local-backups/aws-private'
$null=New-Item -ItemType Directory -Path $state -Force
$pwsh=(Get-Process -Id $PID).Path
foreach($service in @('Application','Mail')) {
    $pidFile=Join-Path $state "tunnel-$service.pid"
    $running=$null
    if(Test-Path -LiteralPath $pidFile) {
        $saved=[IO.File]::ReadAllText($pidFile).Trim()
        if($saved -match '^[0-9]+$') {
            $candidate=Get-CimInstance Win32_Process -Filter "ProcessId=$saved" -ErrorAction SilentlyContinue
            if($candidate -and $candidate.CommandLine.Contains($connector) -and $candidate.CommandLine.Contains('-Reconnect')) { $running=$candidate }
        }
    }
    if(!$running) {
        $port=if($service -eq 'Application'){4302}else{8126}
        if(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "El puerto $port está ocupado por otro proceso. No se modificará." }
        $process=Start-Process -FilePath $pwsh -ArgumentList @('-NoProfile','-File',('"{0}"' -f $connector),'-Service',$service,'-Reconnect') -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput (Join-Path $state "tunnel-$service.log") -RedirectStandardError (Join-Path $state "tunnel-$service-error.log") -PassThru
        [IO.File]::WriteAllText($pidFile,[string]$process.Id)
    }
}
$ready=$false
$deadline=[DateTime]::UtcNow.AddSeconds(60)
do {
    try { $response=Invoke-WebRequest -Uri 'http://localhost:4302/' -NoProxy -TimeoutSec 3; $ready=$response.StatusCode -eq 200 } catch { $ready=$false }
    if(!$ready) { Start-Sleep -Seconds 2 }
} while(!$ready -and [DateTime]::UtcNow -lt $deadline)
if(!$ready) { throw 'No se abrió la aplicación. Revisa .local-backups/aws-private/tunnel-Application-error.log y la sesión AWS.' }
Write-Output 'Aplicación disponible: http://localhost:4302/ (servidor AWS, túnel privado).'
if($OpenBrowser) { Start-Process 'http://localhost:4302/' }
