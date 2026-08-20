<div align="center">

```text
 ╔══════════════════════════════════════════════════════════════════╗
 ║  AURA STUDIO CLI v1.0 - Next-Gen Termux Android Development Tool ║
 ╚══════════════════════════════════════════════════════════════════╝
```

# ⚡ AuraStudio CLI v1.0

**Turn your Android device into a full-fledged native Android development environment.**

[![Shell](https://img.shields.io/badge/Language-Bash-4EAA25.svg?style=for-the-badge&logo=gnu-bash&logoColor=white)](https://www.gnu.org/software/bash/)
[![Termux](https://img.shields.io/badge/Environment-Termux-24292E.svg?style=for-the-badge&logo=android&logoColor=3DDC84)](https://termux.dev)
[![Architecture](https://img.shields.io/badge/Arch-aarch64--linux--musl-9966FF.svg?style=for-the-badge)](https://github.com/HomuHomu833/android-ndk-custom)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)

[Installation](#-installation) • [Features](#-features) • [Commands](#-command-reference) • [Building .DEB](#-building-deb-package) • [Troubleshooting](#-troubleshooting)

</div>

---

## 📖 Overview

**AuraStudio CLI** is an automated, modular, and modern command-line tool suite built specifically for **Termux on Android**. It automates the installation, configuration, and management of complete Android development toolchains—including **OpenJDK 21, Gradle, Android SDK, custom NDK (aarch64-linux-musl), CMake, and starter templates**—directly on your device without requiring a PC.

Designed with truecolor RGB palettes, smooth background job spinners, and resilient network error handling, AuraStudio CLI delivers a desktop-class CLI experience right inside Termux.

---

## ✨ Features

- 🚀 **Automated Environment Bootstrap**: Configures OpenJDK 21, Gradle, `cmdline-tools`, and accepts Android SDK licenses in one command.
- 📦 **Custom NDK & CMake Manager**: Integrates pre-built `aarch64-linux-musl` toolchains (NDK r26d through r30 beta2) to bypass glibc compatibility issues in Termux.
- ⚡ **Multi-Threaded Download Acceleration**: Automatically utilizes `aria2c` (16 connections) with seamless fallback to `curl` with resume capability (`-C -`).
- 📁 **Modular System Architecture**: Code base cleanly divided into core configurations, UI libraries, system helpers, and modular command handlers.
- 🔒 **Isolated Shell Environment**: Keeps main shell configurations clean by managing environment variables inside a dedicated `~/.aurastudiorc` file.
- 🛠️ **Project Starter Initializer (`aurastudio init`)**: Instantly scaffolds Native C++ CMake console apps, NDK shared libraries (`libnative.so`), and full Android Gradle projects (Java/Kotlin) with automated `./gradlew` generation.
- 📦 **Native Debian (.deb) & CI/CD Support**: Easily packaged into `.deb` installers with automated release builds via GitHub Actions workflow.
- 🛡️ **Storage Protection & Diagnostics**: Pre-checks available disk space before heavy downloads and includes built-in troubleshooting diagnostics (`aurastudio doctor`).
- 🔄 **Auto-Updater & Package Manager**: Update the CLI directly from GitHub or remove specific SDK/NDK packages using interactive menus.

---

## ⚡ Installation

Choose one of the following methods to install AuraStudio CLI in Termux:

### Option 1: One-Liner Web Installer (Recommended)
Paste and run this single-line command inside your Termux terminal:

```bash
curl -sSL https://raw.githubusercontent.com/Arata-Labs/AuraStudio/main/install.sh | bash
```

### Option 2: Pre-built `.deb` Package (GitHub Releases)
1. Download the latest `.deb` package from [GitHub Releases](https://github.com/Arata-Labs/AuraStudio/releases/latest).
2. Install it using Termux package manager:
   ```bash
   pkg install ./aurastudio_1.0_all.deb
   ```

---

### Post-Installation Setup
After installation completes, run the environment setup wizard:

```bash
aurastudio setup
```

Then reload your shell configuration:

```bash
source ~/.aurastudiorc
```

---

## 🚀 Command Reference

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `aurastudio setup` | - | Runs full automated environment setup (Java 21, Gradle, SDK Manager, API 37/Custom). |
| `aurastudio install sdk` | `[platform <API>] [buildtools <ver>]` | Interactive or direct CLI installation for Android Platforms & Build-Tools. |
| `aurastudio install ndk` | - | Interactive selector to install custom Native NDK versions (r26d - r30). |
| `aurastudio install cmake` | - | Interactive selector to install native CMake binaries (3.10.2 - 4.1.2). |
| `aurastudio init` | - | Scaffolds C++ CMake, NDK Shared Lib, or Gradle Android App (Java/Kotlin) starters. |
| `aurastudio remove` | `[ndk\|cmake\|sdk] [name/type] [ver]` | Uninstalls specific SDK/NDK components via direct flags or interactive menu. |
| `aurastudio clean` | - | Cleans up temporary downloaded zips and extraction caches in `$TMPDIR`. |
| `aurastudio update` | - | Fetches and applies the latest updates directly from GitHub or Release packages. |
| `aurastudio check-update` | - | Checks if a newer version of AuraStudio CLI is available on GitHub. |
| `aurastudio status` | - | Displays a visual dashboard summary of all installed system components. |
| `aurastudio doctor` | - | Runs diagnostic checks on your environment and offers fix instructions. |
| `aurastudio uninstall` | - | Completely removes AuraStudio CLI and provides an option to wipe `~/android-sdk`. |

> **Tip:** Append `--verbose` or `-v` to any command to enable debug logging without background spinner animations.

---

## 🏗️ Project Structure

AuraStudio CLI follows a modular architecture for clean maintainability:

```text
aurastudio/
├── .github/workflows/
│   └── release.yml         # GitHub Actions automated DEB package release workflow
├── aurastudio              # Main CLI entry point & command router
├── install.sh              # Official web installer script
├── build_deb.sh            # Automated Debian package (.deb) builder script
├── config/
│   └── env.sh              # Environment paths, color palette & package metadata
├── lib/
│   ├── ui.sh               # UI engine, RGB banners, loggers & spinner animations
│   ├── utils.sh            # Storage check, aria2c/curl downloader & env writer
│   └── tools.sh            # Package dependency manager & Java validators
└── modules/
    ├── setup.sh            # Full setup orchestrator module
    ├── sdk.sh              # Android SDK & Build-Tools management module
    ├── ndk.sh              # Custom Native NDK toolchain installer module
    ├── cmake.sh            # Native CMake SDK installer module
    ├── remove.sh           # Package uninstaller module
    ├── clean.sh            # Cache & temp cleanup module
    ├── init.sh             # Boilerplate project scaffold generator
    ├── update.sh           # GitHub auto-updater module
    ├── uninstall.sh        # System uninstaller module
    ├── status.sh           # Environment status dashboard module
    └── doctor.sh           # Diagnostic & system health module
```

---

## 🛠️ Building `.deb` Package

You can easily build a standalone Debian package (`.deb`) locally or let GitHub Actions build it automatically when pushing release tags.

### Building Locally:
```bash
chmod +x build_deb.sh
./build_deb.sh
```
The output file and its SHA256 checksum will be generated inside the `dist/` directory:
- `dist/aurastudio_1.0_all.deb`
- `dist/aurastudio_1.0_all.deb.sha256`

---

## ⚙️ Environment Configuration

During setup, AuraStudio generates an isolated configuration file at `~/.aurastudiorc` and injects a single load line into your shell profile (`~/.zshrc` or `$PREFIX/etc/bash.bashrc`):

```bash
[ -f "$HOME/.aurastudiorc" ] && source "$HOME/.aurastudiorc"
```

### Managed Environment Variables (`~/.aurastudiorc`):
- `ANDROID_HOME` & `ANDROID_SDK_ROOT` -> Points to `$HOME/android-sdk`
- `JAVA_HOME` -> Dynamically detects and points to OpenJDK 21
- `PATH` -> Automatically includes `cmdline-tools`, `platform-tools`, `JAVA_HOME/bin`, and active `build-tools`

---

## 🩺 Diagnostics & Troubleshooting

If you encounter issues during builds or component execution, run the built-in diagnostic tool:

```bash
aurastudio doctor
```

### Common Fixes:
1. **Command Not Found Errors:**
   Ensure you have sourced the environment after setup:
   ```bash
   source ~/.aurastudiorc
   ```
2. **Out of Memory during Gradle Builds:**
   Adjust JVM heap settings inside your project's `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
   ```
3. **Download Interruptions:**
   Install `aria2` to enable resilient 16-connection parallel downloads:
   ```bash
   pkg install aria2
   ```

---

## 🤝 Contributing

Contributions, bug reports, and feature requests are welcome!
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
