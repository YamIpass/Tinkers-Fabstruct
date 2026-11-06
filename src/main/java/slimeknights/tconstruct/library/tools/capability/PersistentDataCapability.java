package slimeknights.tconstruct.library.tools.capability;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.network.SyncPersistentDataPacket;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;

/**
 * Capability to store persistent NBT data on an entity. For players, this is automatically synced to the client on load, but not during gameplay.
 * Persists after death, will reassess if we need some data to not persist death
 */
public class PersistentDataCapability implements EntityComponentInitializer {
  public PersistentDataCapability() {}

  /** Component ID */
  private static final ResourceLocation ID = TConstruct.getResource("persistent_data");
  /** Component type */
  public static final ComponentKey<ModDataNBT> CAPABILITY = ComponentRegistry.getOrCreate(ID, ModDataNBT.class);

  /** Gets the data or warns if its missing */
  public static ModDataNBT getOrWarn(Entity entity) {
    ModDataNBT data = CAPABILITY.maybeGet(entity).orElse(null);
    if (data == null) {
      TConstruct.LOG.warn("Missing Tinkers NBT on entity {}, this should not happen", entity.getType());
      return new ModDataNBT();
    }
    return data;
  }

  /** Registers this capability */
  public static void register() {
    ServerPlayerEvents.COPY_FROM.register(PersistentDataCapability::playerClone);
    ServerPlayerEvents.AFTER_RESPAWN.register(PersistentDataCapability::playerRespawn);
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(PersistentDataCapability::playerChangeDimension);
    ServerPlayConnectionEvents.JOIN.register(PersistentDataCapability::playerLoggedIn);
  }

  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    // must be on living entities as we use this for potions, but also support anything else with modifiers, this is their data
    registry.registerFor(LivingEntity.class, CAPABILITY, entity -> new ModDataNBT());
    registry.registerFor(Projectile.class, CAPABILITY, entity -> new ModDataNBT());
    // TODO(fabric): CCA registers components per entity class, so entities only matching a registered predicate get no data
  }

  /** Syncs the data to the given player */
  private static void sync(Player player) {
    CAPABILITY.maybeGet(player).ifPresent(data -> TinkerNetwork.getInstance().sendTo(new SyncPersistentDataPacket(data.getCopy()), (ServerPlayer) player));
  }

  /** copy caps when the player respawns/returns from the end */
  private static void playerClone(ServerPlayer original, ServerPlayer newPlayer, boolean isAlive) {
    ModDataNBT oldData = CAPABILITY.maybeGet(original).orElse(null);
    if (oldData != null) {
      CompoundTag nbt = oldData.getCopy();
      if (!nbt.isEmpty()) {
        CAPABILITY.get(newPlayer).copyFrom(nbt);
      }
    }
  }

  /** sync caps when the player respawns/returns from the end */
  private static void playerRespawn(ServerPlayer original, ServerPlayer newPlayer, boolean isAlive) {
    sync(newPlayer);
  }

  /** sync caps when the player changes dimensions */
  private static void playerChangeDimension(ServerPlayer player, ServerLevel origin, ServerLevel destination) {
    sync(player);
  }

  /** sync caps when the player logs in */
  private static void playerLoggedIn(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server) {
    sync(handler.getPlayer());
  }
}
