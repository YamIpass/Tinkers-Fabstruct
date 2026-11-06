package slimeknights.tconstruct.mixin;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.item.ranged.ModifiableBowItem;

import java.util.function.Predicate;

/**
 * Replacement for Forge's {@code LivingGetProjectileEvent}.
 * <p>
 * Upstream posts that event from this static helper to let the ballista swap the vanilla arrow for a held ballista
 * ammo item. Fabric and Porting Lib have no equivalent event, so this mixin applies the exact same rules at the
 * return of {@link ProjectileWeaponItem#getHeldProjectile(LivingEntity, Predicate)}. Only stacks that are arrows or
 * empty are replaced, everything else keeps the vanilla result.
 */
@Mixin(ProjectileWeaponItem.class)
public class ProjectileWeaponMixin {
  @Inject(method = "getHeldProjectile", at = @At("RETURN"), cancellable = true)
  private static void tconstruct$ballistaAmmo(LivingEntity entity, Predicate<ItemStack> predicate, CallbackInfoReturnable<ItemStack> callback) {
    ItemStack ammo = callback.getReturnValue();
    // only swap arrows or a missing projectile, other items are handled by the vanilla predicate
    if (ammo != null && !ammo.isEmpty() && !ammo.is(ItemTags.ARROWS)) {
      return;
    }
    ItemStack bow = entity.getUseItem();
    if (bow.isEmpty()) {
      bow = entity.getMainHandItem();
    }
    if (!bow.is(TinkerTags.Items.BALLISTAS) || !ModifierUtil.checkVolatileFlag(bow, ModifiableBowItem.KEY_BALLISTA)) {
      return;
    }
    int flag = ModifierUtil.getPersistentInt(bow, ModifiableBowItem.KEY_BALLISTA, 0);
    // requesting a held ballista or undecided: look in both hands, but never return the bow itself
    if (flag <= ModifiableBowItem.FLAG_BALLISTA_HELD) {
      ItemStack check = entity.getOffhandItem();
      if (check != bow && check.is(TinkerTags.Items.BALLISTA_AMMO)) {
        callback.setReturnValue(check);
        return;
      }
      check = entity.getMainHandItem();
      if (check != bow && check.is(TinkerTags.Items.BALLISTA_AMMO)) {
        callback.setReturnValue(check);
      }
    } else if (flag == ModifiableBowItem.FLAG_BALLISTA_QUIVER) {
      // the quiver modifier already supplied the ammo through its own hook
      callback.setReturnValue(ItemStack.EMPTY);
    }
  }
}
