#!/data/data/com.termux/files/usr/bin/env bash
# shellcheck disable=SC2312

# ─────────────────────────────────────────────────────────────
# aurastudio use java <17|21>
# Switches the active JDK by re-pointing the $PREFIX/bin/*java*
# symlinks to the target JVM. Instant (no download), no $PATH dupes.
# ─────────────────────────────────────────────────────────────
cmd_use_java() {
    local version="$1"
    if [[ ! "$version" =~ ^(17|21)$ ]]; then
        error "Invalid Java version: '${version:-<empty>}'"
        printf "  %b\n" "${MUTED}Usage: aurastudio use java 17 | aurastudio use java 21${RESET}"
        return 1
    fi

    local bin_prefix="${PREFIX:-/data/data/com.termux/files/usr}/bin"
    local jvm_root="${PREFIX:-/data/data/com.termux/files/usr}/lib/jvm"
    local target_jvm="$jvm_root/java-${version}-openjdk"

    if [ ! -x "$target_jvm/bin/java" ]; then
        error "openjdk-${version} not found at $target_jvm"
        printf "  %b\n" "${MUTED}Install it first: aurastudio install sdk java ${version}${RESET}"
        return 1
    fi

    local cmd
    local switched=0
    for cmd in java javac javadoc jar keytool jshell javap jdb jdeps jlink; do
        local target="$bin_prefix/$cmd"
        local from="$target_jvm/bin/$cmd"
        [ -f "$from" ] || continue

        if [ -L "$target" ]; then
            rm -f "$target"
        elif [ -e "$target" ]; then
            # A static file (e.g. previous pkg install) — back it up
            mv "$target" "$target.bak-$(date +%s)" 2>/dev/null
        fi
        ln -s "$from" "$target"
        chmod +x "$target"
        switched=1
    done

    if [ "$switched" -eq 0 ]; then
        error "No java tools found under $target_jvm/bin"
        return 1
    fi

    # Persist JAVA_HOME in the environment file for new shells
    if [ -f "$AURA_CONFIG_DIR/env.sh" ]; then
        sed -i "s|^export JAVA_HOME=.*|export JAVA_HOME=\"$target_jvm\"|" "$AURA_CONFIG_DIR/env.sh" 2>/dev/null
    fi

    local ver
    ver=$("$target_jvm/bin/java" -version 2>&1 | head -1 | sed 's/.*"\(.*\)".*/\1/')
    success "Java switched to openjdk-${version} ($ver)"
    printf "  %b\n" "${MUTED}JAVA_HOME → $target_jvm${RESET}"
    return 0
}