#!/data/data/com.termux/files/usr/bin/env bash

cmd_install_ndk() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Installing Android NDK + CMake${RESET}"
    draw_divider

    if ! check_storage 1500; then
        return 1
    fi
    if ! ensure_tools curl unzip tar "findutils:find" sed; then
        return 1
    fi

    printf "\n  %b\n\n" "${BOLD}Available NDK Versions (HomuHomu833):${RESET}"
    local i=1
    for entry in "${NDK_VERSIONS[@]}"; do
        local name ver
        name=$(parse_entry "$entry" 1)
        ver=$(parse_entry "$entry" 2)
        local status_tag="${MUTED}[ Not Installed ]${RESET}"
        [ -d "$NDK_DIR/$ver" ] && status_tag="${GREEN}[ Installed ]${RESET}"
        printf "  %b NDK %-12s %b %b\n" "${CYAN}[$i]${RESET}" "$name" "${MUTED}($ver)${RESET}" "$status_tag"
        i=$((i + 1))
    done
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-%d/q]: " "${#NDK_VERSIONS[@]}"
    read -r choice

    [ "$choice" = "q" ] && echo "" && info "Cancelled." && return

    if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt "${#NDK_VERSIONS[@]}" ]; then
        error "Invalid selection"
        return 1
    fi

    local selected="${NDK_VERSIONS[$((choice - 1))]}"
    local ndk_name ndk_ver ndk_url
    ndk_name=$(parse_entry "$selected" 1)
    ndk_ver=$(parse_entry "$selected" 2)
    ndk_url=$(parse_entry "$selected" 3)

    local ndk_install_dir="$NDK_DIR/$ndk_ver"
    local ndk_file
    ndk_file=$(basename_fast "$ndk_url")
    local download_target="$TMPDIR/$ndk_file"

    _do_install_ndk_files() {
        info "Downloading NDK $ndk_name..."
        if ! download_file "$ndk_url" "$download_target"; then
            error "NDK download failed!"
            return 1
        fi

        _extract_ndk() {
            cd "$HOME_DIR" || return 1
            tar --no-same-owner -xf "$download_target" --warning=no-unknown-keyword 2>/dev/null || return 1
            rm -f "$download_target"
            mkdir -p "$NDK_DIR"
            local extracted_dir
            extracted_dir=$(find "$HOME_DIR" -maxdepth 1 -type d \( -name "android-ndk-*" -o -name "$ndk_ver" \) 2>/dev/null | head -1)
            if [ -n "$extracted_dir" ]; then
                mv "$extracted_dir" "$ndk_install_dir" || return 1
            else
                return 1
            fi
            return 0
        }
        
        if ! (_extract_ndk); then
            error "NDK extraction failed!"
            return 1
        fi
        
        return 0
    }

    if [ -d "$ndk_install_dir" ]; then
        warn "NDK $ndk_name is already installed at $ndk_install_dir"
        if confirm_action "Force reinstall?"; then
            rm -rf "$ndk_install_dir"
            if ! _do_install_ndk_files; then
                return 1
            fi
        else
            return 0
        fi
    else
        if ! _do_install_ndk_files; then
            return 1
        fi
    fi

    if [ -d "$ndk_install_dir" ]; then
        _symlink_musl() {
            for path in "toolchains/llvm/prebuilt" "prebuilt" "shader-tools"; do
                if [ -d "$ndk_install_dir/$path" ]; then
                    cd "$ndk_install_dir/$path" || return 1
                    [ ! -e "linux-aarch64" ] && ln -s linux-arm64 linux-aarch64 2>/dev/null
                fi
            done
            return 0
        }

        _symlink_gradle_version() {
            local source_props="$ndk_install_dir/source.properties"
            if [ -f "$source_props" ]; then
                local real_version
                real_version=$(grep "^Pkg.Revision" "$source_props" | cut -d'=' -f2 | tr -d ' ')
                if [ -n "$real_version" ] && [ "$real_version" != "$ndk_ver" ]; then
                    local gradle_ndk_dir="$NDK_DIR/$real_version"
                    if [ ! -e "$gradle_ndk_dir" ]; then
                        ln -s "$ndk_install_dir" "$gradle_ndk_dir" 2>/dev/null
                        info "Gradle symlink: $real_version -> $ndk_ver"
                    fi
                fi
            fi
        }

        if ! (_symlink_musl); then
            warn "Failed to create some musl symlinks (non-critical)"
        fi
        _symlink_gradle_version
        success "NDK $ndk_name ready at ${CYAN}$ndk_install_dir${RESET}"
    fi

    echo ""
    if confirm_action "Install CMake now?"; then
        _do_install_cmake
    fi

    write_env

    echo ""
    draw_divider
    success "NDK Installation Complete!"
    local shell_rc
    shell_rc="$(detect_shell_rc)"
    printf "  Run: %b\n\n" "${CYAN}source $shell_rc${RESET}"
}
