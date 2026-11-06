package slimeknights.tconstruct.library.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.GsonHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Evaluates the condition JSON found in tinkers data files.
 * <p>
 * Forge evaluated these through its own condition registry. Fabric only knows about {@code fabric:} conditions, so the
 * Forge and Mantle types used by the 3.12.1 data are evaluated here instead. Conditions we cannot evaluate are treated as
 * passing (the same effect Forge had when the condition context was unavailable), except for tag checks which report
 * missing so that redirects fall back to their default material.
 */
public final class ConditionHelper {
  /** Conditions we already complained about, to keep the log from flooding */
  private static final Set<String> UNKNOWN = new HashSet<>();

  private ConditionHelper() {}

  /** Tests the "conditions" field of the given object, if present */
  public static boolean testConditions(JsonObject parent) {
    JsonElement element = parent.get(ResourceConditions.CONDITIONS_KEY);
    if (element == null) {
      element = parent.get("conditions");
    }
    return element == null || testConditions(element);
  }

  /** Tests a list or a single condition */
  public static boolean testConditions(JsonElement element) {
    if (element.isJsonArray()) {
      // a list is treated like "and", the same as Forge
      JsonArray array = element.getAsJsonArray();
      for (JsonElement child : array) {
        if (!testConditions(child)) {
          return false;
        }
      }
      return true;
    }
    return testCondition(element);
  }

  /** Tests a single condition object */
  public static boolean testCondition(@Nullable JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return true;
    }
    if (element.isJsonArray()) {
      return testConditions(element);
    }
    if (!element.isJsonObject()) {
      return true;
    }
    JsonObject json = element.getAsJsonObject();
    String type = GsonHelper.getAsString(json, "type", "");
    switch (type) {
      case "forge:mod_loaded":
      case "neoforge:mod_loaded":
      case "fabric:mod_loaded":
        return FabricLoader.getInstance().isModLoaded(GsonHelper.getAsString(json, "modid", ""));
      case "forge:item_exists": {
        ResourceLocation id = new ResourceLocation(GsonHelper.getAsString(json, "item"));
        return BuiltInRegistries.ITEM.containsKey(id);
      }
      case "forge:false":
      case "mantle:false":
        return false;
      case "forge:true":
      case "mantle:true":
        return true;
      case "forge:or":
      case "forge:and": {
        JsonArray values = GsonHelper.getAsJsonArray(json, "values");
        boolean or = type.endsWith("or");
        for (JsonElement value : values) {
          boolean result = testCondition(value);
          if (or && result) {
            return true;
          }
          if (!or && !result) {
            return false;
          }
        }
        return !or;
      }
      case "forge:not":
        return !testCondition(json.get("value"));
      case "forge:tag_filled":
      case "mantle:tag_filled":
        // TODO(fabric): tags are not bound while the reload listeners run, so report missing which makes redirects fall
        //  back to their default instead of redirecting to a material from an uninstalled mod
        return false;
      case "mantle:tag_empty":
        return true;
      default:
        break;
    }
    if (type.startsWith("fabric:")) {
      // let Fabric evaluate its own conditions
      JsonObject wrapper = new JsonObject();
      JsonArray conditions = new JsonArray();
      conditions.add(json);
      wrapper.add(ResourceConditions.CONDITIONS_KEY, conditions);
      try {
        return ResourceConditions.objectMatchesConditions(wrapper);
      } catch (RuntimeException e) {
        TConstruct.LOG.debug("Failed to evaluate condition {}", json, e);
        return false;
      }
    }
    if (!type.isEmpty() && UNKNOWN.add(type)) {
      TConstruct.LOG.debug("Treating unknown condition type {} as passing", type);
    }
    return true;
  }
}
