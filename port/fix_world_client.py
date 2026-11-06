import io, re

path = 'src/main/java/slimeknights/tconstruct/world/WorldClientEvents.java'
src = io.open(path, encoding='utf-8').read()

def rep(old, new):
    global src
    if old not in src:
        print('MISS:', old[:70].replace('\n', '\\n'))
        return
    src = src.replace(old, new, 1)

# ---------------- imports ----------------
for line in [
    'import net.minecraft.client.color.block.BlockColors;\n',
    'import net.minecraft.client.color.item.ItemColors;\n',
    'import net.minecraftforge.api.distmarker.Dist;\n',
    'import net.minecraftforge.client.event.EntityRenderersEvent;\n',
    'import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;\n',
    'import net.minecraftforge.client.event.RegisterColorHandlersEvent;\n',
    'import net.minecraftforge.client.event.RegisterParticleProvidersEvent;\n',
    'import net.minecraftforge.common.util.Lazy;\n',
    'import net.minecraftforge.eventbus.api.SubscribeEvent;\n',
    'import net.minecraftforge.fml.common.Mod.EventBusSubscriber;\n',
    'import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;\n',
    'import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;\n',
]:
    rep(line, '')

rep('import net.minecraft.client.color.block.BlockColors;\n', '')  # already gone, ignore
rep('package slimeknights.tconstruct.world;\n\n',
    'package slimeknights.tconstruct.world;\n\n'
    'import com.google.common.collect.ImmutableMap;\n'
    'import io.github.fabricators_of_create.porting_lib.common.util.Lazy;\n'
    'import io.github.fabricators_of_create.porting_lib.event.client.CreateSkullModelsCallback;\n'
    'import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;\n'
    'import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;\n'
    'import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;\n'
    'import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;\n'
    'import net.fabricmc.fabric.api.resource.ResourceManagerHelper;\n')
rep('import net.minecraft.client.model.SkullModel;\n',
    'import net.minecraft.client.model.SkullModel;\nimport net.minecraft.client.model.SkullModelBase;\n')
rep('import net.minecraft.resources.ResourceLocation;\n',
    'import net.minecraft.resources.ResourceLocation;\nimport net.minecraft.server.packs.PackType;\n')

# ---------------- class declaration ----------------
rep('@EventBusSubscriber(modid=TConstruct.MOD_ID, value=Dist.CLIENT, bus=Bus.MOD)\n', '')

# ---------------- reload listeners ----------------
rep('''  @SubscribeEvent
  static void addResourceListener(RegisterClientReloadListenersEvent event) {
    for (FoliageType type : FoliageType.values()) {
      event.registerReloadListener(new SlimeColorReloadListener(type));
    }
  }''',
'''  static void addResourceListener() {
    for (FoliageType type : FoliageType.values()) {
      ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SlimeColorReloadListener(type));
    }
  }''')

# ---------------- particles ----------------
rep('''  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    event.registerSpecial(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
    event.registerSpecial(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
    event.registerSpecial(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
  }''',
'''  static void registerParticleFactories() {
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.skySlimeParticle.get(), new SlimeParticle.Factory(SlimeType.SKY));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.enderSlimeParticle.get(), new SlimeParticle.Factory(SlimeType.ENDER));
    ParticleFactoryRegistry.getInstance().register(TinkerWorld.terracubeParticle.get(), new SlimeParticle.Factory(Items.CLAY_BALL));
  }''')

# ---------------- model layers ----------------
rep('''  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterLayerDefinitions event) {''',
'''  static void registerRenderers() {''')
src = src.replace('registerLayerDefinition(event, ', 'registerLayerDefinition(')
rep('    event.registerLayerDefinition(SkullModelHelper.FLUID_CANNON, headOverlayCustom);',
    '    EntityModelLayerRegistry.registerModelLayer(SkullModelHelper.FLUID_CANNON, headOverlayCustom::get);')

# ---------------- skull models ----------------
rep('''  @SubscribeEvent
  static void registerSkullModels(EntityRenderersEvent.CreateSkullModels event) {
    EntityModelSet modelSet = event.getEntityModelSet();
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> {
      if (type.isPiglin()) {
        event.registerSkullModel(type, new PiglinHeadModel(modelSet.bakeLayer(layer)));
      } else {
        event.registerSkullModel(type, new SkullModel(modelSet.bakeLayer(layer)));
      }
    });
  }''',
'''  static void registerSkullModels(ImmutableMap.Builder<SkullBlock.Type, SkullModelBase> builder, EntityModelSet modelSet) {
    SkullModelHelper.HEAD_LAYERS.forEach((type, layer) -> {
      if (type.isPiglin()) {
        builder.put(type, new PiglinHeadModel(modelSet.bakeLayer(layer)));
      } else {
        builder.put(type, new SkullModel(modelSet.bakeLayer(layer)));
      }
    });
  }''')

# ---------------- entity renderers ----------------
rep('''  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
    event.registerEntityRenderer(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
    event.registerEntityRenderer(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
  }''',
'''  static void registerEntityRenderers() {
    EntityRendererRegistry.register(TinkerWorld.skySlimeEntity.get(), TinkerSlimeRenderer.SKY_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.enderSlimeEntity.get(), TinkerSlimeRenderer.ENDER_SLIME_FACTORY);
    EntityRendererRegistry.register(TinkerWorld.terracubeEntity.get(), TerracubeRenderer::new);
  }''')

# ---------------- client setup ----------------
rep('''  @SuppressWarnings("removal")
  @SubscribeEvent
  static void clientSetup(FMLClientSetupEvent event) {
    // skull textures
    event.enqueueWork(() -> {''',
'''  public static void clientSetup() {
    // TODO(fabric): called from TConstruct's client initializer; Fabric has no client mod loading event bus
    // skull textures
    {''')

# ---------------- block colors ----------------
rep('''  @SubscribeEvent
  static void registerBlockColorHandlers(RegisterColorHandlersEvent.Block event) {''',
'''  static void registerBlockColorHandlers() {''')
src = src.replace('      event.register(\n', '      ColorProviderRegistry.BLOCK.register(\n')
src = src.replace('    event.register(\n', '    ColorProviderRegistry.BLOCK.register(\n')

# ---------------- item colors ----------------
rep('''  @SubscribeEvent
  static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
    BlockColors blockColors = event.getBlockColors();
    ItemColors itemColors = event.getItemColors();
    // slime grass items''',
'''  static void registerItemColorHandlers() {
    // TODO(fabric): Forge handed the client color managers to the event; Fabric looks them up per block, so only the
    //  block based alias overloads are available
    // slime grass items''')
src = re.sub(r'registerBlockItemColorAlias\(blockColors, itemColors, (TinkerWorld\.\w+)\);',
             r'registerBlockItemColorAlias(\1);', src)
src = src.replace('registerBlockItemColorAlias(TinkerWorld.skySlimeVine);', 'registerBlockItemColorAlias(TinkerWorld.skySlimeVine.get());')
src = src.replace('registerBlockItemColorAlias(TinkerWorld.enderSlimeVine);', 'registerBlockItemColorAlias(TinkerWorld.enderSlimeVine.get());')

# ---------------- layer definition helper ----------------
rep('''  /** Register a head layer definition with forge */
  private static void registerLayerDefinition(EntityRenderersEvent.RegisterLayerDefinitions event, TinkerHeadType head, Supplier<LayerDefinition> supplier) {
    event.registerLayerDefinition(SkullModelHelper.HEAD_LAYERS.get(head), supplier);
  }''',
'''  /** Registers a head layer definition with Fabric */
  private static void registerLayerDefinition(TinkerHeadType head, Supplier<LayerDefinition> supplier) {
    EntityModelLayerRegistry.registerModelLayer(SkullModelHelper.HEAD_LAYERS.get(head), supplier::get);
  }''')

io.open(path, 'w', encoding='utf-8', newline='\n').write(src)
print('forge refs left:', src.count('minecraftforge'))
print('lines:', len(src.splitlines()))
