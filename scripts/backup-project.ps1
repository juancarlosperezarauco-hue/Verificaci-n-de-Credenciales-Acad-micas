param(
 [ValidateSet('verificacion-credenciales','credentials-v3-audit')][string]$Project='verificacion-credenciales',
 [Parameter(Mandatory)][string]$RecipientCertificate,
 [string]$EnvFile='.env',[string]$Docker='docker',[string]$OpenSSL='openssl'
)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Push-Location $root
$stage=$null;$tar=$null;$sealed=$false;$restart=@()
try{
 $envPath=(Resolve-Path -LiteralPath $EnvFile).Path
 $cert=(Resolve-Path -LiteralPath $RecipientCertificate).Path
 $compose=@('compose','-p',$Project,'--env-file',$envPath,'-f',$(if($Project -eq 'credentials-v3-audit'){'compose.audit.yml'}else{'docker-compose.yml'}))
 if($Project -eq 'verificacion-credenciales' -and (Test-Path '.local-backups/security02-runtime.yml')){$compose+=@('-f','.local-backups/security02-runtime.yml')}
 $ids=@{}
 foreach($service in @('postgres','minio','backend','frontend','ai-service')){
  $id=(& $Docker @compose ps -a -q $service 2>$null | Select-Object -First 1)
  if($id){$ids[$service]=$id}
 }
 if(!$ids.postgres -or !$ids.minio){throw 'No se encontraron PostgreSQL y MinIO del proyecto indicado.'}
 if((& $Docker inspect --format '{{.State.Running}}' $ids.postgres) -ne 'true'){throw 'Inicia PostgreSQL del proyecto antes del backup.'}
 foreach($service in @('frontend','backend','ai-service','minio')){
  if($ids[$service] -and ((& $Docker inspect --format '{{.State.Running}}' $ids[$service]) -eq 'true')){
   $restart+=@($ids[$service]);& $Docker stop $ids[$service] | Out-Null;if($LASTEXITCODE -ne 0){throw 'No se pudo detener un escritor del proyecto.'}
  }
 }
 $name=(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[guid]::NewGuid().ToString('N').Substring(0,8)
 $base=Join-Path $root '.local-backups/snapshots';$null=New-Item -ItemType Directory -Force -Path $base
 $stage=[IO.Path]::GetFullPath((Join-Path $base ($name+'-staging')));$null=New-Item -ItemType Directory -Path $stage
 Copy-Item -LiteralPath $envPath -Destination (Join-Path $stage 'keys.env')
 & $Docker exec $ids.postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f /tmp/credential-snapshot.dump'
 if($LASTEXITCODE -ne 0){throw 'Falló pg_dump.'}
 & $Docker cp ($ids.postgres+':/tmp/credential-snapshot.dump') (Join-Path $stage 'database.dump')
 if($LASTEXITCODE -ne 0){throw 'No se pudo copiar el dump.'}
 $null=New-Item -ItemType Directory -Path (Join-Path $stage 'minio')
 & $Docker cp ($ids.minio+':/data/.') (Join-Path $stage 'minio')
 if($LASTEXITCODE -ne 0){throw 'No se pudo copiar el almacenamiento detenido.'}
 $counts='select json_build_object(''tenants'',(select count(*) from tenants),''students'',(select count(*) from student_accounts),''credentials'',(select count(*) from credentials),''audit'',(select count(*) from audit_events),''managedObjects'',(select count(*) from managed_objects))::text'
 $baseline=($counts | & $Docker exec -i $ids.postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At -v ON_ERROR_STOP=1')
 if($LASTEXITCODE -ne 0){throw 'No se pudo obtener el control de restauración.'}
 [IO.File]::WriteAllText((Join-Path $stage 'baseline.json'),($baseline -join [Environment]::NewLine))
 $files=@(Get-ChildItem -LiteralPath $stage -Force -File -Recurse | ForEach-Object {
  @{path=[IO.Path]::GetRelativePath($stage,$_.FullName).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
 })
 @{project=$Project;createdAt=[DateTime]::UtcNow.ToString('O');files=$files}|ConvertTo-Json -Depth 5|Set-Content -LiteralPath (Join-Path $stage 'manifest.json') -Encoding utf8NoBOM
 $tar=Join-Path $base ($name+'.tar');$destination=Join-Path $base ($name+'.cms')
 & tar -cf $tar -C $stage .
 if($LASTEXITCODE -ne 0){throw 'No se pudo empaquetar el snapshot.'}
 & $OpenSSL cms -encrypt -binary -aes-256-gcm -outform DER -recip $cert -in $tar -out $destination
 if($LASTEXITCODE -ne 0){throw 'No se pudo cifrar el snapshot.'}
 $sealed=$true
 Remove-Item -LiteralPath $tar
 [pscustomobject]@{Snapshot=$destination;SHA256=(Get-FileHash -LiteralPath $destination).Hash;Files=$files.Count;Project=$Project}
}finally{
 if($sealed -and $stage){
  $allowed=[IO.Path]::GetFullPath((Join-Path $root '.local-backups/snapshots'))+[IO.Path]::DirectorySeparatorChar
  if(!$stage.StartsWith($allowed,[StringComparison]::OrdinalIgnoreCase) -or !$stage.EndsWith('-staging')){throw 'Ruta de limpieza rechazada.'}
  Remove-Item -LiteralPath $stage -Recurse
 }
 [array]::Reverse($restart)
 foreach($id in $restart){& $Docker start $id | Out-Null}
 Pop-Location
}
