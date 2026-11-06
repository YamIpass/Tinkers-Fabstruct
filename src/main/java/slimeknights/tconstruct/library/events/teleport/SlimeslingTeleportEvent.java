package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** @deprecated No longer used. See {@link SlingModifierTeleportEvent} */
@Deprecated(forRemoval = true)
public class SlimeslingTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  @Getter
  private final ItemStack sling;
  public SlimeslingTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ItemStack sling) {
    super(entity, targetX, targetY, targetZ);
    this.sling = sling;
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
