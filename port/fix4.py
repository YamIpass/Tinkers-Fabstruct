import io, os

ROOT = r"C:\Users\YamIpass\Documents\deepseek-harness\default-workspace\tconstruct-fabric\src\main\java\slimeknights\tconstruct\library\recipe"

def read(p):
    with io.open(p, encoding='utf-8') as f:
        return f.read()

def write(p, s):
    with io.open(p, 'w', encoding='utf-8', newline='\n') as f:
        f.write(s)

GLOBAL = [
    ("BuiltInRegistries.", "Registries."),
    ("Registries.ITEM.getKey(", "Registries.ITEM.getId("),
    ("Registries.FLUID.getKey(", "Registries.FLUID.getId("),
    ("getTagOrEmpty(", "iterateEntries("),
    ("getHolder(", "getEntry("),
    ("writeResourceLocation(", "writeIdentifier("),
    ("readResourceLocation(", "readIdentifier("),
    ("getCast().getItems()", "getCast().getMatchingStacks()"),
    ("List.of(cast.getItems())", "List.of(cast.getMatchingStacks())"),
    ("tools.getItems()[0].getItem()", "tools.getMatchingStacks().get(0).getItem()"),
]

POTION = ("""    return Registries.POTION.holders()
      .filter(holder -> !holder.is(Potions.EMPTY_ID))
      .map(holder -> holder.key().location().toString())
      .toList();""",
          """    return Registries.POTION.streamEntries()
      .filter(holder -> !holder.matchesKey(Potions.EMPTY_KEY))
      .map(holder -> holder.getKey().orElseThrow().getValue().toString())
      .toList();""")

PER_FILE = {
    os.path.join("casting", "PotionCastingRecipe.java"): [POTION],
    os.path.join("ingredient", "InstrumentIngredient.java"): [
        ("this.instrument.location()", "this.instrument.getValue()"),
        ("instrument.location()", "instrument.getValue()"),
        ("ignore.location()", "ignore.id()"),
        ("RegistryKey.create(Registries.INSTRUMENT", "RegistryKey.create(RegistryKeys.INSTRUMENT"),
        ("instrument.get().is(ignore)", "instrument.get().isIn(ignore)"),
        ("buffer.writeId(Registries.ITEM, ingredient.item);",
         "// TODO(fabric): Forge FriendlyByteBuf#writeId has no yarn/Porting Lib equivalent\n      buffer.writeId(Registries.ITEM, ingredient.item);"),
        ("Item item = buffer.readById(Registries.ITEM);",
         "// TODO(fabric): Forge FriendlyByteBuf#readById has no yarn/Porting Lib equivalent\n      Item item = buffer.readById(Registries.ITEM);"),
    ],
    os.path.join("ingredient", "ToolHookIngredient.java"): [
        ("tag.location()", "tag.id()"),
    ],
}

for dirpath, dirnames, filenames in os.walk(ROOT):
    for fn in filenames:
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        rel = os.path.relpath(p, ROOT)
        s = orig = read(p)
        for a, b in GLOBAL:
            s = s.replace(a, b)
        for a, b in PER_FILE.get(rel, []):
            if a not in s:
                print("  MISS", rel, a[:60])
            s = s.replace(a, b)
        if s != orig:
            write(p, s)
            print("  fixed", rel)
