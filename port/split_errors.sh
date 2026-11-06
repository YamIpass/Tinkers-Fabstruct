#!/usr/bin/env bash
# Split current javac errors into: files blocked on Mantle loadables vs files fixable now.
set -u
WS=/c/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric
ERR=/tmp/portslice/errors.txt
grep -oE '^[^ :]+\.java' $ERR | tr '\\' '/' | sort -u > /tmp/portslice/errfiles.txt
echo "files with errors: $(wc -l < /tmp/portslice/errfiles.txt)"
: > /tmp/portslice/blocked.txt
: > /tmp/portslice/fixable.txt
while IFS= read -r f; do
  if grep -q "mantle.data.loadable\|LoadableRecipeSerializer\|TypeAwareRecipeSerializer\|LoadableFinishedRecipe\|SimpleCache\|IFluidHandler\|common.crafting" "$WS/$f" 2>/dev/null; then
    echo "$f" >> /tmp/portslice/blocked.txt
  else
    echo "$f" >> /tmp/portslice/fixable.txt
  fi
done < /tmp/portslice/errfiles.txt
echo "blocked (mantle loadables / forge-only): $(wc -l < /tmp/portslice/blocked.txt)"
echo "fixable by me: $(wc -l < /tmp/portslice/fixable.txt)"
