#!/usr/bin/env python3
"""Mechanical Mojang->yarn / Forge->Fabric rename pass for library/recipe/** (throwaway helper)."""
import pathlib
import re
import sys

ROOT = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")

# Fully qualified class renames (Mojang / Forge -> yarn / Fabric), longest first.
FQN = [
    # Forge -> Fabric
    ("net.minecraftforge.fluids.FluidStack", "io.github.fabricators_of_create.porting_lib.fluids.FluidStack"),
    ("net.minecraftforge.registries.RegistryObject", "io.github.fabricators_of_create.porting_lib.util.RegistryObject"),
    ("net.minecraftforge.items.ItemHandlerHelper", "io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper"),
    ("net.minecraftforge.common.util.Lazy", "io.github.fabricators_of_create.porting_lib.common.util.Lazy"),
    # Minecraft core
    ("net.minecraft.ChatFormatting", "net.minecraft.util.Formatting"),
    ("net.minecraft.MethodsReturnNonnullByDefault", "net.minecraft.util.annotation.MethodsReturnNonnullByDefault"),
    ("net.minecraft.core.Holder", "net.minecraft.registry.entry.RegistryEntry"),
    ("net.minecraft.core.NonNullList", "net.minecraft.util.collection.DefaultedList"),
    ("net.minecraft.core.RegistryAccess", "net.minecraft.registry.DynamicRegistryManager"),
    ("net.minecraft.core.registries.BuiltInRegistries", "net.minecraft.registry.Registries"),
    ("net.minecraft.core.registries.Registries", "net.minecraft.registry.RegistryKeys"),
    ("net.minecraft.data.recipes.FinishedRecipe", "net.minecraft.data.server.recipe.RecipeJsonProvider"),
    ("net.minecraft.nbt.CompoundTag", "net.minecraft.nbt.NbtCompound"),
    ("net.minecraft.nbt.ListTag", "net.minecraft.nbt.NbtList"),
    ("net.minecraft.nbt.NbtUtils", "net.minecraft.nbt.NbtHelper"),
    ("net.minecraft.nbt.StringTag", "net.minecraft.nbt.NbtString"),
    ("net.minecraft.nbt.Tag", "net.minecraft.nbt.NbtElement"),
    ("net.minecraft.network.FriendlyByteBuf", "net.minecraft.network.PacketByteBuf"),
    ("net.minecraft.network.chat.MutableComponent", "net.minecraft.text.MutableText"),
    ("net.minecraft.network.chat.Component", "net.minecraft.text.Text"),
    ("net.minecraft.resources.ResourceKey", "net.minecraft.registry.RegistryKey"),
    ("net.minecraft.resources.ResourceLocation", "net.minecraft.util.Identifier"),
    ("net.minecraft.tags.TagKey", "net.minecraft.registry.tag.TagKey"),
    ("net.minecraft.util.GsonHelper", "net.minecraft.util.JsonHelper"),
    ("net.minecraft.world.Container", "net.minecraft.inventory.Inventory"),
    ("net.minecraft.world.entity.EntityType", "net.minecraft.entity.EntityType"),
    ("net.minecraft.world.entity.Entity", "net.minecraft.entity.Entity"),
    ("net.minecraft.world.entity.LivingEntity", "net.minecraft.entity.LivingEntity"),
    ("net.minecraft.world.inventory.CraftingContainer", "net.minecraft.inventory.RecipeInputInventory"),
    ("net.minecraft.world.item.BlockItem", "net.minecraft.item.BlockItem"),
    ("net.minecraft.world.item.Instrument", "net.minecraft.item.Instrument"),
    ("net.minecraft.world.item.ItemStack", "net.minecraft.item.ItemStack"),
    ("net.minecraft.world.item.Item", "net.minecraft.item.Item"),
    ("net.minecraft.world.item.Items", "net.minecraft.item.Items"),
    ("net.minecraft.world.item.alchemy.PotionUtils", "net.minecraft.potion.PotionUtil"),
    ("net.minecraft.world.item.alchemy.Potions", "net.minecraft.potion.Potions"),
    ("net.minecraft.world.item.alchemy.Potion", "net.minecraft.potion.Potion"),
    ("net.minecraft.world.item.crafting.CraftingBookCategory", "net.minecraft.recipe.book.CraftingRecipeCategory"),
    ("net.minecraft.world.item.crafting.CraftingRecipe", "net.minecraft.recipe.CraftingRecipe"),
    ("net.minecraft.world.item.crafting.CustomRecipe", "net.minecraft.recipe.SpecialCraftingRecipe"),
    ("net.minecraft.world.item.crafting.Ingredient", "net.minecraft.recipe.Ingredient"),
    ("net.minecraft.world.item.crafting.RecipeSerializer", "net.minecraft.recipe.RecipeSerializer"),
    ("net.minecraft.world.item.crafting.RecipeManager", "net.minecraft.recipe.RecipeManager"),
    ("net.minecraft.world.item.crafting.RecipeType", "net.minecraft.recipe.RecipeType"),
    ("net.minecraft.world.item.crafting.Recipe", "net.minecraft.recipe.Recipe"),
    ("net.minecraft.world.item.crafting.ShapedRecipe", "net.minecraft.recipe.ShapedRecipe"),
    ("net.minecraft.world.item.crafting.ShapelessRecipe", "net.minecraft.recipe.ShapelessRecipe"),
    ("net.minecraft.world.level.ItemLike", "net.minecraft.item.ItemConvertible"),
    ("net.minecraft.world.level.Level", "net.minecraft.world.World"),
    ("net.minecraft.world.level.block.Blocks", "net.minecraft.block.Blocks"),
    ("net.minecraft.world.level.block.Block", "net.minecraft.block.Block"),
    ("net.minecraft.world.level.material.Fluid", "net.minecraft.fluid.Fluid"),
]

# Simple names used in code (word boundary), applied after FQN pass.
SIMPLE = [
    ("MutableComponent", "MutableText"),
    ("Component", "Text"),
    ("ResourceLocation", "Identifier"),
    ("CompoundTag", "NbtCompound"),
    ("ListTag", "NbtList"),
    ("StringTag", "NbtString"),
    ("NbtUtils", "NbtHelper"),
    ("RegistryAccess", "DynamicRegistryManager"),
    ("NonNullList", "DefaultedList"),
    ("ChatFormatting", "Formatting"),
    ("ResourceKey", "RegistryKey"),
    ("FriendlyByteBuf", "PacketByteBuf"),
    ("GsonHelper", "JsonHelper"),
    ("ItemLike", "ItemConvertible"),
    ("CraftingContainer", "RecipeInputInventory"),
    ("CraftingBookCategory", "CraftingRecipeCategory"),
    ("CustomRecipe", "SpecialCraftingRecipe"),
    ("PotionUtils", "PotionUtil"),
    ("Holder", "RegistryEntry"),
    ("CraftingContainer", "RecipeInputInventory"),
    ("Container", "Inventory"),
    ("Level", "World"),
]

# Method / constant level renames that are unambiguous in this slice.
METHODS = [
    ("getResultItem(", "getOutput("),
    ("getRemainingItems(", "getRemainder("),
    ("assemble(", "craft("),
    (".isSpecial()", ".isIgnoredInRecipeBook()"),
    ("PotionUtil.TAG_POTION", "PotionUtil.POTION_KEY"),
    ("Tag.TAG_STRING", "NbtElement.STRING_TYPE"),
    ("Tag.TAG_LIST", "NbtElement.LIST_TYPE"),
    ("Tag.TAG_COMPOUND", "NbtElement.COMPOUND_TYPE"),
    ("NbtElement.TAG_STRING", "NbtElement.STRING_TYPE"),
    ("NbtElement.TAG_LIST", "NbtElement.LIST_TYPE"),
]


def apply(text, pairs):
    for old, new in pairs:
        text = text.replace(old, new)
    return text


def word_replace(text, pairs):
    for old, new in pairs:
        text = re.sub(r"\b%s\b" % re.escape(old), new, text)
    return text


changed = 0
for path in sorted(ROOT.rglob("*.java")):
    original = path.read_text(encoding="utf-8")
    text = apply(original, FQN)
    text = word_replace(text, SIMPLE)
    text = apply(text, METHODS)
    if text != original:
        path.write_text(text, encoding="utf-8")
        changed += 1
print(f"rewrote {changed} files")
