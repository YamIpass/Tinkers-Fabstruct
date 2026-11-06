package slimeknights.tconstruct.library.client.model.block;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.geometry.extensions.BlockModelExtensions;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedContext;
import slimeknights.mantle.client.model.data.IModelData;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.ColoredBlockModel.ColorData;
import slimeknights.mantle.client.model.util.DynamicBakedWrapper;
import slimeknights.mantle.client.model.util.MantleBakedModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.util.JsonHelper;
import slimeknights.mantle.util.LogicHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.model.ModelProperties;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model that replaces fluid textures with the fluid from model data
 */
@SuppressWarnings("WeakerAccess")
public class FluidTextureModel implements IUnbakedGeometry<FluidTextureModel> {
  /** Loader instance */
  public static final IGeometryLoader<FluidTextureModel> LOADER = FluidTextureModel::deserialize;

  private final ColoredBlockModel model;
  private final Set<String> fluids;
  private final Set<String> retextured;

  public FluidTextureModel(ColoredBlockModel model, Set<String> fluids, Set<String> retextured) {
    this.model = model;
    this.fluids = fluids;
    this.retextured = retextured;
  }

  @Override
  public void resolveParents(Function<ResourceLocation,UnbakedModel> modelGetter, BlockModel context) {
    model.resolveParents(modelGetter, context);
  }

  /** Trims the # character off the beginning of a texture name (if present) */
  private static String trimTextureName(String name) {
    if (name.charAt(0) == '#') {
      return name.substring(1);
    }
    return name;
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material,TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation modelLocation, boolean isGui3d) {
    // start by baking the model, handing UV lock
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides, modelLocation, isGui3d);

    // determine which block parts are fluids
    Set<String> fluidTextures = this.fluids.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(owner, model, this.fluids);
    List<BlockElement> elements = model.getElements();
    int size = elements.size();
    BitSet fluidParts = new BitSet(size);
    if (!fluidTextures.isEmpty()) {
      for (int i = 0; i < size; i++) {
        BlockElement part = elements.get(i);
        long fluidFaces = part.faces.values().stream()
                                    .filter(face -> fluidTextures.contains(trimTextureName(face.texture)))
                                    .count();
        // for simplicity, each part is either a fluid or not. If for some reason it contains both we mark it as a fluid, meaning it may get colored
        // if this is undesired, just use separate elements
        if (fluidFaces > 0) {
          if (fluidFaces < part.faces.size()) {
            TConstruct.LOG.warn("Mixed fluid and non-fluid elements in model {}, may cause unexpected results", modelLocation);
          }
          fluidParts.set(i);
        }
      }
    }
    Set<String> retextured = this.retextured.isEmpty() ? Collections.emptySet() : RetexturedModel.getAllRetextured(owner, this.model, this.retextured);
    return new Baked(baked, elements, model.getColorData(), owner, transform, fluidTextures, fluidParts, retextured);
  }

  private record BakedCacheKey(FluidStack fluid, @Nullable ResourceLocation texture) {}

  /** Baked wrapper class */
  private static class Baked extends DynamicBakedWrapper<BakedModel> {
    private final Map<BakedCacheKey,BakedModel> cache = new ConcurrentHashMap<>();
    private final List<BlockElement> elements;
    private final List<ColorData> colorData;
    private final BlockModel owner;
    private final ModelState transform;
    private final Set<String> fluids;
    private final BitSet fluidParts;
    private final Set<String> retextured;
    private final ItemOverrides overrides = new RetexturedOverride();

    protected Baked(BakedModel originalModel, List<BlockElement> elements, List<ColorData> colorData, BlockModel owner, ModelState transform, Set<String> fluids, BitSet fluidParts, Set<String> retextured) {
      super(originalModel);
      this.elements = elements;
      this.colorData = colorData;
      this.owner = owner;
      this.transform = transform;
      this.fluids = fluids;
      this.fluidParts = fluidParts;
      this.retextured = retextured;
    }

    /** Retextures a model for the given fluid */
    private BakedModel getRetexturedModel(BakedCacheKey key) {
      // setup model baking
      Function<Material,TextureAtlasSprite> spriteGetter = Material::sprite;

      // if textured, retexture. Its fine to nest these configurations
      BlockModel textured = this.owner;
      if (key.texture != null) {
        textured = new RetexturedContext(textured, this.retextured, key.texture);
      }

      // setup transformers, quadTransformer will be applied to all parts while fluid also adds in colors for the fluid
      RenderContext.QuadTransform quadTransformer = SimpleBlockModel.applyTransform(transform, ((BlockModelExtensions) owner).getRootTransform());
      RenderContext.QuadTransform fluidTransformer = quadTransformer;

      // get fluid details if needed
      int luminosity = 0;
      if (!key.fluid.isEmpty()) {
        int color = FluidVariantRendering.getColor(key.fluid.getType());
        if (color != -1) {
          fluidTransformer = ColoredBlockModel.mergeTransform(ColoredBlockModel.applyColorQuadTransformer(color), quadTransformer);
        }
        luminosity = FluidVariantAttributes.getLuminance(key.fluid.getType());
        textured = new RetexturedContext(textured, this.fluids, FluidVariantRendering.getSprite(key.fluid.getType()).contents().name());
      }

      // start baking
      TextureAtlasSprite particle = spriteGetter.apply(textured.getMaterial("particle"));
      MantleBakedModel.Builder builder = SimpleBlockModel.bakedBuilder(owner, ItemOverrides.EMPTY).particle(particle);

      boolean defaultUvLock = transform.isUvLocked();
      int size = elements.size();
      for (int i = 0; i < size; i++) {
        BlockElement element = elements.get(i);
        ColorData colors = LogicHelper.getOrDefault(colorData, i, ColorData.DEFAULT);
        if (fluidParts.get(i)) {
          ColoredBlockModel.bakePart(builder, textured, element, luminosity, spriteGetter, transform.getRotation(), fluidTransformer, colors.isUvLock(defaultUvLock), TankModel.BAKE_LOCATION);
        } else {
          int partColor = colors.color();
          RenderContext.QuadTransform partTransformer = partColor == -1 ? quadTransformer : ColoredBlockModel.mergeTransform(ColoredBlockModel.applyColorQuadTransformer(partColor), quadTransformer);
          ColoredBlockModel.bakePart(builder, textured, element, colors.luminosity(), spriteGetter, transform.getRotation(), partTransformer, colors.isUvLock(defaultUvLock), TankModel.BAKE_LOCATION);
        }
      }
      return builder.build();
    }

    /** Gets a retextured model for the given fluid, using the cached model if possible */
    private BakedModel getCachedModel(BakedCacheKey key) {
      return this.cache.computeIfAbsent(key, this::getRetexturedModel);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      IModelData data = blockView instanceof RenderAttachedBlockView attached ? attached.getBlockEntityRenderAttachment(pos) instanceof IModelData modelData ? modelData : null : null;
      if (data != null) {
        FluidStack fluid = fluids.isEmpty() ? FluidStack.EMPTY : data.has(ModelProperties.FLUID_STACK) ? data.get(ModelProperties.FLUID_STACK) : FluidStack.EMPTY;
        if (fluid == null) {
          fluid = FluidStack.EMPTY;
        }
        Block block = retextured.isEmpty() || !data.has(RetexturedHelper.BLOCK_PROPERTY) ? null : data.get(RetexturedHelper.BLOCK_PROPERTY);
        if (!fluid.isEmpty() || block != null) {
          BakedCacheKey key = new BakedCacheKey(fluid, block != null ? ModelHelper.getParticleTexture(block) : null);
          ((FabricBakedModel) getCachedModel(key)).emitBlockQuads(blockView, state, pos, randomSupplier, context);
          return;
        }
      }
      super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    @Override
    public ItemOverrides getOverrides() {
      return overrides;
    }

    /** Override list to swap the texture in from NBT */
    private class RetexturedOverride extends ItemOverrides {
      @Nullable
      @Override
      public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int pSeed) {
        if (stack.isEmpty() || !stack.hasTag()) {
          return originalModel;
        }

        // get the block first, ensuring its valid
        Block block = RetexturedHelper.getTexture(stack);
        if (block == Blocks.AIR) {
          return originalModel;
        }

        // if valid, use the block
        return getCachedModel(new BakedCacheKey(FluidStack.EMPTY, ModelHelper.getParticleTexture(block)));
      }
    }
  }

  /** Deserializes this model from JSON */
  public static FluidTextureModel deserialize(JsonObject json, JsonDeserializationContext context) {
    ColoredBlockModel model = ColoredBlockModel.deserialize(json, context);
    Set<String> fluids = Collections.emptySet();
    if (json.has("fluids")) {
      fluids = ImmutableSet.copyOf(JsonHelper.parseList(json, "fluids", GsonHelper::convertToString));
    }
    Set<String> retextured = Collections.emptySet();
    if (json.has("retextured")) {
      retextured = ImmutableSet.copyOf(JsonHelper.parseList(json, "retextured", GsonHelper::convertToString));
    }
    return new FluidTextureModel(model, fluids, retextured);
  }
}
