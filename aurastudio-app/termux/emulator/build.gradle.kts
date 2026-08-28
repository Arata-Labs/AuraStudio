plugins {
    id("com.android.library")
}

android {
    namespace = "com.termux.emulator"
    compileSdk = 36
    ndkVersion = "27.3.13750724"

    defaultConfig {
        minSdk = 26
        externalNativeBuild {
            cmake {
                cFlags("-std=c11", "-Wall", "-Wextra", "-Werror", "-Os", "-fno-stack-protector")
                abiFilters += setOf("arm64-v8a")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}