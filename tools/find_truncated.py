"""Report port files that are markedly shorter than their upstream 3.12.1 counterpart.

A large drop usually means an agent dropped a chunk of the file (as happened with
TinkerSmeltery's creative tab) instead of adapting it.

Usage: python tools/find_truncated.py [--min-lines 40] [--ratio 0.7] [--show N]
"""
import argparse
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
PORT = ROOT / 'src/main/java'
UPSTREAM = ROOT.parent / 'ref/TinkersConstruct/src/main/java'


def count_lines(path):
    return len(path.read_text(encoding='utf-8').splitlines())


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--min-lines', type=int, default=40)
    parser.add_argument('--ratio', type=float, default=0.7)
    parser.add_argument('--show', type=int, default=40)
    args = parser.parse_args()

    rows = []
    for port_file in PORT.rglob('*.java'):
        rel = port_file.relative_to(PORT)
        upstream_file = UPSTREAM / rel
        if not upstream_file.is_file():
            continue
        port_lines = count_lines(port_file)
        up_lines = count_lines(upstream_file)
        if up_lines < args.min_lines:
            continue
        ratio = port_lines / up_lines
        if ratio < args.ratio:
            rows.append((round(ratio, 2), port_lines, up_lines, str(rel).replace('\\', '/')))

    rows.sort()
    print(f'{len(rows)} file(s) below {args.ratio:.0%} of upstream length')
    for ratio, port_lines, up_lines, rel in rows[:args.show]:
        print(f'{ratio:5.2f}  {port_lines:5d} / {up_lines:5d}  {rel}')


if __name__ == '__main__':
    main()
