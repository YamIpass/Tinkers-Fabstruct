package slimeknights.tconstruct.fluids;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.fluid.FluidVariantTextureHandler;
import slimeknights.tconstruct.library.client.fluid.TinkerFluidRenderHandler;
import slimeknights.tconstruct.library.client.model.FluidContainerModel;

public class FluidClientEvents extends ClientEventBase {
  // TODO(fabric): called from the Fabric client initializer instead of the Forge mod event bus
  public static void clientSetup() {
    // texture data for the tank only fluids lives in mantle/fluid_texture json files
    FluidTextureManager.init(ResourceManagerHelper.get(PackType.CLIENT_RESOURCES));
    registerFluidRenderHandlers();
    setTranslucent(TinkerFluids.honey);
    // slime
    setTranslucent(TinkerFluids.earthSlime);
    setTranslucent(TinkerFluids.skySlime);
    setTranslucent(TinkerFluids.enderSlime);
    // molten
    setTranslucent(TinkerFluids.moltenDiamond);
    setTranslucent(TinkerFluids.moltenEmerald);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.moltenGlass);
    setTranslucent(TinkerFluids.liquidSoul);
    setTranslucent(TinkerFluids.moltenSoulsteel);
    setTranslucent(TinkerFluids.moltenAmethyst);

    itemColors();
  }

  static void itemColors() {
    ColorProviderRegistry.ITEM.register((stack, index) -> index > 0 ? -1 : PotionUtils.getColor(stack), TinkerFluids.potion.asItem());
  }

  // Forge registered the "fluid_container" geometry loader here through RegisterGeometryLoaders; on Fabric the same hook
  // comes from Porting Lib's geometry module. Called from the client initializer.
  public static void registerModelLoaders() {
    RegisterGeometryLoadersCallback.EVENT.register(loaders -> loaders.put(TConstruct.getResource("fluid_container"), FluidContainerModel.LOADER));
  }

  private static void setTranslucent(FlowingFluidObject<?> fluid) {
    BlockRenderLayerMap.INSTANCE.putFluid(fluid.getStill(), RenderType.translucent());
    BlockRenderLayerMap.INSTANCE.putFluid(fluid.getFlowing(), RenderType.translucent());
  }

  /**
   * Registers the render handler for all our fluids. Fabric's default handler only knows fluids with a block, so tank
   * only fluids would report no sprites at all.
   */
  private static void registerFluidRenderHandlers() {
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
      if (TConstruct.MOD_ID.equals(id.getNamespace())) {
        FluidVariantRendering.register(fluid, FluidVariantTextureHandler.INSTANCE);
        // the block fluid path (Star's chunk rebuild hook, particles) asks this registry instead, and fabric only
        // registers a default handler for fluids that own a block state
        FluidRenderHandlerRegistry.INSTANCE.register(fluid, TinkerFluidRenderHandler.INSTANCE);
      }
    }
  }
}
