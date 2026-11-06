package slimeknights.tconstruct.library.tools.capability;

import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Capability provider for tool stacks.
 * On Fabric, item and block capabilities are registered directly on the item via the transfer API,
 * so this class only keeps the provider registry used by {@code TinkerTools} to declare the tool capabilities.
 */
public class ToolCapabilityProvider {
  private static final List<BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider>> PROVIDER_CONSTRUCTORS = new ArrayList<>();

  /** Registers a tool capability provider constructor. Every new tool will call this constructor to create your provider.
   * Is it valid for this constructor to return null, just note that it will not be called a second time if the tools state changes. Thus you should avoid conditioning on anything other than item type */
  public static void register(BiFunction<ItemStack,Supplier<? extends IToolStackView>,IToolCapabilityProvider> constructor) {
    PROVIDER_CONSTRUCTORS.add(constructor);
  }

  /** Interface to get a capability on a tool */
  public interface IToolCapabilityProvider {
    // TODO(fabric): this interface used to expose getCapability(Capability); Fabric item lookups are registered directly on the item instead

    /** Called to clear the cache of the provider */
    default void clearCache() {}
  }
}
