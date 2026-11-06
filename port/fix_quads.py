"""Replaces Mantle's removed MantleItemLayerModel quad helpers with RenderUtils#getQuadsForSprite."""
import re
import sys
from pathlib import Path

ROOT = Path(r"C:/Users/YamIpass/Documents/deepseek-harness/default-workspace/tconstruct-fabric/src/main/java/slimeknights/tconstruct/library/client")

TODO = "        // TODO(fabric): Mantle removed MantleItemLayerModel; RenderUtils bakes untinted quads instead (no colour, emissivity or item layer pixels).\n"

REPL = [
    ("modifiers/DyedModifierModel.java",
     "      quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(0xFF000000 | data.getInt(key), -1, spriteGetter.apply(texture), transforms, 0, pixels));",
     "      Sprite sprite = spriteGetter.apply(texture);\n" + TODO +
     "      quadConsumer.accept(RenderUtils.getQuadsForSprite(sprite, transforms));"),
    ("modifiers/NormalModifierModel.java",
     "      quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(color, -1, spriteGetter.apply(spriteName), transforms, luminosity, pixels));",
     "      Sprite sprite = spriteGetter.apply(spriteName);\n" + TODO +
     "      quadConsumer.accept(RenderUtils.getQuadsForSprite(sprite, transforms));"),
    ("modifiers/PotionModifierModel.java",
     "            quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(0xFF000000 | PotionUtil.getColor(potion), -1, spriteGetter.apply(texture), transforms, 0, pixels));",
     "            Sprite sprite = spriteGetter.apply(texture);\n" + TODO +
     "            quadConsumer.accept(RenderUtils.getQuadsForSprite(sprite, transforms));"),
    ("modifiers/model/BannerModifierModel.java",
     "                quads.add(MantleItemLayerModel.getQuadForGui(color, -1, sprite, transforms, 0));",
     TODO + "                quads.addAll(RenderUtils.getQuadsForSprite(sprite, transforms));"),
    ("modifiers/model/MaterialModifierModel.java",
     "        quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(getColor(tool, modifier, sprite), -1, sprite.sprite(), transforms, sprite.emissivity(), pixels));",
     TODO + "        quadConsumer.accept(RenderUtils.getQuadsForSprite(sprite.sprite(), transforms));"),
    ("modifiers/model/TrimModifierModel.java",
     "          quadConsumer.accept(MantleItemLayerModel.getQuadsForSprite(texture.color, -1, texture.sprite, transforms, 0, pixels));",
     TODO + "          quadConsumer.accept(RenderUtils.getQuadsForSprite(texture.sprite, transforms));"),
]

apply = "--apply" in sys.argv
for rel, old, new in REPL:
    path = ROOT / rel
    text = path.read_text(encoding="utf-8")
    if old not in text:
        print(f"MISS {rel}")
        continue
    text = text.replace(old, new)
    text = text.replace("import slimeknights.mantle.client.model.util.MantleItemLayerModel;\n", "")
    if "import slimeknights.tconstruct.library.client.RenderUtils;" not in text:
        text = text.replace("import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;",
                            "import slimeknights.tconstruct.library.client.RenderUtils;\nimport slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;")
        if "import slimeknights.tconstruct.library.client.RenderUtils;" not in text:
            # fall back to inserting after the last net.minecraft import
            lines = text.split("\n")
            idx = max(i for i, l in enumerate(lines) if l.startswith("import net.minecraft."))
            lines.insert(idx + 1, "import slimeknights.tconstruct.library.client.RenderUtils;")
            text = "\n".join(lines)
    # ensure Sprite import where the new local is introduced
    if "Sprite sprite = " in text and "import net.minecraft.client.texture.Sprite;" not in text:
        lines = text.split("\n")
        idx = max(i for i, l in enumerate(lines) if l.startswith("import net.minecraft."))
        lines.insert(idx + 1, "import net.minecraft.client.texture.Sprite;")
        text = "\n".join(lines)
    if apply:
        path.write_text(text, encoding="utf-8")
        print(f"OK {rel}")
    else:
        print(f"DRY {rel}")
