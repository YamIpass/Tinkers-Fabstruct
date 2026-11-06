package slimeknights.tconstruct.fluids.fluids;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.tconstruct.fluids.TinkerFluids;

import java.util.Objects;

public class PotionFluidType extends FluidType {
  public PotionFluidType(Properties properties) {
    super(properties);
  }

  @Override
  public String getDescriptionId(FluidStack stack) {
    return PotionUtils.getPotion(stack.getTag()).getName("item.minecraft.potion.effect.");
  }

  @Override
  public ItemStack getBucket(FluidStack fluidStack) {
    ItemStack itemStack = new ItemStack(fluidStack.getFluid().getBucket());
    itemStack.setTag(fluidStack.getTag());
    return itemStack;
  }

  // TODO(fabric): Forge's FluidType#initializeClient (a tint color based on the potion effects) does not exist on
  //  Porting Lib. The equivalent is registering a FluidAttributes with getColor(FluidStack) overridden through
  //  slimeknights.mantle.registration.ClientFluidAttributeRegistry in FluidClientEvents; see PotionFluidAttributes in
  //  the 3.6.4 Fabric port (ref/Hephaestus-1.20.1) for that shape. Rendering only, no gameplay change.

  /** Creates the potion tag */
  private static CompoundTag potionTag(ResourceLocation location) {
    CompoundTag tag = new CompoundTag();
    tag.putString("Potion", location.toString());
    return tag;
  }

  /** Creates a fluid stack for the given potion */
  public static FluidStack potionFluid(ResourceKey<Potion> potion, int size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY_ID) {
      tag = potionTag(potion.location());
    }
    return new FluidStack(TinkerFluids.potion.get(), size, tag);
  }

  /** Creates a fluid stack for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidStack potionFluid(Potion potion, int size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY) {
      tag = potionTag(BuiltInRegistries.POTION.getKey(potion));
    }
    return new FluidStack(TinkerFluids.potion.get(), size, tag);
  }

  /** Creates a fluid output for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static FluidOutput potionResult(Potion potion, int size) {
    CompoundTag tag = null;
    if (potion != Potions.EMPTY) {
      tag = potionTag(BuiltInRegistries.POTION.getKey(potion));
    }
    return FluidOutput.fromTag(Objects.requireNonNull(TinkerFluids.potion.getCommonTag()), size, tag);
  }

  /** Creates a potion bucket for the given potion */
  public static ItemStack potionBucket(ResourceKey<Potion> potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    if (potion != Potions.EMPTY_ID) {
      stack.setTag(potionTag(potion.location()));
    }
    return stack;
  }

  /** Creates a potion bucket for the given potion */
  @SuppressWarnings("deprecation")  // forge registries have nullable keys, like why would you want that?
  public static ItemStack potionBucket(Potion potion) {
    ItemStack stack = new ItemStack(TinkerFluids.potion);
    if (potion != Potions.EMPTY) {
      stack.setTag(potionTag(BuiltInRegistries.POTION.getKey(potion)));
    }
    return stack;
  }
}
