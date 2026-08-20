# AuraStudio CLI v{VERSION}

We are excited to announce a new release of **AuraStudio CLI**!

## Highlights

{HIGHLIGHTS}

{CHANGELOG}

## Quick Installation

### Option 1: Web Installer (Recommended)

```bash
curl -sSL https://raw.githubusercontent.com/Arata-Labs/AuraStudio/main/install.sh | bash
```

### Option 2: DEB Package

```bash
# Download the .deb package from this release, then:
pkg install ./aurastudio_{VERSION}_all.deb
```

## Verify Installation

```bash
# Verify package checksum
sha256sum -c aurastudio_{VERSION}_all.deb.sha256

# Check installed version
aurastudio version
```

## Post-Installation Setup

```bash
# Run environment setup
aurastudio setup

# Reload shell configuration
source ~/.config/aurastudio/env.sh
```

## Documentation

- [README](https://github.com/Arata-Labs/AuraStudio#readme)
- [Command Reference](https://github.com/Arata-Labs/AuraStudio#-command-reference)
- [Troubleshooting](https://github.com/Arata-Labs/AuraStudio#-troubleshooting)

## Support

- [Report Issues](https://github.com/Arata-Labs/AuraStudio/issues)
- [Discussions](https://github.com/Arata-Labs/AuraStudio/discussions)

---

{COMPARE_URL}
