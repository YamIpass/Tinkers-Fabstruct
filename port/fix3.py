import io, os, re, sys

ROOT = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java\slimeknights\tconstruct\library\recipe"

fallback = "// TODO(fabric): Mantle 1.9.291 %s; upstream code kept verbatim for Mantle 1.11"

def read(p):
    with io.open(p, encoding='utf-8') as f:
        return f.read()

def write(p, s):
    with io.open(p, 'w', encoding='utf-8', newline='\n') as f:
        f.write(s)

def sub(p, pairs):
    s = read(p); orig = s
    for a, b in pairs:
        if a not in s:
            print("  MISS", os.path.relpath(p, ROOT), a)
        s = s.replace(a, b)
    if s != orig:
        write(p, s); print("  fixed", os.path.relpath(p, ROOT))

# 1. display crafting interface: yarn recipe book / category names
sub(os.path.join(ROOT, "tinkerstation", "IDisplayCraftingTinkering.java"), [
    ("default boolean isSpecial() {", "default boolean isIgnoredInRecipeBook() {"),
    ("default CraftingRecipeCategory category() {", "default CraftingRecipeCategory getCategory() {"),
])

# 2. datagen consumer wrappers: Forge FinishedRecipe API -> yarn RecipeJsonProvider API
for name in ("MaterialsConsumerBuilder.java", "ShapedMaterialConsumerBuilder.java"):
    sub(os.path.join(ROOT, "material", name), [
        ("public Identifier getId() {\n      return original.getId();",
         "public Identifier getRecipeId() {\n      return original.getRecipeId();"),
        ("public RecipeSerializer<?> getType() {", "public RecipeSerializer<?> getSerializer() {"),
        ("public void serializeRecipeData(JsonObject json) {\n      original.serializeRecipeData(json);",
         "public void serialize(JsonObject json) {\n      original.serialize(json);"),
        ("public JsonObject serializeAdvancement() {\n      return original.serializeAdvancement();",
         "public JsonObject toAdvancementJson() {\n      return original.toAdvancementJson();"),
    ])

# 3. builder finished recipes: same rename inside the inner class
sub(os.path.join(ROOT, "molding", "MoldingRecipeBuilder.java"), [
    ("public void serializeRecipeData(JsonObject json) {", "public void serialize(JsonObject json) {"),
    ("public RecipeSerializer<?> getType() {", "public RecipeSerializer<?> getSerializer() {"),
])
sub(os.path.join(ROOT, "casting", "container", "ContainerFillingRecipeBuilder.java"), [
    ("public void serializeRecipeData(JsonObject json) {", "public void serialize(JsonObject json) {"),
    ("public RecipeSerializer<?> getType() {", "public RecipeSerializer<?> getSerializer() {"),
])

# 4. Mantle 1.11 signature differences: annotate declarations in place (no code change)
PATTERNS = [
    (re.compile(r'^(\s*)(?:@Override\s*)?(?:default |public )?ItemStack getToastSymbol\(\)'), 
     "ICommonRecipe#getToastSymbol() is absent"),
    (re.compile(r'^(\s*)public List<[^>]+> getRecipes\(DynamicRegistryManager access\)'),
     "IMultiRecipe#getRecipes() takes no argument"),
    (re.compile(r'^(\s*)public static class Serializer implements LoggingRecipeSerializer<'),
     "LoggingRecipeSerializer is an abstract class, not an interface"),
]
changed = []
for dirpath, dirnames, filenames in os.walk(ROOT):
    for fn in filenames:
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        lines = read(p).split('\n')
        out = []
        touched = False
        for i, line in enumerate(lines):
            for rx, reason in PATTERNS:
                m = rx.match(line)
                if m and not (out and out[-1].strip().startswith('// TODO(fabric): Mantle 1.9.291')):
                    out.append(m.group(1) + (fallback % reason))
                    touched = True
                    break
            out.append(line)
        if touched:
            write(p, '\n'.join(out)); changed.append(os.path.relpath(p, ROOT))
print("annotated:", len(changed))
for c in changed:
    print("   ", c)
