#!/usr/bin/env bash
# one.sh <relpath under library/client> [extra javac args]
# Compiles ONE slice file (plus its source dependencies) and prints only errors located in that file.
set -u
cd "$(dirname "$0")/.." || exit 1
REL="$1"; shift || true
SRC_WIN="$(cygpath -w "$PWD/src/main/java")"
TMP="$(cygpath -u "${TEMP:-/tmp}")/lc"
CP="$(cat "$TMP/cp_win.txt")"
OUT="$TMP/out_one"; mkdir -p "$OUT"
TARGET="$SRC_WIN\\slimeknights\\tconstruct\\"$(echo "library/client/$REL" | tr '/' '\\')
javac -J-Duser.language=en -J-Duser.country=US -proc:full -nowarn -Xmaxerrs 500 \
  -cp "$CP" -sourcepath "$SRC_WIN" -d "$(cygpath -w "$OUT")" "$TARGET" "$@" 2>&1 \
  | tr '\\' '/' | grep -A4 "library/client/${REL}[:][0-9]*: error" | sed 's/^/  /'
