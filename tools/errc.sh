#!/usr/bin/env bash
# Show compile errors for one target file with source context.
# usage: tools/errc.sh <file-basename> [context-lines]
set -u
f="${1}"
ctx="${2:-0}"
grep -A12 -- "$f" /tmp/javac_slice.err | head -80
