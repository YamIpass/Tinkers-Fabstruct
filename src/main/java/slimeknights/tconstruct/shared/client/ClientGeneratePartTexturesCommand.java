package slimeknights.tconstruct.shared.client;

import lombok.extern.log4j.Log4j2;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import slimeknights.mantle.command.GeneratePackHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.network.GeneratePartTexturesPacket.Operation;

import java.io.File;

/**
 * Actual logic to generate tool textures
 *
 * TODO(fabric): this command cannot generate anything in this port, as the Forge data generator pipeline was dropped:
 *  {@code slimeknights.tconstruct.library.client.data.material.MaterialPartTextureGenerator} and its companion
 *  {@code GeneratorPartTextureJsonGenerator.StatOverride} do not exist here, so all texture compositing logic is gone.
 *  Restore this class once those library classes are ported.
 */
@Log4j2
public class ClientGeneratePartTexturesCommand {
  private static final String FAILURE_KEY = TConstruct.makeTranslationKey("command", "generate_part_textures.failure");

  /** @deprecated use {@link GeneratePackHelper#getOutputComponent(File)} */
  @Deprecated(forRemoval = true)
  protected static Component getOutputComponent(File file) {
    return GeneratePackHelper.getOutputComponent(file);
  }

  /** Generates all textures using the resource pack list */
  public static void generateTextures(Operation operation, String modId, String materialPath) {
    long time = System.nanoTime();
    // TODO(fabric): MaterialPartTextureGenerator is missing from this port, so no sprites can be composed
    log.error("Unable to generate part textures: this Fabric port has no texture generator (MaterialPartTextureGenerator is missing)");
    long deltaTime = System.nanoTime() - time;
    if (Minecraft.getInstance().player != null) {
      Minecraft.getInstance().player.displayClientMessage(Component.translatable(FAILURE_KEY, (deltaTime / 1000000) / 1000f, "texture generation is unavailable in this port").withStyle(ChatFormatting.RED), false);
    }
  }
}
