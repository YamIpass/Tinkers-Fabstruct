package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents.BreakSpeed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Optional;
import javax.annotation.Nullable;

/** Gets the biome temperature at the targeted block */
public record BlockTemperatureVariable(float fallback) implements MiningSpeedVariable {
  public static final RecordLoadable<BlockTemperatureVariable> LOADER = RecordLoadable.create(
    FloatLoadable.ANY.requiredField("fallback", BlockTemperatureVariable::fallback),
    BlockTemperatureVariable::new);

  @Deprecated
  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeed event, @Nullable Player player, @Nullable Direction sideHit) {
    if (player != null) {
      // use block position if possible player position otherwise
      BlockPos pos = player.blockPosition();
      if (event != null) {
        Optional<BlockPos> eventPos = Optional.ofNullable(event.getPos());
        if (eventPos.isPresent()) {
          pos = eventPos.get();
        }
      }
      return player.level().getBiome(pos).value().getTemperature(pos);
    }
    return fallback;
  }

  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeedContext context, @Nullable Player player) {
    if (player != null) {
      // use block position if possible player position otherwise
      BlockPos pos = player.blockPosition();
      if (context != null) {
        BlockPos contextPos = context.pos();
        if (contextPos != null) {
          pos = contextPos;
        }
      }
      return player.level().getBiome(pos).value().getTemperature(pos);
    }
    return fallback;
  }

  @Override
  public RecordLoadable<BlockTemperatureVariable> getLoader() {
    return LOADER;
  }
}
