package slimeknights.tconstruct.fluids.item;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import slimeknights.mantle.item.ConstantFluidContainerWrapper;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nullable;
import java.util.List;

/** Magma bottle instance, which lights the drinker on fire */
public class MagmaBottleItem extends Item {
  private final int fireTime;
  public MagmaBottleItem(Properties props, int fireTime) {
    super(props);
    this.fireTime = fireTime;
    // TODO(fabric): Forge provided this through Item#initCapabilities, Fabric registers the item fluid storage instead
    FluidStorage.ITEM.registerForItems((stack, context) -> new ConstantFluidContainerWrapper(new FluidStack(TinkerFluids.magma.get(), FluidValues.BOTTLE), context), this);
  }

  @Override
  public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
    super.appendHoverText(stack, worldIn, tooltip, flagIn);
    tooltip.add(Component.translatable(
      "potion.withDuration",
      Blocks.FIRE.getName(),
      StringUtil.formatTickDuration(fireTime * 20)
    ).withStyle(MobEffectCategory.HARMFUL.getTooltipFormatting()));
  }

  @Override
  public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
    player.startUsingItem(hand);
    return InteractionResultHolder.consume(player.getItemInHand(hand));
  }

  @Override
  public int getUseDuration(ItemStack pStack) {
    return 32;
  }

  @Override
  public UseAnim getUseAnimation(ItemStack pStack) {
    return UseAnim.DRINK;
  }

  @Override
  public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
    living.setSecondsOnFire(fireTime);
    // TODO(fabric): Forge 的 ItemStack#getCraftingRemainingItem() 返回 ItemStack；1.20.1 vanilla 只有 Item 级 API 且可为空
    Item containerItem = stack.getItem().getCraftingRemainingItem();
    ItemStack container = containerItem == null ? ItemStack.EMPTY : new ItemStack(containerItem);
    Player player = living instanceof Player p ? p : null;
    if (player == null || !player.getAbilities().instabuild) {
      stack.shrink(1);
      container = container.copy();
      if (stack.isEmpty()) {
        return container;
      }
      if (player != null) {
        if (!player.getInventory().add(container)) {
          player.drop(container, false);
        }
      }
    }
    return stack;
  }
}
