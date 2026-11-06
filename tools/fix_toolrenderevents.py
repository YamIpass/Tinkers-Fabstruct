import sys
sys.path.insert(0, 'tools')
import patchlib
rel = 'client/ToolRenderEvents.java'
lines, nl = patchlib.read('src/main/java/slimeknights/tconstruct/tools/' + rel)
t = chr(10).join(lines)
# 1. Nullable import
if 'org.jetbrains.annotations.Nullable' not in t:
    t = t.replace('import net.minecraft.world.phys.Vec3;',
                  'import net.minecraft.world.phys.Vec3;' + chr(10) + 'import org.jetbrains.annotations.Nullable;', 1)
# 2. boolean returns in renderBlockHighlights
t = t.replace('if (stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) {' + chr(10) + '      return;',
              'if (stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) {' + chr(10) + '      return true;', 1)
t = t.replace('} else if (!IsEffectiveToolHook.isEffective(tool, state)) {' + chr(10) + '      return;',
              '} else if (!IsEffectiveToolHook.isEffective(tool, state)) {' + chr(10) + '      return true;', 1)
# 3. rename the shadowing UseOnContext locals to useContext
t = t.replace('UseOnContext context = new UseOnContext(', 'UseOnContext useContext = new UseOnContext(')
t = t.replace('getBlocks(tool, context, state, matchType)', 'getBlocks(tool, useContext, state, matchType)')
t = t.replace('getBlocks(tool, context, state, AreaOfEffectIterator.AOEMatchType.BREAKING)',
              'getBlocks(tool, useContext, state, AreaOfEffectIterator.AOEMatchType.BREAKING)')
patchlib.write('src/main/java/slimeknights/tconstruct/tools/' + rel, t.split(chr(10)), nl)
print('returns left:', t.count('      return;' + chr(10) + '    }'))
