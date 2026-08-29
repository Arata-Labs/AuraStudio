# AuraStudio CLI

## Project Overview
AuraStudio CLI v1.5 is a Bash-based command-line tool suite for Android development in Termux on Android. It automates installation and configuration of development toolchains (OpenJDK 21, Gradle, Android SDK, AAPT2, custom NDK aarch64-linux-musl, CMake).

## Language & Stack
- **Primary Language:** Bash (shell scripts)
- **Target Environment:** Termux on Android (aarch64-linux-musl)
- **Dependencies:** git, curl, tar, unzip, aria2c (optional for parallel downloads)

## Project Structure

```
aurastudio/
├── aurastudio                    # Main CLI entry point & command router
├── aurastudio-app/               # Android GUI app (Kotlin + Jetpack Compose)
│   ├── app/src/main/java/com/aurastudio/
│   │   ├── MainActivity.kt              # Main activity with startup flow
│   │   ├── data/
│   │   │   ├── bootstrap/
│   │   │   │   ├── BootstrapCoordinator.kt
│   │   │   │   └── BootstrapState.kt
│   │   │   ├── models/Models.kt
│   │   │   ├── repository/
│   │   │   │   ├── DashboardRepository.kt
│   │   │   │   └── PackageInstaller.kt
│   │   │   └── viewmodel/DashboardViewModel.kt
│   │   └── ui/
│   │       ├── components/
│   │       │   ├── BottomNavBar.kt
│   │       │   └── TopBar.kt
│   │       ├── navigation/Navigation.kt
│   │       ├── screens/
│   │       │   ├── bootstrap/BootstrapSetupScreen.kt
│   │       │   ├── dashboard/DashboardScreen.kt
│   │       │   ├── projects/
│   │       │   │   ├── CreateProjectScreen.kt
│   │       │   │   └── ProjectsScreen.kt
│   │       │   ├── settings/SettingsScreen.kt
│   │       │   ├── splash/SplashScreen.kt
│   │       │   └── terminal/TerminalScreen.kt
│   │       └── theme/
│   │           ├── Color.kt
│   │           ├── Theme.kt
│   │           └── Type.kt
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   └── termux/                     # Embedded Termux terminal emulator (Java)
├── install.sh              # Web installer script
├── build_deb.sh            # Debian package builder
├── config/
│   └── env.sh              # Environment paths, color palette, package metadata
├── lib/
│   ├── ui.sh               # UI engine, RGB banners, spinners, loggers
│   ├── utils.sh            # Storage check, downloader (aria2c/curl), env writer
│   ├── tools.sh            # Package dependency manager, Java validators
│   └── aurastudio-completion.bash  # Bash completion script
├── modules/
│   ├── setup.sh            # Full environment setup orchestrator
│   ├── sdk.sh              # Android SDK & Build-Tools manager
│   ├── ndk.sh              # Custom NDK toolchain installer
│   ├── cmake.sh            # Native CMake installer
│   ├── init.sh             # Project scaffold generator (C++/NDK/Gradle)
│   ├── remove.sh           # Package uninstaller
│   ├── clean.sh            # Cache/temp cleanup
│   ├── update.sh           # GitHub auto-updater
│   ├── uninstall.sh        # Full uninstaller
│   ├── use.sh              # Java version switcher (17/21)
│   ├── status.sh           # Environment status dashboard + health score
│   └── doctor.sh           # Diagnostic, health checks & snapshots
├── tests/
│   ├── run_tests.sh        # Main test runner
│   └── test_download.sh    # Download function tests
└── .github/
    ├── workflows/
    │   ├── lint.yml        # ShellCheck CI + unit tests
    │   ├── release.yml     # Automated DEB build + changelog + GitHub Release
    │   └── build-bootstrap.yml
    ├── ISSUE_TEMPLATE/
    │   ├── bug_report.md
    │   └── feature_request.md
    ├── pull_request_template.md
    └── RELEASE_TEMPLATE.md
```

## Coding Conventions

### Style
- Use `#!/data/data/com.termux/files/usr/bin/env bash` shebang
- Add `# shellcheck disable=SC...` at top when needed
- Use RGB truecolor for UI: `\033[38;2;R;G;Bm`
- Colors defined in `config/env.sh`: INDIGO, PURPLE, CYAN, GREEN, AMBER, RED, MUTED, WHITE, BOLD, RESET

### Functions
- Public command functions: `cmd_<name>` (e.g., `cmd_setup`, `cmd_install_ndk`)
- UI helpers: `info()`, `success()`, `warn()`, `error()`, `step()`, `spin()`, `run_animated()`
- Use `draw_banner()` and `draw_divider()` for visual structure
- String parsing: `parse_entry()` for pipe-delimited strings
- Path utilities: `basename_fast()`, `dirname_fast()` (built-in, no fork)
- Network: `download_file()` with retry, `download_parallel()`
- System: `check_java()`, `detect_java_home()`, `check_storage()`, `ensure_tools()`
- User interaction: `menu_selector()`, `confirm_action()`
- Logging: `log()` with levels (DEBUG, INFO, WARN, ERROR)
- Retry: `retry_command()` with exponential backoff
- State management: `save_state()`, `restore_state()`
- Diagnostics: `calculate_health_score()`, `save_env_snapshot()`
- Plugins: `load_plugins()`
- Help: `show_man_page()`, `show_help()`

### Error Handling
- Check command existence: `command -v <cmd> &>/dev/null`
- Use `run_animated()` for background tasks with spinner
- Validate user input before execution
- Use `return 1` instead of `exit 1` in modules (prevents Termux session close)
- `retry_command()` for network operations with exponential backoff

### File Sourcing
- Entry point (`aurastudio`) sources: `config/env.sh`, `lib/*.sh`, `modules/*.sh`
- Modules are self-contained, loaded once at startup
- User plugins loaded from `~/.config/aurastudio/plugins/`

## Commands Reference

| Command | Handler Function | Description |
|---------|------------------|-------------|
| `aurastudio setup` | `cmd_setup` | Smart environment setup with status dashboard, skip existing, optional NDK/CMake |
| `aurastudio install sdk` | `cmd_install_sdk` | Install Android SDK platforms/build-tools |
| `aurastudio install ndk` | `cmd_install_ndk` | Install custom NDK versions |
| `aurastudio install cmake` | `cmd_install_cmake` | Install CMake versions |
| `aurastudio use java` | `cmd_use_java` | Switch active JDK instantly (17\|21) via symlink re-point |
| `aurastudio completion` | `show_completion_setup` | Show how to set up shell autocompletion |
| `aurastudio init` | `cmd_init` | Scaffold new project (C++/NDK/Gradle) |
| `aurastudio remove` | `cmd_remove` | Uninstall specific components |
| `aurastudio clean` | `cmd_clean` | Clean temp/cache files |
| `aurastudio update` | `cmd_update` | Update CLI from GitHub |
| `aurastudio check-update` | `cmd_update --check` | Check if update is available |
| `aurastudio status` | `cmd_status` | Show installed components with health score |
| `aurastudio status --json` | `output_json_status` | Output status in JSON format |
| `aurastudio doctor` | `cmd_doctor` | Run diagnostics and health checks |
| `aurastudio doctor --fix` | `cmd_doctor --fix` | Auto-fix detected issues |
| `aurastudio doctor --snapshot` | `cmd_doctor --snapshot` | Save environment snapshot |
| `aurastudio uninstall` | `cmd_uninstall` | Full uninstall |
| `aurastudio man` | `show_man_page` | Show full manual page |
| `aurastudio version` | - | Show CLI version |
| `aurastudio help` | `show_help` | Show help message |

## Key Configuration

- **SDK Directory:** `$HOME/android-sdk`
- **NDK Directory:** `$HOME/android-sdk/ndk`
- **CMake Directory:** `$HOME/android-sdk/cmake`
- **AAPT2 Global Override:** `~/.gradle/gradle.properties` (auto-configured during setup)
- **Environment File:** `~/.config/aurastudio/env.sh` (XDG-compliant)
- **Config Directory:** `~/.config/aurastudio/`
- **Plugins Directory:** `~/.config/aurastudio/plugins/`
- **Cache Directory:** `~/.cache/aurastudio/`
- **CLI Version:** Defined in `config/env.sh` as `CLI_VERSION`

## Utility Functions

### String Parsing
```bash
# Parse pipe-delimited entry (field starts at 1)
parse_entry "r30|r30-beta2|https://..." 1  # → "r30"
parse_entry "r30|r30-beta2|https://..." 2  # → "r30-beta2"
```

### Path Utilities (Fork-free)
```bash
basename_fast "/path/to/file.tar.xz"  # → "file.tar.xz"
dirname_fast "/path/to/file.tar.xz"   # → "/path/to"
```

### Network
```bash
# Download with retry (3 attempts, exponential backoff)
download_file "$url" "$output"

# Parallel downloads
download_parallel "$url1" "$out1" "$url2" "$out2"
```

### System Checks
```bash
# Check all dependencies at once
check_all_deps || exit 1

# Run command with timeout
run_with_timeout 120 curl -L "$url" -o "$out"

# Save/restore state
save_state
restore_state
```

### Diagnostics
```bash
# Get health score (0-100)
score=$(calculate_health_score)

# Save environment snapshot
snapshot_path=$(save_env_snapshot)
```

### JSON Output
```bash
# Get status as JSON
output_json_status
```

### Plugin System
```bash
# Load plugins from ~/.config/aurastudio/plugins/
load_plugins
```

## Testing & Validation

- Run `aurastudio doctor` to validate environment
- Run `aurastudio status` to check installed components and health score
- Run `aurastudio status --json` for JSON output
- Run `aurastudio doctor --snapshot` to save environment snapshot
- Test with `--verbose` or `-v` flag for debug output
- Build `.deb` with `./build_deb.sh` and test installation
- Run unit tests: `./tests/run_tests.sh`

## Common Tasks

### Add New Module
1. Create `modules/<name>.sh`
2. Define `cmd_<name>()` function
3. Source it in `aurastudio` entry point
4. Add case in command router

### Add New NDK/CMake Version
1. Edit `config/env.sh`
2. Add entry to `NDK_VERSIONS` or `CMAKE_VERSIONS` array
3. Format: `"Display Name|folder-name|download-url"`

### Create Plugin
1. Create `~/.config/aurastudio/plugins/<name>.sh`
2. Define custom functions (e.g., `cmd_mycommand()`)
3. Plugin will be auto-loaded on startup

### Modify UI
- Edit `lib/ui.sh` for core UI functions
- Colors are in `config/env.sh`
- Use existing helpers: `info()`, `success()`, `warn()`, `error()`
