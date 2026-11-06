package slimeknights.tconstruct.world.client;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.resources.LegacyStuffWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.world.block.FoliageType;

import java.io.IOException;

/**
 * Color reload listener for all slime foliage types
 */
public class SlimeColorReloadListener extends SimplePreparableReloadListener<int[]> implements IdentifiableResourceReloadListener {
  private final FoliageType color;
  private final ResourceLocation path;
  public SlimeColorReloadListener(FoliageType color) {
    this.color = color;
    this.path = TConstruct.getResource("textures/colormap/" + color.getSerializedName() + "_grass_color.png");
  }

  @Override
  public ResourceLocation getFabricId() {
    return TConstruct.getResource("slime_colors/" + this.color.getSerializedName());
  }

  /**
   * Performs any reloading that can be done off-thread, such as file IO
   */
  @Override
  protected int[] prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
    // TODO(fabric): Forge skipped loading while the mod loading state was invalid (ModLoader#isLoadingStateValid).
    //  Fabric has no such state, and a missing texture already falls through to the empty buffer below.
    try {
      return LegacyStuffWrapper.getPixels(resourceManager, path);
    } catch (IOException ioexception) {
      TConstruct.LOG.error("Failed to load slime colors", ioexception);
      return new int[0];
    }
  }

  @Override
  protected void apply(int[] buffer, ResourceManager resourceManager, ProfilerFiller profiler) {
    if (buffer.length != 0) {
      SlimeColorizer.setGrassColor(color, buffer);
    }
  }
}
