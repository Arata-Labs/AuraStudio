#!/data/data/com.termux/files/usr/bin/env bash

_update_java_home() {
    local new_jdk_home="$1"
    local env_file="$AURA_CONFIG_DIR/env.sh"
    if [ -f "$env_file" ]; then
        sed -i "s|JAVA_HOME=.*|JAVA_HOME=\"$new_jdk_home\"|" "$env_file"
    fi
}

cmd_install_sdk() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Installing Android SDK Components (Multi-Version)${RESET}"
    draw_divider

    if [ ! -f "$SDKMANAGER" ]; then
        error "cmdline-tools not found. Run: aurastudio setup"
        return 1
    fi

    export ANDROID_HOME="$SDK_DIR"
    export ANDROID_SDK_ROOT="$SDK_DIR"
    JAVA_HOME="$(detect_java_home)" || { error "Java not found"; return 1; }
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$CMDTOOLS_DIR/bin:$SDK_DIR/platform-tools:$PATH"

    local param_api="" param_bt="" param_java="" param_java_use=""
    local args=("${@}")
    local idx=0
    while [ $idx -lt ${#args[@]} ]; do
        case "${args[$idx]}" in
            platform)
                idx=$((idx+1)); param_api="${args[$idx]}" ;;
            buildtools)
                idx=$((idx+1)); param_bt="${args[$idx]}" ;;
            java)
                idx=$((idx+1)); param_java="${args[$idx]}" ;;
            java-use)
                idx=$((idx+1)); param_java_use="${args[$idx]}" ;;
        esac
        idx=$((idx+1))
    done

    if [ -n "$param_java" ]; then
        if [[ "$param_java" =~ ^(21|17)$ ]]; then
            info "Installing openjdk-${param_java}..."
            run_animated "Installing openjdk-${param_java}" pkg install -y "openjdk-${param_java}"
            success "openjdk-${param_java} installed"
        else
            error "Unsupported Java version: $param_java (supported: 21, 17)"
            return 1
        fi
        return
    fi

    if [ -n "$param_java_use" ]; then
        if [[ "$param_java_use" =~ ^(21|17)$ ]]; then
            local new_home
            new_home="$(detect_java_home "$param_java_use")"
            if [ -n "$new_home" ]; then
                _update_java_home "$new_home"
                export JAVA_HOME="$new_home"
                export PATH="$JAVA_HOME/bin:$PATH"
                success "JAVA_HOME switched to $new_home"
            else
                error "openjdk-${param_java_use} not installed or not found"
                return 1
            fi
        else
            error "Unsupported Java version: $param_java_use (supported: 21, 17)"
            return 1
        fi
        return
    fi

    if [ -n "$param_api" ] || [ -n "$param_bt" ]; then
        if [ -n "$param_api" ]; then
            local pkg_p="platforms;android-${param_api}"
            info "Installing $pkg_p..."
            ("$SDKMANAGER" "$pkg_p" >/dev/null 2>&1) &
            spin $! "Downloading $pkg_p"
            success "$pkg_p installed"
        fi
        if [ -n "$param_bt" ]; then
            local pkg_b="build-tools;${param_bt}"
            info "Installing $pkg_b..."
            ("$SDKMANAGER" "$pkg_b" >/dev/null 2>&1) &
            spin $! "Downloading $pkg_b"
            success "$pkg_b installed"
        fi
        echo ""
        draw_divider
        success "SDK component installation complete!"
        echo ""
        return
    fi

    printf "\n  %b\n\n" "${BOLD}What component do you want to manage?${RESET}"
    printf "  %b Platforms (API Level)\n" "${CYAN}[1]${RESET}"
    printf "  %b Build-Tools\n" "${CYAN}[2]${RESET}"
    printf "  %b Both Platforms and Build-Tools\n" "${CYAN}[3]${RESET}"
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-3/q]: "
    read -r sdk_choice

    [ "$sdk_choice" = "q" ] && info "Cancelled." && return

    local do_plat=false do_bt=false
    case "$sdk_choice" in
        1) do_plat=true ;;
        2) do_bt=true ;;
        3) do_plat=true; do_bt=true ;;
        *) error "Invalid choice"; return 1 ;;
    esac

    if $do_plat; then
        printf "\n  %b\n\n" "${BOLD}Available Android Platforms:${RESET}"
        local i=1
        for api in "${PLATFORM_LIST[@]}"; do
            local status_tag="${MUTED}[ Not Installed ]${RESET}"
            [ -d "$SDK_DIR/platforms/android-$api" ] && status_tag="${GREEN}[ Installed ]${RESET}"
            printf "  %b platform;android-%-2s %b\n" "${CYAN}[$i]${RESET}" "$api" "$status_tag"
            i=$((i+1))
        done
        printf "  %b Custom API Level\n\n" "${CYAN}[c]${RESET}"
        printf "  Select option [1-%d/c]: " "${#PLATFORM_LIST[@]}"
        read -r p_sel

        local sel_api=""
        if [ "$p_sel" = "c" ]; then
            printf "  Enter API level (e.g. 29): "; read -r sel_api
        elif [[ "$p_sel" =~ ^[0-9]+$ ]] && [ "$p_sel" -ge 1 ] && [ "$p_sel" -le "${#PLATFORM_LIST[@]}" ]; then
            sel_api="${PLATFORM_LIST[$((p_sel-1))]}"
        fi

        if [ -n "$sel_api" ]; then
            local target_pkg="platforms;android-${sel_api}"
            info "Installing $target_pkg..."
            ("$SDKMANAGER" "$target_pkg" >/dev/null 2>&1) &
            spin $! "Downloading $target_pkg"
            success "$target_pkg ready at $SDK_DIR/platforms/android-${sel_api}"
        fi
    fi

    if $do_bt; then
        printf "\n  %b\n\n" "${BOLD}Available Build-Tools Versions:${RESET}"
        local i=1
        for bt in "${BUILDTOOLS_LIST[@]}"; do
            local status_tag="${MUTED}[ Not Installed ]${RESET}"
            [ -d "$SDK_DIR/build-tools/$bt" ] && status_tag="${GREEN}[ Installed ]${RESET}"
            printf "  %b build-tools;%-8s %b\n" "${CYAN}[$i]${RESET}" "$bt" "$status_tag"
            i=$((i+1))
        done
        printf "  %b Custom Version\n\n" "${CYAN}[c]${RESET}"
        printf "  Select option [1-%d/c]: " "${#BUILDTOOLS_LIST[@]}"
        read -r bt_sel

        local sel_bt=""
        if [ "$bt_sel" = "c" ]; then
            printf "  Enter version (e.g. 30.0.3): "; read -r sel_bt
        elif [[ "$bt_sel" =~ ^[0-9]+$ ]] && [ "$bt_sel" -ge 1 ] && [ "$bt_sel" -le "${#BUILDTOOLS_LIST[@]}" ]; then
            sel_bt="${BUILDTOOLS_LIST[$((bt_sel-1))]}"
        fi

        if [ -n "$sel_bt" ]; then
            local target_pkg="build-tools;${sel_bt}"
            info "Installing $target_pkg..."
            ("$SDKMANAGER" "$target_pkg" >/dev/null 2>&1) &
            spin $! "Downloading $target_pkg"
            success "$target_pkg ready at $SDK_DIR/build-tools/${sel_bt}"
        fi
    fi

    echo ""
    draw_divider
    success "SDK component management complete!"
    echo ""
}
