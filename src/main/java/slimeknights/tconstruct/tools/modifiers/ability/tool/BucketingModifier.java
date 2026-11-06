package slimeknights.tconstruct.tools.modifiers.ability.tool;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.data.predicate.fluid.FluidPredicate;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.modules.behavior.ShowOffhandModule;
import slimeknights.tconstruct.library.modifiers.modules.build.StatBoostModule;
import slimeknights.tconstruct.library.module.ModuleHookMap.Builder;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.modules.interaction.BucketModule;
import slimeknights.tconstruct.tools.modules.interaction.TankInteractionModule;

import javax.annotation.Nullable;

/** @deprecated use {@link BucketModule}, {@link ToolTankHelper#TANK_HANDLER}, {@link TankInteractionModule}, and {@link ShowOffhandModule} */
@Deprecated(forRemoval = true)
public class BucketingModifier extends Modifier {
  @Override
  protected void registerHooks(Builder hookBuilder) {
    super.registerHooks(hookBuilder);
    hookBuilder.addModule(ToolTankHelper.TANK_HANDLER);
    // TODO(fabric): Forge's FluidType.BUCKET_VOLUME (1000 mB) has no Fabric equivalent, 1000 is one bucket
    hookBuilder.addModule(StatBoostModule.add(ToolTankHelper.CAPACITY_STAT).flat(1000));
    hookBuilder.addModule(new TankInteractionModule(InteractionSource.ARMOR));
    hookBuilder.addModule(new BucketModule(FluidPredicate.ANY));
    hookBuilder.addModule(ShowOffhandModule.ALLOW_BROKEN);
  }

  @Override
  public Component getDisplayName(IToolStackView tool, ModifierEntry entry, @Nullable RegistryAccess access) {
    return InteractionSource.formatModifierName(tool, this, super.getDisplayName(tool, entry, access));
  }
}
