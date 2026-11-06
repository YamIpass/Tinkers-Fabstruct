"""TinkerTools: swap Forge registration/creative tab/particle/datagen glue for the Fabric idiom."""
import sys

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from patchlib import read, write, rm_imports, add_imports, find_line, method_bounds, dedent, unwrap_block

REL = 'src/main/java/slimeknights/tconstruct/tools/TinkerTools.java'
lines, nl = read(REL)

lines = rm_imports(lines, [
  # forge
  'net.minecraftforge.common.crafting.CraftingHelper',
  'net.minecraftforge.common.data.ExistingFileHelper',
  'net.minecraftforge.data.event.GatherDataEvent',
  'net.minecraftforge.eventbus.api.SubscribeEvent',
  'net.minecraftforge.fml.ModList',
  'net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent',
  'net.minecraftforge.registries.ForgeRegistries',
  'net.minecraftforge.registries.RegisterEvent',
  'net.minecraftforge.registries.RegistryObject',
  'net.minecraft.advancements.critereon.ItemPredicate',
  'net.minecraft.data.DataGenerator',
  'net.minecraft.data.PackOutput',
  # datagen classes that do not exist in the Fabric port yet
  'slimeknights.tconstruct.common.data.tags.MaterialTagProvider',
  'slimeknights.tconstruct.library.client.data.material.GeneratorPartTextureJsonGenerator',
  'slimeknights.tconstruct.library.client.data.material.MaterialPaletteDebugGenerator',
  'slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator',
  'slimeknights.tconstruct.tools.data.ArmorModelProvider',
  'slimeknights.tconstruct.tools.data.StationSlotLayoutProvider',
  'slimeknights.tconstruct.tools.data.ToolDefinitionDataProvider',
  'slimeknights.tconstruct.tools.data.ToolItemModelProvider',
  'slimeknights.tconstruct.tools.data.ToolsRecipeProvider',
  'slimeknights.tconstruct.tools.data.client.ModifierModelMapProvider',
  'slimeknights.tconstruct.tools.data.material.MaterialDataProvider',
  'slimeknights.tconstruct.tools.data.material.MaterialRecipeProvider',
  'slimeknights.tconstruct.tools.data.material.MaterialRenderInfoProvider',
  'slimeknights.tconstruct.tools.data.material.MaterialStatsDataProvider',
  'slimeknights.tconstruct.tools.data.material.MaterialTraitsDataProvider',
  'slimeknights.tconstruct.tools.data.sprite.TinkerTrimMaterialPaletteGenerator',
])

lines = add_imports(lines, [
  'io.github.fabricators_of_create.porting_lib.util.ItemPredicateRegistry',
  'io.github.fabricators_of_create.porting_lib.util.RegistryObject',
  'net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup',
  'net.fabricmc.fabric.api.particle.v1.FabricParticleTypes',
  'net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer',
  'net.fabricmc.loader.api.FabricLoader',
  'net.minecraft.resources.ResourceKey',
])

# --- constructor: run the setup phases directly, Fabric has no loading-phase event ---
i = find_line(lines, 'RandomMaterial.init();')
lines[i + 1:i + 1] = ['    commonSetup();', '    registerRecipeSerializers();']

# --- creative tab: FabricItemGroup, the Forge tab ordering/search bar have no equivalent ---
i = find_line(lines, 'CreativeModeTab.builder()')
lines[i] = lines[i].replace('CreativeModeTab.builder()', 'FabricItemGroup.builder()')
for needle in ['.withTabsBefore(TinkerCommons.tabGeneral.getId())', '.withSearchBar()']:
  try:
    del lines[find_line(lines, needle)]
  except SystemExit:
    print('skip', needle)

# --- particles: SimpleParticleType has a protected constructor, Fabric wraps it ---
for k, l in enumerate(lines):
  lines[k] = l.replace('new SimpleParticleType(true)', 'FabricParticleTypes.simple(true)')

# --- mod loaded checks go through the Fabric loader ---
for k, l in enumerate(lines):
  lines[k] = l.replace('ModList.get().isLoaded(', 'FabricLoader.getInstance().isModLoaded(')

# --- conditional registration: no Forge registry lookup, resolve lazily by key ---
i = find_line(lines, 'minotaurAxe = new ItemObject<>(RegistryObject.create(')
lines[i] = ('      minotaurAxe = new ItemObject<>(new RegistryObject<>(getResource("minotaur_axe"),'
            ' ResourceKey.create(Registries.ITEM, getResource("minotaur_axe"))));')

# --- custom ingredient serializers are registered through the Fabric API ---
i = find_line(lines, 'CraftingHelper.register(ToolHookIngredient.Serializer.ID')
lines[i] = '      CustomIngredientSerializer.register(ToolHookIngredient.Serializer.INSTANCE);'
i = find_line(lines, 'ItemPredicate.register(ToolStackItemPredicate.ID')
lines[i] = '      ItemPredicateRegistry.register(ToolStackItemPredicate.ID, ToolStackItemPredicate::deserialize);'

# --- commonSetup: plain method ---
sig = find_line(lines, 'void commonSetup(FMLCommonSetupEvent event) {')
lines[sig] = '  void commonSetup() {'
assert lines[sig - 1].strip() == '@SubscribeEvent', repr(lines[sig - 1])
lines[sig - 1] = '  /** Registers the tool capabilities and behaviours */'
lines = unwrap_block(lines, '    event.enqueueWork(() -> {')
for k, l in enumerate(lines):
  lines[k] = l.replace('event.enqueueWork(action);', 'action.run();')

# --- registerRecipeSerializers: plain method, no registry event ---
sig = find_line(lines, 'void registerRecipeSerializers(RegisterEvent event) {')
lines[sig] = '  void registerRecipeSerializers() {'
assert lines[sig - 1].strip() == '@SubscribeEvent', repr(lines[sig - 1])
lines[sig - 1] = '  /** Registers the serializers, loaders and predicates used by the tools module */'
lines = unwrap_block(lines, 'if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {')

# --- datagen: providers are not ported yet ---
top, end = method_bounds(lines, 'void gatherData(final GatherDataEvent event) {')
lines[top:end + 1] = [
  '  // TODO(fabric): the tools datagen providers (tools/data/**, library/client/data/**) are not ported to the',
  '  //  Fabric data generator yet; port them with FabricDataGenerator alongside the rest of the datagen package.',
]

write(REL, lines, nl)
