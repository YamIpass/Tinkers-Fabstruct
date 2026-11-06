package slimeknights.tconstruct.shared;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SidedStorageBlockEntity;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.crafting.RecipeManager;
import slimeknights.tconstruct.TConstruct;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import slimeknights.tconstruct.world.TinkerWorld;
import slimeknights.tconstruct.library.materials.MaterialRegistry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.tables.block.entity.table.TinkerStationBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.fluids.TinkerFluids;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ArmorItem;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.recipe.melting.IMeltingRecipe;
import slimeknights.tconstruct.library.recipe.melting.MeltingRecipe;
import slimeknights.tconstruct.smeltery.block.FaucetBlock;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ISmelteryTankHandler;
import slimeknights.tconstruct.smeltery.block.entity.controller.MelterBlockEntity;
import slimeknights.tconstruct.common.multiblock.ServantTileEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.FoundryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.SmelteryBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.controller.HeatingStructureBlockEntity;
import slimeknights.tconstruct.smeltery.block.controller.ControllerBlock;
import slimeknights.tconstruct.smeltery.block.entity.component.TankBlockEntity;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.ITinkerStationRecipe;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer;
import slimeknights.tconstruct.library.recipe.alloying.AlloyRecipe;
import slimeknights.tconstruct.library.recipe.alloying.IAlloyTank;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.tools.definition.ToolDefinitionLoader;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.stat.ToolStats;
import slimeknights.mantle.fluid.FluidTransferHelper;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection;
import slimeknights.mantle.fluid.transfer.IFluidContainerTransfer.TransferResult;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.entity.CastingBlockEntity;
import slimeknights.tconstruct.fluids.TinkerFluids;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.block.entity.table.PartBuilderBlockEntity;
import slimeknights.tconstruct.library.recipe.partbuilder.Pattern;
import slimeknights.tconstruct.library.materials.definition.IMaterial;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.tools.TinkerTools;
import slimeknights.tconstruct.tools.TinkerToolParts;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.tools.stats.HeadMaterialStats;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Headless smoke test for the ported data: runs once the server finished loading and reports what it found.
 *
 * <p>The port replaces two mod loaders, so "the jar starts" says nothing about whether the data packs actually resolved.
 * Every check below reads a live registry or a live recipe manager, so a failure means the corresponding data file was
 * dropped, mis-typed or lost a loader specific wrapper.
 *
 * <p>Enable with {@code -Dtconstruct.selftest=true}; the gradle task {@code runServer -Pselftest} does that.
 */
public final class PortSelfTest {
  /** System property that turns the self test on */
  public static final String FLAG = "tconstruct.selftest";
  /** ported fluid code counts millibuckets, a forge bucket is 1000 */
  private static final long FORGE_BUCKET = 1000;

  private PortSelfTest() {}

  /** Registers the self test if the system property is set */
  public static void init() {
    if (!Boolean.getBoolean(FLAG)) {
      return;
    }
    ServerLifecycleEvents.SERVER_STARTED.register(PortSelfTest::run);
  }

  private static void run(MinecraftServer server) {
    List<String> failures = new ArrayList<>();
    RecipeManager recipes = server.getRecipeManager();

    count(failures, "part builder recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.PART_BUILDER.get()).size());
    count(failures, "material recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.MATERIAL.get()).size());
    count(failures, "tinker station recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.TINKER_STATION.get()).size());
    count(failures, "melting recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.MELTING.get()).size());
    count(failures, "casting table recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.CASTING_TABLE.get()).size());
    count(failures, "casting basin recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.CASTING_BASIN.get()).size());
    count(failures, "alloy recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get()).size());
    count(failures, "fuel recipes", recipes.getAllRecipesFor(TinkerRecipeTypes.FUEL.get()).size());
    count(failures, "tool definitions", ToolDefinitionLoader.getInstance().getRegisteredToolDefinitions().size());
    count(failures, "materials", MaterialRegistry.getMaterials().size());

    // the material stats of iron as shipped in the 3.12.1 data pack
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    HeadMaterialStats head = MaterialRegistry.getInstance().getMaterialStats(iron.getIdentifier(), HeadMaterialStats.ID)
      .map(HeadMaterialStats.class::cast)
      .orElse(null);
    if (head == null) {
      fail(failures, "iron head stats are missing");
    } else {
      expect(failures, "iron head durability", head.durability(), 250);
      expectFloat(failures, "iron head mining speed", head.miningSpeed(), 6f);
      TConstruct.LOG.info("[selftest] iron head: durability={} miningSpeed={} tier={} attack={}",
        head.durability(), head.miningSpeed(), head.tier(), head.attack());
    }

    // build a fully iron pickaxe the same way the tinker station would, then read the computed stats back
    ItemStack stack = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    if (stack.isEmpty()) {
      fail(failures, "the iron pickaxe could not be built");
    } else {
      ToolStack tool = ToolStack.from(stack);
      int durability = tool.getStats().getInt(ToolStats.DURABILITY);
      float speed = tool.getStats().get(ToolStats.MINING_SPEED);
      float attack = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
      TConstruct.LOG.info("[selftest] iron pickaxe: durability={} miningSpeed={} attack={} tier={}",
        durability, speed, attack, tool.getStats().get(ToolStats.HARVEST_TIER));
      if (durability <= 0) {
        fail(failures, "the iron pickaxe has no durability");
      }
      if (speed <= 0) {
        fail(failures, "the iron pickaxe has no mining speed");
      }
    }

    checkFluidStorage(server, failures);
    checkFluidNames(failures);
    checkTankRightClick(server, failures);
    checkTankItem(failures);
    checkToolLifecycle(failures);
    checkTankEmptyBucket(server, failures);
    checkCasting(server, failures);
    checkCastingBasin(server, failures);
    checkAlloying(server, failures);
    checkMelting(server, failures);
    checkSmelteryStructure(server, failures);
    checkAllToolRecipes(server, failures);
    checkFoundryStructure(server, failures);
    checkFluidPlacement(server, failures);
    checkDrain(server, failures);
    checkSmelteryMelting(server, failures);
    checkMelter(server, failures);
    checkFaucet(server, failures);
    checkEntityMelting(server, failures);
    checkGlass(server, failures);
    checkPartBuilder(server, failures);
    checkModifierModules(failures);
    checkArmor(failures);
    checkToolTraits(failures);
    checkShield(failures);
    checkTankItemTransfer(failures);
    checkToolSlots(failures);
    checkToolBuilding(server, failures);
    checkModifierApplication(server, failures);
    checkArmorModifier(server, failures);
    checkStationRepair(server, failures);
    checkCraftingRepairKit(server, failures);
    checkBiomeModifications(server, failures);

    if (failures.isEmpty()) {
      TConstruct.LOG.info("[selftest] all checks passed");
    } else {
      for (String failure : failures) {
        TConstruct.LOG.error("[selftest] {}", failure);
      }
      TConstruct.LOG.error("[selftest] {} failure(s)", failures.size());
    }
    if (Boolean.getBoolean("tconstruct.selftest.stop")) {
      server.halt(false);
    }
  }

  /**
   * Runs a tool through the modifier and damage paths the same way gameplay would: add a modifier, damage it, break it
   * and repair it. This is the part of the port that cannot be checked by looking at data files.
   */
  /** the plate shield takes a shield core plus plating and gets its block stats from the definition */
  private static void checkShield(List<String> failures) {
    IMaterial core = MaterialRegistry.getMaterial(new MaterialId("tconstruct:wood"));
    IMaterial plating = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack stack = ToolBuildHandler.buildItemFromMaterials(TinkerTools.plateShield.get(), MaterialNBT.of(core, plating));
    if (stack.isEmpty()) {
      fail(failures, "the wood/iron plate shield could not be built");
      return;
    }
    slimeknights.tconstruct.library.tools.nbt.StatsNBT stats = ToolStack.from(stack).getStats();
    float durability = stats.get(ToolStats.DURABILITY);
    float blockAmount = stats.get(ToolStats.BLOCK_AMOUNT);
    float blockAngle = stats.get(ToolStats.BLOCK_ANGLE);
    if (durability <= 0) {
      fail(failures, "the plate shield has no durability: " + durability);
      return;
    }
    if (blockAmount != 100.0F || blockAngle != 180.0F) {
      fail(failures, "the plate shield has block_amount " + blockAmount + " and block_angle " + blockAngle + " instead of 100/180");
      return;
    }
    TConstruct.LOG.info("[selftest] the wood/iron plate shield has {} durability, blocks {} degrees for {} damage", durability, blockAngle, blockAmount);
  }

  /** the pickaxe definition grants its trait through the tool build, the same as upstream */
  private static void checkToolTraits(List<String> failures) {
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack stack = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    if (stack.isEmpty()) {
      fail(failures, "the iron pickaxe could not be built for the trait check");
      return;
    }
    ModifierId pierce = new ModifierId(TConstruct.getResource("pierce"));
    ModifierEntry entry = ToolStack.from(stack).getModifiers().getEntry(pierce);
    if (entry == ModifierEntry.EMPTY) {
      fail(failures, "the iron pickaxe is missing the pierce trait from its tool definition");
      return;
    }
    if (entry.getLevel() != 1) {
      fail(failures, "the pierce trait is level " + entry.getLevel() + " instead of 1");
      return;
    }
    TConstruct.LOG.info("[selftest] the iron pickaxe has the pierce trait at level 1 from tconstruct:tool_definitions/pickaxe");
  }

  private static void checkToolLifecycle(List<String> failures) {
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack stack = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    if (stack.isEmpty()) {
      fail(failures, "the iron pickaxe could not be built for the lifecycle check");
      return;
    }
    ToolStack tool = ToolStack.from(stack);
    int base = tool.getStats().getInt(ToolStats.DURABILITY);

    // diamond is a plain stat boost, so it has to come out of the modifier stat builder
    tool.addModifier(new ModifierId("tconstruct:diamond"), 1);
    tool.rebuildStats();
    int boosted = tool.getStats().getInt(ToolStats.DURABILITY);
    if (boosted <= base) {
      fail(failures, "adding the diamond modifier did not increase the durability, " + base + " -> " + boosted);
    } else {
      TConstruct.LOG.info("[selftest] diamond raised durability {} -> {}", base, boosted);
    }

    // damage, break and repair through the same helpers gameplay uses; diamond must not inflate the stored damage
    tool.setDamage(0);
    int damaged = ToolDamageUtil.damage(tool, 10, null, stack) ? 1 : 0;
    if (damaged == 0 && tool.getDamage() <= 0) {
      fail(failures, "damaging the pickaxe by 10 left damage " + tool.getDamage());
    } else {
      TConstruct.LOG.info("[selftest] damaging by 10 left damage {}", tool.getDamage());
    }
    tool.setDamage(tool.getStats().getInt(ToolStats.DURABILITY));
    if (!tool.isBroken()) {
      fail(failures, "the pickaxe is not reported broken at full damage");
    }
    tool.setDamage(0);
    if (tool.isBroken()) {
      fail(failures, "the pickaxe is still broken after repairing");
    } else {
      TConstruct.LOG.info("[selftest] tool damage, breaking and repairing behave");
    }
  }

  /**
   * Places a seared fuel tank and pushes lava into it through the sided storage lookup.
   *
   * <p>The port has no Forge capabilities; block entities expose {@code SidedStorageBlockEntity} which is wired into the
   * Fabric lookups by {@code FabricEvents.registerStorageFallbacks()}. A null here means right clicking a tank with a
   * bucket cannot work, which is exactly the bug this test guards against.
   */
  /** Reads the amount of fluid in the first non empty view */
  private static long firstAmount(Storage<FluidVariant> storage) {
    for (StorageView<FluidVariant> view : storage) {
      return view.getAmount();
    }
    return 0;
  }

  /**
   * Runs the block use path a real right click with a filled bucket takes, so a broken interaction shows up here instead of
   * only in game.
   */
  /** player without a network connection, used for the interaction tests */
  private static ServerPlayer fakePlayer(MinecraftServer server, ServerLevel level) {
    return new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "selftest")) {
      @Override
      public void displayClientMessage(Component message, boolean actionBar) {
        // no connection to send over while running the self test
      }
    };
  }

  private static void checkTankRightClick(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(4, 100, 4);
    BlockState state = TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState();
    level.setBlockAndUpdate(pos, state);

    // a bare server player is enough for the interaction, it only reads the held stack and the game mode
    ServerPlayer player = fakePlayer(server, level);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    InteractionResult result;
    try {
      result = state.use(level, player, InteractionHand.MAIN_HAND, hit);
    } catch (RuntimeException exception) {
      // the bare player has no connection, so the sound and inventory syncs the block sends blow up after the
      // transfer already happened; the tank contents below tell us if the interaction worked
      TConstruct.LOG.info("[selftest] the bare server player cannot receive packets, ignoring {}", exception);
      result = InteractionResult.SUCCESS;
    }
    if (!result.consumesAction()) {
      fail(failures, "right clicking a seared fuel tank with a lava bucket did nothing (" + result + ")");
      return;
    }

    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, Direction.UP);
    if (storage == null) {
      fail(failures, "the seared fuel tank lost its fluid storage after the right click");
      return;
    }
    FluidVariant held = FluidVariant.blank();
    for (StorageView<FluidVariant> view : storage) {
      held = view.getResource();
    }
    if (held.getFluid() != Fluids.LAVA) {
      fail(failures, "right clicking the seared fuel tank with a lava bucket did not store any lava");
      return;
    }
    if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET)) {
      fail(failures, "right clicking the seared fuel tank with a lava bucket did not return the empty bucket");
      return;
    }
    TConstruct.LOG.info("[selftest] right clicked a seared tank with a lava bucket and it stored lava");
  }

  /**
   * Checks every fluid we own resolves a real translation key instead of a fabricated block key.
   * <p>
   * The Fabric transfer API default naming fabricates {@code block.<namespace>.<path>} for fluids without a placeable
   * block, which showed up in JEI as raw keys such as {@code block.tconstruct.powdered_snow}. The fix lives in
   * {@link TinkerFluids#registerFluidAttributes()}.
   */
  private static void checkFluidNames(List<String> failures) {
    int checked = 0;
    for (Fluid fluid : BuiltInRegistries.FLUID) {
      ResourceLocation id = BuiltInRegistries.FLUID.getKey(fluid);
      if (!TConstruct.MOD_ID.equals(id.getNamespace())) {
        continue;
      }
      checked++;
      Component name = FluidVariantAttributes.getName(FluidVariant.of(fluid));
      if (name.getContents() instanceof TranslatableContents contents) {
        if (contents.getKey().startsWith("block.")) {
          fail(failures, "fluid " + id + " is named with the fabricated key " + contents.getKey());
          return;
        }
      } else if (name.getString().isBlank()) {
        fail(failures, "fluid " + id + " has a blank display name");
        return;
      }
    }
    TConstruct.LOG.info("[selftest] all {} tconstruct fluids resolved a proper display name", checked);
  }

  private static void checkFluidStorage(MinecraftServer server, List<String> failures) {    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(0, 100, 4);
    level.setBlockAndUpdate(pos, TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());

    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, Direction.UP);
    if (storage == null) {
      fail(failures, "fluid storage lookup returned null for a seared fuel tank");
      return;
    }
    // ported tanks still count millibuckets like forge did, so a forge bucket is 1000 units
    long simulated = StorageUtil.simulateInsert(storage, FluidVariant.of(Fluids.LAVA), FORGE_BUCKET, null);
    if (simulated != FORGE_BUCKET) {
      fail(failures, "the seared fuel tank refused lava (simulate=" + simulated + ")");
      return;
    }
    // a null transaction is only valid for simulate calls, the real insert needs an open transaction
    long filled;
    try (Transaction tx = Transaction.openOuter()) {
      filled = storage.insert(FluidVariant.of(Fluids.LAVA), FORGE_BUCKET, tx);
      tx.commit();
    }
    if (filled != FORGE_BUCKET) {
      fail(failures, "the seared fuel tank accepted the simulated lava but inserted " + filled);
      return;
    }
    FluidVariant heldFluid = FluidVariant.blank();
    for (StorageView<FluidVariant> view : storage) {
      heldFluid = view.getResource();
    }
    if (heldFluid.getFluid() != Fluids.LAVA) {
      fail(failures, "the seared tank does not hold lava");
    }
    TConstruct.LOG.info("[selftest] seared tank took {} mB of lava", filled);

    // the same path a right click with a filled bucket takes
    BlockPos bucketPos = new BlockPos(2, 100, 4);
    level.setBlockAndUpdate(bucketPos, TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    Storage<FluidVariant> bucketTank = FluidStorage.SIDED.find(level, bucketPos, Direction.UP);
    if (bucketTank == null) {
      fail(failures, "fluid storage lookup returned null for the second seared fuel tank");
      return;
    }
    TransferResult result = FluidTransferHelper.interactWithStack(bucketTank, new ItemStack(Items.LAVA_BUCKET), TransferDirection.AUTO);
    if (result == null) {
      fail(failures, "a lava bucket did not transfer into a seared fuel tank");
    } else if (result.stack().getItem() != Items.BUCKET) {
      fail(failures, "emptying a lava bucket left " + result.stack().getItem() + " instead of a bucket");
    } else {
      long kept = 0;
      for (StorageView<FluidVariant> view : bucketTank) {
        kept = view.getAmount();
      }
      TConstruct.LOG.info("[selftest] lava bucket emptied into the tank, tank now holds {} mB", kept);
      if (kept < FORGE_BUCKET) {
        fail(failures, "the seared fuel tank did not keep the lava from the bucket");
      }
    }
  }

  /**
   * Round trips fluid through a tank item, which guards the inventory drag and drop path of {@code TankItem}.
   *
   * <p>The port had dropped {@code mayHaveFluid} and the two {@code overrideStackedOnOther} overrides entirely, so a
   * filled tank could not be emptied into anything. Everything below is exactly what those methods consult.
   */
  private static void checkTankItem(List<String> failures) {
    ItemStack tank = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
    if (!TankItem.mayHaveFluid(tank)) {
      fail(failures, "a seared tank item reports that it cannot hold fluid");
    }
    TankItem.setTank(tank, new FluidStack(Fluids.LAVA, 1000));
    long stored = TankItem.getTank(tank, 1).getFluidAmount();
    if (stored != 1000) {
      fail(failures, "a seared tank item lost its fluid, got " + stored + " mB");
    } else {
      TConstruct.LOG.info("[selftest] seared tank item holds {} mB of {}", stored, TankItem.getTank(tank, 1).getFluid().getFluid());    }
    if (TankItem.mayHaveFluid(new ItemStack(Items.STONE))) {
      fail(failures, "a plain stone item claims it can hold fluid");
    }
  }

  /** a tank that was filled with a bucket must hand the fluid back when right clicked with an empty bucket */
  private static void checkTankEmptyBucket(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(10, 100, 4);
    level.setBlockAndUpdate(pos, TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    BlockState state = level.getBlockState(pos);
    ServerPlayer player = fakePlayer(server, level);

    // fill the tank first, the same way a player would
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LAVA_BUCKET));
    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    try {
      state.use(level, player, InteractionHand.MAIN_HAND, hit);
    } catch (RuntimeException exception) {
      TConstruct.LOG.info("[selftest] the bare server player cannot receive packets, ignoring {}", exception);
    }
    Storage<FluidVariant> storage = FluidStorage.SIDED.find(level, pos, Direction.UP);
    if (storage == null) {
      fail(failures, "the seared fuel tank lost its fluid storage");
      return;
    }
    long filled = firstAmount(storage);
    if (filled <= 0) {
      fail(failures, "the seared fuel tank did not accept the lava bucket, holds " + filled);
      return;
    }

    // now drain it back out with an empty bucket
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
    ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
    BlockState state2 = level.getBlockState(pos);
    try {
      state2.use(level, player, InteractionHand.MAIN_HAND, hit);
    } catch (RuntimeException exception) {
      TConstruct.LOG.info("[selftest] the bare server player cannot receive packets, ignoring {}", exception);
    }
    ItemStack after = player.getItemInHand(InteractionHand.MAIN_HAND);
    long left = firstAmount(storage);
    if (after.is(Items.LAVA_BUCKET) && left < filled) {
      TConstruct.LOG.info("[selftest] an empty bucket drained the seared fuel tank, tank now holds {} mB", left);
    } else {
      fail(failures, "right clicking the seared fuel tank with an empty bucket gave " + after.getItem()
        + " and left " + left + " mB of " + filled);
    }
  }

  /** pours molten iron onto a seared casting table holding an ingot cast, then ticks it until the ingot appears */
  private static void checkCasting(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(6, 100, 8);
    level.setBlockAndUpdate(pos, TinkerSmeltery.searedTable.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof CastingBlockEntity casting)) {
      fail(failures, "the seared casting table did not create a block entity");
      return;
    }
    casting.setItem(CastingBlockEntity.INPUT, new ItemStack(TinkerSmeltery.ingotCast.get()));
    Storage<FluidVariant> storage = casting.getFluidStorage(null);
    if (storage == null) {
      fail(failures, "the seared casting table has no fluid storage");
      return;
    }
    long inserted;
    try (Transaction transaction = Transaction.openOuter()) {
      inserted = storage.insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 90, transaction);
      if (inserted == 90) {
        transaction.commit();
      }
    }
    BlockState state = level.getBlockState(pos);
    for (int i = 0; i < 400 && casting.getItem(CastingBlockEntity.OUTPUT).isEmpty(); i++) {
      CastingBlockEntity.SERVER_TICKER.tick(level, pos, state, casting);
    }
    ItemStack output = casting.getItem(CastingBlockEntity.OUTPUT);
    if (output.isEmpty()) {
      fail(failures, "pouring 90 mB of molten iron into a casting table with an ingot cast produced nothing (inserted " + inserted + ")");
    } else {
      TConstruct.LOG.info("[selftest] casting table produced {} from 90 mB of molten iron", output);
    }
  }

  /** crafts a pick head in the part builder to prove the table -> recipe -> material pipeline works */
  /** checks the casting basin turns a molten metal block amount into a block */
  private static void checkCastingBasin(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(6, 100, 12);
    level.setBlockAndUpdate(pos, TinkerSmeltery.searedBasin.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof CastingBlockEntity casting)) {
      fail(failures, "the seared casting basin did not create a block entity");
      return;
    }
    Storage<FluidVariant> storage = casting.getFluidStorage(null);
    if (storage == null) {
      fail(failures, "the seared casting basin has no fluid storage");
      return;
    }
    long inserted;
    try (Transaction transaction = Transaction.openOuter()) {
      inserted = storage.insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 810, transaction);
      if (inserted == 810) {
        transaction.commit();
      }
    }
    BlockState state = level.getBlockState(pos);
    for (int i = 0; i < 600 && casting.getItem(CastingBlockEntity.OUTPUT).isEmpty(); i++) {
      CastingBlockEntity.SERVER_TICKER.tick(level, pos, state, casting);
    }
    ItemStack output = casting.getItem(CastingBlockEntity.OUTPUT);
    if (output.isEmpty()) {
      fail(failures, "pouring 810 mB of molten iron into a casting basin produced nothing (inserted " + inserted + ")");
    } else {
      TConstruct.LOG.info("[selftest] casting basin produced {} from 810 mB of molten iron", output);
    }
  }

  /** checks alloying recipes match and can perform on a tank holding copper and tin */
  private static void checkAlloying(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    List<AlloyRecipe> alloys = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.ALLOYING.get()));
    if (alloys.isEmpty()) {
      fail(failures, "no alloying recipes loaded");
      return;
    }
    FluidStack copper = new FluidStack(TinkerFluids.moltenCopper.get(), 270);
    FluidStack tin = new FluidStack(TinkerFluids.moltenTin.get(), 90);
    IAlloyTank tank = new IAlloyTank() {
      @Override
      public int getTemperature() {
        return 1000;
      }

      @Override
      public int getTanks() {
        return 2;
      }

      @Override
      public FluidStack getFluidInTank(int index) {
        return index == 0 ? copper : index == 1 ? tin : FluidStack.EMPTY;
      }

      @Override
      public boolean canFit(FluidStack fluid, int removed) {
        return true;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }
    };
    AlloyRecipe bronze = null;
    for (AlloyRecipe recipe : alloys) {
      if (recipe.getId().getPath().equals("smeltery/alloys/molten_bronze")) {
        bronze = recipe;
        break;
      }
    }
    if (bronze == null) {
      fail(failures, "the molten bronze alloy recipe is missing");
      return;
    }
    if (!bronze.matches(tank, level) || !bronze.canPerform(tank)) {
      fail(failures, "the bronze alloy recipe does not match 270 mB copper + 90 mB tin");
      return;
    }
    FluidStack output = bronze.getOutput();
    expect(failures, "bronze alloy output amount", (int) output.getAmount(), 360);
    TConstruct.LOG.info("[selftest] alloying matched {} from 270 mB copper + 90 mB tin", BuiltInRegistries.FLUID.getKey(output.getFluid()));
  }

  /** checks melting recipes resolve ingots to molten metal */
  private static void checkMelting(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    List<IMeltingRecipe> recipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.MELTING.get()));
    if (recipes.isEmpty()) {
      fail(failures, "no melting recipes loaded");
      return;
    }
    IMeltingContainer inv = meltingContainer(new ItemStack(Items.IRON_INGOT));
    // regression: the smeltery checks every melting recipe against the inserted stack, which used to throw
    // AbstractMethodError from CustomIngredient#test for the custom ingredients (no_container, material, ...)
    int matched = 0;
    for (IMeltingRecipe recipe : recipes) {
      if (recipe.matches(meltingContainer(new ItemStack(Items.IRON_ORE)), level)) {
        matched++;
      }
    }
    if (matched == 0) {
      fail(failures, "no melting recipe matched an iron ore stack");
      return;
    }
    TConstruct.LOG.info("[selftest] {} melting recipes matched an iron ore without a custom ingredient crash", matched);
    for (IMeltingRecipe recipe : recipes) {
      if (!recipe.getId().getPath().equals("smeltery/melting/metal/iron/ingot")) {
        continue;
      }
      if (!recipe.matches(inv, level)) {
        fail(failures, "the iron ingot melting recipe does not match an iron ingot");
        return;
      }
      FluidStack output = recipe.getOutput(inv);
      expect(failures, "iron ingot melting temperature", recipe.getTemperature(inv), 800);
      expect(failures, "iron ingot melting output amount", (int) output.getAmount(), 90);
      String fluidId = BuiltInRegistries.FLUID.getKey(output.getFluid()).toString();
      if (!fluidId.equals("tconstruct:molten_iron")) {
        fail(failures, "iron ingot melting output fluid is " + fluidId + ", expected tconstruct:molten_iron");
      }
      TConstruct.LOG.info("[selftest] melting resolves {} to {} mB {} at {}",
        BuiltInRegistries.ITEM.getKey(inv.getStack().getItem()), output.getAmount(),
        BuiltInRegistries.FLUID.getKey(output.getFluid()), recipe.getTemperature(inv));
      return;
    }
    fail(failures, "the iron ingot melting recipe is missing");
  }

  /** checks the clear glass blocks still collide, while soul glass is passable by design */
  private static void checkGlass(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(200, 100, 0);
    BlockState clear = TinkerCommons.clearGlass.get().defaultBlockState();
    if (clear.getCollisionShape(level, pos).isEmpty()) {
      fail(failures, "clear glass has no collision shape");
    }
    BlockState stained = TinkerCommons.clearStainedGlass.get(slimeknights.tconstruct.shared.block.ClearStainedGlassBlock.GlassColor.CYAN).defaultBlockState();
    if (stained.getCollisionShape(level, pos).isEmpty()) {
      fail(failures, "clear stained glass has no collision shape");
    }
    BlockState soul = TinkerCommons.soulGlass.get().defaultBlockState();
    if (!soul.getCollisionShape(level, pos).isEmpty()) {
      fail(failures, "soul glass has a collision shape, it should be passable");
    }
    BlockState seared = slimeknights.tconstruct.smeltery.TinkerSmeltery.searedGlass.get().defaultBlockState();
    boolean searedSolid = !seared.getCollisionShape(level, pos).isEmpty();
    BlockState searedSoul = slimeknights.tconstruct.smeltery.TinkerSmeltery.searedSoulGlass.get().defaultBlockState();
    boolean searedSoulSolid = !searedSoul.getCollisionShape(level, pos).isEmpty();
    if (!searedSolid) {
      fail(failures, "seared glass has no collision shape");
    }
    if (searedSoulSolid) {
      fail(failures, "seared soul glass has a collision shape, it should be passable");
    }
    TConstruct.LOG.info("[selftest] glass: clear solid={} stained solid={} soul solid={} seared solid={} seared soul solid={} seared speed={}",
      !clear.getCollisionShape(level, pos).isEmpty(), !stained.getCollisionShape(level, pos).isEmpty(),
      !soul.getCollisionShape(level, pos).isEmpty(), searedSolid, searedSoulSolid, seared.getBlock().getSpeedFactor());
  }

  /** Creates a melting container for the given stack */
  private static IMeltingContainer meltingContainer(ItemStack stack) {
    return new IMeltingContainer() {
      @Override
      public ItemStack getStack() {
        return stack;
      }

      @Override
      public IMeltingContainer.IOreRate getOreRate() {
        return (rate, amount) -> amount;
      }
    };
  }

  /** builds a minimal 3x3 smeltery with a tank in the east wall, returns the controller position */
  private static BlockPos buildSmelteryAt(ServerLevel level, int x0, int z0) {
    Block seared = TinkerSmeltery.searedBricks.get();
    for (int x = 0; x < 5; x++) {
      for (int z = 0; z < 5; z++) {
        level.setBlockAndUpdate(new BlockPos(x0 + x, 98, z0 + z), seared.defaultBlockState());
        boolean wall = x == 0 || x == 4 || z == 0 || z == 4;
        level.setBlockAndUpdate(new BlockPos(x0 + x, 99, z0 + z),
          wall ? seared.defaultBlockState() : Blocks.AIR.defaultBlockState());
      }
    }
    BlockPos controllerPos = new BlockPos(x0, 99, z0 + 2);
    level.setBlockAndUpdate(controllerPos, TinkerSmeltery.smelteryController.get().defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
    level.setBlockAndUpdate(new BlockPos(x0 + 4, 99, z0 + 2),
      TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    return controllerPos;
  }

  /** drops a zombie into a formed smeltery and checks the entity melting module damages it for fluid */
  private static void checkEntityMelting(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    level.setDayTime(18000);
    BlockPos controllerPos = buildSmelteryAt(level, 60, 0);
    if (!(level.getBlockEntity(controllerPos) instanceof SmelteryBlockEntity smeltery)) {
      fail(failures, "the smeltery controller did not create a block entity");
      return;
    }
    smeltery.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
      state = level.getBlockState(controllerPos);
    }
    if (!state.getValue(ControllerBlock.IN_STRUCTURE)) {
      fail(failures, "the smeltery did not form before the entity melting test");
      return;
    }
    // entity melting only runs when the structure has fuel
    Storage<FluidVariant> fuel = FluidStorage.SIDED.find(level, new BlockPos(64, 99, 2), null);
    if (fuel == null) {
      fail(failures, "the fuel tank of the entity melting smeltery exposes no fluid storage");
      return;
    }
    try (Transaction tx = Transaction.openOuter()) {
      fuel.insert(FluidVariant.of(Fluids.LAVA), 81000, tx);
      tx.commit();
    }
    Zombie zombie = EntityType.ZOMBIE.create(level);
    if (zombie == null) {
      fail(failures, "could not create a zombie for the entity melting test");
      return;
    }
    zombie.moveTo(62.5, 99.0, 2.5, 0.0F, 0.0F);
    zombie.setPersistenceRequired();
    level.addFreshEntity(zombie);
    // tick enough for the module to run several damage cycles
    for (int i = 0; i < 200; i++) {
      state = level.getBlockState(controllerPos);
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
    }
    FluidStack melted = smeltery.getTank().getFluidInTank(0);
    if (melted.getFluid() != TinkerFluids.moltenIron.get() || melted.getAmount() <= 0) {
      fail(failures, "the smeltery did not melt the zombie, tank holds " + melted + " (zombie health " + zombie.getHealth() + ")");
      return;
    }
    TConstruct.LOG.info("[selftest] smeltery melted a zombie for {} mB of molten iron, zombie health now {}", melted.getAmount(), zombie.getHealth());
  }

  /** pours molten iron out of a tank through a faucet into a casting table */
  private static void checkFaucet(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos tankPos = new BlockPos(49, 101, 0);
    BlockPos faucetPos = new BlockPos(49, 100, 0);
    BlockPos tablePos = new BlockPos(49, 99, 0);
    level.setBlockAndUpdate(tankPos, TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.INGOT_TANK).defaultBlockState());
    level.setBlockAndUpdate(tablePos, TinkerSmeltery.searedTable.get().defaultBlockState());
    level.setBlockAndUpdate(faucetPos, TinkerSmeltery.searedFaucet.get().defaultBlockState().setValue(FaucetBlock.FACING, Direction.DOWN));
    Storage<FluidVariant> tank = FluidStorage.SIDED.find(level, tankPos, Direction.DOWN);
    if (tank == null) {
      fail(failures, "the tank above the faucet exposes no fluid storage");
      return;
    }
    try (Transaction tx = Transaction.openOuter()) {
      tank.insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 90, tx);
      tx.commit();
    }
    // a cast in the table gives the casting recipe a capacity and a product
    if (level.getBlockEntity(tablePos) instanceof CastingBlockEntity table) {
      table.setItem(CastingBlockEntity.INPUT, new ItemStack(TinkerSmeltery.ingotCast.get()));
    }
    if (!(level.getBlockEntity(faucetPos) instanceof FaucetBlockEntity faucet)) {
      fail(failures, "the faucet did not create a block entity");
      return;
    }
    faucet.activate();
    BlockState state = level.getBlockState(faucetPos);
    for (int i = 0; i < 200; i++) {
      state = level.getBlockState(faucetPos);
      FaucetBlockEntity.SERVER_TICKER.tick(level, faucetPos, state, faucet);
    }
    // let the poured fluid cool into the cast result
    if (level.getBlockEntity(tablePos) instanceof CastingBlockEntity cooling) {
      BlockState tableState = level.getBlockState(tablePos);
      for (int i = 0; i < 100; i++) {
        tableState = level.getBlockState(tablePos);
        CastingBlockEntity.SERVER_TICKER.tick(level, tablePos, tableState, cooling);
      }
    }
    long amount = 0;
    ItemStack output = ItemStack.EMPTY;
    if (level.getBlockEntity(tablePos) instanceof CastingBlockEntity casting) {
      FluidStack fluid = casting.getTank().getFluid();
      if (fluid.getFluid() == TinkerFluids.moltenIron.get()) {
        amount = fluid.getAmount();
      }
      output = casting.getItem(CastingBlockEntity.OUTPUT).copy();
    }
    if (!output.is(Items.IRON_INGOT)) {
      fail(failures, "the faucet did not pour molten iron into the casting table, tank holds " + amount + " mB and output is " + output);
      return;
    }
    TConstruct.LOG.info("[selftest] faucet poured molten iron into a casting table and produced {}", output);
  }

  /** melts an ingot in the seared melter, fueled by lava in the block below */
  private static void checkMelter(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(40, 100, 0);
    BlockPos fuelPos = pos.below();
    // the melter reads fuel from the block below, a seared tank of lava is the cheapest fuel source
    level.setBlockAndUpdate(fuelPos, TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    // the melter is formed by placement logic when a fuel tank sits below, reproduce that state directly
    level.setBlockAndUpdate(pos, TinkerSmeltery.searedMelter.get().defaultBlockState().setValue(ControllerBlock.IN_STRUCTURE, true));
    if (!(level.getBlockEntity(pos) instanceof MelterBlockEntity melter)) {
      fail(failures, "the melter did not create a block entity");
      return;
    }
    Storage<FluidVariant> fuel = FluidStorage.SIDED.find(level, fuelPos, null);
    if (fuel == null) {
      fail(failures, "the fuel tank below the melter exposes no fluid storage");
      return;
    }
    try (Transaction tx = Transaction.openOuter()) {
      fuel.insert(FluidVariant.of(Fluids.LAVA), 81000, tx);
      tx.commit();
    }
    melter.getItemHandler().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    BlockState state = level.getBlockState(pos);
    for (int i = 0; i < 300; i++) {
      state = level.getBlockState(pos);
      MelterBlockEntity.SERVER_TICKER.tick(level, pos, state, melter);
    }
    FluidStack melted = melter.getTank().getFluid();
    if (melted.getFluid() != TinkerFluids.moltenIron.get() || melted.getAmount() < 90) {
      fail(failures, "the melter did not melt the iron ingot, tank holds " + melted);
      return;
    }
    TConstruct.LOG.info("[selftest] melter melted an iron ingot, tank holds {} mB of molten iron", melted.getAmount());
  }

  /** runs an ingot through a formed smeltery and checks it melts into the smeltery tank */
  private static void checkSmelteryMelting(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos controllerPos = buildSmelteryAt(level, 20, 0);
    if (!(level.getBlockEntity(controllerPos) instanceof SmelteryBlockEntity smeltery)) {
      fail(failures, "the smeltery controller did not create a block entity");
      return;
    }
    smeltery.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
      state = level.getBlockState(controllerPos);
    }
    if (!state.getValue(ControllerBlock.IN_STRUCTURE)) {
      fail(failures, "the smeltery did not form before the melting test");
      return;
    }
    // lava goes into the seared tank in the wall, it is the smeltery fuel
    Storage<FluidVariant> fuel = FluidStorage.SIDED.find(level, new BlockPos(24, 99, 2), null);
    if (fuel == null) {
      fail(failures, "the seared tank does not expose a fluid storage");
      return;
    }
    try (Transaction tx = Transaction.openOuter()) {
      fuel.insert(FluidVariant.of(Fluids.LAVA), 81000, tx);
      tx.commit();
    }
    smeltery.getMeltingInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    for (int i = 0; i < 300; i++) {
      state = level.getBlockState(controllerPos);
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
    }
    FluidStack melted = smeltery.getTank().getFluidInTank(0);
    if (melted.getFluid() != TinkerFluids.moltenIron.get() || melted.getAmount() < 90) {
      fail(failures, "the smeltery did not melt the iron ingot with lava fuel, tank holds " + melted);
      return;
    }
    TConstruct.LOG.info("[selftest] smeltery melted an iron ingot, tank holds {} mB of molten iron", melted.getAmount());
  }

  /** builds a smeltery with a drain in its wall and checks the fluid capability reaches the smeltery tank */
  private static void checkDrain(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    int x0 = 140;
    int z0 = 0;
    Block seared = TinkerSmeltery.searedBricks.get();
    BlockPos drainPos = new BlockPos(x0 + 4, 99, z0 + 2);
    for (int x = x0; x <= x0 + 4; x++) {
      for (int z = z0; z <= z0 + 4; z++) {
        level.setBlockAndUpdate(new BlockPos(x, 98, z), seared.defaultBlockState());
        boolean wall = x == x0 || x == x0 + 4 || z == z0 || z == z0 + 4;
        level.setBlockAndUpdate(new BlockPos(x, 99, z), wall ? seared.defaultBlockState() : Blocks.AIR.defaultBlockState());
      }
    }
    // for the drain the master is only assigned when the structure is built for the first time
    level.setBlockAndUpdate(drainPos, TinkerSmeltery.searedDrain.get().defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
    level.setBlockAndUpdate(new BlockPos(x0 + 4, 99, z0 + 1), TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    BlockPos controllerPos = new BlockPos(x0, 99, z0 + 2);
    level.setBlockAndUpdate(controllerPos, TinkerSmeltery.smelteryController.get().defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
    if (!(level.getBlockEntity(controllerPos) instanceof SmelteryBlockEntity smeltery)) {
      fail(failures, "the smeltery controller did not create a block entity");
      return;
    }
    smeltery.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
      state = level.getBlockState(controllerPos);
    }
    if (!state.getValue(ControllerBlock.IN_STRUCTURE)) {
      fail(failures, "the smeltery with a drain did not form: " + smeltery.getStructureResult());
      return;
    }
    Storage<FluidVariant> drain = FluidStorage.SIDED.find(level, drainPos, Direction.WEST);
    if (drain == null) {
      fail(failures, "the drain exposes no fluid storage on its smeltery side");
      return;
    }
    long inserted;
    try (Transaction tx = Transaction.openOuter()) {
      inserted = drain.insert(FluidVariant.of(TinkerFluids.moltenIron.get()), 90, tx);
      tx.commit();
    }
    FluidStack tank = smeltery.getTank().getFluidInTank(0);
    if (tank.getFluid() != TinkerFluids.moltenIron.get() || tank.getAmount() < 90) {
      fail(failures, "the drain accepted " + inserted + " mB but the smeltery tank holds " + tank.getAmount() + " mB");
      return;
    }
    TConstruct.LOG.info("[selftest] drain accepted 90 mB of molten iron into the smeltery tank");
  }

  /** places a molten metal bucket in the world and checks it creates the fluid block */
  private static void checkFluidPlacement(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos ground = new BlockPos(100, 90, 0);
    level.setBlockAndUpdate(ground, Blocks.STONE.defaultBlockState());
    level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
    ItemStack bucket = new ItemStack(TinkerFluids.moltenIron.getBucket());
    if (bucket.isEmpty()) {
      fail(failures, "molten iron has no bucket item");
      return;
    }
    Player player = fakePlayer(server, level);
    // same call the vanilla bucket item makes, without the client packet round trip
    // same call the bucket item makes on use, without the client packet round trip
    boolean placedFluid = ((BucketItem) bucket.getItem()).emptyContents(player, level, ground.above(), null);
    if (!placedFluid) {
      fail(failures, "the molten iron bucket reported it could not place its fluid");
      return;
    }
    FluidState placed = level.getFluidState(ground.above());
    if (placed.getType() != TinkerFluids.moltenIron.get()) {
      fail(failures, "placing a molten iron bucket left " + placed.getType() + " instead of molten iron");
      level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
      return;
    }
    TConstruct.LOG.info("[selftest] placed a molten iron fluid block from a bucket");
    level.setBlockAndUpdate(ground.above(), Blocks.AIR.defaultBlockState());
  }

  /** builds a 3x3 foundry out of scorched blocks and checks the controller forms the structure */
  private static void checkFoundryStructure(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    Block scorched = TinkerSmeltery.scorchedBricks.get();
    int x0 = 80;
    int z0 = 0;
    for (int x = x0; x <= x0 + 4; x++) {
      for (int z = z0; z <= z0 + 4; z++) {
        boolean wall = x == x0 || x == x0 + 4 || z == z0 || z == z0 + 4;
        level.setBlockAndUpdate(new BlockPos(x, 98, z), scorched.defaultBlockState());
        if (wall) {
          level.setBlockAndUpdate(new BlockPos(x, 99, z), scorched.defaultBlockState());
        } else {
          level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.AIR.defaultBlockState());
        }
      }
    }
    BlockPos controllerPos = new BlockPos(x0, 99, z0 + 2);
    level.setBlockAndUpdate(controllerPos, TinkerSmeltery.foundryController.get().defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
    level.setBlockAndUpdate(new BlockPos(x0 + 4, 99, z0 + 2), TinkerSmeltery.scorchedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    if (!(level.getBlockEntity(controllerPos) instanceof FoundryBlockEntity foundry)) {
      fail(failures, "the foundry controller did not create a block entity");
      return;
    }
    foundry.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, foundry);
      state = level.getBlockState(controllerPos);
    }
    if (!state.getValue(ControllerBlock.IN_STRUCTURE)) {
      fail(failures, "the foundry controller did not form a 3x3 structure: " + foundry.getStructureResult());
      return;
    }
    Storage<FluidVariant> fuel = FluidStorage.SIDED.find(level, new BlockPos(x0 + 4, 99, z0 + 2), null);
    if (fuel == null) {
      fail(failures, "the foundry fuel tank exposes no fluid storage");
      return;
    }
    try (Transaction tx = Transaction.openOuter()) {
      fuel.insert(FluidVariant.of(Fluids.LAVA), 81000, tx);
      tx.commit();
    }
    foundry.getMeltingInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
    for (int i = 0; i < 300; i++) {
      state = level.getBlockState(controllerPos);
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, foundry);
    }
    FluidStack melted = foundry.getTank().getFluidInTank(0);
    if (melted.getFluid() != TinkerFluids.moltenIron.get() || melted.getAmount() < 90) {
      fail(failures, "the foundry did not melt an iron ingot, tank holds " + melted.getAmount() + " mB");
      return;
    }
    TConstruct.LOG.info("[selftest] foundry controller formed a 5x5 floor and 3x3 interior");
  }

  /** builds every tool in the tool building recipe list out of iron and checks it comes out usable */
  private static void checkAllToolRecipes(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId(TConstruct.getResource("iron")));
    List<ToolBuildingRecipe> recipes = new ArrayList<>();
    for (ITinkerStationRecipe recipe : level.getRecipeManager().getAllRecipesFor(TinkerRecipeTypes.TINKER_STATION.get())) {
      if (recipe instanceof ToolBuildingRecipe building) {
        recipes.add(building);
      }
    }
    int built = 0;
    for (ToolBuildingRecipe recipe : recipes) {
      ItemStack result = recipe.getResultItem(level.registryAccess());
      if (!(result.getItem() instanceof IModifiable tool)) {
        fail(failures, "tool building recipe " + recipe.getId() + " produced a non-modifiable item " + result);
        continue;
      }
      IMaterial[] materials = new IMaterial[recipe.getToolParts().size()];
      java.util.Arrays.fill(materials, iron);
      ItemStack stack;
      try {
        stack = ToolBuildHandler.buildItemFromMaterials(tool, MaterialNBT.of(materials));
      } catch (RuntimeException e) {
        fail(failures, "building " + result.getItem() + " from iron threw " + e);
        continue;
      }
      if (stack.isEmpty() || !stack.is(result.getItem())) {
        fail(failures, "building " + result.getItem() + " produced " + stack);
        continue;
      }
      float durability = ToolStack.from(stack).getStats().get(ToolStats.DURABILITY);
      if (durability <= 0) {
        fail(failures, result.getItem() + " built from iron has durability " + durability);
        continue;
      }
      built++;
    }
    TConstruct.LOG.info("[selftest] built {} of {} tools from iron", built, recipes.size());
    if (built < recipes.size()) {
      fail(failures, "only " + built + " of " + recipes.size() + " tool building recipes produced a usable tool");
    }
  }

  /** builds a minimal 3x3 smeltery and checks the controller forms the structure */
  private static void checkSmelteryStructure(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    Block seared = TinkerSmeltery.searedBricks.get();
    // floor at y=98, walls at y=99, interior 3x3 stays air
    for (int x = -1; x <= 3; x++) {
      for (int z = -1; z <= 3; z++) {
        level.setBlockAndUpdate(new BlockPos(x, 98, z), seared.defaultBlockState());
      }
    }
    for (int x = -1; x <= 3; x++) {
      for (int z = -1; z <= 3; z++) {
        if (x == -1 || x == 3 || z == -1 || z == 3) {
          level.setBlockAndUpdate(new BlockPos(x, 99, z), seared.defaultBlockState());
        } else {
          level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.AIR.defaultBlockState());
        }
      }
    }
    BlockPos controllerPos = new BlockPos(-1, 99, 1);
    level.setBlockAndUpdate(controllerPos, TinkerSmeltery.smelteryController.get().defaultBlockState()
      .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.WEST));
    level.setBlockAndUpdate(new BlockPos(3, 99, 1), TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK).defaultBlockState());
    if (!(level.getBlockEntity(controllerPos) instanceof SmelteryBlockEntity smeltery)) {
      fail(failures, "the smeltery controller did not create a block entity");
      return;
    }
    smeltery.updateStructure();
    BlockState state = level.getBlockState(controllerPos);
    for (int i = 0; i < 5 && !state.getValue(ControllerBlock.IN_STRUCTURE); i++) {
      HeatingStructureBlockEntity.SERVER_TICKER.tick(level, controllerPos, state, smeltery);
      state = level.getBlockState(controllerPos);
    }
    if (!state.getValue(ControllerBlock.IN_STRUCTURE)) {
      fail(failures, "the smeltery controller did not form a 3x3 structure: " + smeltery.getStructureResult());
      return;
    }
    TConstruct.LOG.info("[selftest] smeltery controller formed a 3x3 structure");
  }

  private static void checkPartBuilder(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(10, 100, 8);
    level.setBlockAndUpdate(pos, TinkerTables.partBuilder.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof PartBuilderBlockEntity builder)) {
      fail(failures, "the part builder did not create a block entity");
      return;
    }
    builder.setItem(PartBuilderBlockEntity.MATERIAL_SLOT, new ItemStack(Items.COPPER_INGOT, 2));
    builder.setItem(PartBuilderBlockEntity.PATTERN_SLOT, new ItemStack(TinkerTables.pattern.get()));
    builder.selectRecipe(new Pattern("tconstruct:pick_head"));
    ItemStack result = builder.calcResult(null);
    if (result.isEmpty()) {
      fail(failures, "the part builder did not produce a pick head from 2 copper ingots");
      return;
    }
    MaterialVariantId material = IMaterialItem.getMaterialFromStack(result);
    if (!result.is(TinkerToolParts.pickHead.get()) || material == null || MaterialVariantId.parse("tconstruct:unknown").equals(material)) {
      fail(failures, "the part builder produced " + result + " instead of a copper pick head");
    } else {
      TConstruct.LOG.info("[selftest] part builder crafted {} for {}", result, material);
    }
  }

  /** builds a whole tool in the tinker station, exercising the tool building recipe and the part order */
  private static void checkToolBuilding(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(12, 100, 8);
    level.setBlockAndUpdate(pos, TinkerTables.tinkerStation.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof TinkerStationBlockEntity station)) {
      fail(failures, "the tinker station did not create a block entity");
      return;
    }
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    IMaterial wood = MaterialRegistry.getMaterial(new MaterialId("tconstruct:wood"));
    ItemStack head = ((IMaterialItem) TinkerToolParts.pickHead.get()).withMaterial(iron.getIdentifier());
    ItemStack handle = ((IMaterialItem) TinkerToolParts.toolHandle.get()).withMaterial(wood.getIdentifier());
    ItemStack binding = ((IMaterialItem) TinkerToolParts.toolBinding.get()).withMaterial(wood.getIdentifier());
    // slot 0 is the tinkerable (existing tool) slot, tool parts go into the following slots
    station.setItem(1, head);
    station.setItem(2, handle);
    station.setItem(3, binding);
    ItemStack result = station.calcResult(null);
    if (result.isEmpty()) {
      fail(failures, "the tinker station did not build a pickaxe from a pick head, a tool handle and a tool binding");
      return;
    }
    if (!result.is(TinkerTools.pickaxe.get())) {
      fail(failures, "the tinker station produced " + result.getItem() + " instead of a pickaxe");
      return;
    }
    ToolStack tool = ToolStack.from(result);
    expect(failures, "tinker station pickaxe material count", tool.getMaterials().size(), 3);
    TConstruct.LOG.info("[selftest] tinker station built {} from {} + {} (durability {})",
      BuiltInRegistries.ITEM.getKey(result.getItem()),
      iron.getIdentifier(), wood.getIdentifier(), tool.getStats().getInt(ToolStats.DURABILITY));
  }

  /** repairs a damaged tool with a repair kit in the crafting table */
  private static void checkCraftingRepairKit(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack kit = IMaterialItem.withMaterial(new ItemStack(TinkerToolParts.repairKit.get()), iron.getIdentifier());
    ItemStack pickaxe = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    pickaxe.setDamageValue(100);
    AbstractContainerMenu menu = new AbstractContainerMenu(null, -1) {
      @Override
      public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int index) {
        return ItemStack.EMPTY;
      }

      @Override
      public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
      }
    };
    TransientCraftingContainer grid = new TransientCraftingContainer(menu, 2, 2);
    grid.setItem(0, pickaxe);
    grid.setItem(1, kit);
    var recipes = level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);
    CraftingRecipe repair = null;
    for (CraftingRecipe candidate : recipes) {
      if (candidate instanceof CraftingTableRepairKitRecipe && candidate.matches(grid, level)) {
        repair = candidate;
        break;
      }
    }
    if (repair == null) {
      fail(failures, "the crafting table did not match the repair kit recipe");
      return;
    }
    ItemStack result = repair.assemble(grid, level.registryAccess());
    if (result.isEmpty() || result.getDamageValue() >= 100) {
      fail(failures, "the repair kit did not repair the pickaxe (damage " + result.getDamageValue() + ")");
      return;
    }
    TConstruct.LOG.info("[selftest] repair kit brought the iron pickaxe damage from 100 to {}", result.getDamageValue());
  }

  /** repairs a damaged iron pickaxe with iron ingots in the tinker station */
  private static void checkStationRepair(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(20, 100, 8);
    level.setBlockAndUpdate(pos, TinkerTables.tinkerStation.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof TinkerStationBlockEntity station)) {
      fail(failures, "the tinker station did not create a block entity for repair");
      return;
    }
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack pickaxe = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    pickaxe.setDamageValue(100);
    station.setItem(0, pickaxe);
    station.setItem(1, new ItemStack(Items.IRON_INGOT, 4));
    ItemStack result = station.calcResult(null);
    if (result.isEmpty()) {
      fail(failures, "the tinker station did not repair a damaged iron pickaxe");
      return;
    }
    int damage = result.getDamageValue();
    if (damage >= 100) {
      fail(failures, "repair reduced no damage (still " + damage + ")");
      return;
    }
    TConstruct.LOG.info("[selftest] station repair brought the iron pickaxe damage from 100 to {}", damage);
  }

  /** applies the diamond upgrade recipe to an iron pickaxe through the tinker station */
  /** the same modifier recipe path has to work on armor, which uses defense and upgrade slots */
  private static void checkArmorModifier(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(18, 100, 8);
    level.setBlockAndUpdate(pos, TinkerTables.tinkerStation.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof TinkerStationBlockEntity station)) {
      fail(failures, "the tinker station did not create a block entity for the armor check");
      return;
    }
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack helmet = ToolBuildHandler.buildItemFromMaterials(TinkerTools.plateArmor.get(ArmorItem.Type.HELMET), MaterialNBT.of(iron));
    if (helmet.isEmpty()) {
      fail(failures, "the iron plate helmet could not be built for the armor modifier check");
      return;
    }
    float before = ToolStack.from(helmet).getStats().get(ToolStats.ARMOR);
    station.setItem(0, helmet);
    station.setItem(1, new ItemStack(Items.DIAMOND, 1));
    ItemStack result = station.calcResult(null);
    if (result.isEmpty()) {
      fail(failures, "the tinker station did not apply the diamond upgrade to an iron plate helmet");
      return;
    }
    ToolStack tool = ToolStack.from(result);
    if (tool.getModifiers().getEntry(new ModifierId("tconstruct:diamond")) == ModifierEntry.EMPTY) {
      fail(failures, "the armored tinker station result is missing the diamond modifier");
      return;
    }
    expect(failures, "armor upgrade recipe consumed an upgrade slot", tool.getFreeSlots(SlotType.UPGRADE), 1);
    // the diamond module adds 1.0 armor unconditionally and 250 durability to armor (500 then -250)
    expect(failures, "armor upgrade armor stat", (int) (tool.getStats().get(ToolStats.ARMOR) * 10), (int) (before * 10) + 10);
    expect(failures, "armor upgrade durability", tool.getStats().getInt(ToolStats.DURABILITY), 415);
    TConstruct.LOG.info("[selftest] modifier recipe applied diamond to an iron plate helmet: armor {} free upgrades {}",
      tool.getStats().get(ToolStats.ARMOR), tool.getFreeSlots(SlotType.UPGRADE));
  }

  private static void checkModifierApplication(MinecraftServer server, List<String> failures) {
    ServerLevel level = server.overworld();
    BlockPos pos = new BlockPos(16, 100, 8);
    level.setBlockAndUpdate(pos, TinkerTables.tinkerStation.get().defaultBlockState());
    if (!(level.getBlockEntity(pos) instanceof TinkerStationBlockEntity station)) {
      fail(failures, "the tinker station did not create a block entity");
      return;
    }
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack pickaxe = ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron));
    station.setItem(0, pickaxe);
    station.setItem(1, new ItemStack(Items.DIAMOND, 1));
    ItemStack result = station.calcResult(null);
    if (result.isEmpty()) {
      fail(failures, "the tinker station did not apply the diamond upgrade to an iron pickaxe");
      return;
    }
    ToolStack tool = ToolStack.from(result);
    if (tool.getModifiers().getEntry(new ModifierId("tconstruct:diamond")) == ModifierEntry.EMPTY) {
      fail(failures, "the tinker station result is missing the diamond modifier");
      return;
    }
    expect(failures, "upgrade recipe consumed an upgrade slot", tool.getFreeSlots(SlotType.UPGRADE), 2);
    expect(failures, "diamond upgrade durability", tool.getStats().getInt(ToolStats.DURABILITY), 825);
    TConstruct.LOG.info("[selftest] modifier recipe applied diamond to an iron pickaxe: durability {} free upgrades {}",
      tool.getStats().getInt(ToolStats.DURABILITY), tool.getFreeSlots(SlotType.UPGRADE));
  }

  /** checks that modifier slots come from the tool definition modules and that recipe style deductions work */
  private static void checkToolSlots(List<String> failures) {
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ToolStack tool = ToolStack.from(ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron)));
    expect(failures, "iron pickaxe upgrade slots", tool.getFreeSlots(SlotType.UPGRADE), 3);
    expect(failures, "iron pickaxe ability slots", tool.getFreeSlots(SlotType.ABILITY), 1);
    // modifier recipes deduct their declared cost from the persistent slot data, this mirrors ModifierRecipe#applyModifiers
    tool.getPersistentData().addSlots(SlotType.UPGRADE, -1);
    expect(failures, "iron pickaxe upgrade slots after one recipe cost", tool.getFreeSlots(SlotType.UPGRADE), 2);
    tool.addModifier(new ModifierId("tconstruct:haste"), 1);
    tool.rebuildStats();
    // rebuilding stats keeps the deducted slot count, the deducted value is persistent tool data
    expect(failures, "iron pickaxe upgrade slots after rebuild", tool.getFreeSlots(SlotType.UPGRADE), 2);
    TConstruct.LOG.info("[selftest] iron pickaxe slots: upgrades={} abilities={} (deducted cost survives rebuildStats)",
      tool.getFreeSlots(SlotType.UPGRADE), tool.getFreeSlots(SlotType.ABILITY));
  }

  /** fills a seared tank item and drains it into an empty bucket, the same path the tank item click handler uses */
  private static void checkTankItemTransfer(List<String> failures) {
    ItemStack tankItem = new ItemStack(TinkerSmeltery.searedTank.get(SearedTankBlock.TankType.FUEL_TANK));
    ItemStack filled = TankItem.setTank(tankItem, new FluidStack(TinkerFluids.moltenIron.get(), 1000));
    if (!(filled.getItem() instanceof TankItem item)) {
      fail(failures, "the seared tank item could not be filled");
      return;
    }
    FluidTank tank = item.getTank(filled);
    expect(failures, "tank item fluid amount", (int) tank.getFluidAmount(), 1000);
    TransferResult result = FluidTransferHelper.interactWithStack(tank, new ItemStack(Items.BUCKET), TransferDirection.REVERSE);
    if (result == null || result.stack().isEmpty()) {
      fail(failures, "an empty bucket could not take molten iron out of a tank item");
      return;
    }
    ItemStack bucket = result.stack();
    if (!(bucket.getItem() instanceof BucketItem)) {
      fail(failures, "the tank item transfer produced " + bucket.getItem() + " instead of a bucket");
      return;
    }
    expect(failures, "tank item fluid after transfer", (int) tank.getFluidAmount(), 0);
    TConstruct.LOG.info("[selftest] tank item transfer: 1000 mB molten iron -> {}", bucket.getItem());
  }

  /** builds plate armor from plating parts and checks the plating stats pipeline */
  private static void checkArmor(List<String> failures) {
    IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
    ItemStack helmet = ToolBuildHandler.buildItemFromMaterials(TinkerTools.plateArmor.get(ArmorItem.Type.HELMET), MaterialNBT.of(iron, iron));
    if (helmet.isEmpty()) {
      fail(failures, "the iron plate helmet could not be built");
      return;
    }
    ToolStack tool = ToolStack.from(helmet);
    expectFloat(failures, "iron plate helmet armor", tool.getStats().get(ToolStats.ARMOR), 2f);
    expect(failures, "iron plate helmet durability", tool.getStats().getInt(ToolStats.DURABILITY), 165);
    expectFloat(failures, "iron plate helmet toughness", tool.getStats().get(ToolStats.ARMOR_TOUGHNESS), 0f);
    TConstruct.LOG.info("[selftest] iron plate helmet: armor={} durability={} toughness={}",
      tool.getStats().get(ToolStats.ARMOR), tool.getStats().getInt(ToolStats.DURABILITY), tool.getStats().get(ToolStats.ARMOR_TOUGHNESS));
  }

  /** formats a stat delta as a signed hundredths value */
  private static String speedDelta(float before, float after) {
    return String.format(java.util.Locale.ROOT, "%+.2f", after - before);
  }

  /** adds a spread of modifiers that exercise the different module types to a fresh pickaxe */
  private static void checkModifierModules(List<String> failures) {
    String[] ids = {"tconstruct:haste", "tconstruct:diamond", "tconstruct:reinforced", "tconstruct:sharpness",
                    "tconstruct:luck", "tconstruct:lightspeed", "tconstruct:reach", "tconstruct:knockback"};
    for (String name : ids) {
      ModifierId id = new ModifierId(name);
      if (!ModifierManager.INSTANCE.contains(id)) {
        // missing from the registry means the data pack did not provide it, which would be a load failure
        fail(failures, "the modifier " + name + " was not loaded");
        continue;
      }
      IMaterial iron = MaterialRegistry.getMaterial(new MaterialId("tconstruct:iron"));
      ToolStack tool = ToolStack.from(ToolBuildHandler.buildItemFromMaterials(TinkerTools.pickaxe.get(), MaterialNBT.of(iron, iron, iron)));
      int before = tool.getStats().getInt(ToolStats.DURABILITY);
      float beforeSpeed = tool.getStats().get(ToolStats.MINING_SPEED);
      float beforeAttack = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
      tool.addModifier(id, 1);
      tool.rebuildStats();
      int after = tool.getStats().getInt(ToolStats.DURABILITY);
      float afterSpeed = tool.getStats().get(ToolStats.MINING_SPEED);
      float afterAttack = tool.getStats().get(ToolStats.ATTACK_DAMAGE);
      if (tool.getModifiers().getEntry(id) == ModifierEntry.EMPTY) {
        fail(failures, "adding the modifier " + name + " did not stick to the tool");
      } else if (before == after) {
        TConstruct.LOG.info("[selftest] modifier {} applied, durability {} speed {} attack {} (module has no base stat change)",
          name, before, speedDelta(beforeSpeed, afterSpeed), speedDelta(beforeAttack, afterAttack));
      } else {
        TConstruct.LOG.info("[selftest] modifier {} applied, durability {} -> {}, speed {} attack {}",
          name, before, after, speedDelta(beforeSpeed, afterSpeed), speedDelta(beforeAttack, afterAttack));
      }
    }
  }

  /** verifies the fabric biome modifications that replace the Forge biome modifier JSON files */
  private static void checkBiomeModifications(MinecraftServer server, List<String> failures) {
    Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
    boolean cobalt = false;
    boolean skyGeode = false;
    for (ResourceKey<Biome> key : biomes.registryKeySet()) {
      Biome biome = biomes.getOrThrow(key);
      Holder<Biome> holder = biomes.wrapAsHolder(biome);
      if (holder.is(BiomeTags.IS_NETHER)) {
        cobalt |= hasFeature(biome, TinkerWorld.placedSmallCobaltOre);
      }
      if (holder.is(BiomeTags.IS_OVERWORLD)) {
        skyGeode |= hasFeature(biome, TinkerWorld.placedSkyGeode);
      }
    }
    if (cobalt) {
      TConstruct.LOG.info("[selftest] cobalt ore was added to the nether biomes");
    } else {
      fail(failures, "no nether biome received the cobalt ore feature");
    }
    if (skyGeode) {
      TConstruct.LOG.info("[selftest] sky geodes were added to the overworld biomes");
    } else {
      fail(failures, "no overworld biome received the sky geode feature");
    }
  }

  /** checks whether a biome has the given placed feature in its generation steps */
  private static boolean hasFeature(Biome biome, ResourceKey<PlacedFeature> key) {
    for (HolderSet<PlacedFeature> step : biome.getGenerationSettings().features()) {
      for (Holder<PlacedFeature> feature : step) {
        if (feature.is(key)) {
          return true;
        }
      }
    }
    return false;
  }

  private static void count(List<String> failures, String name, int value) {    TConstruct.LOG.info("[selftest] {}: {}", name, value);
    if (value <= 0) {
      fail(failures, name + " did not load");
    }
  }

  private static void expect(List<String> failures, String name, int actual, int expected) {
    if (actual != expected) {
      fail(failures, name + " is " + actual + " instead of " + expected);
    }
  }

  private static void expectFloat(List<String> failures, String name, float actual, float expected) {
    if (Math.abs(actual - expected) > 1.0E-4f) {
      fail(failures, name + " is " + actual + " instead of " + expected);
    }
  }

  private static void fail(List<String> failures, String message) {
    failures.add(message);
  }
}
