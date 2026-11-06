package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.library.utils.TeleportHelper.ITeleportEventFactory;

/** Event fired when an entity teleports via the fluid effect */
public class FluidEffectTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  public static final ITeleportEventFactory TELEPORT_FACTORY = FluidEffectTeleportEvent::new;

  public FluidEffectTeleportEvent(Entity entity, double targetX, double targetY, double targetZ) {
    super(entity, targetX, targetY, targetZ);
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
