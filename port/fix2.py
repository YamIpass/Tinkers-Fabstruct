import io, os, sys
ROOT = r"tconstruct-fabric/src/main/java/slimeknights/tconstruct/library/recipe"

EDITS = [
 ("FluidValues.java", [
   ("import net.minecraftforge.fluids.FluidType;\n", ""),
   ("  // soup\n  /** Value of a single bowl of soup */",
    "  /** TODO(fabric): Forge's FluidType.BUCKET_VOLUME does not exist in Porting Lib 2.3.14; a bucket is 1000 mB */\n"
    "  public static final int BUCKET_VOLUME = 1000;\n\n"
    "  // soup\n"
    "  /** Value of a single bowl of soup */"),
   ("ICHOR_BLAZING_BLOOD = FluidType.BUCKET_VOLUME / 100;", "ICHOR_BLAZING_BLOOD = BUCKET_VOLUME / 100;"),
 ]),
 ("melting/IMeltingRecipe.java", [
   ("import net.minecraftforge.fluids.capability.IFluidHandler;",
    "// TODO(fabric): no IFluidHandler equivalent in Porting Lib 2.3.14 (fabric-transfer FluidStorage is not API compatible)\nimport net.minecraftforge.fluids.capability.IFluidHandler;"),
   ("    return fluid.getFluidType().getTemperature() - 300;",
    "    // TODO(fabric): Fluid#getFluidType is injected by Porting Lib's FluidExtension mixin\n    return ((io.github.fabricators_of_create.porting_lib.fluids.extensions.FluidExtension) fluid).getFluidType().getTemperature() - 300;"),
   ("    return fluid.getFluid().getFluidType().getTemperature(fluid) - 300;",
    "    return ((io.github.fabricators_of_create.porting_lib.fluids.extensions.FluidExtension) fluid.getFluid()).getFluidType().getTemperature(fluid) - 300;"),
 ]),
 ("melting/DamageableMeltingRecipe.java", [
   ("    int amount = fluid.getAmount() * (maxDamage - damage) / maxDamage;",
    "    int amount = (int)(fluid.getAmount() * (maxDamage - damage) / maxDamage);"),
 ]),
 ("casting/container/ContainerFillingRecipe.java", [
   ("import net.minecraftforge.common.capabilities.ForgeCapabilities;",
    "// TODO(fabric): Forge item fluid capability (FLUID_HANDLER_ITEM) has no Porting Lib 2.3.14 equivalent; use fabric-transfer FluidStorage.ITEM when the casting slice is ported\nimport net.minecraftforge.common.capabilities.ForgeCapabilities;"),
   ("import net.minecraftforge.registries.ForgeRegistries;\n", ""),
   ("ForgeRegistries.FLUIDS.getValues().stream()", "Registries.FLUID.stream()"),
   ("import net.minecraft.item.Items;", "import net.minecraft.item.Items;\nimport net.minecraft.registry.Registries;"),
 ]),
 ("alloying/AlloyRecipe.java", [
   ("        int amount = ingredient.fluid.getAmount(fluid.getFluid());",
    "        // TODO(fabric): Mantle 1.9.291 FluidIngredient#getAmount returns long\n"
    "        int amount = (int)ingredient.fluid.getAmount(fluid.getFluid());"),
 ]),
 ("casting/ItemCastingRecipe.java", [
   ("    return this.fluid.getAmount(inv.getFluid());",
    "    // TODO(fabric): Mantle 1.9.291 FluidIngredient#getAmount returns long\n"
    "    return (int)this.fluid.getAmount(inv.getFluid());"),
 ]),
 ("casting/material/MaterialFluidRecipe.java", [
   ("    return this.fluid.getAmount(fluid);",
    "    // TODO(fabric): Mantle 1.9.291 FluidIngredient#getAmount returns long\n"
    "    return (int)this.fluid.getAmount(fluid);"),
 ]),
 ("casting/PotionCastingRecipe.java", [
   ("    return fluid.getAmount(inv.getFluid());",
    "    // TODO(fabric): Mantle 1.9.291 FluidIngredient#getAmount returns long\n"
    "    return (int)fluid.getAmount(inv.getFluid());"),
 ]),
 ("fuel/MeltingFuel.java", [
   ("  public int getAmount(Fluid fluid) {\n    return input.getAmount(fluid);",
    "  public int getAmount(Fluid fluid) {\n    // TODO(fabric): Mantle 1.9.291 FluidIngredient#getAmount returns long\n    return (int)input.getAmount(fluid);"),
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
            print("MISS %s : %r" % (rel, old[:80]))
            failed = True
            continue
        src = src.replace(old, new, 1)
    if src != orig:
        with io.open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(src)
        print("ok", rel)
sys.exit(1 if failed else 0)
