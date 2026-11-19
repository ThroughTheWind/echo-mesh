#!/usr/bin/env bash
# Is Echo's release APK reproducible? (WI-032, AC-31, NFR-24, extended spec FR-19.4)
#
#   scripts/reproducible.sh [commit]
#       Builds the unsigned release APK of <commit> (default HEAD) twice, from two separate copies
#       at different paths, in different time zones and locales, and fails on any difference.
#   scripts/reproducible.sh --against <published.apk> [commit]
#       Builds <commit> once and compares it with a published, signed APK: every file inside must
#       match, the signature files apart. This is how anyone checks a release is the source it names.
#
# Needs a JDK and the Android SDK (ANDROID_HOME), as for any build. Compares file by file with the
# CRC and size `unzip -v` prints, so the report names what differs.
set -euo pipefail

against=""
if [[ "${1:-}" == "--against" ]]; then
  against="$(realpath "$2")"
  shift 2
fi
commit="$(git rev-parse --verify "${1:-HEAD}^{commit}")"
repo="$(git rev-parse --show-toplevel)"
work="$(mktemp -d)"
trap 'rm -rf "$work"; git -C "$repo" worktree prune' EXIT

# Builds <commit> in a fresh worktree at <dir> and prints the path of the unsigned APK.
build() {
  local dir="$1" tz="$2" lang="$3"
  git -C "$repo" worktree add --quiet --detach "$dir" "$commit"
  (cd "$dir" && TZ="$tz" LC_ALL="$lang" LANG="$lang" ./gradlew --quiet --no-daemon --no-build-cache \
    --no-configuration-cache :app:assembleRelease >&2)
  local apks=("$dir"/app/build/outputs/apk/release/*-unsigned.apk)
  if [[ ! -f "${apks[0]}" ]]; then
    echo "No unsigned release APK in $dir/app/build/outputs/apk/release" >&2
    exit 1
  fi
  echo "${apks[0]}"
}

# The files inside an APK as "CRC size name", sorted, without the v1 signature files.
listing() {
  unzip -v "$1" | awk 'NR > 3 && $1 ~ /^[0-9]+$/ && NF >= 8 { print $7, $1, $8 }' \
    | grep -Ev ' META-INF/[^/]*\.(SF|RSA|DSA|EC|MF)$' | sort -k3 || true
}

compare() {
  local a="$1" b="$2" la lb
  la="$(listing "$a")"
  lb="$(listing "$b")"
  if [[ -z "$la" || -z "$lb" ]]; then
    echo "The comparison is blind: an APK listed no files." >&2
    exit 1
  fi
  if [[ "$la" != "$lb" ]]; then
    echo "NOT reproducible: these files differ (CRC size name):" >&2
    diff <(echo "$la") <(echo "$lb") >&2 || true
    exit 1
  fi
}

first="$(build "$work/one" UTC C.UTF-8)"
if [[ -n "$against" ]]; then
  compare "$first" "$against"
  echo "Reproducible: every file in $(basename "$against") matches a build of ${commit::12}."
  exit 0
fi
second="$(build "$work/two-other-path" Pacific/Auckland fr_FR.UTF-8)"
compare "$first" "$second"
# Unsigned, the two files must be byte for byte the same, not only their contents.
if ! cmp -s "$first" "$second"; then
  echo "NOT reproducible: same files inside, but the APKs differ byte for byte (order, timestamps or alignment)." >&2
  exit 1
fi
echo "Reproducible: two builds of ${commit::12} are identical, sha256 $(sha256sum "$first" | cut -d' ' -f1)."
