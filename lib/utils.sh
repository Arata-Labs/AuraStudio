#!/data/data/com.termux/files/usr/bin/env bash

cleanup() {
    rm -rf "$TMPDIR"/aurastudio_* 2>/dev/null
    tput cnorm 2>/dev/null
}
trap 'cleanup; exit' SIGINT SIGTERM EXIT

check_storage() {
    local req_mb="$1"
    local free_mb
    free_mb=$(df -m "$HOME_DIR" 2>/dev/null | awk 'NR==2 {print $4}')
    if [ -n "$free_mb" ] && [ "$free_mb" -lt "$req_mb" ]; then
        error "Ruang penyimpanan tidak cukup! Membutuhkan ${req_mb}MB, tersisa ${free_mb}MB."
        exit 1
    fi
}

download_file() {
    local url="$1"
    local out="$2"
    local out_dir; out_dir=$(dirname "$out")
    local out_name; out_name=$(basename "$out")

    mkdir -p "$out_dir"
    if command -v aria2c &>/dev/null; then
        aria2c -x16 -s16 -k1M --continue=true --dir="$out_dir" --out="$out_name" "$url"
    else
        curl -L -C - --retry 3 --retry-connrefused --connect-timeout 10 --progress-bar "$url" -o "$out"
    fi
}

detect_shell_rc() {
    [ -f "$HOME_DIR/.zshrc" ] && echo "$HOME_DIR/.zshrc" || echo "$PREFIX/etc/bash.bashrc"
}

write_env() {
    local env_file="$HOME_DIR/.aurastudiorc"
    cat > "$env_file" << 'ENV'
# ── AuraStudio Environment Configuration ──────────────────
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

if command -v java &>/dev/null; then
    export JAVA_HOME="$(dirname $(dirname $(readlink -f $(which java))))"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

for ver in $(ls -r $ANDROID_HOME/build-tools/ 2>/dev/null); do
    export PATH="$ANDROID_HOME/build-tools/$ver:$PATH"
done
# ── End AuraStudio ─────────────────────────────────────────
ENV

    # Auto-Completion Script Injection if available
    local comp_script_web="$HOME_DIR/.aurastudio/lib/aurastudio-completion.bash"
    local comp_script_deb="$PREFIX/opt/aurastudio/lib/aurastudio-completion.bash"
    
    if [ -f "$comp_script_web" ]; then
        echo "[ -f \"$comp_script_web\" ] && source \"$comp_script_web\"" >> "$env_file"
    elif [ -f "$comp_script_deb" ]; then
        echo "[ -f \"$comp_script_deb\" ] && source \"$comp_script_deb\"" >> "$env_file"
    fi

    # Inject sourcing .aurastudiorc into main rc (bash.bashrc / .zshrc)
    local shell_rc
    shell_rc=$(detect_shell_rc)
    if ! grep -q "\.aurastudiorc" "$shell_rc" 2>/dev/null; then
        echo -e "\n[ -f \"\$HOME/.aurastudiorc\" ] && source \"\$HOME/.aurastudiorc\"" >> "$shell_rc"
    fi
}

