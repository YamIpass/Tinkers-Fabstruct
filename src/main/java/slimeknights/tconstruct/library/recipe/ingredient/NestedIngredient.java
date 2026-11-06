package slimeknights.tconstruct.library.recipe.ingredient;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * Ingredient that contains another ingredient nested inside
 * <p>
 * TODO(fabric): Forge's {@code AbstractIngredient} exposed {@code invalidate()} to drop cached stacks when tags reload; Fabric offers no
 * equivalent hook on {@link Ingredient}, so subclasses keep their cached stacks until re-created.
 */
public abstract class NestedIngredient extends Ingredient {
  protected final Ingredient nested;

  protected NestedIngredient(Ingredient nested) {
    super(Stream.empty());
    this.nested = nested;
  }


  /* Defer to nested */

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return nested.test(stack);
  }

  @Override
  public ItemStack[] getItems() {
    return nested.getItems();
  }

  @Override
  public IntList getStackingIds() {
    return nested.getStackingIds();
  }

  @Override
  public boolean isEmpty() {
    return nested.isEmpty();
  }

  /**
   * Returns this ingredient instead of letting Fabric wrap it.
   * <p>
   * TODO(fabric): the default implementation wraps us in a {@code CustomIngredientImpl}, which tests through
   * {@code CustomIngredient#test}. That is a Fabric API method and keeps its name, while our override of the vanilla
   * {@code Ingredient#test} is remapped to the intermediary name, so the wrapper would throw {@code AbstractMethodError}.
   */
  
  public Ingredient toVanilla() {
    return this;
  }

}
