import io, re, sys

path = 'src/main/java/slimeknights/tconstruct/world/TinkerWorld.java'
src = io.open(path, encoding='utf-8').read()
orig = src

def rep(old, new, count=1):
    global src
    if old not in src:
        print('MISS:', old[:70].replace('\n', '\\n'))
        return
    src = src.replace(old, new, count)

# ---- imports: drop Forge / datagen ----
for line in [
    'import net.minecraft.data.DataGenerator;\n',
    'import net.minecraft.data.PackOutput;\n',
    'import net.minecraft.world.level.block.ComposterBlock;\n',
    'import net.minecraftforge.common.PlantType;\n',
    'import net.minecraftforge.common.world.BiomeModifier;\n',
    'import net.minecraftforge.data.event.GatherDataEvent;\n',
    'import net.minecraftforge.event.entity.EntityAttributeCreationEvent;\n',
    'import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;\n',
    'import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation;\n',
    'import net.minecraftforge.eventbus.api.SubscribeEvent;\n',
    'import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;\n',
    'import net.minecraftforge.registries.ForgeRegistries;\n',
    'import net.minecraftforge.registries.RegistryObject;\n',
    'import slimeknights.tconstruct.world.data.MobEquipmentProvider;\n',
    'import slimeknights.tconstruct.world.data.WorldRecipeProvider;\n',
]:
    rep(line, '')

rep('import com.google.common.collect.ImmutableSet;\n',
    'import com.google.common.collect.ImmutableSet;\n'
    'import io.github.fabricators_of_create.porting_lib.common.util.PlantType;\n'
    'import io.github.fabricators_of_create.porting_lib.util.RegistryObject;\n'
    'import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;\n'
    'import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;\n'
    'import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;\n'
    'import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;\n')

rep('import net.minecraft.world.entity.monster.Monster;\n',
    'import net.minecraft.world.entity.monster.Monster;\nimport net.minecraft.world.entity.monster.Slime;\n')

# ---- delegate for the vanilla slime block ----
rep('.put(SlimeType.EARTH, () -> (SlimeBlock)Blocks.SLIME_BLOCK)',
    '.putDelegate(SlimeType.EARTH, (SlimeBlock) Blocks.SLIME_BLOCK)')

# ---- potted enum needs the explicit values array on Mantle 1.11 ----
rep('BLOCKS.registerPottedEnum("slime_fern", slimeFern)',
    'BLOCKS.registerPottedEnum(FoliageType.values(), "slime_fern", slimeFern)')

# ---- biome modifier keys have no Fabric registry ----
rep('''  public static final ResourceKey<BiomeModifier> spawnEarthGeode = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "earth_geode");
  public static final ResourceKey<BiomeModifier> spawnSkyGeode = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "sky_geode");
  public static final ResourceKey<BiomeModifier> spawnIchorGeode = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "ichor_geode");
  public static final ResourceKey<BiomeModifier> spawnEnderGeode = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "ender_geode");
''',
'''  // TODO(fabric): Forge biome modifiers (data/tconstruct/forge/biome_modifier/*.json) placed the four geodes and the
  //  slime/terracube/cobalt spawns. Fabric has no biome modifier registry; the Fabric equivalent is
  //  net.fabricmc.fabric.api.biome.v1.BiomeModifications#addFeature and #addSpawn, as the reference port does in
  //  slimeknights.tconstruct.world.WorldEvents. The 3.12.1 modifier JSONs are not part of this port yet.
''')

rep('''  public static final ResourceKey<BiomeModifier> spawnOverworldSlime = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "spawn_overworld_slime");
  public static final ResourceKey<BiomeModifier> spawnTerracube = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "spawn_terracube");
  public static final ResourceKey<BiomeModifier> spawnEndSlime = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "spawn_end_slime");
''', '')

rep('  public static ResourceKey<BiomeModifier> spawnCobaltOre = key(ForgeRegistries.Keys.BIOME_MODIFIERS, "cobalt_ore");\n', '')

# ---- entity builders: vanilla API only ----
entity_re = re.compile(
    r'EntityType\.Builder\.of\((\w+)::new, MobCategory\.MONSTER\)\n'
    r'\s*\.setShouldReceiveVelocityUpdates\(true\)\n'
    r'\s*\.setTrackingRange\((\d+)\)\n'
    r'\s*\.sized\(2\.04F, 2\.04F\)\n'
    r'\s*\.setCustomClientFactory\(\(spawnEntity, world\) -> TinkerWorld\.\w+\.get\(\)\.create\(world\)\)')

def entity_sub(m):
    return ('EntityType.Builder.of(%s::new, MobCategory.MONSTER)\n'
            '                      // TODO(fabric): Forge added setShouldReceiveVelocityUpdates(true) and a custom client\n'
            '                      //  factory; vanilla always syncs velocity and has no client factory\n'
            '                      .clientTrackingRange(%s)\n'
            '                      .sized(2.04F, 2.04F)') % (m.group(1), m.group(2))

src, n = entity_re.subn(entity_sub, src)
print('entities rewritten:', n)

# ---- particles ----
src = src.replace('new SimpleParticleType(false)', 'FabricParticleTypes.simple(false)')

# ---- events ----
rep('''  @SubscribeEvent
  void entityAttributes(EntityAttributeCreationEvent event) {
    event.put(skySlimeEntity.get(), Monster.createMonsterAttributes().build());
    event.put(enderSlimeEntity.get(), Monster.createMonsterAttributes().build());
    event.put(terracubeEntity.get(), Monster.createMonsterAttributes().build());
  }
''',
'''  /** Registers the entity data, spawn placements, compostables and flammability of everything in this module */
  public static void init() {
    // TODO(fabric): Forge called the handlers below from the mod event bus. Fabric has no mod loading event bus, so
    //  TConstruct#onInitialize calls this instead.
    entityAttributes();
    registerSpawnPlacement();
    commonSetup();
  }

  public static void entityAttributes() {
    FabricDefaultAttributeRegistry.register(skySlimeEntity.get(), Monster.createMonsterAttributes());
    FabricDefaultAttributeRegistry.register(enderSlimeEntity.get(), Monster.createMonsterAttributes());
    FabricDefaultAttributeRegistry.register(terracubeEntity.get(), Monster.createMonsterAttributes());
  }
''')

rep('''  @SubscribeEvent
  void registerSpawnPlacement(SpawnPlacementRegisterEvent event) {
    event.register(EntityType.SLIME, null, null, new SlimePlacementPredicate<>(TinkerTags.Blocks.EARTH_SLIME_SPAWN), Operation.OR);
    event.register(skySlimeEntity.get(),   SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.SKY_SLIME_SPAWN), Operation.OR);
    event.register(enderSlimeEntity.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.ENDER_SLIME_SPAWN), Operation.OR);
    event.register(terracubeEntity.get(),  SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TerracubeEntity::canSpawnHere, Operation.OR);

  }
''',
'''  public static void registerSpawnPlacement() {
    // TODO(fabric): Forge's SpawnPlacementRegisterEvent.Operation.OR merged the new predicate with the existing one;
    //  vanilla SpawnPlacements#register replaces it, so the vanilla slime rule is merged by hand to keep both.
    SlimePlacementPredicate<Slime> earthSlime = new SlimePlacementPredicate<>(TinkerTags.Blocks.EARTH_SLIME_SPAWN);
    SpawnPlacements.register(EntityType.SLIME, SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
      (type, level, spawnType, pos, random) -> Slime.checkSlimeSpawnRules(type, level, spawnType, pos, random) || earthSlime.test(type, level, spawnType, pos, random));
    SpawnPlacements.register(skySlimeEntity.get(),   SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.SKY_SLIME_SPAWN));
    SpawnPlacements.register(enderSlimeEntity.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new SlimePlacementPredicate<>(TinkerTags.Blocks.ENDER_SLIME_SPAWN));
    SpawnPlacements.register(terracubeEntity.get(),  SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, TerracubeEntity::canSpawnHere);
  }
''')

# ---- commonSetup + gatherData: rebuild the whole region ----
start = src.index('  @SubscribeEvent\n  void commonSetup(final FMLCommonSetupEvent event) {')
end_marker = '  /** Adds all relevant items to the creative tab */'
end = src.index(end_marker)
new_block = '''  public static void commonSetup() {
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
    TinkerWorld.heads.forEach(builder::add);
    TinkerWorld.wallHeads.forEach(builder::add);
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

'''
src = src[:start] + new_block + src[end:]

# ---- remaining setFlammable calls (setWoodFireInfo) ----
src = re.sub(r'fireBlock\.setFlammable\(([^,]+), (\d+), (\d+)\);',
             r'FlammableBlockRegistry.getDefaultInstance().add(\1, \2, \3);', src)

io.open(path, 'w', encoding='utf-8', newline='\n').write(src)
print('forge refs left:', src.count('minecraftforge'))
print('lines:', len(src.splitlines()), 'was', len(orig.splitlines()))
