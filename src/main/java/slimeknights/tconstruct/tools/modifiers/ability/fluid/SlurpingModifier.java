package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.tools.modules.interaction.SlurpingModule;

/** @deprecated use {@link SlurpingModule} */
@Deprecated(forRemoval = true)
public class SlurpingModifier extends Modifier {
  @Override
  public int getPriority() {
    return 40;
  }

  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addModule(new SlurpingModule(LevelingValue.eachLevel(1), LevelingInt.flat(21)));
    hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
    // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, 1000 is one bucket
    hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
  }
}
