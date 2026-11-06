#!/usr/bin/env bash
# Copies the freshly built jar into the user's Fabric instance.
#
# The dev client (`./gradlew runClient`) keeps the project locked, so this script refuses to run while it is up:
# kill the dev client first, otherwise the copy is pointless (the user would keep playing an old build).
set -euo pipefail

cd "$(dirname "$0")/.."

MODS="/d/game/.minecraft/versions/1.20.1-Fabric 0.20.1/mods"
JAR=$(ls build/libs/Tinkers-Fabstruct-*.jar 2>/dev/null | grep -v -e '-sources' -e '-dev' | head -n 1 || true)
TARGET="$MODS/$(basename "$JAR")"

if jps -l | grep -q net.fabricmc.devlaunchinjector.Main; then
  echo "a dev client is still running, stop it before deploying" >&2
  exit 1
fi

if [ ! -f "$JAR" ]; then
  echo "missing $JAR, run ./gradlew build --offline -x test first" >&2
  exit 1
fi

# drop older builds under the previous file name, otherwise two jars with the same mod id load at once
rm -f "$MODS"/tconstruct-fabric-*.jar
cp -f "$JAR" "$TARGET"
echo "deployed $(stat -c%s "$JAR") bytes to $TARGET"
