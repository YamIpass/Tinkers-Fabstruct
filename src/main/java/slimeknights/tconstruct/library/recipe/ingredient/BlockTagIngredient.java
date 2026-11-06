package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Item ingredient matching items with a block form in the given tag */
public class BlockTagIngredient extends Ingredient implements CustomIngredient {
  private final TagKey<Block> tag;
  @Nullable
  private Set<Item> matchingItems;
  @Nullable
  private ItemStack[] items;
  @Nullable
  private IntList stackingIds;

  public BlockTagIngredient(TagKey<Block> tag) {
    super(Stream.empty());
    this.tag = tag;
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    return stack != null && getMatchingItems().contains(stack.getItem());
  }

  /** Gets the ordered matching items set */
  @SuppressWarnings("deprecation")
  private Set<Item> getMatchingItems() {
    // TODO(fabric): Forge invalidated this cache on tag reload; Fabric has no equivalent hook
    if (matchingItems == null) {
      matchingItems = RegistryHelper.getTagValueStream(BuiltInRegistries.BLOCK, tag)
                                    .map(Block::asItem)
                                    .filter(item -> item != Items.AIR)
                                    .collect(Collectors.toCollection(LinkedHashSet::new));
    }
    return matchingItems;
  }

  @Override
  public ItemStack[] getItems() {
    if (items == null) {
      items = getMatchingItems().stream().map(ItemStack::new).toArray(ItemStack[]::new);
    }
    return items;
  }

  @SuppressWarnings("deprecation")
  @Override
  public IntList getStackingIds() {
    if (stackingIds == null) {
      Set<Item> items = getMatchingItems();
      stackingIds = new IntArrayList(items.size());
      for (Item item : items) {
        stackingIds.add(BuiltInRegistries.ITEM.getId(item));
      }
      stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
    }
    return stackingIds;
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

  @Override
  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("fabric:type", Serializer.ID.toString());
    json.add("tag", Loadables.BLOCK_TAG.serialize(tag));
    return json;
  }

  /** Serializer instance */
  public enum Serializer implements CustomIngredientSerializer<BlockTagIngredient> {
    INSTANCE;

    public static final ResourceLocation ID = TConstruct.getResource("block_tag");

    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public BlockTagIngredient read(JsonObject json) {
      return new BlockTagIngredient(Loadables.BLOCK_TAG.getIfPresent(json, "tag"));
    }

    @Override
    public void write(JsonObject json, BlockTagIngredient ingredient) {
      json.addProperty("fabric:type", ID.toString());
      json.add("tag", Loadables.BLOCK_TAG.serialize(ingredient.tag));
    }

    @Override
    public void write(FriendlyByteBuf buffer, BlockTagIngredient ingredient) {
      // TODO(fabric): Forge synced just the resolved item list here (the client has no block tags in Forge); Fabric syncs block tags,
      //  so we sync the tag itself which resolves to the exact same item set on the client
      buffer.writeResourceLocation(ingredient.tag.location());
    }

    @Override
    public BlockTagIngredient read(FriendlyByteBuf buffer) {
      return new BlockTagIngredient(TagKey.create(BuiltInRegistries.BLOCK.key(), buffer.readResourceLocation()));
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
