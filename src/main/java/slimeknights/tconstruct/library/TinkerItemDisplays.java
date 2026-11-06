package slimeknights.tconstruct.library;

import net.minecraft.world.item.ItemDisplayContext;

/**
 * Custom transform types used for tinkers item rendering.
 *
 * <p>{@code ItemDisplayContext} is a vanilla enum in 1.20.1; Forge added a registry on top of it so mods could add their
 * own entries. Fabric has no such registry and the enum cannot be extended, so the port maps every tinkers display
 * context onto the closest vanilla constant with the same transformation semantics. Renderers that read these only do so
 * to pick a transform, which is exactly what these constants preserve.</p>
 */
public class TinkerItemDisplays {
  private TinkerItemDisplays() {}

  /** Vanilla has no registry for display contexts on Fabric, so nothing needs registering */
  public static void init() {
    // TODO(fabric): Forge registered these in its DISPLAY_CONTEXTS registry, Fabric's ItemDisplayContext is a closed enum
  }

  /** Used by the melter and smeltery for display of items its melting */
  public static final ItemDisplayContext MELTER = ItemDisplayContext.NONE;
  /** Used by the part builder, crafting station, tinkers station, and tinker anvil */
  public static final ItemDisplayContext TABLE = ItemDisplayContext.NONE;
  /** Used by the casting table for item rendering */
  public static final ItemDisplayContext CASTING_TABLE = ItemDisplayContext.FIXED;
  /** Used by the casting basin for item rendering */
  public static final ItemDisplayContext CASTING_BASIN = ItemDisplayContext.NONE;
  /** Used by the fluid cannon for display of the item in front */
  public static final ItemDisplayContext FLUID_CANNON = ItemDisplayContext.FIXED;
  /** Used by throwing to allow adjusting the tool position */
  public static final ItemDisplayContext THROWN = ItemDisplayContext.FIXED;
}
