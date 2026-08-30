package com.aurastudio.data.project

import android.content.Context
import android.os.Build
import android.os.Environment
import com.aurastudio.data.models.Project
import com.aurastudio.data.models.ProjectType
import java.io.File

/**
 * Generates project scaffolds in the device's Internal Storage
 * (`/storage/emulated/0/AuraStudio`). On Android 11+ this requires the
 * "All files access" permission (MANAGE_EXTERNAL_STORAGE); see
 * [externalStorageReady]. Content mirrors the CLI modules/init.sh scaffolds
 * (AGP 8.2.2 + Kotlin 1.9.22, compileSdk 34, minSdk 24, targetSdk 34, Java 17)
 * and AndroidIDE's (acs) project layout. A hidden marker file records which
 * template produced the project so the Projects screen can restore its type.
 */
class ProjectCreator(private val context: Context) {

    val projectsDir: File
        get() = File(Environment.getExternalStorageDirectory(), "AuraStudio").apply { mkdirs() }

    private val markerFile = ".aurastudio-template"

    private val aapt2Path: String get() = "${context.filesDir}/usr/bin/aapt2"

    fun create(request: CreateProjectRequest): Project {
        if (!externalStorageReady(context)) throw IllegalStateException(STORAGE_ACCESS_REQUIRED)
        val dirName = sanitizeDirName(request.name)
        val dir = File(projectsDir, dirName)
        if (dir.exists() && dir.list()?.isNotEmpty() == true) {
            throw IllegalArgumentException(dirName)
        }
        dir.mkdirs()

        writeFile(dir, markerFile, request.template.key)

        when (request.template) {
            ProjectTemplate.GRADLE_KOTLIN -> writeAndroidApp(dir, request, kotlin = true)
            ProjectTemplate.GRADLE_JAVA -> writeAndroidApp(dir, request, kotlin = false)
            ProjectTemplate.MATERIAL_YOU -> writeMaterialYouApp(dir, request)
            ProjectTemplate.COMPOSE -> writeComposeApp(dir, request)
            ProjectTemplate.ANDROID_LIBRARY -> writeAndroidLibrary(dir, request)
            ProjectTemplate.JAVA_LIBRARY -> writeJavaLibrary(dir, request)
            ProjectTemplate.KOTLIN_LIBRARY -> writeKotlinLibrary(dir, request)
            ProjectTemplate.NATIVE_CPP -> writeNativeCpp(dir)
            ProjectTemplate.NDK -> writeNdk(dir)
        }
        return Project(name = request.name, path = dir.absolutePath, type = request.template.projectType)
    }

    fun scanProjects(): List<Project> =
        projectsDir.listFiles()
            ?.filter { it.isDirectory && it.listFiles()?.isNotEmpty() == true }
            ?.mapNotNull { dir ->
                val marker = File(dir, markerFile)
                val template = if (marker.exists()) ProjectTemplate.fromKey(marker.readText()) else null
                Project(
                    name = dir.name,
                    path = dir.absolutePath,
                    type = template?.projectType ?: inferType(dir)
                )
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()

    private fun inferType(dir: File): ProjectType {
        if (File(dir, "CMakeLists.txt").exists()) {
            return if (File(dir, "main.cpp").exists()) ProjectType.NATIVE_CPP else ProjectType.NDK_SHARED_LIB
        }
        return ProjectType.GRADLE_KOTLIN
    }

    // ── helpers ────────────────────────────────────────────────────

    private fun File.file(relative: String): File =
        File(this, relative).apply { parentFile?.mkdirs() }

    private fun writeFile(dir: File, relative: String, content: String) {
        dir.file(relative).writeText(content)
    }

    private fun sanitizeDirName(name: String): String {
        val cleaned = name.trim().replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return cleaned.ifEmpty { "project" }
    }

    private fun rootName(name: String): String {
        val cleaned = sanitizeDirName(name).replace('.', '_').replace('-', '_')
        return if (cleaned.firstOrNull()?.isDigit() == true) "p$cleaned" else cleaned
    }

    private fun defaultPackageName(name: String): String {
        val base = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "").ifEmpty { "app" }
        val safe = if (base.first().isDigit()) "p$base" else base
        return "com.example.$safe"
    }

    private fun effectivePackage(request: CreateProjectRequest): String =
        request.packageName.ifBlank { defaultPackageName(request.name) }

    // ── shared root files ──────────────────────────────────────────

    private fun settingsGradle(rootName: String): String = """
        |pluginManagement {
        |    repositories {
        |        google()
        |        mavenCentral()
        |        gradlePluginPortal()
        |    }
        |}
        |dependencyResolutionManagement {
        |    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
        |    repositories {
        |        google()
        |        mavenCentral()
        |    }
        |}
        |rootProject.name = '$rootName'
        |include ':app'
        |""".trimMargin()

    private fun gradleProperties(): String = """
        |org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
        |android.useAndroidX=true
        |android.nonTransitiveRClass=true
        |android.aapt2FromMavenOverride=${aapt2Path}
        |""".trimMargin()

    private fun gitignore(): String = """
        |.gradle/
        |build/
        |local.properties
        |.idea/
        |*.iml
        |.DS_Store
        |/captures
        |.externalNativeBuild
        |.cxx
        |""".trimMargin()

    private fun writeGradleRoot(dir: File, request: CreateProjectRequest, plugins: String) {
        writeFile(dir, "settings.gradle", settingsGradle(rootName(request.name)))
        writeFile(dir, "build.gradle", plugins.trimIndent() + "\n")
        writeFile(dir, "gradle.properties", gradleProperties())
        writeFile(dir, ".gitignore", gitignore())
    }

    // ── Android apps ───────────────────────────────────────────────

    private fun writeAndroidApp(dir: File, request: CreateProjectRequest, kotlin: Boolean) {
        val pkg = effectivePackage(request)
        val rootPlugins = buildString {
            appendLine("plugins {")
            appendLine("    id 'com.android.application' version '$AGP' apply false")
            if (kotlin) appendLine("    id 'org.jetbrains.kotlin.android' version '$KOTLIN' apply false")
            appendLine("}")
        }
        writeGradleRoot(dir, request, rootPlugins)

        writeFile(dir, "app/build.gradle", appBuildGradle(pkg, kotlin))
        writeFile(dir, "app/proguard-rules.pro", "# Add project specific ProGuard rules here.\n")
        writeFile(dir, "app/src/main/AndroidManifest.xml", appManifest())
        writeFile(dir, "app/src/main/res/values/strings.xml", stringsXml(request.name))
        writeFile(dir, "app/src/main/res/values/themes.xml", themesApp())
        writeFile(dir, "app/src/main/res/layout/activity_main.xml", layoutMain())

        val srcDir = "app/src/main/java/${pkg.replace('.', '/')}"
        if (kotlin) writeFile(dir, "$srcDir/MainActivity.kt", mainActivityKt(pkg))
        else writeFile(dir, "$srcDir/MainActivity.java", mainActivityJava(pkg))
    }

    private fun writeMaterialYouApp(dir: File, request: CreateProjectRequest) {
        val pkg = effectivePackage(request)
        val rootPlugins = """
            |plugins {
            |    id 'com.android.application' version '$AGP' apply false
            |    id 'org.jetbrains.kotlin.android' version '$KOTLIN' apply false
            |}
            |""".trimMargin()
        writeGradleRoot(dir, request, rootPlugins)

        writeFile(dir, "app/build.gradle", appBuildGradle(pkg, kotlin = true))
        writeFile(dir, "app/proguard-rules.pro", "# Add project specific ProGuard rules here.\n")
        writeFile(dir, "app/src/main/AndroidManifest.xml", appManifest())
        writeFile(dir, "app/src/main/res/values/strings.xml", stringsXml(request.name))
        writeFile(dir, "app/src/main/res/values/themes.xml", themesMaterialYou())
        writeFile(dir, "app/src/main/res/layout/activity_main.xml", layoutMain())

        val srcDir = "app/src/main/java/${pkg.replace('.', '/')}"
        writeFile(dir, "$srcDir/MainActivity.kt", mainActivityKt(pkg))
    }

    private fun writeComposeApp(dir: File, request: CreateProjectRequest) {
        val pkg = effectivePackage(request)
        val rootPlugins = """
            |plugins {
            |    id 'com.android.application' version '$AGP' apply false
            |    id 'org.jetbrains.kotlin.android' version '$KOTLIN' apply false
            |}
            |""".trimMargin()
        writeGradleRoot(dir, request, rootPlugins)

        writeFile(dir, "app/build.gradle", composeBuildGradle(pkg))
        writeFile(dir, "app/proguard-rules.pro", "# Add project specific ProGuard rules here.\n")
        writeFile(dir, "app/src/main/AndroidManifest.xml", appManifest())
        writeFile(dir, "app/src/main/res/values/strings.xml", stringsXml(request.name))
        writeFile(dir, "app/src/main/res/values/themes.xml", themesCompose())

        val srcDir = "app/src/main/java/${pkg.replace('.', '/')}"
        writeFile(dir, "$srcDir/MainActivity.kt", composeMainActivityKt(pkg))
    }

    // ── libraries ──────────────────────────────────────────────────

    private fun writeAndroidLibrary(dir: File, request: CreateProjectRequest) {
        val pkg = effectivePackage(request)
        val rootPlugins = """
            |plugins {
            |    id 'com.android.library' version '$AGP' apply false
            |    id 'org.jetbrains.kotlin.android' version '$KOTLIN' apply false
            |}
            |""".trimMargin()
        writeGradleRoot(dir, request, rootPlugins)

        writeFile(dir, "app/build.gradle", androidLibraryGradle(pkg))
        writeFile(dir, "app/src/main/AndroidManifest.xml", libraryManifest())
        val srcDir = "app/src/main/java/${pkg.replace('.', '/')}"
        writeFile(dir, "$srcDir/MyLibrary.kt", androidLibrarySource(pkg))
    }

    private fun writeJavaLibrary(dir: File, request: CreateProjectRequest) {
        val pkg = effectivePackage(request)
        writeGradleRoot(dir, request, "// Top-level build file. The project is a single :app module.\n")

        writeFile(dir, "app/build.gradle", javaLibraryGradle())
        val srcDir = "app/src/main/java/${pkg.replace('.', '/')}"
        writeFile(dir, "$srcDir/Library.java", javaLibrarySource(pkg))
    }

    private fun writeKotlinLibrary(dir: File, request: CreateProjectRequest) {
        val pkg = effectivePackage(request)
        val rootPlugins = """
            |plugins {
            |    id 'org.jetbrains.kotlin.jvm' version '$KOTLIN' apply false
            |}
            |""".trimMargin()
        writeGradleRoot(dir, request, rootPlugins)

        writeFile(dir, "app/build.gradle", kotlinLibraryGradle())
        val srcDir = "app/src/main/kotlin/${pkg.replace('.', '/')}"
        writeFile(dir, "$srcDir/Hello.kt", kotlinLibrarySource(pkg))
    }

    // ── native (CMake) ─────────────────────────────────────────────

    private fun writeNativeCpp(dir: File) {
        writeFile(dir, "CMakeLists.txt", """
            |cmake_minimum_required(VERSION 3.22.1)
            |project(my_app CXX)
            |
            |set(CMAKE_CXX_STANDARD 17)
            |set(CMAKE_CXX_STANDARD_REQUIRED ON)
            |
            |add_executable(my_app main.cpp)
            |""".trimMargin())
        writeFile(dir, "main.cpp", """
            |#include <iostream>
            |
            |int main() {
            |    std::cout << "Hello from AuraStudio Native C++!" << std::endl;
            |    return 0;
            |}
            |""".trimMargin())
        writeNativeBuildSh(dir)
    }

    private fun writeNdk(dir: File) {
        writeFile(dir, "CMakeLists.txt", """
            |cmake_minimum_required(VERSION 3.22.1)
            |project(native_lib C)
            |
            |add_library(native_lib SHARED native.cpp)
            |""".trimMargin())
        writeFile(dir, "native.cpp", """
            |#include <iostream>
            |
            |extern "C" void hello_from_native() {
            |    std::cout << "Hello from AuraStudio NDK Shared Library!" << std::endl;
            |}
            |""".trimMargin())
        writeNativeBuildSh(dir)
    }

    private fun writeNativeBuildSh(dir: File) {
        val content = """
            |#!/usr/bin/env bash
            |set -e
            |
            |mkdir -p build
            |cd build
            |cmake ..
            |make -j{D}(nproc)
            |echo "Build complete."
            |""".trimMargin().replace("{D}", "$")
        val sh = dir.file("build.sh")
        sh.writeText(content)
        sh.setExecutable(true)
    }

    // ── Android app file contents ──────────────────────────────────

    private fun appBuildGradle(pkg: String, kotlin: Boolean): String {
        val pluginsBlock = if (kotlin) """
            |plugins {
            |    id 'com.android.application'
            |    id 'org.jetbrains.kotlin.android'
            |}
            |""".trimMargin() else """
            |plugins {
            |    id 'com.android.application'
            |}
            |""".trimMargin()
        val kotlinOptionsBlock = if (kotlin) """
            |
            |    kotlinOptions {
            |        jvmTarget = '17'
            |    }
            |""".trimMargin() else ""
        val depsBlock = if (kotlin) """
            |    implementation 'androidx.core:core-ktx:1.12.0'
            |    implementation 'androidx.appcompat:appcompat:1.6.1'
            |    implementation 'com.google.android.material:material:1.11.0'
            |""".trimMargin() else """
            |    implementation 'androidx.appcompat:appcompat:1.6.1'
            |    implementation 'com.google.android.material:material:1.11.0'
            |""".trimMargin()
        return """
            |$pluginsBlock
            |android {
            |    namespace '$pkg'
            |    compileSdk $COMPILE_SDK
            |
            |    defaultConfig {
            |        applicationId '$pkg'
            |        minSdk $MIN_SDK
            |        targetSdk $TARGET_SDK
            |        versionCode 1
            |        versionName "1.0"
            |    }
            |
            |    buildTypes {
            |        release {
            |            minifyEnabled false
            |            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            |        }
            |    }
            |
            |    compileOptions {
            |        sourceCompatibility JavaVersion.VERSION_17
            |        targetCompatibility JavaVersion.VERSION_17
            |    }
            |$kotlinOptionsBlock
            |}
            |
            |dependencies {
            |$depsBlock
            |}
            |""".trimMargin()
    }

    private fun composeBuildGradle(pkg: String): String = """
        |plugins {
        |    id 'com.android.application'
        |    id 'org.jetbrains.kotlin.android'
        |}
        |
        |android {
        |    namespace '$pkg'
        |    compileSdk $COMPILE_SDK
        |
        |    defaultConfig {
        |        applicationId '$pkg'
        |        minSdk $MIN_SDK
        |        targetSdk $TARGET_SDK
        |        versionCode 1
        |        versionName "1.0"
        |    }
        |
        |    buildTypes {
        |        release {
        |            minifyEnabled false
        |            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        |        }
        |    }
        |
        |    buildFeatures {
        |        compose = true
        |    }
        |
        |    composeOptions {
        |        kotlinCompilerExtensionVersion = '1.5.4'
        |    }
        |
        |    compileOptions {
        |        sourceCompatibility JavaVersion.VERSION_17
        |        targetCompatibility JavaVersion.VERSION_17
        |    }
        |
        |    kotlinOptions {
        |        jvmTarget = '17'
        |    }
        |}
        |
        |dependencies {
        |    implementation platform('androidx.compose:compose-bom:2024.02.00')
        |    implementation 'androidx.core:core-ktx:1.12.0'
        |    implementation 'androidx.activity:activity-compose:1.8.2'
        |    implementation 'androidx.compose.ui:ui'
        |    implementation 'androidx.compose.ui:ui-graphics'
        |    implementation 'androidx.compose.ui:ui-tooling-preview'
        |    implementation 'androidx.compose.material3:material3'
        |    debugImplementation 'androidx.compose.ui:ui-tooling'
        |}
        |""".trimMargin()

    private fun androidLibraryGradle(pkg: String): String = """
        |plugins {
        |    id 'com.android.library'
        |    id 'org.jetbrains.kotlin.android'
        |}
        |
        |android {
        |    namespace '$pkg'
        |    compileSdk $COMPILE_SDK
        |
        |    defaultConfig {
        |        minSdk $MIN_SDK
        |    }
        |
        |    compileOptions {
        |        sourceCompatibility JavaVersion.VERSION_17
        |        targetCompatibility JavaVersion.VERSION_17
        |    }
        |
        |    kotlinOptions {
        |        jvmTarget = '17'
        |    }
        |}
        |
        |dependencies {
        |    implementation 'androidx.core:core-ktx:1.12.0'
        |}
        |""".trimMargin()

    private fun javaLibraryGradle(): String = """
        |plugins {
        |    id 'java-library'
        |}
        |
        |java {
        |    sourceCompatibility = JavaVersion.VERSION_17
        |    targetCompatibility = JavaVersion.VERSION_17
        |}
        |""".trimMargin()

    private fun kotlinLibraryGradle(): String = """
        |plugins {
        |    id 'org.jetbrains.kotlin.jvm'
        |    id 'java-library'
        |}
        |
        |java {
        |    sourceCompatibility = JavaVersion.VERSION_17
        |    targetCompatibility = JavaVersion.VERSION_17
        |}
        |""".trimMargin()

    private fun appManifest(): String = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
        |    <application
        |        android:label="@string/app_name"
        |        android:supportsRtl="true"
        |        android:theme="@style/Theme.AuraStudioApp">
        |        <activity
        |            android:name=".MainActivity"
        |            android:exported="true">
        |            <intent-filter>
        |                <action android:name="android.intent.action.MAIN" />
        |                <category android:name="android.intent.category.LAUNCHER" />
        |            </intent-filter>
        |        </activity>
        |    </application>
        |</manifest>
        |""".trimMargin()

    private fun libraryManifest(): String = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<manifest xmlns:android="http://schemas.android.com/apk/res/android" />
        |""".trimMargin()

    private fun stringsXml(name: String): String = """
        |<resources>
        |    <string name="app_name">${name.trim()}</string>
        |</resources>
        |""".trimMargin()

    private fun themesApp(): String = """
        |<resources>
        |    <style name="Theme.AuraStudioApp" parent="Theme.Material3.DayNight.NoActionBar" />
        |</resources>
        |""".trimMargin()

    private fun themesMaterialYou(): String = """
        |<resources>
        |    <style name="Theme.AuraStudioApp" parent="Theme.Material3.DynamicColors.DayNight" />
        |</resources>
        |""".trimMargin()

    private fun themesCompose(): String = """
        |<resources>
        |    <style name="Theme.AuraStudioApp" parent="android:Theme.Material.Light.NoActionBar" />
        |</resources>
        |""".trimMargin()

    private fun layoutMain(): String = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
        |    android:layout_width="match_parent"
        |    android:layout_height="match_parent"
        |    android:gravity="center"
        |    android:orientation="vertical">
        |
        |    <TextView
        |        android:layout_width="wrap_content"
        |        android:layout_height="wrap_content"
        |        android:text="Hello from AuraStudio!"
        |        android:textSize="18sp"
        |        android:textStyle="bold" />
        |</LinearLayout>
        |""".trimMargin()

    private fun mainActivityKt(pkg: String): String = """
        |package $pkg
        |
        |import android.os.Bundle
        |import androidx.appcompat.app.AppCompatActivity
        |
        |class MainActivity : AppCompatActivity() {
        |    override fun onCreate(savedInstanceState: Bundle?) {
        |        super.onCreate(savedInstanceState)
        |        setContentView(R.layout.activity_main)
        |    }
        |}
        |""".trimMargin()

    private fun mainActivityJava(pkg: String): String = """
        |package $pkg;
        |
        |import android.os.Bundle;
        |import androidx.appcompat.app.AppCompatActivity;
        |
        |public class MainActivity extends AppCompatActivity {
        |    @Override
        |    protected void onCreate(Bundle savedInstanceState) {
        |        super.onCreate(savedInstanceState);
        |        setContentView(R.layout.activity_main);
        |    }
        |}
        |""".trimMargin()

    private fun composeMainActivityKt(pkg: String): String = """
        |package $pkg
        |
        |import android.os.Bundle
        |import androidx.activity.ComponentActivity
        |import androidx.activity.compose.setContent
        |import androidx.compose.foundation.layout.Arrangement
        |import androidx.compose.foundation.layout.Column
        |import androidx.compose.foundation.layout.fillMaxSize
        |import androidx.compose.foundation.layout.padding
        |import androidx.compose.material3.MaterialTheme
        |import androidx.compose.material3.Surface
        |import androidx.compose.material3.Text
        |import androidx.compose.runtime.Composable
        |import androidx.compose.ui.Alignment
        |import androidx.compose.ui.Modifier
        |import androidx.compose.ui.tooling.preview.Preview
        |import androidx.compose.ui.unit.dp
        |
        |class MainActivity : ComponentActivity() {
        |    override fun onCreate(savedInstanceState: Bundle?) {
        |        super.onCreate(savedInstanceState)
        |        setContent {
        |            MaterialTheme {
        |                Surface(modifier = Modifier.fillMaxSize()) {
        |                    Greeting()
        |                }
        |            }
        |        }
        |    }
        |}
        |
        |@Composable
        |fun Greeting() {
        |    Column(
        |        modifier = Modifier
        |            .fillMaxSize()
        |            .padding(24.dp),
        |        verticalArrangement = Arrangement.Center,
        |        horizontalAlignment = Alignment.CenterHorizontally
        |    ) {
        |        Text(
        |            text = "Hello from AuraStudio Compose!",
        |            style = MaterialTheme.typography.titleLarge
        |        )
        |    }
        |}
        |
        |@Preview(showBackground = true)
        |@Composable
        |private fun GreetingPreview() {
        |    MaterialTheme {
        |        Greeting()
        |    }
        |}
        |""".trimMargin()

    private fun androidLibrarySource(pkg: String): String = """
        |package $pkg
        |
        |class MyLibrary {
        |    fun greet(): String = "Hello from the Android library!"
        |}
        |""".trimMargin()

    private fun javaLibrarySource(pkg: String): String = """
        |package $pkg;
        |
        |public final class Library {
        |    public static String greet() {
        |        return "Hello from the Java library!";
        |    }
        |}
        |""".trimMargin()

    private fun kotlinLibrarySource(pkg: String): String = """
        |package $pkg
        |
        |fun greet(): String = "Hello from the Kotlin library!"
        |""".trimMargin()

    companion object {
        const val STORAGE_ACCESS_REQUIRED = "STORAGE_ACCESS_REQUIRED"

        private const val AGP = "8.2.2"
        private const val KOTLIN = "1.9.22"
        private const val COMPILE_SDK = 34
        private const val MIN_SDK = 24
        private const val TARGET_SDK = 34

        /** True when the app may write to the real Internal Storage.
         *  Android 11+ requires the "All files access" (MANAGE_EXTERNAL_STORAGE) grant. */
        fun externalStorageReady(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()
    }
}