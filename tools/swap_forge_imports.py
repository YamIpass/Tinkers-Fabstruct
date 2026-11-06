"""Mechanically swaps Forge imports for their Fabric / Porting Lib equivalents across tools/.

Only unambiguous 1:1 replacements are applied here; event-glue files are handled separately.
"""
import sys, os, glob
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib as P

ROOT = 'src/main/java/slimeknights/tconstruct/tools'
NL = chr(10)

# forge import -> fabric import
MAP = {
    'net.minecraftforge.fluids.FluidStack': 'io.github.fabricators_of_create.porting_lib.fluids.FluidStack',
    'net.minecraftforge.fluids.FluidType': 'io.github.fabricators_of_create.porting_lib.fluids.FluidType',
    'net.minecraftforge.common.ToolActions': 'io.github.fabricators_of_create.porting_lib.tool.ToolActions',
    'net.minecraftforge.common.ToolAction': 'io.github.fabricators_of_create.porting_lib.tool.ToolAction',
    'net.minecraftforge.registries.RegistryObject': 'io.github.fabricators_of_create.porting_lib.util.RegistryObject',
    'net.minecraftforge.items.ItemStackHandler': 'io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler',
    'net.minecraftforge.items.ItemHandlerHelper': 'io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper',
    'net.minecraftforge.eventbus.api.Event.Result': 'io.github.fabricators_of_create.porting_lib.core.event.BaseEvent.Result',
    'net.minecraftforge.common.ForgeMod': 'io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes',
    'net.minecraftforge.fml.ModList': 'net.fabricmc.loader.api.FabricLoader',
    'net.minecraftforge.common.loot.IGlobalLootModifier': 'io.github.fabricators_of_create.porting_lib.loot.IGlobalLootModifier',
    'net.minecraftforge.common.loot.LootModifier': 'io.github.fabricators_of_create.porting_lib.loot.LootModifier',
}

BODY = {
    'ForgeMod.BLOCK_REACH.get()': 'PortingLibAttributes.BLOCK_REACH',
    'ForgeMod.ENTITY_REACH.get()': 'PortingLibAttributes.ENTITY_REACH',
    'ForgeMod.ENTITY_GRAVITY.get()': 'PortingLibAttributes.ENTITY_GRAVITY',
    'ForgeMod.STEP_HEIGHT_ADDITION.get()': 'PortingLibAttributes.STEP_HEIGHT_ADDITION',
    'ForgeMod.SWIM_SPEED.get()': 'PortingLibAttributes.SWIM_SPEED',
    'ModList.get().isLoaded(': 'FabricLoader.getInstance().isModLoaded(',
}

changed = {}
for path in sorted(glob.glob(ROOT + '/**/*.java', recursive=True)):
    rel = path.replace(os.sep, '/')
    lines, nl = P.read(rel)
    if not any(l.startswith('import ') for l in lines):
        continue
    body = NL.join(lines)
    orig = body
    for old, new in BODY.items():
        body = body.replace(old, new)
    lines = body.split(NL)
    swapped = []
    for forge, fabric in MAP.items():
        line = 'import ' + forge + ';'
        if line in lines:
            lines = [l for l in lines if l.strip() != line]
            swapped.append(fabric)
    if not swapped and body == orig:
        continue
    body = NL.join(lines)
    if 'PortingLibAttributes.' in body:
        swapped.append('io.github.fabricators_of_create.porting_lib.attributes.PortingLibAttributes')
    if 'FabricLoader.' in body:
        swapped.append('net.fabricmc.loader.api.FabricLoader')
    # keep only imports whose simple name is still referenced
    keep = []
    for imp in swapped:
        simple = imp.rsplit('.', 1)[-1]
        if simple in body and imp not in keep:
            keep.append(imp)
    if any(l.startswith('import ') for l in lines):
        lines = P.add_imports(lines, keep)
    else:
        lines = [l for l in lines if l.strip()]
    P.write(rel, lines, nl)
    changed[rel] = len(swapped)

print('files changed:', len(changed))
for rel in sorted(changed):
    print(' ', rel)
