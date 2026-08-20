#!/data/data/com.termux/files/usr/bin/env bash

cleanup() {
    rm -rf "$TMPDIR"/aurastudio_* 2>/dev/null
    tput cnorm 2>/dev/null
}
trap 'cleanup; exit' SIGINT SIGTERM EXIT

check_storage() {
    local req_mb="$1"
    local free_mb
    free_mb=$(df -m "$HOME_DIR" 2>/dev/null | awk 'NR==2 {print $4}')
    if [ -n "$free_mb" ] && [ "$free_mb" -lt "$req_mb" ]; then
        error "Insufficient storage! Need ${req_mb}MB, only ${free_mb}MB available."
        return 1
    fi
    return 0
}

download_file() {
    local url="$1"
    local out="$2"
    local out_dir
    out_dir="$(dirname "$out")"
    local out_name
    out_name="$(basename "$out")"

    mkdir -p "$out_dir"

    # Cache avoidance - skip if file exists and valid
    if [ -f "$out" ] && [ -s "$out" ]; then
        if [ -f "${out}.sha256" ]; then
            if sha256sum -c "${out}.sha256" &>/dev/null; then
                info "Using cached: $out_name"
                return 0
            fi
        elif [ "$(stat -c%s "$out" 2>/dev/null || stat -f%z "$out" 2>/dev/null)" -gt 0 ]; then
            info "Using cached: $out_name"
            return 0
        fi
    fi

    _download_internal() {
        local exit_code=0
        if command -v aria2c &>/dev/null; then
            aria2c -x16 -s16 -k1M --continue=true --dir="$out_dir" --out="$out_name" "$url" || exit_code=$?
        else
            curl -L -C - --retry 3 --retry-connrefused --connect-timeout 10 --max-time 300 --progress-bar "$url" -o "$out" || exit_code=$?
        fi

        if [ $exit_code -ne 0 ] || [ ! -f "$out" ] || [ ! -s "$out" ]; then
            rm -f "$out" 2>/dev/null
            return 1
        fi
        return 0
    }

    if ! retry_command 3 2 _download_internal; then
        error "Download failed after 3 attempts: $out_name"
        return 1
    fi

    return 0
}

verify_download() {
    local file="$1"
    local expected_hash="${2:-}"
    
    [ ! -f "$file" ] && return 1
    [ ! -s "$file" ] && return 1
    
    if [ -n "$expected_hash" ]; then
        local actual_hash
        actual_hash="$(sha256sum "$file" 2>/dev/null | cut -d' ' -f1)"
        [ "$actual_hash" = "$expected_hash" ] && return 0 || return 1
    fi
    
    return 0
}

menu_selector() {
    local prompt="$1"
    shift
    local options=("$@")
    local max=${#options[@]}
    
    printf "\n  %b\n\n" "${BOLD}${prompt}${RESET}"
    local i=1
    for opt in "${options[@]}"; do
        printf "  %b %s\n" "${CYAN}[$i]${RESET}" "$opt"
        i=$((i + 1))
    done
    printf "  %b Custom\n" "${CYAN}[c]${RESET}"
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-%d/c/q]: " "$max"
    read -r selection
    
    case "$selection" in
        q|Q) echo "cancel"; return 1 ;;
        c|C) echo "custom"; return 0 ;;
        *)
            if [[ "$selection" =~ ^[0-9]+$ ]] && [ "$selection" -ge 1 ] && [ "$selection" -le "$max" ]; then
                echo "$selection"
                return 0
            fi
            error "Invalid selection"
            return 1
            ;;
    esac
}

confirm_action() {
    local prompt="$1"
    local default="${2:-N}"
    local yn_prompt="[y/N]"
    [ "$default" = "y" ] && yn_prompt="[Y/n]"
    
    printf "  %s %s " "$prompt" "$yn_prompt"
    read -r answer
    
    case "${answer:-$default}" in
        y|Y) return 0 ;;
        *) return 1 ;;
    esac
}

download_parallel() {
    local pids=()
    local outputs=()
    local i=0
    
    while [ $# -ge 2 ]; do
        local url="$1"
        local out="$2"
        shift 2
        
        download_file "$url" "$out" &
        pids+=($!)
        outputs+=("$out")
        i=$((i + 1))
    done
    
    local failed=0
    for idx in "${!pids[@]}"; do
        if ! wait "${pids[$idx]}"; then
            error "Parallel download failed: ${outputs[$idx]}"
            failed=$((failed + 1))
        fi
    done
    
    [ "$failed" -eq 0 ] && return 0 || return 1
}

parse_entry() {
    local entry="$1"
    local field="${2:-1}"
    local delimiter="${3:-|}"
    
    local IFS="$delimiter"
    local parts=($entry)
    echo "${parts[$((field - 1))]}"
}

basename_fast() {
    local path="$1"
    echo "${path##*/}"
}

dirname_fast() {
    local path="$1"
    echo "${path%/*}"
}

log() {
    local level="$1"
    shift
    local timestamp
    timestamp="$(date '+%H:%M:%S' 2>/dev/null || echo "00:00:00")"
    
    case "$level" in
        DEBUG)
            [ "${DEBUG:-0}" -eq 1 ] && printf "[${timestamp}] [DEBUG] %s\n" "$*" >&2
            ;;
        INFO)
            printf "[${timestamp}] [INFO] %s\n" "$*" >&2
            ;;
        WARN)
            printf "[${timestamp}] [WARN] %s\n" "$*" >&2
            ;;
        ERROR)
            printf "[${timestamp}] [ERROR] %s\n" "$*" >&2
            ;;
    esac
}

retry_command() {
    local max_retries="${1:-3}"
    local delay="${2:-2}"
    shift 2
    
    local attempt=1
    while [ "$attempt" -le "$max_retries" ]; do
        if "$@"; then
            return 0
        fi
        
        if [ "$attempt" -lt "$max_retries" ]; then
            log WARN "Attempt $attempt/$max_retries failed, retrying in ${delay}s..."
            sleep "$delay"
            delay=$((delay * 2))
        fi
        attempt=$((attempt + 1))
    done
    
    log ERROR "All $max_retries attempts failed for: $*"
    return 1
}

detect_java_home() {
    local java_bin
    java_bin="$(command -v java 2>/dev/null)" || return 1
    local real_path
    real_path="$(readlink -f "$java_bin" 2>/dev/null)" || real_path="$java_bin"
    local dir
    dir="$(dirname "$(dirname "$real_path")")"
    [ -d "$dir" ] && echo "$dir" && return 0
    return 1
}

detect_shell_rc() {
    [ -f "$HOME_DIR/.zshrc" ] && echo "$HOME_DIR/.zshrc" || echo "$PREFIX/etc/bash.bashrc"
}

write_env() {
    local env_file="$AURA_CONFIG_DIR/env.sh"
    mkdir -p "$(dirname "$env_file")"
    cat > "$env_file" << 'ENV'
# ── AuraStudio Environment Configuration ──────────────────
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

if command -v java &>/dev/null; then
    _java_real="$(readlink -f "$(command -v java)" 2>/dev/null || command -v java)"
    export JAVA_HOME="$(dirname "$(dirname "$_java_real")")"
    unset _java_real
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# Add build-tools to PATH (latest first)
for _bt_dir in "$ANDROID_HOME/build-tools"/*/; do
    [ -d "$_bt_dir" ] && export PATH="${_bt_dir}bin:$PATH"
done
unset _bt_dir

# ── End AuraStudio ─────────────────────────────────────────
ENV

    # Auto-Completion Script Injection if available
    local comp_script_deb="$PREFIX/opt/aurastudio/lib/aurastudio-completion.bash"
    local comp_script_git="$HOME/.aurastudio/lib/aurastudio-completion.bash"
    local comp_script_auraroot="$AURA_ROOT/lib/aurastudio-completion.bash"
    
    local comp_script=""
    if [ -f "$comp_script_deb" ]; then
        comp_script="$comp_script_deb"
    elif [ -f "$comp_script_git" ]; then
        comp_script="$comp_script_git"
    elif [ -n "$AURA_ROOT" ] && [ -f "$comp_script_auraroot" ]; then
        comp_script="$comp_script_auraroot"
    fi
    
    if [ -n "$comp_script" ]; then
        echo "[ -f \"$comp_script\" ] && source \"$comp_script\"" >> "$env_file"
    fi

    # Inject sourcing env into shell profile
    local shell_rc
    shell_rc=$(detect_shell_rc)
    local source_line="[ -f \"$AURA_CONFIG_DIR/env.sh\" ] && source \"$AURA_CONFIG_DIR/env.sh\""
    if ! grep -qF "aurastudio/env.sh" "$shell_rc" 2>/dev/null; then
        echo -e "\n$source_line" >> "$shell_rc"
    fi
}

check_all_deps() {
    local deps=("bash" "curl" "tar" "unzip" "git" "sed" "grep" "awk" "find")
    local missing=()
    
    for dep in "${deps[@]}"; do
        command -v "$dep" &>/dev/null || missing+=("$dep")
    done
    
    if [ "${#missing[@]}" -gt 0 ]; then
        error "Missing dependencies: ${missing[*]}"
        printf "  %b\n" "${MUTED}Run: pkg install ${missing[*]}${RESET}"
        return 1
    fi
    return 0
}

run_with_timeout() {
    local timeout="${1:-60}"
    shift
    
    if command -v timeout &>/dev/null; then
        timeout --signal=KILL "$timeout" "$@" 2>/dev/null
        local exit_code=$?
        if [ "$exit_code" -eq 124 ]; then
            error "Operation timed out after ${timeout}s"
            return 1
        fi
        return "$exit_code"
    else
        # Fallback without timeout command
        "$@" &
        local pid=$!
        local count=0
        while kill -0 "$pid" 2>/dev/null; do
            sleep 1
            count=$((count + 1))
            if [ "$count" -ge "$timeout" ]; then
                kill -9 "$pid" 2>/dev/null
                wait "$pid" 2>/dev/null
                error "Operation timed out after ${timeout}s"
                return 1
            fi
        done
        wait "$pid"
        return $?
    fi
}

save_state() {
    local state_file="$AURA_CACHE_DIR/state_$(date +%s).json"
    mkdir -p "$AURA_CACHE_DIR"
    
    local ndk_list cmake_list sdk_platforms sdk_buildtools
    ndk_list=$(ls -1 "$NDK_DIR" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
    cmake_list=$(ls -1 "$CMAKE_DIR" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
    sdk_platforms=$(ls -1 "$SDK_DIR/platforms" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
    sdk_buildtools=$(ls -1 "$SDK_DIR/build-tools" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
    
    cat > "$state_file" << EOF
{
    "timestamp": "$(date -Iseconds)",
    "cli_version": "$CLI_VERSION",
    "installed_ndk": "$ndk_list",
    "installed_cmake": "$cmake_list",
    "sdk_platforms": "$sdk_platforms",
    "sdk_buildtools": "$sdk_buildtools"
}
EOF
    
    # Keep only last 5 states
    ls -t "$AURA_CACHE_DIR"/state_*.json 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null
    
    log DEBUG "State saved: $state_file"
    return 0
}

restore_state() {
    local state_file="${1:-}"
    
    if [ -z "$state_file" ]; then
        state_file=$(ls -t "$AURA_CACHE_DIR"/state_*.json 2>/dev/null | head -1)
    fi
    
    if [ ! -f "$state_file" ]; then
        error "No state file found to restore"
        return 1
    fi
    
    info "Restoring state from: $(basename "$state_file")"
    
    # Parse state file
    local ndk_list cmake_list
    ndk_list=$(grep '"installed_ndk"' "$state_file" | cut -d'"' -f4)
    cmake_list=$(grep '"installed_cmake"' "$state_file" | cut -d'"' -f4)
    
    # Report differences
    local current_ndk
    current_ndk=$(ls -1 "$NDK_DIR" 2>/dev/null | tr '\n' ',' | sed 's/,$//')
    
    if [ "$ndk_list" != "$current_ndk" ]; then
        warn "NDK versions changed since last state"
    fi
    
    return 0
}

output_json_status() {
    local java_installed=false
    local gradle_installed=false
    local git_installed=false
    local aria2_installed=false
    
    command -v java &>/dev/null && java_installed=true
    command -v gradle &>/dev/null && gradle_installed=true
    command -v git &>/dev/null && git_installed=true
    command -v aria2c &>/dev/null && aria2_installed=true
    
    local ndk_versions cmake_versions
    ndk_versions=$(ls -1 "$NDK_DIR" 2>/dev/null | sed 's/^/"/;s/$/"/' | paste -sd, 2>/dev/null || echo "")
    cmake_versions=$(ls -1 "$CMAKE_DIR" 2>/dev/null | sed 's/^/"/;s/$/"/' | paste -sd, 2>/dev/null || echo "")
    local sdk_platforms
    sdk_platforms=$(ls -1 "$SDK_DIR/platforms" 2>/dev/null | sed 's/^/"/;s/$/"/' | paste -sd, 2>/dev/null || echo "")
    
    cat << EOF
{
    "cli_version": "$CLI_VERSION",
    "timestamp": "$(date -Iseconds)",
    "tools": {
        "java": $java_installed,
        "gradle": $gradle_installed,
        "git": $git_installed,
        "aria2": $aria2_installed
    },
    "paths": {
        "sdk_dir": "$SDK_DIR",
        "ndk_dir": "$NDK_DIR",
        "cmake_dir": "$CMAKE_DIR"
    },
    "installed": {
        "ndk": [${ndk_versions}],
        "cmake": [${cmake_versions}],
        "sdk_platforms": [${sdk_platforms}]
    }
}
EOF
}

calculate_health_score() {
    local score=0
    local max_score=100
    
    # Java (+20)
    command -v java &>/dev/null && score=$((score + 20))
    
    # Gradle (+15)
    command -v gradle &>/dev/null && score=$((score + 15))
    
    # Git (+5)
    command -v git &>/dev/null && score=$((score + 5))
    
    # SDK Directory (+10)
    [ -d "$SDK_DIR" ] && score=$((score + 10))
    
    # cmdline-tools (+15)
    [ -f "$SDKMANAGER" ] && score=$((score + 15))
    
    # platform-tools (+5)
    [ -f "$SDK_DIR/platform-tools/adb" ] && score=$((score + 5))
    
    # NDK (+10)
    if [ -d "$NDK_DIR" ] && [ "$(ls -1 "$NDK_DIR" 2>/dev/null | wc -l)" -gt 0 ]; then
        score=$((score + 10))
    fi
    
    # CMake (+5)
    if [ -d "$CMAKE_DIR" ] && [ "$(ls -1 "$CMAKE_DIR" 2>/dev/null | wc -l)" -gt 0 ]; then
        score=$((score + 5))
    
    fi
    
    # aria2 bonus (+5)
    command -v aria2c &>/dev/null && score=$((score + 5))
    
    echo "$score"
}

save_env_snapshot() {
    local snapshot="$AURA_CACHE_DIR/env_snapshot_$(date +%Y%m%d_%H%M%S).txt"
    mkdir -p "$AURA_CACHE_DIR"
    
    {
        echo "=========================================="
        echo "  AuraStudio Environment Snapshot"
        echo "=========================================="
        echo "Date: $(date)"
        echo "Shell: ${SHELL:-unknown}"
        echo "Bash Version: ${BASH_VERSION:-unknown}"
        echo "Termux: ${TERMUX_VERSION:-unknown}"
        echo ""
        echo "=== System Info ==="
        uname -a 2>/dev/null
        echo ""
        echo "=== PATH ==="
        echo "$PATH" | tr ':' '\n'
        echo ""
        echo "=== Environment Variables ==="
        echo "ANDROID_HOME=${ANDROID_HOME:-not set}"
        echo "ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-not set}"
        echo "JAVA_HOME=${JAVA_HOME:-not set}"
        echo ""
        echo "=== Installed Tools ==="
        java -version 2>&1 | head -1 || echo "Java: not installed"
        gradle -v 2>/dev/null | grep 'Gradle ' | head -1 || echo "Gradle: not installed"
        git --version 2>/dev/null || echo "Git: not installed"
        aria2c --version 2>/dev/null | head -1 || echo "aria2: not installed"
        echo ""
        echo "=== SDK Contents ==="
        echo "SDK Directory: $SDK_DIR"
        ls -la "$SDK_DIR" 2>/dev/null || echo "SDK not found"
        echo ""
        echo "Platform Tools:"
        ls -1 "$SDK_DIR/platform-tools" 2>/dev/null | head -5 || echo "None"
        echo ""
        echo "Platforms:"
        ls -1 "$SDK_DIR/platforms" 2>/dev/null || echo "None"
        echo ""
        echo "Build Tools:"
        ls -1 "$SDK_DIR/build-tools" 2>/dev/null || echo "None"
        echo ""
        echo "=== NDK ==="
        ls -1 "$NDK_DIR" 2>/dev/null || echo "None"
        echo ""
        echo "=== CMake ==="
        ls -1 "$CMAKE_DIR" 2>/dev/null || echo "None"
        echo ""
        echo "=== AuraStudio Config ==="
        echo "Config Dir: $AURA_CONFIG_DIR"
        ls -la "$AURA_CONFIG_DIR" 2>/dev/null || echo "Config dir not found"
    } > "$snapshot" 2>&1
    
    echo "$snapshot"
}

load_plugins() {
    local plugin_dir="$AURA_CONFIG_DIR/plugins"
    [ -d "$plugin_dir" ] || return 0
    
    local loaded=0
    for plugin in "$plugin_dir"/*.sh; do
        if [ -f "$plugin" ]; then
            if source "$plugin" 2>/dev/null; then
                loaded=$((loaded + 1))
                log DEBUG "Loaded plugin: $(basename "$plugin")"
            else
                warn "Failed to load plugin: $(basename "$plugin")"
            fi
        fi
    done
    
    [ "$loaded" -gt 0 ] && log INFO "Loaded $loaded plugin(s)"
    return 0
}

show_man_page() {
    cat << 'MANPAGE'
AURASTUDIO(1)                   User Commands                   AURASTUDIO(1)

NAME
       aurastudio - Next-Gen Termux Android Development Tool

SYNOPSIS
       aurastudio [command] [options]

DESCRIPTION
       AuraStudio CLI is an automated, modular command-line tool suite
       for Android development in Termux on Android. It automates
       installation and configuration of development toolchains.

COMMANDS
       setup           Full environment setup (Java, Gradle, SDK, NDK)
       
       install sdk     Install Android SDK platforms and build-tools
       install ndk     Install custom NDK versions (aarch64-linux-musl)
       install cmake   Install native CMake versions
       
       init            Create new project from templates
                         1. C++ Native CMake Starter
                         2. Android NDK Shared Library
                         3. Android App - Java (Gradle)
                         4. Android App - Kotlin (Gradle)
       
       remove          Uninstall specific components (interactive)
       clean           Clean temporary files and cache
       update          Update CLI from GitHub
       check-update    Check if update is available
       uninstall       Completely remove AuraStudio CLI
       
       status          Show installed components dashboard
       doctor          Run diagnostics and health checks
       version         Show CLI version

OPTIONS
       --verbose, -v   Enable debug output and verbose logging
       --help, -h      Show this help message
       --version       Show version number
       --json          Output status in JSON format (status command)

EXAMPLES
       aurastudio setup
           Run full environment setup wizard
       
       aurastudio install ndk
           Install custom NDK toolchain
       
       aurastudio install sdk platform 34 buildtools 34.0.0
           Install specific SDK components
       
       aurastudio init
           Create new project from templates
       
       aurastudio status --json
           Show status in JSON format
       
       aurastudio doctor
           Run environment diagnostics

ENVIRONMENT
       ANDROID_HOME    Android SDK directory (~/android-sdk)
       JAVA_HOME       Java installation directory
       AURA_CONFIG     AuraStudio config directory (~/.config/aurastudio)

FILES
       ~/.config/aurastudio/     Configuration directory
       ~/.config/aurastudio/env.sh   Environment variables
       ~/.config/aurastudio/plugins/ User plugins
       ~/.cache/aurastudio/      Cache directory
       ~/android-sdk/            Android SDK

BUGS
       Report bugs at: https://github.com/Arata-Labs/AuraStudio/issues

AUTHOR
       HinohArata <github.com/HinohArata>

LICENSE
       MIT License

AURA STUDIO CLI v1.0            August 2026                    AURASTUDIO(1)
MANPAGE
}

show_help() {
    clear
    draw_banner
    printf "\n  %b\n\n" "${BOLD}CLI Usage Guide:${RESET}"
    printf "  %b\n" "${CYAN}aurastudio setup${RESET}"
    printf "  %b\n\n" "${MUTED}    Full automated environment setup (Java 21, Gradle, SDK Manager, API 37/Custom)${RESET}"
    printf "  %b\n" "${CYAN}aurastudio install sdk [platform <API>] [buildtools <ver>]${RESET}"
    printf "  %b\n\n" "${MUTED}    Select and install Android Platform APIs and Build-Tools${RESET}"
    printf "  %b\n" "${CYAN}aurastudio install ndk${RESET}"
    printf "  %b\n\n" "${MUTED}    Select and install Native NDK (r30, r29, r28c, r27d, r26d) & CMake${RESET}"
    printf "  %b\n" "${CYAN}aurastudio install cmake${RESET}"
    printf "  %b\n\n" "${MUTED}    Select and install native CMake SDK versions${RESET}"
    printf "  %b\n" "${CYAN}aurastudio update${RESET}"
    printf "  %b\n\n" "${MUTED}    Update AuraStudio CLI to the latest version from GitHub${RESET}"
    printf "  %b\n" "${CYAN}aurastudio check-update${RESET}"
    printf "  %b\n\n" "${MUTED}    Check if a new update is available on GitHub${RESET}"
    printf "  %b\n" "${CYAN}aurastudio remove [ndk|cmake|sdk] [name]${RESET}"
    printf "  %b\n\n" "${MUTED}    Uninstall specific components or run interactively${RESET}"
    printf "  %b\n" "${CYAN}aurastudio clean${RESET}"
    printf "  %b\n\n" "${MUTED}    Clean temporary downloaded files and extraction cache${RESET}"
    printf "  %b\n" "${CYAN}aurastudio init${RESET}"
    printf "  %b\n\n" "${MUTED}    Generate starter project (CMake C++, NDK, Android Gradle Java/Kotlin)${RESET}"
    printf "  %b\n" "${CYAN}aurastudio uninstall${RESET}"
    printf "  %b\n\n" "${MUTED}    Completely uninstall AuraStudio CLI and optional SDK files${RESET}"
    printf "  %b\n" "${CYAN}aurastudio status [--json]${RESET}"
    printf "  %b\n\n" "${MUTED}    Show a visual status summary of installed components${RESET}"
    printf "  %b\n" "${CYAN}aurastudio doctor${RESET}"
    printf "  %b\n\n" "${MUTED}    Run diagnostic checks and view recommended fixes${RESET}"
    printf "  %b\n" "${CYAN}aurastudio man${RESET}"
    printf "  %b\n\n" "${MUTED}    Show full manual page${RESET}"
    draw_divider
    echo ""
}

