package com.aurastudio.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class InstallEvent(
    val isFinished: Boolean = false,
    val isSuccess: Boolean = true,
    val line: String = "",
    val error: String = ""
)

/**
 * Runs install/uninstall commands inside the app's embedded Termux prefix.
 * Everything is handled standalone (no aurastudio CLI dependency):
 * - [java]/[aapt2]/[gradle]        → apt from the hosted repo
 * - [cmdline_tools]                → direct Google commandlinetools download
 * - [platforms]/[build_tools]      → sdkmanager from cmdline-tools
 * - [ndk]/[cmake]                  → direct GitHub archive download/extract
 * - java version switch            → re-points $PREFIX/bin cmd symlinks
 */
class PackageInstaller(private val context: Context) {

    private val filesDir: String get() = context.filesDir.absolutePath
    private val prefix: String get() = "$filesDir/usr"
    private val home: String get() = "$filesDir/home"

    // Downloads mirrored from the CLI's config/env.sh (must stay in sync).
    private val cmdlineToolsUrl =
        "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
    private val ndkBaseUrl = "https://github.com/HomuHomu833/android-ndk-custom/releases/download"
    private val cmakeHomuBase = "https://github.com/HomuHomu833/cmake-custom/releases/download"
    private val cmakeIksoBase = "https://github.com/MrIkso/AndroidIDE-NDK/releases/download/cmake"

    private fun buildCommand(command: String): Process {
        val bash = "$prefix/bin/bash"
        val pb = ProcessBuilder(bash, "-lc", command)
        pb.redirectErrorStream(true)
        val env = pb.environment()
        env["PREFIX"] = prefix
        env["TERMUX_PREFIX"] = prefix
        env["HOME"] = home
        env["PATH"] = "$prefix/bin:$prefix/bin/applets"
        env["LD_LIBRARY_PATH"] = "$prefix/lib"
        env["TMPDIR"] = "$prefix/tmp"
        env["TERMUX_APP_PACKAGE"] = "com.aurastudio"
        env["TERMUX_PREFIX"] = prefix
        val androidSdk = "$home/android-sdk"
        env["ANDROID_HOME"] = androidSdk
        env["ANDROID_SDK_ROOT"] = androidSdk
        val javaLink = File("$prefix/bin/java")
        if (javaLink.exists()) {
            val jvmHome = File(javaLink.canonicalPath).parentFile?.parentFile?.absolutePath
            if (jvmHome != null && File("$jvmHome/bin/java").exists()) {
                env["JAVA_HOME"] = jvmHome
            }
        }
        return pb.start()
    }

    /**
     * Run [command] and stream its stdout/stderr lines via the returned flow.
     * The flow emits [InstallEvent] per line, then a final event with
     * [InstallEvent.isFinished] = true and [InstallEvent.isSuccess].
     * Terminates process on flow collector cancellation.
     */
    fun run(command: String): Flow<InstallEvent> = flow {
        if (!File("$prefix/bin/bash").exists()) {
            emit(InstallEvent(line = "Bootstrap not installed yet."))
            emit(InstallEvent(isFinished = true, isSuccess = false, error = "Bootstrap not installed"))
            return@flow
        }

        val process = buildCommand(command)
        try {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.useLines { lines ->
                for (line in lines) {
                    emit(InstallEvent(line = line))
                }
            }
            val exitCode = process.waitFor()
            emit(InstallEvent(isFinished = true, isSuccess = exitCode == 0, error = if (exitCode == 0) "" else "Command exited with code $exitCode"))
        } catch (e: Exception) {
            emit(InstallEvent(isFinished = true, isSuccess = false, error = e.message ?: "Install failed"))
        } finally {
            process.destroy()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Convert a raw shell script (where `{DOLLAR}` is a literal `$`) into a
     * real command string. Kotlin raw strings treat `$` as template syntax, so
     * every shell variable is written as `{DOLLAR}VAR` and normalized here.
     */
    private fun sh(script: String): String = script.replace("{DOLLAR}", "$")

    /** Build an apt install command, refreshing package lists first if the
     *  local apt cache under $PREFIX/var/lib/apt/lists is empty. */
    private fun apt(packageName: String, forceOverwrite: Boolean = false): String =
        "ls {DOLLAR}PREFIX/var/lib/apt/lists/*_Packages >/dev/null 2>&1 || apt update 2>&1; " +
            "apt install -y ${if (forceOverwrite) "-o Dpkg::Options::=--force-overwrite " else ""}$packageName 2>&1 || exit 1"
        .replace("{DOLLAR}", "$")

    /**
     * Map a component key + version to the standalone shell command that
     * installs it. No aurastudio CLI calls — every command runs directly
     * inside the app's embedded prefix.
     */
    fun installCommand(componentKey: String, version: String): String = when (componentKey) {
        "java" -> apt(if (version.contains("21")) "openjdk-21" else "openjdk-17", forceOverwrite = true) +
            "\n" + ensureEnvironmentCommand()
        "gradle" -> apt("gradle")
        "aapt2" -> apt("aapt2")

        "cmdline_tools" -> installCmdlineToolsCommand()

        "platforms" -> sdkmanagerInstallCommand("platforms;android-$version")
        "build_tools" -> sdkmanagerInstallCommand("build-tools;$version")

        "ndk" -> installNdkCommand(version)
        "cmake" -> installCmakeCommand(version)

        else -> "echo \"Unknown component: $componentKey\""
    }

    /** Map a component key + version to the shell command that uninstalls it. */
    fun uninstallCommand(componentKey: String, version: String): String = when (componentKey) {
        "java" -> "ls {DOLLAR}PREFIX/var/lib/apt/lists/*_Packages >/dev/null 2>&1 || apt update 2>&1; apt remove -y openjdk-21 openjdk-17 2>&1".replace("{DOLLAR}", "$")
        "gradle" -> "ls {DOLLAR}PREFIX/var/lib/apt/lists/*_Packages >/dev/null 2>&1 || apt update 2>&1; apt remove -y gradle 2>&1".replace("{DOLLAR}", "$")
        "aapt2" -> "ls {DOLLAR}PREFIX/var/lib/apt/lists/*_Packages >/dev/null 2>&1 || apt update 2>&1; apt remove -y aapt2 2>&1".replace("{DOLLAR}", "$")
        "cmdline_tools" -> "rm -rf $home/android-sdk/cmdline-tools 2>&1"
        "platforms" -> "rm -rf $home/android-sdk/platforms/android-$version 2>&1"
        "build_tools" -> "rm -rf $home/android-sdk/build-tools/$version 2>&1"
        "ndk" -> "rm -rf $home/android-sdk/ndk/$version 2>&1"
        "cmake" -> "rm -rf $home/android-sdk/cmake/$version 2>&1"
        else -> "echo \"Unknown component: $componentKey\""
    }

    fun switchJavaCommand(version: String): String = when (version) {
        "21", "17" -> switchJavaScript(version)
        else -> "echo \"Invalid Java version: $version\""
    }

    // ── cmdline-tools ──────────────────────────────────────────────
    private fun installCmdlineToolsCommand(): String =
        sh(
            """
            SDK_DIR="{DOLLAR}HOME/android-sdk"
            TMP_ZIP="{DOLLAR}TMPDIR/aurastudio_cmdtools.zip"
            TMP_DIR="{DOLLAR}TMPDIR/aurastudio_cmdtools_extract"
            curl -L --retry 3 --fail "$cmdlineToolsUrl" -o "{DOLLAR}TMP_ZIP" 2>&1 || exit 1
            rm -rf "{DOLLAR}TMP_DIR"
            unzip -q "{DOLLAR}TMP_ZIP" -d "{DOLLAR}TMP_DIR" || exit 1
            mkdir -p "{DOLLAR}SDK_DIR/cmdline-tools/latest"
            if [ -d "{DOLLAR}TMP_DIR/cmdline-tools" ]; then
                cp -r "{DOLLAR}TMP_DIR/cmdline-tools/." "{DOLLAR}SDK_DIR/cmdline-tools/latest/" || exit 1
            else
                cp -r "{DOLLAR}TMP_DIR/." "{DOLLAR}SDK_DIR/cmdline-tools/latest/" || exit 1
            fi
            rm -rf "{DOLLAR}TMP_ZIP" "{DOLLAR}TMP_DIR"
            chmod -R 755 "{DOLLAR}SDK_DIR/cmdline-tools/latest/bin/" 2>/dev/null
            # Patch /usr/bin/env shebangs to the embedded prefix env
            TERMUX_ENV="{DOLLAR}(command -v env)"
            for bin_file in "{DOLLAR}SDK_DIR/cmdline-tools/latest/bin/"*; do
                [ -f "{DOLLAR}bin_file" ] || continue
                SHEBANG="{DOLLAR}(head -1 "{DOLLAR}bin_file" 2>/dev/null)"
                if [[ "{DOLLAR}SHEBANG" == "#!/usr/bin/env"* ]]; then
                    REST="{DOLLAR}{SHEBANG#'#!/usr/bin/env'}"
                    sed -i "1s|.*|#!{DOLLAR}TERMUX_ENV{DOLLAR}REST|" "{DOLLAR}bin_file"
                fi
            done
            """.trimIndent()
        ) + "\n" + ensureEnvironmentCommand()

    // ── platforms / build-tools (sdkmanager) ───────────────────────
    private fun sdkmanagerInstallCommand(pkg: String): String = sh(
        """
        export ANDROID_HOME="{DOLLAR}HOME/android-sdk"
        export ANDROID_SDK_ROOT="{DOLLAR}HOME/android-sdk"
        JAVA_HOME="{DOLLAR}(dirname "{DOLLAR}(dirname "{DOLLAR}(readlink -f "{DOLLAR}PREFIX/bin/java")")")"
        [ -n "{DOLLAR}JAVA_HOME" ] && [ -x "{DOLLAR}JAVA_HOME/bin/java" ] || { echo "Java not installed"; exit 1; }
        export JAVA_HOME
        export PATH="{DOLLAR}JAVA_HOME/bin:{DOLLAR}HOME/android-sdk/cmdline-tools/latest/bin:{DOLLAR}HOME/android-sdk/platform-tools:{DOLLAR}PATH"
        SDKMANAGER="{DOLLAR}HOME/android-sdk/cmdline-tools/latest/bin/sdkmanager"
        [ -x "{DOLLAR}SDKMANAGER" ] || { echo "cmdline-tools not installed"; exit 1; }
        yes 2>/dev/null | "{DOLLAR}SDKMANAGER" --sdk_root="{DOLLAR}HOME/android-sdk" "$pkg" 2>&1 || exit 1
        """.trimIndent()
    )

    // ── NDK (HomuHomu833 archives) ─────────────────────────────────
    private fun installNdkCommand(version: String): String {
        val url = ndkUrl(version)
        return if (url == null) {
            "echo \"Unknown NDK version: $version\""
        } else sh(
            """
            SDK_DIR="{DOLLAR}HOME/android-sdk"
            NDK_DIR="{DOLLAR}SDK_DIR/ndk"
            NDK_VER="$version"
            FILE="{DOLLAR}(basename "$url")"
            TARGET="{DOLLAR}TMPDIR/{DOLLAR}FILE"
            curl -L --retry 3 --fail "$url" -o "{DOLLAR}TARGET" 2>&1 || exit 1
            (cd "{DOLLAR}HOME" && tar --no-same-owner -xf "{DOLLAR}TARGET" --warning=no-unknown-keyword 2>/dev/null) || { rm -f "{DOLLAR}TARGET"; exit 1; }
            rm -f "{DOLLAR}TARGET"
            mkdir -p "{DOLLAR}NDK_DIR"
            EXTRACTED="{DOLLAR}(find "{DOLLAR}HOME" -maxdepth 1 -type d \( -name "android-ndk-*" -o -name "{DOLLAR}NDK_VER" \) 2>/dev/null | head -1)"
            [ -n "{DOLLAR}EXTRACTED" ] || { echo "Extracted NDK dir not found"; exit 1; }
            mv "{DOLLAR}EXTRACTED" "{DOLLAR}NDK_DIR/{DOLLAR}NDK_VER" || exit 1
            # musl symlinks (linux-aarch64 -> linux-arm64) for clang toolchain
            for p in toolchains/llvm/prebuilt prebuilt shader-tools; do
                if [ -d "{DOLLAR}NDK_DIR/{DOLLAR}NDK_VER/{DOLLAR}p" ]; then
                    ( cd "{DOLLAR}NDK_DIR/{DOLLAR}NDK_VER/{DOLLAR}p" && [ ! -e linux-aarch64 ] && ln -s linux-arm64 linux-aarch64 ) 2>/dev/null
                fi
            done
            # Gradle expects NDK dir named by real Pkg.Revision — symlink it
            SP="{DOLLAR}NDK_DIR/{DOLLAR}NDK_VER/source.properties"
            if [ -f "{DOLLAR}SP" ]; then
                RV="{DOLLAR}(grep "^Pkg.Revision" "{DOLLAR}SP" | cut -d'=' -f2 | tr -d ' ')"
                if [ -n "{DOLLAR}RV" ] && [ "{DOLLAR}RV" != "{DOLLAR}NDK_VER" ] && [ ! -e "{DOLLAR}NDK_DIR/{DOLLAR}RV" ]; then
                    ln -s "{DOLLAR}NDK_DIR/{DOLLAR}NDK_VER" "{DOLLAR}NDK_DIR/{DOLLAR}RV" 2>/dev/null
                fi
            fi
            """.trimIndent()
        )
    }

    private fun ndkUrl(version: String): String? = when (version) {
        "r30-beta2" -> "$ndkBaseUrl/r30/android-ndk-r30-beta2-aarch64-linux-musl.tar.xz"
        "r29" -> "$ndkBaseUrl/r29/android-ndk-r29-aarch64-linux-musl.tar.xz"
        "r28c" -> "$ndkBaseUrl/r28/android-ndk-r28c-aarch64-linux-musl.tar.xz"
        "r27d" -> "$ndkBaseUrl/r27/android-ndk-r27d-aarch64-linux-musl.tar.xz"
        "r26d" -> "$ndkBaseUrl/r26/android-ndk-r26d-aarch64-linux-musl.tar.xz"
        else -> null
    }

    // ── CMake (HomuHomu833 tar.xz / MrIkso zips) ───────────────────
    private fun installCmakeCommand(version: String): String {
        val url = cmakeUrl(version)
        return if (url == null) {
            "echo \"Unknown CMake version: $version\""
        } else sh(
            """
            CMAKE_DIR="{DOLLAR}HOME/android-sdk/cmake"
            CMAKE_VER="$version"
            FILE="{DOLLAR}(basename "$url")"
            TARGET="{DOLLAR}TMPDIR/{DOLLAR}FILE"
            mkdir -p "{DOLLAR}CMAKE_DIR" "{DOLLAR}CMAKE_DIR/{DOLLAR}CMAKE_VER"
            curl -L --retry 3 --fail "$url" -o "{DOLLAR}TARGET" 2>&1 || exit 1
            [ -s "{DOLLAR}TARGET" ] || { rm -f "{DOLLAR}TARGET"; echo "Download failed"; exit 1; }
            if [[ "{DOLLAR}FILE" == *.tar.xz ]]; then
                tar -xf "{DOLLAR}TARGET" -C "{DOLLAR}CMAKE_DIR/{DOLLAR}CMAKE_VER" --strip-components=1 2>/dev/null || tar -xf "{DOLLAR}TARGET" -C "{DOLLAR}CMAKE_DIR/{DOLLAR}CMAKE_VER"
            elif [[ "{DOLLAR}FILE" == *.zip ]]; then
                unzip -qq "{DOLLAR}TARGET" -d "{DOLLAR}CMAKE_DIR"
            fi
            rm -f "{DOLLAR}TARGET"
            chmod -R +x "{DOLLAR}CMAKE_DIR/{DOLLAR}CMAKE_VER/bin" 2>/dev/null
            """.trimIndent()
        )
    }

    private fun cmakeUrl(version: String): String? = when (version) {
        "4.1.2", "4.1.1", "4.1.0", "4.0.3", "4.0.2" ->
            "$cmakeHomuBase/$version/cmake-aarch64-linux-musl.tar.xz"
        "3.25.1", "3.22.1", "3.18.1", "3.10.2" ->
            "$cmakeIksoBase/cmake-$version-android-aarch64.zip"
        else -> null
    }

    // ── Java version switch (re-points $PREFIX/bin/*java* symlinks) ─
    private fun switchJavaScript(version: String): String =
        sh(
            """
            TARGET_JVM="{DOLLAR}PREFIX/lib/jvm/java-$version-openjdk"
            [ -x "{DOLLAR}TARGET_JVM/bin/java" ] || { echo "openjdk-$version is not installed"; exit 1; }
            for cmd in java javac javadoc jar keytool jshell javap jdb jdeps jlink; do
                [ -f "{DOLLAR}TARGET_JVM/bin/{DOLLAR}cmd" ] || continue
                TARGET="{DOLLAR}PREFIX/bin/{DOLLAR}cmd"
                if [ -L "{DOLLAR}TARGET" ]; then
                    rm -f "{DOLLAR}TARGET"
                elif [ -e "{DOLLAR}TARGET" ]; then
                    mv "{DOLLAR}TARGET" "{DOLLAR}TARGET.bak-{DOLLAR}(date +%s)" 2>/dev/null
                fi
                ln -s "{DOLLAR}TARGET_JVM/bin/{DOLLAR}cmd" "{DOLLAR}TARGET"
                chmod +x "{DOLLAR}TARGET"
            done
            """.trimIndent()
        ) + "\n" + ensureEnvironmentCommand()

    /**
     * Generate the app's own `~/.config/aurastudio/env.sh` (ANDROID_HOME,
     * JAVA_HOME, PATH) and make it load from `$PREFIX/etc/bash.bashrc`, so the
     * environment no longer depends on files left behind by the aurastudio CLI.
     * Uses a quoted heredoc so `$VAR` refs stay literal inside the file and
     * expand when a shell sources it later.
     */
    fun ensureEnvironmentCommand(): String = sh(
        """
        ENV_DIR="{DOLLAR}HOME/.config/aurastudio"
        ENV_FILE="{DOLLAR}ENV_DIR/env.sh"
        mkdir -p "{DOLLAR}ENV_DIR"
        cat > "{DOLLAR}ENV_FILE" << 'AS_ENV'
        export ANDROID_HOME="{DOLLAR}HOME/android-sdk"
        export ANDROID_SDK_ROOT="{DOLLAR}ANDROID_HOME"
        export PATH="{DOLLAR}ANDROID_HOME/cmdline-tools/latest/bin:{DOLLAR}ANDROID_HOME/platform-tools:{DOLLAR}PATH"

        if command -v java &>/dev/null; then
            _java_real="{DOLLAR}(readlink -f "{DOLLAR}(command -v java)" 2>/dev/null || command -v java)"
            export JAVA_HOME="{DOLLAR}(dirname "{DOLLAR}(dirname "{DOLLAR}_java_real")")"
            unset _java_real
            export PATH="{DOLLAR}JAVA_HOME/bin:{DOLLAR}PATH"
        fi

        for _bt_dir in "{DOLLAR}ANDROID_HOME/build-tools"/*/; do
            [ -d "{DOLLAR}_bt_dir" ] && export PATH="{DOLLAR}{_bt_dir}bin:{DOLLAR}PATH"
        done
        unset _bt_dir
        AS_ENV
        RC_FILE="{DOLLAR}PREFIX/etc/bash.bashrc"
        if [ -f "{DOLLAR}RC_FILE" ]; then
            grep -qF "aurastudio/env.sh" "{DOLLAR}RC_FILE" 2>/dev/null || echo "source {DOLLAR}ENV_FILE" >> "{DOLLAR}RC_FILE"
        fi
        """.trimIndent()
    )
}