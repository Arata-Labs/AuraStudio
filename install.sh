#!/data/data/com.termux/files/usr/bin/env bash
# ╔══════════════════════════════════════════════════════════════════╗
# ║  AURA STUDIO CLI - Official Web Installer for Termux            ║
# ╚══════════════════════════════════════════════════════════════════╝

REPO_URL="https://github.com/Arata-Labs/AuraStudio.git"
INSTALL_DIR="$HOME/.aurastudio"
BIN_DIR="$PREFIX/bin"
TMPDIR="${TMPDIR:-$PREFIX/tmp}"

INDIGO='\033[38;2;99;102;241m'
PURPLE='\033[38;2;168;85;247m'
CYAN='\033[38;2;6;182;212m'
GREEN='\033[38;2;34;197;94m'
AMBER='\033[38;2;245;158;11m'
RED='\033[38;2;239;68;68m'
MUTED='\033[38;2;100;116;139m'
WHITE='\033[38;2;248;250;252m'
BOLD='\033[1m'
RESET='\033[0m'

cleanup() { tput cnorm 2>/dev/null; }
trap 'cleanup; exit' SIGINT SIGTERM EXIT

draw_banner() {
    printf "%b\n" "${PURPLE}┌─────────────────────────────────────────────────────────────┐${RESET}"
    printf "%b\n" "${PURPLE}│${RESET}  ${INDIGO}${BOLD}⚡ AURA STUDIO CLI${RESET}                  ${CYAN}Automated Web Installer${RESET}  ${PURPLE}│${RESET}"
    printf "%b\n" "${PURPLE}└─────────────────────────────────────────────────────────────┘${RESET}"
}

draw_divider() { printf "%b\n" "${MUTED}───────────────────────────────────────────────────────────────${RESET}"; }
info()    { printf "  %b\n" "${INDIGO}❯${RESET} $*"; }
success() { printf "  %b\n" "${GREEN}✔${RESET} $*"; }
warn()    { printf "  %b\n" "${AMBER}▲${RESET} $*"; }
error()   { printf "  %b\n" "${RED}✖${RESET} $*"; }
step()    { printf "\n  %b %b\n" "${PURPLE}${BOLD}[$1]${RESET}" "${WHITE}${BOLD}$2${RESET}"; }

spin() {
    local pid=$1 msg="$2" delay=0.07 spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    tput civis 2>/dev/null
    while ps -p "$pid" > /dev/null 2>&1; do
        local temp=${spinstr#?}
        printf "\r  ${CYAN}%c${RESET} ${WHITE}%s${RESET}..." "$spinstr" "$msg"
        spinstr=$temp${spinstr%"$temp"}
        sleep $delay
    done
    printf "\r\033[K"
    tput cnorm 2>/dev/null
}

run_animated() {
    local msg="$1"
    shift
    ("$@") > /dev/null 2>&1 &
    local pid=$!
    spin $pid "$msg"
    wait $pid
    local res=$?
    if [ $res -eq 0 ]; then success "$msg"; else error "$msg ${RED}(Failed)${RESET}"; return 1; fi
}

main() {
    clear
    draw_banner
    printf "\n  %b\n" "${BOLD}${WHITE}Installing AuraStudio Environment to Termux${RESET}"
    draw_divider

    step "1/4" "Checking & Installing Required Core Tools..."
    local required_pkgs=("git" "curl" "tar" "unzip")
    local missing_pkgs=()

    for pkg in "${required_pkgs[@]}"; do
        command -v "$pkg" &>/dev/null || missing_pkgs+=("$pkg")
    done

    if [ "${#missing_pkgs[@]}" -gt 0 ]; then
        info "Installing missing dependencies: ${CYAN}${missing_pkgs[*]}${RESET}"
        run_animated "Updating package database" pkg update -y
        run_animated "Installing dependencies (${missing_pkgs[*]})" pkg install -y "${missing_pkgs[@]}"
    else
        success "All core bootstrap tools are ready (${CYAN}git, curl, tar, unzip${RESET})"
    fi

    step "2/4" "Preparing Environment Directory..."
    if [ -d "$INSTALL_DIR" ]; then
        warn "Existing installation found at $INSTALL_DIR. Cleaning up..."
        rm -rf "$INSTALL_DIR"
    fi
    mkdir -p "$INSTALL_DIR" "$BIN_DIR"
    success "Directories prepared at ${CYAN}$INSTALL_DIR${RESET}"

    step "3/4" "Fetching Source Code from GitHub..."
    (_clone_repo() { git clone --depth=1 "$REPO_URL" "$INSTALL_DIR"; }; _clone_repo) &
    spin $! "Cloning repository: $REPO_URL"

    if [ ! -f "$INSTALL_DIR/aurastudio" ]; then
        error "Failed to fetch AuraStudio source code!"
        printf "  %b\n" "${MUTED}Check repository URL or network connection.${RESET}"
        exit 1
    fi
    success "Repository cloned successfully"

    step "4/4" "Configuring Binary Symlink..."
    chmod +x "$INSTALL_DIR/aurastudio"
    ln -sf "$INSTALL_DIR/aurastudio" "$BIN_DIR/aurastudio"
    success "Created symlink: ${CYAN}$BIN_DIR/aurastudio${RESET} -> ${CYAN}$INSTALL_DIR/aurastudio${RESET}"

    echo ""
    draw_divider
    printf "  %b\n" "${GREEN}${BOLD}✨ Installation Successful!${RESET}"
    draw_divider
    printf "  %-18s %s\n" "Installed Path:" "$INSTALL_DIR"
    printf "  %-18s %s\n" "Executable Link:" "$BIN_DIR/aurastudio"
    echo ""
    printf "  Get started by running: %b\n\n" "${CYAN}${BOLD}aurastudio setup${RESET}"
}

main "$@"
