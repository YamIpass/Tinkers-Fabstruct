package slimeknights.tconstruct.world;

import com.google.common.collect.ImmutableMap;
import io.github.fabricators_of_create.porting_lib.common.util.Lazy;
import io.github.fabricators_of_create.porting_lib.event.client.CreateSkullModelsCallback;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.model.PiglinHeadModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.library.client.particle.SlimeParticle;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.data.material.MaterialIds;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.client.DragonSkullModel;
import slimeknights.tconstruct.world.client.SkullModelHelper;
import slimeknights.tconstruct.world.client.SlimeColorReloadListener;
import slimeknights.tconstruct.world.client.SlimeColorizer;
import slimeknights.tconstruct.world.client.TerracubeRenderer;
import slimeknights.tconstruct.world.client.TinkerSlimeRenderer;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class WorldClientEvents extends ClientEventBase {
  static void addResourceListener() {
    for (FoliageType type : FoliageType.values()) {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SlimeColorReloadListener(type));
    }
  }

  static void registerParticleFactories() {
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
  }

  static void registerRenderers() {
    // TODO: do we really need a separate copy of each head for each mob, or can we reuse them?
    Supplier<LayerDefinition> normalHead = Lazy.of(SkullModel::createMobHeadLayer);
    Supplier<LayerDefinition> customHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 32, 16));
    Supplier<LayerDefinition> headOverlayCustom = Lazy.of(() -> SkullModelHelper.createHeadHatLayer(0, 16, 32, 32));
    registerLayerDefinition(TinkerHeadType.BLAZE, normalHead);
    registerLayerDefinition(TinkerHeadType.ENDERMAN, customHead);
    registerLayerDefinition(TinkerHeadType.STRAY, headOverlayCustom);

    // zombie
    registerLayerDefinition(TinkerHeadType.HUSK, Lazy.of(() -> SkullModelHelper.createHeadLayer(0, 0, 64, 64)));
    registerLayerDefinition(TinkerHeadType.DROWNED, headOverlayCustom);

    // spiders
    Supplier<LayerDefinition> spiderHead = Lazy.of(() -> SkullModelHelper.createHeadLayer(32, 4, 64, 32));
    registerLayerDefinition(TinkerHeadType.SPIDER, spiderHead);
    registerLayerDefinition(TinkerHeadType.CAVE_SPIDER, spiderHead);

    // piglin
    Supplier<LayerDefinition> piglinHead = Lazy.of(() -> LayerDefinition.create(PiglinHeadModel.createHeadModel(), 64, 64));
    registerLayerDefinition(TinkerHeadType.PIGLIN_BRUTE, piglinHead);
    registerLayerDefinition(TinkerHeadType.ZOMBIFIED_PIGLIN, piglinHead);

    // crafted
    EntityModelLayerRegistry.registerModelLayer(SkullModelHelper.FLUID_CANNON, headOverlayCustom::get);
  }

  static void registerSkullModels(ImmutableMap.Builder<SkullBlock.Type, SkullModelBase> builder, EntityModelSet modelSet) {
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> {
      if (type.isPiglin()) {
        builder.put(type, new PiglinHeadModel(modelSet.bakeLayer(layer)));
      } else {
        builder.put(type, new SkullModel(modelSet.bakeLayer(layer)));
      }
    });
  }

  static void registerEntityRenderers() {
    EntityRendererRegistry.register(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
  }

  public static void clientSetup() {
    // TODO(fabric): called from TConstruct's client initializer; Fabric has no client mod loading event bus
    // skull textures
    {
      registerHeadModel(TinkerHeadType.BLAZE, MaterialIds.blaze, new ResourceLocation("textures/entity/blaze.png"));
      registerHeadModel(TinkerHeadType.ENDERMAN, MaterialIds.enderPearl, TConstruct.getResource("textures/entity/skull/enderman.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.dragonScale, modelSet -> new DragonSkullModel(modelSet.bakeLayer(ModelLayers.DRAGON_SKULL)), new ResourceLocation("textures/entity/enderdragon/dragon.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.gunpowder, ModelLayers.CREEPER_HEAD, new ResourceLocation("textures/entity/creeper/creeper.png"));
      // skeleton
      SlimeskullArmorModel.registerHeadModel(MaterialIds.bone, ModelLayers.SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/skeleton.png"));
      SlimeskullArmorModel.registerHeadModel(MaterialIds.necroticBone, ModelLayers.WITHER_SKELETON_SKULL, new ResourceLocation("textures/entity/skeleton/wither_skeleton.png"));
      registerHeadModel(TinkerHeadType.STRAY, MaterialIds.ice, TConstruct.getResource("textures/entity/skull/stray.png"));
      // zombies
      SlimeskullArmorModel.registerHeadModel(MaterialIds.leather, ModelLayers.ZOMBIE_HEAD, new ResourceLocation("textures/entity/zombie/zombie.png"));
      registerHeadModel(TinkerHeadType.HUSK, MaterialIds.iron, new ResourceLocation("textures/entity/zombie/husk.png"));
      registerHeadModel(TinkerHeadType.DROWNED, MaterialIds.copper, TConstruct.getResource("textures/entity/skull/drowned.png"));
      // spider
      registerHeadModel(TinkerHeadType.SPIDER, MaterialIds.string, new ResourceLocation("textures/entity/spider/spider.png"));
      registerHeadModel(TinkerHeadType.CAVE_SPIDER, MaterialIds.darkthread, new ResourceLocation("textures/entity/spider/cave_spider.png"));
      // piglins
      SlimeskullArmorModel.registerPiglinHeadModel(MaterialIds.gold, ModelLayers.PIGLIN_HEAD, new ResourceLocation("textures/entity/piglin/piglin.png"));
      registerPiglinHeadModel(TinkerHeadType.PIGLIN_BRUTE, MaterialIds.roseGold, new ResourceLocation("textures/entity/piglin/piglin_brute.png"));
      registerPiglinHeadModel(TinkerHeadType.ZOMBIFIED_PIGLIN, MaterialIds.pigIron, new ResourceLocation("textures/entity/piglin/zombified_piglin.png"));
      // crafted
      SlimeskullArmorModel.registerHeadModel(MaterialIds.knightmetal, SkullModelHelper.FLUID_CANNON, TConstruct.getResource("textures/entity/skull/fluid_cannon.png"));
    }

    addResourceListener();
    registerParticleFactories();
    registerRenderers();
    registerEntityRenderers();
    registerBlockColorHandlers();
    registerItemColorHandlers();
    CreateSkullModelsCallback.EVENT.register(WorldClientEvents::registerSkullModels);
  }

  static void registerBlockColorHandlers() {
    // slime plants - blocks
    for (FoliageType type : FoliageType.values()) {
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
        TinkerWorld.vanillaSlimeGrass.get(type), TinkerWorld.earthSlimeGrass.get(type), TinkerWorld.skySlimeGrass.get(type),
        TinkerWorld.enderSlimeGrass.get(type), TinkerWorld.ichorSlimeGrass.get(type));
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, SlimeColorizer.LOOP_OFFSET),
        TinkerWorld.slimeLeaves.get(type));
      ColorProviderRegistry.BLOCK.register(
        (state, reader, pos, index) -> getSlimeColorByPos(pos, type, null),
        TinkerWorld.slimeFern.get(type), TinkerWorld.slimeTallGrass.get(type), TinkerWorld.pottedSlimeFern.get(type));
    }

    // vines
    ColorProviderRegistry.BLOCK.register(
      (state, reader, pos, index) -> getSlimeColorByPos(pos, FoliageType.SKY, SlimeColorizer.LOOP_OFFSET),
      TinkerWorld.skySlimeVine.get());
    ColorProviderRegistry.BLOCK.register(
      (state, reader, pos, index) -> getSlimeColorByPos(pos, FoliageType.ENDER, SlimeColorizer.LOOP_OFFSET),
      TinkerWorld.enderSlimeVine.get());
  }

  static void registerItemColorHandlers() {
    // TODO(fabric): Forge handed the client color managers to the event; Fabric looks them up per block, so only the
    //  block based alias overloads are available
    // slime grass items
    registerBlockItemColorAlias(TinkerWorld.vanillaSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.earthSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.skySlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.enderSlimeGrass);
    registerBlockItemColorAlias(TinkerWorld.ichorSlimeGrass);
    // plant items
    registerBlockItemColorAlias(TinkerWorld.slimeLeaves);
    registerBlockItemColorAlias(TinkerWorld.slimeFern);
    registerBlockItemColorAlias(TinkerWorld.slimeTallGrass);
    registerBlockItemColorAlias(TinkerWorld.skySlimeVine.get());
    registerBlockItemColorAlias(TinkerWorld.enderSlimeVine.get());
  }

  /**
   * Block colors for a slime type
   * @param pos   Block position
   * @param type  Slime foliage color
   * @param add   Offset position
   * @return  Color for the given position, or the default if position is null
   */
  private static int getSlimeColorByPos(@Nullable BlockPos pos, FoliageType type, @Nullable BlockPos add) {
    if (pos == null) {
      return type.getColor();
    }
    if (add != null) {
      pos = pos.offset(add);
    }

    return SlimeColorizer.getColorForPos(pos, type);
  }

  /** Registers a skull with the entity renderer and the slimeskull renderer */
  private static void registerHeadModel(TinkerHeadType skull, MaterialId materialId, ResourceLocation texture) {
    SkullBlockRenderer.SKIN_BY_TYPE.put(skull, texture);
    SlimeskullArmorModel.registerHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), texture);
  }

  /** Registers a skull with the entity renderer and the slimeskull renderer */
  private static void registerPiglinHeadModel(TinkerHeadType skull, MaterialId materialId, ResourceLocation texture) {
    SkullBlockRenderer.SKIN_BY_TYPE.put(skull, texture);
    SlimeskullArmorModel.registerPiglinHeadModel(materialId, SkullModelHelper.HEAD_LAYERS.get(skull), texture);
  }

  /** Register a layer without being under the minecraft domain. TODO: is this needed? */
  private static ModelLayerLocation registerLayer(String name) {
    ModelLayerLocation location = new ModelLayerLocation(TConstruct.getResource(name), "main");
    if (!ModelLayers.ALL_MODELS.add(location)) {
      throw new IllegalStateException("Duplicate registration for " + location);
    } else {
      return location;
    }
  }

  /** Registers a head layer definition with Fabric */
  private static void registerLayerDefinition(TinkerHeadType head, Supplier<LayerDefinition> supplier) {
    EntityModelLayerRegistry.registerModelLayer(SkullModelHelper.HEAD_LAYERS.get(head), supplier::get);
  }
}
