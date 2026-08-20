#!/data/data/com.termux/files/usr/bin/env bash
# shellcheck disable=SC2034

CLI_VERSION="1.1"

# ── XDG Base Directory Paths (Termux) ────────────────────────
AURA_CONFIG_DIR="${XDG_CONFIG_HOME:-$HOME/.config}/aurastudio"
AURA_DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/aurastudio"
AURA_CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/aurastudio"
AURA_ENV_FILE="$AURA_CONFIG_DIR/env.sh"

HOME_DIR="$HOME"
SDK_DIR="$HOME/android-sdk"
NDK_DIR="$SDK_DIR/ndk"
CMAKE_DIR="$SDK_DIR/cmake"
CMDTOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
CMDTOOLS_DIR="$SDK_DIR/cmdline-tools/latest"
SDKMANAGER="$CMDTOOLS_DIR/bin/sdkmanager"

TMPDIR="${TMPDIR:-${PREFIX:-/tmp}/tmp}"
mkdir -p "$TMPDIR" "$AURA_CONFIG_DIR" "$AURA_CACHE_DIR"
DEBUG="${DEBUG:-0}"

# ── Repositori NDK HomuHomu833 ──────────────────────────────────
NDK_BASE_URL="https://github.com/HomuHomu833/android-ndk-custom/releases/download"
NDK_VERSIONS=(
    "r30 (beta2)|r30-beta2|$NDK_BASE_URL/r30/android-ndk-r30-beta2-aarch64-linux-musl.tar.xz"
    "r29|r29|$NDK_BASE_URL/r29/android-ndk-r29-aarch64-linux-musl.tar.xz"
    "r28c|r28c|$NDK_BASE_URL/r28/android-ndk-r28c-aarch64-linux-musl.tar.xz"
    "r27d|r27d|$NDK_BASE_URL/r27/android-ndk-r27d-aarch64-linux-musl.tar.xz"
    "r26d|r26d|$NDK_BASE_URL/r26/android-ndk-r26d-aarch64-linux-musl.tar.xz"
)

# ── Repositori CMake ─────────────────────────────────────────────
CMAKE_HOMU_BASE="https://github.com/HomuHomu833/cmake-custom/releases/download"
CMAKE_IKSO_BASE="https://github.com/MrIkso/AndroidIDE-NDK/releases/download/cmake"
CMAKE_VERSIONS=(
    "4.1.2|4.1.2|$CMAKE_HOMU_BASE/4.1.2/cmake-aarch64-linux-musl.tar.xz"
    "4.1.1|4.1.1|$CMAKE_HOMU_BASE/4.1.1/cmake-aarch64-linux-musl.tar.xz"
    "4.1.0|4.1.0|$CMAKE_HOMU_BASE/4.1.0/cmake-aarch64-linux-musl.tar.xz"
    "4.0.3|4.0.3|$CMAKE_HOMU_BASE/4.0.3/cmake-aarch64-linux-musl.tar.xz"
    "4.0.2|4.0.2|$CMAKE_HOMU_BASE/4.0.2/cmake-aarch64-linux-musl.tar.xz"
    "3.25.1|3.25.1|$CMAKE_IKSO_BASE/cmake-3.25.1-android-aarch64.zip"
    "3.22.1|3.22.1|$CMAKE_IKSO_BASE/cmake-3.22.1-android-aarch64.zip"
    "3.18.1|3.18.1|$CMAKE_IKSO_BASE/cmake-3.18.1-android-aarch64.zip"
    "3.10.2|3.10.2|$CMAKE_IKSO_BASE/cmake-3.10.2-android-aarch64.zip"
)

# ── Preset List SDK ─────────────────────────────────────────────
PLATFORM_LIST=("37" "36" "35" "34" "33" "32" "31" "30")
BUILDTOOLS_LIST=("37.0.0" "36.0.0" "35.0.0" "34.0.0" "33.0.2" "32.0.0" "31.0.0" "30.0.3")

# ── Color Palette ────────────────────────────────────────────────
INDIGO='\033[38;2;99;102;241m'
PURPLE='\033[38;2;168;85;247m'
CYAN='\033[38;2;6;182;212m'
GREEN='\033[38;2;34;197;94m'
AMBER='\033[38;2;245;158;11m'
RED='\033[38;2;239;68;68m'
MUTED='\033[38;2;100;116;139m'
WHITE='\033[38;2;248;250;252m'
BOLD='\033[1m'
DIM='\033[2m'
RESET='\033[0m'
