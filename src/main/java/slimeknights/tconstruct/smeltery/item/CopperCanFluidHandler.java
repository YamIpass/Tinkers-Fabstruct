package slimeknights.tconstruct.smeltery.item;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantItemStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import slimeknights.tconstruct.library.recipe.FluidValues;

import javax.annotation.Nullable;
import java.util.Objects;

/** Capability handler instance for the copper can item */
@SuppressWarnings("UnstableApiUsage")
public class CopperCanFluidHandler extends SingleVariantItemStorage<FluidVariant> {
  @Getter
  private final ContainerItemContext container;

  public CopperCanFluidHandler(ContainerItemContext container) {
    super(container);
    this.container = container;
  }


  /* Tank properties */

  /** Gets the stack size sensitive capacity of the container */
  @Override
  public long getCapacity(FluidVariant variant) {
    // scale up by the stack size to prevent dupes with people trying to fill a stack of containers
    return (long)FluidValues.INGOT * container.getAmount();
  }

  /** Gets the contained fluid */
  private Fluid getFluid(ItemVariant variant) {
    return CopperCanItem.getFluid(variant.getNbt());
  }

  /** Gets the contained fluid */
  @Nullable
  private CompoundTag getFluidTag() {
    return CopperCanItem.getFluidTag(container.getItemVariant().getNbt());
  }

  @Override
  protected FluidVariant getBlankResource() {
    return FluidVariant.blank();
  }

  @Override
  protected FluidVariant getResource(ItemVariant currentVariant) {
    Fluid fluid = getFluid(currentVariant);
    if (fluid == Fluids.EMPTY) {
      return FluidVariant.blank();
    }
    return FluidVariant.of(fluid, getFluidTag());
  }

  @Override
  protected long getAmount(ItemVariant currentVariant) {
    return getFluid(currentVariant) == Fluids.EMPTY ? 0 : getCapacity(FluidVariant.blank());
  }


  /* Interaction */

  @Override
  public long insert(FluidVariant insertedResource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(insertedResource, maxAmount);
    long capacity = getCapacity(insertedResource);
    // must not be filled, must have enough
    if (getFluid(container.getItemVariant()) != Fluids.EMPTY || maxAmount < capacity) {
      return 0;
    }
    return super.insert(insertedResource, capacity, transaction);
  }

  @Override
  public long extract(FluidVariant extractedResource, long maxAmount, TransactionContext transaction) {
    StoragePreconditions.notBlankNotNegative(extractedResource, maxAmount);
    long capacity = getCapacity(extractedResource);
    // must be draining at least an ingot times the stack size
    if (maxAmount < capacity) {
      return 0;
    }

    // must have a fluid, must match what they are draining
    Fluid fluid = getFluid(container.getItemVariant());
    if (fluid == Fluids.EMPTY || fluid != extractedResource.getFluid()) {
      return 0;
    }
    // make sure NBT matches the requested NBT
    if (!Objects.equals(extractedResource.getNbt(), getFluidTag())) {
      return 0;
    }
    return super.extract(extractedResource, capacity, transaction);
  }

  @Override
  protected ItemVariant getUpdatedVariant(ItemVariant currentVariant, FluidVariant newResource, long newAmount) {
    FluidStack fluid = newAmount <= 0 ? FluidStack.EMPTY : new FluidStack(newResource.getFluid(), newAmount, newResource.copyNbt());
    return ItemVariant.of(CopperCanItem.setFluid(currentVariant.toStack(), fluid));
  }
}
