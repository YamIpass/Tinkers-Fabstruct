package slimeknights.tconstruct.library.client.fluid;

import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRenderHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.fluid.texture.FluidTextureManager;

import java.util.function.Function;

/**
 * Render handler for fluids that cannot be placed in world.
 * <p>
 * Fabric resolves sprites through {@code FluidRenderHandlerRegistry}, which only knows fluids that have a block state to
 * look at. All tinkers fluids are tank-only, so the loader returns no sprites and item rendering dies. Porting the texture
 * data from mantle's {@code mantle/fluid_texture} JSON gives us the actual still and flowing textures instead.
 */
public class FluidVariantTextureHandler implements FluidVariantRenderHandler {
  public static final FluidVariantTextureHandler INSTANCE = new FluidVariantTextureHandler();

  @Override
  public TextureAtlasSprite[] getSprites(FluidVariant variant) {
    FluidType type = variant.getFluid().getFluidType();
    Function<ResourceLocation,TextureAtlasSprite> sprites = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    return new TextureAtlasSprite[]{
      sprites.apply(FluidTextureManager.getStillTexture(type)),
      sprites.apply(FluidTextureManager.getFlowingTexture(type))};
  }

  @Override
  public int getColor(FluidVariant variant, @Nullable BlockAndTintGetter view, @Nullable BlockPos pos) {
    return FluidTextureManager.getColor(variant.getFluid().getFluidType());
  }
}
