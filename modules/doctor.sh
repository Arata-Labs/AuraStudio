#!/data/data/com.termux/files/usr/bin/env bash

cmd_doctor() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}AuraStudio Doctor — System Diagnostics${RESET}"
    draw_divider

    local issues=0
    local auto_fix=false
    local save_snapshot=false
    [ "${1:-}" = "--fix" ] && auto_fix=true
    [ "${1:-}" = "--snapshot" ] || [ "${2:-}" = "--snapshot" ] && save_snapshot=true

    chk() {
        local label="$1" condition="$2" fix_cmd="$3"
        if [ "$condition" = "true" ]; then
            printf "  %b✔%b %-30s %b[ OK ]%b\n" "$GREEN" "$RESET" "$label" "$GREEN" "$RESET"
        else
            printf "  %b✖%b %-30s %b[ ERROR ]%b\n" "$RED" "$RESET" "$label" "$RED" "$RESET"
            if $auto_fix && [ -n "$fix_cmd" ]; then
                _safe_fix "$fix_cmd"
            else
                [ -n "$fix_cmd" ] && printf "    %b\n" "${MUTED}└─ Fix: $fix_cmd${RESET}"
            fi
            issues=$((issues + 1))
        fi
    }

    _safe_fix() {
        local fix_cmd="$1"
        local cmd_base="${fix_cmd%% *}"
        local cmd_args="${fix_cmd#* }"
        case "$cmd_base" in
            pkg)
                info "Auto-fixing: $fix_cmd"
                run_animated "Installing package" pkg install -y $cmd_args
                ;;
            aurastudio)
                local subcmd="${cmd_args%% *}"
                case "$subcmd" in
                    setup) info "Running: aurastudio setup"; cmd_setup ;;
                    *) warn "Auto-fix not supported for: aurastudio $subcmd" ;;
                esac
                ;;
            *)
                warn "Auto-fix not supported for: $fix_cmd"
                ;;
        esac
    }

    command -v java &>/dev/null && chk "Java OpenJDK 21" "true" "pkg install -y openjdk-21" || chk "Java OpenJDK 21" "false" "pkg install -y openjdk-21"
    command -v gradle &>/dev/null && chk "Gradle Build Tool" "true" "pkg install -y gradle" || chk "Gradle Build Tool" "false" "pkg install -y gradle"
    [ -d "$SDK_DIR" ] && chk "ANDROID_HOME Directory" "true" "aurastudio setup" || chk "ANDROID_HOME Directory" "false" "aurastudio setup"
    [ -f "$SDKMANAGER" ] && chk "Android cmdline-tools" "true" "aurastudio setup" || chk "Android cmdline-tools" "false" "aurastudio setup"

    echo ""
    draw_divider
    if [ "$issues" -eq 0 ]; then
        printf "  %b\n" "${GREEN}${BOLD}All checks passed! AuraStudio environment is ready.${RESET}"
    else
        printf "  %b\n" "${AMBER}${BOLD}Found $issues issue(s) with your setup.${RESET}"
        ! $auto_fix && printf "  %b\n" "${MUTED}Tip: Run 'aurastudio doctor --fix' to auto-resolve issues.${RESET}"
    fi
    
    # Health Score
    local health_score
    health_score=$(calculate_health_score)
    local health_color="$RED"
    [ "$health_score" -ge 50 ] && health_color="$AMBER"
    [ "$health_score" -ge 80 ] && health_color="$GREEN"
    printf "\n  %b%sHealth Score: %s%%%b\n" "$health_color" "$BOLD" "$health_score" "$RESET"
    
    # Save snapshot if requested
    if $save_snapshot; then
        local snapshot_path
        snapshot_path=$(save_env_snapshot)
        echo ""
        info "Environment snapshot saved: $snapshot_path"
    fi
    
    echo ""
}
