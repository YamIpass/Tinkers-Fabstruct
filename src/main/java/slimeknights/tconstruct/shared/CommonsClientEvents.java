package slimeknights.tconstruct.shared;

import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.model.NBTKeyModel;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.connected.ConnectedModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.MantleItemLayerModel;
import slimeknights.mantle.client.screen.book.BookScreen;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.client.armor.texture.TrimArmorTextureSupplier;
import slimeknights.tconstruct.library.client.book.TinkerBook;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.client.model.DynamicTextureLoader;
import slimeknights.tconstruct.library.client.model.UniqueGuiModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierIconManager;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.shared.client.FluidParticle;
import slimeknights.tconstruct.shared.client.TinkerRenderLayers;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class CommonsClientEvents extends ClientEventBase {
  /**
   * Registers the client events
   * TODO(fabric): Forge ran these from the mod event bus; the Fabric port calls this from the client entrypoint
   *  (see {@link TinkerClient#onConstruct()})
   */
  public static void init() {
    addResourceListeners();
    registerModelLoaders();
    clientSetup();
    registerParticleFactories();
  }

  static void addResourceListeners() {
    // Forge fired RegisterClientReloadListenersEvent; on Fabric the same hook is Fabric API's resource manager helper.
    // The listeners themselves are already written against IdentifiableResourceReloadListener / IEarlySafeManagerReloadListener,
    // so they only need to be handed to the helper here.
    ResourceManagerHelper manager = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
    ArmorModelManager.init(manager);
    MaterialRenderInfoLoader.init(manager);
    MaterialTooltipCache.init(manager);
    DynamicTextureLoader.init(manager);
    ModifierIconManager.init(manager);
    ModifierModelManager.init(manager);
    // these two are plain ResourceManagerReloadListener (Mantle's data loader / a lambda), so wrap them with an id
    manager.registerReloadListener(wrap(TConstruct.getResource("modifier_model_map"), ModifierModelMapManager.INSTANCE));
    manager.registerReloadListener(wrap(TConstruct.getResource("armor_trim_cache"), TrimArmorTextureSupplier.CACHE_INVALIDATOR));
  }

  /** Wraps a plain Forge-style reload listener into a Fabric identifiable one */
  private static IdentifiableResourceReloadListener wrap(ResourceLocation id, ResourceManagerReloadListener listener) {
    return new IdentifiableResourceReloadListener() {
      @Override
      public ResourceLocation getFabricId() {
        return id;
      }

      @Override
      public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager, ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return listener.reload(stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor);
      }
    };
  }

  static void registerModelLoaders() {
    // Forge registered the "gui" geometry loader through ModelEvent.RegisterGeometryLoaders; on Fabric the same hook
    // comes from Porting Lib's geometry module (RegisterGeometryLoadersCallback).
    RegisterGeometryLoadersCallback.EVENT.register(loaders -> {
      loaders.put(TConstruct.getResource("gui"), UniqueGuiModel.LOADER);
      // Mantle ships these geometry loaders but its Fabric fork never registers them (upstream does it on the mod bus),
      // so any model using mantle:item_layer / mantle:nbt_key fails to bake and renders as a blank item.
      loaders.put(Mantle.getResource("item_layer"), MantleItemLayerModel.LOADER);
      loaders.put(Mantle.getResource("nbt_key"), NBTKeyModel.LOADER);
      // same story for the block model loaders: seamless/colored glass, the scrolling textures and the
      // retextured blocks all depended on these, without them the models bake as plain vanilla cubes
      loaders.put(Mantle.getResource("connected"), ConnectedModel.LOADER);
      loaders.put(Mantle.getResource("retextured"), RetexturedModel.LOADER);
      loaders.put(Mantle.getResource("colored_block"), ColoredBlockModel.LOADER);
    });
  }

  static void clientSetup() {
    // Fabric has no model level render type, so every non solid block is registered explicitly
    TinkerRenderLayers.register();
    Font unicode = unicodeFontRender();
    TinkerBook.MATERIALS_AND_YOU.fontRenderer = unicode;
    TinkerBook.TINKERS_GADGETRY.fontRenderer = unicode;
    TinkerBook.PUNY_SMELTING.fontRenderer = unicode;
    TinkerBook.MIGHTY_SMELTING.fontRenderer = unicode;
    TinkerBook.FANTASTIC_FOUNDRY.fontRenderer = unicode;
    TinkerBook.ENCYCLOPEDIA.fontRenderer = unicode;
  }

  static void registerParticleFactories() {
    // TODO(fabric): replaced Forge's RegisterParticleProvidersEvent with the Fabric particle factory registry
    ParticleFactoryRegistry.getInstance().register(TinkerCommons.fluidParticle.get(), new FluidParticle.Factory());
  }

  /** @deprecated use {@link BookScreen#getUniformFont()} */
  @Deprecated(forRemoval = true)
  public static Font unicodeFontRender() {
    return BookScreen.getUniformFont();
  }
}
