package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import slimeknights.tconstruct.library.json.LevelingInt;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.tools.modules.interaction.SpittingModule;

/** @deprecated use {@link SpittingModule} */
@Deprecated(forRemoval = true)
public class SpittingModifier extends Modifier {
  @Override
  protected void registerHooks(Builder builder) {
    builder.addModule(new SpittingModule(LevelingInt.eachLevel(1)));
    builder.addModule(ToolTankHelper.TANK_HANDLER);
    // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, 1000 is one bucket
    builder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
  }

  @Override
  public int getPriority() {
    return 120; // want to run before sling modifiers so we can sling spit, and before throwing so we use our tank first
  }
}
