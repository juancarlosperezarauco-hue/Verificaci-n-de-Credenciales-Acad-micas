import java.util.Properties
import com.android.build.api.dsl.ApplicationExtension
plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

extensions.configure<ApplicationExtension> {
    namespace = "bo.edu.uagrm.credential_wallet"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "bo.edu.uagrm.credential_wallet"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        // Uses the version code from pubspec.yaml. When using split APKs, 1000 * ABI_VERSION
        // is added automatically by Flutter. (https://developer.android.com/studio/build/configure-apk-splits#configure-APK-versions)
        // You can force using the value of versionCode by specifying the `-P force-version-code-ignoring-abi=true`
        // flag during build.
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // A production key must be configured separately.
            signingConfig = null
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

// Credential release signing: never silently sign a release with the debug key.
val credentialKeyFile = rootProject.file("key.properties")
val credentialKeys = Properties()
if (credentialKeyFile.exists()) {
    credentialKeyFile.inputStream().use { credentialKeys.load(it) }
    extensions.getByType<ApplicationExtension>().signingConfigs.create("credentialRelease") {
        keyAlias = credentialKeys.getProperty("keyAlias")
        keyPassword = credentialKeys.getProperty("keyPassword")
        storeFile = file(credentialKeys.getProperty("storeFile"))
        storePassword = credentialKeys.getProperty("storePassword")
    }
    extensions.getByType<ApplicationExtension>().apply {
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("credentialRelease")
    }
}
val validateCredentialSigning = tasks.register("validateCredentialSigning") {
    doLast {
        if (!credentialKeyFile.exists()) {
            throw GradleException("Configura android/key.properties y una clave privada de publicaci\u00f3n.")
        }
    }
}
tasks.configureEach {
    if (name.contains("Release", ignoreCase = true)) {
        dependsOn(validateCredentialSigning)
    }
}
