/*
 * Minecraft Forge
 * Copyright (c) 2016-2021.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package slimeknights.tconstruct.library.client.model;

import com.google.common.collect.Maps;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.math.Transformation;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext.QuadTransform;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import io.github.fabricators_of_create.porting_lib.models.CompositeModel;
import io.github.fabricators_of_create.porting_lib.models.QuadTransformers;
import io.github.fabricators_of_create.porting_lib.models.geometry.IGeometryLoader;
import io.github.fabricators_of_create.porting_lib.models.geometry.IUnbakedGeometry;
import io.github.fabricators_of_create.porting_lib.models.geometry.SimpleModelState;
import io.github.fabricators_of_create.porting_lib.models.UnbakedGeometryHelper;
import io.github.fabricators_of_create.porting_lib.util.CraftingHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import io.github.fabricators_of_create.porting_lib.fluids.FluidType;
import slimeknights.mantle.client.model.util.ColoredBlockModel;
import slimeknights.mantle.client.model.util.MantleBakedModel;
import slimeknights.mantle.client.model.util.SimpleBlockModel;
import slimeknights.mantle.data.loadable.Loadables;
import slimeknights.mantle.fluid.texture.FluidTextureManager;
import slimeknights.tconstruct.TConstruct;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extension of the Forge dynamic fluid container model with two additional features: baked tints and fluid stack sensitive models.
 * Does not handle covers as I have never seen a need for them, and it means less code duplication (plus the forge model does the whole cover is mask thing wrong compared to 1.18).
 */
public record FluidContainerModel(FluidStack fluid, boolean flipGas) implements IUnbakedGeometry<FluidContainerModel> {
  public static final IGeometryLoader<FluidContainerModel> LOADER = FluidContainerModel::deserialize;

  /** Clone of same named field from Forge's dynamic fluid container model */
  public static final Transformation FLUID_TRANSFORM = new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1, 1, 1.002f), new Quaternionf());

  /** Fluid amount used for the cached models, the model is the same regardless of size */
  private static final long FLUID_AMOUNT = FluidConstants.BUCKET;

  /** Deserializes this model from JSON */
  public static FluidContainerModel deserialize(JsonObject json, JsonDeserializationContext context) {
    FluidStack fluidStack = FluidStack.EMPTY;
    // parse the fluid with an optional tag
    if (json.has("fluid")) {
      JsonElement fluidElement = json.get("fluid");
      Fluid fluid;
      CompoundTag tag = null;
      if (fluidElement.isJsonObject()) {
        JsonObject fluidObject = fluidElement.getAsJsonObject();
        fluid = Loadables.FLUID.getIfPresent(fluidObject, "name");
        if (fluidObject.has("nbt")) {
          tag = CraftingHelper.getNBT(fluidObject.get("nbt"));
        }
      } else {
        fluid = Loadables.FLUID.convert(fluidElement, "fluid");
      }
      fluidStack = new FluidStack(fluid, FLUID_AMOUNT, tag);
    }
    boolean flipGas = GsonHelper.getAsBoolean(json, "flip_gas", true);
    return new FluidContainerModel(fluidStack, flipGas);
  }

  /** Gets the given sprite, or null if the texture is not present in the model */
  @Nullable
  private static TextureAtlasSprite getSprite(BlockModel owner, Function<Material,TextureAtlasSprite> spriteGetter, String key) {
    if (owner.hasTexture(key)) {
      return spriteGetter.apply(owner.getMaterial(key));
    }
    return null;
  }

  private static BakedModel bakeInternal(BlockModel owner, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation, FluidStack fluid, boolean flipGas) {
    // get basic sprites
    TextureAtlasSprite baseSprite = getSprite(owner, spriteGetter, "base");
    TextureAtlasSprite fluidSprite = null;
    if (!fluid.isEmpty()) {
      // resolve the fluid texture straight from the texture data instead of FluidVariantRendering: that helper forces
      // a non null sprite and fabric only knows fluids that own a block state, so the old call baked the missing
      // texture into every bucket. Mantle's texture data covers every fluid of this mod regardless of placement.
      try {
        FluidType textureType = fluid.getType().getFluid().getFluidType();
        fluidSprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, FluidTextureManager.getStillTexture(textureType)));
      } catch (RuntimeException exception) {
        TConstruct.LOG.error("Failed to resolve the still texture of {} for model {}", fluid, modelLocation, exception);
      }
      if (fluidSprite == null) {
        TextureAtlasSprite[] sprites = FluidVariantRendering.getSprites(fluid.getType());
        if (sprites != null && sprites.length > 0) {
          fluidSprite = sprites[0];
        }
      }
      if (fluidSprite == null) {
        fluidSprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
      }
    }

    // determine particle
    TextureAtlasSprite particleSprite = getSprite(owner, spriteGetter, "particle");
    if (particleSprite == null) particleSprite = fluidSprite;
    if (particleSprite == null) particleSprite = baseSprite;
    if (particleSprite == null) {
      TConstruct.LOG.error("No valid particle sprite for fluid container model, you should supply either 'base' or 'particle'");
      particleSprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, MissingTextureAtlasSprite.getLocation()));
    }

    // if its a gas and we flipping, flip it
    if (flipGas && !fluid.isEmpty() && FluidVariantAttributes.isLighterThanAir(fluid.getType())) {
      modelState = new SimpleModelState(modelState.getRotation().compose(new Transformation(null, new Quaternionf(0, 0, 1, 0), null, null)));
    }

    // start building the model, the second and third booleans replace Forge's gui 3d and block light overrides
    CompositeModel.Baked.Builder modelBuilder = CompositeModel.Baked.builder(owner.hasAmbientOcclusion(), false, false, particleSprite, overrides, owner.getTransforms());

    // add in the base
    if (baseSprite != null) {
      modelBuilder.addQuads(UnbakedGeometryHelper.bakeElements(
        UnbakedGeometryHelper.createUnbakedItemElements(0, baseSprite.contents()),
        $ -> baseSprite, modelState, modelLocation
      ));
    }

    // add in fluid
    TextureAtlasSprite finalFluidSprite = fluidSprite;
    if (finalFluidSprite != null) {
      try {
        Material fluidMaterial = owner.getMaterial("fluid");
        if (fluidMaterial == null) {
          // the parent model is expected to supply the fluid mask, without it we can only bake the base
          TConstruct.LOG.error("Model {} has no fluid mask texture, falling back to the base layer", modelLocation);
        } else {
          List<BakedQuad> quads = UnbakedGeometryHelper.bakeElements(
            UnbakedGeometryHelper.createUnbakedItemMaskElements(1, spriteGetter.apply(fluidMaterial).contents()),
            $ -> finalFluidSprite,
            new SimpleModelState(modelState.getRotation().compose(FLUID_TRANSFORM), modelState.isUvLocked()),
            modelLocation
          );
          // the fluid is baked into its own layer so we can tint it without tinting the base texture
          MantleBakedModel.Builder fluidBuilder = SimpleBlockModel.bakedBuilder(owner, ItemOverrides.EMPTY).particle(finalFluidSprite);
          QuadEmitter emitter = fluidBuilder.getEmitter();
          MaterialFinder finder = RendererAccess.INSTANCE.getRenderer().materialFinder().blendMode(BlendMode.TRANSLUCENT);
          RenderMaterial material = finder.find();
          // note these two come from the fluid type, replacing Forge's IClientFluidTypeExtensions
          int light = FluidVariantAttributes.getLuminance(fluid.getType());
          int color = FluidVariantRendering.getColor(fluid.getType());
          QuadTransform colorTransformer = color == -1 ? null : QuadTransformers.applyingColor(color);
          QuadTransform lightTransformer = light <= 0 ? null : QuadTransformers.settingEmissivity(light);
          QuadTransform transformer = colorTransformer == null ? lightTransformer : (lightTransformer == null ? colorTransformer : ColoredBlockModel.mergeTransform(colorTransformer, lightTransformer));
          for (BakedQuad quad : quads) {
            emitter.fromVanilla(quad, material, quad.getDirection());
            if (transformer != null) {
              transformer.transform(emitter);
            }
            emitter.emit();
          }
          modelBuilder.addLayer(fluidBuilder.build());
        }
      } catch (RuntimeException exception) {
        TConstruct.LOG.error("Failed to bake the fluid layer of {}, only the base layer is shown", modelLocation, exception);
      }
    }
    return modelBuilder.build();
  }

  @Override
  public BakedModel bake(BlockModel owner, ModelBaker bakery, Function<Material,TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation, boolean isGui3d) {
    // only do contained fluid if we did not set the fluid in the model properties
    if (fluid.isEmpty()) {
      overrides = new ContainedFluidOverrideHandler(owner, overrides, modelState, flipGas);
    }
    try {
      return bakeInternal(owner, spriteGetter, modelState, overrides, modelLocation, fluid, flipGas);
    } catch (RuntimeException exception) {
      // the loader is reached from vanilla model loading, which only logs the message, so keep the trace for us
      TConstruct.LOG.error("Failed to bake the fluid container model {}", modelLocation, exception);
      throw exception;
    }
  }

  /** Handles swapping the model based on the contained fluid */
  private static final class ContainedFluidOverrideHandler extends ItemOverrides {
    private static final ResourceLocation BAKE_LOCATION = TConstruct.getResource("copper_can_dynamic");

    private final Map<FluidStack,BakedModel> cache = Maps.newHashMap(); // contains all the baked models since they'll never change

    private final BlockModel owner;
    private final ItemOverrides nested;
    private final ModelState modelState;
    private final boolean flipGas;

    public ContainedFluidOverrideHandler(BlockModel owner, ItemOverrides nested, ModelState modelState, boolean flipGas) {
      super(null, owner, List.of());
      this.owner = owner;
      this.nested = nested;
      this.modelState = modelState;
      this.flipGas = flipGas;
    }


    /** Gets the model directly, for creating the cached models */
    private BakedModel getUncahcedModel(FluidStack fluid) {
      return bakeInternal(owner, Material::sprite, modelState, ItemOverrides.EMPTY, BAKE_LOCATION, fluid, flipGas);
    }

    @Override
    public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity entity, int seed) {
      BakedModel overriden = nested.resolve(originalModel, stack, world, entity, seed);
      if (overriden != originalModel) return overriden;
      // TODO(fabric): Forge's FluidUtil.getFluidContained(stack) has no Fabric equivalent, we query the item's fluid storage instead
      Storage<FluidVariant> storage = FluidStorage.ITEM.find(stack, ContainerItemContext.withConstant(stack));
      if (storage != null) {
        for (StorageView<FluidVariant> view : storage) {
          if (!view.isResourceBlank() && view.getAmount() > 0) {
            FluidStack fluid = new FluidStack(view.getResource(), FLUID_AMOUNT); // cache considers amount, so ensure its consistent
            return cache.computeIfAbsent(fluid, this::getUncahcedModel);
          }
        }
      }
      return originalModel;
    }
  }
}
