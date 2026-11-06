#!/usr/bin/env bash
# sweep_all.sh: compile every library/client file on its own and record errors located in that file.
set -u
cd "$(dirname "$0")/.." || exit 1
OUT="$(cygpath -u "${TEMP:-/tmp}")/lc/sweep_all.txt"
: > "$OUT"
files=$(cd src/main/java/slimeknights/tconstruct/library/client && find . -name '*.java' | sed 's|^\./||' | sort)
total=0
for rel in $files; do
  n=$(bash port/one.sh "$rel" 2>/dev/null | grep -c ": error:")
  total=$((total + n))
  printf '%s\t%s\n' "$n" "$rel" >> "$OUT"
done
echo "files: $(wc -l < "$OUT")  total own errors: $total"
awk -F'\t' '$1>0' "$OUT" | sort -rn
