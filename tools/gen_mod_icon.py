#!/usr/bin/env python3
"""Builds the project icon: the upstream Tinkers' Construct sign with the Fabric cloth beneath it.

Both halves are original artwork: the sign from the upstream 3.12.1 jar (`logo.png`, MIT, credited
in NOTICE) and the Fabric Loader cloth (`assets/fabricloader/icon.png`, Apache-2.0, used only to
indicate platform compatibility). The canvas is fully transparent - no frame, no background plate.

Outputs:

  docs/modrinth/icon.png                 512x512 for the Modrinth project page
  src/main/resources/icon.png            128x128 for the in-game mod list / jade tooltip
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SIGN = ROOT / "docs/upstream/upstream_logo.png"
CLOTH = ROOT / "docs/upstream/fabric_loader_icon.png"


def build(size: int) -> Image.Image:
  canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))

  sign_width = size
  sign = Image.open(SIGN).convert("RGBA")
  sign_height = round(sign.height * sign_width / sign.width)
  sign = sign.resize((sign_width, sign_height), Image.Resampling.LANCZOS)

  cloth = Image.open(CLOTH).convert("RGBA")
  cloth = cloth.crop(cloth.getbbox())
  cloth_size = round(size * 0.46)
  cloth = cloth.resize((cloth_size, cloth_size), Image.Resampling.NEAREST)

  overlap = round(size * 0.02)
  total = sign_height + cloth_size - overlap
  top = (size - total) // 2
  canvas.alpha_composite(sign, (0, top))
  canvas.alpha_composite(cloth, ((size - cloth_size) // 2, top + sign_height - overlap))
  return canvas


def main() -> None:
  for size, out in ((512, ROOT / "docs/modrinth/icon.png"), (128, ROOT / "src/main/resources/icon.png")):
    out.parent.mkdir(parents=True, exist_ok=True)
    build(size).save(out)
    print(f"wrote {out.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
  main()
