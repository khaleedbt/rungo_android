import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Not committed (local.properties is gitignored) — each dev/CI machine needs
// its own copy with MAPS_API_KEY set, restricted to this package + their own
// debug/release signing SHA-1 in the Google Cloud Console.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

// Committed (unlike local.properties) — a shared, ever-increasing build
// counter so every APK that comes out of `assemble*` has a version distinct
// from the last one, visible on the Profile screen (BuildConfig.VERSION_NAME)
// without anyone having to remember to bump a number by hand. Only read here
// at config time; the actual increment+write happens in bumpBuildNumber
// below, `finalizedBy` a real assemble — not on every IDE sync, which
// re-evaluates this file constantly and would otherwise burn through numbers
// for builds that never happened.
val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
val buildNumber = versionProps.getProperty("BUILD_NUMBER", "1").toInt()

android {
    namespace = "dev.batipy.rungo"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.batipy.rungo"
        minSdk = 24
        targetSdk = 36
        versionCode = buildNumber
        versionName = "2.8.$buildNumber"

        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release keystore lives outside the repo history (gitignored, *.jks) —
    // credentials come from local.properties same as MAPS_API_KEY. Without
    // this, `assembleRelease` still "succeeds" but produces an unsigned APK
    // that most devices refuse to install — fine for local debug testing,
    // useless for actually handing the file to someone.
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE", "release-keystore.jks"))
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Names the actual output file "RunGo-2.8.<build>-<debug|release>.apk"
// instead of the generic "app-debug.apk" — so the file itself is
// identifiable (e.g. once shared/copied elsewhere) without installing it
// or digging through package info first.
androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            output.outputFileName.set("RunGo-2.8.$buildNumber-${variant.name}.apk")
        }
    }
}

// Bumps and persists version.properties right after a successful assemble —
// checked here (rather than incrementing eagerly above) so a failed build,
// or an IDE sync that never actually assembles anything, doesn't still burn
// a version number. Covers both APK (assemble*) and Android App Bundle
// (bundle*) tasks — Android Studio's "Generate Signed App Bundle or APK"
// wizard runs one or the other depending on which format is picked there,
// and either way should count as "a build happened."
gradle.taskGraph.afterTask {
    val versionedTasks = setOf("assembleDebug", "assembleRelease", "bundleDebug", "bundleRelease")
    if (name in versionedTasks && state.failure == null) {
        versionProps.setProperty("BUILD_NUMBER", (buildNumber + 1).toString())
        versionPropsFile.outputStream().use { versionProps.store(it, null) }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)
    coreLibraryDesugaring(libs.android.desugar.jdk.libs)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}