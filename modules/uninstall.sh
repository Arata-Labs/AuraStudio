#!/data/data/com.termux/files/usr/bin/env bash

cmd_uninstall() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Uninstalling AuraStudio CLI${RESET}"
    draw_divider

    warn "This action will completely remove AuraStudio CLI from your Termux system."
    printf "\n  Are you sure you want to proceed with uninstallation? [y/N]: "
    read -r confirm
    if ! [[ "$confirm" =~ ^[Yy]$ ]]; then
        info "Uninstallation cancelled."
        echo ""
        return 0
    fi

    echo ""
    printf "  Do you also want to remove Android SDK & NDK (~/android-sdk)? [y/N]: "
    read -r remove_sdk

    step "1/3" "Removing Binary Symlink & Environment Configuration..."
    rm -f "$PREFIX/bin/aurastudio"
    rm -f "$HOME_DIR/.aurastudiorc"
    rm -rf "$AURA_CONFIG_DIR"
    success "Symlink and config directory successfully removed."

    step "2/3" "Cleaning Shell Config Integration..."
    local shell_rc
    shell_rc="$(detect_shell_rc)"
    if [ -f "$shell_rc" ]; then
        sed -i '/aurastudio/d' "$shell_rc" 2>/dev/null
        success "Shell configuration references cleaned from ${CYAN}$shell_rc${RESET}"
    fi

    if [[ "$remove_sdk" =~ ^[Yy]$ ]]; then
        step "3/3" "Removing Android SDK & NDK Directory ($SDK_DIR)..."
        rm -rf "$SDK_DIR"
        success "Android SDK directory completely removed."
    else
        step "3/3" "Skipping Android SDK directory removal."
        info "Android SDK directory preserved at: ${CYAN}$SDK_DIR${RESET}"
    fi

    echo ""
    draw_divider
    success "AuraStudio CLI successfully uninstalled from Termux!"
    draw_divider
    echo ""

    rm -rf "$AURA_ROOT"
    exit 0
}
