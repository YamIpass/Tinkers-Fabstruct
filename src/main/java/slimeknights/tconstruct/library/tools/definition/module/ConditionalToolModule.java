package slimeknights.tconstruct.library.tools.definition.module;

import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.ModuleHook;

import java.util.List;

/**
 * Datagen helper for making conditional {@link ModifierModule}.
 * @param ifTrue      Module to use if all conditions are true.
 * @param ifFalse     Module to use if any condition is false.
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record ConditionalToolModule(ToolModule ifTrue, ToolModule ifFalse, ConditionJsonProvider... conditions) implements ToolModule, ConditionalObject<ToolModule> {
  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return List.of();
  }

  @Override
  public RecordLoadable<? extends ToolModule> getLoader() {
    return ToolModule.LOADER.getConditionalLoader();
  }
}
