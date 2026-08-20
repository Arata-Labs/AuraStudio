#!/data/data/com.termux/files/usr/bin/env bash

cmd_update() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Checking & Updating AuraStudio CLI${RESET}"
    draw_divider

    # Checking installation via DEB package
    if [ -d "${PREFIX:-}/opt/aurastudio" ] && [ ! -d "$AURA_ROOT/.git" ]; then
        info "AuraStudio is currently installed via DEB package."
        info "To update to the latest version:"
        info "  1. Download the latest .deb file from GitHub Releases:"
        info "     ${CYAN}https://github.com/Arata-Labs/AuraStudio/releases/latest${RESET}"
        info "  2. Install the downloaded package using:"
        info "     ${CYAN}pkg install ./aurastudio_<version>_all.deb${RESET}"
        echo ""
        return 0
    fi

    if [ ! -d "$AURA_ROOT/.git" ]; then
        error "AuraStudio was not installed via Git repository!"
        info "Please reinstall using the official installer script."
        echo ""
        return 1
    fi

    info "Checking for updates from GitHub..."
    cd "$AURA_ROOT" || return 1
    
    (_fetch_git() { git fetch origin >/dev/null 2>&1; }; _fetch_git) &
    spin $! "Connecting to GitHub repository"

    local local_hash remote_hash
    local_hash=$(git rev-parse HEAD 2>/dev/null)
    remote_hash=$(git rev-parse origin/main 2>/dev/null || git rev-parse origin/master 2>/dev/null)

    if [ "$local_hash" = "$remote_hash" ]; then
        success "AuraStudio CLI is already up to date! (v$CLI_VERSION)"
        echo ""
        return 0
    fi

    if [ "${1:-}" = "--check" ] || [ "${1:-}" = "check" ]; then
        warn "A new version of AuraStudio CLI is available!"
        info "Run '${CYAN}aurastudio update${RESET}' to update the script."
        echo ""
        return 0
    fi

    info "New version found. Updating source code..."
    (_do_pull() {
        git reset --hard origin/main 2>/dev/null || git reset --hard origin/master 2>/dev/null
        chmod +x "$AURA_ROOT/aurastudio"
    }; _do_pull) &
    spin $! "Downloading latest updates"

    success "AuraStudio CLI successfully updated to the latest version!"
    info "Reload your shell environment or run: ${CYAN}source ~/.aurastudiorc${RESET}"
    echo ""
}
