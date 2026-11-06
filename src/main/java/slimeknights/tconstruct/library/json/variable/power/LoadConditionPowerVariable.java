package slimeknights.tconstruct.library.json.variable.power;

import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import javax.annotation.Nullable;

/**
 * Datagen helper for making conditional {@link PowerVariable}.
 * @param ifTrue      Variable to use if all conditions are true.
 * @param ifFalse     Variable to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record LoadConditionPowerVariable(PowerVariable ifTrue, PowerVariable ifFalse, ConditionJsonProvider... conditions) implements PowerVariable, ConditionalObject<PowerVariable> {
  @Override
  public float getValue(ModifierNBT modifiers, ModDataNBT persistentData, ModifierEntry modifier, @Nullable Projectile projectile, @Nullable EntityHitResult hit, @Nullable LivingEntity attacker, @Nullable LivingEntity target) {
    return (testConditions(conditions) ? ifTrue : ifFalse).getValue(modifiers, persistentData, modifier, projectile, hit, attacker, target);
  }

  @Override
  public RecordLoadable<? extends PowerVariable> getLoader() {
    return PowerVariable.LOADER.getConditionalLoader();
  }

  /** Tests the conditions for this variable, mirroring the Forge condition context as closely as Fabric allows */
  public static boolean testConditions(ConditionJsonProvider[] conditions) {
    JsonObject json = new JsonObject();
    ConditionJsonProvider.write(json, conditions);
    return ResourceConditions.objectMatchesConditions(json);
  }
}
