package slimeknights.tconstruct.library.json.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagCombinationCondition#difference(TagKey, TagKey)} */
@Deprecated(forRemoval = true)
public class TagDifferencePresentCondition<T> implements ConditionJsonProvider {
  private static final ResourceLocation NAME = TConstruct.getResource("tag_difference_present");
  public static final Serializer SERIALIZER = new Serializer();

  private final TagKey<T> base;
  private final List<TagKey<T>> subtracted;

  public TagDifferencePresentCondition(TagKey<T> base, List<TagKey<T>> subtracted) {
    if (subtracted.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a condition with no subtracted");
    }
    this.base = base;
    this.subtracted = subtracted;
  }

  /** Creates a condition from a set of keys */
  @SafeVarargs
  public static <T> TagDifferencePresentCondition<T> ofKeys(TagKey<T> base, TagKey<T>... subtracted) {
    return new TagDifferencePresentCondition<>(base, Arrays.asList(subtracted));
  }

  /** Creates a condition from a registry and a set of names */
  public static <T> TagDifferencePresentCondition<T> ofNames(ResourceKey<? extends Registry<T>> registry, ResourceLocation base, ResourceLocation... subtracted) {
    TagKey<T> baseKey = TagKey.create(registry, base);
    return new TagDifferencePresentCondition<>(baseKey, Arrays.stream(subtracted).map(name -> TagKey.create(registry, name)).toList());
  }

  @Override
  public ResourceLocation getConditionId() {
    return NAME;
  }

  // TODO(fabric): Forge evaluated this condition while loading recipes via ICondition.IContext; the matching Fabric
  //  resource condition is not registered yet, so this logic is currently unused (see shared/TinkerCommons)
  public boolean test() {
    // get the base tag
    Collection<Holder<T>> base = RegistryHelper.getTagStream(this.base).toList();
    if (base == null || base.isEmpty()) {
      return false;
    }

    // no subtracted tags? we good
    if (subtracted.isEmpty()) {
      return true;
    }
    // all tags have something, so find the first item that is in all tags
    itemLoop:
    for (Holder<T> entry : base) {
      // find the first item contained in no subtracted tags
      for (TagKey<T> tag : subtracted) {
        if (RegistryHelper.getTagStream(tag).toList().contains(entry)) {
          continue itemLoop;
        }
      }
      // no subtracted contains the item? success
      return true;
    }
    // no item not in any subtracted
    return false;
  }

  @Override
  public void writeParameters(JsonObject json) {
    Serializer.serialize(json, this);
  }

  private static class Serializer {
    private static void serialize(JsonObject json, TagDifferencePresentCondition<?> value) {
      json.addProperty("registry", value.base.registry().location().toString());
      json.addProperty("base", value.base.location().toString());
      JsonArray names = new JsonArray();
      for (TagKey<?> name : value.subtracted) {
        names.add(name.location().toString());
      }
      json.add("subtracted", names);
    }

    private static <T> TagDifferencePresentCondition<T> readGeneric(JsonObject json) {
      ResourceKey<Registry<T>> registry = ResourceKey.createRegistryKey(JsonHelper.getResourceLocation(json, "registry"));
      return new TagDifferencePresentCondition<>(
        TagKey.create(registry, JsonHelper.getResourceLocation(json, "base")),
        JsonHelper.parseList(json, "subtracted", (e, s) -> TagKey.create(registry, JsonHelper.convertToResourceLocation(e, s))));
    }

    private static TagDifferencePresentCondition<?> read(JsonObject json) {
      return readGeneric(json);
    }
  }
}
