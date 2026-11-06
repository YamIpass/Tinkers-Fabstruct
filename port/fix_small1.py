import io

def edit(path, pairs):
    src = io.open(path, encoding='utf-8').read()
    for old, new in pairs:
        if old not in src:
            print('MISS', path, old[:60].replace('\n', '\\n'))
            continue
        src = src.replace(old, new, 1)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(src)

base = 'src/main/java/slimeknights/tconstruct/world/'

# --- SlimeSaplingBlock: PortingLib PlantType / IPlantable
edit(base + 'block/SlimeSaplingBlock.java', [
    ('import net.minecraftforge.common.PlantType;\n', ''),
    ('import net.minecraft.core.BlockPos;\n',
     'import io.github.fabricators_of_create.porting_lib.common.util.IPlantable;\n'
     'import io.github.fabricators_of_create.porting_lib.common.util.PlantType;\n'
     'import net.minecraft.core.BlockPos;\n'),
    ('public class SlimeSaplingBlock extends SaplingBlock {',
     'public class SlimeSaplingBlock extends SaplingBlock implements IPlantable {'),
])

# --- SlimeTallGrassBlock: PortingLib shearable + plantable
edit(base + 'block/SlimeTallGrassBlock.java', [
    ('import net.minecraftforge.common.IForgeShearable;\nimport net.minecraftforge.common.PlantType;\n', ''),
    ('import com.google.common.collect.Lists;\n',
     'import com.google.common.collect.Lists;\n'
     'import io.github.fabricators_of_create.porting_lib.common.util.IPlantable;\n'
     'import io.github.fabricators_of_create.porting_lib.common.util.PlantType;\n'
     'import io.github.fabricators_of_create.porting_lib.extensions.extensions.IShearable;\n'),
    ('public class SlimeTallGrassBlock extends BushBlock implements IForgeShearable {',
     'public class SlimeTallGrassBlock extends BushBlock implements IShearable, IPlantable {'),
    ('  /* Forge/MC callbacks */\n', '  /* MC callbacks */\n'),
])

# --- SkySlimeEntity: Forge fall hook is gone on Fabric
edit(base + 'entity/SkySlimeEntity.java', [
    ('import net.minecraftforge.common.ForgeHooks;\n', ''),
    ('''    float[] ret = ForgeHooks.onLivingFall(this, distance, damageMultiplier);
    if (ret == null) {
      return false;
    }
    distance = ret[0];
''',
     '''    // TODO(fabric): Forge fired LivingEntityEvents.FALL so other mods could cancel or change the distance.
    //  Porting Lib 2.3.16 no longer ships LivingEntityEvents, so the vanilla distance is used unchanged.
'''),
])
print('ok')
