#!/usr/bin/env bash
# ╔══════════════════════════════════════════════════════════════════╗
# ║  AURA STUDIO CLI - Automated Debian (.deb) Package Builder       ║
# ╚══════════════════════════════════════════════════════════════════╝

PACKAGE_NAME="aurastudio"
# Automatically fetch version from config/env.sh
VERSION=$(grep -E '^CLI_VERSION=' config/env.sh | cut -d'"' -f2)
VERSION="${VERSION:-1.1}"
ARCH="all"
BUILD_DIR="dist/deb_build"
OUTPUT_DEB="${PACKAGE_NAME}_${VERSION}_${ARCH}.deb"

PREFIX_PATH="/data/data/com.termux/files/usr"
OPT_TARGET="$BUILD_DIR$PREFIX_PATH/opt/$PACKAGE_NAME"
BIN_TARGET="$BUILD_DIR$PREFIX_PATH/bin"
COMPLETION_TARGET="$BUILD_DIR$PREFIX_PATH/share/bash-completion/completions"

echo "⚡ Building $OUTPUT_DEB..."

# 1. Clean up any remaining builds from previous builds
rm -rf dist
mkdir -p "$BUILD_DIR/DEBIAN"
mkdir -p "$OPT_TARGET"
mkdir -p "$BIN_TARGET"
mkdir -p "$COMPLETION_TARGET"

# 2. Create a DEBIAN/control file
cat > "$BUILD_DIR/DEBIAN/control" << EOF
Package: $PACKAGE_NAME
Version: $VERSION
Architecture: $ARCH
Maintainer: HinohArata <github.com/HinohArata>
Depends: bash, curl, unzip, tar, findutils, sed, grep, gawk
Section: utils
Priority: optional
Description: Next-Gen Termux Android Development Tool Suite
 Automatically configures OpenJDK 21, Gradle, Android SDK, custom NDK,
 CMake, and project starters directly inside Termux.
EOF

# 3. Create maintainer scripts (postinst & prerm)
cat > "$BUILD_DIR/DEBIAN/postinst" << 'POSTINST_EOF'
#!/bin/sh
COMPLETION_FILE="/data/data/com.termux/files/usr/share/bash-completion/completions/aurastudio"

# Detect correct shell RC for Termux
detect_shell_rc() {
    [ -f "$HOME/.zshrc" ] && echo "$HOME/.zshrc" && return
    [ -f "${PREFIX:-}/etc/bash.bashrc" ] && echo "${PREFIX:-}/etc/bash.bashrc" && return
    [ -f /etc/bash.bashrc ] && echo /etc/bash.bashrc && return
    echo /etc/bash.bashrc
}

BASHRC="$(detect_shell_rc)"

echo ""
echo "✨ AuraStudio CLI successfully installed via DEB package!"

# Setup autocompletion if completion file exists
if [ -f "$COMPLETION_FILE" ]; then
    SOURCE_LINE="[ -f \"$COMPLETION_FILE\" ] && source \"$COMPLETION_FILE\""
    if ! grep -qF "aurastudio-completion" "$BASHRC" 2>/dev/null; then
        echo "" >> "$BASHRC"
        echo "# AuraStudio CLI autocompletion" >> "$BASHRC"
        echo "$SOURCE_LINE" >> "$BASHRC"
        echo "✔ Bash autocompletion enabled. Run 'source $BASHRC' to activate."
    else
        echo "✔ Bash autocompletion already configured."
    fi
else
    echo "⚠ Completion script not found. Try reinstalling the package."
fi

echo ""
echo "   Run 'aurastudio setup' to finish configuring your environment."
echo ""
POSTINST_EOF

cat > "$BUILD_DIR/DEBIAN/prerm" << 'PRERM_EOF'
#!/bin/sh
rm -f /data/data/com.termux/files/usr/bin/aurastudio
rm -f /data/data/com.termux/files/usr/share/bash-completion/completions/aurastudio
PRERM_EOF

chmod 755 "$BUILD_DIR/DEBIAN/postinst" "$BUILD_DIR/DEBIAN/prerm"

# 4. Copy the source files to the opt directory
cp -r aurastudio config lib modules "$OPT_TARGET/"
chmod +x "$OPT_TARGET/aurastudio"

# 4b. Install bash completion script
cp lib/aurastudio-completion.bash "$COMPLETION_TARGET/aurastudio"

# 5. Create symlink binaries
ln -sf "$PREFIX_PATH/opt/$PACKAGE_NAME/aurastudio" "$BIN_TARGET/aurastudio"

# 6. Create .deb package and SHA256 checksum
dpkg-deb --build "$BUILD_DIR" "dist/$OUTPUT_DEB"

if [ -f "dist/$OUTPUT_DEB" ]; then
    cd dist || return 1
    sha256sum "$OUTPUT_DEB" > "$OUTPUT_DEB.sha256" && cd ..
    echo "✔ Package built successfully: dist/$OUTPUT_DEB"
    echo "✔ Checksum generated: dist/$OUTPUT_DEB.sha256"
else
    echo "✖ Failed to build package."
    exit 1
fi
