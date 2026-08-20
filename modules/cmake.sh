#!/data/data/com.termux/files/usr/bin/env bash

_do_install_cmake() {
    check_storage 200
    printf "\n  %b\n\n" "${BOLD}Available CMake Versions:${RESET}"
    local i=1
    for entry in "${CMAKE_VERSIONS[@]}"; do
        local ver
        ver=$(echo "$entry" | cut -d'|' -f2)
        local status_tag="${MUTED}[ Not Installed ]${RESET}"
        [ -d "$CMAKE_DIR/$ver" ] && status_tag="${GREEN}[ Installed ]${RESET}"
        printf "  %b CMake %-10s %b\n" "${CYAN}[$i]${RESET}" "$ver" "$status_tag"
        i=$((i + 1))
    done
    printf "  %b Install All\n" "${CYAN}[a]${RESET}"
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-%d/a/q]: " "${#CMAKE_VERSIONS[@]}"
    read -r cmake_sel

    [ "$cmake_sel" = "q" ] && return
    ensure_tools curl unzip tar sed
    mkdir -p "$CMAKE_DIR"

    if [ "$cmake_sel" = "a" ]; then
        for entry in "${CMAKE_VERSIONS[@]}"; do
            _download_cmake "$entry"
        done
    elif [[ "$cmake_sel" =~ ^[0-9]+$ ]] && [ "$cmake_sel" -ge 1 ] && [ "$cmake_sel" -le "${#CMAKE_VERSIONS[@]}" ]; then
        _download_cmake "${CMAKE_VERSIONS[$((cmake_sel - 1))]}"
    fi
}

_download_cmake() {
    local entry="$1"
    local cmake_name cmake_ver cmake_url
    cmake_name=$(echo "$entry" | cut -d'|' -f1)
    cmake_ver=$(echo "$entry"  | cut -d'|' -f2)
    cmake_url=$(echo "$entry"  | cut -d'|' -f3)

    local target_dir="$CMAKE_DIR/$cmake_ver"
    if [ -d "$target_dir" ]; then
        warn "CMake $cmake_name is already installed"
        return
    fi
    local cmake_file; cmake_file=$(basename "$cmake_url")
    local download_target="$TMPDIR/$cmake_file"

    info "Downloading CMake $cmake_name..."
    download_file "$cmake_url" "$download_target"
    [ ! -s "$download_target" ] && error "Download failed!" && return
    
    (_ex_cmake() {
        mkdir -p "$target_dir"
        if [[ "$cmake_file" == *.tar.xz ]]; then
            tar -xf "$download_target" -C "$target_dir" --strip-components=1 2>/dev/null || tar -xf "$download_target" -C "$target_dir"
        elif [[ "$cmake_file" == *.zip ]]; then
            unzip -qq "$download_target" -d "$CMAKE_DIR"
        fi
        rm -f "$download_target"
        chmod -R +x "$target_dir/bin" 2>/dev/null
    }; _ex_cmake) &
    spin $! "Extracting CMake $cmake_name"
    success "CMake $cmake_name installed"
}

cmd_install_cmake() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Installing Native SDK CMake${RESET}"
    draw_divider
    _do_install_cmake
    write_env
    local shell_rc; shell_rc=$(detect_shell_rc)
    echo ""
    draw_divider
    success "CMake Installation Complete!"
    printf "  Run: %b\n\n" "${CYAN}source $shell_rc${RESET}"
}
