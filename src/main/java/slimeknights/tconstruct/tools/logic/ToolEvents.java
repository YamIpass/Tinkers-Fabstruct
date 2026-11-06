package slimeknights.tconstruct.tools.logic;

import com.google.common.collect.Multiset;
import io.github.fabricators_of_create.porting_lib.core.event.BaseEvent.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingTickEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingVisibilityEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent;
import io.github.fabricators_of_create.porting_lib.entity.events.living.LivingDamageEvent;
import io.github.fabricators_of_create.porting_lib.util.EntityHelper;
import net.minecraft.server.level.ServerPlayer;
import slimeknights.mantle.data.predicate.damage.DamageSourcePredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.events.TinkerToolEvent.ToolHarvestEvent;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.ProtectionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.modifiers.hook.ranged.ProjectileHitModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.armor.MobDisguiseModule;
import slimeknights.tconstruct.library.modifiers.modules.technical.ArmorStatModule;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataKeys;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.context.EquipmentIterator.EquipmentEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.module.mining.IsEffectiveToolHook;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolAttackUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.tconstruct.library.utils.BlockSideHitListener;
import slimeknights.tconstruct.shared.TinkerAttributes;
import slimeknights.tconstruct.shared.TinkerEffects;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.network.SyncProjectileModifiersPacket;

import java.util.List;
import java.util.Objects;

/**
 * Event subscriber for tool events
 */
public class ToolEvents {

  public static void init() {
    PlayerEvents.BREAK_SPEED.register(ToolEvents::onBreakSpeed);
    ToolHarvestEvent.EVENT.register(ToolEvents::onHarvest);
    LivingEntityEvents.HURT.register(ToolEvents::livingAttack);
    LivingEntityEvents.HURT.register(ToolEvents::livingHurt);
    LivingDamageEvent.DAMAGE.register(ToolEvents::livingDamage);
    LivingTickEvent.TICK.register(ToolEvents::livingWalk);
    LivingVisibilityEvent.VISIBILITY.register(ToolEvents::livingVisibility);
    EntityEvents.PROJECTILE_IMPACT.register(ToolEvents::projectileHit);
    EntityEvents.START_TRACKING_TAIL.register(ToolEvents::projectileSync);
  }

  static void onBreakSpeed(PlayerEvents.BreakSpeed event) {
    Player player = (Player) event.getEntity();

    // tool break speed hook
    ItemStack stack = player.getMainHandItem();
    if (stack.is(TinkerTags.Items.HARVEST)) {
      ToolStack tool = ToolStack.from(stack);
      if (!tool.isBroken()) {
        List<ModifierEntry> modifiers = tool.getModifierList();
        if (!modifiers.isEmpty()) {
          // build context
          BreakSpeedContext context = new BreakSpeedContext.Event(
            event,
            BlockSideHitListener.getSideHit(player),
            IsEffectiveToolHook.isEffective(tool, event.getState()),
            BreakSpeedContext.getMiningModifier(player)
          );

          // run each modifier hook
          float speed = event.getNewSpeed();
          for (ModifierEntry entry : tool.getModifierList()) {
            speed = entry.getHook(ModifierHooks.BREAK_SPEED).modifyBreakSpeed(tool, entry, context, speed);
            // if any modifier cancels mining, stop right here
            if (speed < 0 || event.isCanceled()) {
              break;
            }
          }
          // update the speed
          event.setNewSpeed(speed);
        }
      }
    }

    // next, add in armor haste
    double armorMultiplier = TinkerAttributes.getValue(player, TinkerAttributes.MINING_SPEED_MULTIPLIER) + ArmorStatModule.getStat(player, TinkerDataKeys.MINING_SPEED);
    if (armorMultiplier >= 0) {
      event.setNewSpeed((float) (event.getNewSpeed() * armorMultiplier));
    }
  }

  static void onHarvest(ToolHarvestEvent event) {
    // prevent processing if already processed
    if (event.getResult() != Result.DEFAULT) {
      return;
    }
    BlockState state = event.getState();
    Block block = state.getBlock();
    Level world = event.getWorld();
    BlockPos pos = event.getPos();

    // carve pumpkins
    if (block == Blocks.PUMPKIN) {
      Direction facing = event.getContext().getClickedFace();
      if (facing.getAxis() == Direction.Axis.Y) {
        facing = event.getContext().getHorizontalDirection().getOpposite();
      }
      // carve block
      world.playSound(null, pos, SoundEvents.PUMPKIN_CARVE, SoundSource.BLOCKS, 1.0F, 1.0F);
      world.setBlock(pos, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(CarvedPumpkinBlock.FACING, facing), 11);
      // spawn seeds
      ItemEntity itemEntity = new ItemEntity(
        world,
        pos.getX() + 0.5D + facing.getStepX() * 0.65D,
        pos.getY() + 0.1D,
        pos.getZ() + 0.5D + facing.getStepZ() * 0.65D,
        new ItemStack(Items.PUMPKIN_SEEDS, 4));
      itemEntity.setDeltaMovement(
        0.05D * facing.getStepX() + world.random.nextDouble() * 0.02D,
        0.05D,
        0.05D * facing.getStepZ() + world.random.nextDouble() * 0.02D);
      world.addFreshEntity(itemEntity);
      event.setResult(Result.ALLOW);
    }

    // hives: get the honey
    if (block instanceof BeehiveBlock beehive) {
      int level = state.getValue(BeehiveBlock.HONEY_LEVEL);
      if (level >= 5) {
        // first, spawn the honey
        world.playSound(null, pos, SoundEvents.BEEHIVE_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
        Block.popResource(world, pos, new ItemStack(Items.HONEYCOMB, 3));

        // if not smoking, make the bees angry
        if (!CampfireBlock.isSmokeyPos(world, pos)) {
          if (beehive.hiveContainsBees(world, pos)) {
            beehive.angerNearbyBees(world, pos);
          }
          beehive.releaseBeesAndResetHoneyLevel(world, state, pos, event.getPlayer(), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        } else {
          beehive.resetHoneyLevel(world, state, pos);
        }
        event.setResult(Result.ALLOW);
      } else {
        event.setResult(Result.DENY);
      }
    }
  }

  static float livingAttack(DamageSource source, LivingEntity entity, float amount) {
    // client side always returns false, so this should be fine?
    if (entity.level().isClientSide() || entity.isDeadOrDying()) {
      return amount;
    }
    // I cannot think of a reason to run when invulnerable
    if (entity.isInvulnerableTo(source)) {
      return amount;
    }

    // a lot of counterattack hooks want to detect direct attacks, so save time by calculating once
    boolean isDirectDamage = OnAttackedModifierHook.isDirectDamage(source);

    // determine if there is any modifiable armor, handles the target wearing modifiable armor
    EquipmentContext context = new EquipmentContext(entity);
    if (context.hasModifiableArmor()) {
      // first we need to determine if any of the four slots want to cancel the event
      for (EquipmentSlot slotType : EquipmentSlot.values()) {
        IToolStackView toolStack = context.getValidTool(slotType);
        if (toolStack != null && !toolStack.isBroken()) {
          for (ModifierEntry entry : toolStack.getModifierList()) {
            if (entry.getHook(ModifierHooks.DAMAGE_BLOCK).isDamageBlocked(toolStack, entry, context, slotType, source, amount)) {
              return 0;
            }
          }
        }
      }

      // then we need to determine if any want to respond assuming its not canceled
      OnAttackedModifierHook.handleAttack(ModifierHooks.ON_ATTACKED, context, source, amount, isDirectDamage);
    }

    // next, consider the attacker is wearing modifiable armor
    Entity attacker = source.getEntity();
    if (attacker instanceof LivingEntity livingAttacker) {
      context = new EquipmentContext(livingAttacker);
      if (context.hasModifiableArmor()) {
        for (EquipmentSlot slotType : ModifiableArmorMaterial.ARMOR_SLOTS) {
          IToolStackView toolStack = context.getToolInSlot(slotType);
          if (toolStack != null && !toolStack.isBroken()) {
            for (ModifierEntry entry : toolStack.getModifierList()) {
              entry.getHook(ModifierHooks.DAMAGE_DEALT).onDamageDealt(toolStack, entry, context, slotType, entity, source, amount, isDirectDamage);
            }
          }
        }
      }
    }
    return amount;
  }

  /**
   * Determines how much to damage armor based on the given damage to the player
   * @param damage  Amount to damage the player
   * @return  Amount to damage the armor
   */
  private static int getArmorDamage(float damage) {
    damage /= 4;
    if (damage < 1) {
      return 1;
    }
    return (int)damage;
  }

  /** Modifier ID used for the extra correction damage on armor. Needs to be a "real modifier" for the tag so we use protection as a reasonable enough source. */
  private static final ModifierId ARMOR_DAMAGE = new ModifierId(TConstruct.MOD_ID, "protection");

  // low priority to minimize conflict as we apply reduction as if we are the final change to damage before vanilla
  static float livingHurt(DamageSource source, LivingEntity entity, float originalDamage) {

    // determine if there is any modifiable armor, if not nothing to do
    EquipmentContext context = new EquipmentContext(entity);
    int vanillaModifier = 0;
    float modifierValue = 0;

    Entity attacker = source.getEntity();
    if (attacker instanceof LivingEntity living) {
      // boost damage based on monster's melee weapon
      if (Config.COMMON.allowMonsterMeleeModifiers.get() && source.is(TinkerTags.DamageTypes.MODIFIER_WHITELIST) && !living.getType().is(TinkerTags.EntityTypes.DAMAGE_MODIFIER_BLACKLIST)) {
        ItemStack weapon = living.getMainHandItem();
        if (!weapon.isEmpty()) {
            if (weapon.is(TinkerTags.Items.MELEE_WEAPON)) {
              IToolStackView tool = ToolStack.from(weapon);
              // already know the player is null
              ToolAttackContext meleeContext = ToolAttackContext.attacker(living, null).target(entity).applyAttributes().build();
              float baseDamage = originalDamage;
              for (ModifierEntry entry : tool.getModifiers()) {
                  originalDamage = entry.getHook(ModifierHooks.MONSTER_MELEE_DAMAGE).getMeleeDamage(tool, entry, meleeContext, baseDamage, originalDamage);
              }
            }
        } else {// unarmed
          // not consider adding non-chest armor to the unarmed tag
          ItemStack unarmed = living.getItemBySlot(EquipmentSlot.CHEST);
          if (!unarmed.isEmpty() && unarmed.is(TinkerTags.Items.UNARMED)) {
            // get the melee damage attribute
            float damageAttr = (float) entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
            IToolStackView tool = ToolStack.from(unarmed);
            // already know the player is null
            ToolAttackContext meleeContext = ToolAttackContext.attacker(living, null).target(entity).toolAttributes(tool).addBaseDamage(originalDamage - damageAttr).slot(EquipmentSlot.CHEST, InteractionHand.MAIN_HAND).build();
            originalDamage = meleeContext.getBaseDamage();
            float baseDamage = originalDamage;
            for (ModifierEntry entry : tool.getModifiers()) {
              originalDamage = entry.getHook(ModifierHooks.MONSTER_MELEE_DAMAGE).getMeleeDamage(tool, entry, meleeContext, baseDamage, originalDamage);
            }
          }
        }
      }

      // run shulking global damage "boost", its a bit hardcoded Java wise to make it softcoded in JSON
      if (attacker.isCrouching()) {
        double crouchMultiplier = TinkerAttributes.getValue(living, TinkerAttributes.CROUCH_DAMAGE_MULTIPLIER);
        crouchMultiplier += ArmorStatModule.getStat(attacker, TinkerDataKeys.CROUCH_DAMAGE);
        if (crouchMultiplier != 0) {
          originalDamage *= crouchMultiplier;
        }
      }
    }

    // conducting - boosts damage from fire
    if (source.is(TinkerTags.DamageTypes.FIRE_PROTECTION)) {
      int level = TinkerEffect.getLevel(entity, TinkerEffects.conductive);
      if (level > 0) {
        originalDamage *= Math.pow(2, level);
      }
    }
    // venom - boosts damage from magic
    if (source.is(TinkerTags.DamageTypes.MAGIC_PROTECTION)) {
      int level = TinkerEffect.getLevel(entity, TinkerEffects.venom);
      if (level > 0) {
        originalDamage *= Math.pow(2, level);
      }
    }
    
    // ensure any changes made so far apply, though we may change it again

    // for our own armor, we have boosts from modifiers to consider
    if (context.hasModifiableArmor()) {
      // first, allow modifiers to change the damage being dealt and respond to it happening
      originalDamage = ModifyDamageModifierHook.modifyDamageTaken(ModifierHooks.MODIFY_HURT, context, source, originalDamage, OnAttackedModifierHook.isDirectDamage(source));
      if (originalDamage <= 0) {
        return 0;
      }

      // remaining logic is reducing damage like vanilla protection
      // fetch vanilla enchant level, assuming its not bypassed in vanilla
      if (DamageSourcePredicate.CAN_PROTECT.matches(source)) {
        modifierValue = vanillaModifier = EnchantmentHelper.getDamageProtection(entity.getArmorSlots(), source);
      }

      // next, determine how much tinkers armor wants to change it
      // note that armor modifiers can choose to block "absolute damage" if they wish, currently just starving damage I think
      for (EquipmentEntry entry : context.iterateTools()) {
        ModifierEntry modifier = entry.modifier();
        modifierValue = modifier.getHook(ModifierHooks.PROTECTION).getProtectionModifier(entry.tool(), modifier, context, entry.slot(), source, modifierValue);
      }

      // give slimes a 4x armor boost
      if (entity.getType().is(TinkerTags.EntityTypes.SMALL_ARMOR)) {
        modifierValue *= 4;
      }
    } else if (DamageSourcePredicate.CAN_PROTECT.matches(source) && entity.getType().is(TinkerTags.EntityTypes.SMALL_ARMOR)) {
      vanillaModifier = EnchantmentHelper.getDamageProtection(entity.getArmorSlots(), source);
      modifierValue = vanillaModifier * 4;
    }

    // if we changed anything, run our logic. Changing the cap has 2 problematic cases where same value will work:
    // * increased cap and vanilla is over the vanilla cap
    // * decreased cap and vanilla is now under the cap
    // that said, don't actually care about cap unless we have some protection, can use vanilla to simplify logic
    float cap = 20f;
    if (modifierValue > 0) {
      cap = (float) ProtectionModifierHook.getProtectionCap(entity, context.getTinkerData());
    }
    if (vanillaModifier != modifierValue || (cap > 20 && vanillaModifier > 20) || (cap < 20 && vanillaModifier > cap)) {
      // fetch armor and toughness if blockable, passing in 0 to the logic will skip the armor calculations
      float armor = 0, toughness = 0;
      if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
        armor = entity.getArmorValue();
        toughness = (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      }

      // set the final dealt damage
      float uncappedDamage = originalDamage;
      float finalDamage = ArmorUtil.getDamageForEvent(originalDamage, armor, toughness, vanillaModifier, modifierValue, cap);
      originalDamage = finalDamage;

      // armor is damaged less as a result of our math, so damage the armor based on the difference if there is one
      if (!source.is(DamageTypeTags.BYPASSES_ARMOR)) {
        int damageMissed = getArmorDamage(uncappedDamage) - getArmorDamage(finalDamage);
        // TODO: is this check sufficient for whether the armor should be damaged? I partly wonder if I need to use reflection to call damageArmor
        if (damageMissed > 0 && entity instanceof Player) {
          for (EquipmentSlot slotType : ModifiableArmorMaterial.ARMOR_SLOTS) {
            // for our own armor, saves effort to damage directly with our utility
            IToolStackView tool = context.getToolInSlot(slotType);
            if (tool != null && (!source.is(DamageTypeTags.IS_FIRE) || !tool.getItem().isFireResistant())) {
              // mark this as protection (any valid modifier really would do) so tanned can reduce it to not count it as separate damage
              ToolDamageUtil.damageAnimated(tool, damageMissed, entity, slotType, ARMOR_DAMAGE);
            } else {
              // if not our armor, damage using vanilla like logic
              ItemStack armorStack = entity.getItemBySlot(slotType);
              if (!armorStack.isEmpty() && (!source.is(DamageTypeTags.IS_FIRE) || !armorStack.getItem().isFireResistant()) && armorStack.getItem() instanceof ArmorItem) {
                armorStack.hurtAndBreak(damageMissed, entity, e -> e.broadcastBreakEvent(slotType));
              }
            }
          }
        }
      }
    }
    return originalDamage;
  }

  static void livingDamage(LivingDamageEvent event) {
    LivingEntity entity = event.getEntity();
    DamageSource source = event.getSource();

    // give modifiers a chance to respond to damage happening
    float amount = event.getAmount();
    EquipmentContext context = new EquipmentContext(entity);
    if (context.hasModifiableArmor()) {
      amount = ModifyDamageModifierHook.modifyDamageTaken(ModifierHooks.MODIFY_DAMAGE, context, source, amount, OnAttackedModifierHook.isDirectDamage(source));
      event.setAmount(amount);
      if (amount <= 0) {
        event.setCanceled(true);
      }
    }

    // for remaining code, ensure amount is not more than they will take
    amount = Math.min(amount, entity.getHealth());

    // apply post hit modifier effects. Done regardless of damage dealt - don't care if absorption took it all
    if (Config.COMMON.allowMonsterMeleeModifiers.get() && source.is(TinkerTags.DamageTypes.MODIFIER_WHITELIST)) {
      Entity attacker = event.getSource().getEntity();
      if (attacker != null && !attacker.getType().is(TinkerTags.EntityTypes.DAMAGE_MODIFIER_BLACKLIST) && attacker instanceof LivingEntity living) {
        ItemStack weapon = living.getMainHandItem();
        if (!weapon.isEmpty()) {
          if (weapon.is(TinkerTags.Items.MELEE_WEAPON)) {
            // already know we are not a player
            IToolStackView tool = ToolStack.from(weapon);
            ToolAttackContext meleeContext = ToolAttackContext.attacker(living, null).target(event.getEntity()).applyAttributes().build();
            for (ModifierEntry entry : tool.getModifiers()) {
              entry.getHook(ModifierHooks.MONSTER_MELEE_HIT).onMonsterMeleeHit(tool, entry, meleeContext, amount);
            }
          }
        } else {// unarmed
          // not consider adding non-chest armor to the unarmed tag
          ItemStack unarmed = living.getItemBySlot(EquipmentSlot.CHEST);
          if (!unarmed.isEmpty() && unarmed.is(TinkerTags.Items.UNARMED)) {
            // already know we are not a player
            IToolStackView tool = ToolStack.from(unarmed);
            ToolAttackContext meleeContext = ToolAttackContext.attacker(living, null).target(event.getEntity()).applyAttributes().addBaseDamage(tool.getStats().get(ToolStats.ATTACK_DAMAGE)).slot(EquipmentSlot.CHEST, InteractionHand.MAIN_HAND).build();
            for (ModifierEntry entry : tool.getModifiers()) {
              entry.getHook(ModifierHooks.MONSTER_MELEE_HIT).onMonsterMeleeHit(tool, entry, meleeContext, amount);
            }
          }
        }
      }
    }

    // when damaging ender dragons, may drop scales - must be player caused explosion, end crystals and TNT are examples
    if (amount >= 2 && Config.COMMON.dropDragonScales.get() && entity.getType() == EntityType.ENDER_DRAGON
        && source.is(DamageTypeTags.IS_EXPLOSION) && source.getEntity() != null && source.getEntity().getType() == EntityType.PLAYER) {
      // drops up to 1 scale per heart lost, capping at 5 scales
      ModifierUtil.dropItem(entity, new ItemStack(TinkerModifiers.dragonScale, 1 + entity.level().random.nextInt(Math.min(5, (int)(amount / 2)))));
    }
  }

  /** Called the modifier hook when an entity's position changes */
  static void livingWalk(LivingTickEvent event) {
    LivingEntity living = event.getEntity();
    // this event runs before vanilla updates prevBlockPos
    BlockPos pos = living.blockPosition();
    BlockPos lastPos = EntityHelper.getLastPos(living);
    if (!living.isSpectator() && !living.level().isClientSide() && living.isAlive() && !Objects.equals(lastPos, pos)) {
      ItemStack boots = living.getItemBySlot(EquipmentSlot.FEET);
      if (!boots.isEmpty() && boots.is(TinkerTags.Items.BOOTS)) {
        ToolStack tool = ToolStack.from(boots);
        for (ModifierEntry entry : tool.getModifierList()) {
          entry.getHook(ModifierHooks.BOOT_WALK).onWalk(tool, entry, living, lastPos, pos);
        }
      }
    }
  }

  /** Handles visibility effects of mob disguise and projectile protection */
  static void livingVisibility(LivingVisibilityEvent event) {
    // always nonnull in vanilla, not sure when it would be nullable but I dont see a need for either modifier
    Entity lookingEntity = event.getLookingEntity();
    if (lookingEntity == null) {
      return;
    }
    LivingEntity living = (LivingEntity) event.getEntity();
    TinkerDataCapability.Holder data = TinkerDataCapability.getData(living);
    if (data != null) {
      // mob disguise
      Multiset<EntityType<?>> disguises = data.get(MobDisguiseModule.DISGUISES);
      if (disguises != null) {
        int count = disguises.count(lookingEntity.getType());
        if (count > 0) {
          // halves the range per level
          event.modifyVisibility(1 / Math.pow(2, count));
        }
      }
    }
  }

  /** Syncs arrow modifier list to the client */
  static void projectileSync(Entity entity, ServerPlayer player) {
    if (entity instanceof Projectile) {
      TinkerNetwork.getInstance().sendTo(new SyncProjectileModifiersPacket(entity), player);
    }
  }

  /** Implements projectile hit hook */
  static void projectileHit(ProjectileImpactEvent event) {
    Projectile projectile = event.getProjectile();
    ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(projectile);
    if (!modifiers.isEmpty()) {
      ModDataNBT nbt = PersistentDataCapability.getOrWarn(projectile);
      HitResult hit = event.getRayTraceResult();
      HitResult.Type type = hit.getType();
      // extract a firing entity as that is a common need
      LivingEntity attacker = projectile.getOwner() instanceof LivingEntity l ? l : null;
      ModuleHook<ProjectileHitModifierHook> hook = projectile.level().isClientSide ? ModifierHooks.PROJECTILE_HIT_CLIENT : ModifierHooks.PROJECTILE_HIT;
      switch(type) {
        case ENTITY -> {
          EntityHitResult entityHit = (EntityHitResult)hit;
          // cancel all effects on endermen unless we have enderference, endermen like to teleport away
          // yes, hardcoded to enderference, if you need your own enderference for whatever reason, talk to us
          Entity entity = entityHit.getEntity();
          // extract a living target as that is the most common need
          LivingEntity target = ToolAttackUtil.getLivingEntity(entity);
          if (TinkerEffects.canHitWithProjectile(target) || nbt.getBoolean(TinkerEffects.ENDERFERENCE_KEY)) {

            // if its a piercing arrow, skip modifier effects when at the piercing limit, arrow is going to skip the hit
            boolean canBlock = true;
            if (projectile instanceof AbstractArrow arrow) {
              int pierce = arrow.getPierceLevel();
              if (pierce > 0) {
                if (arrow.piercingIgnoreEntityIds != null && arrow.piercingIgnoreEntityIds.size() >= pierce + 1) {
                  return;
                }
                canBlock = false;
              }
            }

            // ensure we are not blocking, that means projectile shouldn't hit
            boolean notBlocked = true;
            if (canBlock && target != null && target.isBlocking()) {
              Vec3 direction = projectile.position().vectorTo(target.position()).normalize();
              direction = new Vec3(direction.x, 0.0D, direction.z);
              if (direction.dot(target.getViewVector(1.0F)) < 0.0D) {
                notBlocked = false;
              }
            }
            for (ModifierEntry entry : modifiers.getModifiers()) {
              // TODO 1.21: pass in arrow with projectile to save some instance of checks
              if (entry.getHook(hook).onProjectileHitEntity(modifiers, nbt, entry, projectile, entityHit, attacker, target, notBlocked)) {
                // on forge, this means the cancelled entity won't be hit again if its a piercing arrow
                // on neo, they will get processed again next frame. Is this something we need to work around?
                event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);
                break;
              }
            }
          }
        }
        case BLOCK -> {
          BlockHitResult blockHit = (BlockHitResult)hit;
          for (ModifierEntry entry : modifiers.getModifiers()) {
            if (entry.getHook(hook).onProjectileHitsBlock(modifiers, nbt, entry, projectile, blockHit, attacker)) {
              event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT);
              break;
            }
          }
        }
      }
    }
  }
}
