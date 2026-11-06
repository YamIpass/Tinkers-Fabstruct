package slimeknights.tconstruct.gadgets;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.gadgets.client.FancyArmorStandRenderer;
import slimeknights.tconstruct.gadgets.client.FancyItemFrameRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownShurikenRenderer;

@SuppressWarnings("unused")
public class GadgetClientEvents extends ClientEventBase {
  /**
   * Registers the client events
   * TODO(fabric): Forge ran these from the mod event bus; the Fabric port calls this from the client entrypoint
   *  (see {@link slimeknights.tconstruct.shared.TinkerClient#onConstruct()})
   */
  public static void init() {
    registerModels();
    registerRenderers();
  }

  static void registerModels() {
    // TODO(fabric): Forge's RegisterAdditional model event was replaced with Fabric's model loading plugin
    ModelLoadingPlugin.register(pluginContext -> {
      FancyItemFrameRenderer.LOCATIONS_MODEL.values().forEach(pluginContext::addModels);
      FancyItemFrameRenderer.LOCATIONS_MODEL_MAP.values().forEach(pluginContext::addModels);
    });
  }

  static void registerRenderers() {
    // TODO(fabric): Forge's EntityRenderersEvent.RegisterRenderers was replaced with Fabric's entity renderer registry
    EntityRendererRegistry.register(TinkerGadgets.itemFrameEntity.get(), FancyItemFrameRenderer::new);
    EntityRendererRegistry.register(TinkerGadgets.armorStandEntity.get(), FancyArmorStandRenderer::new);
    EntityRendererRegistry.register(TinkerGadgets.glowBallEntity.get(), ThrownItemRenderer::new);
    EntityRendererRegistry.register(TinkerGadgets.eflnEntity.get(), ThrownItemRenderer::new);
    EntityRendererRegistry.register(TinkerGadgets.quartzShurikenEntity.get(), ThrownShurikenRenderer::new);
    EntityRendererRegistry.register(TinkerGadgets.flintShurikenEntity.get(), ThrownShurikenRenderer::new);
  }
}
