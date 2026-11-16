param([Parameter(Mandatory)][string]$Directory,[string]$OpenSSL='openssl')
$ErrorActionPreference='Stop'
if(!$env:CREDENTIAL_BACKUP_PASSPHRASE -or $env:CREDENTIAL_BACKUP_PASSPHRASE.Length -lt 20){throw 'Configura CREDENTIAL_BACKUP_PASSPHRASE (20 caracteres como mínimo) fuera del chat.'}
$folder=[IO.Path]::GetFullPath($Directory)
$null=New-Item -ItemType Directory -Force -Path $folder
$key=Join-Path $folder 'recovery-key.pem';$cert=Join-Path $folder 'recovery-certificate.pem'
if((Test-Path -LiteralPath $key) -or (Test-Path -LiteralPath $cert)){throw 'El destinatario ya existe. No se reemplaza una clave de recuperación.'}
& $OpenSSL genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -aes-256-cbc -pass env:CREDENTIAL_BACKUP_PASSPHRASE -out $key 2>$null
if($LASTEXITCODE -ne 0){throw 'No se pudo generar la clave cifrada de recuperación.'}
& $OpenSSL req -new -x509 -sha256 -days 3650 -key $key -passin env:CREDENTIAL_BACKUP_PASSPHRASE -subj '/CN=Credential Backup Recovery' -out $cert
if($LASTEXITCODE -ne 0){throw 'No se pudo generar el certificado de recuperación.'}
[pscustomobject]@{Certificate=$cert;EncryptedPrivateKey=$key;Note='Conserva clave y frase de recuperación en una bóveda separada del backup.'}
