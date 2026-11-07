package slimeknights.tconstruct.shared;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.fluid.FluidStorages;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;

/**
 * Development helper that builds a small showcase of the mod's blocks in the loaded world, used by the scripted
 * capture run that produces marketing screenshots. Never called in a normal game.
 */
public final class PortShowcase {
  /** platform top surface, blocks are placed on top of it */
  private static final int FLOOR_Y = 99;

  private PortShowcase() {}

  /** the y level of the stage surface, valid after {@link #build(MinecraftServer)} ran */
  public static int floorY() {
    return floorY;
  }

  public static void build(MinecraftServer server) {
    ServerLevel level = server.overworld();
    if (!level.getBlockState(new BlockPos(20, level.getHeight(Heightmap.Types.WORLD_SURFACE, 20, 6), 6)).is(Blocks.SMOOTH_STONE)) {
      buildPlatform(level);
    }
    buildSmeltery(level, 0, 0, true);
    buildSmeltery(level, 10, 0, false);
    buildStationRow(level);
    buildCasting(level);
    buildGlassWall(level);
    buildSoulRoom(level);
  }

  /**
   * Development helper: seals a small cube of soul glass so the camera can sit inside it. Used to reproduce the
   * "inside a view blocking block" overlay, which renders very differently from regular block rendering.
   */
  private static void buildSoulRoom(ServerLevel level) {
    TConstruct.LOG.info("[showcase] soul room at y={} floorY={}", floorY + 1, floorY);
    int y = floorY + 1;
    BlockState glass = TinkerCommons.soulGlass.get().defaultBlockState();
    for (int x = 24; x <= 26; x++) {
      for (int z = 16; z <= 18; z++) {
        for (int h = 0; h < 3; h++) {
          level.setBlockAndUpdate(new BlockPos(x, y + h, z), glass);
        }
      }
    }
  }

  private static int floorY = FLOOR_Y;

  /** flattens the area around the showcase into a lit stone stage sitting on the local terrain height */
  private static void buildPlatform(ServerLevel level) {
    floorY = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, 20, 6), level.getMinBuildHeight() + 16);
    BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
    BlockState support = Blocks.STONE_BRICKS.defaultBlockState();
    for (int x = -8; x <= 46; x++) {
      for (int z = -8; z <= 26; z++) {
        for (int y = floorY - 40; y < floorY; y++) {
          level.setBlockAndUpdate(new BlockPos(x, y, z), support);
        }
        level.setBlockAndUpdate(new BlockPos(x, floorY, z), floor);
        for (int y = floorY + 1; y <= floorY + 14; y++) {
          level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
      }
    }
    // lamp posts instead of floating lights, so the stage reads as a built structure
    for (int[] post : new int[][] {{-4, -4}, {42, -4}, {-4, 22}, {42, 22}}) {
      BlockPos base = new BlockPos(post[0], floorY + 1, post[1]);
      for (int h = 0; h < 3; h++) {
        level.setBlockAndUpdate(base.above(h), support);
      }
      level.setBlockAndUpdate(base.above(3), Blocks.SEA_LANTERN.defaultBlockState());
    }
  }

  /** builds a formed 5x5 smeltery, the seared variant gets a burning fuel tank and molten iron inside */
  private static void buildSmeltery(ServerLevel level, int x0, int z0, boolean seared) {
    Block wallBlock = seared ? TinkerSmeltery.searedBricks.get() : TinkerSmeltery.scorchedBricks.get();
    int y = floorY + 1;
    for (int x = 0; x < 5; x++) {
      for (int z = 0; z < 5; z++) {
        level.setBlockAndUpdate(new BlockPos(x0 + x, y, z0 + z), wallBlock.defaultBlockState());
        boolean wall = x == 0 || x == 4 || z == 0 || z == 4;
        level.setBlockAndUpdate(new BlockPos(x0 + x, y + 1, z0 + z), wall ? wallBlock.defaultBlockState() : Blocks.AIR.defaultBlockState());
      }
    }
    BlockPos controllerPos = new BlockPos(x0, y + 1, z0 + 2);
    level.setBlockAndUpdate(controllerPos, (seared ? TinkerSmeltery.smelteryController.get() : TinkerSmeltery.foundryController.get())
      .defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
    BlockPos tankPos = new BlockPos(x0 + 4, y + 1, z0 + 2);
    level.setBlockAndUpdate(tankPos, TinkerSmeltery.searedTank.get(TankType.FUEL_TANK).defaultBlockState());
    if (!(level.getBlockEntity(controllerPos) instanceof HeatingStructureBlockEntity structure)) {
      return;
    }
    structure.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, structure);
      state = level.getBlockState(controllerPos);
    }
    Storage<FluidVariant> fuel = FluidStorages.raw(level, tankPos, null);
    if (fuel != null) {
      try (Transaction tx = Transaction.openOuter()) {
        fuel.insert(FluidVariant.of(Fluids.LAVA), 81000, tx);
        tx.commit();
      }
    }
    // put some molten metal in the tank so the controller shows a full, working smeltery
    if (seared && structure instanceof SmelteryBlockEntity smeltery) {
      try (Transaction tx = Transaction.openOuter()) {
        smeltery.getTank().insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 4050, tx);
        smeltery.getTank().insert(FluidVariant.of(TinkerFluids.moltenGold.get()), 1620, tx);
        tx.commit();
      }
    }
  }

  /** the vanilla style row of tables: tinker station, part builder, modifier station */
  private static void buildStationRow(ServerLevel level) {
    int y = floorY + 1;
    level.setBlockAndUpdate(new BlockPos(0, y, 10), TinkerTables.tinkerStation.get().defaultBlockState());
    level.setBlockAndUpdate(new BlockPos(2, y, 10), TinkerTables.partBuilder.get().defaultBlockState());
    level.setBlockAndUpdate(new BlockPos(4, y, 10), TinkerTables.craftingStation.get().defaultBlockState());
    level.setBlockAndUpdate(new BlockPos(6, y, 10), TinkerTables.partBuilder.get().defaultBlockState());
  }

  /** tank above a faucet above a casting table, ready to pour an ingot */
  private static void buildCasting(ServerLevel level) {
    int y = floorY + 1;
    BlockPos tankPos = new BlockPos(10, y + 2, 10);
    BlockPos faucetPos = new BlockPos(10, y + 1, 10);
    BlockPos tablePos = new BlockPos(10, y, 10);
    level.setBlockAndUpdate(tankPos, TinkerSmeltery.searedTank.get(TankType.INGOT_TANK).defaultBlockState());
    level.setBlockAndUpdate(faucetPos, TinkerSmeltery.searedFaucet.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.DOWN));
    level.setBlockAndUpdate(tablePos, TinkerSmeltery.searedTable.get().defaultBlockState());
    Storage<FluidVariant> tank = FluidStorages.raw(level, tankPos, Direction.DOWN);
    if (tank != null) {
      try (Transaction tx = Transaction.openOuter()) {
        tank.insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 2880, tx);
        tx.commit();
      }
    }
    if (level.getBlockEntity(tablePos) instanceof CastingBlockEntity table) {
      table.setItem(CastingBlockEntity.INPUT, new ItemStack(TinkerSmeltery.ingotCast.get()));
    }
    level.setBlockAndUpdate(new BlockPos(12, y, 10), TinkerSmeltery.searedBasin.get().defaultBlockState());
  }

  /** a wall of clear and seared glass, the connected textures are one of the mod's visual highlights */
  private static void buildGlassWall(ServerLevel level) {
    int y = floorY + 1;
    for (int x = 0; x < 8; x++) {
      for (int h = 0; h < 3; h++) {
        level.setBlockAndUpdate(new BlockPos(x, y + h, 20), TinkerCommons.clearGlass.get().defaultBlockState());
      }
    }
    for (int x = 9; x < 13; x++) {
      for (int h = 0; h < 3; h++) {
        level.setBlockAndUpdate(new BlockPos(x, y + h, 20), TinkerSmeltery.searedGlass.get().defaultBlockState());
      }
    }
    for (int x = 14; x < 17; x++) {
      level.setBlockAndUpdate(new BlockPos(x, y, 20), TinkerSmeltery.searedSoulGlass.get().defaultBlockState());
    }
  }

  /** unused marker to keep the tag import meaningful if the showcase ever needs tagged blocks */
  @SuppressWarnings("unused")
  private static boolean isTagged(BlockState state) {
    return state.is(TinkerTags.Blocks.SMELTERY_TANKS);
  }
}
