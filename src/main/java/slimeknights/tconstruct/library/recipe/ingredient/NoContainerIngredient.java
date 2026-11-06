package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.JsonUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/** Ingredient matching an item with no container item, used to ensure NBT fluid items are empty */
public class NoContainerIngredient extends NestedIngredient implements CustomIngredient {
  public static final ResourceLocation ID = TConstruct.getResource("no_container");

  protected NoContainerIngredient(Ingredient nested) {
    super(nested);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    // TODO(fabric): Forge's ItemStack#getCraftingRemainingItem() is stack aware (NBT dependent); Fabric only exposes the item level check
    return stack != null && super.test(stack) && !stack.getItem().hasCraftingRemainingItem();
  }

  @Override
  public JsonElement toJson() {
    JsonElement nestedElement = nested.toJson();
    // if we are a vanilla ingredient, and not an array ingredient, serialize into the ingredient directly
    if (nested.getCustomIngredient() == null && nestedElement.isJsonObject()) {
      JsonObject nestedObject = nestedElement.getAsJsonObject();
      nestedObject.addProperty("fabric:type", ID.toString());
      return nestedObject;
    }
    // if we have an array or a type, then serialize nested
    JsonObject json = JsonUtils.withLocation("fabric:type", ID);
    json.add("match", nestedElement);
    return json;
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

  /** Serializer instance */
  public enum Serializer implements CustomIngredientSerializer<NoContainerIngredient> {
    INSTANCE;

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public NoContainerIngredient read(JsonObject json) {
      // if we have match, parse as a nested object. Without match, just parse the object as vanilla
      Ingredient ingredient;
      if (json.has("match")) {
        ingredient = CraftingHelper.getIngredient(json.get("match"));
      } else {
        json.remove("fabric:type");
        ingredient = Ingredient.fromJson(json);
      }
      return new NoContainerIngredient(ingredient);
    }

    @Override
    public void write(JsonObject json, NoContainerIngredient ingredient) {
      json.addProperty("fabric:type", ID.toString());
      json.add("match", ingredient.nested.toJson());
    }

    @Override
    public NoContainerIngredient read(FriendlyByteBuf buffer) {
      return new NoContainerIngredient(Ingredient.fromNetwork(buffer));
    }

    @Override
    public void write(FriendlyByteBuf buffer, NoContainerIngredient ingredient) {
      ingredient.nested.toNetwork(buffer);
    }
  }


  /* Static constructors */

  /** Creates an instance from the given nested ingredient */
  public static NoContainerIngredient of(Ingredient ingredient) {
    return new NoContainerIngredient(ingredient);
  }

  /** Creates an instance from the given items */
  public static NoContainerIngredient of(ItemLike... items) {
    return of(Ingredient.of(items));
  }

  /** Creates an instance from the given stacks */
  public static NoContainerIngredient of(ItemStack... stacks) {
    return of(Ingredient.of(stacks));
  }

  /** Creates an instance from the given tag */
  public static NoContainerIngredient of(TagKey<Item> tag) {
    return of(Ingredient.of(tag));
  }

  /**
   * Returns this ingredient instead of letting Fabric wrap it.
   * <p>
   * TODO(fabric): the default implementation wraps us in a {@code CustomIngredientImpl}, which tests through
   * {@code CustomIngredient#test}. That is a Fabric API method and keeps its name, while our override of the vanilla
   * {@code Ingredient#test} is remapped to the intermediary name, so the wrapper would throw {@code AbstractMethodError}.
   */
  
  public Ingredient toVanilla() {
    return this;
  }

}
