#!/usr/bin/env bash
# Compile-check the gadgets slice only (no gradle).
# Usage: tools/gadgets_check.sh
set -u
cd /c/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric || exit 1
export JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US"
OUT=/tmp/gadgets
mkdir -p $OUT/out
find src/main/java/slimeknights/tconstruct/gadgets -name '*.java' | sort > $OUT/srcs.txt
{
  echo '-proc:full'
  echo '-nowarn'
  echo '-Xmaxerrs'
  echo '100000'
  echo '--release'
  echo '17'
  echo '-encoding'
  echo 'UTF-8'
  echo "-cp $(cat $OUT/cp_win.txt)"
  echo '-d'
  echo "$OUT/out"
  echo '-sourcepath'
  echo 'src/main/java'
  cat $OUT/srcs.txt
} > $OUT/args.txt
echo "sources: $(wc -l < $OUT/srcs.txt)"
javac @$OUT/args.txt > $OUT/errors.txt 2>&1
echo "total error lines: $(grep -cE '\.java:[0-9]+: error:' $OUT/errors.txt)"
grep -oE '^[^ :]+\.java(?=:[0-9]+: error:)' -P $OUT/errors.txt | sort | uniq -c | sort -rn | head -40
