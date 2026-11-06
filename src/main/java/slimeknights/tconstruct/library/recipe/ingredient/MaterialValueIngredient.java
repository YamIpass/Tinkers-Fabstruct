package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import slimeknights.mantle.data.loadable.field.LoadableField;
import slimeknights.mantle.data.predicate.IJsonPredicate;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicate;
import slimeknights.tconstruct.library.json.predicate.material.MaterialPredicateField;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipe;
import slimeknights.tconstruct.library.recipe.material.MaterialRecipeCache;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ingredient matching material items with the given value. Typically, matches ingots or blocks
 */
@Getter
public class MaterialValueIngredient extends Ingredient implements CustomIngredient {
  private final IJsonPredicate<MaterialVariantId> material;
  private final float minValue;
  private final float maxValue;
  private ItemStack[] items;

  public MaterialValueIngredient(IJsonPredicate<MaterialVariantId> material, float minValue, float maxValue) {
    super(Stream.empty());
    this.material = material;
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  /** Creates an ingredient matching a range of values */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float minValue, float maxValue) {
    return new MaterialValueIngredient(materials, minValue, maxValue);
  }

  /** Creates an ingredient matching an exact value */
  public static MaterialValueIngredient of(IJsonPredicate<MaterialVariantId> materials, float value) {
    return of(materials, value, value);
  }

  /** Checks the given material recipe against our filters */
  public boolean test(MaterialRecipe material) {
    float value = material.getValue() / (float) material.getNeeded();
    return minValue <= value && value <= maxValue && this.material.matches(material.getMaterial().getVariant());
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null) {
      return false;
    }
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe);
  }

  @Override
  public ItemStack[] getItems() {
    // TODO(fabric): Forge invalidated this cache on tag reload; Fabric has no equivalent hook
    if (items == null) {
      items = MaterialRecipeCache.getSortedRecipes().stream()
        .filter(this::test)
        .flatMap(material -> Arrays.stream(material.getIngredient().getItems()))
        .toArray(ItemStack[]::new);
    }
    return items;
  }


  /* Helpers for ShapedMaterialRecipe */

  /** Checks if this ingredient fully contains the range of the other */
  private boolean contains(MaterialValueIngredient other) {
    return this.minValue <= other.minValue && other.maxValue <= this.maxValue;
  }

  /** Creates an ingredient that matches anything either of the two ingredients matches */
  public MaterialValueIngredient merge(MaterialValueIngredient other) {
    if (this == other) return this;

    // if we have the same predicate, we can possibly skip creating a new instance
    IJsonPredicate<MaterialVariantId> predicate = this.material;
    if (this.material.equals(other.material)) {
      if (this.contains(other)) {
        return this;
      }
      if (other.contains(this)) {
        return other;
      }
    } else {
      predicate = MaterialPredicate.or(this.material, other.material);
    }
    return new MaterialValueIngredient(predicate, Math.min(this.minValue, other.minValue), Math.max(this.maxValue, other.maxValue));
  }

  /** Gets the material matching this recipe */
  @Nullable
  public MaterialVariantId getMaterial(ItemStack stack) {
    MaterialRecipe recipe = MaterialRecipeCache.findRecipe(stack);
    return recipe != MaterialRecipe.EMPTY && test(recipe) ? recipe.getMaterial().getVariant() : null;
  }


  /* Fabric custom ingredient */

  @Override
  public CustomIngredient getCustomIngredient() {
    return this;
  }

  @Override
  public List<ItemStack> getMatchingStacks() {
    return Arrays.asList(getItems());
  }

  @Override
  public boolean requiresTesting() {
    return true;
  }

  @Override
  public CustomIngredientSerializer<?> getSerializer() {
    return Serializer.INSTANCE;
  }


  /* JSON */

  @Override
  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("fabric:type", Serializer.ID.toString());
    Serializer.MATERIAL_FIELD.serialize(this, json);
    if (minValue == maxValue) {
      json.addProperty("value", minValue);
    } else {
      JsonObject value = new JsonObject();
      if (minValue > 0) {
        value.addProperty("min", minValue);
      }
      if (Float.isFinite(maxValue)) {
        value.addProperty("max", maxValue);
      }
      json.add("value", value);
    }
    return json;
  }


  /** Serializer instance */
  public enum Serializer implements CustomIngredientSerializer<MaterialValueIngredient> {
    INSTANCE;
    public static final ResourceLocation ID = TConstruct.getResource("material_value");
    private static final LoadableField<IJsonPredicate<MaterialVariantId>, MaterialValueIngredient> MATERIAL_FIELD = new MaterialPredicateField<>("material", i -> i.material);

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public MaterialValueIngredient read(JsonObject json) {
      float minValue, maxValue;
      JsonElement value = json.get("value");
      if (value.isJsonPrimitive()) {
        minValue = maxValue = value.getAsJsonPrimitive().getAsFloat();
      } else {
        JsonObject object = GsonHelper.convertToJsonObject(value, "value");
        minValue = GsonHelper.getAsFloat(object, "min", 0);
        maxValue = GsonHelper.getAsFloat(object, "max", Float.POSITIVE_INFINITY);
      }
      return new MaterialValueIngredient(MATERIAL_FIELD.get(json), minValue, maxValue);
    }

    @Override
    public void write(JsonObject json, MaterialValueIngredient ingredient) {
      json.addProperty("fabric:type", ID.toString());
      MATERIAL_FIELD.serialize(ingredient, json);
      if (ingredient.minValue == ingredient.maxValue) {
        json.addProperty("value", ingredient.minValue);
      } else {
        JsonObject value = new JsonObject();
        if (ingredient.minValue > 0) {
          value.addProperty("min", ingredient.minValue);
        }
        if (Float.isFinite(ingredient.maxValue)) {
          value.addProperty("max", ingredient.maxValue);
        }
        json.add("value", value);
      }
    }

    @Override
    public MaterialValueIngredient read(FriendlyByteBuf buffer) {
      return new MaterialValueIngredient(
        MATERIAL_FIELD.decode(buffer),
        buffer.readFloat(),
        buffer.readFloat()
      );
    }

    @Override
    public void write(FriendlyByteBuf buffer, MaterialValueIngredient ingredient) {
      MATERIAL_FIELD.encode(buffer, ingredient);
      buffer.writeFloat(ingredient.minValue);
      buffer.writeFloat(ingredient.maxValue);
    }
  }

  /**
   * Returns this ingredient instead of letting Fabric wrap it.
   * <p>
   * TODO(fabric): the default implementation wraps us in a {@code CustomIngredientImpl}, which tests through
   * {@code CustomIngredient#test}. That is a Fabric API method and keeps its name, while our override of the vanilla
   * {@code Ingredient#test} is remapped to the intermediary name, so the wrapper would throw {@code AbstractMethodError}.
   */
  @Override
  public Ingredient toVanilla() {
    return this;
  }

}
