package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.registry.FuelRegistry;

/**
 * Event subscriber for modifier events
 * Note the way the subscribers are set up, technically works on anything that has the tic_modifiers tag
 */
@SuppressWarnings("unused")
public class FluidEvents {
  // TODO(fabric): called from the Fabric mod initializer instead of the Forge event bus, as Fabric has no event bus.
  public static void onFurnaceFuel() {
    // 150% efficiency compared to lava bucket, compare to casting blaze rods, which cast into 120%
    FuelRegistry.INSTANCE.add(TinkerFluids.blazingBlood.asItem(), 30000);
  }

  // TODO(fabric): Forge attached a fluid handler capability to the vanilla powder snow bucket here through
  //  AttachCapabilitiesEvent<ItemStack>. Fabric cannot attach capabilities to existing item stacks, and Porting Lib
  //  only exposes fluid handlers for items implementing FluidStorageProvider, so the powder snow bucket no longer
  //  exposes its fluid to tanks. Restoring this needs a mixin on the item stack capability lookup, which is out of
  //  scope for the loader glue port.
}
