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

echo "⚡ Building $OUTPUT_DEB..."

# 1. Clean up any remaining builds from previous builds
rm -rf dist
mkdir -p "$BUILD_DIR/DEBIAN"
mkdir -p "$OPT_TARGET"
mkdir -p "$BIN_TARGET"

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
cat > "$BUILD_DIR/DEBIAN/postinst" << 'EOF'
#!/bin/sh
echo ""
echo "✨ AuraStudio CLI successfully installed via DEB package!"
echo "   Run 'aurastudio setup' to finish configuring your environment."
echo ""
EOF

cat > "$BUILD_DIR/DEBIAN/prerm" << 'EOF'
#!/bin/sh
rm -f /data/data/com.termux/files/usr/bin/aurastudio
EOF

chmod 755 "$BUILD_DIR/DEBIAN/postinst" "$BUILD_DIR/DEBIAN/prerm"

# 4. Copy the source files to the opt directory
cp -r aurastudio config lib modules "$OPT_TARGET/"
chmod +x "$OPT_TARGET/aurastudio"

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
