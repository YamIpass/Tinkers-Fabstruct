#!/usr/bin/env python3
"""Builds the project icon: the upstream Tinkers' Construct sign with the Fabric cloth beneath it.

Top: the original sign artwork from the upstream 3.12.1 jar (`logo.png`, MIT, credited in NOTICE).
Bottom: the Fabric Loader cloth icon (`assets/fabricloader/icon.png` from Fabric Loader, Apache-2.0,
used only to indicate platform compatibility), placed flush under the sign.

Outputs:

  docs/modrinth/icon.png                 512x512 for the Modrinth project page
  src/main/resources/icon.png            128x128 for the in-game mod list / jade tooltip
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SIGN = ROOT / "docs/upstream/upstream_logo.png"
CLOTH = ROOT / "docs/upstream/fabric_loader_icon.png"

BG_TOP = (46, 36, 27, 255)
BG_BOTTOM = (26, 20, 15, 255)
FRAME = (92, 70, 46, 255)
FRAME_INNER = (58, 44, 30, 255)


def background(size: int) -> Image.Image:
  im = Image.new("RGBA", (size, size), BG_TOP)
  d = ImageDraw.Draw(im)
  for y in range(size):
    t = y / (size - 1)
    color = tuple(int(BG_TOP[i] * (1 - t) + BG_BOTTOM[i] * t) for i in range(3)) + (255,)
    d.line([(0, y), (size, y)], fill=color)
  border = max(3, size // 85)
  d.rectangle([0, 0, size - 1, size - 1], outline=FRAME, width=border)
  inset = max(6, size // 50)
  d.rectangle([inset, inset, size - inset - 1, size - inset - 1], outline=FRAME_INNER, width=max(1, size // 256))
  return im


def build(size: int) -> Image.Image:
  im = background(size)

  sign_width = round(size * 0.88)
  sign = Image.open(SIGN).convert("RGBA")
  sign_height = round(sign.height * sign_width / sign.width)
  sign = sign.resize((sign_width, sign_height), Image.Resampling.LANCZOS)

  cloth = Image.open(CLOTH).convert("RGBA")
  cloth = cloth.crop(cloth.getbbox())
  cloth_size = round(size * 0.3125)
  cloth = cloth.resize((cloth_size, cloth_size), Image.Resampling.NEAREST)

  overlap = round(size * 0.015)
  total = sign_height + cloth_size - overlap
  top = (size - total) // 2
  sign_y = top
  cloth_y = top + sign_height - overlap

  im.alpha_composite(sign, ((size - sign_width) // 2, sign_y))
  im.alpha_composite(cloth, ((size - cloth_size) // 2, cloth_y))
  return im


def main() -> None:
  for size, out in ((512, ROOT / "docs/modrinth/icon.png"), (128, ROOT / "src/main/resources/icon.png")):
    out.parent.mkdir(parents=True, exist_ok=True)
    build(size).save(out)
    print(f"wrote {out.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
  main()
