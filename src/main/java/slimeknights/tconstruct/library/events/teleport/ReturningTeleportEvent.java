package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import net.minecraft.world.entity.LivingEntity;

/** Event fired when {@link slimeknights.tconstruct.shared.TinkerEffects#returning} teleport triggers */
public class ReturningTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  public ReturningTeleportEvent(LivingEntity entity, double targetX, double targetY, double targetZ) {
    super(entity, targetX, targetY, targetZ);
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
