#!/usr/bin/env bash
# sweep.sh -> for every slice file currently reported in errors.txt, compile it standalone
# and count only errors located in that file (cascades from other files are ignored).
# Output: %TEMP%/lc/sweep2.txt  (count<TAB>relpath), sorted descending.
set -u
cd "$(dirname "$0")/.." || exit 1
TMP="$(cygpath -u "${TEMP:-/tmp}")/lc"
OUT="$TMP/sweep2.txt"
: > "$OUT"
mapfile -t FILES < <(bash port/py.sh port/err.py summary 2>/dev/null | grep -E "^\s+[0-9]+" | sed 's/^ *[0-9]*  //')
for rel in "${FILES[@]}"; do
  n=$(bash port/one.sh "$rel" 2>&1 | grep -c "library/client/${rel}:[0-9]*: error" || true)
  printf '%s\t%s\n' "$n" "$rel" >> "$OUT"
done
sort -rn "$OUT" -o "$OUT"
echo "SWEEP2 DONE: $(awk -F'\t' '$1>0' "$OUT" | wc -l) files with own errors, $(awk -F'\t' '{s+=$1} END {print s}' "$OUT") total"
