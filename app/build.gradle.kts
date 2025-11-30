plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "io.github.harrbca.rackxlate"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "io.github.harrbca.rackxlate"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1-poc"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this
            // The type check ensures this logic only applies to APK outputs
            if (output is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                // You can customize the name format here
                // Example: com.buckwold.scanner.rackmapper-1.0-release.apk
                output.outputFileName = "RackXLate-${variant.versionName}.apk"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.work.runtime.ktx.v290)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}