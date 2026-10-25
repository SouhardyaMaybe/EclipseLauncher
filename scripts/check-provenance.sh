#!/usr/bin/env bash
# Fail if source code contains a component that is not allowed by PROVENANCE.md.
# Documentation is excluded because it legitimately names competitors and
# rejected components; code, build files and workflows are not.
set -euo pipefail

cd "$(dirname "$0")/.."

roots=()
for d in app runtime sandpaper sash foreman crowbar gradle scripts .github; do
  [ -d "$d" ] && roots+=("$d")
done

if [ ${#roots[@]} -eq 0 ]; then
  echo "provenance: no source directories yet; nothing to scan"
  exit 0
fi

# Markers that must never appear in shipped source or build inputs.
pattern='PojavLauncherTeam|FCL-Team|AmethystLauncher|ZalithLauncher|MioLibPatcher|MioFabricAgent|caciocavallo|net\.kdt\.pojavlaunch|me\.shadow\.eclipselauncher|com\.mio\.|OptiFineRenamer'
if grep -RInE "$pattern" "${roots[@]}" \
    --exclude-dir=build --exclude-dir=.gradle \
    --exclude='*.md' --exclude='check-provenance.sh'; then
  echo
  echo "provenance: forbidden component marker found (see PROVENANCE.md §3)" >&2
  exit 1
fi

# Every Java/Kotlin source file must carry an SPDX identifier in its header.
missing=0
while IFS= read -r -d '' f; do
  if ! head -n 8 "$f" | grep -q 'SPDX-License-Identifier:'; then
    echo "provenance: missing SPDX header: $f" >&2
    missing=1
  fi
done < <(find "${roots[@]}" -type f \( -name '*.kt' -o -name '*.java' -o -name '*.c' -o -name '*.h' -o -name '*.cpp' \) -print0)

if [ "$missing" -ne 0 ]; then
  exit 1
fi

echo "provenance: passed (${#roots[@]} roots)"
