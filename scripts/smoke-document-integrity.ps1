param(
    [string]$ApiBase = 'http://127.0.0.1:18081',
    [Parameter(Mandatory)][string]$Token,
    [Parameter(Mandatory)][string]$StudentId
)
$ErrorActionPreference = 'Stop'
$headers = @{Authorization = 'Bearer ' + $Token}
$tempDir = Join-Path ([System.IO.Path]::GetTempPath()) ('credential-integrity-' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $tempDir
$uploadPath = Join-Path $tempDir 'credential.pdf'
$downloadPath = Join-Path $tempDir 'download.pdf'
try {
    $created = Invoke-RestMethod -Method Post -Uri ($ApiBase + '/api/credentials') -Headers $headers `
        -ContentType 'application/json' -Body (@{studentId = $StudentId; credentialType = 'CERTIFICADO'; title = 'Prueba de integridad'} | ConvertTo-Json)
    $path = $ApiBase + '/api/credentials/' + $created.id
    $null = Invoke-RestMethod -Method Post -Uri ($path + '/generate-document') -Headers $headers
    Invoke-WebRequest -Uri ($path + '/document') -Headers $headers -OutFile $uploadPath
    $original = [System.IO.File]::ReadAllBytes($uploadPath)
    $null = Invoke-RestMethod -Method Post -Uri ($path + '/document') -Headers $headers -Form @{file = Get-Item -LiteralPath $uploadPath}
    $issued = Invoke-RestMethod -Method Patch -Uri ($path + '/issue') -Headers $headers
    [System.IO.File]::WriteAllBytes($uploadPath, [System.Text.Encoding]::UTF8.GetBytes('%PDF-modified-test-content'))
    foreach ($state in @('ISSUED', 'REVOKED')) {
        if ($state -eq 'REVOKED') {
            $null = Invoke-RestMethod -Method Patch -Uri ($path + '/revoke') -Headers $headers `
                -ContentType 'application/json' -Body '{"reason":"Prueba de conservación documental"}'
        }
        $rejected = $false
        try {
            $null = Invoke-RestMethod -Method Post -Uri ($path + '/document') -Headers $headers -Form @{file = Get-Item -LiteralPath $uploadPath}
        } catch {
            if ([int]$_.Exception.Response.StatusCode -ne 400) { throw }
            $rejected = $true
        }
        if (-not $rejected) { throw 'Se permitió reemplazar un documento publicado' }
        Invoke-WebRequest -Uri ($path + '/document') -Headers $headers -OutFile $downloadPath
        $actual = [System.IO.File]::ReadAllBytes($downloadPath)
        $hash = [Convert]::ToHexString([System.Security.Cryptography.SHA256]::HashData($actual)).ToLowerInvariant()
        if ([Convert]::ToBase64String($original) -ne [Convert]::ToBase64String($actual) -or $hash -ne $issued.documentHash) {
            throw 'El documento confirmado o su hash fueron alterados'
        }
        $public = Invoke-RestMethod -Uri ($ApiBase + '/api/public/credentials/' + $issued.verificationCode)
        if ($public.status -ne $state -or $public.documentHash -ne $hash) { throw 'Verificación inconsistente' }
    }
    [pscustomobject]@{result = 'PASS'; credentialId = $created.id; checks = 'Carga real, emisión, rechazo sin alteración, hash y revocación en PostgreSQL/MinIO'}
} finally {
    foreach ($file in @($uploadPath, $downloadPath)) {
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file }
    }
    Remove-Item -LiteralPath $tempDir
}
