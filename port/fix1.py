import io, sys, os
ROOT = r"tconstruct-fabric/src/main/java/slimeknights/tconstruct/library/recipe"

EDITS = [
 # ITinkerableContainer
 ("ITinkerableContainer.java", [
   ("  default ItemStack getItem(int index) {", "  default ItemStack getStack(int index) {"),
   ("  default int getContainerSize() {", "  default int size() {"),
   ("          stack.shrink(amount);", "          stack.decrement(amount);"),
   ("        ItemStack container = stack.getCraftingRemainingItem();",
    "        // TODO(fabric): yarn has Item#getRecipeRemainder/hasRecipeRemainder\n"
    "        ItemStack container = stack.getItem().hasRecipeRemainder() ? new ItemStack(stack.getItem().getRecipeRemainder()) : ItemStack.EMPTY;"),
 ]),
 # IMoldingContainer
 ("molding/IMoldingContainer.java", [
   ("  default ItemStack getItem(int index) {", "  default ItemStack getStack(int index) {"),
   ("  default int getContainerSize() {", "  default int size() {"),
 ]),
 # IPartBuilderContainer
 ("partbuilder/IPartBuilderContainer.java", [
   ("  default ItemStack getItem(int index) {", "  default ItemStack getStack(int index) {"),
   ("  default int getContainerSize() {", "  default int size() {"),
 ]),
 # ShapedMaterialRecipe
 ("material/ShapedMaterialRecipe.java", [
   ("inventory.getContainerSize()", "inventory.size()"),
   ("inventory.getItem(i)", "inventory.getStack(i)"),
 ]),
 ("material/ShapedMaterialsRecipe.java", [
   ("inventory.getContainerSize()", "inventory.size()"),
   ("inventory.getItem(i)", "inventory.getStack(i)"),
 ]),
 ("modifiers/adding/OverslimeCraftingTableRecipe.java", [
   ("inv.getContainerSize()", "inv.size()"),
   ("inv.getItem(i)", "inv.getStack(i)"),
   ("      if (stack.hasCraftingRemainingItem()) {\n        list.set(i, stack.getCraftingRemainingItem());\n      }",
    "      // TODO(fabric): yarn has Item#getRecipeRemainder/hasRecipeRemainder\n"
    "      if (stack.getItem().hasRecipeRemainder()) {\n"
    "        list.set(i, new ItemStack(stack.getItem().getRecipeRemainder()));\n      }"),
 ]),
 # Potion casting: NBT accessors + registry
 ("casting/PotionCastingRecipe.java", [
   ("import net.minecraftforge.registries.ForgeRegistries;\n", ""),
   ("result.setTag(inv.getFluidTag());", "result.setNbt(inv.getFluidTag());"),
   ("result.getTag()", "result.getNbt()"),
   ("Collection<Potion> potions = ForgeRegistries.POTIONS.getValues();",
    "Collection<Potion> potions = Registries.POTION.stream().toList();"),
 ]),
 # CastingRecipeLookup: Mantle ItemOutput#getTag missing in 1.9.291
 ("casting/CastingRecipeLookup.java", [
   ("    TagKey<Item> tag = output.getTag();",
    "    // TODO(fabric): Mantle 1.9.291 ItemOutput has no getTag(), arrives with Mantle 1.11\n    TagKey<Item> tag = output.getTag();"),
 ]),
 # InstrumentIngredient
 ("ingredient/InstrumentIngredient.java", [
   ("    NbtCompound tag = stack.getTag();", "    NbtCompound tag = stack.getNbt();"),
 ]),
 # ToolHookIngredient: setCustomName is void in yarn
 ("ingredient/ToolHookIngredient.java", [
   ("        list.add(new ItemStack(Blocks.BARRIER).setHoverName(Text.literal(\"Empty Tag: \" + tag.location())));",
    "        // TODO(fabric): yarn ItemStack#setCustomName returns void\n"
    "        ItemStack barrier = new ItemStack(Blocks.BARRIER);\n"
    "        barrier.setCustomName(Text.literal(\"Empty Tag: \" + tag.location()));\n"
    "        list.add(barrier);"),
 ]),
]

failed = False
for rel, subs in EDITS:
    path = os.path.join(ROOT, rel)
    with io.open(path, encoding="utf-8") as f:
        src = f.read()
    orig = src
    for old, new in subs:
        if old not in src:
            print("MISS %s : %r" % (rel, old[:70]))
            failed = True
            continue
        src = src.replace(old, new)
    if src != orig:
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(src)
        print("ok", rel)
sys.exit(1 if failed else 0)
