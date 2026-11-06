package slimeknights.tconstruct.fluids.util;

import io.github.fabricators_of_create.porting_lib.mixin.accessors.common.accessor.PotionBrewingAccessor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Registry for {@link BrewingRecipe}, standing in for
 * {@code io.github.fabricators_of_create.porting_lib.brewing.BrewingRecipeRegistry}.
 * <p>
 * Recipes are added to the vanilla container mixes, which is the only brewing hook available without the PortingLib
 * brewing module (that module also patches the brewing stand menu slots, which is not reproduced here).
 * <p>
 * TODO(fabric): replace with PortingLib's brewing registry once the brewing artifact is available in {@code libs/}.
 *  Until then the brewing stand will only accept the custom bottles from automation, not from the player's cursor.
 */
public final class BrewingRecipeRegistry {
  private BrewingRecipeRegistry() {}

  /** Adds the given recipe to the vanilla container mixes */
  public static void addRecipe(BrewingRecipe recipe) {
    Ingredient ingredient = recipe.getIngredient();
    ItemStack output = recipe.getOutput();
    if (ingredient.isEmpty() || output.isEmpty()) {
      return;
    }
    Item result = output.getItem();
    for (ItemStack inputStack : recipe.getInput().getItems()) {
      PotionBrewingAccessor.port_lib$CONTAINER_MIXES().add(new PotionBrewing.Mix<>(inputStack.getItem(), ingredient, result));
    }
  }
}
