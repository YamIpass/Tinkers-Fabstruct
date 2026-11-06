package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import javax.annotation.Nullable;

/**
 * A capability that provides block items to things that place blocks, such as the Exchanging modifier or some place block fluid effects like Ichor.
 * Providers of this capability are encouraged to use a single instance for all objects that use the same logic, as the stack and more context are provided in the relevant methods.
 */
public interface BlockItemProviderCapability {

  /** Capability ID */
  ResourceLocation ID = TConstruct.getResource("block_provider");

  /** Registers this capability */
  @ApiStatus.Internal
  static void register() {
    // TODO(fabric): Fabric has no item stack capabilities, so there is no attach event to listen to.
    //  Lookups are resolved directly in {@link #getBlockProvider(ItemStack)} instead.
  }

  /**
   * Utility to fetch a BlockProvider or null from a given stack.
   * @return The block provider for this stack, or null if this stack cannot provide block items.
   */
  @Nullable
  static BlockItemProviderCapability getBlockProvider(ItemStack stack) {
    if (stack.isEmpty()) {
      return null;
    }
    // TODO(fabric): these were two separate capability providers in Forge, attached to the item stack.
    if (stack.getItem() instanceof IModifiable) {
      return new BlockItemProviderModifierHook.CapabilityImpl(ToolStack.from(stack));
    }
    if (stack.getItem() instanceof BlockItem) {
      return SimpleBlockItem.INSTANCE;
    }
    return null;
  }

  /**
   * Utility to verify that a given stack does indeed contain a BlockItem
   * @param stack The stack to check
   * @param blockProvider The provider that provided this item, used in case it fails as debugging information
   * @return the contained BlockItem, or null if it was not a BlockItem
   */
  @Nullable
  static BlockItem verifyBlockItem(ItemStack stack, BlockItemProviderCapability blockProvider) {
    if (stack.getItem() instanceof BlockItem bItem) {
      return bItem;
    } else {
      TConstruct.LOG.warn("BlockItemProviderCapability implementation tried to return a non-empty, non-blockitem stack! Cap: {}, Cap Class: {}, Provided Item: {}", blockProvider, blockProvider.getClass().getName(), Loadables.ITEM.getKey(stack.getItem()));
      return null;
    }
  }

  /**
   * Get a {@link BlockItem} to provide, wrapped as an ItemStack with any required placement NBT data. Can be randomised, if desired.
   * <br>
   * <br>
   * <b>The returned stack must have {@link ItemStack#getItem} return an instance of {@link BlockItem}, or be {@link ItemStack#EMPTY}!</b>
   * @param stack The {@link ItemStack} that this capability was attached to.
   * @param entity The {@link LivingEntity} (usually a {@link Player}) that is requesting a block.
   * @return the {@link ItemStack} that this provides, or {@link ItemStack#EMPTY} if this cannot provide more block items (for example if the stack has been depleted)
   */
  ItemStack getBlockItemStack(ItemStack stack, @Nullable LivingEntity entity);

  /**
   * Consume one item from this provider.
   * @param stack The {@link ItemStack} that this capability was attached to.
   * @param backingStack The stack returned by {@link #getBlockItemStack} that was placed and is now being consumed. It is unmodified and the same instance so can use == for comparisons.
   * @param entity The {@link LivingEntity} (usually a {@link Player}) that has just consumed a block.
   * Consume a block from this provider. For example may decrease a contained stacks size or remove fluid from the stack's tank.
   */
  void consume(ItemStack stack, ItemStack backingStack, @Nullable LivingEntity entity);

  /**
   * A simple implementation of {@link BlockItemProviderCapability} that provides from an ItemStack holding a BlockItem
   */
  final class SimpleBlockItem implements BlockItemProviderCapability {
    public static final SimpleBlockItem INSTANCE = new SimpleBlockItem();
    private static final ResourceLocation ID = TConstruct.getResource("block_item_provider");

    private SimpleBlockItem() {}

    @Override
    public ItemStack getBlockItemStack(ItemStack capStack, @Nullable LivingEntity entity) {
      return capStack.isEmpty() ? ItemStack.EMPTY : capStack;
    }

    @Override
    public void consume(ItemStack capStack, ItemStack backingStack, @Nullable LivingEntity entity) {
      capStack.shrink(1);
    }
  }
}
