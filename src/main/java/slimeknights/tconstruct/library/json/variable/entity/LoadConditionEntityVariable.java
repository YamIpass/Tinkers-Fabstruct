package slimeknights.tconstruct.library.json.variable.entity;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.json.variable.tool.ToolVariable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * Datagen helper for making conditional {@link EntityVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionEntityVariable(EntityVariable ifTrue, EntityVariable ifFalse, ConditionJsonProvider... conditions) implements EntityVariable, ConditionalObject<EntityVariable> {
  @Override
  public float getValue(LivingEntity entity) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(entity);
  }

  @Override
  public RecordLoadable<? extends EntityVariable> getLoader() {
    return EntityVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
