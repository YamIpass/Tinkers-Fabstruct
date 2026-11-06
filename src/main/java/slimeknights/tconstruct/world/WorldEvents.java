package slimeknights.tconstruct.world;

import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents;
import io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.LivingVisibilityEvent;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SkullBlock;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.world.logic.AncientToolItemListing;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class WorldEvents {
  /** Registered the world event listeners */
  public static void init() {
    LivingVisibilityEvent.VISIBILITY.register(WorldEvents::livingVisibility);
    LivingEntityEvents.DROPS.register(WorldEvents::creeperKill);
    // TODO(fabric): Forge listened for WandererTradesEvent, which handed both the common and the rare trade lists.
    //  Fabric registers the trade factories per level instead; level 2 is the rare list this handler filled.
    TradeOfferHelper.registerWanderingTraderOffers(2, WorldEvents::wanderingTrades);
  }

  /* Heads */

  static void livingVisibility(LivingVisibilityEvent event) {
    Entity lookingEntity = event.getLookingEntity();
    if (lookingEntity == null) {
      return;
    }
    ItemStack helmet = event.getEntity().getItemBySlot(EquipmentSlot.HEAD);
    Item item = helmet.getItem();
    if (item != Items.AIR && TinkerWorld.headItems.contains(item)) {
      if (lookingEntity.getType() == ((TinkerHeadType)((SkullBlock)((BlockItem)item).getBlock()).getType()).getType()) {
        event.modifyVisibility(0.5f);
      }
    }
  }

  static boolean creeperKill(LivingEntity dying, DamageSource source, Collection<ItemEntity> drops, int lootingLevel, boolean recentlyHit) {
    if (source != null) {
      Entity entity = source.getEntity();
      if (entity instanceof Creeper creeper) {
        if (creeper.canDropMobsSkull()) {
          TinkerHeadType headType = TinkerHeadType.fromEntityType(dying.getType());
          if (headType != null && Config.COMMON.headDrops.get(headType).get()) {
            creeper.increaseDroppedSkulls();
            drops.add(dying.spawnAtLocation(TinkerWorld.heads.get(headType)));
          }
        }
      }
    }
    return false;
  }

  static void wanderingTrades(List<ItemListing> trades) {
    // add ancient tools to the wandering trader table
    int weight = Config.COMMON.wandererAncientToolWeight.get();
    if (weight > 0) {
      trades.addAll(Collections.nCopies(weight, AncientToolItemListing.INSTANCE));
    }
  }
}
