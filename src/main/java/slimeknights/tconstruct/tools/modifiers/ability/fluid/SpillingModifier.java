package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.modifiers.modules.util.ModifierCondition;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.tools.modules.combat.SpillingModule;

/** @deprecated use {@link SpillingModule}, {@link ToolTankHelper#CAPACITY_STAT}, and {@link ToolTankHelper#TANK_HANDLER} */
@Deprecated(forRemoval = true)
public class SpillingModifier extends Modifier {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
    // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, 1000 is one bucket
    hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
    hookBuilder.addModule(new SpillingModule(LevelingValue.eachLevel(1), ModifierCondition.ANY_TOOL));
  }
}
