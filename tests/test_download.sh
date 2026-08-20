#!/data/data/com.termux/files/usr/bin/env bash
# Test download functions

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

# Load project files
source "$PROJECT_DIR/config/env.sh"
source "$PROJECT_DIR/lib/utils.sh"

echo ""
printf "${CYAN}╔══════════════════════════════════════╗${RESET}\n"
printf "${CYAN}║     Download Functions Tests         ║${RESET}\n"
printf "${CYAN}╚══════════════════════════════════════╝${RESET}\n"
echo ""

# Test: download_file with invalid URL
printf "${CYAN}Testing download_file error handling...${RESET}\n"
test_out="$TMPDIR/aurastudio_test_download_$$.tmp"
download_file "https://invalid.invalid.invalid/file.zip" "$test_out" 2>/dev/null && \
    assert_eq "download_file returns error for invalid URL" "1" "0" || \
    assert_eq "download_file returns error for invalid URL" "1" "1"
rm -f "$test_out"

# Test: verify_download
printf "\n${CYAN}Testing verify_download...${RESET}\n"
test_file="$TMPDIR/aurastudio_verify_test_$$.tmp"
echo "test" > "$test_file"
assert_true "verify_download accepts valid file" verify_download "$test_file"
rm -f "$test_file"
assert_true "verify_download rejects empty file" test ! -s "$test_file"

# Test: cache avoidance
printf "\n${CYAN}Testing cache avoidance...${RESET}\n"
cached_file="$TMPDIR/aurastudio_cached_$$.tmp"
echo "cached content" > "$cached_file"
assert_true "download_file uses existing cache" [ -f "$cached_file" ]
rm -f "$cached_file"

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
