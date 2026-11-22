param(
 [switch]$StartDocker,
 [switch]$Build,
 [switch]$Prebuilt,
 [ValidateRange(30,600)][int]$WaitSeconds=180,
 [string]$Docker='docker'
)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if(!(Test-Path -LiteralPath (Join-Path $root '.env'))){
 throw 'Falta el .env privado del proyecto. Configúralo desde .env.example; no se generarán ni reemplazarán claves existentes.'
}
if(!(Get-Command $Docker -ErrorAction SilentlyContinue)){
 $installed=if($IsWindows){Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/resources/bin/docker.exe'}else{''}
 if($installed -and (Test-Path -LiteralPath $installed)){$Docker=$installed}
 else{throw 'No se encontró Docker. Instala Docker Desktop/Engine y Docker Compose.'}
}
function Get-EngineType {
 $probe=[Diagnostics.Process]::new()
 $probe.StartInfo=[Diagnostics.ProcessStartInfo]::new()
 $probe.StartInfo.FileName=$Docker
 foreach($arg in @('version','--format','{{.Server.Os}}')){$probe.StartInfo.ArgumentList.Add($arg)}
 $probe.StartInfo.UseShellExecute=$false
 $probe.StartInfo.CreateNoWindow=$true
 $probe.StartInfo.RedirectStandardOutput=$true
 $probe.StartInfo.RedirectStandardError=$true
 try{
  $null=$probe.Start()
  $output=$probe.StandardOutput.ReadToEndAsync()
  $errors=$probe.StandardError.ReadToEndAsync()
  if(!$probe.WaitForExit(5000)){
   $probe.Kill($true);$null=$probe.WaitForExit(2000);return ''
  }
  if($probe.ExitCode -eq 0){return $output.GetAwaiter().GetResult().Trim()}
  return ''
 }finally{$probe.Dispose()}
}
Push-Location $root
try{
 # This command targets this computer, never a remote Docker context.
 $endpoint=if($env:DOCKER_HOST){$env:DOCKER_HOST}else{
  $value=& $Docker context inspect --format '{{.Endpoints.docker.Host}}'
  if($LASTEXITCODE -ne 0){throw 'No se pudo consultar el contexto de Docker.'}
  [string]$value
 }
 if($endpoint -notmatch '^(npipe|unix)://'){throw 'start-local requiere un contexto Docker local, sin conexiones remotas.'}
 $engine=Get-EngineType
 if(!$engine -and $StartDocker){
  if(!$IsWindows){throw 'Inicia el servicio Docker del sistema y vuelve a ejecutar el comando.'}
  $desktop=Join-Path $env:LOCALAPPDATA 'Programs/DockerDesktop/Docker Desktop.exe'
  if(!(Test-Path -LiteralPath $desktop)){throw 'Abre Docker Desktop manualmente; no se encontró su ejecutable.'}
  Start-Process -FilePath $desktop -WindowStyle Hidden
  $timer=[Diagnostics.Stopwatch]::StartNew()
  while(!$engine -and $timer.Elapsed.TotalSeconds -lt $WaitSeconds){
   Start-Sleep -Seconds 2
   $engine=Get-EngineType
  }
 }
 if(!$engine){throw 'Docker no responde. Abre Docker Desktop o usa -StartDocker; no se modificarán contenedores ni volúmenes.'}
 if($engine -ne 'linux'){throw 'Selecciona el motor de contenedores Linux en Docker Desktop.'}
 $compose=@('compose','-p','verificacion-credenciales','--env-file',(Join-Path $root '.env'),'-f','docker-compose.yml')
 if($Prebuilt){
  foreach($file in @('backend/target/credential-api-0.0.1-SNAPSHOT.jar','frontend/dist/frontend/browser/index.html')){
   if(!(Test-Path -LiteralPath (Join-Path $root $file))){throw 'Compila backend y frontend antes de usar -Prebuilt.'}
  }
  $compose+=@('-f','compose.prebuilt.yml')
 }
 & $Docker @compose config --quiet
 if($LASTEXITCODE -ne 0){throw 'La configuración Compose no es válida. Revisa los nombres requeridos en .env sin compartir sus valores.'}
 $up=@('up','-d','--wait','--wait-timeout',[string]$WaitSeconds)
 if($Build){$up+='--build'}
 & $Docker @compose @up
 if($LASTEXITCODE -ne 0){throw 'El proyecto no quedó listo dentro del plazo. Consulta docker compose ps y los logs del servicio afectado.'}
 function Local-Port([string]$Service,[string]$Port){
  $mapping=& $Docker @compose port $Service $Port
  if($LASTEXITCODE -ne 0 -or [string]$mapping -notmatch ':(\d+)\s*$'){throw 'No se pudo consultar un puerto publicado del proyecto.'}
  return $Matches[1]
 }
 $api='http://localhost:'+(Local-Port 'backend' '8080')
 $web='http://localhost:'+(Local-Port 'frontend' '80')
 & (Join-Path $PSScriptRoot 'check-health.ps1') -Api $api -Web $web
 if($LASTEXITCODE -ne 0){throw 'Los contenedores arrancaron, pero la comprobación HTTP detectó un fallo.'}
 Write-Output ('Proyecto listo: '+$web)
}finally{Pop-Location}
