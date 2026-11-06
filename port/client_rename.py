#!/usr/bin/env python3
"""Mechanical Mojang -> yarn rename for the library/client slice.

Rule: an import line is rewritten; if the simple class name changes, every
standalone occurrence of that simple name in the same file is renamed too.
Nothing else is touched.
"""
import os
import re
import sys

ROOT = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java\slimeknights\tconstruct\library\client"

# mojang FQCN -> yarn FQCN
IMPORTS = {
    "net.minecraft.resources.ResourceLocation": "net.minecraft.util.Identifier",
    "net.minecraft.client.resources.model.Material": "net.minecraft.client.util.SpriteIdentifier",
    "net.minecraft.client.renderer.texture.TextureAtlasSprite": "net.minecraft.client.texture.Sprite",
    "net.minecraft.world.item.ItemStack": "net.minecraft.item.ItemStack",
    "net.minecraft.client.renderer.block.model.BakedQuad": "net.minecraft.client.render.model.BakedQuad",
    "net.minecraft.util.GsonHelper": "net.minecraft.util.JsonHelper",
    "net.minecraft.MethodsReturnNonnullByDefault": "net.minecraft.util.annotation.MethodsReturnNonnullByDefault",
    "net.minecraft.client.Minecraft": "net.minecraft.client.MinecraftClient",
    "com.mojang.blaze3d.platform.NativeImage": "net.minecraft.client.texture.NativeImage",
    "net.minecraft.world.inventory.InventoryMenu": "net.minecraft.screen.PlayerScreenHandler",
    "net.minecraft.world.entity.LivingEntity": "net.minecraft.entity.LivingEntity",
    "net.minecraft.network.chat.Component": "net.minecraft.text.Text",
    "net.minecraft.core.RegistryAccess": "net.minecraft.registry.DynamicRegistryManager",
    "com.mojang.blaze3d.vertex.PoseStack": "net.minecraft.client.util.math.MatrixStack",
    "net.minecraft.server.packs.resources.ResourceManager": "net.minecraft.resource.ResourceManager",
    "net.minecraft.nbt.CompoundTag": "net.minecraft.nbt.NbtCompound",
    "net.minecraft.client.multiplayer.ClientLevel": "net.minecraft.client.world.ClientWorld",
    "net.minecraft.client.resources.model.BakedModel": "net.minecraft.client.render.model.BakedModel",
    "net.minecraft.client.renderer.texture.MissingTextureAtlasSprite": "net.minecraft.client.texture.MissingSprite",
    "net.minecraft.client.renderer.block.model.ItemOverrides": "net.minecraft.client.render.model.json.ModelOverrideList",
    "net.minecraft.world.level.Level": "net.minecraft.world.World",
    "net.minecraft.world.item.Item": "net.minecraft.item.Item",
    "net.minecraft.client.gui.GuiGraphics": "net.minecraft.client.gui.DrawContext",
    "net.minecraft.client.resources.model.ModelState": "net.minecraft.client.render.model.ModelBakeSettings",
    "net.minecraft.client.resources.model.ModelBaker": "net.minecraft.client.render.model.Baker",
    "net.minecraft.client.renderer.RenderType": "net.minecraft.client.render.RenderLayer",
    "net.minecraft.world.level.material.Fluid": "net.minecraft.fluid.Fluid",
    "net.minecraft.core.Direction": "net.minecraft.util.math.Direction",
    "net.minecraft.client.renderer.block.model.BlockElement": "net.minecraft.client.render.model.json.ModelElement",
    "net.minecraft.client.renderer.MultiBufferSource": "net.minecraft.client.render.VertexConsumerProvider",
    "net.minecraft.client.gui.Font": "net.minecraft.client.font.TextRenderer",
    "net.minecraft.world.level.block.state.BlockState": "net.minecraft.block.BlockState",
    "net.minecraft.world.item.ArmorItem": "net.minecraft.item.ArmorItem",
    "net.minecraft.world.entity.EquipmentSlot": "net.minecraft.entity.EquipmentSlot",
    "net.minecraft.util.RandomSource": "net.minecraft.util.math.random.Random",
    "net.minecraft.tags.TagKey": "net.minecraft.registry.tag.TagKey",
    "net.minecraft.core.registries.Registries": "net.minecraft.registry.RegistryKeys",
    "net.minecraft.client.resources.model.UnbakedModel": "net.minecraft.client.render.model.UnbakedModel",
    "net.minecraft.client.resources.language.I18n": "net.minecraft.client.resource.language.I18n",
    "com.mojang.blaze3d.vertex.VertexConsumer": "net.minecraft.client.render.VertexConsumer",
    "net.minecraft.world.level.block.Block": "net.minecraft.block.Block",
    "net.minecraft.world.level.ItemLike": "net.minecraft.item.ItemConvertible",
    "net.minecraft.world.item.Items": "net.minecraft.item.Items",
    "net.minecraft.util.profiling.ProfilerFiller": "net.minecraft.util.profiler.Profiler",
    "net.minecraft.util.Mth": "net.minecraft.util.math.MathHelper",
    "net.minecraft.util.FastColor": "net.minecraft.util.math.ColorHelper",
    "net.minecraft.network.chat.TextColor": "net.minecraft.text.TextColor",
    "net.minecraft.client.resources.model.SimpleBakedModel": "net.minecraft.client.render.model.BasicBakedModel",
    "net.minecraft.client.model.HumanoidModel": "net.minecraft.client.render.entity.model.BipedEntityModel",
    "net.minecraft.ChatFormatting": "net.minecraft.util.Formatting",
    "net.minecraft.world.phys.Vec2": "net.minecraft.util.math.Vec2f",
    "net.minecraft.world.level.block.Blocks": "net.minecraft.block.Blocks",
    "net.minecraft.world.item.armortrim.TrimMaterial": "net.minecraft.item.trim.ArmorTrimMaterial",
    "net.minecraft.world.item.armortrim.TrimPattern": "net.minecraft.item.trim.ArmorTrimPattern",
    "net.minecraft.world.item.UseAnim": "net.minecraft.util.UseAction",
    "net.minecraft.world.item.ItemDisplayContext": "net.minecraft.client.render.model.json.ModelTransformationMode",
    "net.minecraft.world.InteractionHand": "net.minecraft.util.Hand",
    "net.minecraft.server.packs.resources.Resource": "net.minecraft.resource.Resource",
    "net.minecraft.server.packs.PackType": "net.minecraft.resource.ResourceType",
    "net.minecraft.resources.ResourceKey": "net.minecraft.registry.RegistryKey",
    "net.minecraft.network.FriendlyByteBuf": "net.minecraft.network.PacketByteBuf",
    "net.minecraft.core.registries.BuiltInRegistries": "net.minecraft.registry.Registries",
    "net.minecraft.core.particles.SimpleParticleType": "net.minecraft.particle.DefaultParticleType",
    "net.minecraft.core.Holder": "net.minecraft.registry.entry.RegistryEntry",
    "net.minecraft.client.renderer.texture.TextureAtlas": "net.minecraft.client.texture.SpriteAtlasTexture",
    "net.minecraft.client.renderer.block.model.BlockElementFace": "net.minecraft.client.render.model.json.ModelElementFace",
    "net.minecraft.client.particle.ParticleProvider": "net.minecraft.client.particle.ParticleFactory",
    "net.minecraft.Util": "net.minecraft.util.Util",
    "com.mojang.math.Axis": "net.minecraft.util.math.RotationAxis",
    "net.minecraft.world.level.block.entity.BlockEntity": "net.minecraft.block.entity.BlockEntity",
    "net.minecraft.world.level.block.entity.BannerPattern": "net.minecraft.block.entity.BannerPattern",
    "net.minecraft.world.item.crafting.Ingredient": "net.minecraft.recipe.Ingredient",
    "net.minecraft.world.item.crafting.Recipe": "net.minecraft.recipe.Recipe",
    "net.minecraft.world.item.crafting.RecipeType": "net.minecraft.recipe.RecipeType",
    "net.minecraft.world.item.alchemy.Potions": "net.minecraft.potion.Potions",
    "net.minecraft.world.item.alchemy.PotionUtils": "net.minecraft.potion.PotionUtil",
    "net.minecraft.world.item.alchemy.Potion": "net.minecraft.potion.Potion",
    "net.minecraft.world.inventory.CraftingContainer": "net.minecraft.inventory.CraftingInventory",
    "net.minecraft.world.entity.player.Player": "net.minecraft.entity.player.PlayerEntity",
    "net.minecraft.world.entity.HumanoidArm": "net.minecraft.util.Arm",
    "net.minecraft.sounds.SoundEvents": "net.minecraft.sound.SoundEvents",
    "net.minecraft.network.chat.MutableComponent": "net.minecraft.text.MutableText",
    "net.minecraft.nbt.ListTag": "net.minecraft.nbt.NbtList",
    "net.minecraft.nbt.Tag": "net.minecraft.nbt.NbtElement",
    "net.minecraft.core.Registry": "net.minecraft.registry.Registry",
    "net.minecraft.client.sounds.SoundManager": "net.minecraft.client.sound.SoundManager",
    "net.minecraft.client.resources.sounds.SimpleSoundInstance": "net.minecraft.client.sound.PositionedSoundInstance",
    "net.minecraft.client.renderer.entity.ItemRenderer": "net.minecraft.client.render.item.ItemRenderer",
    "net.minecraft.client.renderer.LightTexture": "net.minecraft.client.render.LightmapTextureManager",
    "net.minecraft.client.renderer.GameRenderer": "net.minecraft.client.render.GameRenderer",
    "net.minecraft.client.player.LocalPlayer": "net.minecraft.client.network.ClientPlayerEntity",
    "net.minecraft.client.particle.TextureSheetParticle": "net.minecraft.client.particle.SpriteBillboardParticle",
    "net.minecraft.client.particle.SpriteSet": "net.minecraft.client.particle.SpriteProvider",
    "net.minecraft.client.particle.ParticleRenderType": "net.minecraft.client.particle.ParticleTextureSheet",
    "net.minecraft.client.particle.BreakingItemParticle": "net.minecraft.client.particle.CrackParticle",
    "net.minecraft.client.particle.Particle": "net.minecraft.client.particle.Particle",
    "net.minecraft.client.model.geom.ModelLayers": "net.minecraft.client.render.entity.model.EntityModelLayers",
    "net.minecraft.client.model.HumanoidModel.ArmPose": "net.minecraft.client.render.entity.model.BipedEntityModel.ArmPose",
    "net.minecraft.client.model.EntityModel": "net.minecraft.client.render.entity.model.EntityModel",
    "net.minecraft.client.model.ElytraModel": "net.minecraft.client.render.entity.model.ElytraEntityModel",
    "net.minecraft.client.model.geom.ModelPart": "net.minecraft.client.model.ModelPart",
    "net.minecraft.client.gui.screens.inventory.AbstractContainerScreen": "net.minecraft.client.gui.screen.ingame.HandledScreen",
    "net.minecraft.ResourceLocationException": "net.minecraft.util.InvalidIdentifierException",
    "net.minecraft.client.renderer.Sheets": "net.minecraft.client.texture.SpriteAtlasTexture",
    "net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener": "net.minecraft.resource.JsonDataLoader",
    "net.minecraft.client.model.Model": "net.minecraft.client.model.Model",
    "net.minecraft.world.phys.AABB": "net.minecraft.util.math.Box",
    "net.minecraft.world.level.block.Rotation": "net.minecraft.util.BlockRotation",
    "net.minecraft.world.item.DyeColor": "net.minecraft.util.DyeColor",
    "net.minecraft.world.entity.Entity": "net.minecraft.entity.Entity",
    "net.minecraft.world.level.LevelAccessor": "net.minecraft.world.WorldAccess",
}

# extra simple-name renames independent of imports (only applied when the
# file does not import the class explicitly, e.g. used fully qualified)
EXTRA = []


def rename(path):
    with open(path, encoding="utf-8") as fh:
        src = fh.read()
    out = src
    changed = []
    for old, new in IMPORTS.items():
        m = re.search(r"^import " + re.escape(old) + r";$", out, re.M)
        if not m:
            continue
        out = out[: m.start()] + "import " + new + ";" + out[m.end():]
        old_simple = old.rsplit(".", 1)[1]
        new_simple = new.rsplit(".", 1)[1]
        if old_simple != new_simple:
            out, n = re.subn(r"(?<![\w.])" + re.escape(old_simple) + r"\b", new_simple, out)
            changed.append(f"{old_simple}->{new_simple}({n})")
    if out != src:
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write(out)
    return changed


def main():
    only = sys.argv[1:] or None
    total = 0
    for dirpath, _dirs, files in os.walk(ROOT):
        for name in sorted(files):
            if not name.endswith(".java"):
                continue
            path = os.path.join(dirpath, name)
            rel = os.path.relpath(path, ROOT).replace("\\", "/")
            if rel in ("Icons.java", "GuiUtil.java"):
                continue
            if only and rel not in only:
                continue
            changed = rename(path)
            if changed:
                total += 1
                print(f"{rel}: {', '.join(changed)}")
    print(f"-- files changed: {total}")


if __name__ == "__main__":
    main()
