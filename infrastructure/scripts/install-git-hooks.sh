#!/usr/bin/env bash
# Installs repository git hooks (git does not track .git/hooks, so this
# copies the versioned hook scripts into place). Run once after cloning:
#   ./infrastructure/scripts/install-git-hooks.sh
set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_SRC="$REPO_ROOT/infrastructure/scripts"
HOOKS_DST="$REPO_ROOT/.git/hooks"

cp "$HOOKS_SRC/pre-commit" "$HOOKS_DST/pre-commit"
chmod +x "$HOOKS_DST/pre-commit"

echo "Git hooks installed. Secret scanning (gitleaks) will run before every commit."
if ! command -v gitleaks >/dev/null 2>&1; then
  echo "NOTE: gitleaks is not installed yet. Run: brew install gitleaks"
fi
