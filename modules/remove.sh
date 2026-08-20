#!/data/data/com.termux/files/usr/bin/env bash

cmd_remove() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Remove Installed Packages${RESET}"
    draw_divider

    local type="${1:-}"
    local name="${2:-}"
    local extra="${3:-}"

    # 1. Direct Target Deletion
    if [ "$type" = "ndk" ] && [ -n "$name" ]; then
        if [ -d "$NDK_DIR/$name" ]; then
            rm -rf "$NDK_DIR/$name"
            success "NDK $name successfully removed."
        else
            error "NDK $name not found at $NDK_DIR/$name"
        fi
        return
    elif [ "$type" = "cmake" ] && [ -n "$name" ]; then
        if [ -d "$CMAKE_DIR/$name" ]; then
            rm -rf "$CMAKE_DIR/$name"
            success "CMake $name successfully removed."
        else
            error "CMake $name not found at $CMAKE_DIR/$name"
        fi
        return
    elif [ "$type" = "sdk" ]; then
        if [ "$name" = "platform" ] && [ -n "$extra" ]; then
            if [ -d "$SDK_DIR/platforms/android-$extra" ]; then
                rm -rf "$SDK_DIR/platforms/android-$extra"
                success "SDK Platform android-$extra successfully removed."
            else
                error "SDK Platform android-$extra not found."
            fi
            return
        elif [ "$name" = "buildtools" ] && [ -n "$extra" ]; then
            if [ -d "$SDK_DIR/build-tools/$extra" ]; then
                rm -rf "$SDK_DIR/build-tools/$extra"
                success "SDK Build-Tools $extra successfully removed."
            else
                error "SDK Build-Tools $extra not found."
            fi
            return
        fi
    fi

    # 2. Interactive Menu Selection
    local installed_items=()

    if ls "$SDK_DIR/platforms"/android-* &>/dev/null 2>&1; then
        for p in "$SDK_DIR/platforms"/android-*/; do
            [ -d "$p" ] && installed_items+=("SDK Platform|$(basename "$p")|$p")
        done
    fi

    if ls "$SDK_DIR/build-tools"/[0-9]* &>/dev/null 2>&1; then
        for b in "$SDK_DIR/build-tools"/*/; do
            [ -d "$b" ] && installed_items+=("Build-Tools|$(basename "$b")|$b")
        done
    fi

    if [ -d "$NDK_DIR" ]; then
        for d in "$NDK_DIR"/*/; do
            [ -d "$d" ] && installed_items+=("NDK|$(basename "$d")|$d")
        done
    fi

    if [ -d "$CMAKE_DIR" ]; then
        for d in "$CMAKE_DIR"/*/; do
            [ -d "$d" ] && installed_items+=("CMake|$(basename "$d")|$d")
        done
    fi

    if [ "${#installed_items[@]}" -eq 0 ]; then
        warn "No installed components found to remove."
        echo ""
        return
    fi

    printf "\n  %b\n\n" "${BOLD}Select package to remove:${RESET}"
    local idx=1
    for item in "${installed_items[@]}"; do
        local cat name
        cat=$(parse_entry "$item" 1)
        name=$(parse_entry "$item" 2)
        printf "  %b %-16s %s\n" "${CYAN}[$idx]${RESET}" "[$cat]" "$name"
        idx=$((idx+1))
    done
    printf "  %b Cancel\n\n" "${RED}[q]${RESET}"
    printf "  Select option [1-%d/q]: " "${#installed_items[@]}"
    read -r r_sel

    [ "$r_sel" = "q" ] && info "Cancelled." && return

    if [[ "$r_sel" =~ ^[0-9]+$ ]] && [ "$r_sel" -ge 1 ] && [ "$r_sel" -le "${#installed_items[@]}" ]; then
        local selected="${installed_items[$((r_sel-1))]}"
        local r_cat r_name r_path
        r_cat=$(parse_entry "$selected" 1)
        r_name=$(parse_entry "$selected" 2)
        r_path=$(parse_entry "$selected" 3)

        printf "  Are you sure you want to remove %s %s? [y/N]: " "$r_cat" "$r_name"
        read -r confirm
        if [[ "$confirm" =~ ^[Yy]$ ]]; then
            rm -rf "$r_path"
            success "$r_cat $r_name successfully removed!"
        else
            info "Cancelled."
        fi
    else
        error "Invalid selection."
    fi
    echo ""
}
