package slimeknights.tconstruct.library.tools.item.armor;

import io.github.fabricators_of_create.porting_lib.item.ArmorTextureItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/** Armor model that applies multiple texture layers in order */
public class MultilayerArmorItem extends ModifiableArmorItem implements ArmorTextureItem {
  private final ResourceLocation name;
  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties) {
    this(material, slot, properties, material.getId());
  }

  public MultilayerArmorItem(ModifiableArmorMaterial material, ArmorItem.Type slot, Properties properties, ResourceLocation name) {
    super(material, slot, properties);
    this.name = name;
  }

  @SuppressWarnings("removal")
  public MultilayerArmorItem(ArmorMaterial material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition) {
    this(material, slot, properties, toolDefinition, new ResourceLocation(material.getName()));
  }

  public MultilayerArmorItem(ArmorMaterial material, ArmorItem.Type slot, Properties properties, ToolDefinition toolDefinition, ResourceLocation name) {
    super(material, slot, properties, toolDefinition);
    this.name = name;
  }

  @Nullable
  @Override
  public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
    return ArmorUtil.getDummyArmorTexture(slot);
  }

  // TODO(fabric): Forge client item extensions have no Fabric equivalent, the armor model dispatcher must be
  //  registered by the client armor renderer instead (see ArmorModelManager)
}
