param([Parameter(Mandatory)][string]$Snapshot,[Parameter(Mandatory)][string]$RecoveryKey,
 [string]$Docker='docker',[string]$OpenSSL='openssl')
$ErrorActionPreference='Stop'
if(!$env:CREDENTIAL_BACKUP_PASSPHRASE){throw 'Configura la frase de recuperación fuera del chat.'}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));Push-Location $root
$compose=$null
try{
 $source=(Resolve-Path -LiteralPath $Snapshot).Path;$key=(Resolve-Path -LiteralPath $RecoveryKey).Path
 $project='credentials-restore-'+[guid]::NewGuid().ToString('N').Substring(0,12)
 $folder=Join-Path $root ('.local-backups/restores/'+$project)
 $null=New-Item -ItemType Directory -Force -Path $folder
 $archive=Join-Path $folder 'decrypted.tar';$payload=Join-Path $folder 'payload'
 & $OpenSSL cms -decrypt -binary -inform DER -in $source -inkey $key -passin env:CREDENTIAL_BACKUP_PASSPHRASE -out $archive
 if($LASTEXITCODE -ne 0){if(Test-Path -LiteralPath $archive){Remove-Item -LiteralPath $archive};throw 'No se pudo autenticar y descifrar el backup.'}
 & python (Join-Path $PSScriptRoot 'unpack-backup.py') $archive $payload
 if($LASTEXITCODE -ne 0){throw 'La integridad del backup no es válida.'}
 Remove-Item -LiteralPath $archive
 $compose=@('compose','-p',$project,'--env-file',(Join-Path $payload 'keys.env'),'-f','compose.restore.yml')
 & $Docker @compose create | Out-Null;if($LASTEXITCODE -ne 0){throw 'No se pudo crear el entorno de restauración.'}
 $pg=(& $Docker @compose ps -a -q postgres);$minio=(& $Docker @compose ps -a -q minio)
 foreach($id in @($pg,$minio)){
  if((& $Docker inspect --format '{{index .Config.Labels "com.docker.compose.project"}}' $id) -ne $project){throw 'Contenedor fuera del proyecto de ensayo.'}
 }
 & $Docker cp ((Join-Path $payload 'minio')+[IO.Path]::DirectorySeparatorChar+'.') ($minio+':/data')
 if($LASTEXITCODE -ne 0){throw 'No se pudo restaurar MinIO.'}
 & $Docker @compose up -d --wait --wait-timeout 90 | Out-Null
 if($LASTEXITCODE -ne 0){throw 'El entorno de restauración no está listo.'}
 & $Docker cp (Join-Path $payload 'database.dump') ($pg+':/tmp/restore.dump')
 if($LASTEXITCODE -ne 0){throw 'No se pudo copiar la base de datos.'}
 & $Docker exec $pg sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --exit-on-error /tmp/restore.dump'
 if($LASTEXITCODE -ne 0){throw 'Falló pg_restore.'}
 $sql='select json_build_object(''tenants'',(select count(*) from tenants),''students'',(select count(*) from student_accounts),''credentials'',(select count(*) from credentials),''audit'',(select count(*) from audit_events),''managedObjects'',(select count(*) from managed_objects))::text'
 $restored=($sql | & $Docker exec -i $pg sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1')
 if($LASTEXITCODE -ne 0){throw 'No se pudieron comprobar los datos restaurados.'}
 $expected=Get-Content -LiteralPath (Join-Path $payload 'baseline.json') -Raw | ConvertFrom-Json
 $actual=($restored -join [Environment]::NewLine)|ConvertFrom-Json
 foreach($p in $expected.PSObject.Properties){if($actual.($p.Name) -ne $p.Value){throw ('La restauración difiere en '+$p.Name)}}
 & $Docker exec $pg wget -q --spider http://minio:9000/minio/health/ready
 if($LASTEXITCODE -ne 0){throw 'MinIO restaurado no responde.'}
 $report=@{result='PASS';project=$project;verifiedAt=[DateTime]::UtcNow.ToString('O');counts=$actual;sourceSHA256=(Get-FileHash -LiteralPath $source).Hash;limits='Conteos PostgreSQL y manifiesto comprobados. Falta ensayo de aplicación y acceso a documentos con las claves restauradas.'}
 $report|ConvertTo-Json -Depth 5|Set-Content -LiteralPath (Join-Path $folder 'restore-report.json')
 [pscustomobject]@{Result='PASS';Project=$project;Report=(Join-Path $folder 'restore-report.json')}
}finally{
 if($compose){& $Docker @compose stop | Out-Null}
 Pop-Location
}
