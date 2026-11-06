package slimeknights.tconstruct.library.recipe.casting.container;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.primitive.IntLoadable;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.IMultiRecipe;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.mantle.recipe.helper.TypeAwareRecipeSerializer;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.recipe.casting.DisplayCastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.ICastingContainer;
import slimeknights.tconstruct.library.recipe.casting.ICastingRecipe;
import slimeknights.tconstruct.library.recipe.casting.IDisplayableCastingRecipe;

import java.util.List;
import java.util.Optional;

/**
 * Casting recipe that takes an arbitrary fluid for a given amount and fills a container
 */
@RequiredArgsConstructor
public class ContainerFillingRecipe implements ICastingRecipe, IMultiRecipe<IDisplayableCastingRecipe> {
  public static final RecordLoadable<ContainerFillingRecipe> LOADER = RecordLoadable.create(
    LoadableRecipeSerializer.TYPED_SERIALIZER.requiredField(), ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP,
    IntLoadable.FROM_ONE.requiredField("fluid_amount", r -> r.fluidAmount),
    Loadables.ITEM.requiredField("container", r -> r.container),
    ContainerFillingRecipe::new);

  @Getter
  private final TypeAwareRecipeSerializer<?> serializer;
  @Getter
  private final ResourceLocation id;
  @Getter
  private final String group;
  private final int fluidAmount;
  private final Item container;

  @Override
  public RecipeType<?> getType() {
    return serializer.getType();
  }

  /** Gets the fluid handler for the given stack, if any */
  private static Optional<Storage<FluidVariant>> getFluidHandlerItem(ItemStack stack) {
    return Optional.ofNullable(FluidStorage.ITEM.find(stack, ContainerItemContext.withInitial(stack)));
  }

  @Override
  public int getFluidAmount(ICastingContainer inv) {
    FluidVariant fluid = FluidVariant.of(inv.getFluid());
    try (Transaction tx = TransferUtil.getTransaction()) {
      return getFluidHandlerItem(inv.getStack())
        .map(handler -> (int) StorageUtil.simulateInsert(handler, fluid, this.fluidAmount, tx))
        .orElse(0);
    }
  }

  @Override
  public boolean isConsumed() {
    return true;
  }

  @Override
  public boolean switchSlots() {
    return false;
  }

  @Override
  public int getCoolingTime(ICastingContainer inv) {
    return 5;
  }

  @Override
  public boolean matches(ICastingContainer inv, Level worldIn) {
    ItemStack stack = inv.getStack();
    FluidVariant fluid = FluidVariant.of(inv.getFluid());
    try (Transaction tx = TransferUtil.getTransaction()) {
      return stack.getItem() == this.container.asItem()
             && getFluidHandlerItem(stack)
                   .filter(handler -> StorageUtil.simulateInsert(handler, fluid, this.fluidAmount, tx) > 0)
                   .isPresent();
    }
  }

  /** @deprecated use {@link ICastingRecipe#assemble(Container, RegistryAccess)} */
  @Override
  @Deprecated
  public ItemStack getResultItem(RegistryAccess access) {
    return new ItemStack(this.container);
  }

  @Override
  public ItemStack assemble(ICastingContainer inv, RegistryAccess access) {
    ItemStack stack = inv.getStack().copy();
    ContainerItemContext context = ContainerItemContext.withInitial(stack);
    return Optional.ofNullable(FluidStorage.ITEM.find(stack, context)).map(handler -> {
      try (Transaction tx = TransferUtil.getTransaction()) {
        handler.insert(FluidVariant.of(inv.getFluid(), inv.getFluidTag()), this.fluidAmount, tx);
        tx.commit();
      }
      return context.getItemVariant().toStack((int) context.getAmount());
    }).orElse(stack);
  }

  /* Display */
  /** Cache of items to display for this container */
  private List<IDisplayableCastingRecipe> displayRecipes = null;

  @Override
  public List<IDisplayableCastingRecipe> getRecipes(RegistryAccess access) {
    if (displayRecipes == null) {
      // filter fluid registry to just fluids we care about
      List<FluidStack> fluids = BuiltInRegistries.FLUID.stream()
        .filter(fluid -> {
          // skip flowing fluids (redundant to source), fluids with no bucket (probably internal), and fluids hidden from recipe viewers (they be hidden)
          if (fluid.isSource(fluid.defaultFluidState()) && !fluid.is(TinkerTags.Fluids.HIDE_IN_CREATIVE_TANKS)) {
            try {
              Item bucket = fluid.getBucket();
              return bucket != Items.AIR && !bucket.builtInRegistryHolder().is(TinkerTags.Items.HIDDEN_IN_RECIPE_VIEWERS);
            } catch (Exception e) {
              // Registrate (popular dependency for making registration easier) is broken and throws in getBucket for fluids with no bucket
              // we could just skip the bucket check, but its just going to throw when we try to fill an empty bucket in map below
            }
          }
          return false;
        })
        .map(fluid -> new FluidStack(fluid, fluidAmount))
        .toList();
      // fill the container with each fluid stack
      List<ItemStack> results = fluids.stream().map(fluid -> {
        ItemStack stack = new ItemStack(container);
        ContainerItemContext context = ContainerItemContext.withInitial(stack);
        return Optional.ofNullable(FluidStorage.ITEM.find(stack, context)).map(handler -> {
          try (Transaction tx = TransferUtil.getTransaction()) {
            handler.insert(fluid.getType(), fluid.getAmount(), tx);
            tx.commit();
          }
          return context.getItemVariant().toStack((int) context.getAmount());
        }).orElse(stack);
      }).toList();
      displayRecipes = List.of(DisplayCastingRecipe.from(this)
        // cast is just an empty container, which is "consumed"
        .cast(new ItemStack(container)).consumed()
        .fluids(fluids).results(results).linkFluidsToOutput()
        .coolingTime(5)
        .build());
    }
    return displayRecipes;
  }
}
