package slimeknights.tconstruct.library.json.variable.melee;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.world.entity.LivingEntity;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

/**
 * Datagen helper for making conditional {@link MeleeVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionMeleeVariable(MeleeVariable ifTrue, MeleeVariable ifFalse, ConditionJsonProvider... conditions) implements MeleeVariable, ConditionalObject<MeleeVariable> {
  @Override
  public float getValue(IToolStackView tool, @Nullable ToolAttackContext context, @Nullable LivingEntity attacker) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(tool, context, attacker);
  }

  @Override
  public RecordLoadable<? extends MeleeVariable> getLoader() {
    return MeleeVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
