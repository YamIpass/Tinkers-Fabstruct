#!/usr/bin/env bash
# Summarize the last gradle compile log by file, package and message.
LOG=${1:-/tmp/c1.log}
echo "=== total errors: $(grep -cE '\.java:[0-9]+: 错误' "$LOG")"
echo "=== top files (errors)"
grep -oE 'src[\\/][^:]*\.java' "$LOG" | sort | uniq -c | sort -rn | head -25
echo "=== top packages (2 levels)"
grep -oE 'src[\\/][^:]*\.java' "$LOG" \
  | sed -E 's#src[\\/]main[\\/]java[\\/]slimeknights[\\/]tconstruct[\\/]?##; s#[\\/][^\\/]*\.java##' \
  | cut -d'\' -f1,2 | sort | uniq -c | sort -rn | head -25
echo "=== top messages"
grep -oE '错误: .*' "$LOG" | sed 's/错误: //' | cut -c1-50 | sort | uniq -c | sort -rn | head -15
