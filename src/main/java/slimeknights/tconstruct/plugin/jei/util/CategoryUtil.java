package slimeknights.tconstruct.plugin.jei.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/** Helpers for setting up JEI recipe categories. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CategoryUtil {
  /** Finds the slot with the given name, or null if the slot is missing */
  @Nullable
  public static IRecipeSlotDrawable findSlot(List<IRecipeSlotDrawable> slots, String name) {
    for (IRecipeSlotDrawable slot : slots) {
      if (slot.getSlotName().orElse("").equals(name)) {
        return slot;
      }
    }
    return null;
  }

  /** Filters the slot list to only those starting with the given prefix */
  public static List<IRecipeSlotDrawable> filterSlots(List<IRecipeSlotDrawable> slots, String prefix) {
    return slots.stream().filter(slot -> slot.getSlotName().orElse("").startsWith(prefix)).toList();
  }

  /** Gets the current result item focus, or {@link ItemStack#EMPTY} if the focus is absent. */
  public static ItemStack getResultItemFocus(IFocusGroup focuses) {
    IFocus<ItemStack> focus = focuses.getFocuses(VanillaTypes.ITEM_STACK, RecipeIngredientRole.OUTPUT).findFirst().orElse(null);
    if (focus != null) {
      return focus.getTypedValue().getIngredient();
    }
    return ItemStack.EMPTY;
  }

  /** Gets the width and height of the grid for a shapeless recipe in a {@link mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension} */
  public static int getShapelessSize(int total) {
    if (total > 4) {
      return 3;
    } else if (total > 1) {
      return 2;
    } else {
      return 1;
    }
  }
}
