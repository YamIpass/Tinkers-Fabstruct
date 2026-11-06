#!/usr/bin/env bash
# Class index over the javac classpath (python helper needs Windows paths).
#   idx.sh build            -> $TEMP/lc/cpindex.txt  (fqcn TAB jar)
#   idx.sh f <Simple>       -> fqcns whose simple name is <Simple>
#   idx.sh g <regex>        -> fqcns matching regex
set -u
TMP=${TEMP:-/tmp}
PY="/c/Users/YamIpass/.dsh/dsh-runtimes/dsh-primary-runtime/dependencies/python/python.exe"
IDX="$TMP/lc/cpindex.txt"
case "${1:-build}" in
  build)
    tr ';' '\n' < "$TMP/lc/cp_win.txt" > "$TMP/lc/cp_lines.txt"
    "$PY" "$(dirname "$0")/index_cp.py"
    ;;
  f)
    cut -f1 "$IDX" | grep -E "(^|\\.)$2\$" | sort | head -40
    ;;
  g)
    cut -f1 "$IDX" | grep -E "$2" | sort | head -60
    ;;
esac
