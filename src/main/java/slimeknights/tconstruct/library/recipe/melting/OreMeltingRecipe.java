package slimeknights.tconstruct.library.recipe.melting;

import com.google.common.collect.Streams;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.transfer.TransferUtil;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.recipe.helper.FluidOutput;
import slimeknights.mantle.recipe.helper.LoadableRecipeSerializer;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.json.TinkerLoadables;
import slimeknights.tconstruct.library.json.field.MergingListField;
import slimeknights.tconstruct.library.recipe.melting.IMeltingContainer.OreRateType;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Extension of melting recipe to boost results of ores
 */
public class OreMeltingRecipe extends MeltingRecipe {
  public static final RecordLoadable<OreMeltingRecipe> LOADER = RecordLoadable.create(
    ContextKey.ID.requiredField(), LoadableRecipeSerializer.RECIPE_GROUP, INPUT, OUTPUT, TEMPERATURE, TIME, BYPRODUCTS,
    TinkerLoadables.ORE_RATE_TYPE.requiredField("rate", OreMeltingRecipe::getOreType),
    new MergingListField<>(TinkerLoadables.ORE_RATE_TYPE.defaultField("rate", OreRateType.DEFAULT, Function.identity()), "byproducts", r -> r.byproductTypes),
    OreMeltingRecipe::new);

  @Getter
  private final OreRateType oreType;
  private final List<OreRateType> byproductTypes;
  protected OreMeltingRecipe(ResourceLocation id, String group, Ingredient input, FluidOutput output, int temperature, int time, List<FluidOutput> byproducts, OreRateType oreType, List<OreRateType> byproductTypes) {
    super(id, group, input, output, temperature, time, byproducts);
    this.oreType = oreType;
    this.byproductTypes = byproductTypes;
  }

  @Override
  public FluidStack getOutput(IMeltingContainer inv) {
    return inv.getOreRate().applyOreBoost(oreType, output.get(), true);
  }

  @Override
  public void handleByproducts(IMeltingContainer inv, SlottedStorage<FluidVariant> handler) {
    // fill byproducts until we run out of space or byproducts
    for (int i = 0; i < byproducts.size(); i++) {
      TransferUtil.insertFluid(handler, Config.COMMON.foundryByproductRate.applyOreBoost(byproductTypes.get(i).orElse(oreType), byproducts.get(i).get(), true));
    }
  }

  @Override
  public List<List<FluidStack>> getOutputWithByproducts() {
    if (outputWithByproducts == null) {
      outputWithByproducts = Stream.concat(
        Stream.of(output).map(output -> Config.COMMON.foundryOreRate.applyOreBoost(oreType, output.get(), false)),
        Streams.zip(byproducts.stream(), byproductTypes.stream(), (byproduct, rate) -> Config.COMMON.foundryByproductRate.applyOreBoost(rate.orElse(oreType), byproduct.get(), false))
      ).map(List::of).toList();
    }
    return outputWithByproducts;
  }

  @Override
  public RecipeSerializer<?> getSerializer() {
    return TinkerSmeltery.oreMeltingSerializer.get();
  }
}
