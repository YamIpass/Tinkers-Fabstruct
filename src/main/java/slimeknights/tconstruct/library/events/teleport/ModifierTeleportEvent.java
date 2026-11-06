package slimeknights.tconstruct.library.events.teleport;

import io.github.fabricators_of_create.porting_lib.entity.events.EntityEvents;
import lombok.Getter;
import net.minecraft.world.entity.Entity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;

/** Event fired when an entity teleports via a tool modifier. Subclasses may have more context. */
@Getter
public class ModifierTeleportEvent extends EntityEvents.Teleport.EntityTeleportEvent {
  private final ModifierEntry modifier;
  public ModifierTeleportEvent(Entity entity, double targetX, double targetY, double targetZ, ModifierEntry modifier) {
    super(entity, targetX, targetY, targetZ);
    this.modifier = modifier;
  }

  @Override
  public void sendEvent() {
    EntityEvents.TELEPORT.invoker().onTeleport(this);
  }
}
