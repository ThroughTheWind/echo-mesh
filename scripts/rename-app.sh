#!/usr/bin/env bash
# Renames the app from its working name once Antoine picks one (run it once, on a branch):
#   scripts/rename-app.sh <id> "<Display Name>"
# <id> is lowercase letters and digits, used for the package root (io.github.throughthewind.<id>),
# the Gradle root project, artifact names and AGENTS.md. Display Name is the launcher label.
# It moves every package directory, rewrites every tracked text file that names the working name,
# then refuses to finish if the old name is still anywhere in a tracked file.
set -euo pipefail

old_id="protestmap"
old_name="Protest Map"
new_id="${1:?usage: rename-app.sh <id> \"<Display Name>\"}"
new_name="${2:?usage: rename-app.sh <id> \"<Display Name>\"}"

if [[ ! "$new_id" =~ ^[a-z][a-z0-9]*$ ]]; then
  echo "error: <id> must be lowercase letters and digits, starting with a letter: $new_id" >&2
  exit 1
fi
if [[ "$new_name" == *"/"* || "$new_name" == *"&"* || "$new_name" == *"\\"* ]]; then
  echo "error: the display name may not contain / & or \\" >&2
  exit 1
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root"
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "error: commit or stash your changes first, so the rename is one reviewable commit" >&2
  exit 1
fi

# 1. Package directories: */io/github/throughthewind/<old_id> -> */<new_id>.
while IFS= read -r dir; do
  git mv "$dir" "$(dirname "$dir")/$new_id"
done < <(git ls-files | grep "/io/github/throughthewind/$old_id/" | sed "s|\(.*/io/github/throughthewind/$old_id\)/.*|\1|" | sort -u)

# 2. Text in tracked files (this script excepted, so it can still be read and rerun).
git ls-files -z | while IFS= read -r -d '' file; do
  [[ "$file" == "scripts/rename-app.sh" ]] && continue
  grep -Iq . "$file" 2>/dev/null || continue
  if grep -q -e "$old_id" -e "$old_name" "$file"; then
    sed -i -e "s/$old_id/$new_id/g" -e "s/$old_name/$new_name/g" "$file"
  fi
done

# 3. Check the artifact, not the script: nothing tracked still names the old app.
left="$(git ls-files | grep -v '^scripts/rename-app.sh$' | xargs grep -Il -e "$old_id" -e "$old_name" 2>/dev/null || true)"
if [[ -n "$left" ]]; then
  echo "error: still names $old_id or $old_name:" >&2
  echo "$left" >&2
  exit 1
fi
echo "Renamed $old_id to $new_id (\"$new_name\"). Next: run ./gradlew test and node .ai/rungs.mjs check,"
echo "then update old_id and old_name at the top of this script, or delete it."
