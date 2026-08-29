#!/data/data/com.termux/files/usr/bin/env bash

cmd_setup() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Environment Setup — AuraStudio${RESET}"
    draw_divider

    if ! check_storage 500; then
        return 1
    fi

    save_state

    # ── Show current status ─────────────────────────────────────
    _show_status_summary

    step "1/8" "Checking & Installing Base Dependencies..."
    if ! ensure_tools curl unzip tar "findutils:find" sed grep "gawk:awk" which; then
        return 1
    fi

    step "2/8" "Configuring OpenJDK 21..."
    _setup_java

    step "3/8" "Configuring Gradle + AAPT2..."
    _setup_gradle

    step "4/8" "Creating SDK Directory Structure..."
    mkdir -p "$SDK_DIR"/{cmdline-tools,platform-tools,platforms,build-tools,ndk,cmake,licenses}
    success "SDK Root Directory: ${CYAN}$SDK_DIR${RESET}"

    step "5/8" "Downloading & Extracting Android cmdline-tools..."
    _setup_cmdtools

    step "6/8" "Patching Termux Shebangs..."
    _patch_shebangs

    step "7/8" "Accepting Android SDK Licenses & Installing Platform/Build-Tools..."
    _setup_sdk_platforms

    step "8/8" "Writing Isolated Environment File..."
    write_env
    success "Environment written to ${CYAN}$AURA_CONFIG_DIR/env.sh${RESET}"
    _setup_aapt2_override

    # ── Summary ─────────────────────────────────────────────────
    _show_final_summary

    # ── Optional NDK/CMake ──────────────────────────────────────
    _optional_ndk_cmake
}

# ── Status Summary ──────────────────────────────────────────────
_show_status_summary() {
    printf "\n  %b\n" "${BOLD}Current Environment Status:${RESET}"
    draw_divider

    local java_ver gradle_ver aapt2_ver cmdtools_ver
    java_ver=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/' 2>/dev/null) || java_ver=""
    gradle_ver=$(gradle --version 2>/dev/null | grep "^Gradle " | awk '{print $2}' 2>/dev/null) || gradle_ver=""
    aapt2_ver=$(aapt2 version 2>&1 | head -1) || aapt2_ver=""
    cmdtools_ver=""
    [ -f "$SDKMANAGER" ] && cmdtools_ver=$("$SDKMANAGER" --version 2>/dev/null | head -1) || cmdtools_ver=""

    # Installed platforms
    local platforms_str=""
    if [ -d "$SDK_DIR/platforms" ]; then
        platforms_str=$(ls "$SDK_DIR/platforms" 2>/dev/null | sed 's/android-/API /' | paste -sd ', ')
    fi

    # Installed build-tools
    local buildtools_str=""
    if [ -d "$SDK_DIR/build-tools" ]; then
        buildtools_str=$(ls "$SDK_DIR/build-tools" 2>/dev/null | paste -sd ', ')
    fi

    # Installed NDK versions
    local ndk_str=""
    if [ -d "$NDK_DIR" ]; then
        ndk_str=$(ls "$NDK_DIR" 2>/dev/null | paste -sd ', ')
    fi

    # Installed CMake versions
    local cmake_str=""
    if [ -d "$CMAKE_DIR" ]; then
        cmake_str=$(ls "$CMAKE_DIR" 2>/dev/null | paste -sd ', ')
    fi

    # Print status table
    _status_line "Java OpenJDK" "$java_ver"
    _status_line "Gradle" "$gradle_ver"
    _status_line "AAPT2" "$aapt2_ver"
    _status_line "cmdline-tools" "$cmdtools_ver"
    _status_line "Platforms" "${platforms_str:-none}"
    _status_line "Build-Tools" "${buildtools_str:-none}"
    _status_line "NDK" "${ndk_str:-none}"
    _status_line "CMake" "${cmake_str:-none}"

    echo ""
}

_status_line() {
    local label="$1" value="$2"
    if [ -n "$value" ]; then
        printf "  %b✔%b %-20s %b%s%b\n" "$GREEN" "$RESET" "$label" "$DIM" "$value" "$RESET"
    else
        printf "  %b✖%b %-20s %bNot installed%b\n" "$RED" "$RESET" "$label" "$MUTED" "$RESET"
    fi
}

# ── Java Setup ──────────────────────────────────────────────────
_setup_java() {
    if command -v java &>/dev/null; then
        local ver
        ver=$(java -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/')
        success "Java already installed: $ver"
    else
        run_animated "Downloading & Installing openjdk-21 & openjdk-17" pkg install -y openjdk-21 openjdk-17
    fi
    check_java
}

# ── Gradle + AAPT2 Setup ───────────────────────────────────────
_setup_gradle() {
    if command -v gradle &>/dev/null; then
        local ver
        ver=$(gradle --version 2>/dev/null | grep "^Gradle " | awk '{print $2}')
        success "Gradle already installed: $ver"
    else
        run_animated "Downloading & Installing gradle" pkg install -y gradle
    fi

    if command -v aapt2 &>/dev/null; then
        success "AAPT2 already installed"
    else
        run_animated "Downloading & Installing aapt2 (standalone)" pkg install -y aapt2
    fi
}

# ── cmdline-tools Setup ────────────────────────────────────────
_setup_cmdtools() {
    if [ -f "$SDKMANAGER" ]; then
        local ver
        ver=$("$SDKMANAGER" --version 2>/dev/null | head -1)
        success "cmdline-tools already installed: $ver"
        chmod -R 755 "$CMDTOOLS_DIR/bin/" 2>/dev/null
        return
    fi

    local tmp_zip="$TMPDIR/aurastudio_cmdtools.zip" tmp_dir="$TMPDIR/aurastudio_cmdtools_extract"
    
    info "Downloading cmdline-tools package..."
    if ! download_file "$CMDTOOLS_URL" "$tmp_zip"; then
        error "cmdline-tools download failed!"
        return 1
    fi

    _ex_tools() {
        rm -rf "$tmp_dir"
        unzip -q "$tmp_zip" -d "$tmp_dir" || return 1
        mkdir -p "$CMDTOOLS_DIR"
        if [ -d "$tmp_dir/cmdline-tools" ]; then
            cp -r "$tmp_dir/cmdline-tools/." "$CMDTOOLS_DIR/" || return 1
        else
            cp -r "$tmp_dir/." "$CMDTOOLS_DIR/" || return 1
        fi
        rm -rf "$tmp_zip" "$tmp_dir"
        return 0
    }
    
    if ! (_ex_tools); then
        error "cmdline-tools extraction failed!"
        return 1
    fi
    chmod -R 755 "$CMDTOOLS_DIR/bin/" 2>/dev/null
    success "cmdline-tools successfully configured"
}

# ── Patch Shebangs ──────────────────────────────────────────────
_patch_shebangs() {
    local termux_env
    termux_env=$(command -v env 2>/dev/null)
    if [ -n "$termux_env" ] && [ "$termux_env" != "/usr/bin/env" ]; then
        (_do_patch() {
            for bin_file in "$CMDTOOLS_DIR/bin/"*; do
                [ -f "$bin_file" ] || continue
                local shebang; shebang=$(head -1 "$bin_file" 2>/dev/null)
                if [[ "$shebang" == "#!/usr/bin/env"* ]]; then
                    local rest="${shebang#'#!/usr/bin/env'}"
                    sed -i "1s|.*|#!${termux_env}${rest}|" "$bin_file"
                fi
            done
        }; _do_patch) &
        spin $! "Patching SDK binary shebangs for Termux"
        success "Binary shebangs patched successfully"
    fi
}

# ── SDK Platforms & Build-Tools ────────────────────────────────
_setup_sdk_platforms() {
    export ANDROID_HOME="$SDK_DIR"
    export ANDROID_SDK_ROOT="$SDK_DIR"
    JAVA_HOME="$(detect_java_home)" || { error "Java not found"; return 1; }
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$CMDTOOLS_DIR/bin:$SDK_DIR/platform-tools:$PATH"

    (yes | "$SDKMANAGER" --licenses > /dev/null 2>&1) &
    spin $! "Verifying & accepting licenses"
    success "Android SDK licenses accepted"

    # Show already installed platforms
    local installed_platforms=()
    if [ -d "$SDK_DIR/platforms" ]; then
        while IFS= read -r p; do
            installed_platforms+=("$p")
        done < <(ls "$SDK_DIR/platforms" 2>/dev/null)
    fi

    local installed_buildtools=()
    if [ -d "$SDK_DIR/build-tools" ]; then
        while IFS= read -r b; do
            installed_buildtools+=("$b")
        done < <(ls "$SDK_DIR/build-tools" 2>/dev/null)
    fi

    # Show installed
    if [ "${#installed_platforms[@]}" -gt 0 ]; then
        printf "\n  %bInstalled Platforms:%b %s\n" "$GREEN" "$RESET" "$(printf '%s\n' "${installed_platforms[@]}" | sed 's/android-//' | paste -sd ', ')"
    fi
    if [ "${#installed_buildtools[@]}" -gt 0 ]; then
        printf "  %bInstalled Build-Tools:%b %s\n" "$GREEN" "$RESET" "$(printf '%s\n' "${installed_buildtools[@]}" | paste -sd ', ')"
    fi

    # If both platforms and build-tools exist, allow skipping entirely
    local has_both=true
    [ "${#installed_platforms[@]}" -eq 0 ] && has_both=false
    [ "${#installed_buildtools[@]}" -eq 0 ] && has_both=false

    local sel_api=""
    local sel_bt=""

    if $has_both; then
        printf "\n  %bSkip platform & build-tools setup?%b (already installed)\n" "${MUTED}" "$RESET"
        printf "  Select [y/N] (default: N): "
        read -r skip_all
        if [ "$skip_all" = "y" ] || [ "$skip_all" = "Y" ]; then
            info "Skipping — keeping existing platforms and build-tools"
            return
        fi
    fi

    printf "\n  %b\n\n" "${BOLD}Select Android Platform API level to install:${RESET}"
    local idx=1
    for api in "${PLATFORM_LIST[@]}"; do
        local tag=""
        [[ -d "$SDK_DIR/platforms/android-$api" ]] && tag=" ${GREEN}(installed)${RESET}"
        printf "  %b platforms;android-%s%b\n" "${CYAN}[$idx]${RESET}" "$api" "$tag"
        idx=$((idx+1))
    done
    printf "  %b Custom API Level\n" "${CYAN}[c]${RESET}"
    printf "  %b Skip\n" "${MUTED}[s]${RESET}"
    printf "\n  Select option [1-%d/c/s] (default: 1 [API 37]): " "${#PLATFORM_LIST[@]}"
    read -r p_sel

    if [ "$p_sel" = "s" ] || [ "$p_sel" = "S" ]; then
        info "Skipping platform installation"
    elif [ "$p_sel" = "c" ]; then
        printf "  Enter API level (e.g. 29): "; read -r sel_api
    elif [[ "$p_sel" =~ ^[0-9]+$ ]] && [ "$p_sel" -ge 1 ] && [ "$p_sel" -le "${#PLATFORM_LIST[@]}" ]; then
        sel_api="${PLATFORM_LIST[$((p_sel-1))]}"
    elif [ -z "$p_sel" ]; then
        sel_api="37"
    fi

    printf "\n  %b\n\n" "${BOLD}Select Build-Tools version to install:${RESET}"
    idx=1
    for bt in "${BUILDTOOLS_LIST[@]}"; do
        local tag=""
        [[ -d "$SDK_DIR/build-tools/$bt" ]] && tag=" ${GREEN}(installed)${RESET}"
        printf "  %b build-tools;%s%b\n" "${CYAN}[$idx]${RESET}" "$bt" "$tag"
        idx=$((idx+1))
    done
    printf "  %b Custom Build-Tools Version\n" "${CYAN}[c]${RESET}"
    printf "  %b Skip\n" "${MUTED}[s]${RESET}"
    printf "\n  Select option [1-%d/c/s] (default: 1 [37.0.0]): " "${#BUILDTOOLS_LIST[@]}"
    read -r bt_sel

    if [ "$bt_sel" = "s" ] || [ "$bt_sel" = "S" ]; then
        info "Skipping build-tools installation"
    elif [ "$bt_sel" = "c" ]; then
        printf "  Enter Build-Tools version (e.g. 30.0.3): "; read -r sel_bt
    elif [[ "$bt_sel" =~ ^[0-9]+$ ]] && [ "$bt_sel" -ge 1 ] && [ "$bt_sel" -le "${#BUILDTOOLS_LIST[@]}" ]; then
        sel_bt="${BUILDTOOLS_LIST[$((bt_sel-1))]}"
    elif [ -z "$bt_sel" ]; then
        sel_bt="37.0.0"
    fi

    # Install selected
    local to_install=()
    [ -n "$sel_api" ] && to_install+=("platforms;android-${sel_api}")
    [ -n "$sel_bt" ] && to_install+=("build-tools;${sel_bt}")

    if [ "${#to_install[@]}" -gt 0 ]; then
        ("$SDKMANAGER" "${to_install[@]}" > /dev/null 2>&1) &
        spin $! "Installing ${to_install[*]}"
        success "SDK components installed: ${to_install[*]}"
    fi
}

# ── Global AAPT2 Override ──────────────────────────────────────
_setup_aapt2_override() {
    local gradle_props="$HOME/.gradle/gradle.properties"
    if ! grep -q "aapt2FromMavenOverride" "$gradle_props" 2>/dev/null; then
        mkdir -p "$HOME/.gradle"
        cat >> "$gradle_props" << 'EOF'

# AuraStudio: use standalone aapt2 instead of Gradle bundled
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
EOF
        success "Global Gradle aapt2 override configured"
    fi
}

# ── Final Summary ──────────────────────────────────────────────
_show_final_summary() {
    echo ""
    draw_divider
    printf "  %b\n" "${GREEN}${BOLD}Setup Complete! AuraStudio Environment is Ready.${RESET}"
    draw_divider

    JAVA_HOME="$(detect_java_home 2>/dev/null)"
    printf "  %-20s %s\n" "ANDROID_HOME:" "$SDK_DIR"
    printf "  %-20s %s\n" "JAVA_HOME:"    "${JAVA_HOME:-unknown}"
    printf "  %-20s %s\n" "Config Dir:"   "$AURA_CONFIG_DIR"
    echo ""

    _show_status_summary

    printf "\n  %b\n" "${MUTED}Tip: Use 'aurastudio install sdk' anytime to install additional APIs.${RESET}"
    echo ""
    local shell_rc
    shell_rc="$(detect_shell_rc)"
    printf "  Apply changes now by running: %b\n\n" "${CYAN}source $shell_rc${RESET}"
}

# ── Optional NDK/CMake ────────────────────────────────────────
_optional_ndk_cmake() {
    printf "\n  %b\n" "${BOLD}Optional: Install NDK & CMake?${RESET}"
    draw_divider

    # Show installed NDK
    local ndk_list=()
    if [ -d "$NDK_DIR" ]; then
        while IFS= read -r d; do
            ndk_list+=("$d")
        done < <(ls "$NDK_DIR" 2>/dev/null)
    fi
    if [ "${#ndk_list[@]}" -gt 0 ]; then
        printf "  %bInstalled NDK:%b %s\n" "$GREEN" "$RESET" "$(printf '%s\n' "${ndk_list[@]}" | paste -sd ', ')"
    else
        printf "  %bInstalled NDK:%b none\n" "$MUTED" "$RESET"
    fi

    # Show installed CMake
    local cmake_list=()
    if [ -d "$CMAKE_DIR" ]; then
        while IFS= read -r d; do
            cmake_list+=("$d")
        done < <(ls "$CMAKE_DIR" 2>/dev/null)
    fi
    if [ "${#cmake_list[@]}" -gt 0 ]; then
        printf "  %bInstalled CMake:%b %s\n" "$GREEN" "$RESET" "$(printf '%s\n' "${cmake_list[@]}" | paste -sd ', ')"
    else
        printf "  %bInstalled CMake:%b none\n" "$MUTED" "$RESET"
    fi

    echo ""
    printf "  %b Install NDK (custom aarch64-linux-musl)\n" "${CYAN}[1]${RESET}"
    printf "  %b Install CMake\n" "${CYAN}[2]${RESET}"
    printf "  %b Skip\n\n" "${MUTED}[s]${RESET}"
    printf "  Select option [1-2/s]: "
    read -r opt_ndk

    case "$opt_ndk" in
        1)
            cmd_install_ndk
            ;;
        2)
            cmd_install_cmake
            ;;
        *)
            info "Skipping NDK/CMake installation"
            ;;
    esac
}
