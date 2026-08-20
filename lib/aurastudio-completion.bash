# AuraStudio CLI Bash Completion
_aurastudio_completions() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    opts="setup install remove clean init update check-update status doctor uninstall -v --verbose --help"

    case "$prev" in
        aurastudio)
            COMPREPLY=( $(compgen -W "$opts" -- "$cur") )
            return 0
            ;;
        install)
            COMPREPLY=( $(compgen -W "sdk ndk cmake" -- "$cur") )
            return 0
            ;;
        remove)
            COMPREPLY=( $(compgen -W "sdk ndk cmake" -- "$cur") )
            return 0
            ;;
        sdk)
            COMPREPLY=( $(compgen -W "platform buildtools" -- "$cur") )
            return 0
            ;;
    esac
}

complete -F _aurastudio_completions aurastudio
