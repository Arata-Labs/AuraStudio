#!/data/data/com.termux/files/usr/bin/env bash

cmd_doctor() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}AuraStudio Doctor — System Diagnostics${RESET}"
    draw_divider

    local issues=0
    local auto_fix=false
    [ "${1:-}" = "--fix" ] && auto_fix=true

    chk() {
        local label="$1" condition="$2" fix_cmd="$3"
        if [ "$condition" = "true" ]; then
            printf "  ${GREEN}✔${RESET} %-30s ${GREEN}[ OK ]${RESET}\n" "$label"
        else
            printf "  ${RED}✖${RESET} %-30s ${RED}[ ERROR ]${RESET}\n" "$label"
            if $auto_fix && [ -n "$fix_cmd" ]; then
                info "Auto-fixing: $fix_cmd"
                eval "$fix_cmd"
            else
                [ -n "$fix_cmd" ] && printf "    %b\n" "${MUTED}└─ Fix: $fix_cmd${RESET}"
            fi
            issues=$((issues + 1))
        fi
    }

    command -v java &>/dev/null && chk "Java OpenJDK 21" "true" "pkg install -y openjdk-21" || chk "Java OpenJDK 21" "false" "pkg install -y openjdk-21"
    command -v gradle &>/dev/null && chk "Gradle Build Tool" "true" "pkg install -y gradle" || chk "Gradle Build Tool" "false" "pkg install -y gradle"
    [ -d "$SDK_DIR" ] && chk "ANDROID_HOME Directory" "true" "aurastudio setup" || chk "ANDROID_HOME Directory" "false" "aurastudio setup"
    [ -f "$SDKMANAGER" ] && chk "Android cmdline-tools" "true" "aurastudio setup" || chk "Android cmdline-tools" "false" "aurastudio setup"

    echo ""
    draw_divider
    if [ "$issues" -eq 0 ]; then
        printf "  %b\n" "${GREEN}${BOLD}✨ All checks passed! AuraStudio environment is ready.${RESET}"
    else
        printf "  %b\n" "${AMBER}${BOLD}▲ Found $issues issue(s) with your setup.${RESET}"
        ! $auto_fix && printf "  %b\n" "${MUTED}Tip: Run 'aurastudio doctor --fix' to auto-resolve issues.${RESET}"
    fi
    echo ""
}
