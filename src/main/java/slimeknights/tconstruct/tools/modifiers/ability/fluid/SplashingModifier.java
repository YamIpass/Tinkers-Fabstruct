package slimeknights.tconstruct.tools.modifiers.ability.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import slimeknights.tconstruct.library.json.LevelingValue;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.modules.interaction.SplashingModule;

import javax.annotation.Nullable;

/** @deprecated use {@link SplashingModule} */
@Deprecated(forRemoval = true)
public class SplashingModifier extends Modifier {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    hookBuilder.addModule(new SplashingModule(LevelingValue.eachLevel(1)));
    hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
    // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, 1000 is one bucket
    hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).eachLevel(1000));
    hookBuilder.addModule(ShowOffhandModule.DISALLOW_BROKEN);
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, @Nullable RegistryAccess access) {
    return InteractionSource.formatModifierName(tool, entry.getModifier(), entry.getDisplayName());
  }
}
