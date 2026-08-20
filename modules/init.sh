#!/data/data/com.termux/files/usr/bin/env bash

cmd_init() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Project Starter Initializer (Template Generator)${RESET}"
    draw_divider

    printf "\n  %b\n\n" "${BOLD}Select project template:${RESET}"
    printf "  %b C++ Native CMake Starter (CLI Console App)\n" "${CYAN}[1]${RESET}"
    printf "  %b Android NDK Shared Library Starter (libnative.so)\n" "${CYAN}[2]${RESET}"
    printf "  %b Android App Starter - Java (Gradle + Gradlew)\n" "${CYAN}[3]${RESET}"
    printf "  %b Android App Starter - Kotlin (Gradle + Gradlew)\n" "${CYAN}[4]${RESET}"
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-4/q]: "
    read -r t_sel

    [ "$t_sel" = "q" ] && info "Cancelled." && return

    printf "  Enter project folder name (default: MyAuraApp): "
    read -r proj_name
    proj_name="${proj_name:-MyAuraApp}"

    local target_dir="$PWD/$proj_name"
    if [ -d "$target_dir" ]; then
        error "Directory $proj_name already exists!"
        return
    fi

    mkdir -p "$target_dir"

    case "$t_sel" in
        1)
            cat > "$target_dir/CMakeLists.txt" << 'EOF'
cmake_minimum_required(VERSION 3.22.1)
project(MyNativeApp CXX)

set(CMAKE_CXX_STANDARD 17)

add_executable(my_app main.cpp)
EOF
            cat > "$target_dir/main.cpp" << 'EOF'
#include <iostream>

int main() {
    std::cout << "Hello from AuraStudio Native C++ Console App!" << std::endl;
    return 0;
}
EOF
            cat > "$target_dir/build.sh" << 'EOF'
#!/usr/bin/env bash
mkdir -p build && cd build
cmake ..
make -j$(nproc)
echo -e "\n[+] Build finished. Run binary with: ./my_app"
EOF
            chmod +x "$target_dir/build.sh"
            success "Created C++ CMake Console project at: ${CYAN}$target_dir${RESET}"
            info "To build and run: cd $proj_name && ./build.sh"
            ;;

        2)
            cat > "$target_dir/CMakeLists.txt" << 'EOF'
cmake_minimum_required(VERSION 3.22.1)
project(NativeLib CXX)

add_library(native_lib SHARED native.cpp)
EOF
            cat > "$target_dir/native.cpp" << 'EOF'
#include <iostream>

extern "C" void hello_from_native() {
    std::cout << "Hello from Native Shared Library!" << std::endl;
}
EOF
            cat > "$target_dir/build.sh" << 'EOF'
#!/usr/bin/env bash
mkdir -p build && cd build
cmake ..
make -j$(nproc)
echo -e "\n[+] Build finished. Shared library generated in build directory."
EOF
            chmod +x "$target_dir/build.sh"
            success "Created NDK Shared Library project at: ${CYAN}$target_dir${RESET}"
            info "To build: cd $proj_name && ./build.sh"
            ;;

        3|4)
            local pkg_name="com.aurastudio.app"
            local is_kotlin=false
            [ "$t_sel" = "4" ] && is_kotlin=true

            info "Generating Android Gradle structure..."

            cat > "$target_dir/settings.gradle" << EOF
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "$proj_name"
include ':app'
EOF

            cat > "$target_dir/build.gradle" << EOF
plugins {
    id 'com.android.application' version '8.2.2' apply false
    $([ "$is_kotlin" = true ] && echo "id 'org.jetbrains.kotlin.android' version '1.9.22' apply false")
}
EOF

            cat > "$target_dir/gradle.properties" << 'EOF'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
EOF

            mkdir -p "$target_dir/app/src/main/res/layout"
            mkdir -p "$target_dir/app/src/main/res/values"

            cat > "$target_dir/app/build.gradle" << EOF
plugins {
    id 'com.android.application'
    $([ "$is_kotlin" = true ] && echo "id 'org.jetbrains.kotlin.android'")
}

android {
    namespace '$pkg_name'
    compileSdk 34

    defaultConfig {
        applicationId "$pkg_name"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    $([ "$is_kotlin" = true ] && echo -e "kotlinOptions {\n        jvmTarget = '17'\n    }")
}

dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
}
EOF

            cat > "$target_dir/app/src/main/AndroidManifest.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="$proj_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
EOF

            cat > "$target_dir/app/src/main/res/layout/activity_main.xml" << 'EOF'
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:orientation="vertical">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Hello from AuraStudio Android App!"
        android:textSize="18sp"
        android:textStyle="bold" />
</LinearLayout>
EOF

            if [ "$is_kotlin" = true ]; then
                mkdir -p "$target_dir/app/src/main/java/com/aurastudio/app"
                cat > "$target_dir/app/src/main/java/com/aurastudio/app/MainActivity.kt" << 'EOF'
package com.aurastudio.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
EOF
            else
                mkdir -p "$target_dir/app/src/main/java/com/aurastudio/app"
                cat > "$target_dir/app/src/main/java/com/aurastudio/app/MainActivity.java" << 'EOF'
package com.aurastudio.app;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
EOF
            fi

            if command -v gradle &>/dev/null; then
                (_gen_wrapper() {
                    cd "$target_dir" || return 1
                    gradle wrapper --gradle-version 8.5 >/dev/null 2>&1
                }; _gen_wrapper) &
                spin $! "Generating Gradle Wrapper (gradlew)"
            fi

            success "Created Android Gradle project ($([ "$is_kotlin" = true ] && echo "Kotlin" || echo "Java")) at: ${CYAN}$target_dir${RESET}"
            info "To build APK: cd $proj_name && ./gradlew assembleDebug"
            info "Output APK path: app/build/outputs/apk/debug/app-debug.apk"
            ;;

        *)
            error "Invalid option."
            ;;
    esac
    echo ""
}
