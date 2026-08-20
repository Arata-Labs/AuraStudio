#!/data/data/com.termux/files/usr/bin/env bash

check_java() { 
    if ! command -v java &>/dev/null; then
        error "Java not found — Run: pkg install openjdk-21"
        return 1
    fi
    return 0
}

ensure_tools() {
    local entry pkg_name cmd_name failed=()
    for entry in "$@"; do
        if [[ "$entry" == *:* ]]; then
            pkg_name="${entry%%:*}"; cmd_name="${entry##*:}"
        else
            pkg_name="$entry"; cmd_name="$entry"
        fi
        if command -v "$cmd_name" &>/dev/null; then
            success "Tool ${CYAN}$pkg_name${RESET} is ready"
        else
            run_animated "Installing $pkg_name" pkg install -y "$pkg_name"
            if ! command -v "$cmd_name" &>/dev/null; then
                failed+=("$pkg_name")
            fi
        fi
    done
    if [ "${#failed[@]}" -gt 0 ]; then
        error "Auto-install failed for: ${failed[*]}"
        printf "  %b\n" "${MUTED}→ Try manually: pkg install ${failed[*]}${RESET}"
        return 1
    fi
    return 0
}
