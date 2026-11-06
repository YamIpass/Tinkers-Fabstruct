package slimeknights.tconstruct.library.client.model.tools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import io.github.fabricators_of_create.porting_lib.models.CustomParticleIconModel;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.geometry.extensions.BlockModelExtensions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import slimeknights.mantle.Mantle;
import slimeknights.mantle.client.model.ModelProperty;
import slimeknights.mantle.client.model.RetexturedModel;
import slimeknights.mantle.client.model.RetexturedModel.RetexturedContext;
import slimeknights.mantle.client.model.data.IModelData;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.DynamicBakedWrapper;
import slimeknights.mantle.client.model.util.ExtraTextureContext;
import slimeknights.mantle.client.model.util.MantleBakedModel;
import slimeknights.mantle.client.model.util.ModelHelper;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.data.loadable.Loadable;
import slimeknights.mantle.data.loadable.array.ArrayLoadable;
import slimeknights.mantle.data.loadable.primitive.StringLoadable;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfo.TintedSprite;
import slimeknights.tconstruct.library.client.materials.MaterialRenderInfoLoader;
import slimeknights.tconstruct.library.client.model.ModelProperties;
import slimeknights.tconstruct.library.materials.definition.MaterialId;
import slimeknights.tconstruct.library.materials.definition.MaterialVariantId;
import slimeknights.tconstruct.library.tools.nbt.MaterialIdNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Model that handles dynamic materials using the block model elements style.
 * When used for tools, notably does not handle modifier models, just materials are considered.
 * @see MaterialModel
 * @see ToolModel
 */
@RequiredArgsConstructor
public class MaterialBlockModel implements IUnbakedGeometry<MaterialBlockModel> {
  /** Location for dynamic baking */
  private static final ResourceLocation BAKE_LOCATION = Mantle.getResource("material_block_dynamic");
  /** Loadable for the list of material textures */
  private static final Loadable<Set<String>> MATERIAL = StringLoadable.DEFAULT.set(ArrayLoadable.COMPACT);
  /** Loadable for the list of parts, each a list of material textures */
  private static final Loadable<List<Set<String>>> PARTS = StringLoadable.DEFAULT.set(ArrayLoadable.COMPACT_OR_EMPTY).list(1);
  /** Shared loader instance */
  public static final IGeometryLoader<MaterialBlockModel> LOADER = MaterialBlockModel::deserialize;

  public enum ModelType { TOOL, PART, ANVIL }

  /** Block model to retexture. Expected each element is a single material or not a material, no multiple or mixed. */
  private final SimpleBlockModel model;
  /** List of parts, each is a set of materials to retexture */
  private final List<Set<String>> parts;
  /** Type of model to create. */
  private final ModelType type;

  /** Loads a material block model from JSON */
  public static MaterialBlockModel deserialize(JsonObject json, JsonDeserializationContext context) {
    SimpleBlockModel model = SimpleBlockModel.deserialize(json, context);

    // if retextured is set, using the anvil style model
    if (json.has("retextured")) {
      return new MaterialBlockModel(model, List.of(MATERIAL.getIfPresent(json, "retextured")), ModelType.ANVIL);
    }
    // if material is set, using the parts model
    if (json.has("material")) {
      return new MaterialBlockModel(model, List.of(MATERIAL.getIfPresent(json, "material")), ModelType.PART);
    }
    // otherwise, using the tool model
    return new MaterialBlockModel(model, PARTS.getIfPresent(json, "parts"), ModelType.TOOL);
  }

  @Override
  public void resolveParents(Function<ResourceLocation, UnbakedModel> modelGetter, BlockModel context) {
    model.resolveParents(modelGetter, context);
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState transform, ItemOverrides overrides, ResourceLocation location, boolean isGui3d) {
    BakedModel baked = model.bake(owner, baker, spriteGetter, transform, overrides, location, isGui3d);
    List<Set<String>> parts = this.parts.stream().map(part -> RetexturedModel.getAllRetextured(owner, model, part)).toList();

    // part model - fetches material from NBT field
    if (type == ModelType.PART) {
      return new BakedPart(baked, owner, model, transform, parts.get(0));
    }
    // anvil model - fetches material from NBT field, but can also choose block texture
    if (type == ModelType.ANVIL) {
      return new BakedAnvil(baked, owner, model, transform, parts.get(0));
    }
    // for tools with just one material, use simpler tool model
    if (parts.size() == 1) {
      return new SimpleBakedTool(baked, owner, model, transform, parts.get(0));
    }
    // standard tool with multiple materials
    boolean particleRetextured = parts.stream().anyMatch(set -> set.contains("particle"));
    return new BakedTool(baked, owner, model, transform, parts, particleRetextured);
  }

  /** Fetches the material for the given texture name, falling back to the missing texture if the name is not defined */
  private static Material getTexture(BlockModel owner, String name) {
    if (owner.hasTexture(name)) {
      return owner.getMaterial(name);
    }
    return new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation());
  }

  /** Common logic between all variants of baking */
  private static abstract class AbstractBaked<T,P> extends DynamicBakedWrapper<BakedModel> implements CustomParticleIconModel {
    protected final BlockModel owner;
    protected final SimpleBlockModel model;
    protected final ModelState transform;
    protected final boolean particleRetextured;
    protected final ModelProperty<P> property;

    private AbstractBaked(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, boolean particleRetextured, ModelProperty<P> property) {
      super(original);
      this.owner = owner;
      this.model = model;
      this.transform = transform;
      this.particleRetextured = particleRetextured;
      this.property = property;
    }

    /** Fetches textures from the materials for baking */
    protected abstract void fetchMaterials(T materials, Function<Material, TextureAtlasSprite> spriteGetter, Map<String, Material> replacements, Map<String, TintedSprite> tints);

    /** Fetches a single material. For use in {@link #fetchMaterials(Object, Function, Map, Map)} */
    protected void fetchMaterial(MaterialVariantId material, Set<String> retextured, Function<Material, TextureAtlasSprite> spriteGetter, Map<String, Material> replacements, Map<String, TintedSprite> tints) {
      // fetch render info
      Optional<MaterialRenderInfo> optional = MaterialRenderInfoLoader.INSTANCE.getRenderInfo(material);
      if (optional.isPresent()) {
        MaterialRenderInfo info = optional.get();

        // may have multiple names going to the same texture
        Map<Material, TintedSprite> seen = new HashMap<>();
        for (String name : retextured) {
          Material texture = getTexture(owner, name);
          TintedSprite tinted = seen.get(texture);
          if (tinted == null) {
            tinted = info.getSprite(texture, spriteGetter);
            seen.put(texture, tinted);
          }
          TextureAtlasSprite sprite = tinted.sprite();
          // bit annoying that we have to unwrap the sprite, but its needed to be fetched to check if its present
          replacements.put(name, new Material(sprite.atlasLocation(), sprite.contents().name()));
          // if we have a transform, save that with the name too
          if (tinted.color() != -1 || tinted.emissivity() > 0) {
            tints.put('#' + name, tinted);
          }
        }
      }
    }

    /** Retextures with the given materials list */
    protected BakedModel bakeWith(T materials) {
      // map of name to replacement texture
      Map<String,Material> replacements = new HashMap<>();
      // map of name to transformation to apply, will likely be smaller than replacement textures
      Map<String,TintedSprite> tints = new HashMap<>();

      // delegate material fetching
      Function<Material,TextureAtlasSprite> spriteGetter = Material::sprite;
      fetchMaterials(materials, spriteGetter, replacements, tints);

      // quick exit in case we found nothing, should never happen
      if (replacements.isEmpty()) {
        return wrapped;
      }

      // create context to swap the textures
      ExtraTextureContext retextureContext = new ExtraTextureContext(owner, replacements);

      // if no parts need colors, we can just use standard baking
      if (tints.isEmpty()) {
        return model.bakeDynamic(retextureContext, transform);
      }

      // need to tint or apply light to some textures
      TextureAtlasSprite particle = spriteGetter.apply(getTexture(owner, "particle"));
      MantleBakedModel.Builder builder = SimpleBlockModel.bakedBuilder(owner, wrapped.getOverrides());
      builder.particle(particle);
      List<BlockElement> elements = model.getElements();
      int size = elements.size();
      RenderContext.QuadTransform quadTransformer = SimpleBlockModel.applyTransform(transform, ((BlockModelExtensions) owner).getRootTransform());
      Transformation transformation = transform.getRotation();
      boolean uvlock = transform.isUvLocked();
      for (int i = 0; i < size; i++) {
        BlockElement part = elements.get(i);
        // determine if any of the faces needs a tint
        // for simplicity, assume the whole part is tinted if so. Build your model to separate distinct material faces if needed
        TintedSprite tint = null;
        for (BlockElementFace face : part.faces.values()) {
          TintedSprite faceTint = tints.get(face.texture);
          if (faceTint != null) {
            tint = faceTint;
            break;
          }
        }
        // apply color if we have it
        if (tint != null) {
          // TODO(fabric): Forge chained the transformers with andThen, Fabric transformers are merged instead
          RenderContext.QuadTransform partTransformer = tint.color() == -1 ? quadTransformer : ColoredBlockModel.mergeTransform(quadTransformer, ColoredBlockModel.applyColorQuadTransformer(tint.color()));
          ColoredBlockModel.bakePart(builder, retextureContext, part, tint.emissivity(), spriteGetter, transformation, partTransformer, uvlock, BAKE_LOCATION);
        } else {
          SimpleBlockModel.bakePart(builder, retextureContext, part, spriteGetter, transform, quadTransformer, BAKE_LOCATION);
        }
      }
      return builder.build();
    }

    /** Gets the cached model for the given materials. */
    protected abstract BakedModel getCachedModel(P materials);

    /** Gets the model data for the given position, if present */
    @Nullable
    protected static IModelData getModelData(BlockAndTintGetter blockView, BlockPos pos) {
      return blockView instanceof RenderAttachedBlockView attached && attached.getBlockEntityRenderAttachment(pos) instanceof IModelData data ? data : null;
    }

    /** Emits the quads of the passed model */
    protected void emitCached(BakedModel cached, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      // TODO(fabric): javac treats the pattern variable as unconditional against the renderer API's BakedModel, plain
      //  instanceof keeps the fallback branch below valid
      if (cached instanceof FabricBakedModel) {
        ((FabricBakedModel) cached).emitBlockQuads(blockView, state, pos, randomSupplier, context);
      } else {
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
      }
    }

    @Override
    public TextureAtlasSprite getParticleIcon(Object data) {
      if (particleRetextured && data instanceof IModelData modelData) {
        P materials = modelData.get(property);
        if (materials != null) {
          return ModelHelper.getParticleIcon(getCachedModel(materials), data);
        }
      }
      return ModelHelper.getParticleIcon(wrapped, data);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      IModelData data = getModelData(blockView, pos);
      if (data != null) {
        P materials = data.get(property);
        if (materials != null) {
          emitCached(getCachedModel(materials), blockView, state, pos, randomSupplier, context);
          return;
        }
      }
      super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }
  }

  /** Custom overrides logic to sub in materials from NBT. */
  private static class MaterialsOverrides extends NestedOverrides {
    private final AbstractBaked<?, MaterialIdNBT> baked;
    public MaterialsOverrides(AbstractBaked<?,MaterialIdNBT> baked, ItemOverrides nested) {
      super(nested);
      this.baked = baked;
    }

    @Nullable
    @Override
    public BakedModel resolve(BakedModel wrapped, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
      BakedModel resolved = super.resolve(wrapped, stack, world, entity, seed);
      if (resolved != wrapped) {
        return resolved;
      }
      if (stack.isEmpty() || !stack.hasTag()) {
        return wrapped;
      }
      return baked.getCachedModel(MaterialIdNBT.from(stack));
    }
  }

  /** Baked model for tools with multiple materials. */
  private static class BakedTool extends AbstractBaked<MaterialIdNBT,MaterialIdNBT> {
    /** Cache of texture name to baked model */
    private final Cache<MaterialIdNBT, BakedModel> cache = CacheBuilder
      .newBuilder()
      // ensure we can display every single tool that shows in JEI, plus a couple extra
      .maximumSize(MaterialRenderInfoLoader.INSTANCE.getAllRenderInfos().size() * 3L / 2)
      .build();

    private final List<Set<String>> parts;
    @Getter
    private final ItemOverrides overrides;
    private BakedTool(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, List<Set<String>> parts, boolean particleRetextured) {
      super(original, owner, model, transform, particleRetextured, ModelProperties.MATERIALS);
      this.parts = parts;
      this.overrides = new MaterialsOverrides(this, original.getOverrides());
    }

    @Override
    protected void fetchMaterials(MaterialIdNBT materials, Function<Material, TextureAtlasSprite> spriteGetter, Map<String, Material> replacements, Map<String, TintedSprite> tints) {
      for (int i = 0; i < parts.size(); i++) {
        fetchMaterial(materials.getMaterial(i), parts.get(i), spriteGetter, replacements, tints);
      }
    }

    @Override
    protected BakedModel getCachedModel(MaterialIdNBT materials) {
      if (!materials.getMaterials().isEmpty()) {
        try {
          return cache.get(materials, () -> bakeWith(materials));
        } catch (ExecutionException e) {
          TConstruct.LOG.error("Failed to get tool model from cache", e);
        }
      }
      return wrapped;
    }
  }

  /** Common logic between tool parts and tools with just 1 material. */
  private static abstract class BakedSingleMaterial<P> extends AbstractBaked<MaterialVariantId,P> {
    /** Cache of texture name to baked model */
    private final Map<MaterialVariantId, BakedModel> cache = new ConcurrentHashMap<>();

    protected final Set<String> retexture;
    private final Function<MaterialVariantId, BakedModel> baker = this::bakeWith;

    private BakedSingleMaterial(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, Set<String> retexture, ModelProperty<P> property) {
      super(original, owner, model, transform, retexture.contains("particle"), property);
      this.retexture = retexture;
    }

    /** Gets the model for the given material */
    public BakedModel getCachedModel(MaterialVariantId material) {
      if (MaterialId.UNKNOWN.equals(material)) {
        return wrapped;
      }
      return cache.computeIfAbsent(material, baker);
    }

    @Override
    protected void fetchMaterials(MaterialVariantId materials, Function<Material, TextureAtlasSprite> spriteGetter, Map<String, Material> replacements, Map<String, TintedSprite> tints) {
      fetchMaterial(materials, retexture, spriteGetter, replacements, tints);
    }
  }

  /** Baked model for tools with just 1 material. */
  private static class SimpleBakedTool extends BakedSingleMaterial<MaterialIdNBT> {
    @Getter
    private final ItemOverrides overrides;
    private SimpleBakedTool(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, Set<String> retexture) {
      super(original, owner, model, transform, retexture, ModelProperties.MATERIALS);
      this.overrides = new MaterialsOverrides(this, original.getOverrides());
    }

    @Override
    protected BakedModel getCachedModel(MaterialIdNBT materials) {
      return getCachedModel(materials.getMaterial(0));
    }
  }

  /** Baked model for tool parts. */
  private static class BakedPart extends BakedSingleMaterial<MaterialVariantId> {
    @Getter
    private final ItemOverrides overrides;
    private BakedPart(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, Set<String> retexture) {
      super(original, owner, model, transform, retexture, ModelProperties.MATERIAL);
      this.overrides = new MaterialOverrides(original.getOverrides());
    }

    /** Custom overrides logic to sub in materials from NBT. */
    private class MaterialOverrides extends NestedOverrides {
      public MaterialOverrides(ItemOverrides nested) {
        super(nested);
      }

      @Nullable
      @Override
      public BakedModel resolve(BakedModel wrapped, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
        BakedModel resolved = super.resolve(wrapped, stack, world, entity, seed);
        if (resolved != wrapped) {
          return resolved;
        }
        if (stack.isEmpty() || !stack.hasTag()) {
          return wrapped;
        }
        return getCachedModel(IMaterialItem.getMaterialFromStack(stack));
      }
    }
  }

  /** Model that supports both block and material textures. */
  private static class BakedAnvil extends BakedSingleMaterial<MaterialVariantId> {
    private final Map<ResourceLocation, BakedModel> blockCache = new ConcurrentHashMap<>();
    private final Function<ResourceLocation, BakedModel> blockBaker = this::bakeWithBlock;
    @Getter
    private final MaterialBlockOverrides overrides;
    private BakedAnvil(BakedModel original, BlockModel owner, SimpleBlockModel model, ModelState transform, Set<String> retexture) {
      super(original, owner, model, transform, retexture, ModelProperties.MATERIAL);
      this.overrides = new MaterialBlockOverrides();
    }

    /** Rebakes the model with a specific texture. See also {@link RetexturedModel} */
    private BakedModel bakeWithBlock(ResourceLocation texture) {
      return this.model.bakeDynamic(new RetexturedContext(this.owner, this.retexture, texture), this.transform);
    }

    /** Gets the cached model for the given block. */
    private BakedModel getCachedModel(Block block) {
      return this.blockCache.computeIfAbsent(ModelHelper.getParticleTexture(block), blockBaker);
    }

    /** Gets the block from the model data for the given position, if present */
    @Nullable
    private static Block getBlock(BlockAndTintGetter blockView, BlockPos pos) {
      IModelData data = getModelData(blockView, pos);
      return data == null ? null : data.get(RetexturedHelper.BLOCK_PROPERTY);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(Object data) {
      // block takes priority if present
      if (particleRetextured && data instanceof IModelData modelData) {
        Block block = modelData.get(RetexturedHelper.BLOCK_PROPERTY);
        if (block != null) {
          return ModelHelper.getParticleIcon(getCachedModel(block), data);
        }
      }
      return super.getParticleIcon(data);
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context) {
      Block block = getBlock(blockView, pos);
      if (block != null) {
        emitCached(getCachedModel(block), blockView, state, pos, randomSupplier, context);
        return;
      }
      super.emitBlockQuads(blockView, state, pos, randomSupplier, context);
    }

    /** Custom overrides logic to sub in materials from NBT. */
    private class MaterialBlockOverrides extends ItemOverrides {
      @Nullable
      @Override
      public BakedModel resolve(BakedModel wrapped, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
        if (stack.isEmpty() || !stack.hasTag()) {
          return wrapped;
        }
        Block block = RetexturedHelper.getTexture(stack);
        if (block != Blocks.AIR) {
          return getCachedModel(block);
        }
        return getCachedModel(IMaterialItem.getMaterialFromStack(stack));
      }
    }
  }
}
