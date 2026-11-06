package slimeknights.tconstruct.library.materials.json;

import com.google.gson.JsonObject;
import lombok.Data;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.ApiStatus.Internal;
import slimeknights.tconstruct.library.json.JsonRedirect;

import javax.annotation.Nullable;

@SuppressWarnings("ClassCanBeRecord") // GSON does not support records
@Data
@Internal
public class MaterialJson {
  /** TODO(fabric): was a Forge {@code ICondition}; kept as a raw JSON object as Fabric has no Forge condition equivalent */
  @Nullable
  private final JsonObject condition;
  @Nullable
  private final Boolean craftable;
  @Nullable
  private final Integer tier;
  @Nullable
  private final Integer sortOrder;
  @Nullable
  private final Rarity rarity;
  @Nullable
  private final Boolean hidden;
  @Nullable
  private final JsonRedirect[] redirect;
}
