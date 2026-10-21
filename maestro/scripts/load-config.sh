# Sourced by the other scripts: loads the local settings file into the
# environment. The file is maestro/.env.e2e in a working copy and
# ~/e2e/.env.e2e on the runner (next to bin/), or $E2E_CONFIG when set.
# Copy .env.example to create it; it is gitignored.
#
# Lines are KEY=value. A variable already set in the environment wins, so a
# one-off override needs no edit. The file is read as data, never executed.

load_e2e_config() {
    local dir file line key value
    dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    file="${E2E_CONFIG:-$dir/../.env.e2e}"
    [ -f "$file" ] || return 0

    if [ -n "$(find "$file" -perm +044 2>/dev/null)" ]; then
        echo "Warning: $file is readable by other users; run chmod 600 on it" >&2
    fi

    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in ''|'#'*) continue ;; esac
        key="${line%%=*}"
        value="${line#*=}"
        [[ "$key" =~ ^[A-Z_][A-Z0-9_]*$ ]] || continue
        # Optional surrounding quotes
        value="${value#\"}"; value="${value%\"}"
        value="${value#\'}"; value="${value%\'}"
        [ -n "$value" ] || continue
        [ -n "${!key:-}" ] && continue
        export "$key=$value"
    done < "$file"
}

load_e2e_config
