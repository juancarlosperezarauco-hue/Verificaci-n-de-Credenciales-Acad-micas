param([switch]$Check)

$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$version = '2.0.0'
$tool = Join-Path $root ('.local-backups/tools/google-java-format-' + $version + '-all-deps.jar')
if (!(Test-Path -LiteralPath $tool)) {
    $null = New-Item -ItemType Directory -Force -Path (Split-Path $tool)
    $url = 'https://github.com/google/google-java-format/releases/download/v' + $version + '/google-java-format-' + $version + '-all-deps.jar'
    Invoke-WebRequest $url -OutFile $tool
}

Push-Location $root
try {
    $base = 'backend/src/main/java/bo/edu/uagrm/credenciales'
    $files = @()
    foreach ($folder in @('academic', 'workflow', 'template', 'signing', 'credential', 'identityreview', 'storage', 'shared', 'notification', 'report')) {
        $files += @(Get-ChildItem -LiteralPath ($base + '/' + $folder) -Filter '*.java' -File | ForEach-Object FullName)
    }
    $files += ($base + '/identity/IdentityService.java')
    $files += ($base + '/consent/ConsentService.java')
    $options = @('--aosp', '--cleanup=false', '--skip-reflowing-long-strings')
    if ($Check) { $options += @('--dry-run', '--set-exit-if-changed') }
    else { $options += '--replace' }
    & java -jar $tool @options @files
    if ($LASTEXITCODE -ne 0) { throw 'El formato Java requiere revisión.' }
    Write-Output ('Formato Java verificado: ' + $files.Count + ' archivos.')
} finally {
    Pop-Location
}
