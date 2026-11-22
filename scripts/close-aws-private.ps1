$ErrorActionPreference='Stop'
if (!$IsWindows) { throw 'Este lanzador es para Windows.' }
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$connector=Join-Path $PSScriptRoot 'connect-aws-private.ps1'
foreach($service in @('Application','Mail')) {
    $pidFile=Join-Path $root ".local-backups/aws-private/tunnel-$service.pid"
    if(!(Test-Path -LiteralPath $pidFile)){continue}
    $saved=[IO.File]::ReadAllText($pidFile).Trim()
    if($saved -notmatch '^[0-9]+$'){throw 'Identificador de proceso no válido.'}
    $candidate=Get-CimInstance Win32_Process -Filter "ProcessId=$saved" -ErrorAction SilentlyContinue
    if($candidate) {
        if(!$candidate.CommandLine.Contains($connector) -or !$candidate.CommandLine.Contains('-Reconnect')) { throw 'El proceso ya no pertenece a este acceso. No se cerrará.' }
        [Diagnostics.Process]::GetProcessById([int]$saved).Kill($true)
    }
    Remove-Item -LiteralPath $pidFile
}
Write-Output 'Túneles locales cerrados. El servidor AWS continúa encendido; no se modificaron sus servicios ni datos.'
