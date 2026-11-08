import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

val appVersion = Properties().apply {
    rootProject.file("version.properties").inputStream().use { load(it) }
}
val signingValues = listOf(
    "SG_RELEASE_STORE_FILE",
    "SG_RELEASE_STORE_PASSWORD",
    "SG_RELEASE_KEY_ALIAS",
    "SG_RELEASE_KEY_PASSWORD"
).map { providers.environmentVariable(it).orNull }
require(signingValues.all { it == null } || signingValues.all { !it.isNullOrBlank() }) {
    "Release signing configuration is incomplete; no secret values were displayed."
}

android {
    namespace = "com.screengarden.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.screengarden.app"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersion.getProperty("versionCode").toInt()
        versionName = appVersion.getProperty("versionName")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (signingValues.all { !it.isNullOrBlank() }) {
            create("privateRelease") {
                storeFile = file(requireNotNull(signingValues[0]))
                storePassword = signingValues[1]
                keyAlias = signingValues[2]
                keyPassword = signingValues[3]
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.findByName("privateRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        // Version bumps are handled deliberately in AGENT_MEMORY, not by lint nagging.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }

    testOptions {
        unitTests {
            // Robolectric needs the real merged resources to build a Context for
            // the in-memory Room tests.
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // MigrationTestHelper reads the exported schemas from the test APK assets
        // (DECISION D22). Without this the helper cannot find them.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Export Room schemas so migrations can be validated against history (D22).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Style is enforced by ktlint (see DECISION D16). `ktlintCheck` fails the build on
// any violation, `ktlintFormat` fixes the mechanical ones.
ktlint {
    version.set("1.5.0")
    android.set(true)
    filter {
        exclude("**/build/**")
        exclude("**/generated/**")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)

    // DAO tests run on the JVM through Robolectric, so CI needs no emulator.
    testImplementation(libs.junit)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    // Compose UI tests run on the JVM through Robolectric, so the first-run happy
    // path is part of the normal gate instead of needing an emulator (D56).
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
