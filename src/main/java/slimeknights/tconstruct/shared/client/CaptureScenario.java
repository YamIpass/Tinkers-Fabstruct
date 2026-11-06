package slimeknights.tconstruct.shared.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import slimeknights.tconstruct.shared.PortShowcase;

import java.io.File;
import java.util.List;

/**
 * Development helper that drives the client through a scripted set of camera positions and writes screenshots into
 * {@code run/capture/}. Used to produce marketing footage, enabled only with {@code -Dtconstruct.capture=true}.
 */
public final class CaptureScenario {
  private static final Logger LOG = LogUtils.getLogger();
  /** frames to wait after a teleport before grabbing, gives the chunk and the entity renderer time to catch up */
  private static final int SETTLE_TICKS = 45;

  /** a camera position and the point it looks at */
  private record Shot(String name, Vec3 eye, Vec3 target) {}

  private static final List<Shot> SHOTS = List.of(
    new Shot("01_overview", new Vec3(27, 120, 33), new Vec3(15, 100, 7)),
    new Shot("02_smeltery", new Vec3(9.5, 105.5, 11.5), new Vec3(2, 100.5, 2)),
    new Shot("03_foundry", new Vec3(19.5, 105.5, 11.5), new Vec3(12, 100.5, 2)),
    new Shot("04_stations", new Vec3(4, 102.4, 15.5), new Vec3(4, 100.4, 9)),
    new Shot("05_casting", new Vec3(10.5, 102.6, 14.2), new Vec3(10.5, 100.4, 10)),
    new Shot("06_glass", new Vec3(8, 102.2, 15.6), new Vec3(8, 100.6, 20)),
    new Shot("07_smeltery_close", new Vec3(4.2, 101.9, 6.5), new Vec3(3.2, 100.4, 1.2))
  );

  private static int tick;
  private static int shotIndex;
  private static int nextActionAt;
  private static boolean built;
  private static boolean finished;
  private static String pendingName;
  private static int captureAt;
  private static int floorOffset;
  private static int tabIndex = -1;

  private CaptureScenario() {}

  public static void init() {
    LOG.info("[capture] scripted capture enabled, {} shots queued", SHOTS.size());
    ClientTickEvents.END_CLIENT_TICK.register(CaptureScenario::onTick);
  }

  private static void onTick(Minecraft minecraft) {
    if (finished || minecraft.player == null || minecraft.level == null) {
      return;
    }
    MinecraftServer server = minecraft.getSingleplayerServer();
    if (server == null) {
      return;
    }
    tick++;
    if (!built) {
      if (tick < 60) {
        return;
      }
      built = true;
      minecraft.options.hideGui = true;
      server.execute(() -> {
        PortShowcase.build(server);
        floorOffset = PortShowcase.floorY() - 99;
        ServerLevel level = server.overworld();
        level.setDayTime(4000L);
        level.setWeatherParameters(12000, 0, false, false);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          player.setGameMode(GameType.SPECTATOR);
        }
        LOG.info("[capture] showcase built");
      });
      nextActionAt = tick + 160;
      return;
    }
    if (pendingName != null && tick >= captureAt) {
      String name = pendingName;
      pendingName = null;
      capture(minecraft, name);
      nextActionAt = tick + 10;
      return;
    }
    if (tick < nextActionAt) {
      return;
    }
    if (shotIndex < SHOTS.size()) {
      Shot shot = SHOTS.get(shotIndex);
      shotIndex++;
      pendingName = shot.name();
      captureAt = tick + SETTLE_TICKS;
      nextActionAt = captureAt + 1;
      server.execute(() -> teleport(server, shot));
      return;
    }
    // the world shots are done, switch to the creative inventory tabs which showcase the full item list
    if (tabIndex < 0) {
      tabIndex = 0;
      server.execute(() -> {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
          player.setGameMode(GameType.CREATIVE);
        }
      });
      minecraft.options.hideGui = false;
      minecraft.options.guiScale().set(3);
      minecraft.resizeDisplay();
      nextActionAt = tick + 20;
      return;
    }
    if (tabIndex < CREATIVE_TABS.size()) {
      if (!selectCreativeTab(minecraft)) {
        nextActionAt = tick + 5;
        return;
      }
      pendingName = CREATIVE_TABS.get(tabIndex);
      captureAt = tick + 25;
      nextActionAt = captureAt + 1;
      tabIndex++;
      return;
    }
    finished = true;
    LOG.info("[capture] all shots written, closing the client");
    minecraft.stop();
  }

  private static final List<String> CREATIVE_TABS = List.of("08_tab_tools", "09_tab_parts", "10_tab_smeltery");

  /** points the open creative screen at the next of the mod's tabs, returns false when the screen is not open yet */
  private static boolean selectCreativeTab(Minecraft minecraft) {
    if (!(minecraft.screen instanceof CreativeModeInventoryScreen screen)) {
      minecraft.setScreen(new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), true));
      return false;
    }
    List<CreativeModeTab> tabs = new java.util.ArrayList<>();
    for (CreativeModeTab tab : net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB) {
      boolean ours = false;
      for (net.minecraft.world.item.ItemStack stack : tab.getDisplayItems()) {
        if (!stack.isEmpty() && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals("tconstruct")) {
          ours = true;
          break;
        }
      }
      if (ours) {
        tabs.add(tab);
      }
    }
    int index = Math.min(tabIndex, tabs.size() - 1);
    if (index >= 0 && index < tabs.size()) {
      screen.selectTab(tabs.get(index));
      LOG.info("[capture] selected creative tab {}", net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tabs.get(index)));
      return true;
    }
    LOG.warn("[capture] the mod creative tabs were not found ({} found)", tabs.size());
    return false;
  }

  private static void teleport(MinecraftServer server, Shot shot) {
    if (server.getPlayerList().getPlayers().isEmpty()) {
      return;
    }
    ServerPlayer player = server.getPlayerList().getPlayers().get(0);
    Vec3 eye = shot.eye().add(0, floorOffset, 0);
    Vec3 target = shot.target().add(0, floorOffset, 0);
    double dx = target.x - eye.x;
    double dy = target.y - eye.y;
    double dz = target.z - eye.z;
    double horizontal = Math.sqrt(dx * dx + dz * dz);
    float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx))) - 90.0F;
    float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontal)));
    player.connection.teleport(eye.x, eye.y, eye.z, yaw, pitch);
  }

  /** writes the current framebuffer to {@code run/capture/<name>.png} */
  private static void capture(Minecraft minecraft, String name) {
    RenderTarget target = minecraft.getMainRenderTarget();
    NativeImage image = new NativeImage(target.width, target.height, false);
    RenderSystem.bindTexture(target.getColorTextureId());
    image.downloadTexture(0, false);
    image.flipY();
    File directory = new File(minecraft.gameDirectory, "capture");
    if (!directory.exists() && !directory.mkdirs()) {
      LOG.warn("[capture] could not create {}", directory);
    }
    File file = new File(directory, name + ".png");
    try {
      image.writeToFile(file);
      LOG.info("[capture] wrote {}", file);
    } catch (Exception exception) {
      LOG.error("[capture] failed to write {}", file, exception);
    } finally {
      image.close();
    }
  }
}
