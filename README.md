<div align="center">

```text
  ╭──────────────────────────────────────────────────────────────╮
  │                                                              │
  │    ⚡  AuraStudio CLI  v1.1                                  │
  │        Build Android, Anywhere                               │
  │                                                              │
  ╰──────────────────────────────────────────────────────────────╯
```

# ⚡ AuraStudio CLI v1.1

**Turn your Android device into a full-fledged native Android development environment.**

[![Shell](https://img.shields.io/badge/Language-Bash-4EAA25.svg?style=for-the-badge&logo=gnu-bash&logoColor=white)](https://www.gnu.org/software/bash/)
[![Termux](https://img.shields.io/badge/Environment-Termux-24292E.svg?style=for-the-badge&logo=android&logoColor=3DDC84)](https://termux.dev)
[![Architecture](https://img.shields.io/badge/Arch-aarch64--linux--musl-9966FF.svg?style=for-the-badge)](https://github.com/HomuHomu833/android-ndk-custom)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE)
[![CI](https://img.shields.io/github/actions/workflow/status/Arata-Labs/AuraStudio/lint.yml?style=for-the-badge&label=CI)](https://github.com/Arata-Labs/AuraStudio/actions)

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
- 🔒 **XDG-Compliant Configuration**: Follows XDG Base Directory specification with configs in `~/.config/aurastudio/`, data in `~/.local/share/aurastudio/`, and cache in `~/.cache/aurastudio/`.
- 🛠️ **Project Starter Initializer (`aurastudio init`)**: Instantly scaffolds Native C++ CMake console apps, NDK shared libraries (`libnative.so`), and full Android Gradle projects (Java/Kotlin) with automated `./gradlew` generation.
- 📦 **Native Debian (.deb) & CI/CD Support**: Easily packaged into `.deb` installers with automated release builds via GitHub Actions workflow (ShellCheck lint + unit tests + changelog generation).
- 🛡️ **Storage Protection & Diagnostics**: Pre-checks available disk space before heavy downloads and includes built-in troubleshooting diagnostics (`aurastudio doctor`).
- 🔄 **Auto-Updater & Package Manager**: Update the CLI directly from GitHub or remove specific SDK/NDK packages using interactive menus.
- 🩺 **Health Score & Snapshots**: `aurastudio status` shows a health score (0-100), and `aurastudio doctor --snapshot` saves environment snapshots for debugging.
- 🔌 **Plugin System**: Load custom plugins from `~/.config/aurastudio/plugins/` to extend CLI functionality.
- ⌨️ **Bash Autocompletion**: Tab completion for all commands and subcommands, auto-configured during installation.
- 🧪 **Unit Testing**: Built-in test framework with `tests/run_tests.sh` for validating core utilities.

---

## ⚡ Installation

Choose one of the following methods to install AuraStudio CLI in Termux:

### Option 1: One-Liner Web Installer (Recommended)
Paste and run this single-line command inside your Termux terminal:

```bash
curl -sSL https://raw.githubusercontent.com/Arata-Labs/AuraStudio/main/install.sh | bash
```

The installer will:
1. Install required dependencies (`git`, `curl`, `tar`, `unzip`)
2. Clone the repository to `~/.aurastudio/`
3. Create a symlink in `$PREFIX/bin/aurastudio`
4. Set up bash autocompletion

### Option 2: Pre-built `.deb` Package (GitHub Releases)
1. Download the latest `.deb` package from [GitHub Releases](https://github.com/Arata-Labs/AuraStudio/releases/latest).
2. Install it using Termux package manager:
   ```bash
   pkg install ./aurastudio_1.1_all.deb
   ```

---

### Post-Installation Setup
After installation completes, run the environment setup wizard:

```bash
aurastudio setup
```

Then reload your shell configuration:

```bash
source ~/.config/aurastudio/env.sh
```

---

## 🚀 Command Reference

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `aurastudio setup` | - | Full automated environment setup (Java 21, Gradle, SDK Manager, API 37/Custom). |
| `aurastudio install sdk` | `[platform <API>] [buildtools <ver>]` | Interactive or direct CLI installation for Android Platforms & Build-Tools. |
| `aurastudio install ndk` | - | Interactive selector to install custom Native NDK versions (r26d - r30). |
| `aurastudio install cmake` | - | Interactive selector to install native CMake binaries (3.10.2 - 4.1.2). |
| `aurastudio init` | - | Scaffolds C++ CMake, NDK Shared Lib, or Gradle Android App (Java/Kotlin) starters. |
| `aurastudio remove` | `[ndk\|cmake\|sdk] [name/type] [ver]` | Uninstalls specific SDK/NDK components via direct flags or interactive menu. |
| `aurastudio clean` | - | Cleans up temporary downloaded zips and extraction caches in `$TMPDIR`. |
| `aurastudio update` | - | Fetches and applies the latest updates directly from GitHub or Release packages. |
| `aurastudio check-update` | - | Checks if a newer version of AuraStudio CLI is available on GitHub. |
| `aurastudio status` | `[--json]` | Visual dashboard of installed components with health score (0-100). |
| `aurastudio doctor` | `[--fix] [--snapshot]` | Diagnostic checks with optional auto-fix and environment snapshot. |
| `aurastudio uninstall` | - | Completely removes AuraStudio CLI and provides an option to wipe `~/android-sdk`. |
| `aurastudio man` | - | Shows the full manual page. |
| `aurastudio version` | - | Displays the current CLI version. |
| `aurastudio help` | - | Shows the help message with all available commands. |

> **Tip:** Append `--verbose` or `-v` to any command to enable debug logging without background spinner animations.

---

## 🏗️ Project Structure

AuraStudio CLI follows a modular architecture for clean maintainability:

```text
aurastudio/
├── .github/
│   ├── workflows/
│   │   ├── lint.yml              # ShellCheck CI + unit tests
│   │   └── release.yml           # Automated DEB build + changelog + GitHub Release
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md         # Bug report template
│   │   └── feature_request.md    # Feature request template
│   ├── pull_request_template.md  # PR template
│   └── RELEASE_TEMPLATE.md       # Release notes template
├── aurastudio                    # Main CLI entry point & command router
├── install.sh                    # Official web installer script
├── build_deb.sh                  # Automated Debian package (.deb) builder
├── config/
│   └── env.sh                    # Environment paths, color palette & package metadata
├── lib/
│   ├── ui.sh                     # UI engine, RGB banners, loggers & spinner animations
│   ├── utils.sh                  # Storage check, downloader, env writer, utilities
│   ├── tools.sh                  # Package dependency manager & Java validators
│   └── aurastudio-completion.bash # Bash autocompletion script
├── modules/
│   ├── setup.sh                  # Full setup orchestrator module
│   ├── sdk.sh                    # Android SDK & Build-Tools management
│   ├── ndk.sh                    # Custom Native NDK toolchain installer
│   ├── cmake.sh                  # Native CMake SDK installer
│   ├── remove.sh                 # Package uninstaller
│   ├── clean.sh                  # Cache & temp cleanup
│   ├── init.sh                   # Boilerplate project scaffold generator
│   ├── update.sh                 # GitHub auto-updater
│   ├── uninstall.sh              # System uninstaller
│   ├── status.sh                 # Environment status dashboard + health score
│   └── doctor.sh                 # Diagnostic, health checks & snapshots
└── tests/
    ├── run_tests.sh              # Unit test runner
    └── test_download.sh          # Download function tests
```

---

## 🏗️ Building `.deb` Package

You can easily build a standalone Debian package (`.deb`) locally or let GitHub Actions build it automatically when pushing release tags.

### Building Locally:
```bash
chmod +x build_deb.sh
./build_deb.sh
```
The output file and its SHA256 checksum will be generated inside the `dist/` directory:
- `dist/aurastudio_1.1_all.deb`
- `dist/aurastudio_1.1_all.deb.sha256`

### CI/CD Pipeline:
When you push a version tag (`v*`), GitHub Actions automatically:
1. Runs ShellCheck lint and unit tests
2. Builds the `.deb` package
3. Generates a changelog from conventional commits
4. Creates a GitHub Release with assets

---

## ⌨️ Bash Autocompletion

AuraStudio CLI includes tab completion for all commands. It is automatically configured during installation.

### Manual Setup:
```bash
# Copy completion script
mkdir -p ~/.config/aurastudio
cp ~/.aurastudio/lib/aurastudio-completion.bash ~/.config/aurastudio/

# Add to your shell RC (auto-detects zsh/bash)
RC=""
[ -f ~/.zshrc ] && RC=~/.zshrc
[ -z "$RC" ] && RC="${PREFIX:-}/etc/bash.bashrc"
echo '[ -f ~/.config/aurastudio/aurastudio-completion.bash ] && source ~/.config/aurastudio/aurastudio-completion.bash' >> "$RC"
source "$RC"
```

### Available Completions:
- `aurastudio <TAB>` → All commands
- `aurastudio install <TAB>` → `sdk`, `ndk`, `cmake`
- `aurastudio status <TAB>` → `--json`, `-j`
- `aurastudio doctor <TAB>` → `--fix`, `--snapshot`

---

## ⚙️ Environment Configuration

During setup, AuraStudio generates an isolated configuration file at `~/.config/aurastudio/env.sh` and injects a single load line into your shell profile (`~/.zshrc` or `$PREFIX/etc/bash.bashrc`):

```bash
[ -f "$HOME/.config/aurastudio/env.sh" ] && source "$HOME/.config/aurastudio/env.sh"
```

### Managed Environment Variables:
- `ANDROID_HOME` & `ANDROID_SDK_ROOT` → Points to `$HOME/android-sdk`
- `JAVA_HOME` → Dynamically detects and points to OpenJDK 21
- `PATH` → Automatically includes `cmdline-tools`, `platform-tools`, `JAVA_HOME/bin`, and active `build-tools`

### XDG Base Directory Paths:
| Variable | Default Path | Description |
| :--- | :--- | :--- |
| `AURA_CONFIG_DIR` | `~/.config/aurastudio/` | Configuration files |
| `AURA_DATA_DIR` | `~/.local/share/aurastudio/` | Data files |
| `AURA_CACHE_DIR` | `~/.cache/aurastudio/` | Cache files |
| `AURA_ENV_FILE` | `~/.config/aurastudio/env.sh` | Environment variables |

---

## 🧪 Testing

Run the built-in unit tests to validate core functionality:

```bash
./tests/run_tests.sh
```

### Test Coverage:
- `check_storage()` function
- `download_file()` with valid/invalid URLs
- `basename_fast()` and `dirname_fast()` path utilities
- `parse_entry()` string parsing
- `detect_shell_rc()` shell detection
- `calculate_health_score()` health calculation
- `output_json_status()` JSON output

---

## 🩺 Diagnostics & Troubleshooting

If you encounter issues during builds or component execution, run the built-in diagnostic tool:

```bash
aurastudio doctor
```

### Additional Diagnostic Options:
```bash
# Auto-fix detected issues
aurastudio doctor --fix

# Save environment snapshot for debugging
aurastudio doctor --snapshot

# View detailed component status with health score
aurastudio status

# Output status as JSON
aurastudio status --json
```

### Common Fixes:
1. **Command Not Found Errors:**
   Ensure you have sourced the environment after setup:
   ```bash
   source ~/.config/aurastudio/env.sh
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

### Commit Convention:
This project uses [Conventional Commits](https://www.conventionalcommits.org/). Please format your commit messages as:

```
<type>(<scope>): <description>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`

Examples:
- `feat(ndk): add NDK r30 support`
- `fix(doctor): resolve eval vulnerability`
- `docs: update README with new commands`

### Steps:
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat(module): add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See `LICENSE` for more information.
