#!/data/data/com.termux/files/usr/bin/env bash

cmd_status() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}AuraStudio Environment Status${RESET}"
    draw_divider

    show_row() {
        local category="$1" name="$2" status="$3" detail="$4"
        if [ "$status" = "ok" ]; then
            printf "  %b✔%b %-14s %-16s %b%s%b\n" "$GREEN" "$RESET" "[$category]" "$name" "$CYAN" "$detail" "$RESET"
        else
            printf "  %b✖%b %-14s %-16s %b%s%b\n" "$RED" "$RESET" "[$category]" "$name" "$MUTED" "$detail" "$RESET"
        fi
    }

    printf "  %b\n" "${BOLD}Core System Tools:${RESET}"
    command -v java &>/dev/null && show_row "Tools" "Java" "ok" "$(java -version 2>&1 | head -1)" || show_row "Tools" "Java" "fail" "Not installed"
    command -v gradle &>/dev/null && show_row "Tools" "Gradle" "ok" "$(gradle -v 2>/dev/null | grep 'Gradle ' | head -1)" || show_row "Tools" "Gradle" "fail" "Not installed"
    command -v git &>/dev/null && show_row "Tools" "Git" "ok" "$(git --version)" || show_row "Tools" "Git" "fail" "Not installed"
    command -v aria2c &>/dev/null && show_row "Tools" "aria2c" "ok" "Installed (Accelerated Downloads)" || show_row "Tools" "aria2c" "fail" "Not installed (Optional)"

    printf "\n  %b\n" "${BOLD}Android SDK:${RESET}"
    [ -f "$SDKMANAGER" ] && show_row "SDK" "cmdline-tools" "ok" "Installed" || show_row "SDK" "cmdline-tools" "fail" "Not installed"
    [ -f "$SDK_DIR/platform-tools/adb" ] && show_row "SDK" "platform-tools" "ok" "Installed" || show_row "SDK" "platform-tools" "fail" "Not installed"
    
    if ls "$SDK_DIR/platforms"/android-* &>/dev/null 2>&1; then
        for p in "$SDK_DIR/platforms"/android-*/; do
            show_row "SDK Platform" "$(basename "$p")" "ok" "Ready"
        done
    else
        show_row "SDK Platform" "None" "fail" "Not installed"
    fi

    if ls "$SDK_DIR/build-tools"/[0-9]* &>/dev/null 2>&1; then
        for b in "$SDK_DIR/build-tools"/*/; do
            show_row "Build-Tools" "$(basename "$b")" "ok" "Ready"
        done
    else
        show_row "Build-Tools" "None" "fail" "Not installed"
    fi

    printf "\n  %b\n" "${BOLD}NDK & CMake:${RESET}"
    local ndk_found=0
    if [ -d "$NDK_DIR" ]; then
        for d in "$NDK_DIR"/*/; do
            [ -d "$d" ] && show_row "NDK" "$(basename "$d")" "ok" "Native Toolchain" && ndk_found=1
        done
    fi
    [ "$ndk_found" -eq 0 ] && show_row "NDK" "None" "fail" "Not installed"

    local cmake_found=0
    if [ -d "$CMAKE_DIR" ]; then
        for d in "$CMAKE_DIR"/*/; do
            [ -d "$d" ] && show_row "CMake" "$(basename "$d")" "ok" "Native Build" && cmake_found=1
        done
    fi
    [ "$cmake_found" -eq 0 ] && show_row "CMake" "None" "fail" "Not installed"

    # Health Score
    local health_score
    health_score=$(calculate_health_score)
    local health_color="$RED"
    [ "$health_score" -ge 50 ] && health_color="$AMBER"
    [ "$health_score" -ge 80 ] && health_color="$GREEN"
    
    printf "\n  %b\n" "${BOLD}Environment Health:${RESET}"
    printf "  %b%s%%%b\n" "$health_color" "$health_score" "$RESET"
    
    if [ "$health_score" -ge 80 ]; then
        printf "  %b\n" "${GREEN}Excellent - Your environment is ready for development${RESET}"
    elif [ "$health_score" -ge 50 ]; then
        printf "  %b\n" "${AMBER}Good - Some components could be improved${RESET}"
    else
        printf "  %b\n" "${RED}Needs attention - Run 'aurastudio setup' to fix${RESET}"
    fi

    echo ""
    draw_divider
    echo ""
}
