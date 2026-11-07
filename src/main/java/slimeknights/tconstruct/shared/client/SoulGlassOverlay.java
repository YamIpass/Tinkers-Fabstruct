package slimeknights.tconstruct.shared.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.tconstruct.TConstruct;

/**
 * Vanilla's "head inside a view blocking block" overlay draws one unblended quad tinted to 10% brightness, which turns
 * translucent soul glass into a black screen. Rebuild that veil on the HUD instead: a light darkening pass plus the
 * block texture stretched over the whole screen, so the glass reads as a translucent layer over the world like the
 * Forge build.
 */
public class SoulGlassOverlay implements HudRenderCallback {
  public static final SoulGlassOverlay INSTANCE = new SoulGlassOverlay();
  private static final ResourceLocation TEXTURE = TConstruct.getResource("textures/block/soul_glass_overlay.png");
  /** generated from the block texture with its alpha scaled down, so the veil stays translucent at night */
  private static final int VEIL = 0x00000000;

  private SoulGlassOverlay() {}

  public static void init() {
    HudRenderCallback.EVENT.register(INSTANCE);
  }

  @Override
  public void onHudRender(GuiGraphics graphics, float tickDelta) {
    Minecraft minecraft = Minecraft.getInstance();
    LocalPlayer player = minecraft.player;
    if (player == null || player.isSpectator()) {
      return;
    }
    if (!isInsideSoulGlass(player)) {
      return;
    }
    int width = graphics.guiWidth();
    int height = graphics.guiHeight();
    // blit samples [0..width] / textureWidth, so handing it the draw size keeps the uv range at 0..1 and the 16x16
    // sprite is stretched once across the screen. Passing the sprite size here would tile it.
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    graphics.blit(TEXTURE, 0, 0, width, height, 0.0F, 0.0F, 16, 16, 16, 16);
    RenderSystem.disableBlend();
  }

  /** checks the block the player's eye sits in */
  private static boolean isInsideSoulGlass(LocalPlayer player) {
    BlockPos pos = BlockPos.containing(player.getEyePosition());
    BlockState state = player.level().getBlockState(pos);
    if (state.isAir() || !state.isViewBlocking(player.level(), pos)) {
      return false;
    }
    ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
    return id.getNamespace().equals("tconstruct") && id.getPath().contains("soul_glass");
  }
}
