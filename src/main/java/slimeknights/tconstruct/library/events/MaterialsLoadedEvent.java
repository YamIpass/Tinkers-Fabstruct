package slimeknights.tconstruct.library.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Event fired on {@link #EVENT} on both sides when the material registry reloads
 */
public interface MaterialsLoadedEvent {
  Event<MaterialsLoadedEvent> EVENT = EventFactory.createArrayBacked(MaterialsLoadedEvent.class, callbacks -> () -> {
    for(MaterialsLoadedEvent event : callbacks) {
      event.onLoad();
    }
  });

  void onLoad();
}
