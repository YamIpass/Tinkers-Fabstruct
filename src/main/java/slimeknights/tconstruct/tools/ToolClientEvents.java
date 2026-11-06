package slimeknights.tconstruct.tools;

import java.util.Map;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.fabricators_of_create.porting_lib.entity.events.PlayerTickEvents;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.RegisterGeometryLoadersCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.player.Input;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.mantle.client.SafeClientAccess;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.ClientEventBase;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.client.armor.AbstractArmorModel;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager;
import slimeknights.tconstruct.library.client.armor.texture.TrimArmorTextureSupplier;
import slimeknights.tconstruct.library.client.book.content.AbstractMaterialContent;
import slimeknights.tconstruct.library.client.materials.MaterialTooltipCache;
import slimeknights.tconstruct.library.client.model.DynamicTextureLoader;
import slimeknights.tconstruct.library.client.model.TinkerItemProperties;
import slimeknights.tconstruct.library.client.model.tools.MaterialBlockModel;
import slimeknights.tconstruct.library.client.model.tools.MaterialModel;
import slimeknights.tconstruct.library.client.model.tools.ToolModel;
import slimeknights.tconstruct.library.client.modifiers.DyedModifierModel;
import slimeknights.tconstruct.library.client.modifiers.FluidModifierModel;
import slimeknights.tconstruct.library.client.modifiers.MaterialModifierModel;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager.ModifierModelRegistrationEvent;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelManager;
import slimeknights.tconstruct.library.client.modifiers.ModifierModelMapManager;
import slimeknights.tconstruct.library.client.modifiers.NormalModifierModel;
import slimeknights.tconstruct.library.client.modifiers.PotionModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TankModifierModel;
import slimeknights.tconstruct.library.client.modifiers.TrimModifierModel;
import slimeknights.tconstruct.library.client.particle.AttackParticle;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorStatModule;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.HarvestTiers;
import slimeknights.tconstruct.library.utils.Util;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.tools.client.CrystalshotRenderer;
import slimeknights.tconstruct.tools.client.FluidEffectProjectileRenderer;
import slimeknights.tconstruct.tools.client.OverslimeModifierModel;
import slimeknights.tconstruct.tools.client.ShieldBannerModifierSpriteSource;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;
import slimeknights.tconstruct.tools.client.ToolContainerScreen;
import slimeknights.tconstruct.tools.client.material.CombatFishingHookRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownShurikenRenderer;
import slimeknights.tconstruct.tools.client.material.ThrownToolRenderer;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;
import slimeknights.tconstruct.tools.logic.DoubleJumpHandler;
import slimeknights.tconstruct.tools.logic.InteractionHandler;
import slimeknights.tconstruct.tools.modules.ranged.ammo.SmashingModule;
import slimeknights.tconstruct.tools.network.TinkerControlPacket;
import static slimeknights.tconstruct.TConstruct.getResource;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ToolClientEvents extends ClientEventBase {
  /** Keybinding for interacting using a helmet */
  private static final KeyMapping HELMET_INTERACT = new KeyMapping(TConstruct.makeTranslationKey("key", "helmet_interact"), InputConstants.getKey("key.keyboard.z").getValue(), "key.categories.tconstruct");
  /** Keybinding for interacting using leggings */
  private static final KeyMapping LEGGINGS_INTERACT = new KeyMapping(TConstruct.makeTranslationKey("key", "leggings_interact"), InputConstants.getKey("key.keyboard.i").getValue(), "key.categories.tconstruct");

  /** Listener to clear modifier cache */
  private static final ResourceLocation MODIFIER_RELOAD_ID = TConstruct.getResource("modifier_reload_listener");
  /** Clears the modifier model cache on resource reload */
  private static final IEarlySafeManagerReloadListener MODIFIER_RELOAD_LISTENER = new IEarlySafeManagerReloadListener() {
    @Override
    public ResourceLocation getFabricId() {
      return MODIFIER_RELOAD_ID;
    }

    @Override
    public void onReloadSafe(ResourceManager manager) {
      ModifierManager.INSTANCE.getAllValues().forEach(modifier -> modifier.clearCache(PackType.CLIENT_RESOURCES));
    }
  };

  static void addResourceListener() {
    ResourceManagerHelper manager = ResourceManagerHelper.get(PackType.CLIENT_RESOURCES);
    ModifierModelManager.init(manager);
    // TODO(fabric): ModifierModelMapManager does not implement IdentifiableResourceReloadListener yet (library-client batch)
    MaterialTooltipCache.init(manager);
    DynamicTextureLoader.init(manager);
    manager.registerReloadListener(MODIFIER_RELOAD_LISTENER);
    manager.registerReloadListener(SlimeskullArmorModel.RELOAD_LISTENER);
    manager.registerReloadListener(HarvestTiers.RELOAD_LISTENER);
    ArmorModelManager.init(manager);
    // TODO(fabric): TrimArmorTextureSupplier.CACHE_INVALIDATOR is a plain ResourceManagerReloadListener, it needs an identifiable wrapper (library-client batch)
    ShieldBannerModifierSpriteSource.register();
  }

  static void registerModelLoaders(Map<ResourceLocation, IGeometryLoader<?>> loaders) {
    loaders.put(TConstruct.getResource("material"), MaterialModel.LOADER);
    loaders.put(TConstruct.getResource("tool"), ToolModel.LOADER);
    loaders.put(TConstruct.getResource("material_block"), MaterialBlockModel.LOADER);
  }

  static void registerModifierModels(ModifierModelRegistrationEvent event) {
    event.registerModel(getResource("normal"), NormalModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("overslime"), OverslimeModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("fluid"), FluidModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("tank"), TankModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("material"), MaterialModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("dyed"), DyedModifierModel.UNBAKED_INSTANCE);
    // trim shows up as valid on every tool, skip to reduce memory overhead on tools using the new system - add it using the new system if you want it
    event.registerModel(getResource("trim"), TrimModifierModel.UNBAKED_INSTANCE);
    ModifierModelMapManager.legacyBlacklist(TrimModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("potion"), PotionModifierModel.UNBAKED_INSTANCE);
    event.registerModel(getResource("smashing_fluid"), new FluidModifierModel.Unbaked(SmashingModule.TANK_HELPER));
  }

  static void registerRenderers() {
    EntityRendererRegistry.register(TinkerTools.indestructibleItem.get(), ItemEntityRenderer::new);
    EntityRendererRegistry.register(TinkerTools.crystalshotEntity.get(), CrystalshotRenderer::new);
    EntityRendererRegistry.register(TinkerTools.fishingHook.get(), CombatFishingHookRenderer::new);
    // TODO: config option for vanilla style renderer?
    EntityRendererRegistry.register(TinkerTools.materialArrow.get(), ThrownToolRenderer::new);
    EntityRendererRegistry.register(TinkerTools.thrownShuriken.get(), ThrownShurikenRenderer::new);
    EntityRendererRegistry.register(TinkerTools.thrownTool.get(), ThrownToolRenderer::new);
    EntityRendererRegistry.register(TinkerModifiers.fluidSpitEntity.get(), FluidEffectProjectileRenderer::new);
    EntityRendererRegistry.register(TinkerModifiers.fireball.get(), context -> new ThrownItemRenderer<>(context, 0.75f, true));
  }

  public static void clientSetupEvent() {
    PlayerTickEvents.START.register(ToolClientEvents::handleKeyBindings);
    // TODO(fabric): MovementInputUpdateCallback lives in Porting Lib's separate client_events module, which is missing
    //  from libs/; add io.github.fabricators_of_create.Porting-Lib:client_events to the build and register
    //  ToolClientEvents::handleInput there. Without it, use-item movement speed is not applied.
    AbstractArmorModel.init();

    // keybinds
    KeyBindingHelper.registerKeyBinding(HELMET_INTERACT);
    KeyBindingHelper.registerKeyBinding(LEGGINGS_INTERACT);
    // fake ingot showing in the book is a little nicer than the repair kits
    AbstractMaterialContent.registerFallbackPart(TinkerToolParts.fakeIngot);
    AbstractMaterialContent.registerFallbackPart(TinkerToolParts.fakeStorageBlockItem);
    // screens
    MenuScreens.register(TinkerTools.toolContainer.get(), ToolContainerScreen::new);

    // properties
    // stone
    TinkerItemProperties.registerToolProperties(TinkerTools.pickaxe);
    TinkerItemProperties.registerToolProperties(TinkerTools.sledgeHammer);
    TinkerItemProperties.registerToolProperties(TinkerTools.veinHammer);
    // dirt
    TinkerItemProperties.registerToolProperties(TinkerTools.mattock);
    TinkerItemProperties.registerToolProperties(TinkerTools.pickadze);
    TinkerItemProperties.registerToolProperties(TinkerTools.excavator);
    // axe
    TinkerItemProperties.registerToolProperties(TinkerTools.handAxe);
    TinkerItemProperties.registerToolProperties(TinkerTools.broadAxe);
    // leaves
    TinkerItemProperties.registerToolProperties(TinkerTools.kama);
    TinkerItemProperties.registerToolProperties(TinkerTools.scythe);
    // sword
    TinkerItemProperties.registerToolProperties(TinkerTools.dagger);
    TinkerItemProperties.registerToolProperties(TinkerTools.sword);
    TinkerItemProperties.registerToolProperties(TinkerTools.cleaver);
    // bow
    TinkerItemProperties.registerCrossbowProperties(TinkerTools.crossbow);
    TinkerItemProperties.registerToolProperties(TinkerTools.longbow);
    TinkerItemProperties.registerToolProperties(TinkerTools.fishingRod);
    TinkerItemProperties.registerToolProperties(TinkerTools.javelin);
    // misc
    TinkerItemProperties.registerToolProperties(TinkerTools.flintAndBrick);
    TinkerItemProperties.registerToolProperties(TinkerTools.skyStaff);
    TinkerItemProperties.registerToolProperties(TinkerTools.earthStaff);
    TinkerItemProperties.registerToolProperties(TinkerTools.ichorStaff);
    TinkerItemProperties.registerToolProperties(TinkerTools.enderStaff);
    // ancient
    TinkerItemProperties.registerToolProperties(TinkerTools.meltingPan);
    TinkerItemProperties.registerCrossbowProperties(TinkerTools.warPick);
    TinkerItemProperties.registerToolProperties(TinkerTools.battlesign);
    TinkerItemProperties.registerToolProperties(TinkerTools.swasher);
    if (FabricLoader.getInstance().isModLoaded("twilightforest")) {
      TinkerItemProperties.registerToolProperties(TinkerTools.minotaurAxe);
    }
    // armor
    TinkerItemProperties.registerToolProperties(TinkerTools.travelersShield);
    TinkerItemProperties.registerToolProperties(TinkerTools.plateShield);
    Consumer<Item> brokenConsumer = TinkerItemProperties::registerBrokenProperty;
    TinkerTools.travelersGear.forEach(brokenConsumer);
    TinkerTools.plateArmor.forEach(brokenConsumer);
    TinkerTools.slimesuit.forEach(brokenConsumer);
    TinkerItemProperties.registerToolProperties(TinkerTools.slimeWings);
    registerRenderers();
    registerParticleFactories();
    itemColors();
    addResourceListener();
    // the model loader registry is populated right before the modifier models reload, so a callback is enough here
    ModifierModelRegistrationEvent.EVENT.register(ToolClientEvents::registerModifierModels);
    RegisterGeometryLoadersCallback.EVENT.register(ToolClientEvents::registerModelLoaders);
  }

  static void registerParticleFactories() {
    ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
    registry.register(TinkerTools.hammerAttackParticle.get(), AttackParticle.Factory::new);
    registry.register(TinkerTools.axeAttackParticle.get(), AttackParticle.Factory::new);
    registry.register(TinkerTools.bonkAttackParticle.get(), AttackParticle.Factory::new);
  }

  static void itemColors() {

    // tint modifiers
    // rock
    registerItemColors(TinkerTools.pickaxe);
    registerItemColors(TinkerTools.sledgeHammer);
    registerItemColors(TinkerTools.veinHammer);
    // dirt
    registerItemColors(TinkerTools.mattock);
    registerItemColors(TinkerTools.pickadze);
    registerItemColors(TinkerTools.excavator);
    // wood
    registerItemColors(TinkerTools.handAxe);
    registerItemColors(TinkerTools.broadAxe);
    // scythe
    registerItemColors(TinkerTools.kama);
    registerItemColors(TinkerTools.scythe);
    // weapon
    registerItemColors(TinkerTools.dagger);
    registerItemColors(TinkerTools.sword);
    registerItemColors(TinkerTools.cleaver);
    // bow
    registerItemColors(TinkerTools.crossbow);
    registerItemColors(TinkerTools.longbow);
    registerItemColors(TinkerTools.fishingRod);
    registerItemColors(TinkerTools.javelin);
    registerItemColors(TinkerTools.arrow);
    registerItemColors(TinkerTools.shuriken);
    registerItemColors(TinkerTools.throwingAxe);
    // ancient
    registerItemColors(TinkerTools.meltingPan);
    registerItemColors(TinkerTools.warPick);
    registerItemColors(TinkerTools.battlesign);
    registerItemColors(TinkerTools.swasher);
    if (FabricLoader.getInstance().isModLoaded("twilightforest")) {
      registerItemColors(TinkerTools.minotaurAxe);
    }
    // armor
    registerItemColors(TinkerTools.travelersShield);
    registerItemColors(TinkerTools.plateShield);
    Consumer<Item> brokenConsumer = item -> ColorProviderRegistry.ITEM.register(ToolModel.COLOR_HANDLER, item);
    TinkerTools.travelersGear.forEach(brokenConsumer);
    TinkerTools.plateArmor.forEach(brokenConsumer);
    TinkerTools.slimesuit.forEach(brokenConsumer);
    registerItemColors(TinkerTools.slimeWings);

    // modifier crystal
    ColorProviderRegistry.ITEM.register((stack, index) -> {
      ModifierId modifier = ModifierCrystalItem.getModifier(stack);
      if (modifier != null) {
        return ResourceColorManager.getColor(Util.makeTranslationKey("modifier", modifier));
      }
      return -1;
    }, TinkerModifiers.modifierCrystal.get());
  }

  /** Registers the item color handler for the given modifiable item */
  private static void registerItemColors(Supplier<? extends IModifiable> item) {
    ColorProviderRegistry.ITEM.register(ToolModel.COLOR_HANDLER, item.get());
  }

  // values to check if a key was being pressed last tick, safe as a static value as we only care about a single player client side
  /** If true, we were jumping last tick */
  private static boolean wasJumping = false;
  /** If true, we were interacting with helmet last tick */
  private static boolean wasHelmetInteracting = false;
  /** If true, we were interacting with leggings last tick */
  private static boolean wasLeggingsInteracting = false;

  /** Called on player tick to handle keybinding presses */
  private static void handleKeyBindings(Player player) {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.player != null && minecraft.player == player && !minecraft.player.isSpectator()) {

      // jumping in mid air for double jump
      // ensure we pressed the key since the last tick, holding should not use all your jumps at once
      boolean isJumping = minecraft.options.keyJump.isDown();
      if (!wasJumping && isJumping) {
        if (TinkerEffects.antigravity.get().antigravityJump(player)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.ANTIGRAVITY_JUMP);
        }
        else if (DoubleJumpHandler.extraJump(player)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.DOUBLE_JUMP);
        }
      }
      wasJumping = isJumping;

      // helmet interaction
      boolean isHelmetInteracting = HELMET_INTERACT.isDown();
      if (!wasHelmetInteracting && isHelmetInteracting) {
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (InteractionHandler.startArmorInteract(player, EquipmentSlot.HEAD, key)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartHelmetInteract(key));
        }
      }
      if (wasHelmetInteracting && !isHelmetInteracting) {
        if (InteractionHandler.stopArmorInteract(player, EquipmentSlot.HEAD)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_HELMET_INTERACT);
        }
      }

      // leggings interaction
      boolean isLeggingsInteract = LEGGINGS_INTERACT.isDown();
      if (!wasLeggingsInteracting && isLeggingsInteract) {
        TooltipKey key = SafeClientAccess.getTooltipKey();
        if (InteractionHandler.startArmorInteract(player, EquipmentSlot.LEGS, key)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.getStartLeggingsInteract(key));
        }
      }
      if (wasLeggingsInteracting && !isLeggingsInteract) {
        if (InteractionHandler.stopArmorInteract(player, EquipmentSlot.LEGS)) {
          TinkerNetwork.getInstance().sendToServer(TinkerControlPacket.STOP_LEGGINGS_INTERACT);
        }
      }

      wasHelmetInteracting = isHelmetInteracting;
      wasLeggingsInteracting = isLeggingsInteract;
    }
  }

  private static void handleInput(Player player, Input input) {
    if (player.isUsingItem() && !player.isPassenger()) {
      ItemStack using = player.getUseItem();
      // start with the attribute
      double speed = TinkerAttributes.getValue(player, TinkerAttributes.USE_ITEM_SPEED);
      // start by calculating tool stat, not an attribute to ensure both hands get their say
      if (using.is(TinkerTags.Items.HELD)) {
        ToolStack tool = ToolStack.from(using);
        speed += tool.getStats().get(ToolStats.USE_ITEM_SPEED) - ToolStats.USE_ITEM_SPEED.getDefaultValue();
      }
      // next, add in deprecated key bonus
      speed = Mth.clamp(speed + ArmorStatModule.getStat(player, TinkerDataKeys.USE_ITEM_SPEED), 0, 1);
      // update speed, note if the armor stat is 0 and the held tool is not tinkers this is a no-op effectively
      // multiply by 5 to cancel out the vanilla 20%
      input.leftImpulse *= (float) (speed * 5);
      input.forwardImpulse *= (float) (speed * 5);
    }
  }
}
