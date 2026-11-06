package slimeknights.tconstruct.library.client.armor;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRenderer;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRendererRegistry;
import io.github.fabricators_of_create.porting_lib.util.client.ClientHooks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.armor.texture.ArmorTextureSupplier;
import slimeknights.tconstruct.tools.client.material.CombatFishingHookRenderer;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

public class ArmorModelManager extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
  /** Folder containing the logic */
  public static final String FOLDER = "tinkering/armor_models";

  /** Object representing parsed models */
  public record ArmorModel(List<ArmorTextureSupplier> layers) {
    /** Empty instace for fallback */
    public static final ArmorModel EMPTY = new ArmorModel(List.of());
    /** Loadable for JSON parsing */
    public static final RecordLoadable<ArmorModel> LOADABLE = RecordLoadable.create(ArmorTextureSupplier.LOADER.list(1).requiredField("layers", ArmorModel::layers), ArmorModel::new);
  }

  /* Instance data */
  public static final ArmorModelManager INSTANCE = new ArmorModelManager();
  /** Map of location to texture suppliers */
  private Map<ResourceLocation,ArmorModel> models = Collections.emptyMap();

  private static final List<ArmorModelDispatcher> DISPATCHERS = new ArrayList<>();

  /**
   * Initializes this manager, registering it with the resource manager
   * @param manager  Manager
   */
  private static boolean registered = false;

  public static void init(ResourceManagerHelper manager) {
    // several client bootstrap paths run during startup (the shared entrypoint and the module client events), keep
    // the registration idempotent so fabric does not log a duplicate listener id
    if (registered) {
      return;
    }
    registered = true;

    manager.registerReloadListener(INSTANCE);
  }

  @Override
  public ResourceLocation getFabricId() {
    return TConstruct.getResource("armor_models");
  }

  private ArmorModelManager() {
    super(JsonHelper.DEFAULT_GSON, FOLDER);
  }

  @Override
  protected void apply(Map<ResourceLocation,JsonElement> splashList, ResourceManager manager, ProfilerFiller pProfiler) {
    long time = System.nanoTime();

    // first, load in all fluid textures, means we are allowed to reference them in fluid texture supplier constructors
    ArmorTextureSupplier.TEXTURE_VALIDATOR.onReloadSafe(manager);
    // reuses armor model logic for simplicity
    CombatFishingHookRenderer.clearCache();

    // load all models
    ImmutableMap.Builder<ResourceLocation,ArmorModel> builder = ImmutableMap.builder();
    for (Entry<ResourceLocation,JsonElement> entry : splashList.entrySet()) {
      ResourceLocation key = entry.getKey();
      JsonElement element = entry.getValue();
      try {
        builder.put(key, ArmorModel.LOADABLE.convert(element, key.toString()));
      } catch (JsonSyntaxException e) {
        TConstruct.LOG.error("Failed to load armor model from {}", key, e);
      }
    }

    this.models = builder.build();
    // clear dispatcher model cache
    Set<ResourceLocation> missing = new HashSet<>();
    for (ArmorModelDispatcher dispatcher : DISPATCHERS) {
      dispatcher.model = null;
      ResourceLocation name = dispatcher.getName();
      if (!this.models.containsKey(name)) {
        missing.add(name);
      }
    }
    if (!missing.isEmpty()) {
      TConstruct.LOG.error("Missing armor models used by items: {}", missing);
    }
    TConstruct.LOG.info("Loaded {} armor models in {} ms", models.size(), (System.nanoTime() - time) / 1000000f);
  }

  /** Gets the armor model for the given location. Location typically corresponds to armor material name */
  public ArmorModel getModel(ResourceLocation name) {
    return models.getOrDefault(name, ArmorModel.EMPTY);
  }

  /** Helper to cache armor models in the item */
  public abstract static class ArmorModelDispatcher implements ArmorRenderer {
    private ArmorModel model;

    public ArmorModelDispatcher() {
      DISPATCHERS.add(this);
    }

    /**
     * Gets the name of the model to use.
     * Not a constructor parameter as the client renderer is created before we can store fields from the parent constructor.
     */
    protected abstract ResourceLocation getName();

    /** Fetches the model from the cache */
    protected ArmorModel getModel(ItemStack stack) {
      if (model == null) {
        model = ArmorModelManager.INSTANCE.getModel(getName());
        if (model == ArmorModel.EMPTY) {
          TConstruct.LOG.warn("Failed to find armor model {}, will skip rendering {}", getName(), stack);
        }
      }
      return model;
    }

    /** Gets the model to render for the given armor stack */
    @Nonnull
    public Model getGenericArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
      return MultilayerArmorModel.INSTANCE.setup(living, stack, slot, original, getModel(stack));
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource bufferSource, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> playerModel, HumanoidModel<LivingEntity> baseArmorModel) {
      playerModel.copyPropertiesTo(baseArmorModel);
      ClientHooks.setPartVisibility(baseArmorModel, slot);
      Model armorModel = getGenericArmorModel(entity, stack, slot, baseArmorModel);
      // TODO(fabric): Forge filled AbstractArmorModel.buffer from RenderLivingEvent.Pre/Post, the Porting Lib armor renderer passes the buffer source directly
      AbstractArmorModel.buffer = bufferSource;
      try {
        // the buffer argument is unused by the multilayer model, it renders into the static buffer source above
        armorModel.renderToBuffer(matrices, null, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
      } finally {
        AbstractArmorModel.buffer = null;
      }
    }
  }

  /** Registers the given armor renderer for the given item, the Fabric replacement for Forge's item client extensions */
  public static void registerRenderer(Item item, ArmorModelDispatcher dispatcher) {
    ArmorRendererRegistry.register(dispatcher, item);
  }
}
