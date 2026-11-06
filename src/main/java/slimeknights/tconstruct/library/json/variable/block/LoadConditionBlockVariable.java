package slimeknights.tconstruct.library.json.variable.block;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;

/**
 * Datagen helper for making conditional {@link BlockVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionBlockVariable(BlockVariable ifTrue, BlockVariable ifFalse, ConditionJsonProvider... conditions) implements BlockVariable, ConditionalObject<BlockVariable> {
  @Override
  public float getValue(BlockState block) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(block);
  }

  @Override
  public RecordLoadable<? extends BlockVariable> getLoader() {
    return BlockVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
