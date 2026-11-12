package slimeknights.mantle.plugin.jei;

import javax.annotation.Nullable;

/**
 * Helpers shared by Mantle based JEI plugins.
 *
 * <p>The upstream Mantle jar we bundle does not ship its JEI plugin package, so the tiny constants
 * the Tinkers' plugin needs live here. JEI adds the vanilla crafting grid column by column, hence the
 * index translation below.</p>
 */
public final class MantleJEIConstants {
  private MantleJEIConstants() {}

  /** Converts a recipe ingredient index into JEI's slot index for a crafting grid. */
  public static int getCraftingIndex(int index, int width, int height) {
    return (index % width) * height + (index / width);
  }
}
