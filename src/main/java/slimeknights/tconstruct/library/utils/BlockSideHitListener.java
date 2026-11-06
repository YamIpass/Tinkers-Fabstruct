package slimeknights.tconstruct.library.utils;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import io.github.fabricators_of_create.porting_lib.entity.events.PlayerInteractionEvents;
import io.github.fabricators_of_create.porting_lib.event.common.BlockEvents;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability.TinkerDataKey;

/**
 * Logic to keep track of the side of the block that was last hit
 */
public class BlockSideHitListener {
  private static final TinkerDataKey<Direction> HIT_FACE = TConstruct.createKey("hit_face");
  private static final TinkerDataKey<Integer> LAST_XP = TConstruct.createKey("last_xp");
  @Getter
  private static Direction clientSideHit = Direction.UP;
  private static boolean init = false;

  /** @apiNote Internal method to initialize the listener. */
  @Internal
  public static void init() {
    if (init) {
      return;
    }
    init = true;
    PlayerInteractionEvents.LEFT_CLICK_BLOCK.register(BlockSideHitListener::onLeftClickBlock);
    // TODO(fabric): Forge event priority (LOWEST) has no Fabric equivalent; Porting Lib orders its own callbacks
    BlockEvents.BLOCK_BREAK.register(BlockSideHitListener::breakBlock);
  }

  /** Called when the player left-clicks a block to store the face */
  private static void onLeftClickBlock(PlayerInteractionEvents.LeftClickBlock event) {
    // TODO(fabric): Porting Lib's left click event has no start/stop action, it fires once per click
    Direction face = event.getFace();
    if (face != null) {
      Player player = event.getPlayer();
      if (player.level().isClientSide()) {
        clientSideHit = face;
      } else {
        TinkerDataCapability.Holder data = TinkerDataCapability.getData(player);
        if (data != null) {
          data.put(HIT_FACE, face);
        }
      }
    }
  }

  /** Called on block break to store the last break XP */
  private static void breakBlock(BlockEvents.BreakEvent event) {
    TinkerDataCapability.Holder data = TinkerDataCapability.getData(event.getPlayer());
    if (data != null) {
      data.put(LAST_XP, event.getExpToDrop());
    }
  }

  /**
   * Gets the side this player last hit, should return correct values in most modifier hooks related to block breaking
   * @param player  Player
   * @return  Side last hit
   */
  public static Direction getSideHit(Player player) {
    if (player.level().isClientSide()) {
      return clientSideHit;
    }
    TinkerDataCapability.Holder data = TinkerDataCapability.getData(player);
    if (data != null) {
      return data.get(HIT_FACE, Direction.UP);
    }
    return Direction.UP;
  }

  /** Gets the last XP from the break block event */
  public static int getLastXP(Player player) {
    TinkerDataCapability.Holder data = TinkerDataCapability.getData(player);
    if (data != null) {
      return data.get(LAST_XP, 0);
    }
    return 0;
  }
}
