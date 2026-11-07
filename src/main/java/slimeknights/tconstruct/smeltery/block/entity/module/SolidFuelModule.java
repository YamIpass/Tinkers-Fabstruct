package slimeknights.tconstruct.smeltery.block.entity.module;

import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import io.github.fabricators_of_create.porting_lib.util.StorageProvider;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.block.entity.MantleBlockEntity;
import slimeknights.tconstruct.library.fluid.FluidStorages;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuel;
import slimeknights.tconstruct.library.recipe.fuel.MeltingFuelLookup;

/** Fuel module variant that supports both item and fluid fuels. Only supports a single fluid position which should not change. */
public class SolidFuelModule extends FuelModule {
  /** Location of the fuel tank */
  private final BlockPos fuelPos;

  public SolidFuelModule(MantleBlockEntity parent, BlockPos fuelPos) {
    super(parent);
    this.fuelPos = fuelPos;
  }

  /* Fuel updating */

  /**
   * Tries to consume fuel from the given item handler
   * @param handler  Handler to consume fuel from
   * @return   Temperature of the consumed fuel, 0 if none found
   */
  @Override
  protected int trySolidFuel(Storage<ItemVariant> handler, boolean consume) {
    for (StorageView<ItemVariant> view : handler.nonEmptyViews()) {
      ItemVariant resource = view.getResource();
      // TODO(fabric): Forge's getBurnTime(stack, FUEL recipe type) hook has no Fabric equivalent, FuelRegistry covers vanilla fuels only
      Integer burnTime = FuelRegistry.INSTANCE.get(resource.getItem());
      int time = burnTime == null ? 0 : burnTime / 4;
      if (time > 0) {
        MeltingFuel solid = MeltingFuelLookup.getSolid();
        if (consume) {
          try (Transaction tx = TransferUtil.getTransaction()) {
            long extracted = view.extract(resource, 1, tx);
            if (extracted == 1) {
              ItemStack extractedStack = resource.toStack(1);
              fuel += time;
              fuelQuality = time;
              temperature = solid.getTemperature();
              rate = solid.getRate();
              parent.setChangedFast();
              // return the container
              ItemStack container = extractedStack.getRecipeRemainder();
              if (!container.isEmpty()) {
                // if we cannot insert the container back, spit it on the ground
                long inserted = TransferUtil.insertItem(handler, container);
                if (inserted == 0) {
                  Level world = getLevel();
                  double x = (world.random.nextFloat() * 0.5F) + 0.25D;
                  double y = (world.random.nextFloat() * 0.5F) + 0.25D;
                  double z = (world.random.nextFloat() * 0.5F) + 0.25D;
                  ItemEntity itementity = new ItemEntity(world, fuelPos.getX() + x, fuelPos.getY() + y, fuelPos.getZ() + z, container);
                  itementity.setDefaultPickUpDelay();
                  world.addFreshEntity(itementity);
                }
              }
              tx.commit();
            } else {
              TConstruct.LOG.error("Invalid item removed from solid fuel handler");
            }
          }
        }
        return solid.getTemperature();
      }
    }
    return 0;
  }

  /** Fetches any relevant fuel handlers from the target position */
  private void fetchHandlers() {
    // if we have handlers, nothing to do
    if (fluidHandler != null && itemHandler != null) {
      return;
    }
    Level level = getLevel();
    BlockEntity te = level.getBlockEntity(fuelPos);
    if (te != null) {
      // first, identify a capability that has what we need
      // on the chance both are present, we prioritize fluid; we don't expect that to change
      if (FluidStorages.raw(level, fuelPos, null) != null) {
        fluidHandler = StorageProvider.createForFluids(level, fuelPos);
      }
      if (ItemStorage.SIDED.find(level, fuelPos, null) != null) {
        itemHandler = StorageProvider.createForItems(level, fuelPos);
      }
    } else {
      fluidHandler = null;
      itemHandler = null;
    }
  }

  @Override
  public int findFuel(boolean consume) {
    fetchHandlers();

    // prioritize liquid fuel - it usually goes hotter
    int temperature = 0;
    Storage<FluidVariant> fluidStorage = getFluidStorage();
    if (fluidStorage != null) {
      temperature = tryLiquidFuel(fluidStorage, consume);
    }
    // next, try solid fuel
    Storage<ItemVariant> itemStorage = getItemStorage();
    if (temperature == 0 && itemStorage != null) {
      temperature = trySolidFuel(itemStorage, consume);
    }
    // no handler found, tell client of the lack of fuel
    if (temperature == 0 && consume) {
      this.temperature = 0;
      this.rate = 0;
    }
    return temperature;
  }


  /* UI Syncing */

  @Override
  public FuelInfo getFuelInfo() {
    fetchHandlers();

    FuelInfo info = super.getFuelInfo();
    if (info.isEmpty() && itemHandler != null) {
      return FuelInfo.ITEM;
    }
    return info;
  }


  /* Fluid handler */

  /** Gets the fluid handler for proxy */
  @Override
  public Storage<FluidVariant> getTank() {
    Storage<FluidVariant> storage = getFluidStorage();
    return storage == null ? EMPTY_FLUID_STORAGE : storage;
  }
}
