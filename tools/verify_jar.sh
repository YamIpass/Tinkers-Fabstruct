#!/usr/bin/env bash
# Sanity checks on a finished jar: entrypoints, access widener, bundled libraries and data coverage.
# Usage: tools/verify_jar.sh [path-to-jar]
set -uo pipefail
JAR=${1:-$(ls build/libs/Tinkers-Fabstruct-*.jar 2>/dev/null | grep -v -e '-sources' -e '-dev' | head -n 1)}
fail=0
say() { printf '%-46s %s\n' "$1" "$2"; }
check() { if [ "$2" = "$3" ]; then say "$1" "ok ($3)"; else say "$1" "MISMATCH (want $3, got $2)"; fail=1; fi; }

unzip -tqq "$JAR" >/dev/null || { echo "jar is corrupt: $JAR"; exit 1; }
say "jar" "$(stat -c %s "$JAR") bytes"

mod=$(unzip -p "$JAR" fabric.mod.json)
echo "$mod" > /tmp/jar_fabric.mod.json
check "entrypoint main" "$(grep -c 'slimeknights.tconstruct.TConstruct' /tmp/jar_fabric.mod.json)" "1"
check "entrypoint client" "$(grep -c 'slimeknights.tconstruct.client.TinkerClient' /tmp/jar_fabric.mod.json)" "1"
check "access widener declared" "$(grep -c 'tconstruct.accesswidener' /tmp/jar_fabric.mod.json)" "1"
check "access widener shipped" "$(unzip -l "$JAR" | grep -c 'tconstruct.accesswidener')" "1"
check "mixin config shipped" "$(unzip -l "$JAR" | grep -c 'tconstruct.mixins.json')" "1"

say "bundled libs" "$(unzip -l "$JAR" | grep -c 'META-INF/jars/')"
say "data jsons" "$(unzip -l "$JAR" | grep -c 'data/tconstruct/.*\.json')"
say "models" "$(unzip -l "$JAR" | grep -c 'assets/tconstruct/models/.*\.json')"
say "textures" "$(unzip -l "$JAR" | grep -c 'assets/tconstruct/textures/.*\.png')"
say "classes" "$(unzip -l "$JAR" | grep -c '\.class')"

exit $fail
