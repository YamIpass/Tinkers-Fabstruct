package slimeknights.tconstruct.library.recipe.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredient;
import net.fabricmc.fabric.api.recipe.v1.ingredient.CustomIngredientSerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.json.TinkerLoadables;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** Ingredient matching an {@link net.minecraft.world.item.InstrumentItem} with a particular instrument. */
public class InstrumentIngredient extends Ingredient implements CustomIngredient {
  private static final String TAG_INSTRUMENT = "instrument";
  public static final ResourceLocation ID = TConstruct.getResource("instrument");

  private final Item item;
  @Nullable
  private final ResourceKey<Instrument> instrument;
  @Nullable
  private final TagKey<Instrument> ignore;
  protected InstrumentIngredient(Item item, @Nullable ResourceKey<Instrument> instrument, @Nullable TagKey<Instrument> ignore) {
    super(Stream.of(new InstrumentValue(item, instrument)));
    this.item = item.asItem();
    this.instrument = instrument;
    this.ignore = ignore;
  }

  /** Creates a new instance matching the given instrument */
  public static InstrumentIngredient of(ItemLike item, ResourceKey<Instrument> instrument) {
    return new InstrumentIngredient(item.asItem(), instrument, null);
  }

  /** Creates a new instance ignoring the given tag */
  public static InstrumentIngredient of(ItemLike item, TagKey<Instrument> ignore) {
    return new InstrumentIngredient(item.asItem(), null, ignore);
  }

  @Override
  public boolean test(@Nullable ItemStack stack) {
    if (stack == null || !stack.is(item)) {
      return false;
    }
    CompoundTag tag = stack.getTag();
    if (tag != null) {
      ResourceLocation key = ResourceLocation.tryParse(tag.getString(TAG_INSTRUMENT));
      if (key != null) {
        // if we just want the instrument to match, can just compare by ID over doing a fetch
        if (this.instrument != null) {
          return this.instrument.location().equals(key);
        }
        assert this.ignore != null;
        // must valid and not in the tag
        Optional<Holder.Reference<Instrument>> instrument = BuiltInRegistries.INSTRUMENT.getHolder(ResourceKey.create(Registries.INSTRUMENT, key));
        return instrument.isPresent() && !instrument.get().is(ignore);
      }
    }
    // if no instrument, its fine as long as we don't have a specific instrument
    return this.instrument == null;
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
    return SERIALIZER;
  }

  @Override
  public JsonElement toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("fabric:type", ID.toString());
    json.addProperty("item", Loadables.ITEM.getString(item));
    if (instrument != null) {
      json.addProperty("instrument", instrument.location().toString());
    }
    if (ignore != null) {
      json.addProperty("ignore", ignore.location().toString());
    }
    return json;
  }

  private record InstrumentValue(Item item, @Nullable ResourceKey<Instrument> instrument) implements Value {
    @Override
    public Collection<ItemStack> getItems() {
      ItemStack stack = new ItemStack(item);
      // set the instrument on the stack
      if (instrument != null) {
        stack.getOrCreateTag().putString(TAG_INSTRUMENT, instrument.location().toString());
      }
      return List.of(stack);
    }

    @Override
    public JsonObject serialize() {
      JsonObject json = new JsonObject();
      json.addProperty("item", Loadables.ITEM.getString(item));
      if (instrument != null) {
        json.addProperty("instrument", instrument.location().toString());
      }
      return json;
    }
  }

  /** Serializer instance */
  public static final CustomIngredientSerializer<InstrumentIngredient> SERIALIZER = new CustomIngredientSerializer<>() {
    @Override
    public ResourceLocation getIdentifier() {
      return ID;
    }

    @Override
    public InstrumentIngredient read(JsonObject json) {
      Item item = Loadables.ITEM.getIfPresent(json, "item");
      ResourceKey<Instrument> instrument = null;
      TagKey<Instrument> ignore = null;
      if (json.has("instrument")) {
        instrument = ResourceKey.create(Registries.INSTRUMENT, JsonHelper.getResourceLocation(json, "instrument"));
      } else if (json.has("ignore")) {
        ignore = TinkerLoadables.INSTRUMENT_TAGS.getIfPresent(json, "ignore");
      } else {
        throw new JsonSyntaxException("Invalid InstrumentIngredient: must set either 'instrument' or 'ignore'");
      }
      return new InstrumentIngredient(item, instrument, ignore);
    }

    @Override
    public void write(JsonObject json, InstrumentIngredient ingredient) {
      json.addProperty("fabric:type", ID.toString());
      json.addProperty("item", Loadables.ITEM.getString(ingredient.item));
      if (ingredient.instrument != null) {
        json.addProperty("instrument", ingredient.instrument.location().toString());
      } else if (ingredient.ignore != null) {
        json.addProperty("ignore", ingredient.ignore.location().toString());
      }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void write(FriendlyByteBuf buffer, InstrumentIngredient ingredient) {
      buffer.writeId(BuiltInRegistries.ITEM, ingredient.item);
      if (ingredient.instrument != null) {
        buffer.writeBoolean(true);
        buffer.writeResourceLocation(ingredient.instrument.location());
      } else {
        assert ingredient.ignore != null;
        buffer.writeBoolean(false);
        buffer.writeResourceLocation(ingredient.ignore.location());
      }
    }

    @SuppressWarnings("deprecation")
    @Override
    public InstrumentIngredient read(FriendlyByteBuf buffer) {
      Item item = buffer.readById(BuiltInRegistries.ITEM);
      // swap missing items for barriers
      if (item == null) {
        item = Items.BARRIER;
      }
      ResourceKey<Instrument> instrument = null;
      TagKey<Instrument> ignore = null;
      if (buffer.readBoolean()) {
        instrument = ResourceKey.create(Registries.INSTRUMENT, buffer.readResourceLocation());
      } else {
        ignore = TagKey.create(Registries.INSTRUMENT, buffer.readResourceLocation());
      }
      return new InstrumentIngredient(item, instrument, ignore);
    }
  };

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
