package slimeknights.tconstruct.library.json.variable.mining;

import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents.BreakSpeed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import slimeknights.mantle.data.loadable.primitive.FloatLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.variable.entity.EntityLightVariable;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Gets the targeted block light level. Will use the targeted position if possible, otherwise the players position
 * @param lightLayer   Block light layer to use
 * @param fallback     Fallback value if missing event and player
 */
public record BlockLightVariable(@Nullable LightLayer lightLayer, float fallback) implements MiningSpeedVariable {
  public static final RecordLoadable<BlockLightVariable> LOADER = RecordLoadable.create(
    TinkerLoadables.LIGHT_LAYER.nullableField("light_layer", BlockLightVariable::lightLayer),
    FloatLoadable.ANY.requiredField("fallback", BlockLightVariable::fallback),
    BlockLightVariable::new);

  @Deprecated
  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeed event, @Nullable Player player, @Nullable Direction sideHit) {
    if (player != null) {
      // use block position if possible player position otherwise
      BlockPos pos = player.blockPosition();
      if (event != null && sideHit != null) {
        Optional<BlockPos> eventPos = Optional.ofNullable(event.getPos());
        if (eventPos.isPresent()) {
          pos = eventPos.get().relative(sideHit);
        }
      }
      return EntityLightVariable.getLightLevel(player.level(), lightLayer, pos);
    }
    return fallback;
  }

  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeedContext context, @Nullable Player player) {
    if (player != null) {
      // use block position if possible, player position otherwise
      BlockPos pos = player.blockPosition();
      if (context != null) {
        BlockPos contextPos = context.pos();
        if (contextPos != null) {
          pos = contextPos.relative(context.sideHit());
        }
      }
      return EntityLightVariable.getLightLevel(player.level(), lightLayer, pos);
    }
    return fallback;
  }

  @Override
  public RecordLoadable<BlockLightVariable> getLoader() {
    return LOADER;
  }
}
