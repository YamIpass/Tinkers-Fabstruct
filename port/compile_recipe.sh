#!/usr/bin/env bash
# Throwaway compile harness for the library/recipe slice (no gradle).
# Usage: compile_recipe.sh [slice|clean]   clean = only files with no Mantle-loadable/Forge-only dependency
set -u
WS=/c/Users/YamIpass/Documents/deepseek-harness/default-workspace
export JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"
cd $WS/tconstruct-fabric
ARGS=/tmp/portslice/args.txt
BLOCK=/tmp/portslice/blocked_src.txt
grep -rlE "mantle\.data\.loadable|LoadableRecipeSerializer|TypeAwareRecipeSerializer|LoadableFinishedRecipe|IFluidHandler|common\.crafting|net\.minecraftforge|slimeknights\.tconstruct\.smeltery" \
  src/main/java/slimeknights/tconstruct/library/recipe --include=*.java > $BLOCK
{
  echo '-proc:full'
  echo '-nowarn'
  echo '-Xmaxerrs'
  echo '100000'
  echo '--release'
  echo '17'
  echo '-encoding'
  echo 'UTF-8'
  echo "-cp $(cat /tmp/portslice/cp_win.txt)"
  echo '-d'
  echo '/tmp/portslice/out'
  find src/main/java/slimeknights/tconstruct/library/recipe -name '*.java' | sort > /tmp/portslice/all_src.txt
  if [ "${1:-}" = "clean" ]; then
    comm -23 /tmp/portslice/all_src.txt <(sort $BLOCK)
  else
    cat /tmp/portslice/all_src.txt
  fi
} > $ARGS
echo "sources: $(grep -c '\.java$' $ARGS)  excluded(blocked): $(wc -l < $BLOCK)"
javac @$ARGS > /tmp/portslice/errors.txt 2>&1
echo "--- errors: $(grep -cE '\.java:[0-9]+: error:' /tmp/portslice/errors.txt)  files: $(grep -oE '^[^ :]+\.java' /tmp/portslice/errors.txt | sort -u | wc -l)"
