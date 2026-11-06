package slimeknights.tconstruct.shared;

import io.github.fabricators_of_create.porting_lib.util.RegistryObject;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.registration.deferred.AttributeDeferredRegister;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.config.Config;

public class TinkerAttributes {
  private static final AttributeDeferredRegister ATTRIBUTES = new AttributeDeferredRegister(TConstruct.MOD_ID);

  public TinkerAttributes() {
    // TODO(fabric): Forge registered this deferred register on the mod event bus, Mantle's Fabric version flushes directly
    ATTRIBUTES.register();
    addAttributes();
    commonSetup();
  }

  // booleans
  /** If true, the entity will bounce. Used to implement slime boots */
  public static final RegistryObject<Attribute> BOUNCY = ATTRIBUTES.registerPercent("generic.bouncy", 0f, true);

  // stats
  /** Changes the speed debuff percentage when the player moves while using an item */
  public static final RegistryObject<Attribute> USE_ITEM_SPEED = ATTRIBUTES.registerPercent("player.use_item_speed", 0.2f, true);
  /** Changes the speed debuff when the player moves while using an item */
  public static final RegistryObject<Attribute> PROTECTION_CAP = ATTRIBUTES.register("generic.protection_cap", 0.8, 0, 0.95f, true);
  /** Percentage boost to critical hits for any airborne attacker, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#dragonborn} */
  public static final RegistryObject<Attribute> CRITICAL_DAMAGE = ATTRIBUTES.register("player.critical_damage", 1.5f, 0, 100, false);
  /** Loot bonus for {@link slimeknights.tconstruct.tools.data.ModifierIds#chrysophilite} */
  public static final RegistryObject<Attribute> CHRYSOPHILITE = ATTRIBUTES.register("generic.chrysophilite", 0, 0, 100, false);

  // stat bonuses
  /** Bonus jump height in blocks */
  public static final RegistryObject<Attribute> JUMP_BOOST = ATTRIBUTES.register("generic.jump_boost", 0, 0, 100, true);
  /** Distance you can safely fall without damage */
  public static final RegistryObject<Attribute> SAFE_FALL_DISTANCE = ATTRIBUTES.register("generic.safe_fall_distance", 0, -10, 100, true);
  /** Number of jumps the player may perform, used by the double jump modifier. */
  public static final RegistryObject<Attribute> JUMP_COUNT = ATTRIBUTES.register("player.jump_count", 1, 1, 100, true);

  // stat multipliers
  /** Multiplier for knockback this entity takes. Similar to {@link net.minecraft.world.entity.ai.attributes.Attributes#KNOCKBACK_RESISTANCE} but can be used to increase knockback */
  public static final RegistryObject<Attribute> KNOCKBACK_MULTIPLIER = ATTRIBUTES.registerMultiplier("generic.knockback_multiplier", true);
  /** Player modifier data key for mining speed multiplier as an additive percentage boost on mining speed. Used for armor haste. */
  public static final RegistryObject<Attribute> MINING_SPEED_MULTIPLIER = ATTRIBUTES.registerMultiplier("player.mining_speed_multiplier", true);
  /** Attribute for experience from all sources */
  public static final RegistryObject<Attribute> EXPERIENCE_MULTIPLIER = ATTRIBUTES.registerMultiplier("player.experience_multiplier", false);
  /** Percentage boost to damage while crouching, used by {@link slimeknights.tconstruct.tools.data.ModifierIds#shulking} */
  public static final RegistryObject<Attribute> CROUCH_DAMAGE_MULTIPLIER = ATTRIBUTES.registerMultiplier("generic.crouch_damage_multiplier", false);
  // effect durations
  /** Percentage boost to positive potion effects */
  public static final RegistryObject<Attribute> GOOD_EFFECT_DURATION = ATTRIBUTES.registerMultiplier("generic.good_effect_duration_multiplier", false);
  /** Percentage boost to negative potion effects, used for {@link slimeknights.tconstruct.tools.data.ModifierIds#magicProtection} */
  public static final RegistryObject<Attribute> BAD_EFFECT_DURATION = ATTRIBUTES.registerMultiplier("generic.bad_effect_duration_multiplier", false);

  /**
   * Gets the value of one of our attributes, falling back to the attribute default when the entity does not have it.
   * <p>Forge added these attributes to every living entity; Fabric has no event for that, so only the player supplier
   * gets them. The default is what a mob effectively has on Forge, so the fallback keeps behavior identical.</p>
   */
  public static double getValue(LivingEntity entity, RegistryObject<Attribute> attribute) {
    AttributeInstance instance = entity.getAttribute(attribute.get());
    return instance == null ? attribute.get().getDefaultValue() : instance.getValue();
  }



  /**
   * Adds the attributes to their entity types
   * TODO(fabric): Forge's EntityAttributeModificationEvent added the attributes to every living entity. Fabric has no
   *  equivalent, so the player defaults are rebuilt with the vanilla values plus ours; the other entity types keep the
   *  vanilla defaults until a mixin can append to their attribute suppliers.
   */
  private static void addAttributes() {
    // player attributes
    AttributeSupplier.Builder player = Player.createAttributes();
    player.add(USE_ITEM_SPEED.get(), USE_ITEM_SPEED.get().getDefaultValue());
    player.add(CRITICAL_DAMAGE.get(), CRITICAL_DAMAGE.get().getDefaultValue());
    player.add(MINING_SPEED_MULTIPLIER.get(), MINING_SPEED_MULTIPLIER.get().getDefaultValue());
    player.add(EXPERIENCE_MULTIPLIER.get(), EXPERIENCE_MULTIPLIER.get().getDefaultValue());
    player.add(JUMP_COUNT.get(), JUMP_COUNT.get().getDefaultValue());
    // general attributes, see the TODO above for why these only cover the player
    addToPlayer(player, BOUNCY);
    addToPlayer(player, PROTECTION_CAP);
    addToPlayer(player, CHRYSOPHILITE);
    addToPlayer(player, JUMP_BOOST);
    addToPlayer(player, SAFE_FALL_DISTANCE);
    addToPlayer(player, CROUCH_DAMAGE_MULTIPLIER);
    addToPlayer(player, KNOCKBACK_MULTIPLIER);
    addToPlayer(player, GOOD_EFFECT_DURATION);
    addToPlayer(player, BAD_EFFECT_DURATION);
    FabricDefaultAttributeRegistry.register(EntityType.PLAYER, player);
  }

  /** Runs common setup; called from the constructor as the Fabric entrypoint does not dispatch Forge lifecycle events */
  private static void commonSetup() {
    // make knockback resistance syncable, as we need that info clientside
    if (Config.COMMON.syncKnockbackResistance.get()) {
      //noinspection ResultOfMethodCallIgnored
      Attributes.KNOCKBACK_RESISTANCE.setSyncable(true);
    }
  }

  /** Adds an attribute to the player defaults */
  private static void addToPlayer(AttributeSupplier.Builder builder, RegistryObject<Attribute> attribute) {
    builder.add(attribute.get(), attribute.get().getDefaultValue());
  }
}
