"""TinkerModifiers: replace Forge registration/datagen glue with the Fabric reference idiom."""
import sys

sys.path.insert(0, __file__.rsplit('\\', 1)[0])
from patchlib import read, write, rm_imports, add_imports, find_line, method_bounds, dedent

REL = 'src/main/java/slimeknights/tconstruct/tools/TinkerModifiers.java'
lines, nl = read(REL)

lines = rm_imports(lines, [
  'net.minecraftforge.data.event.GatherDataEvent',
  'net.minecraftforge.eventbus.api.SubscribeEvent',
  'net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent',
  'net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext',
  'net.minecraftforge.registries.RegisterEvent',
  'net.minecraftforge.registries.RegistryObject',
  'net.minecraft.data.DataGenerator',
  'net.minecraft.data.PackOutput',
  'slimeknights.tconstruct.common.data.tags.ModifierTagProvider',
  'slimeknights.tconstruct.tools.data.EnchantmentToModifierProvider',
  'slimeknights.tconstruct.tools.data.FluidEffectProvider',
  'slimeknights.tconstruct.tools.data.ModifierProvider',
  'slimeknights.tconstruct.tools.data.ModifierRecipeProvider',
])

lines = add_imports(lines, ['io.github.fabricators_of_create.porting_lib.util.RegistryObject'])

# --- constructor: register the modifiers with a plain register() call, then run the setup phases ---
i = find_line(lines, 'MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());')
lines[i] = lines[i].replace('MODIFIERS.register(FMLJavaModLoadingContext.get().getModEventBus());', 'MODIFIERS.register();')
j = find_line(lines, 'TinkerDataKeys.init();')
lines[j + 1:j + 1] = ['    commonSetup();', '    registerSerializers();']
print('constructor updated')

# --- registerSerializers: no RegisterEvent, the loadables registry is plain static ---
i_if = find_line(lines, 'if (event.getRegistryKey() == Registries.RECIPE_SERIALIZER) {')
k = next(k for k in range(i_if + 1, len(lines)) if lines[k] == '  }')  # method close

assert lines[k - 1] == '    }', repr(lines[k - 1])
lines = dedent(lines, i_if + 1, k - 2)
del lines[k - 1]   # the `if` close brace
del lines[i_if]    # the `if` line itself

sig = find_line(lines, 'void registerSerializers(RegisterEvent event) {')
lines[sig] = '  void registerSerializers() {'
assert lines[sig - 1].strip() == '@SubscribeEvent', repr(lines[sig - 1])
lines[sig - 1] = '  /** Registers the loadables and serializers used by the tools module */'
print('registerSerializers updated')

# --- commonSetup: plain method called from the constructor ---
sig = find_line(lines, 'void commonSetup(final FMLCommonSetupEvent event) {')
lines[sig] = '  void commonSetup() {'
assert lines[sig - 1].strip() == '@SubscribeEvent', repr(lines[sig - 1])
lines[sig - 1] = '  /** Registers the capabilities used by the tools module */'
print('commonSetup updated')

# --- gatherData: the tools data providers are not ported yet ---
top, end = method_bounds(lines, 'void gatherData(final GatherDataEvent event) {')
lines[top:end + 1] = [
  '  // TODO(fabric): the tools datagen providers (ModifierProvider, ModifierRecipeProvider, FluidEffectProvider,',
  '  //  ModifierTagProvider, EnchantmentToModifierProvider) are not ported to the Fabric data generator yet;',
  '  //  port them with FabricDataGenerator alongside the rest of the datagen package.',
]
print('gatherData stubbed')

write(REL, lines, nl)
