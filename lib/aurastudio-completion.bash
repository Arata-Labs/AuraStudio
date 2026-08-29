# AuraStudio CLI Bash Completion
# shellcheck disable=SC2207
_aurastudio_completions() {
    local cur prev opts
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"

    opts="setup install use remove clean init update check-update status doctor uninstall man completion version -v --verbose --help"

    case "$prev" in
        aurastudio)
            COMPREPLY=( $(compgen -W "$opts" -- "$cur") )
            return 0
            ;;
        install)
            COMPREPLY=( $(compgen -W "sdk ndk cmake" -- "$cur") )
            return 0
            ;;
        use)
            COMPREPLY=( $(compgen -W "java" -- "$cur") )
            return 0
            ;;
        java)
            COMPREPLY=( $(compgen -W "17 21" -- "$cur") )
            return 0
            ;;
        remove)
            COMPREPLY=( $(compgen -W "sdk ndk cmake" -- "$cur") )
            return 0
            ;;
        sdk)
            COMPREPLY=( $(compgen -W "platform buildtools java" -- "$cur") )
            return 0
            ;;
        status)
            COMPREPLY=( $(compgen -W "--json -j" -- "$cur") )
            return 0
            ;;
        doctor)
            COMPREPLY=( $(compgen -W "--fix --snapshot" -- "$cur") )
            return 0
            ;;
    esac
}

complete -F _aurastudio_completions aurastudio
