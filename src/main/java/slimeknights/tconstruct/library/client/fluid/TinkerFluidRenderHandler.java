package slimeknights.tconstruct.library.client.fluid;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.inventory.InventoryMenu;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;

import java.util.function.Function;

/**
 * Fabric's block fluid rendering path, used by Star's chunk rebuild hook, {@link net.minecraft.client.particle.ParticleEngine}
 * and anything else that asks {@link net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry} for a fluid.
 * <p>
 * Ported fluids are mostly not placeable, and fabric only resolves sprites for fluids that own a block state, so every
 * fluid of this mod needs an explicit handler. The sprites come from the same {@link FluidTextureManager} data that
 * {@link FluidVariantTextureHandler} uses for item and tank rendering.
 */
public enum TinkerFluidRenderHandler implements FluidRenderHandler {
  INSTANCE;

  @Override
  public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter view, BlockPos pos, FluidState state) {
    FluidType type = state.getType().getFluidType();
    Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    return new TextureAtlasSprite[]{
      atlas.apply(FluidTextureManager.getStillTexture(type)),
      atlas.apply(FluidTextureManager.getFlowingTexture(type))};
  }

  @Override
  public int getFluidColor(BlockAndTintGetter view, BlockPos pos, FluidState state) {
    return FluidTextureManager.getColor(state.getType().getFluidType());
  }
}
