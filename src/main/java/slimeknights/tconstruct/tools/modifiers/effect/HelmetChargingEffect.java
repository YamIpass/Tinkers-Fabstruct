package slimeknights.tconstruct.tools.modifiers.effect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.MobEffectTextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.tools.TinkerModifiers;

import java.util.ArrayList;
import java.util.List;

/** Effect for rendering the charge up when you start using a helmet */
public class HelmetChargingEffect extends MobEffect {
  public HelmetChargingEffect() {
    super(MobEffectCategory.NEUTRAL, -1);
  }

  @Override
  public List<ItemStack> getCurativeItems() {
    return new ArrayList<>();
  }

  // TODO(fabric): Forge's IClientMobEffectExtensions has no Fabric equivalent yet; Porting Lib ships a
  //  MobEffectRenderer interface but no registry for it, so the custom helmet charging icon is not rendered
  // private void initializeClient() { ... }

  /* Helpers */

  /** Starts using the helmet with the charge time rendering */
  public static int startUsingHelmet(IToolStackView tool, LivingEntity living, float speedFactor) {
    int time = GeneralInteractionModifierHook.startDrawing(tool, living, speedFactor);
    living.addEffect(new MobEffectInstance(TinkerModifiers.helmetCharging.get(), time + 20, 0, true, false, true));
    return time;
  }
}
