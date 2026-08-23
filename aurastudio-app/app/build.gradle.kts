plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.hinohara.aurastudio"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    defaultConfig {
        applicationId = "com.hinohara.aurastudio"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndk {
            abiFilters += setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")
        }

        externalNativeBuild {
            cmake {
                cppFlags += ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // Material 3 Expressive
    implementation("androidx.compose.material3:material3:1.4.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")

    // Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended")

    // Activity & Lifecycle
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")

    // Core KTX
    implementation("androidx.core:core-ktx:1.17.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.register<Copy>("copyBootstrap") {
    description = "Copy bootstrap zip to assets if not already present"
    val arch = when {
        org.gradle.internal.os.OperatingSystem.current().isLinux -> "aarch64"
        else -> "aarch64"
    }
    val destFile = file("src/main/assets/bootstrap-$arch.zip")
    val candidates = listOf(
        System.getProperty("user.home") + "/bootstrap-$arch.zip",
        System.getProperty("user.home") + "/termux-app/app/src/main/jniLibs/arm64-v8a/libtermux-bootstrap.so"
    )
    val source = candidates.map { file(it) }.firstOrNull { it.exists() }
    if (source != null && source.length() > 1_000_000 && !destFile.exists()) {
        from(source)
        into("src/main/assets")
        if (source.name.endsWith(".so")) {
            rename { "bootstrap-$arch.zip" }
        }
    } else {
        enabled = false
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    dependsOn("copyBootstrap")
}
