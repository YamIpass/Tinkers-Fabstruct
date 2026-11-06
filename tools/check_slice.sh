#!/usr/bin/env bash
# Compile-check a slice of the port against the cached javac classpath.
# Usage: tools/check_slice.sh <file-with-source-paths>
set -u
cd /c/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric || exit 1
CP=$(cat /tmp/cp_min.txt)
SRC=src/main/java
javac -J-Duser.language=en -J-Duser.country=US --release 17 -proc:full -nowarn \
  -Xmaxerrs 20000 -d /tmp/jout -cp "$CP" -sourcepath "$SRC" @"${1}" > /tmp/javac.err 2>&1
echo "total errors: $(grep -c 'error:' /tmp/javac.err)"
# filter: keep error lines whose path ends in one of the listed sources, plus indented continuation lines
awk -v list="$1" '
  BEGIN {
    n = 0
    while ((getline l < list) > 0) {
      sub(/^src\/main\/java\//, "", l)
      keys[n++] = l
    }
  }
  {
    line = $0
    gsub(/\\/, "/", line)
    if (index(line, "error:") > 0) {
      keep = 0
      for (i = 0; i < n; i++) { if (index(line, keys[i] ":") > 0) { keep = 1; break } }
    }
    if (keep) print
  }
' /tmp/javac.err > /tmp/javac_slice.err
echo "slice errors: $(grep -c 'error:' /tmp/javac_slice.err)"
