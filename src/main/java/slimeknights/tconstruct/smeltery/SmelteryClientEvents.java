package slimeknights.tconstruct.smeltery;

import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import java.util.Map;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import slimeknights.mantle.client.render.ChannelFluids;
import slimeknights.mantle.client.render.FaucetFluid;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.TinkerItemDisplays;
import slimeknights.tconstruct.library.client.model.block.FluidTextureModel;
import slimeknights.tconstruct.library.client.model.block.TankModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.smeltery.client.render.CastingBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.ChannelBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.FaucetBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.GaugeBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.HeatingStructureBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.ProxyTankBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.TankInventoryBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.render.TankBlockEntityRenderer;
import slimeknights.tconstruct.smeltery.client.screen.AlloyerScreen;
import slimeknights.tconstruct.smeltery.client.screen.HeatingStructureScreen;
import slimeknights.tconstruct.smeltery.client.screen.MelterScreen;
import slimeknights.tconstruct.smeltery.client.screen.SingleItemScreenFactory;

@SuppressWarnings("unused")
public class SmelteryClientEvents extends ClientEventBase {

  public static void init() {
    addResourceListener();
    registerRenderers();
    clientSetup();
    RegisterGeometryLoadersCallback.EVENT.register(SmelteryClientEvents::registerModelLoaders);
  }

  static void addResourceListener() {
    // TODO(fabric): Mantle 1.11 replaced FaucetFluidLoader with data-map loaders registration
    ResourceManagerHelper resources = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
    ChannelFluids.initialize(resources);
    FaucetFluid.initialize(resources);
  }

  static void registerRenderers() {
    BlockEntityRenderers.register(TinkerSmeltery.tank.get(), TankBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.faucet.get(), FaucetBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.channel.get(), ChannelBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.table.get(), CastingBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.basin.get(), CastingBlockEntityRenderer::new);
    // TODO(fabric): 3.12.1 renders the melter with a tank inventory renderer (no dedicated MelterBlockEntityRenderer)
    BlockEntityRenderers.register(TinkerSmeltery.melter.get(), context -> new TankInventoryBlockEntityRenderer<>(BlockStateProperties.HORIZONTAL_FACING));
    BlockEntityRenderers.register(TinkerSmeltery.fluidCannon.get(), context -> new TankInventoryBlockEntityRenderer<>(BlockStateProperties.FACING));
    BlockEntityRenderers.register(TinkerSmeltery.gauge.get(), GaugeBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.proxyTank.get(), ProxyTankBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.castingTank.get(), context -> new TankInventoryBlockEntityRenderer<>(BlockStateProperties.HORIZONTAL_FACING));
    BlockEntityRenderers.register(TinkerSmeltery.alloyer.get(), TankBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.smeltery.get(), HeatingStructureBlockEntityRenderer::new);
    BlockEntityRenderers.register(TinkerSmeltery.foundry.get(), HeatingStructureBlockEntityRenderer::new);
  }

  static void clientSetup() {
    // render layers
    RenderType cutout = RenderType.cutout();
    // seared
    // casting
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedFaucet.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedBasin.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedTable.get(), cutout);
    // controller
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedMelter.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.smelteryController.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.foundryController.get(), cutout);
    // peripherals
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedDrain.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedDuct.get(), cutout);
    TinkerSmeltery.searedTank.forEach(tank -> BlockRenderLayerMap.INSTANCE.putBlock(tank, cutout));
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedLantern.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedGlass.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.searedGlassPane.get(), cutout);
    // scorched
    // casting
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedFaucet.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedBasin.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedTable.get(), cutout);
    // controller
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedAlloyer.get(), cutout);
    // peripherals
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedDrain.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedDuct.get(), cutout);
    TinkerSmeltery.scorchedTank.forEach(tank -> BlockRenderLayerMap.INSTANCE.putBlock(tank, cutout));
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedLantern.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedGlass.get(), cutout);
    BlockRenderLayerMap.INSTANCE.putBlock(TinkerSmeltery.scorchedGlassPane.get(), cutout);

    // screens
    ScreenRegistry.register(TinkerSmeltery.melterContainer.get(), MelterScreen::new);
    ScreenRegistry.register(TinkerSmeltery.smelteryContainer.get(), HeatingStructureScreen::new);
    ScreenRegistry.register(TinkerSmeltery.singleItemContainer.get(), new SingleItemScreenFactory());
    ScreenRegistry.register(TinkerSmeltery.alloyerContainer.get(), AlloyerScreen::new);
  }

  static void registerModelLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
    loaders.put(TConstruct.getResource("tank"), TankModel.LOADER);
    // TODO(fabric): CastingModel, MelterModel, ChannelModel and CopperCanModel are not ported yet (library/client/model/block)
    loaders.put(TConstruct.getResource("fluid_texture"), FluidTextureModel.LOADER);
  }
}
