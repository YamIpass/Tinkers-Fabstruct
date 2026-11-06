package slimeknights.tconstruct.tools.item;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRenderer;
import io.github.fabricators_of_create.porting_lib.client.armor.ArmorRendererRegistry;
import io.github.fabricators_of_create.porting_lib.util.EnvExecutor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.definition.ModifiableArmorMaterial;
import slimeknights.tconstruct.library.tools.helper.ArmorUtil;
import slimeknights.tconstruct.library.tools.item.armor.ModifiableArmorItem;
import slimeknights.tconstruct.tools.client.SlimeskullArmorModel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** This item is mainly to return the proper model for a slimeskull */
public class SlimeskullItem extends ModifiableArmorItem {
  /** Model ID for our slimeskull. You may want your own for a custom slimeskull */
  public static final ResourceLocation MODEL_LOCATION = TConstruct.getResource("slimeskull");

  private final ResourceLocation name;

  public SlimeskullItem(ModifiableArmorMaterial material, ResourceLocation name, Properties properties) {
    super(material, ArmorItem.Type.HELMET, properties);
    this.name = name;
    EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::initializeClient);
  }

  public SlimeskullItem(ModifiableArmorMaterial material, Properties properties) {
    this(material, material.getId(), properties);
  }

  @Nullable
    // TODO(fabric): Forge's Item#getArmorTexture has no Fabric equivalent
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
    return ArmorUtil.getDummyArmorTexture(slot);
  }

  @Environment(EnvType.CLIENT)
  public void initializeClient() {
    ArmorRendererRegistry.register(new Slimeskull(), this);
  }

  @Environment(EnvType.CLIENT)
  private static final class Slimeskull implements ArmorRenderer {
    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel, HumanoidModel<LivingEntity> armorModel) {
      // TODO(fabric): ArmorModelManager (library/client/armor) still needs its Fabric port; until then render the vanilla armor model
      Model model = SlimeskullArmorModel.INSTANCE.setup(entity, stack, armorModel, null);
      model.renderToBuffer(matrices, vertexConsumers.getBuffer(model.renderType(new ResourceLocation("textures/misc/white.png"))), light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
    }
  }
}
