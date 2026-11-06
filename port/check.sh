#!/usr/bin/env bash
# Compile the given project-relative java files and show only their own diagnostics.
cd /c/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric || exit 1
OUT=$(mktemp)
"$JAVA_HOME/bin/javac" "@C:/Users/YamIpass/AppData/Local/Temp/cpt/opts_mine.args" "$@" > "$OUT" 2>&1
iconv -f GBK -t UTF-8 "$OUT" 2>/dev/null > "$OUT.u8" || cp "$OUT" "$OUT.u8"
for f in "$@"; do
  base=$(basename "$f" .java)
  echo "=== $base"
  awk -v b="$base.java" '
    $0 ~ ("[\\\\/]" b ":") { show=1; print; next }
    show == 1 && /^[a-zA-Z ]*: / && $0 !~ /[\\\\/]/ { print; next }
    { show=0 }
  ' "$OUT.u8" | head -30
done
