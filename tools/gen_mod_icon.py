#!/usr/bin/env python3
"""Builds the project icon: the upstream Tinkers' Construct sign over a Fabric cloth emblem.

The sign is the original artwork shipped by the upstream 3.12.1 jar (`logo.png`, MIT, credited in
NOTICE); the woven cloth below it is drawn here so the icon says "Fabric port" at a glance.
Outputs:

  docs/modrinth/icon.png                 512x512 for the Modrinth project page
  src/main/resources/icon.png            128x128 for the in-game mod list / jade tooltip
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SIGN = ROOT / "docs/upstream/upstream_logo.png"

SIZE = 512
BG_TOP = (44, 34, 26, 255)
BG_BOTTOM = (28, 22, 17, 255)
FRAME = (86, 66, 44, 255)
CLOTH = (240, 230, 212, 255)
CLOTH_SHADE = (206, 188, 160, 255)
CLOTH_DARK = (168, 148, 118, 255)
CLOTH_EDGE = (120, 102, 80, 255)
NEEDLE = (226, 232, 240, 255)
NEEDLE_SHADE = (150, 160, 176, 255)
THREAD = (198, 96, 72, 255)


def background() -> Image.Image:
  im = Image.new("RGBA", (SIZE, SIZE), BG_TOP)
  d = ImageDraw.Draw(im)
  for y in range(SIZE):
    t = y / (SIZE - 1)
    color = tuple(int(BG_TOP[i] * (1 - t) + BG_BOTTOM[i] * t) for i in range(3)) + (255,)
    d.line([(0, y), (SIZE, y)], fill=color)
  d.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=FRAME, width=6)
  d.rectangle([10, 10, SIZE - 11, SIZE - 11], outline=(60, 46, 32, 255), width=2)
  return im


def cloth_layer(cx: int, cy: int, half: int) -> Image.Image:
  """Returns a transparent layer holding a woven cloth square rotated 45 degrees."""
  layer = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
  d = ImageDraw.Draw(layer)

  top = (cx, cy - half)
  right = (cx + half, cy)
  bottom = (cx, cy + half)
  left = (cx - half, cy)
  d.polygon([top, right, bottom, left], fill=CLOTH_SHADE)

  inset = half - 14
  itop = (cx, cy - inset)
  iright = (cx + inset, cy)
  ibottom = (cx, cy + inset)
  ileft = (cx - inset, cy)
  d.polygon([itop, iright, ibottom, ileft], fill=CLOTH)

  # weave: checker blocks plus thin diagonal threads, clipped by the inner diamond
  weave = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
  wd = ImageDraw.Draw(weave)
  cell = 14
  for gy in range(cy - inset, cy + inset, cell):
    for gx in range(cx - inset, cx + inset, cell):
      shade = CLOTH if ((gx - cx) // cell + (gy - cy) // cell) % 2 == 0 else CLOTH_SHADE
      wd.rectangle([gx, gy, gx + cell - 1, gy + cell - 1], fill=shade)
  for offset in range(-inset, inset + 1, 9):
    wd.line([(cx + offset - inset, cy - inset), (cx + offset + inset, cy + inset)], fill=CLOTH_DARK, width=1)
    wd.line([(cx + offset - inset, cy + inset), (cx + offset + inset, cy - inset)], fill=(222, 208, 184, 255), width=1)
  mask = Image.new("L", (SIZE, SIZE), 0)
  ImageDraw.Draw(mask).polygon([itop, iright, ibottom, ileft], fill=255)
  layer.alpha_composite(Image.composite(weave, Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0)), mask))

  # cloth outline and stitched hem
  d.line([top, right, bottom, left, top], fill=CLOTH_EDGE, width=3)
  d.line([top, left], fill=THREAD, width=4)
  d.line([bottom, right], fill=THREAD, width=4)

  # needle and thread poking out of the right corner
  d.line([(right[0] - 6, right[1] - 4), (right[0] + 52, right[1] - 62)], fill=NEEDLE_SHADE, width=7)
  d.line([(right[0] - 2, right[1] - 8), (right[0] + 50, right[1] - 60)], fill=NEEDLE, width=4)
  d.line([(right[0] + 42, right[1] - 54), (right[0] + 60, right[1] - 36), (right[0] + 40, right[1] - 26)],
         fill=THREAD, width=4)
  return layer


def build() -> Image.Image:
  im = background()

  sign = Image.open(SIGN).convert("RGBA")
  width = 456
  height = round(sign.height * width / sign.width)
  sign = sign.resize((width, height), Image.Resampling.LANCZOS)
  im.alpha_composite(sign, ((SIZE - width) // 2, 52))

  # soft shadow under the cloth
  shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
  ImageDraw.Draw(shadow).ellipse([146, 366, 400, 462], fill=(0, 0, 0, 110))
  im.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(14)))

  im.alpha_composite(cloth_layer(SIZE // 2, 336, 118))
  return im


def main() -> None:
  icon = build()
  for size, out in ((512, ROOT / "docs/modrinth/icon.png"), (128, ROOT / "src/main/resources/icon.png")):
    out.parent.mkdir(parents=True, exist_ok=True)
    resized = icon if size == 512 else icon.resize((size, size), Image.Resampling.LANCZOS)
    resized.save(out)
    print(f"wrote {out.relative_to(ROOT)} ({size}x{size})")


if __name__ == "__main__":
  main()
