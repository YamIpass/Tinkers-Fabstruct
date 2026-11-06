"""Replace LogicHelper.orElseNull(<lazy optional>) with TinkerDataCapability.orNull(...).

The Forge LogicHelper had an overload taking LazyOptional; Mantle 1.11 only has the Optional one, so the tconstruct
port provides TinkerDataCapability.orNull instead.
"""
import re
import sys

files = [
    'src/main/java/slimeknights/tconstruct/library/modifiers/modules/technical/ArmorLevelModule.java',
    'src/main/java/slimeknights/tconstruct/library/modifiers/modules/technical/MaxArmorLevelModule.java',
    'src/main/java/slimeknights/tconstruct/library/modifiers/modules/technical/SlotInChargeModule.java',
    'src/main/java/slimeknights/tconstruct/tools/modules/armor/MinimapModule.java',
    'src/main/java/slimeknights/tconstruct/tools/modules/FovModule.java',
]

# only these argument shapes are lazy optionals, Optional<...> call sites keep using LogicHelper
pattern = re.compile(r'LogicHelper\.orElseNull\(((?:context\.getTinkerData\(\))|cap|capability)\)')

changed = 0
for path in files:
    text = open(path, encoding='utf-8').read()
    new, n = pattern.subn(r'TinkerDataCapability.orNull(\1)', text)
    if n:
        if 'import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;' not in new:
            new = new.replace('import slimeknights.mantle.util.LogicHelper;',
                              'import slimeknights.mantle.util.LogicHelper;\nimport slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;', 1)
        if 'LogicHelper.' not in new:
            new = new.replace('import slimeknights.mantle.util.LogicHelper;\n', '')
        open(path, 'w', encoding='utf-8').write(new)
        print(f'{path}: {n}')
        changed += n
print('total', changed)
