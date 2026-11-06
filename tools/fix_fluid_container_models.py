"""Restores the fluid container item models that the resource bake flattened.

Upstream ships them with Forge's `tconstruct:fluid_container` geometry loader and a `forge:item/bucket_drip` parent.
Our bake turned them into plain `minecraft:item/generated` models without textures, so the buckets rendered invisible in
the creative tabs. The loader is registered by FluidClientEvents, so re-adding the loader plus a base texture and a mask
texture restores both the bucket shape and the tinted fluid layer.
"""
import io
import json
import os

from PIL import Image, ImageDraw

ASSETS = "src/main/resources/assets/tconstruct"
MODELS = os.path.join(ASSETS, "models", "item")
BASE = "minecraft:item/bucket"
MASK = "tconstruct:item/bucket_fluid_mask"
MASK_PATH = os.path.join(ASSETS, "textures", "item", "bucket_fluid_mask.png")


def write_mask():
    if os.path.exists(MASK_PATH):
        return
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    # inside of the vanilla bucket sprite, kept one pixel away from the rim
    draw.rectangle([2, 3, 13, 12], fill=(255, 255, 255, 255))
    os.makedirs(os.path.dirname(MASK_PATH), exist_ok=True)
    image.save(MASK_PATH)
    print("wrote", MASK_PATH)


def rewrite():
    write_mask()
    with_fluid = 0
    with_container = 0
    for name in sorted(os.listdir(MODELS)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(MODELS, name)
        with io.open(path, encoding="utf-8") as fp:
            model = json.load(fp)
        textures = model.get("textures", {})
        if "fluid" in model and not isinstance(model["fluid"], dict):
            # simple fluid bucket, the loader keeps the fluid name from the model
            model["loader"] = "tconstruct:fluid_container"
            model["parent"] = "minecraft:item/generated"
            model["textures"] = {"base": BASE, "fluid": MASK}
            with_fluid += 1
        elif "base" in textures and "fluid" in textures:
            # copper can and friends carry their own textures and pick the fluid from the stack
            model["loader"] = "tconstruct:fluid_container"
            model["parent"] = "minecraft:item/generated"
            with_container += 1
        else:
            continue
        model.pop("display", None)
        with io.open(path, "w", encoding="utf-8", newline="\n") as fp:
            json.dump(model, fp, indent=2)
            fp.write("\n")
    print("fluid buckets", with_fluid, "containers", with_container)


if __name__ == "__main__":
    rewrite()
