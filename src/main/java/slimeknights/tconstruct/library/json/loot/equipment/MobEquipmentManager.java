package slimeknights.tconstruct.library.json.loot.equipment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.data.loadable.field.ContextKey;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.typed.TypedMapBuilder;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.utils.ConditionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Loads the list of mob equipment replacements from JSON */
public class MobEquipmentManager extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
  public static final String FOLDER = "tinkering/mob_equipment";
  /** Singleton instance of the manager */
  private static final MobEquipmentManager INSTANCE = new MobEquipmentManager();
  /** Loadable for the list of entities */
  private static final Loadable<List<EntityType<?>>> ENTITY_LIST = Loadables.ENTITY_TYPE.list(1);

  /** Map of active replacements */
  private Map<EntityType<?>,List<MobEquipment>> replacements = Map.of();

  private MobEquipmentManager() {
    super(JsonHelper.DEFAULT_GSON, FOLDER);
  }

  /** @apiNote no need for addons to call this */
  @Internal
  public static void init() {
    // TODO(fabric): Forge registered this on AddReloadListenerEvent, and hooked MobSpawnEvent.FinalizeSpawn on the event bus.
    // The reload listener is registered below; the spawn hook needs a mixin on Mob#finalizeSpawn calling applyFinalizeSpawn,
    // which lives outside this package, so mob equipment is loaded but currently never applied.
    ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(INSTANCE);
  }

  @Override
  public ResourceLocation getFabricId() {
    return TConstruct.getResource("mob_equipment");
  }

  @Override
  protected void apply(Map<ResourceLocation, JsonElement> jsons, ResourceManager manager, ProfilerFiller profiler) {
    long time = System.nanoTime();
    int loaded = 0;

    // location of the objects only matters for debug, just parse each one
    Map<EntityType<?>, List<MobEquipment>> parsed = new HashMap<>();
    Function<EntityType<?>, List<MobEquipment>> ifAbsent = type -> new ArrayList<>();
    for (Entry<ResourceLocation,JsonElement> entry : jsons.entrySet()) {
      ResourceLocation key = entry.getKey();
      try {
        JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), key.toString());
        // skip if conditions fail
        // TODO(fabric): Forge evaluated the "conditions" field with the load context, the loader port evaluates it in ConditionHelper
        if (!ConditionHelper.testConditions(json)) {
          continue;
        }
        // parse the object
        List<MobEquipment> equipment = MobEquipment.LIST_LOADABLE.getIfPresent(json, "equip", TypedMapBuilder.builder().put(ContextKey.ID, key).put(ContextKey.DEBUG, "Mob Equipment " + key).build());

        // determine the entities
        JsonElement entityElement = JsonHelper.getElement(json, "entity");
        // primitive is either single value or tag
        if (entityElement.isJsonPrimitive()) {
          String type = entityElement.getAsString();
          // starting with # is a tag
          if (type.charAt(0) == '#') {
            // TODO(fabric): Forge used the load context to fetch tag values as they are not yet in the manager
            TagKey<EntityType<?>> tag = Loadables.ENTITY_TYPE_TAG.parseString(type.substring(1), "entity");
            for (Holder<EntityType<?>> holder : BuiltInRegistries.ENTITY_TYPE.getTagOrEmpty(tag)) {
              parsed.computeIfAbsent(holder.value(), ifAbsent).addAll(equipment);
            }
          } else {
            parsed.computeIfAbsent(Loadables.ENTITY_TYPE.parseString(type, "entity"), ifAbsent).addAll(equipment);
          }
        } else if (entityElement.isJsonArray()) {
          // if its an array, assume an array of entitiy type names. No support for tags in the array
          for (EntityType<?> type : ENTITY_LIST.convert(entityElement, "entity")) {
            parsed.computeIfAbsent(type, ifAbsent).addAll(equipment);
          }
        } else {
          throw new JsonSyntaxException("Expected entity to be either a string or an array");
        }

        // add the value to the map
        loaded++;
      } catch (Exception e) {
        TConstruct.LOG.error("Failed to replacement from {}", key, e);
      }
    }

    // build the final map
    this.replacements = parsed.entrySet().stream()
      .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().stream().sorted(Comparator.comparing(MobEquipment::priority).reversed()).toList()));

    TConstruct.LOG.info("Loaded {} mob equipment replacements targeting {} mobs in {} ms", loaded, replacements.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Gets the equipment for the given entity */
  public List<MobEquipment> get(EntityType<?> type) {
    return replacements.getOrDefault(type, List.of());
  }


  /* Events */

  /** Hook to run during mob spawn finalization, before vanilla applies its equipment
   * @return true if equipment was applied, the caller should skip vanilla finalization */
  public static boolean applyFinalizeSpawn(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag spawnTag) {
    List<MobEquipment> equipment = INSTANCE.get(mob.getType());
    return !equipment.isEmpty() && MobEquipment.apply(equipment, mob, level, spawnType, spawnData, spawnTag);
  }
}
