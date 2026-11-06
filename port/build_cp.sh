#!/usr/bin/env bash
# Rebuild /tmp/portslice/cp.txt (POSIX, colon separated) and /tmp/portslice/cp_win.txt (Windows, semicolon separated).
set -u
WS=/c/Users/YamIpass/Documents/deepseek-harness/default-workspace
MC=$(cat /tmp/portslice/mcpath.txt | tr -d '\r')
REMAPPED=$WS/tconstruct-fabric/build/loom-cache/remapped_working
PRE=$WS/tconstruct-fabric/build/classes/java/main
MODS=$(ls $REMAPPED/*.jar | grep -v sources | tr '\n' ':')
G=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1
pick() { find "$G/$1/$2" -name "$2-*.jar" 2>/dev/null | grep -v sources | sort | tail -1; }
DEPS=""
for ga in "com.mojang brigadier" "com.mojang datafixerupper" "com.mojang authlib" \
          "com.mojang logging" "com.mojang patchy" "com.mojang blocklist" \
          "com.google.guava guava" "com.google.code.gson gson" \
          "com.google.code.findbugs jsr305" \
          "org.apache.commons commons-lang3" "it.unimi.dsi fastutil" \
          "net.java.dev.jna jna" "net.java.dev.jna jna-platform" \
          "net.fabricmc fabric-loader" "org.slf4j slf4j-api" \
          "org.ow2.asm asm" "org.ow2.asm asm-tree" "org.ow2.asm asm-analysis" \
          "org.ow2.asm asm-commons" "org.ow2.asm asm-util" \
          "org.jetbrains annotations" "io.netty netty-common" "io.netty netty-codec" \
          "org.projectlombok lombok"; do
  j=$(pick $ga)
  [ -n "$j" ] && DEPS="$DEPS$j:"
done
POSIX="$MC:$MODS$DEPS$PRE"
echo "$POSIX" | sed 's/:$//' > /tmp/portslice/cp.txt
echo "$POSIX" | tr ':' '\n' | sed 's|^/\([a-zA-Z]\)/|\1:/|' | grep . | paste -sd';' - > /tmp/portslice/cp_win.txt
echo "entries: $(tr ':' '\n' < /tmp/portslice/cp.txt | grep -c .)  win-chars: $(wc -c < /tmp/portslice/cp_win.txt)"
head -c 160 /tmp/portslice/cp_win.txt; echo
