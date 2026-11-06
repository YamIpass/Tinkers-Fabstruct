package slimeknights.tconstruct.library.client.model;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ItemLike;
import com.google.common.collect.Maps;
import io.github.fabricators_of_create.porting_lib.tool.ToolAction;
import io.github.fabricators_of_create.porting_lib.tool.ToolActions;
import io.github.fabricators_of_create.porting_lib.tool.addons.ToolActionItem;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.helper.ToolDamageUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableCrossbowItem;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableLauncherItem;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Properties for tinker tools */
@SuppressWarnings("deprecation")
public class TinkerItemProperties {
  /** ID for broken property */
  private static final ResourceLocation BROKEN_ID = TConstruct.getResource("broken");
  /** Property declaring broken */
  private static final ClampedItemPropertyFunction BROKEN = (stack, level, entity, seed) -> {
    return ToolDamageUtil.isBroken(stack) ? 1 : 0;
  };

  /** ID for ammo property */
  private static final ResourceLocation AMMO_ID = TConstruct.getResource("ammo");
  /** ID of fireworks rocket for NBT check */
  private static final String FIREWORKS_ID = "minecraft:firework_rocket";
  /** Int declaring ammo type */
  private static final ClampedItemPropertyFunction AMMO = (stack, level, entity, seed) -> {
    CompoundTag nbt = stack.getTag();
    if (nbt != null) {
      CompoundTag persistentData = nbt.getCompound(ToolStack.TAG_PERSISTENT_MOD_DATA);
      if (!persistentData.isEmpty()) {
        CompoundTag ammo = persistentData.getCompound(ModifiableCrossbowItem.KEY_CROSSBOW_AMMO.toString());
        if (!ammo.isEmpty()) {
          // no sense having two keys for ammo, just set 1 for arrow, 2 for fireworks
          return FIREWORKS_ID.equals(ammo.getString("id")) ? 2 : 1;
        }
      }
    }
    return 0;
  };

  /** ID for the pulling property */
  private static final ResourceLocation CHARGING_ID = TConstruct.getResource("charging");
  /** Boolean indicating the bow is pulling */
  private static final ClampedItemPropertyFunction CHARGING = (stack, level, holder, seed) -> {
    if (holder != null && holder.isUsingItem() && holder.getUseItem() == stack) {
      UseAnim anim = stack.getUseAnimation();
      if (anim == UseAnim.BLOCK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 2.5f : 2;
      }
      // TODO 1.21: space this out a bit more
      if (anim == UseAnim.SPEAR) {
        // shouldn't need to worry about arrows on spearing, everything supporting arrows uses just bow or block
        return 1.75f;
      }
      if (anim != UseAnim.EAT && anim != UseAnim.DRINK) {
        return ModifierUtil.checkPersistentPresent(stack, ModifiableLauncherItem.KEY_DRAWBACK_AMMO) ? 1.5f : 1;
      }
    }
    return 0;
  };
  /** ID for the pull property */
  private static final ResourceLocation CHARGE_ID = TConstruct.getResource("charge");
  /** Property for bow pull amount */
  private static final ClampedItemPropertyFunction CHARGE = (stack, level, holder, seed) -> {
    if (holder == null || holder.getUseItem() != stack) {
      return 0.0F;
    }
    int drawtime = ModifierUtil.getPersistentInt(stack, GeneralInteractionModifierHook.KEY_DRAWTIME, -1);
    return drawtime == -1 ? 0 : (float)(stack.getUseDuration() - holder.getUseItemRemainingTicks()) / drawtime;
  };
  /** ID for the cast fishing rods */
  private static final ResourceLocation CAST_ID = TConstruct.getResource("cast");
  /** Property for casting a fishing rod */
  private static final ClampedItemPropertyFunction CAST = (stack, level, holder, seed) -> {
    // must be a fishing rod, and the player must be fishing
    // does player check first since its the fastest, avoids NBT parsing
    if (holder instanceof Player player && player.fishing != null && canPerformAction(stack, ToolActions.FISHING_ROD_CAST)) {
      // must be in a hand, but if both hands have fishing rods, must be the one in the main hand
      ItemStack mainhand = holder.getMainHandItem();
      if (mainhand == stack || holder.getOffhandItem() == stack && !canPerformAction(mainhand, ToolActions.FISHING_ROD_CAST)) {
        return 1;
      }
    }
    return 0;
  };

  /** Checks if the stack can perform the given action, Fabric replacement for {@code ItemStack#canPerformAction} */
  private static boolean canPerformAction(ItemStack stack, ToolAction action) {
    return stack.getItem() instanceof ToolActionItem item && item.canPerformAction(stack, action);
  }

  /** Registers properties for a tool, including the option to have charge/block animations */
  public static void registerBrokenProperty(Item item) {
    ItemProperties.PROPERTIES.computeIfAbsent(item, it -> Maps.newHashMap()).put(BROKEN_ID, BROKEN);
  }

  /** Registers properties for a tool, including the option to have charge/block animations */
  public static void registerToolProperties(ItemLike itemlike) {
    Item item = itemlike.asItem();
    registerBrokenProperty(item);
    ItemProperties.PROPERTIES.computeIfAbsent(item, it -> Maps.newHashMap()).put(CHARGING_ID, CHARGING);
    ItemProperties.PROPERTIES.computeIfAbsent(item, it -> Maps.newHashMap()).put(CHARGE_ID, CHARGE);
    ItemProperties.PROPERTIES.computeIfAbsent(item, it -> Maps.newHashMap()).put(CAST_ID, CAST);
  }

  /** Registers properties for a bow */
  public static void registerCrossbowProperties(ItemLike item) {
    registerToolProperties(item);
    ItemProperties.PROPERTIES.computeIfAbsent(item.asItem(), it -> Maps.newHashMap()).put(AMMO_ID, AMMO);
  }
}
