package slimeknights.tconstruct.library.modifiers.modules.capacity;

import net.minecraft.world.level.material.Fluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.loadable.record.SingletonLoader;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.mantle.data.predicate.TagPredicateRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.special.CapacityBarHook;
import slimeknights.tconstruct.library.modifiers.modules.ModifierModule;
import slimeknights.tconstruct.library.module.HookProvider;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.capability.fluid.ToolTankHelper;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.function.Predicate;

/**
 * Module allowing draining a predicate of fluids as {@link CapacityBarHook}.
 * Meant to be used on the specific modifier rather than an internal modifier. Does not support filling.
 * @param helper  Tank helper to locate the fluid
 * @param fluid   Fluid to match. If the fluid does not match it will be treated as 0.
 */
public record FluidPredicateAsCapacityModule(ToolTankHelper helper, IJsonPredicate<Fluid> fluid) implements ModifierModule, CapacityBarHook {
  private static final List<ModuleHook<?>> DEFAULT_HOOKS = HookProvider.<FluidPredicateAsCapacityModule>defaultHooks(ModifierHooks.CAPACITY_BAR);
  /** TODO(fabric): Mantle 1.11 dropped the fluid predicate package, so a local tag based registry stands in for it */
  private static final TagPredicateRegistry<Fluid,Fluid> FLUID_PREDICATES = new TagPredicateRegistry<>("Fluid Predicate", simple(fluid -> true), simple(fluid -> false), Loadables.FLUID_TAG, (tag, fluid) -> fluid.is(tag));
  public static final RecordLoadable<FluidPredicateAsCapacityModule> LOADER = RecordLoadable.create(
    ToolTankHelper.LOADABLE.defaultField("tank_helper", ToolTankHelper.TANK_HELPER, FluidPredicateAsCapacityModule::helper),
    FLUID_PREDICATES.defaultField("fluid", FluidPredicateAsCapacityModule::fluid),
    FluidPredicateAsCapacityModule::new);

  /** Creates a simple fluid predicate for the local registry */
  private static IJsonPredicate<Fluid> simple(Predicate<Fluid> predicate) {
    return SingletonLoader.<IJsonPredicate<Fluid>>singleton(loader -> new IJsonPredicate<>() {
      @Override
      public boolean matches(Fluid fluid) {
        return predicate.test(fluid);
      }

      @Override
      public IJsonPredicate<Fluid> inverted() {
        return FLUID_PREDICATES.invert(this);
      }

      @Override
      public RecordLoadable<? extends IJsonPredicate<Fluid>> getLoader() {
        return loader;
      }
    });
  }

  public FluidPredicateAsCapacityModule(IJsonPredicate<Fluid> fluid) {
    this(ToolTankHelper.TANK_HELPER, fluid);
  }

  @Override
  public RecordLoadable<FluidPredicateAsCapacityModule> getLoader() {
    return LOADER;
  }

  @Override
  public List<ModuleHook<?>> getDefaultHooks() {
    return DEFAULT_HOOKS;
  }
  @Override
  public int getAmount(IToolStackView tool) {
    FluidStack fluid = helper.getFluid(tool);
    return !fluid.isEmpty() && this.fluid.matches(fluid.getFluid()) ? (int)fluid.getAmount() : 0;
  }

  @Override
  public int getCapacity(IToolStackView tool, ModifierEntry entry) {
    // if another fluid is present, act like the tank is full
    FluidStack fluid = helper.getFluid(tool);
    return fluid.isEmpty() || this.fluid.matches(fluid.getFluid()) ? helper.getCapacity(tool) : 0;
  }

  @Override
  public void setAmount(IToolStackView tool, ModifierEntry entry, int amount) {
    // only allow draining; growing the fluid is just going to cause exploits as this is a predicate; we don't know the type
    FluidStack fluid = helper.getFluid(tool);
    if (amount < fluid.getAmount() && this.fluid.matches(fluid.getFluid())) {
      helper.setFluid(tool, new FluidStack(fluid, amount));
    }
  }
}
