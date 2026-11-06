"""Bakes the clear stained glass pane item icons.

The upstream item models use the ``mantle:item_layer`` loader with a per layer colour, which does not tint item layers
in this port yet. Instead of relying on that loader we multiply the shared pane texture by every colour and emit plain
``minecraft:item/generated`` models, which render correctly everywhere.
"""
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/tconstruct/models/item"
TEXTURES = ROOT / "src/main/resources/assets/tconstruct/textures"
PANE_TEXTURE = TEXTURES / "block/clear_stained_glass.png"
OUT_DIR = TEXTURES / "item/port"


def read_png(path):
    """minimal png reader for the rgba8 textures used by the mod"""
    data = path.read_bytes()
    pos = 8
    width = height = None
    idat = b""
    while pos < len(data):
        length = struct.unpack(">I", data[pos:pos + 4])[0]
        kind = data[pos + 4:pos + 8]
        payload = data[pos + 8:pos + 8 + length]
        if kind == b"IHDR":
            width, height, depth, color = struct.unpack(">IIBB", payload[:10])
            if depth != 8 or color != 6:
                raise ValueError(f"unsupported png format in {path}")
        elif kind == b"IDAT":
            idat += payload
        pos += 12 + length
    raw = zlib.decompress(idat)
    stride = width * 4
    rows = []
    prev = bytearray(stride)
    offset = 0
    for _ in range(height):
        filter_type = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride
        if filter_type == 1:
            for i in range(4, stride):
                line[i] = (line[i] + line[i - 4]) & 0xFF
        elif filter_type == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif filter_type == 3:
            for i in range(stride):
                left = line[i - 4] if i >= 4 else 0
                line[i] = (line[i] + ((left + prev[i]) >> 1)) & 0xFF
        elif filter_type == 4:
            for i in range(stride):
                left = line[i - 4] if i >= 4 else 0
                up = prev[i]
                up_left = prev[i - 4] if i >= 4 else 0
                estimate = left + up - up_left
                pa, pb, pc = abs(estimate - left), abs(estimate - up), abs(estimate - up_left)
                predictor = left if (pa <= pb and pa <= pc) else (up if pb <= pc else up_left)
                line[i] = (line[i] + predictor) & 0xFF
        rows.append(line)
        prev = line
    return width, height, rows


def write_png(path, width, height, rows):
    raw = b"".join(b"\x00" + bytes(row) for row in rows)

    def chunk(kind, payload):
        return (struct.pack(">I", len(payload)) + kind + payload
                + struct.pack(">I", zlib.crc32(kind + payload) & 0xFFFFFFFF))

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
                     + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))


def tint(rows, argb):
    red = (argb >> 16) & 0xFF
    green = (argb >> 8) & 0xFF
    blue = argb & 0xFF
    out = []
    for row in rows:
        line = bytearray(row)
        for i in range(0, len(line), 4):
            if line[i + 3] == 0:
                continue
            line[i] = (line[i] * red) // 255
            line[i + 1] = (line[i + 1] * green) // 255
            line[i + 2] = (line[i + 2] * blue) // 255
        out.append(line)
    return out


def main():
    width, height, rows = read_png(PANE_TEXTURE)
    count = 0
    for model in sorted(MODELS.glob("*_clear_stained_glass_pane.json")):
        data = json.loads(model.read_text(encoding="utf-8"))
        if data.get("loader") != "mantle:item_layer":
            continue
        color = 0xFFFFFFFF
        for layer in data.get("layers", []):
            value = layer.get("color")
            if isinstance(value, str):
                color = int(value, 16)
                break
        color_name = model.stem.replace("_clear_stained_glass_pane", "")
        texture = OUT_DIR / f"{color_name}_clear_glass_pane.png"
        write_png(texture, width, height, tint(rows, color))
        model.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"tconstruct:item/port/{color_name}_clear_glass_pane"},
        }, indent=2) + "\n", encoding="utf-8")
        count += 1
    print(f"baked {count} clear glass pane icons")


if __name__ == "__main__":
    main()
