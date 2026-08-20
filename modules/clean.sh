#!/data/data/com.termux/files/usr/bin/env bash

cmd_clean() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Cleaning Temporary Cache & Downloads${RESET}"
    draw_divider
    info "Removing cache files in $TMPDIR..."
    rm -rf "$TMPDIR"/aurastudio_* "$TMPDIR"/*.zip "$TMPDIR"/*.tar.xz 2>/dev/null
    success "Cache and temporary files cleaned successfully!"
    echo ""
}
