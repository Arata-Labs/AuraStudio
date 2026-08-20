#!/data/data/com.termux/files/usr/bin/env bash

cmd_install_ndk() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Installing Android NDK + CMake${RESET}"
    draw_divider

    check_storage 1500
    ensure_tools curl unzip tar "findutils:find" sed

    printf "\n  %b\n\n" "${BOLD}Available NDK Versions (HomuHomu833):${RESET}"
    local i=1
    for entry in "${NDK_VERSIONS[@]}"; do
        local name ver
        name=$(echo "$entry" | cut -d'|' -f1)
        ver=$(echo "$entry"  | cut -d'|' -f2)
        local status_tag="${MUTED}[ Not Installed ]${RESET}"
        [ -d "$NDK_DIR/$ver" ] && status_tag="${GREEN}[ Installed ]${RESET}"
        printf "  %b NDK %-12s %b %b\n" "${CYAN}[$i]${RESET}" "$name" "${MUTED}($ver)${RESET}" "$status_tag"
        i=$((i + 1))
    done
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-${#NDK_VERSIONS[@]}/q]: "
    read -r choice

    [ "$choice" = "q" ] && echo "" && info "Cancelled." && return

    if ! [[ "$choice" =~ ^[0-9]+$ ]] || [ "$choice" -lt 1 ] || [ "$choice" -gt "${#NDK_VERSIONS[@]}" ]; then
        error "Invalid selection"; exit 1
    fi

    local selected="${NDK_VERSIONS[$((choice - 1))]}"
    local ndk_name ndk_ver ndk_url
    ndk_name=$(echo "$selected" | cut -d'|' -f1)
    ndk_ver=$(echo "$selected"  | cut -d'|' -f2)
    ndk_url=$(echo "$selected"  | cut -d'|' -f3)

    local ndk_install_dir="$NDK_DIR/$ndk_ver"
    local ndk_file; ndk_file=$(basename "$ndk_url")
    local download_target="$TMPDIR/$ndk_file"

    _do_install_ndk_files() {
        info "Downloading NDK $ndk_name..."
        download_file "$ndk_url" "$download_target"
        [ ! -s "$download_target" ] && error "Download failed!" && exit 1

        (_extract_ndk() {
            cd "$HOME_DIR"
            tar --no-same-owner -xf "$download_target" --warning=no-unknown-keyword
            rm -f "$download_target"
            mkdir -p "$NDK_DIR"
            local extracted_dir
            extracted_dir=$(find "$HOME_DIR" -maxdepth 1 -type d \( -name "android-ndk-*" -o -name "$ndk_ver" \) | head -1)
            if [ -n "$extracted_dir" ]; then
                mv "$extracted_dir" "$ndk_install_dir"
            fi
        }; _extract_ndk) &
        spin $! "Extracting NDK $ndk_name"
    }

    if [ -d "$ndk_install_dir" ]; then
        warn "NDK $ndk_name is already installed at $ndk_install_dir"
        printf "  Force reinstall? [y/N]: "
        read -r force_ndk
        if [[ "$force_ndk" =~ ^[Yy]$ ]]; then
            rm -rf "$ndk_install_dir"
            _do_install_ndk_files
        fi
    else
        _do_install_ndk_files
    fi

    if [ -d "$ndk_install_dir" ]; then
        (_symlink_musl() {
            for path in "toolchains/llvm/prebuilt" "prebuilt" "shader-tools"; do
                if [ -d "$ndk_install_dir/$path" ]; then
                    cd "$ndk_install_dir/$path"
                    [ ! -e "linux-aarch64" ] && ln -s linux-arm64 linux-aarch64 2>/dev/null
                fi
            done
        }; _symlink_musl) &
        spin $! "Creating musl compatibility symlinks"
        success "NDK $ndk_name ready at ${CYAN}$ndk_install_dir${RESET}"
    fi

    echo ""
    printf "  Install CMake now? [y/N]: "
    read -r cmake_choice
    [[ "$cmake_choice" =~ ^[Yy]$ ]] && _do_install_cmake

    write_env
    local shell_rc; shell_rc=$(detect_shell_rc)

    echo ""
    draw_divider
    success "NDK Installation Complete!"
    printf "  Run: %b\n\n" "${CYAN}source $shell_rc${RESET}"
}
