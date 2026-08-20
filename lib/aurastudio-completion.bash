# AuraStudio CLI Bash Completion
_aurastudio_completions() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    opts="setup install remove clean init update check-update status doctor uninstall man version -v --verbose --help"

    case "$prev" in
        aurastudio)
            mapfile -t COMPREPLY < <(compgen -W "$opts" -- "$cur")
            return 0
            ;;
        install)
            mapfile -t COMPREPLY < <(compgen -W "sdk ndk cmake" -- "$cur")
            return 0
            ;;
        remove)
            mapfile -t COMPREPLY < <(compgen -W "sdk ndk cmake" -- "$cur")
            return 0
            ;;
        sdk)
            mapfile -t COMPREPLY < <(compgen -W "platform buildtools" -- "$cur")
            return 0
            ;;
        status)
            mapfile -t COMPREPLY < <(compgen -W "--json -j" -- "$cur")
            return 0
            ;;
        doctor)
            mapfile -t COMPREPLY < <(compgen -W "--fix --snapshot" -- "$cur")
            return 0
            ;;
    esac
}

complete -F _aurastudio_completions aurastudio
