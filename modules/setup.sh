#!/data/data/com.termux/files/usr/bin/env bash

cmd_setup() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Running Environment Setup${RESET}"
    draw_divider

    check_storage 500

    step "1/9" "Checking & Installing Dependencies..."
    ensure_tools curl unzip tar "findutils:find" sed grep "gawk:awk" which

    step "2/9" "Configuring OpenJDK 21..."
    if dpkg -l openjdk-21 &>/dev/null 2>&1; then
        success "openjdk-21 is already installed"
    else
        run_animated "Downloading & Installing openjdk-21" pkg install -y openjdk-21
    fi
    check_java

    step "3/9" "Configuring Gradle..."
    if dpkg -l gradle &>/dev/null 2>&1; then
        success "gradle is already installed"
    else
        run_animated "Downloading & Installing gradle" pkg install -y gradle
    fi

    step "4/9" "Creating SDK Directory Structure..."
    mkdir -p "$SDK_DIR"/{cmdline-tools,platform-tools,platforms,build-tools,ndk,cmake,licenses}
    success "SDK Root Directory: ${CYAN}$SDK_DIR${RESET}"

    step "5/9" "Downloading & Extracting Android cmdline-tools..."
    if [ -f "$SDKMANAGER" ]; then
        success "cmdline-tools is already installed"
    else
        local tmp_zip="$TMPDIR/aurastudio_cmdtools.zip" tmp_dir="$TMPDIR/aurastudio_cmdtools_extract"
        
        info "Downloading cmdline-tools package..."
        download_file "$CMDTOOLS_URL" "$tmp_zip"
        [ ! -s "$tmp_zip" ] && error "cmdline-tools download failed!" && exit 1

        (_ex_tools() {
            rm -rf "$tmp_dir"
            unzip -q "$tmp_zip" -d "$tmp_dir"
            mkdir -p "$CMDTOOLS_DIR"
            if [ -d "$tmp_dir/cmdline-tools" ]; then
                cp -r "$tmp_dir/cmdline-tools/." "$CMDTOOLS_DIR/"
            else
                cp -r "$tmp_dir/." "$CMDTOOLS_DIR/"
            fi
            rm -rf "$tmp_zip" "$tmp_dir"
        }; _ex_tools) &
        spin $! "Extracting cmdline-tools package"
        success "cmdline-tools successfully configured"
    fi
    chmod -R 755 "$CMDTOOLS_DIR/bin/" 2>/dev/null

    step "6/9" "Patching Termux Shebangs..."
    local termux_env
    termux_env=$(command -v env 2>/dev/null)
    if [ -n "$termux_env" ] && [ "$termux_env" != "/usr/bin/env" ]; then
        (_patch_shebang() {
            for bin_file in "$CMDTOOLS_DIR/bin/"*; do
                [ -f "$bin_file" ] || continue
                local shebang; shebang=$(head -1 "$bin_file" 2>/dev/null)
                if [[ "$shebang" == "#!/usr/bin/env"* ]]; then
                    local rest="${shebang#'#!/usr/bin/env'}"
                    sed -i "1s|.*|#!${termux_env}${rest}|" "$bin_file"
                fi
            done
        }; _patch_shebang) &
        spin $! "Patching SDK binary shebangs for Termux"
        success "Binary shebangs patched successfully"
    fi

    step "7/9" "Accepting Android SDK Licenses..."
    export ANDROID_HOME="$SDK_DIR"
    export ANDROID_SDK_ROOT="$SDK_DIR"
    export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"
    export PATH="$JAVA_HOME/bin:$CMDTOOLS_DIR/bin:$SDK_DIR/platform-tools:$PATH"

    (yes | "$SDKMANAGER" --licenses > /dev/null 2>&1) &
    spin $! "Verifying & accepting licenses"
    success "Android SDK licenses accepted"

    step "8/9" "Selecting & Installing Platform API & Build-Tools..."
    
    ("$SDKMANAGER" "platform-tools" > /dev/null 2>&1) &
    spin $! "Downloading platform-tools"
    success "platform-tools installed"

    printf "\n  %b\n\n" "${BOLD}Select Android Platform API level to install:${RESET}"
    local idx=1
    for api in "${PLATFORM_LIST[@]}"; do
        printf "  %b platforms;android-%s\n" "${CYAN}[$idx]${RESET}" "$api"
        idx=$((idx+1))
    done
    printf "  %b Custom API Level\n" "${CYAN}[c]${RESET}"
    printf "\n  Select option [1-${#PLATFORM_LIST[@]}/c] (default: 1 [API 37]): "
    read -r p_sel

    local sel_api="37"
    if [ "$p_sel" = "c" ]; then
        printf "  Enter API level (e.g. 29): "; read -r sel_api
    elif [[ "$p_sel" =~ ^[0-9]+$ ]] && [ "$p_sel" -ge 1 ] && [ "$p_sel" -le "${#PLATFORM_LIST[@]}" ]; then
        sel_api="${PLATFORM_LIST[$((p_sel-1))]}"
    elif [ -z "$p_sel" ]; then
        sel_api="37"
    fi

    printf "\n  %b\n\n" "${BOLD}Select Build-Tools version to install:${RESET}"
    idx=1
    for bt in "${BUILDTOOLS_LIST[@]}"; do
        printf "  %b build-tools;%s\n" "${CYAN}[$idx]${RESET}" "$bt"
        idx=$((idx+1))
    done
    printf "  %b Custom Build-Tools Version\n" "${CYAN}[c]${RESET}"
    printf "\n  Select option [1-${#BUILDTOOLS_LIST[@]}/c] (default: 1 [37.0.0]): "
    read -r bt_sel

    local sel_bt="37.0.0"
    if [ "$bt_sel" = "c" ]; then
        printf "  Enter Build-Tools version (e.g. 30.0.3): "; read -r sel_bt
    elif [[ "$bt_sel" =~ ^[0-9]+$ ]] && [ "$bt_sel" -ge 1 ] && [ "$bt_sel" -le "${#BUILDTOOLS_LIST[@]}" ]; then
        sel_bt="${BUILDTOOLS_LIST[$((bt_sel-1))]}"
    elif [ -z "$bt_sel" ]; then
        sel_bt="37.0.0"
    fi

    local target_platform="platforms;android-${sel_api}"
    local target_buildtools="build-tools;${sel_bt}"

    ("$SDKMANAGER" "$target_platform" "$target_buildtools" > /dev/null 2>&1) &
    spin $! "Downloading $target_platform & $target_buildtools"
    success "Selected SDK components installed ($target_platform, $target_buildtools)"

    step "9/9" "Writing Isolated Environment File..."
    write_env
    local shell_rc; shell_rc=$(detect_shell_rc)
    success "Environment written to ${CYAN}$HOME_DIR/.aurastudiorc${RESET}"

    echo ""
    draw_divider
    printf "  %b\n" "${GREEN}${BOLD}✨ Setup Complete! AuraStudio Environment is Ready.${RESET}"
    draw_divider
    printf "  %-20s %s\n" "ANDROID_HOME:" "$SDK_DIR"
    printf "  %-20s %s\n" "JAVA_HOME:"    "$(dirname $(dirname $(readlink -f $(which java))))"
    printf "  %-20s %s\n" "Platform:"     "$target_platform"
    printf "  %-20s %s\n" "Build-tools:"  "$target_buildtools"
    printf "\n  %b\n" "${MUTED}Tip: Use 'aurastudio install sdk' anytime to install additional APIs.${RESET}"
    echo ""
    printf "  Apply changes now by running: %b\n\n" "${CYAN}source $shell_rc${RESET}"
}
