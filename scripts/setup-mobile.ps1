param([string]$Flutter='flutter')
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$mobile=Join-Path $root 'mobile'
Push-Location $mobile
try {
 & $Flutter create --platforms=android --empty --project-name credential_wallet --org bo.edu.uagrm --no-pub .
 if($LASTEXITCODE -ne 0){throw 'No se pudo preparar Android.'}
 $activity=Join-Path $mobile 'android/app/src/main/kotlin/bo/edu/uagrm/credential_wallet/MainActivity.kt'
 $null=New-Item -ItemType Directory -Force -Path (Split-Path $activity)
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/MainActivity.kt') -Destination $activity -Force
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/AndroidManifest.xml') -Destination 'android/app/src/main/AndroidManifest.xml' -Force
 $null=New-Item -ItemType Directory -Force -Path 'android/app/src/debug/res/xml'
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/DebugManifest.xml') -Destination 'android/app/src/debug/AndroidManifest.xml' -Force
 Copy-Item -LiteralPath (Join-Path $mobile 'platform/android/debug_network_security_config.xml') -Destination 'android/app/src/debug/res/xml/debug_network_security_config.xml' -Force
 $gradle=Join-Path $mobile 'android/app/build.gradle.kts';$source=Get-Content -LiteralPath $gradle -Raw
 if(!$source.Contains('// Credential release signing')){
  $source=$source.Replace('signingConfig = signingConfigs.getByName("debug")','signingConfig = null')
  if(!$source.Contains('import java.util.Properties')){$source="import java.util.Properties"+[Environment]::NewLine+$source}
  $source+=@'

// Credential release signing: never silently sign a release with the debug key.
val credentialKeyFile = rootProject.file("key.properties")
val credentialKeys = Properties()
if (credentialKeyFile.exists()) {
    credentialKeyFile.inputStream().use { credentialKeys.load(it) }
    android.signingConfigs.create("credentialRelease") {
        keyAlias = credentialKeys.getProperty("keyAlias")
        keyPassword = credentialKeys.getProperty("keyPassword")
        storeFile = file(credentialKeys.getProperty("storeFile"))
        storePassword = credentialKeys.getProperty("storePassword")
    }
    android.buildTypes.getByName("release").signingConfig = android.signingConfigs.getByName("credentialRelease")
}
gradle.taskGraph.whenReady { graph ->
    if (graph.allTasks.any { it.name.contains("Release", ignoreCase = true) } && !credentialKeyFile.exists()) {
        throw GradleException("Configura android/key.properties y una clave privada de publicación.")
    }
}
'@
  [IO.File]::WriteAllText($gradle,$source)
 }
 $properties=Join-Path $mobile 'android/gradle.properties'
 $settings=Get-Content -LiteralPath $properties -Raw
 $settings=[regex]::Replace($settings,'(?m)^org.gradle.jvmargs=.*','org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -XX:+HeapDumpOnOutOfMemoryError')
 if(!$settings.Contains('org.gradle.workers.max=')){$settings+=[Environment]::NewLine+'org.gradle.workers.max=2'+[Environment]::NewLine}
 [IO.File]::WriteAllText($properties,$settings)
 & $Flutter pub get
 if($LASTEXITCODE -ne 0){throw 'No se pudieron resolver las dependencias móviles.'}
}finally {Pop-Location}
