package slimeknights.tconstruct.library.client.materials;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import slimeknights.mantle.client.ResourceColorManager;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.utils.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import slimeknights.mantle.data.listener.IEarlySafeManagerReloadListener;
import slimeknights.tconstruct.TConstruct;

public class MaterialTooltipCache {
  /** Map of the key for each material variant */
  private static final Map<MaterialVariantId,String> KEY_CACHE = new HashMap<>();
  /** Map of the color for each material variant */
  private static final Map<MaterialVariantId,TextColor> COLOR_CACHE = new HashMap<>();
  /** Map of the key for each material variant */
  private static final Map<MaterialVariantId,Component> DISPLAY_NAME_CACHE = new HashMap<>();
  /** Map of the key for each material variant */
  private static final Map<MaterialVariantId,Component> COLORED_DISPLAY_NAME_CACHE = new HashMap<>();

  /** Clears all resource pack driven caches */
  private static final IEarlySafeManagerReloadListener RELOAD_LISTENER = new IEarlySafeManagerReloadListener() {
    @Override
    public void onReloadSafe(net.minecraft.server.packs.resources.ResourceManager manager) {
      COLOR_CACHE.clear();
      DISPLAY_NAME_CACHE.clear();
      COLORED_DISPLAY_NAME_CACHE.clear();
    }

    @Override
    public ResourceLocation getFabricId() {
      return TConstruct.getResource("tool_materials");
    }
  };

  private MaterialTooltipCache() {}

  /** Called during the event to initialize the cache invalidators */
  private static boolean registered = false;

  public static void init(ResourceManagerHelper manager)  {
    // several client bootstrap paths run during startup (the shared entrypoint and the module client events), keep
    // the registration idempotent so fabric does not log a duplicate listener id
    if (registered) {
      return;
    }
    registered = true;

    manager.registerReloadListener(RELOAD_LISTENER);
  }

  /** Logic to convert a material ID to a string */
  private static final Function<MaterialVariantId,String> KEY_GETTER = id -> Util.makeTranslationKey("material", id.getLocation('.'));

  /** Gets the key name for a given material variant, prevents need to create a lot of extra strings */
  public static String getKey(MaterialVariantId variantId) {
    return KEY_CACHE.computeIfAbsent(variantId, KEY_GETTER);
  }

  /** Gets the display name for a component */
  private static final Function<MaterialVariantId,MutableComponent> DISPLAY_NAME_GETTER = id -> {
    if (id.hasVariant()) {
      String variantKey = getKey(id);
      // TODO 1.21: ditch can translate and assume variants must always translate
      if (Util.canTranslate(variantKey)) {
        return Component.translatable(variantKey);
      }
    }
    return Component.translatable(getKey(id.getId()));
  };

  /** Gets the key name for a given material variant, prevents need to create a lot of extra strings */
  public static Component getDisplayName(MaterialVariantId variantId) {
    return DISPLAY_NAME_CACHE.computeIfAbsent(variantId, DISPLAY_NAME_GETTER);
  }

  /** Getter for colors given a material variant ID */
  public static final Function<MaterialVariantId,TextColor> COLOR_GETTER = id -> {
    if (id.hasVariant()) {
      String variantKey = getKey(id);
      TextColor color = ResourceColorManager.getOrNull(variantKey);
      if (color != null) {
        return color;
      }
    }
    return ResourceColorManager.getTextColor(getKey(id.getId()));
  };

  /** Gets the color for a given material ID */
  public static TextColor getColor(MaterialVariantId id) {
    return COLOR_CACHE.computeIfAbsent(id, COLOR_GETTER);
  }

  /** Gets the display name for a component */
  private static final Function<MaterialVariantId,Component> COLORED_DISPLAY_NAME_GETTER = id -> {
    TextColor color = getColor(id);
    if ((color.getValue() & 0xFFFFFF) != 0xFFFFFF) {
      return DISPLAY_NAME_GETTER.apply(id).withStyle(style -> style.withColor(color));
    }
    // no color? Use the already cached white component
    return getDisplayName(id);
  };

  /** Gets the key name for a given material variant, prevents need to create a lot of extra strings */
  public static Component getColoredDisplayName(MaterialVariantId variantId) {
    return COLORED_DISPLAY_NAME_CACHE.computeIfAbsent(variantId, COLORED_DISPLAY_NAME_GETTER);
  }
}
