import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.nexus.browser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nexus.browser"
        minSdk = 29
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val githubOwner = System.getenv("NEXUS_GITHUB_OWNER")
            ?: localProps.getProperty("nexus.github.owner")
            ?: "KALIAZ2"
        val githubRepo = System.getenv("NEXUS_GITHUB_REPO")
            ?: localProps.getProperty("nexus.github.repo")
            ?: "NEXUS-Browser"

        buildConfigField("String", "GECKOVIEW_VERSION", "\"${libs.versions.geckoview.get()}\"")
        buildConfigField("String", "GITHUB_OWNER", "\"$githubOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    signingConfigs {
        val keystorePath = System.getenv("NEXUS_KEYSTORE_PATH") ?: localProps.getProperty("nexus.keystore.path")
        val resolvedKeystore = keystorePath?.takeIf { it.isNotBlank() }?.let { path ->
            rootProject.file(path).takeIf { it.exists() } ?: file(path).takeIf { it.exists() }
        }
        if (resolvedKeystore != null) {
            create("release") {
                storeFile = resolvedKeystore
                storePassword = System.getenv("NEXUS_KEYSTORE_PASSWORD") ?: localProps.getProperty("nexus.keystore.password")
                keyAlias = System.getenv("NEXUS_KEY_ALIAS") ?: localProps.getProperty("nexus.key.alias")
                keyPassword = System.getenv("NEXUS_KEY_PASSWORD") ?: localProps.getProperty("nexus.key.password")
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ""
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore Preferences
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Mozilla GeckoView Engine (arm64-v8a by default for physical phones, or universal if -PuniversalApk=true)
    val useUniversalGecko = project.findProperty("universalApk")?.toString()?.toBoolean() ?: false
    if (useUniversalGecko) {
        implementation(libs.mozilla.geckoview.universal)
    } else {
        implementation(libs.mozilla.geckoview.arm64)
    }

    // Network (for Downloads & Extension Catalog)
    implementation(libs.okhttp)

    // Unit & Instrumentation Tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.org.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
