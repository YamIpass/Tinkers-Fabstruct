#!/usr/bin/env bash
# Classpath + compile harness for the library/client slice (no gradle).
# Usage:
#   client_cp.sh cp            -> writes /tmp/lc/cp_win.txt
#   client_cp.sh javap <fqcn>  -> javap against the full classpath
#   client_cp.sh compile       -> compile every library/client source
set -u
WS=/c/Users/YamIpass/Documents/deepseek-harness/default-workspace
P=$WS/tconstruct-fabric
MC=/c/Users/YamIpass/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged/1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2/minecraft-merged-1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2.jar
R=$P/build/loom-cache/remapped_working
G=/c/Users/YamIpass/.gradle/caches/modules-2/files-2.1
mkdir -p /tmp/lc

build_cp() {
  # the yarn-named minecraft jar itself, then new deps (Mantle 1.11 / Porting Lib 2.3.16
  # must win over stale remaps)
  # stable copies first (/tmp/lc/jars) because a parallel gradle run may clobber the loom caches
  if [ -f /tmp/lc/jars/mc_yarn.jar ]; then echo -n "$(cygpath -m /tmp/lc/jars/mc_yarn.jar):"; else echo -n "$MC:"; fi
  {
    ls /tmp/lc/jars/*Mantle-1.20.1-1.11*.jar /tmp/lc/jars/*2.3.16-beta.81*.jar 2>/dev/null
    ls $R/*.jar | grep -v sources | grep -vE "Mantle-1.20.1-1.9|2\.3\.14|Mantle-1.20.1-1.11|2\.3\.16-beta\.81"
  } | while read -r j; do cygpath -m "$j"; done | sort -u | tr '\n' ':'
  for ga in "com.google.code.gson gson" "com.google.guava guava" "com.google.code.findbugs jsr305" \
            "org.joml joml" "org.projectlombok lombok" "org.slf4j slf4j-api" "org.jetbrains annotations" \
            "org.apache.commons commons-lang3" "it.unimi.dsi fastutil" \
            "org.apache.logging.log4j log4j-api" "org.apache.logging.log4j log4j-core" \
            "org.apache.logging.log4j log4j-slf4j2-impl" "com.mojang brigadier" \
            "com.mojang datafixerupper" "com.mojang authlib" "com.mojang logging" \
            "com.mojang blocklist" "com.mojang patchy" "net.fabricmc fabric-loader" \
            "org.ow2.asm asm" "org.ow2.asm asm-tree" "org.ow2.asm asm-analysis" \
            "org.ow2.asm asm-commons" "org.ow2.asm asm-util" "net.java.dev.jna jna" \
            "io.netty netty-common" "io.netty netty-codec" "io.netty netty-buffer" \
            "io.netty netty-transport" "io.netty netty-handler" "io.netty netty-resolver"; do
    set -- $ga
    j=$(find "$G/$1/$2" -name "$2*.jar" 2>/dev/null | grep -v sources | sort | tail -1)
    [ -n "$j" ] && echo -n "$j:"
  done
}

case "${1:-cp}" in
  cp)
    CP="$(build_cp | sed 's/:$//')"
    echo "$CP" | tr ':' '\n' | sed 's|^/\([a-zA-Z]\)/|\1:/|' | paste -sd';' - | tr -d '\r' > /tmp/lc/cp_win.txt
    echo "entries: $(tr ';' '\n' < /tmp/lc/cp_win.txt | grep -c .)"
    ;;
  javap)
    shift
    javap -p -cp "$(cat /tmp/lc/cp_win.txt)" "$@" 2>&1 | head -200
    ;;
  compile)
    echo "-proc:full" > /tmp/lc/args.txt
    echo "-nowarn" >> /tmp/lc/args.txt
    echo "-Xmaxerrs" >> /tmp/lc/args.txt
    echo "100000" >> /tmp/lc/args.txt
    echo "--release" >> /tmp/lc/args.txt
    echo "17" >> /tmp/lc/args.txt
    echo "-encoding" >> /tmp/lc/args.txt
    echo "UTF-8" >> /tmp/lc/args.txt
    echo "-cp" >> /tmp/lc/args.txt
    echo "$(cat /tmp/lc/cp_win.txt)" >> /tmp/lc/args.txt
    echo "-d" >> /tmp/lc/args.txt
    cygpath -w /tmp/lc/out >> /tmp/lc/args.txt
    echo "-sourcepath" >> /tmp/lc/args.txt
    cygpath -w "$P/src/main/java" >> /tmp/lc/args.txt
    find $P/src/main/java/slimeknights/tconstruct/library/client -name '*.java' | sort | while read -r f; do cygpath -w "$f"; done >> /tmp/lc/args.txt
    mkdir -p /tmp/lc/out
    ( cd $P && JAVA_TOOL_OPTIONS="-Duser.language=en -Duser.country=US" javac @/tmp/lc/args.txt > /tmp/lc/errors.txt 2>&1 )
    E=/tmp/lc/errors.txt
    echo "errors: $(grep -cE '\.java:[0-9]+: error:' $E)  files: $(sed -nE 's#^.*\\src\\main\\java\\([^:]+\.java):[0-9]+: error:.*#\1#p' $E | sort -u | wc -l)"
    echo "--- slice (library\\client) errors per file:"
    sed -nE 's#^.*\\library\\client\\([^:]+\.java):[0-9]+: error:.*#\1#p' $E | sort | uniq -c | sort -rn | head -40
    echo "--- non-client java errors per file:"
    sed -nE 's#^.*\\src\\main\\java\\([^:]+\.java):[0-9]+: error:.*#\1#p' $E | grep -v '^slimeknights\\tconstruct\\library\\client\\' | sort | uniq -c | sort -rn | head -20
    ;;
esac
