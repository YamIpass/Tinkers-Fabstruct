package slimeknights.tconstruct.world;

import com.google.common.collect.ImmutableSet;
import io.github.fabricators_of_create.porting_lib.common.util.PlantType;
import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.OptionalDispenseItemBehavior;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters;
import net.minecraft.world.item.CreativeModeTab.Output;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockBehaviour.OffsetType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import slimeknights.mantle.registration.object.EntityObject;
import slimeknights.mantle.registration.object.EnumObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.mantle.registration.object.WoodBlockObject;
import slimeknights.mantle.registration.object.WoodBlockObject.WoodVariant;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.common.TinkerModule;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.registration.GeodeItemObject;
import slimeknights.tconstruct.common.registration.GeodeItemObject.BudSize;
import slimeknights.tconstruct.library.json.loot.equipment.MobEquipmentManager;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.TinkerCommons;
import slimeknights.tconstruct.shared.TinkerMaterials;
import slimeknights.tconstruct.shared.block.SlimeType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.world.block.CongealedSlimeBlock;
import slimeknights.tconstruct.world.block.CrystalClusterBlock;
import slimeknights.tconstruct.world.block.DirtType;
import slimeknights.tconstruct.world.block.FoliageType;
import slimeknights.tconstruct.world.block.PiglinHeadBlock;
import slimeknights.tconstruct.world.block.PiglinWallHeadBlock;
import slimeknights.tconstruct.world.block.SlimeDirtBlock;
import slimeknights.tconstruct.world.block.SlimeFungusBlock;
import slimeknights.tconstruct.world.block.SlimeGrassBlock;
import slimeknights.tconstruct.world.block.SlimeLeavesBlock;
import slimeknights.tconstruct.world.block.SlimeNyliumBlock;
import slimeknights.tconstruct.world.block.SlimePropaguleBlock;
import slimeknights.tconstruct.world.block.SlimePropaguleLeavesBlock;
import slimeknights.tconstruct.world.block.SlimeRootsBlock;
import slimeknights.tconstruct.world.block.SlimeSaplingBlock;
import slimeknights.tconstruct.world.block.SlimeTallGrassBlock;
import slimeknights.tconstruct.world.block.SlimeVineBlock;
import slimeknights.tconstruct.world.block.StickySlimeBlock;
import slimeknights.tconstruct.world.entity.EnderSlimeEntity;
import slimeknights.tconstruct.world.entity.SkySlimeEntity;
import slimeknights.tconstruct.world.entity.SlimePlacementPredicate;
import slimeknights.tconstruct.world.entity.TerracubeEntity;
import slimeknights.tconstruct.world.item.EndermanHeadItem;
import slimeknights.tconstruct.world.item.SlimeGrassSeedItem;
import slimeknights.tconstruct.world.worldgen.trees.SlimeTree;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Contains blocks and items relevant to structures and world gen
 */
@SuppressWarnings("unused")
public final class TinkerWorld extends TinkerModule {
  public TinkerWorld() {
    MobEquipmentManager.init();
  }

  public static final PlantType SLIME_PLANT_TYPE = PlantType.get("slime");

  /** Creative tab for anything that is naturally found in the world */
  public static final RegistryObject<CreativeModeTab> tabWorld = CREATIVE_TABS.register(
    "world", () -> FabricItemGroup.builder().title(TConstruct.makeTranslation("itemGroup", "world"))
      .icon(() -> new ItemStack(TinkerWorld.cobaltOre))
      .displayItems(TinkerWorld::addTabItems)
      // TODO(fabric): Forge's withTabsBefore() has no Fabric counterpart
      .build());

  /*
   * Block base properties
   */

  /*
   * Metals
   */
  // ores
  public static final ItemObject<Block> cobaltOre = BLOCKS.register("cobalt_ore", () -> new Block(builder(MapColor.NETHER, SoundType.NETHER_ORE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(10.0F)), BLOCK_ITEM);
  public static final ItemObject<Block> rawCobaltBlock = BLOCKS.register("raw_cobalt_block", () -> new Block(builder(MapColor.COLOR_BLUE, SoundType.NETHER_ORE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(6.0f, 7.0f)), BLOCK_ITEM);
  public static final ItemObject<Item> rawCobalt = ITEMS.register("raw_cobalt", ITEM_PROPS);

  // shards
  public static final ItemObject<Item> steelShard = ITEMS.register("steel_shard", TOOLTIP_ITEM);
  public static final ItemObject<Item> cobaltShard = ITEMS.register("cobalt_shard", TOOLTIP_ITEM);
  public static final ItemObject<Item> knightmetalShard = ITEMS.register("knightmetal_shard", TOOLTIP_ITEM);
  public static final ItemObject<Block> steelCluster = BLOCKS.register("steel_cluster", () -> new CrystalClusterBlock(Sounds.SKY_CRYSTAL_CHIME.getSound(), 7, 3, clusterProps().mapColor(MapColor.STONE).lightLevel(state -> 5).sound(SoundType.METAL)), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<Block> cobaltCluster = BLOCKS.register("cobalt_cluster", () -> new CrystalClusterBlock(Sounds.ICHOR_CRYSTAL_CHIME.getSound(), 7, 3, clusterProps().mapColor(MapColor.COLOR_BLUE).lightLevel(state -> 8).sound(SoundType.NETHERITE_BLOCK)), TOOLTIP_BLOCK_ITEM);
  public static final ItemObject<Block> knightmetalCluster = BLOCKS.register("knightmetal_cluster", () -> new CrystalClusterBlock(Sounds.ENDER_CRYSTAL_CHIME.getSound(), 7, 3, clusterProps().mapColor(MapColor.GRASS).lightLevel(state -> 12).sound(SoundType.NETHERITE_BLOCK)), TOOLTIP_BLOCK_ITEM);

  // slime
  public static final EnumObject<SlimeType, SlimeBlock> slime = Util.make(() -> {
    Function<SlimeType,BlockBehaviour.Properties> slimeProps = type -> builder(type.getMapColor(), SoundType.SLIME_BLOCK).friction(0.8F).sound(SoundType.SLIME_BLOCK).noOcclusion();
    return new EnumObject.Builder<SlimeType, SlimeBlock>(SlimeType.class)
      .put(SlimeType.EARTH, (SlimeBlock) Blocks.SLIME_BLOCK)
      // sky slime: sticks to anything, but will not pull back
      .put(SlimeType.SKY,   BLOCKS.register("sky_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.SKY), (state, other) -> true), TOOLTIP_BLOCK_ITEM))
      // ichor: does not stick to self, but sticks to anything else
      .put(SlimeType.ICHOR, BLOCKS.register("ichor_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.ICHOR).lightLevel(s -> SlimeType.ICHOR.getLightLevel()),
                                                                                      (state, other) -> other.getBlock() != state.getBlock()), TOOLTIP_BLOCK_ITEM))
      // ender: only sticks to self
      .put(SlimeType.ENDER, BLOCKS.register("ender_slime", () -> new StickySlimeBlock(slimeProps.apply(SlimeType.ENDER), (state, other) -> other.getBlock() == state.getBlock()), TOOLTIP_BLOCK_ITEM))
      // blood slime: not sticky, and honey won't stick to it, good for bounce pads
      .build();
  });
  public static final EnumObject<SlimeType, CongealedSlimeBlock> congealedSlime = BLOCKS.registerEnum(SlimeType.values(), "congealed_slime", type -> new CongealedSlimeBlock(builder(type.getMapColor(), SoundType.SLIME_BLOCK).strength(0.5F).friction(0.5F).lightLevel(s -> type.getLightLevel())), TOOLTIP_BLOCK_ITEM);

  // island blocks
  public static final EnumObject<DirtType, Block> slimeDirt = BLOCKS.registerEnum(DirtType.TINKER, "slime_dirt", (type) -> new SlimeDirtBlock(builder(type.getMapColor(), SoundType.SLIME_BLOCK).strength(1.9f)), TOOLTIP_BLOCK_ITEM);
  public static final EnumObject<DirtType, Block> allDirt = new EnumObject.Builder<DirtType, Block>(DirtType.class).put(DirtType.VANILLA, () -> Blocks.DIRT).putAll(slimeDirt).build();

  /** Grass variants, the name represents the dirt type */
  public static final EnumObject<FoliageType, Block> vanillaSlimeGrass, earthSlimeGrass, skySlimeGrass, enderSlimeGrass, ichorSlimeGrass;
  /** Map of dirt type to slime grass type. Each slime grass is a map from foliage to grass type */
  public static final Map<DirtType, EnumObject<FoliageType, Block>> slimeGrass = new EnumMap<>(DirtType.class);

	static {
    Function<FoliageType,BlockBehaviour.Properties> slimeGrassProps = type -> builder(type.getMapColor(), SoundType.SLIME_BLOCK).strength(2.0f).requiresCorrectToolForDrops().randomTicks();
    Function<FoliageType, Block> slimeGrassRegister = type -> type.isNether() ? new SlimeNyliumBlock(slimeGrassProps.apply(type), type) : new SlimeGrassBlock(slimeGrassProps.apply(type), type);
    // blood is not an exact match for vanilla, but close enough
    FoliageType[] values = FoliageType.values();
    vanillaSlimeGrass = BLOCKS.registerEnum(values, "vanilla_slime_grass", slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
    earthSlimeGrass   = BLOCKS.registerEnum(values, "earth_slime_grass",   slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
    skySlimeGrass     = BLOCKS.registerEnum(values, "sky_slime_grass",     slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
    enderSlimeGrass   = BLOCKS.registerEnum(values, "ender_slime_grass",   slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
    ichorSlimeGrass   = BLOCKS.registerEnum(values, "ichor_slime_grass",   slimeGrassRegister, TOOLTIP_BLOCK_ITEM);
    slimeGrass.put(DirtType.VANILLA, vanillaSlimeGrass);
    slimeGrass.put(DirtType.EARTH, earthSlimeGrass);
    slimeGrass.put(DirtType.SKY,   skySlimeGrass);
    slimeGrass.put(DirtType.ENDER, enderSlimeGrass);
    slimeGrass.put(DirtType.ICHOR, ichorSlimeGrass);
  }
  public static final EnumObject<FoliageType, SlimeGrassSeedItem> slimeGrassSeeds = ITEMS.registerEnum(FoliageType.values(), "slime_grass_seeds", type -> new SlimeGrassSeedItem(ITEM_PROPS, type));

  /** Creates a wood variant properties function */
  private static Function<WoodVariant,BlockBehaviour.Properties> createSlimewood(MapColor planks, MapColor bark) {
    return type -> switch (type) {
      case WOOD -> BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).mapColor(bark).sound(SoundType.WOOD).requiresCorrectToolForDrops();
      case LOG -> BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? planks : bark).sound(SoundType.WOOD).requiresCorrectToolForDrops();
      default -> BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASS).mapColor(planks).sound(SoundType.SLIME_BLOCK);
    };
  }

  // wood
  public static final WoodBlockObject greenheart  = BLOCKS.registerWood("greenheart",  createSlimewood(MapColor.COLOR_LIGHT_GREEN, MapColor.COLOR_GREEN),     false);
  public static final WoodBlockObject skyroot     = BLOCKS.registerWood("skyroot",     createSlimewood(MapColor.COLOR_CYAN,        MapColor.TERRACOTTA_CYAN), false);
  public static final WoodBlockObject bloodshroom = BLOCKS.registerWood("bloodshroom", createSlimewood(MapColor.COLOR_RED,         MapColor.COLOR_ORANGE),    false);
  public static final WoodBlockObject enderbark   = BLOCKS.registerWood("enderbark",   createSlimewood(MapColor.COLOR_BLACK,       MapColor.COLOR_BLACK),     false);
  public static final ItemObject<Block> enderbarkRoots = BLOCKS.register("enderbark_roots", () -> new SlimeRootsBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS).strength(0.7F).randomTicks().sound(SoundType.MANGROVE_ROOTS).noOcclusion().isSuffocating(Blocks::never).isViewBlocking(Blocks::never).noOcclusion()), BLOCK_ITEM);
  public static final EnumObject<SlimeType,Block> slimyEnderbarkRoots = BLOCKS.registerEnum(SlimeType.values(), "enderbark_roots", type -> new SlimeDirtBlock(BlockBehaviour.Properties.of().mapColor(type.getMapColor()).strength(0.7F).sound(SoundType.MUDDY_MANGROVE_ROOTS).lightLevel(s -> type.getLightLevel())), BLOCK_ITEM);

  // plants
  public static final EnumObject<FoliageType, SlimeTallGrassBlock> slimeFern, slimeTallGrass;
  static {
    Function<FoliageType,BlockBehaviour.Properties> props = type -> {
      BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().mapColor(type.getMapColor());
      if (type.isNether()) {
        properties.sound(SoundType.ROOTS).offsetType(OffsetType.XZ);
      } else {
        properties.sound(SoundType.GRASS).offsetType(OffsetType.XYZ);
      }
      return properties.replaceable().instabreak().noCollission().pushReaction(PushReaction.DESTROY);
    };
    slimeFern = BLOCKS.registerEnum(FoliageType.values(), "slime_fern", type -> new SlimeTallGrassBlock(props.apply(type), type), BLOCK_ITEM);
    slimeTallGrass = BLOCKS.registerEnum(FoliageType.values(), "slime_tall_grass", type -> new SlimeTallGrassBlock(props.apply(type), type), BLOCK_ITEM);
  }
  public static final EnumObject<FoliageType,FlowerPotBlock> pottedSlimeFern = BLOCKS.registerPottedEnum(FoliageType.values(), "slime_fern", slimeFern);

  // trees
  public static final EnumObject<FoliageType, Block> slimeSapling = Util.make(() -> {
    Function<FoliageType,BlockBehaviour.Properties> props = type -> builder(type.getMapColor(), type.isNether() ? SoundType.FUNGUS : SoundType.GRASS).instabreak().noCollission().pushReaction(PushReaction.DESTROY);
    return new EnumObject.Builder<FoliageType,Block>(FoliageType.class)
      .putAll(BLOCKS.registerEnum(FoliageType.OVERWORLD, "slime_sapling", (type) -> new SlimeSaplingBlock(new SlimeTree(type), type, props.apply(type).randomTicks()), TOOLTIP_BLOCK_ITEM))
      .put(FoliageType.BLOOD, BLOCKS.register("blood_slime_sapling", () -> new SlimeFungusBlock(props.apply(FoliageType.BLOOD), TinkerStructures.bloodSlimeFungus), TOOLTIP_BLOCK_ITEM))
      .put(FoliageType.ICHOR, BLOCKS.register("ichor_slime_sapling", () -> new SlimeFungusBlock(props.apply(FoliageType.ICHOR), TinkerStructures.ichorSlimeFungus), BLOCK_ITEM))
      .put(FoliageType.ENDER, BLOCKS.register("ender_slime_sapling", () -> new SlimePropaguleBlock(new SlimeTree(FoliageType.ENDER), FoliageType.ENDER, props.apply(FoliageType.ENDER).randomTicks()), TOOLTIP_BLOCK_ITEM))
      .build();
  });
  public static final EnumObject<FoliageType,FlowerPotBlock> pottedSlimeSapling = BLOCKS.registerPottedEnum(FoliageType.values(), "slime_sapling", slimeSapling);
  public static final EnumObject<FoliageType, Block> slimeLeaves = new EnumObject.Builder<FoliageType, Block>(FoliageType.class)
    .putAll(BLOCKS.registerEnum(FoliageType.OVERWORLD, "slime_leaves", type -> new SlimeLeavesBlock(builder(type.getMapColor(), SoundType.GRASS).strength(1.0f).randomTicks().noOcclusion().isValidSpawn(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never).pushReaction(PushReaction.DESTROY).isRedstoneConductor(Blocks::never), type), BLOCK_ITEM))
    .putAll(BLOCKS.registerEnum(FoliageType.NETHER, "slime_leaves", type -> new Block(builder(type.getMapColor(), SoundType.WART_BLOCK).strength(1.5F).isValidSpawn((s, w, p, e) -> false)), BLOCK_ITEM))
    .put(FoliageType.ENDER, BLOCKS.register("ender_slime_leaves", () -> new SlimePropaguleLeavesBlock(builder(FoliageType.ENDER.getMapColor(), SoundType.GRASS).strength(1.0f).randomTicks().noOcclusion().isValidSpawn(Blocks::never).isSuffocating(Blocks::never).isViewBlocking(Blocks::never).pushReaction(PushReaction.DESTROY).isRedstoneConductor(Blocks::never), FoliageType.ENDER), BLOCK_ITEM))
    .build();

  // slime vines
  public static final ItemObject<SlimeVineBlock> skySlimeVine, enderSlimeVine;
  static {
    Function<SlimeType,BlockBehaviour.Properties> props = type -> builder(type.getMapColor(), SoundType.GRASS).replaceable().strength(0.75F).noCollission().randomTicks().pushReaction(PushReaction.DESTROY);
    skySlimeVine = BLOCKS.register("sky_slime_vine", () -> new SlimeVineBlock(props.apply(SlimeType.SKY), SlimeType.SKY), BLOCK_ITEM);
    enderSlimeVine = BLOCKS.register("ender_slime_vine", () -> new SlimeVineBlock(props.apply(SlimeType.ENDER), SlimeType.ENDER), BLOCK_ITEM);
  }

  // geodes
  // earth
  public static final GeodeItemObject earthGeode = BLOCKS.registerGeode("earth_slime_crystal", MapColor.COLOR_LIGHT_GREEN, Sounds.EARTH_CRYSTAL, Sounds.EARTH_CRYSTAL_CHIME.getSound(), Sounds.EARTH_CRYSTAL_CLUSTER, 3, ITEM_PROPS);
  public static final ResourceKey<ConfiguredFeature<?,?>> configuredEarthGeode = key(Registries.CONFIGURED_FEATURE, "earth_geode");
  public static final ResourceKey<PlacedFeature> placedEarthGeode = key(Registries.PLACED_FEATURE, "earth_geode");
  // sky
  public static final GeodeItemObject skyGeode   = BLOCKS.registerGeode("sky_slime_crystal", MapColor.COLOR_BLUE, Sounds.SKY_CRYSTAL, Sounds.SKY_CRYSTAL_CHIME.getSound(), Sounds.SKY_CRYSTAL_CLUSTER, 0, ITEM_PROPS);
  public static final ResourceKey<ConfiguredFeature<?,?>> configuredSkyGeode = key(Registries.CONFIGURED_FEATURE, "sky_geode");
  public static final ResourceKey<PlacedFeature> placedSkyGeode = key(Registries.PLACED_FEATURE, "sky_geode");
  // ichor
  public static final GeodeItemObject ichorGeode = BLOCKS.registerGeode("ichor_slime_crystal", MapColor.COLOR_ORANGE, Sounds.ICHOR_CRYSTAL, Sounds.ICHOR_CRYSTAL_CHIME.getSound(), Sounds.ICHOR_CRYSTAL_CLUSTER, 10, ITEM_PROPS);
  public static final ResourceKey<ConfiguredFeature<?,?>> configuredIchorGeode = key(Registries.CONFIGURED_FEATURE, "ichor_geode");
  public static final ResourceKey<PlacedFeature> placedIchorGeode = key(Registries.PLACED_FEATURE, "ichor_geode");
  // ender
  public static final GeodeItemObject enderGeode = BLOCKS.registerGeode("ender_slime_crystal", MapColor.COLOR_PURPLE, Sounds.ENDER_CRYSTAL, Sounds.ENDER_CRYSTAL_CHIME.getSound(), Sounds.ENDER_CRYSTAL_CLUSTER, 7, ITEM_PROPS);
  public static final ResourceKey<ConfiguredFeature<?,?>> configuredEnderGeode = key(Registries.CONFIGURED_FEATURE, "ender_geode");
  public static final ResourceKey<PlacedFeature> placedEnderGeode = key(Registries.PLACED_FEATURE, "ender_geode");

  // TODO(fabric): Forge biome modifiers (data/tconstruct/forge/biome_modifier/*.json) placed the four geodes and the
  //  slime/terracube/cobalt spawns. Fabric has no biome modifier registry; the Fabric equivalent is
  //  net.fabricmc.fabric.api.biome.v1.BiomeModifications#addFeature and #addSpawn, as the reference port does in
  //  slimeknights.tconstruct.world.WorldEvents. The 3.12.1 modifier JSONs are not part of this port yet.

  // heads
  public static final EnumObject<TinkerHeadType,SkullBlock> heads = BLOCKS.registerEnumNoItem(TinkerHeadType.values(), "head", TinkerWorld::makeHead);
  public static final EnumObject<TinkerHeadType,WallSkullBlock> wallHeads = BLOCKS.registerEnumNoItem(TinkerHeadType.values(), "wall_head", TinkerWorld::makeWallHead);
  public static final EnumObject<TinkerHeadType,StandingAndWallBlockItem> headItems = ITEMS.registerEnum(TinkerHeadType.values(), "head", TinkerWorld::makeHeadItem);

  /*
   * Entities
   */
  // our own copy of the slime to make spawning a bit easier
  public static final EntityObject<SkySlimeEntity> skySlimeEntity = ENTITIES.registerWithEgg("sky_slime", () ->
    EntityType.Builder.of(SkySlimeEntity::new, MobCategory.MONSTER)
                      // TODO(fabric): Forge added setShouldReceiveVelocityUpdates(true) and a custom client
                      //  factory; vanilla always syncs velocity and has no client factory
                      .clientTrackingRange(20)
                      .sized(2.04F, 2.04F), 0x47eff5, 0xacfff4);
  public static final EntityObject<EnderSlimeEntity> enderSlimeEntity = ENTITIES.registerWithEgg("ender_slime", () ->
    EntityType.Builder.of(EnderSlimeEntity::new, MobCategory.MONSTER)
                      // TODO(fabric): Forge added setShouldReceiveVelocityUpdates(true) and a custom client
                      //  factory; vanilla always syncs velocity and has no client factory
                      .clientTrackingRange(32)
                      .sized(2.04F, 2.04F), 0x6300B0, 0xD37CFF);
  public static final EntityObject<TerracubeEntity> terracubeEntity = ENTITIES.registerWithEgg("terracube", () ->
    EntityType.Builder.of(TerracubeEntity::new, MobCategory.MONSTER)
                      // TODO(fabric): Forge added setShouldReceiveVelocityUpdates(true) and a custom client
                      //  factory; vanilla always syncs velocity and has no client factory
                      .clientTrackingRange(8)
                      .sized(2.04F, 2.04F), 0xAFB9D6, 0xA1A7B1);


  /*
   * Particles
   */
  public static final RegistryObject<SimpleParticleType> skySlimeParticle = PARTICLE_TYPES.register("sky_slime", () -> FabricParticleTypes.simple(false));
  public static final RegistryObject<SimpleParticleType> enderSlimeParticle = PARTICLE_TYPES.register("ender_slime", () -> FabricParticleTypes.simple(false));
  public static final RegistryObject<SimpleParticleType> terracubeParticle = PARTICLE_TYPES.register("terracube", () -> FabricParticleTypes.simple(false));

  /*
   * Features
   */
  // small veins, standard distribution
  public static ResourceKey<ConfiguredFeature<?,?>> configuredSmallCobaltOre = key(Registries.CONFIGURED_FEATURE, "cobalt_ore_small");
  public static ResourceKey<PlacedFeature> placedSmallCobaltOre = key(Registries.PLACED_FEATURE, "cobalt_ore_small");
  // large veins, around y=16, up to 48
  public static ResourceKey<ConfiguredFeature<?,?>> configuredLargeCobaltOre = key(Registries.CONFIGURED_FEATURE, "cobalt_ore_large");
  public static ResourceKey<PlacedFeature> placedLargeCobaltOre = key(Registries.PLACED_FEATURE, "cobalt_ore_large");


  /*
   * Events
   */

  /** Registers the entity data, spawn placements, compostables and flammability of everything in this module */
  public static void init() {
    // TODO(fabric): Forge called the handlers below from the mod event bus. Fabric has no mod loading event bus, so
    //  TConstruct#onInitialize calls this instead.
    entityAttributes();
    registerSpawnPlacement();
    commonSetup();
    // Forge read data/tconstruct/forge/biome_modifier/*.json for these; Fabric needs the fabric biome api instead
    TinkerBiomeModifications.init();
  }

  public static void entityAttributes() {
    FabricDefaultAttributeRegistry.register(skySlimeEntity.get(), Monster.createMonsterAttributes());
    FabricDefaultAttributeRegistry.register(enderSlimeEntity.get(), Monster.createMonsterAttributes());
    FabricDefaultAttributeRegistry.register(terracubeEntity.get(), Monster.createMonsterAttributes());
  }

  /** Sets all fire info for the given wood */
  private static void setWoodFireInfo(FireBlock fireBlock, WoodBlockObject wood) {
    // planks
    FlammableBlockRegistry.getDefaultInstance().add(wood.get(), 5, 20);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getSlab(), 5, 20);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getStairs(), 5, 20);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getFence(), 5, 20);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getFenceGate(), 5, 20);
    // logs
    FlammableBlockRegistry.getDefaultInstance().add(wood.getLog(), 5, 5);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getStrippedLog(), 5, 5);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getWood(), 5, 5);
    FlammableBlockRegistry.getDefaultInstance().add(wood.getStrippedWood(), 5, 5);
  }

  public static void registerSpawnPlacement() {
    // TODO(fabric): Forge's SpawnPlacementRegisterEvent.Operation.OR merged the new predicate with the existing one;
    //  vanilla SpawnPlacements#register refuses duplicates, so the vanilla slime rule is merged by hand to keep both
    //  and the merged rule is written straight into SpawnPlacements.DATA_BY_TYPE (widened by the access widener).
    SlimePlacementPredicate<Slime> earthSlime = new SlimePlacementPredicate<>(TinkerTags.Blocks.EARTH_SLIME_SPAWN);
    SpawnPlacements.DATA_BY_TYPE.put(EntityType.SLIME, new SpawnPlacements.Data(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SpawnPlacements.Type.ON_GROUND,
      (SpawnPlacements.SpawnPredicate<Slime>) (type, level, spawnType, pos, random) -> Slime.checkSlimeSpawnRules(type, level, spawnType, pos, random) || earthSlime.test(type, level, spawnType, pos, random)));
    SpawnPlacements.register(skySlimeEntity.get(),   SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.SKY_SLIME_SPAWN));
    SpawnPlacements.register(enderSlimeEntity.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.ENDER_SLIME_SPAWN));
    SpawnPlacements.register(terracubeEntity.get(),  SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TerracubeEntity::canSpawnHere);
  }

  public static void commonSetup() {
    // TODO(fabric): Forge ran this from the mod event bus inside event.enqueueWork(). Fabric has no mod loading event
    //  bus, so the content registries are simply filled during mod initialization.
    // compostables
    slimeLeaves.forEach((type, block) -> CompostingChanceRegistry.INSTANCE.add(block, type.isNether() ? 0.85f : 0.35f));
    slimeSapling.forEach(block -> CompostingChanceRegistry.INSTANCE.add(block, 0.35f));
    slimeTallGrass.forEach(block -> CompostingChanceRegistry.INSTANCE.add(block, 0.35f));
    slimeFern.forEach(block -> CompostingChanceRegistry.INSTANCE.add(block, 0.65f));
    slimeGrassSeeds.forEach(block -> CompostingChanceRegistry.INSTANCE.add(block, 0.35F));
    CompostingChanceRegistry.INSTANCE.add(skySlimeVine, 0.5f);
    CompostingChanceRegistry.INSTANCE.add(enderSlimeVine, 0.5f);
    CompostingChanceRegistry.INSTANCE.add(enderbarkRoots, 0.4f);

    // head equipping
    DispenseItemBehavior dispenseArmor = new OptionalDispenseItemBehavior() {
      @Override
      protected ItemStack execute(BlockSource source, ItemStack stack) {
        this.setSuccess(ArmorItem.dispenseArmor(source, stack));
        return stack;
      }
    };
    TinkerWorld.heads.forEach(head -> DispenserBlock.registerBehavior(head, dispenseArmor));
    // heads in firework stars
    TinkerWorld.heads.forEach(head -> FireworkStarRecipe.SHAPE_BY_ITEM.put(head.asItem(), FireworkRocketItem.Shape.CREEPER));
    // inject heads into the tile entity type
    ImmutableSet.Builder<Block> builder = ImmutableSet.builder();
    builder.addAll(BlockEntityType.SKULL.validBlocks);
    TinkerWorld.heads.forEach(head -> builder.add(head));
    TinkerWorld.wallHeads.forEach(head -> builder.add(head));
    BlockEntityType.SKULL.validBlocks = builder.build();

    // flammability
    // TODO(fabric): Forge's FireBlock#setFlammable is private in vanilla, Fabric's FlammableBlockRegistry is the hook
    BiConsumer<FoliageType, Block> plantFireInfo = (type, block) -> {
      if (!type.isNether()) {
        FlammableBlockRegistry.getDefaultInstance().add(block, 30, 60);
      }
    };
    slimeLeaves.forEach(plantFireInfo);
    slimeTallGrass.forEach(plantFireInfo);
    slimeFern.forEach(plantFireInfo);
    // vines
    FlammableBlockRegistry.getDefaultInstance().add(skySlimeVine.get(), 15, 100);
    FlammableBlockRegistry.getDefaultInstance().add(enderSlimeVine.get(), 15, 100);
  }

  // TODO(fabric): 3.12.1 generated the world recipes and mob equipment here through GatherDataEvent. Fabric datagen is
  //  not wired up for this module and both providers live outside this port, so the whole event is dropped.

  /** Adds all relevant items to the creative tab */
  private static void addTabItems(ItemDisplayParameters itemDisplayParameters, Output output) {
    // ores
    output.accept(cobaltOre);
    output.accept(rawCobalt);
    output.accept(rawCobaltBlock);

    // monsters
    output.accept(terracubeEntity);
    output.accept(skySlimeEntity);
    output.accept(enderSlimeEntity);

    // mob drops
    output.accept(TinkerMaterials.necroticBone);
    output.accept(TinkerModifiers.dragonScale);
    // skip necronium head unless necronium is enabled
    headItems.forEach((type, head) -> output.accept(head));

    // earth is not in the loop as we only add congealed
    output.accept(congealedSlime.get(SlimeType.EARTH));
    for (SlimeType type : SlimeType.TINKER) {
      output.accept(TinkerCommons.slimeball.get(type));
      output.accept(slime.get(type));
      output.accept(congealedSlime.get(type));
    }

    // slime wood
    accept(output, greenheart);
    output.accept(slimeSapling.get(FoliageType.EARTH));
    output.accept(slimeLeaves.get(FoliageType.EARTH));
    accept(output, skyroot);
    output.accept(slimeSapling.get(FoliageType.SKY));
    output.accept(slimeLeaves.get(FoliageType.SKY));
    accept(output, bloodshroom);
    output.accept(slimeSapling.get(FoliageType.BLOOD));
    output.accept(slimeLeaves.get(FoliageType.BLOOD));
    accept(output, enderbark);
    output.accept(slimeSapling.get(FoliageType.ENDER));
    output.accept(slimeLeaves.get(FoliageType.ENDER));
    output.accept(enderbarkRoots);
    accept(output, slimyEnderbarkRoots);

    // slime foliage
    accept(output, slimeDirt);
    for (FoliageType type : FoliageType.VISIBLE) {
      output.accept(slimeTallGrass.get(type));
      output.accept(slimeFern.get(type));
      if (type == FoliageType.SKY) {
        output.accept(skySlimeVine);
      } else if (type == FoliageType.ENDER) {
        output.accept(enderSlimeVine);
      }
      output.accept(slimeGrassSeeds.get(type));
      output.accept(vanillaSlimeGrass.get(type));
      output.accept(earthSlimeGrass.get(type));
      output.accept(skySlimeGrass.get(type));
      output.accept(ichorSlimeGrass.get(type));
      output.accept(enderSlimeGrass.get(type));
    }

    // geodes
    accept(output, earthGeode);
    accept(output, skyGeode);
    output.accept(steelShard);
    output.accept(steelCluster);
    accept(output, ichorGeode);
    output.accept(cobaltShard);
    output.accept(cobaltCluster);
    accept(output, enderGeode);
    output.accept(knightmetalShard);
    output.accept(knightmetalCluster);
  }

  /** Add a geode to the creative tab */
  private static void accept(CreativeModeTab.Output output, GeodeItemObject geode) {
    output.accept(geode);
    output.accept(geode.getBlock());
    output.accept(geode.getBudding());
    for (BudSize size : BudSize.values()) {
      output.accept(geode.getBud(size));
    }
  }

  /* helpers */

  /** Creates a skull block for the given head type */
  private static SkullBlock makeHead(TinkerHeadType type) {
    // TODO: find a way for Forge to give me new instruments
    BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(1.0F).pushReaction(PushReaction.DESTROY);
    if (type.isPiglin()) {
      return new PiglinHeadBlock(type, props);
    }
    return new SkullBlock(type, props);
  }

  /** Creates a skull wall block for the given head type */
  private static WallSkullBlock makeWallHead(TinkerHeadType type) {
    BlockBehaviour.Properties props = BlockBehaviour.Properties.of().strength(1.0F).dropsLike(heads.get(type));
    if (type.isPiglin()) {
      return new PiglinWallHeadBlock(type, props);
    }
    return new WallSkullBlock(type, props);
  }

  /** Creates a skull wall block for the given head type */
  private static StandingAndWallBlockItem makeHeadItem(TinkerHeadType type) {
    Item.Properties properties = new Item.Properties().rarity(Rarity.UNCOMMON);
    if (type == TinkerHeadType.ENDERMAN) {
      return new EndermanHeadItem(heads.get(type), wallHeads.get(type), properties, Direction.DOWN);
    }
    return new StandingAndWallBlockItem(heads.get(type), wallHeads.get(type), properties, Direction.DOWN);
  }

  /** Properties for a cluster of shards. */
  private static Properties clusterProps() {
    return BlockBehaviour.Properties.of().forceSolidOn().noOcclusion().randomTicks().strength(2.5f).requiresCorrectToolForDrops().pushReaction(PushReaction.DESTROY);
  }
}
