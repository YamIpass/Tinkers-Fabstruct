package slimeknights.tconstruct.fluids.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Simple brewing recipe, standing in for {@code io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipe}.
 * <p>
 * TODO(fabric): the brewing module of PortingLib is not part of the bundled jars in {@code libs/}, so this is a
 *  local copy of that API. Switch {@link BottleBrewingRecipe} and {@code TinkerFluids} back to PortingLib's
 *  classes once the brewing artifact is added to the build.
 */
public class BrewingRecipe {
  private final Ingredient input;
  private final Ingredient ingredient;
  private final ItemStack output;

  public BrewingRecipe(Ingredient input, Ingredient ingredient, ItemStack output) {
    this.input = input;
    this.ingredient = ingredient;
    this.output = output;
  }

  /** Checks if the given stack can be brewed by this recipe */
  public boolean isInput(ItemStack stack) {
    return input.test(stack);
  }

  /** Checks if the given stack is a valid ingredient for this recipe */
  public boolean isIngredient(ItemStack stack) {
    return ingredient.test(stack);
  }

  /** Gets the input for this recipe */
  public Ingredient getInput() {
    return input;
  }

  /** Gets the ingredient required for this recipe */
  public Ingredient getIngredient() {
    return ingredient;
  }

  /** Gets the output of this recipe */
  public ItemStack getOutput() {
    return output;
  }

  /** Gets the output of this recipe if the passed stacks match, empty if they do not */
  public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
    if (isInput(input) && isIngredient(ingredient)) {
      return output.copy();
    }
    return ItemStack.EMPTY;
  }
}
