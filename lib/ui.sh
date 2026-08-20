#!/data/data/com.termux/files/usr/bin/env bash

draw_banner() {
    printf "%b\n" "${PURPLE}┌─────────────────────────────────────────────────────────────┐${RESET}"
    printf "%b\n" "${PURPLE}│${RESET}  ${INDIGO}${BOLD}⚡ AURA STUDIO CLI${RESET} ${MUTED}v${CLI_VERSION}${RESET}  ${MUTED}│${RESET}  ${CYAN}Modern Termux Android Environment${RESET}  ${PURPLE}│${RESET}"
    printf "%b\n" "${PURPLE}└─────────────────────────────────────────────────────────────┘${RESET}"
}

draw_divider() {
    printf "%b\n" "${MUTED}───────────────────────────────────────────────────────────────${RESET}"
}

info()    { printf "  %b\n" "${INDIGO}❯${RESET} $*"; }
success() { printf "  %b\n" "${GREEN}✔${RESET} $*"; }
warn()    { printf "  %b\n" "${AMBER}▲${RESET} $*"; }
error()   { printf "  %b\n" "${RED}✖${RESET} $*"; }
step()    { printf "\n  %b %b\n" "${PURPLE}${BOLD}[$1]${RESET}" "${WHITE}${BOLD}$2${RESET}"; }

spin() {
    local pid=$1
    local msg="$2"
    local delay=0.07
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    tput civis 2>/dev/null
    while ps -p "$pid" > /dev/null 2>&1; do
        local temp=${spinstr#?}
        printf "\r  %b%c%b %b%s%b..." "$CYAN" "$spinstr" "$RESET" "$WHITE" "$msg" "$RESET"
        spinstr=$temp${spinstr%"$temp"}
        sleep $delay
    done
    printf "\r\033[K"
    tput cnorm 2>/dev/null
}

run_animated() {
    local msg="$1"
    shift

    if [ "$DEBUG" -eq 1 ]; then
        info "Running (Debug Mode): $*"
        "$@"
        local res=$?
        [ $res -eq 0 ] && success "$msg" || error "$msg (Failed with code $res)"
        return $res
    fi

    ("$@") > /dev/null 2>&1 &
    local pid=$!
    spin $pid "$msg"
    wait $pid
    local res=$?
    if [ $res -eq 0 ]; then
        success "$msg"
    else
        error "$msg ${RED}(Failed)${RESET}"
        return 1
    fi
}
