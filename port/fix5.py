import io, os

ROOT = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java\slimeknights\tconstruct\library\recipe"

EDITS = {
    os.path.join("casting", "ICastingRecipe.java"): [
        ("return calcCoolingTime(getTemperature(fluid), fluid.getAmount());",
         "// TODO(fabric): Porting Lib FluidStack#getAmount() returns long\n    return calcCoolingTime(getTemperature(fluid), (int) fluid.getAmount());"),
    ],
    os.path.join("casting", "material", "CompositeCastingRecipe.java"): [
        ("int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), fluid.getAmount());",
         "int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), (int) fluid.getAmount());"),
    ],
    os.path.join("casting", "material", "MaterialCastingLookup.java"): [
        ("return ICastingRecipe.calcCoolingTime(temperature, fluid.getAmount());",
         "return ICastingRecipe.calcCoolingTime(temperature, (int) fluid.getAmount());"),
    ],
    os.path.join("casting", "material", "MaterialCastingRecipe.java"): [
        ("int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), fluid.getAmount());",
         "int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), (int) fluid.getAmount());"),
    ],
    os.path.join("casting", "material", "PartSwapCastingRecipe.java"): [
        ("int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), fluid.getAmount());",
         "int time = ICastingRecipe.calcCoolingTime(recipe.getTemperature(), (int) fluid.getAmount());"),
    ],
    os.path.join("melting", "DisplayMeltingRecipe.java"): [
        ("return IMeltingRecipe.calcTimeForAmount(temperature, fluid.getAmount());",
         "return IMeltingRecipe.calcTimeForAmount(temperature, (int) fluid.getAmount());"),
        (".mapToInt(fluid -> IMeltingRecipe.calcTimeForAmount(temperature, fluid.getAmount()))",
         ".mapToInt(fluid -> IMeltingRecipe.calcTimeForAmount(temperature, (int) fluid.getAmount()))"),
    ],
    os.path.join("melting", "MaterialMeltingRecipe.java"): [
        ("return IMeltingRecipe.calcTimeForAmount(temperature, result.getAmount() * cost);",
         "return IMeltingRecipe.calcTimeForAmount(temperature, (int) (result.getAmount() * cost));"),
    ],
    os.path.join("melting", "IMeltingContainer.java"): [
        ("return new FluidStack(fluid, applyOreBoost(rate, fluid.getAmount()));",
         "return new FluidStack(fluid, applyOreBoost(rate, (int) fluid.getAmount()));"),
    ],
}

for rel, pairs in EDITS.items():
    p = os.path.join(ROOT, rel)
    s = orig = io.open(p, encoding='utf-8').read()
    for a, b in pairs:
        if a not in s:
            print("  MISS", rel, "|", a[:60])
        s = s.replace(a, b)
    if s != orig:
        io.open(p, 'w', encoding='utf-8', newline='\n').write(s)
        print("  fixed", rel)
