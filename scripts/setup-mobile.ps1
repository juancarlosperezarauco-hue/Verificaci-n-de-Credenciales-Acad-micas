param([string]$Flutter='flutter')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$mobile=Join-Path $root 'mobile'
Push-Location $mobile
try {
 # Flutter create can regenerate the lockfile even with --no-pub.
 # Restore the application's manifests before resolving the pinned dependencies.
 $manifests=@{}
 foreach($name in @('pubspec.yaml','pubspec.lock')){
  $path=Join-Path $mobile $name
  if(Test-Path -LiteralPath $path){$manifests[$path]=[IO.File]::ReadAllBytes($path)}
 }
 try {
  & $Flutter create --platforms=android --empty --project-name credential_wallet --org bo.edu.uagrm --no-pub .
  if($LASTEXITCODE -ne 0){throw 'No se pudo preparar Android.'}
 }finally{
  foreach($path in $manifests.Keys){[IO.File]::WriteAllBytes($path,$manifests[$path])}
 }
 $activity=Join-Path $mobile 'android/app/src/main/kotlin/bo/edu/uagrm/credential_wallet/MainActivity.kt'
 $null=New-Item -ItemType Directory -Force -Path (Split-Path $activity)
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/MainActivity.kt') -Destination $activity -Force
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/AndroidManifest.xml') -Destination 'android/app/src/main/AndroidManifest.xml' -Force
 $null=New-Item -ItemType Directory -Force -Path 'android/app/src/debug/res/xml'
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/DebugManifest.xml') -Destination 'android/app/src/debug/AndroidManifest.xml' -Force
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/debug_network_security_config.xml') -Destination 'android/app/src/debug/res/xml/debug_network_security_config.xml' -Force
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/build.gradle.kts') -Destination 'android/app/build.gradle.kts' -Force
 $properties=Join-Path $mobile 'android/gradle.properties'
 $settings=Get-Content -LiteralPath $properties -Raw
 $settings=$settings.Replace('android.newDsl=false','android.newDsl=true').Replace('android.builtInKotlin=false','android.builtInKotlin=true')
 $settings=[regex]::Replace($settings,'(?m)^org.gradle.jvmargs=.*','org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:+HeapDumpOnOutOfMemoryError')
 if(!$settings.Contains('org.gradle.workers.max=')){$settings+=[Environment]::NewLine+'org.gradle.workers.max=2'+[Environment]::NewLine}
 [IO.File]::WriteAllText($properties,$settings)
 & $Flutter pub get --enforce-lockfile
 if($LASTEXITCODE -ne 0){throw 'No se pudieron resolver las dependencias móviles.'}
}finally {Pop-Location}
