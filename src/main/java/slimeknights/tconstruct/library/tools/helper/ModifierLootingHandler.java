package slimeknights.tconstruct.library.tools.helper;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;




import slimeknights.tconstruct.common.TinkerDamageTypes;
import slimeknights.tconstruct.common.TinkerEffect;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.hook.combat.ArmorLootingModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.LootingModifierHook;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import slimeknights.tconstruct.library.tools.capability.PersistentDataCapability;
import slimeknights.tconstruct.library.tools.context.LootingContext;
import slimeknights.tconstruct.library.tools.nbt.DummyToolStack;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.shared.TinkerEffects;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Logic to handle the looting event for all main tinker tools
 */
public class ModifierLootingHandler {
  /** If contained in the set, they should use the offhand for looting */
  private static final Map<UUID,EquipmentSlot> LOOTING_OFFHAND = new HashMap<>();
  private static boolean init = false;

  /** Initializies this listener */
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    // we overwrite looting values from vanilla in a couple cases, but mod effects that globally boost looting should still boost us
    // TODO(fabric): Forge's event priority ordering has no Fabric equivalent, so this callback cannot run at HIGH priority
    LivingEntityEvents.LOOTING_LEVEL.register(ModifierLootingHandler::onLooting);
    ServerPlayConnectionEvents.DISCONNECT.register(ModifierLootingHandler::onLeaveServer);
  }

  /**
   * Sets the hand used for looting, so the tool is fetched from the proper context
   * @param entity    Player to set
   * @param slotType  Slot type
   */
  public static void setLootingSlot(LivingEntity entity, EquipmentSlot slotType) {
    if (slotType == EquipmentSlot.MAINHAND) {
      LOOTING_OFFHAND.remove(entity.getUUID());
    } else {
      LOOTING_OFFHAND.put(entity.getUUID(), slotType);
    }
  }

  /** Gets the slot to use for looting */
  public static EquipmentSlot getLootingSlot(@Nullable LivingEntity entity) {
    return entity != null ? LOOTING_OFFHAND.getOrDefault(entity.getUUID(), EquipmentSlot.MAINHAND) : EquipmentSlot.MAINHAND;
  }

  /** Applies the looting bonus for modifiers */
  private static int onLooting(DamageSource damageSource, LivingEntity target, int level, boolean recentlyHit) {
    // must be an attacker with our tool
    if (damageSource == null) {
      return level;
    }

    // bleeding kills use the level of the effect for looting
    if (damageSource.is(TinkerDamageTypes.BLEEDING)) {
      return Math.max(0, TinkerEffect.getAmplifier(target, TinkerEffects.bleeding.get()));
    }

    // otherwise, use the proper tool
    Entity source = damageSource.getEntity();
    if (source instanceof LivingEntity holder) {
      Entity direct = damageSource.getDirectEntity();

      // determine who is in charge of the looting
      LootingContext context;
      IToolStackView tool = null;
      if (direct instanceof Projectile) {
        // need to build a context from the relevant capabilities to use the modifier
        ModifierNBT modifiers = EntityModifierCapability.getOrEmpty(direct);
        context = new LootingContext(holder, target, damageSource, null);
        // no modifiers means its not a projectile we fired, so just defer to dumb vanilla behavior of whatever looting
        // since we don't set the enchantment on our tools, our looting modifiers won't set anything here anyways
        if (!modifiers.isEmpty()) {
          // TODO(fabric): CCA components are not LazyOptional, so the fresh data is created eagerly
          ModDataNBT persistentData = PersistentDataCapability.CAPABILITY.maybeGet(direct).orElseGet(ModDataNBT::new);
          level = LootingModifierHook.getLooting(new DummyToolStack(Items.AIR, modifiers, persistentData), context, 0);
        }
      } else {
        // not an arrow? means the held tool is to blame
        EquipmentSlot slotType = getLootingSlot(holder);
        context = new LootingContext(holder, target, damageSource, slotType);
        ItemStack held = holder.getItemBySlot(slotType);

        // if its modifiable, let it increase the level
        if (held.is(TinkerTags.Items.MODIFIABLE)) {
          tool = ToolStack.from(held);
          level = LootingModifierHook.getLooting(tool, context, level);
        } else if (slotType != EquipmentSlot.MAINHAND) {
          // if it's not modifiable, yet we have a lot marked to blame for looting, ignore the event value
          level = 0;
        }
      }
      // boost looting with armor regardless, hopefully you did not switch your pants mid arrow firing
      level = ArmorLootingModifierHook.getLooting(tool, context, level);
      // we allow the hook to return negatives to cancel out looting, so ensure its at least 0
      return Math.max(level, 0);
    }
    return level;
  }

  /** Called when a player leaves the server to clear the face */
  private static void onLeaveServer(ServerGamePacketListenerImpl handler, MinecraftServer server) {
    LOOTING_OFFHAND.remove(handler.getPlayer().getUUID());
  }
}
