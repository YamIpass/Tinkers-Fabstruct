package slimeknights.tconstruct;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import slimeknights.tconstruct.fluids.FluidEvents;
import slimeknights.tconstruct.shared.PortSelfTest;
import slimeknights.tconstruct.smeltery.block.entity.tank.MillibucketStorage;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import slimeknights.mantle.client.render.MantleShaders;
import slimeknights.tconstruct.tools.ToolClientEvents;
import slimeknights.tconstruct.tools.client.ClientInteractionHandler;
import slimeknights.tconstruct.tools.client.ModifierClientEvents;
import slimeknights.tconstruct.tools.client.ToolRenderEvents;
import slimeknights.tconstruct.tools.logic.DoubleJumpHandler;
import slimeknights.tconstruct.tools.logic.EquipmentChangeWatcher;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.logic.ModifierEvents;
import slimeknights.tconstruct.tools.logic.ToolEvents;

/**
 * Port shim replacing Forge's mod event bus subscribers.
 * <p>
 * Upstream registers these classes through {@code @EventBusSubscriber} / {@code FMLCommonSetupEvent} / {@code FMLClientSetupEvent}.
 * Fabric has no loader-provided equivalent, so the common entrypoint calls {@link #init()} and the client entrypoint calls
 * {@link #initClient()} explicitly. Everything below is the original upstream registration logic; only the registration
 * site moved.
 */
public final class FabricEvents {
  private FabricEvents() {}

  /** Registers common (server + client) gameplay hooks. Called from {@link TConstruct#onInitialize()} */
  public static void init() {
    FluidEvents.onFurnaceFuel();
    ToolEvents.init();
    ModifierEvents.init();
    InteractionHandler.init();
    DoubleJumpHandler.init();
    EquipmentChangeWatcher.register();
    registerStorageFallbacks();
    // dev only, no-op unless -Dtconstruct.selftest=true; verifies the ported data packs actually resolved
    PortSelfTest.init();
  }

  /**
   * Forge exposed block entity fluid/item handlers through {@code CapabilityManager.get(...)}; on Fabric the same lookups go
   * through {@code FluidStorage.SIDED} / {@code ItemStorage.SIDED}. Upstream block entities answer per side, which is exactly
   * what {@link SidedStorageBlockEntity} models, so one fallback covers every ported block entity. Without this call every
   * sided lookup (tanks, drains, faucets, gauges) returns null and fluid containers refuse to interact.
   */
  private static void registerStorageFallbacks() {
    // fabric-transfer-api already answers SidedStorageBlockEntity through its own fallback, this registration is the
    // safety net for loaders that drop that one; both simply ask the block entity for the per side handler
    FluidStorage.SIDED.registerFallback((world, pos, state, blockEntity, side) ->
      blockEntity instanceof SidedStorageBlockEntity sided ? dropletView(sided.getFluidStorage(side)) : null);
    ItemStorage.SIDED.registerFallback((world, pos, state, blockEntity, side) ->
      blockEntity instanceof SidedStorageBlockEntity sided ? sided.getItemStorage(side) : null);
  }

  /** our tanks count millibuckets, Fabric consumers (and Jade's tooltip) count droplets, so convert at the boundary */
  private static Storage<FluidVariant> dropletView(Storage<FluidVariant> storage) {
    if (storage == null || storage instanceof MillibucketStorage) {
      return storage;
    }
    return new MillibucketStorage(storage);
  }

  /** Registers client-only hooks. Called from {@code client.TinkerClient#onInitializeClient()} */
  @Environment(EnvType.CLIENT)
  public static void initClient() {
    ClientInteractionHandler.init();
    ModifierClientEvents.init();
    ToolRenderEvents.init();
    ToolClientEvents.clientSetupEvent();
    // mantle ships its own core shaders, without this callback the fluid shader stays null and drawing any fluid
    // render type crashes the client with "shaderInstance is null"
    CoreShaderRegistrationCallback.EVENT.register(MantleShaders::registerShaders);
  }
}
