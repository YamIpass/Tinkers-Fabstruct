package slimeknights.tconstruct.library.json.variable.mining;

import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents.BreakSpeed;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.hook.mining.BreakSpeedContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * Datagen helper for making conditional {@link MiningSpeedVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionMiningSpeedVariable(MiningSpeedVariable ifTrue, MiningSpeedVariable ifFalse, ConditionJsonProvider... conditions) implements MiningSpeedVariable, ConditionalObject<MiningSpeedVariable> {
  @Deprecated
  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeed event, @Nullable Player player, @Nullable Direction sideHit) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(tool, event, player, sideHit);
  }

  @Override
  public float getValue(IToolStackView tool, @Nullable BreakSpeedContext context, @Nullable Player player) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(tool, context, player);
  }

  @Override
  public RecordLoadable<? extends MiningSpeedVariable> getLoader() {
    return MiningSpeedVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
