package slimeknights.tconstruct.library.json.condition;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import lombok.RequiredArgsConstructor;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.RegistryHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.shared.TinkerCommons;

/** @deprecated use {@link slimeknights.mantle.recipe.condition.TagFilledCondition} */
@Deprecated(forRemoval = true)
@RequiredArgsConstructor
public class TagNotEmptyCondition<T> implements LootItemCondition, ConditionJsonProvider {
  private static final ResourceLocation NAME = TConstruct.getResource("tag_not_empty");
  private final TagKey<T> tag;

  @SuppressWarnings("removal")
  @Override
  public LootItemConditionType getType() {
    return TinkerCommons.lootTagNotEmptyCondition.get();
  }

  @Override
  public ResourceLocation getConditionId() {
    return NAME;
  }

  // TODO(fabric): Forge evaluated this condition while loading recipes via ICondition.IContext; the matching Fabric
  //  resource condition is not registered yet, so this logic is currently unused (see shared/TinkerCommons)
  public boolean test() {
    return RegistryHelper.getTagStream(tag).findAny().isPresent();
  }

  @Override
  public void writeParameters(JsonObject json) {
    json.addProperty("registry", tag.registry().location().toString());
    json.addProperty("tag", tag.location().toString());
  }

  @Override
  public boolean test(LootContext context) {
    Registry<T> registry = RegistryHelper.getRegistry(tag.registry());
    return registry != null && registry.getTagOrEmpty(tag).iterator().hasNext();
  }

  public static class ConditionSerializer implements Serializer<TagNotEmptyCondition<?>> {
    /** Helper to deal with generics */
    private static <T> TagKey<T> createKey(JsonObject json) {
      ResourceKey<? extends Registry<T>> registry = ResourceKey.createRegistryKey(JsonHelper.getResourceLocation(json, "registry"));
      return TagKey.create(registry, JsonHelper.getResourceLocation(json, "tag"));
    }

    /** Writes the condition parameters, shared with the resource condition */
    public void write(JsonObject json, TagNotEmptyCondition<?> value) {
      value.writeParameters(json);
    }

    @Override
    public void serialize(JsonObject json, TagNotEmptyCondition<?> value, JsonSerializationContext context) {
      write(json, value);
    }

    public TagNotEmptyCondition<?> read(JsonObject json) {
      return new TagNotEmptyCondition<>(createKey(json));
    }

    @Override
    public TagNotEmptyCondition<?> deserialize(JsonObject json, JsonDeserializationContext context) {
      return read(json);
    }
  }
}
