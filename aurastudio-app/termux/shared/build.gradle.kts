plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux.shared"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    api(project(":termux:view"))

    implementation("androidx.annotation:annotation:1.9.1")
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.guava:guava:33.4.0-android")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("commons-io:commons-io:2.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:6.1")

    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:linkify:4.6.2")
    implementation("io.noties.markwon:recycler:4.6.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}