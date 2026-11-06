package slimeknights.tconstruct.library.json.variable.stat;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * Datagen helper for making conditional {@link ConditionalStatVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionStatVariable(ConditionalStatVariable ifTrue, ConditionalStatVariable ifFalse, ConditionJsonProvider... conditions) implements ConditionalStatVariable, ConditionalObject<ConditionalStatVariable> {
  @Override
  public float getValue(IToolStackView tool, @Nullable LivingEntity entity) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(tool, entity);
  }

  @Override
  public RecordLoadable<? extends ConditionalStatVariable> getLoader() {
    return ConditionalStatVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
