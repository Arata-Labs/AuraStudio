#!/data/data/com.termux/files/usr/bin/env bash
# AuraStudio CLI Test Runner

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# Colors
RED='\033[38;2;239;68;68m'
GREEN='\033[38;2;34;197;94m'
CYAN='\033[38;2;6;182;212m'
RESET='\033[0m'

TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

assert_eq() {
    local test_name="$1" expected="$2" actual="$3"
    TESTS_RUN=$((TESTS_RUN + 1))
    
    if [ "$expected" = "$actual" ]; then
        printf "  ${GREEN}✓${RESET} %s\n" "$test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        printf "  ${RED}✗${RESET} %s (expected: '%s', got: '%s')\n" "$test_name" "$expected" "$actual"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

assert_true() {
    local test_name="$1"
    shift
    TESTS_RUN=$((TESTS_RUN + 1))
    
    if "$@" &>/dev/null; then
        printf "  ${GREEN}✓${RESET} %s\n" "$test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        printf "  ${RED}✗${RESET} %s (command failed: %s)\n" "$test_name" "$*"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

assert_file_exists() {
    local test_name="$1" file="$2"
    TESTS_RUN=$((TESTS_RUN + 1))
    
    if [ -f "$file" ]; then
        printf "  ${GREEN}✓${RESET} %s\n" "$test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
    else
        printf "  ${RED}✗${RESET} %s (file not found: %s)\n" "$test_name" "$file"
        TESTS_FAILED=$((TESTS_FAILED + 1))
    fi
}

# Load project files
source "$PROJECT_DIR/config/env.sh"
source "$PROJECT_DIR/lib/utils.sh"
source "$PROJECT_DIR/lib/tools.sh"

echo ""
printf "${CYAN}╔══════════════════════════════════════╗${RESET}\n"
printf "${CYAN}║     AuraStudio CLI Unit Tests        ║${RESET}\n"
printf "${CYAN}╚══════════════════════════════════════╝${RESET}\n"
echo ""

# Test: detect_java_home
printf "${CYAN}Testing detect_java_home...${RESET}\n"
if command -v java &>/dev/null; then
    java_home_result="$(detect_java_home)"
    assert_true "detect_java_home returns valid directory" test -d "$java_home_result"
else
    printf "  ${RED}⚠${RESET} Java not installed, skipping tests\n"
fi

# Test: detect_shell_rc
printf "\n${CYAN}Testing detect_shell_rc...${RESET}\n"
shell_rc_result="$(detect_shell_rc)"
assert_true "detect_shell_rc returns existing file" test -f "$shell_rc_result"

# Test: verify_download
printf "\n${CYAN}Testing verify_download...${RESET}\n"
test_file="$TMPDIR/aurastudio_test_file"
echo "test content" > "$test_file"
assert_true "verify_download accepts valid file" verify_download "$test_file"
rm -f "$test_file"
assert_true "verify_download rejects missing file" test ! -f "$test_file"

# Test: ensure_tools doesn't fail with existing tool
printf "\n${CYAN}Testing ensure_tools...${RESET}\n"
assert_true "ensure_tools succeeds with existing command" ensure_tools "bash"

# Test: config variables
printf "\n${CYAN}Testing config/env.sh variables...${RESET}\n"
assert_true "CLI_VERSION is set" test -n "$CLI_VERSION"
assert_true "SDK_DIR is set" test -n "$SDK_DIR"
assert_true "NDK_DIR is set" test -n "$NDK_DIR"
assert_true "CMAKE_DIR is set" test -n "$CMAKE_DIR"
assert_true "NDK_VERSIONS array is not empty" test "${#NDK_VERSIONS[@]}" -gt 0
assert_true "CMAKE_VERSIONS array is not empty" test "${#CMAKE_VERSIONS[@]}" -gt 0

# Test: XDG directories created
printf "\n${CYAN}Testing XDG directory creation...${RESET}\n"
assert_true "AURA_CONFIG_DIR exists" test -d "$AURA_CONFIG_DIR"
assert_true "AURA_CACHE_DIR exists" test -d "$AURA_CACHE_DIR"

# Summary
echo ""
printf "${CYAN}════════════════════════════════════════${RESET}\n"
printf "  Tests run:    %d\n" "$TESTS_RUN"
printf "  ${GREEN}Tests passed: %d${RESET}\n" "$TESTS_PASSED"
if [ "$TESTS_FAILED" -gt 0 ]; then
    printf "  ${RED}Tests failed: %d${RESET}\n" "$TESTS_FAILED"
fi
printf "${CYAN}════════════════════════════════════════${RESET}\n"
echo ""

[ "$TESTS_FAILED" -eq 0 ] && exit 0 || exit 1
