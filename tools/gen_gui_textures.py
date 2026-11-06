"""Generate GUI backgrounds for the ported table screens and the melter.

The upstream GUI sheets are Mantle palette-index textures: the Forge renderer
recolours them through a palette, so a plain Fabric Screen draws them as a flat
grey mess.  Rather than port the palette machinery, draw a fresh sheet that lines
up with the slot coordinates the port actually uses.

Coordinates are duplicated from TableType / MelterMenu; keep them in sync.
"""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources" / "assets" / "tconstruct" / "textures" / "gui" / "port"

PANEL = (198, 198, 198, 255)
PANEL_DARK = (139, 139, 139, 255)
BORDER = (55, 55, 55, 255)
BEVEL = (255, 255, 255, 255)
SHADOW = (85, 85, 85, 255)
WELL = (139, 139, 139, 255)
TRACK = (55, 55, 55, 255)
ACCENT = (159, 132, 77, 255)
ACCENT_DARK = (126, 98, 55, 255)


def slot(draw, x, y):
    """One 18x18 slot well, top left corner at x, y."""
    draw.rectangle([x, y, x + 17, y + 17], fill=WELL)
    draw.line([x, y, x + 17, y], fill=TRACK)
    draw.line([x, y, x, y + 17], fill=TRACK)
    draw.line([x + 1, y + 17, x + 17, y + 17], fill=BEVEL)
    draw.line([x + 17, y + 1, x + 17, y + 17], fill=BEVEL)


def panel(width, height):
    image = Image.new("RGBA", (width, height), PANEL)
    draw = ImageDraw.Draw(image)
    draw.rectangle([0, 0, width - 1, height - 1], outline=BORDER)
    draw.line([1, 1, width - 2, 1], fill=BEVEL)
    draw.line([1, 1, 1, height - 2], fill=BEVEL)
    draw.line([2, height - 2, width - 2, height - 2], fill=SHADOW)
    draw.line([width - 2, 2, width - 2, height - 2], fill=SHADOW)
    return image, draw


def player_inventory(draw, inventory_y):
    for row in range(3):
        for column in range(9):
            slot(draw, 8 + column * 18, inventory_y + row * 18)
    for column in range(9):
        slot(draw, 8 + column * 18, inventory_y + 58)


def arrow(draw, x, y, length=22):
    """Right pointing crafting arrow."""
    draw.rectangle([x, y + 5, x + length - 6, y + 10], fill=ACCENT_DARK)
    draw.polygon(
        [(x + length - 7, y), (x + length, y + 8), (x + length - 7, y + 15)],
        fill=ACCENT,
    )


def table(name, slots, result, height=184, inventory_y=102):
    image, draw = panel(176, height)
    for slot_x, slot_y in slots:
        slot(draw, slot_x, slot_y)
    slot(draw, result[0], result[1])
    player_inventory(draw, inventory_y)
    image.save(OUT / f"{name}.png")


def part_builder():
    image, draw = panel(176, 184)
    slot(draw, 8, 43)
    slot(draw, 29, 43)
    # preview well the upstream sheet has in the middle of the panel
    draw.rectangle([52, 20, 108, 66], fill=PANEL_DARK, outline=TRACK)
    arrow(draw, 114, 42, 26)
    slot(draw, 148, 42)
    player_inventory(draw, 102)
    image.save(OUT / "part_builder.png")


def tinker_station():
    image, draw = panel(176, 184)
    slot(draw, 53, 41)  # tool slot, station slot 0
    slot(draw, 11, 41)
    slot(draw, 31, 30)
    slot(draw, 31, 50)
    arrow(draw, 74, 41, 30)
    slot(draw, 114, 38)
    player_inventory(draw, 102)
    image.save(OUT / "tinker_station.png")


def modifier_worktable():
    image, draw = panel(176, 184)
    slot(draw, 8, 21)  # tool slot
    slot(draw, 8, 45)
    slot(draw, 8, 67)
    draw.rectangle([30, 21, 104, 84], fill=PANEL_DARK, outline=TRACK)
    arrow(draw, 108, 58, 14)
    slot(draw, 125, 42)
    player_inventory(draw, 102)
    image.save(OUT / "modifier_worktable.png")


def melter():
    image, draw = panel(176, 166)
    slot(draw, 26, 20)
    slot(draw, 26, 50)
    # fuel gauge next to the fuel slot
    draw.rectangle([47, 20, 60, 66], outline=TRACK, fill=PANEL_DARK)
    # smelting progress arrow pointing right into the tank gauge
    draw.rectangle([64, 32, 84, 41], fill=TRACK)
    draw.rectangle([64, 32, 70, 41], fill=ACCENT)
    draw.polygon([(84, 26), (94, 36), (84, 46)], fill=ACCENT)
    # molten metal gauge on the right edge of the panel
    draw.rectangle([96, 18, 114, 70], outline=TRACK, fill=SHADOW)
    draw.rectangle([98, 20, 112, 68], fill=TRACK)
    player_inventory(draw, 84)
    image.save(OUT / "melter.png")


def crafting_station():
    image, draw = panel(176, 166)
    for row in range(3):
        for column in range(3):
            slot(draw, 30 + column * 18, 17 + row * 18)
    arrow(draw, 92, 31, 26)
    slot(draw, 124, 35)
    player_inventory(draw, 84)
    image.save(OUT / "crafting_station.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    crafting_station()
    part_builder()
    tinker_station()
    modifier_worktable()
    melter()
    print("wrote", len(list(OUT.glob("*.png"))), "gui sheets to", OUT)


if __name__ == "__main__":
    main()
