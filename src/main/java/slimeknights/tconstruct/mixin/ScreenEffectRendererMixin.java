package slimeknights.tconstruct.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla draws the "head inside a view blocking block" overlay as an unblended quad tinted to 10% brightness, which
 * makes translucent soul glass look like a black screen. Draw the same quad with blending and a darker tint so the
 * block texture reads as a translucent veil over the world, matching the Forge build.
 */
@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
  @Unique
  private static boolean tconstruct;

  @Inject(method = "renderTex", at = @At("HEAD"), cancellable = true)
  private static void tconstruct$renderBlendedOverlay(TextureAtlasSprite sprite, PoseStack poseStack, CallbackInfo callback) {
    if (sprite == null || !sprite.contents().name().getNamespace().equals("tconstruct")) {
      return;
    }
    if (!tconstruct) {
      tconstruct = true;
      slimeknights.tconstruct.TConstruct.LOG.info("[overlay] drawing blended overlay for {}", sprite.contents().name());
    }
    RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
    RenderSystem.setShaderTexture(0, sprite.atlasLocation());
    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    RenderSystem.enableBlend();
    RenderSystem.defaultBlendFunc();
    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
    buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
    buffer.vertex(poseStack.last().pose(), -1.0F, -1.0F, -0.5F).color(0.12F, 0.11F, 0.10F, 0.88F).uv(sprite.getU0(), sprite.getV1()).endVertex();
    buffer.vertex(poseStack.last().pose(), 1.0F, -1.0F, -0.5F).color(0.12F, 0.11F, 0.10F, 0.88F).uv(sprite.getU1(), sprite.getV1()).endVertex();
    buffer.vertex(poseStack.last().pose(), 1.0F, 1.0F, -0.5F).color(0.12F, 0.11F, 0.10F, 0.88F).uv(sprite.getU1(), sprite.getV0()).endVertex();
    buffer.vertex(poseStack.last().pose(), -1.0F, 1.0F, -0.5F).color(0.12F, 0.11F, 0.10F, 0.88F).uv(sprite.getU0(), sprite.getV0()).endVertex();
    Tesselator.getInstance().end();
    RenderSystem.disableBlend();
    callback.cancel();
  }
}
