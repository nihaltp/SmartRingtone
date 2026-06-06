import java.util.Properties

val keystorePropertiesFile = file("key.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "com.nihaltp.smartringtone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nihaltp.smartringtone"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["storeFile"] as String)
            storePassword = keystoreProperties["storePassword"] as String
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // WorkManager background processing
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Gson for JSON Serialization in SharedPreferences
    implementation("com.google.code.gson:gson:2.10.1")

    // Tooling/Test
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("tools.fastlane:screengrab:2.1.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
}

// https://stackoverflow.com/questions/70917287/how-to-assign-different-versioncode-for-multiple-architecture-apks-built-with-fl/70942242#70942242
// Map each ABI to a unique integer suffix
val abiCodes =
    mapOf(
        "armeabi-v7a" to 1,
        "arm64-v8a" to 2,
        "x86" to 3,
        "x86_64" to 4,
    )

android.applicationVariants.all {
    val variant = this
    variant.outputs.all {
        val output = this as? com.android.build.gradle.api.ApkVariantOutput
        val abi = output?.getFilter(com.android.build.VariantOutput.FilterType.ABI)
        if (abi != null) {
            val baseAbiVersionCode = abiCodes[abi] ?: 0
            output.versionCodeOverride = variant.versionCode * 10 + baseAbiVersionCode
        } else {
            output?.versionCodeOverride = variant.versionCode * 10
        }
    }
}
