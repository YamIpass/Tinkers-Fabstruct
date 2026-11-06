package slimeknights.tconstruct.library.client.armor.texture;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import slimeknights.mantle.data.loadable.mapping.ConditionalLoadable.ConditionalObject;
import slimeknights.mantle.data.loadable.record.RecordLoadable;
import slimeknights.mantle.data.registry.GenericLoaderRegistry.IHaveLoader;

/**
 * Datagen helper for making conditional {@link ArmorTextureSupplier}.
 * @param ifTrue      Supplier to use if all conditions are true.
 * @param ifFalse     Supplier to use if any condition is false. Defaults to {@link ArmorTextureSupplier#EMPTY}
 * @param conditions  Conditions to evaluate.
 */
@SuppressWarnings("unused") // API
public record ConditionalArmorTextureSupplier(ArmorTextureSupplier ifTrue, ArmorTextureSupplier ifFalse, ConditionJsonProvider... conditions) implements ArmorTextureSupplier, ConditionalObject<ArmorTextureSupplier> {
  public ConditionalArmorTextureSupplier(ArmorTextureSupplier ifTrue, ConditionJsonProvider... conditions) {
    this(ifTrue, ArmorTextureSupplier.EMPTY, conditions);
  }

  @Override
  public ArmorTexture getArmorTexture(ItemStack stack, TextureType leggings, RegistryAccess access) {
    // TODO(fabric): Forge evaluated ICondition at runtime; Fabric resolves the condition when the JSON is loaded, so the resolved instance is always ifTrue
    return ifTrue.getArmorTexture(stack, leggings, access);
  }

  @Override
  public RecordLoadable<? extends IHaveLoader> getLoader() {
    return ArmorTextureSupplier.LOADER.getConditionalLoader();
  }
}
