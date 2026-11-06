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

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagCombinationCondition#intersection(TagKey[])} */
@Deprecated(forRemoval = true)
public class TagIntersectionPresentCondition<T> implements ConditionJsonProvider {
  private static final ResourceLocation NAME = TConstruct.getResource("tag_intersection_present");
  public static final Serializer SERIALIZER = new Serializer();

  private final List<TagKey<T>> names;

  public TagIntersectionPresentCondition(List<TagKey<T>> names) {
    if (names.isEmpty()) {
      throw new IllegalArgumentException("Cannot create a condition with no names");
    }
    this.names = names;
  }

  /** Creates a condition from a set of keys */
  @SafeVarargs
  public static <T> TagIntersectionPresentCondition<T> ofKeys(TagKey<T>... names) {
    return new TagIntersectionPresentCondition<>(Arrays.asList(names));
  }

  /** Creates a condition from a registry and a set of names */
  public static <T> TagIntersectionPresentCondition<T> ofNames(ResourceKey<? extends Registry<T>> registry, ResourceLocation... names) {
    return new TagIntersectionPresentCondition<>(Arrays.stream(names).map(name -> TagKey.create(registry, name)).toList());
  }

  @Override
  public ResourceLocation getConditionId() {
    return NAME;
  }

  // TODO(fabric): Forge evaluated this condition while loading recipes via ICondition.IContext; the matching Fabric
  //  resource condition is not registered yet, so this logic is currently unused (see shared/TinkerCommons)
  public boolean test() {
    // if there is just one tag, just needs to be filled
    List<List<Holder<T>>> tags = names.stream().map(name -> RegistryHelper.getTagStream(name).toList()).toList();
    if (tags.size() == 1) {
      return !tags.get(0).isEmpty();
    }
    // if any remaining tag is empty, give up
    int count = tags.size();
    for (int i = 1; i < count; i++) {
      if (tags.get(i).isEmpty()) {
        return false;
      }
    }

    // all tags have something, so find the first item that is in all tags
    itemLoop:
    for (Holder<T> entry : tags.get(0)) {
      // find the first item contained in all other intersection tags
      for (int i = 1; i < count; i++) {
        if (!tags.get(i).contains(entry)) {
          continue itemLoop;
        }
      }
      // all tags contain the item? success
      return true;
    }
    // no item in all tags
    return false;
  }

  @Override
  public void writeParameters(JsonObject json) {
    Serializer.serialize(json, this);
  }

  private static class Serializer {
    private static void serialize(JsonObject json, TagIntersectionPresentCondition<?> value) {
      JsonArray names = new JsonArray();
      json.addProperty("registry", value.names.get(0).registry().location().toString());
      for (TagKey<?> name : value.names) {
        names.add(name.location().toString());
      }
      json.add("tags", names);
    }

    /** Reads with generics happy */
    private static <T> TagIntersectionPresentCondition<T> readGeneric(JsonObject json) {
      ResourceKey<Registry<T>> registry = ResourceKey.createRegistryKey(JsonHelper.getResourceLocation(json, "registry"));
      return new TagIntersectionPresentCondition<>(JsonHelper.parseList(json, "tags", (element, s) -> TagKey.create(registry, JsonHelper.convertToResourceLocation(element, s))));
    }

    private static TagIntersectionPresentCondition<?> read(JsonObject json) {
      return readGeneric(json);
    }
  }
}
