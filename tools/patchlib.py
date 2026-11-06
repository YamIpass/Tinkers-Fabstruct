"""Small helpers for surgical, CRLF-preserving edits of the Fabric port sources.

All paths are relative to the project root (cwd when the script is run).
"""
import os

PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(rel):
  with open(os.path.join(PROJECT, rel), 'r', encoding='utf-8', newline='') as f:
    text = f.read()
  # keep line endings intact, but work on a list of logical lines
  if '\r\n' in text:
    return text.split('\r\n'), '\r\n'
  return text.split('\n'), '\n'


def write(rel, lines, nl):
  path = os.path.join(PROJECT, rel)
  with open(path, 'w', encoding='utf-8', newline='') as f:
    f.write(nl.join(lines))
  print('wrote', rel, len(lines), 'lines')


def rm_imports(lines, names):
  """Removes `import X;` / `import static X;` lines for the given fully qualified names."""
  wanted = {'import ' + n + ';' for n in names} | {'import static ' + n + ';' for n in names}
  out = [l for l in lines if l.strip() not in wanted]
  removed = len(lines) - len(out)
  print('removed imports:', removed, 'expected', len(names))
  return out


def add_imports(lines, names):
  """Inserts imports before the first net.minecraft import (keeping io.github/net.fabricmc above it)."""
  first = next((i for i, l in enumerate(lines) if l.startswith('import ')), None)
  if first is None:
    raise SystemExit('no import block found')
  at = next((i for i in range(first, len(lines)) if lines[i].startswith('import net.minecraft')), first)
  block = ['import ' + n + ';' for n in names]
  for l in block:
    if l in lines:
      block.remove(l)
  return lines[:at] + block + lines[at:]


def find_line(lines, needle, start=0):
  for i in range(start, len(lines)):
    if needle in lines[i]:
      return i
  raise SystemExit('line not found: ' + needle)


def method_bounds(lines, signature_needle, start=0):
  """Returns (index of the @SubscribeEvent/javadoc/@ line above the signature, index of the closing brace)."""
  sig = find_line(lines, signature_needle, start)
  # walk up over annotations and javadoc/comments
  top = sig
  while top > 0:
    prev = lines[top - 1].strip()
    if prev.startswith('@') or prev.startswith('*') or prev.startswith('/*') or prev.startswith('//'):
      top -= 1
    else:
      break
  # brace match from the signature
  depth = 0
  opened = False
  for i in range(sig, len(lines)):
    for ch in lines[i]:
      if ch == '{':
        depth += 1
        opened = True
      elif ch == '}':
        depth -= 1
        if opened and depth == 0:
          return top, i
  raise SystemExit('unbalanced braces for ' + signature_needle)


def unwrap_block(lines, needle, start=0):
  """Deletes a `{`-opening line (e.g. an `if (...) {`) plus its matching close brace, dedenting the body by 2."""
  i = find_line(lines, needle, start)
  if not lines[i].rstrip().endswith('{'):
    raise SystemExit('not a block opener: ' + lines[i])
  depth = 0
  end = None
  for k in range(i, len(lines)):
    for ch in lines[k]:
      if ch == '{':
        depth += 1
      elif ch == '}':
        depth -= 1
        if depth == 0:
          end = k
          break
    if end is not None:
      break
  if end is None:
    raise SystemExit('unbalanced block: ' + needle)
  dedent(lines, i + 1, end - 1)
  del lines[end]
  del lines[i]
  return lines


def dedent(lines, start, end, amount=2):
  """Dedents lines[start:end] (inclusive) by removing up to `amount` leading spaces."""
  for i in range(start, end + 1):
    removed = 0
    while removed < amount and lines[i].startswith(' '):
      lines[i] = lines[i][1:]
      removed += 1
  return lines
