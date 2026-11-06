import sys
POL = 'src/main/java/slimeknights/tconstruct/tools/ToolClientEvents.java'
NL = chr(10)
raw = open(POL, 'rb').read().decode('utf-8')
t = raw.replace(chr(13) + NL, NL)

def rep(old, new, count=1):
    global t
    assert t.count(old) == count, 'match ' + str(t.count(old)) + ' != ' + str(count) + ' for: ' + old[:70]
    t = t.replace(old, new, count)


# ---------------------------------------------------------------- imports
lines = t.split(NL)
out = [l for l in lines if not l.startswith('import net.minecraftforge.')]
out = [l for l in out if l != 'import static slimeknights.tconstruct.library.client.model.tools.ToolModel.registerItemColors;']
lines = out
start = next(i for i, l in enumerate(lines) if l.startswith('import '))
end = max(i for i, l in enumerate(lines) if l.startswith('import '))
block = [l for l in lines[start:end + 1] if l.startswith('import ')]
extra = [
    'import io.github.fabricators_of_create.porting_lib.client_events.event.client.MovementInputUpdateCallback',
    'import io.github.fabricators_of_create.porting_lib.entity.events.PlayerTickEvents',
    'import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader',
    'import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback',
    'import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper',
    'import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry',
    'import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry',
    'import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry',
    'import net.fabricmc.fabric.api.resource.ResourceManagerHelper',
    'import net.minecraft.resources.ResourceLocation',
    'import net.minecraft.server.packs.resources.ResourceManager',
    'import slimeknights.mantle.data.fabric.IdentifiableISafeManagerReloadListener',
]
have = set(l[7:-1] for l in block)
for e in extra:
    if e[7:] not in have:
        block.append(e + ';')
        have.add(e[7:])
block = [l for l in block if l.startswith('import ')]
non_java = sorted([l for l in block if not l.startswith('import java.')])
java = sorted([l for l in block if l.startswith('import java.')])
lines = lines[:start] + non_java + java + lines[end + 1:]
t = NL.join(lines)

# --------------------------------------------------------------- class head
rep('@SuppressWarnings("unused")' + NL + '@EventBusSubscriber(modid = TConstruct.MOD_ID, value = Dist.CLIENT, bus = Bus.MOD)' + NL + 'public class ToolClientEvents',
    '@SuppressWarnings("unused")' + NL + 'public class ToolClientEvents')

# ---------------------------------------------------------------- key binds
rep('new KeyMapping(TConstruct.makeTranslationKey("key", "helmet_interact"), KeyConflictContext.IN_GAME, InputConstants.getKey("key.keyboard.z"), "key.categories.tconstruct")',
    'new KeyMapping(TConstruct.makeTranslationKey("key", "helmet_interact"), InputConstants.getKey("key.keyboard.z").getValue(), "key.categories.tconstruct")')
rep('new KeyMapping(TConstruct.makeTranslationKey("key", "leggings_interact"), KeyConflictContext.IN_GAME, InputConstants.getKey("key.keyboard.i"), "key.categories.tconstruct")',
    'new KeyMapping(TConstruct.makeTranslationKey("key", "leggings_interact"), InputConstants.getKey("key.keyboard.i").getValue(), "key.categories.tconstruct")')

# ---------------------------------------------------------- reload listener
rep('''  private static final ISafeManagerReloadListener MODIFIER_RELOAD_LISTENER = manager -> {
    ModifierManager.INSTANCE.getAllValues().forEach(modifier -> modifier.clearCache(PackType.CLIENT_RESOURCES));
  };''',
'''  private static final IdentifiableISafeManagerReloadListener MODIFIER_RELOAD_LISTENER = new IdentifiableISafeManagerReloadListener(TConstruct.getResource("modifier_reload_listener")) {
    @Override
    public void onReloadSafe(ResourceManager manager) {
      ModifierManager.INSTANCE.getAllValues().forEach(modifier -> modifier.clearCache(PackType.CLIENT_RESOURCES));
    }
  };''')

# ------------------------------------------------------- resource listeners
rep('''  @SuppressWarnings("deprecation")
  @SubscribeEvent
  static void addResourceListener(RegisterClientReloadListenersEvent manager) {
    ModifierModelManager.init(manager);
    manager.registerReloadListener(ModifierModelMapManager.INSTANCE);
    MaterialTooltipCache.init(manager);
    DynamicTextureLoader.init(manager);
    manager.registerReloadListener(MODIFIER_RELOAD_LISTENER);
    manager.registerReloadListener(SlimeskullArmorModel.RELOAD_LISTENER);
    manager.registerReloadListener(HarvestTiers.RELOAD_LISTENER);
    ArmorModelManager.init(manager);
    manager.registerReloadListener(TrimArmorTextureSupplier.CACHE_INVALIDATOR);
    ShieldBannerModifierSpriteSource.register();
  }''',
'''  static void addResourceListener() {
    ResourceManagerHelper manager = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
    ModifierModelManager.init(manager);
    manager.registerReloadListener(ModifierModelMapManager.INSTANCE);
    MaterialTooltipCache.init(manager);
    DynamicTextureLoader.init(manager);
    manager.registerReloadListener(MODIFIER_RELOAD_LISTENER);
    manager.registerReloadListener(SlimeskullArmorModel.RELOAD_LISTENER);
    manager.registerReloadListener(HarvestTiers.RELOAD_LISTENER);
    ArmorModelManager.init(manager);
    manager.registerReloadListener(TrimArmorTextureSupplier.CACHE_INVALIDATOR);
    ShieldBannerModifierSpriteSource.register();
  }''')

# ----------------------------------------------------------- model loaders
rep('''  @SubscribeEvent
  static void registerModelLoaders(RegisterGeometryLoaders event) {
    event.register("material", MaterialModel.LOADER);
    event.register("tool", ToolModel.LOADER);
    event.register("material_block", MaterialBlockModel.LOADER);
  }''',
'''  static void registerModelLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
    loaders.put(TConstruct.getResource("material"), MaterialModel.LOADER);
    loaders.put(TConstruct.getResource("tool"), ToolModel.LOADER);
    loaders.put(TConstruct.getResource("material_block"), MaterialBlockModel.LOADER);
  }''')

# ------------------------------------------------------- modifier models
rep('  @SubscribeEvent' + NL + '  static void registerModifierModels(ModifierModelRegistrationEvent event) {',
    '  static void registerModifierModels(ModifierModelRegistrationEvent event) {')

# --------------------------------------------------------------- renderers
rep('''  @SubscribeEvent
  static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(TinkerTools.indestructibleItem.get(), ItemEntityRenderer::new);
    event.registerEntityRenderer(TinkerTools.crystalshotEntity.get(), CrystalshotRenderer::new);
    event.registerEntityRenderer(TinkerTools.fishingHook.get(), CombatFishingHookRenderer::new);
    // TODO: config option for vanilla style renderer?
    event.registerEntityRenderer(TinkerTools.materialArrow.get(), ThrownToolRenderer::new);
    event.registerEntityRenderer(TinkerTools.thrownShuriken.get(), ThrownShurikenRenderer::new);
    event.registerEntityRenderer(TinkerTools.thrownTool.get(), ThrownToolRenderer::new);
    event.registerEntityRenderer(TinkerModifiers.fluidSpitEntity.get(), FluidEffectProjectileRenderer::new);
    event.registerEntityRenderer(TinkerModifiers.fireball.get(), context -> new ThrownItemRenderer<>(context, 0.75f, true));
  }''',
'''  static void registerRenderers() {
    EntityRendererRegistry.register(TinkerTools.indestructibleItem.get(), ItemEntityRenderer::new);
    EntityRendererRegistry.register(TinkerTools.crystalshotEntity.get(), CrystalshotRenderer::new);
    EntityRendererRegistry.register(TinkerTools.fishingHook.get(), CombatFishingHookRenderer::new);
    // TODO: config option for vanilla style renderer?
    EntityRendererRegistry.register(TinkerTools.materialArrow.get(), ThrownToolRenderer::new);
    EntityRendererRegistry.register(TinkerTools.thrownShuriken.get(), ThrownShurikenRenderer::new);
    EntityRendererRegistry.register(TinkerTools.thrownTool.get(), ThrownToolRenderer::new);
    EntityRendererRegistry.register(TinkerModifiers.fluidSpitEntity.get(), FluidEffectProjectileRenderer::new);
    EntityRendererRegistry.register(TinkerModifiers.fireball.get(), context -> new ThrownItemRenderer<>(context, 0.75f, true));
  }''')

# ------------------------------------------------------------- key bindings
rep('''  @SubscribeEvent
  static void registerKeyBinding(RegisterKeyMappingsEvent event) {
    event.register(HELMET_INTERACT);
    event.register(LEGGINGS_INTERACT);
  }

''', '')

# ---------------------------------------------------------------- setup hook
rep('''  @SubscribeEvent
  static void clientSetupEvent(FMLClientSetupEvent event) {
    MinecraftForge.EVENT_BUS.addListener(ToolClientEvents::handleKeyBindings);
    MinecraftForge.EVENT_BUS.addListener(ToolClientEvents::handleInput);
    AbstractArmorModel.init();

    // keybinds
    event.enqueueWork(() -> {''',
'''  public static void clientSetupEvent() {
    PlayerTickEvents.START.register(ToolClientEvents::handleKeyBindings);
    MovementInputUpdateCallback.EVENT.register(ToolClientEvents::handleInput);
    AbstractArmorModel.init();

    // keybinds
    KeyBindingHelper.registerKeyBinding(HELMET_INTERACT);
    KeyBindingHelper.registerKeyBinding(LEGGINGS_INTERACT);
    {''')

# ------------------------------------------------ particles (sprite sets)
rep('''  @SubscribeEvent
  static void registerParticleFactories(RegisterParticleProvidersEvent event) {
    ParticleEngine.SpriteParticleRegistration<SimpleParticleType> factory = AttackParticle.Factory::new;
    event.registerSpriteSet(TinkerTools.hammerAttackParticle.get(), factory);
    event.registerSpriteSet(TinkerTools.axeAttackParticle.get(), factory);
    event.registerSpriteSet(TinkerTools.bonkAttackParticle.get(), factory);
  }''',
'''  static void registerParticleFactories() {
    ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
    registry.register(TinkerTools.hammerAttackParticle.get(), AttackParticle.Factory::new);
    registry.register(TinkerTools.axeAttackParticle.get(), AttackParticle.Factory::new);
    registry.register(TinkerTools.bonkAttackParticle.get(), AttackParticle.Factory::new);
  }''')

# ------------------------------------------------------------------ colors
rep('''  @SubscribeEvent
  static void itemColors(RegisterColorHandlersEvent.Item event) {
    final ItemColors colors = event.getItemColors();
''',
'''  static void itemColors() {
''')
rep('registerItemColors(colors, ', 'registerItemColors(', 28)
rep('Consumer<Item> brokenConsumer = item -> event.register(ToolModel.COLOR_HANDLER, item);',
    'Consumer<Item> brokenConsumer = item -> ColorProviderRegistry.ITEM.register(ToolModel.COLOR_HANDLER, item);')
rep('    // modifier crystal' + NL + '    event.register((stack, index) -> {',
    '    // modifier crystal' + NL + '    ColorProviderRegistry.ITEM.register((stack, index) -> {')
rep('    }, TinkerModifiers.modifierCrystal);', '    }, TinkerModifiers.modifierCrystal.get());')

# ----------------------------------------------------------------- helpers
rep('  // values to check if a key was being pressed last tick, safe as a static value as we only care about a single player client side',
'''  /** Registers the item color handler for the given modifiable item */
  private static void registerItemColors(Supplier<? extends IModifiable> item) {
    ColorProviderRegistry.ITEM.register(ToolModel.COLOR_HANDLER, item.get());
  }

  // values to check if a key was being pressed last tick, safe as a static value as we only care about a single player client side''')

# -------------------------------------------------------- keybind handler
rep('''  private static void handleKeyBindings(PlayerTickEvent event) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null && minecraft.player == event.player && event.phase == Phase.START && event.side == LogicalSide.CLIENT && !minecraft.player.isSpectator()) {''',
'''  private static void handleKeyBindings(Player player) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null && minecraft.player == player && !minecraft.player.isSpectator()) {''')
t = t.replace('event.player', 'player')

# ------------------------------------------------------------ input handler
rep('''  @SuppressWarnings("removal")
  private static void handleInput(MovementInputUpdateEvent event) {
    Player player = event.getEntity();''',
'''  private static void handleInput(Player player, Input input) {''')
rep('''      // update speed, note if the armor stat is 0 and the held tool is not tinkers this is a no-op effectively
      Input input = event.getInput();''',
'''      // update speed, note if the armor stat is 0 and the held tool is not tinkers this is a no-op effectively''')

# missing imports for IModifiable / Supplier
lines = t.split(NL)
start = next(i for i, l in enumerate(lines) if l.startswith('import '))
end = max(i for i, l in enumerate(lines) if l.startswith('import '))
block = [l for l in lines[start:end + 1] if l.startswith('import ')]
have = set(l[7:-1] for l in block)
add = ['import java.util.function.Supplier', 'import slimeknights.tconstruct.library.tools.item.IModifiable']
for a in add:
    if a[7:] not in have:
        block.append(a + ';')
non_java = sorted([l for l in block if not l.startswith('import java.')])
java = sorted([l for l in block if l.startswith('import java.')])
lines = lines[:start] + non_java + java + lines[end + 1:]

open(POL, 'wb').write((NL.join(lines)).replace(NL, chr(13) + NL).encode('utf-8'))
print('done')
