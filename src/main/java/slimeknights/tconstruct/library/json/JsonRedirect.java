package slimeknights.tconstruct.library.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Data;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.library.utils.ConditionHelper;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/** Represents a redirect in a material or modifier JSON */
@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
public class JsonRedirect {
  private final ResourceLocation id;
  /** TODO(fabric): was a Forge {@code ICondition}; kept as raw JSON as Fabric has no Forge condition equivalent */
  @Nullable
  private final JsonObject condition;

  /**
   * Gets the predicate testing this redirect's condition
   * @return  Predicate, or null if this redirect has no condition
   */
  @Nullable
  public Predicate<JsonObject> getConditionPredicate() {
    JsonObject condition = this.condition;
    if (condition == null) {
      return null;
    }
    // TODO(fabric): Forge conditions are evaluated by our own helper as Mantle's conditions are not registered with Fabric
    return json -> ConditionHelper.testCondition(condition);
  }

  /** Serializes this to JSON */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.addProperty("id", id.toString());
    JsonElement condition = this.condition;
    if (condition != null) {
      json.add("condition", condition);
    }
    return json;
  }

  /** Deserializes this to JSON */
  public static JsonRedirect fromJson(JsonObject json) {
    ResourceLocation id = JsonHelper.getResourceLocation(json, "id");
    JsonObject condition = null;
    if (json.has("condition") && json.get("condition").isJsonObject()) {
      condition = json.getAsJsonObject("condition");
    }
    return new JsonRedirect(id, condition);
  }
}
