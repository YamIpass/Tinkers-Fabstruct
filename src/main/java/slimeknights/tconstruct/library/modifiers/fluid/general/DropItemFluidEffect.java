package slimeknights.tconstruct.library.modifiers.fluid.general;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ItemLike;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.ItemOutput;
import slimeknights.tconstruct.library.modifiers.fluid.EffectLevel;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffect;
import slimeknights.tconstruct.library.modifiers.fluid.FluidEffectContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

/** Effect to drop an item at the target */
public record DropItemFluidEffect(ItemOutput item) implements FluidEffect<FluidEffectContext> {
  public static final RecordLoadable<DropItemFluidEffect> LOADER = ItemOutput.Loadable.REQUIRED_STACK.xmap((output, error) -> new DropItemFluidEffect(output), (effect, error) -> effect.item());

  public DropItemFluidEffect(ItemLike item) {
    this(ItemOutput.fromItem(item));
  }

  @Override
  public RecordLoadable<DropItemFluidEffect> getLoader() {
    return LOADER;
  }

  @Override
  public float apply(FluidStack fluid, EffectLevel level, FluidEffectContext context, TransactionContext transaction) {
    int count = (int)(level.value() * item.getCount());
    if (count > 0) {
      if (transaction != null) {
        ModifierUtil.dropItem(context.getLevel(), context.getLocation(), ItemHandlerHelper.copyStackWithSize(item.get(), count * item.getCount()));
      }
      return (float) count / item.getCount();
    }
    return 0;
  }

  @Override
  public Component getDescription(RegistryAccess registryAccess) {
    return FluidEffect.makeTranslation(getLoader(), item.get().getHoverName());
  }
}
