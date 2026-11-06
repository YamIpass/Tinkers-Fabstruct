package slimeknights.tconstruct.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import slimeknights.tconstruct.gadgets.entity.FancyArmorStandEntity;

/**
 * Hook that lets the fancy armor stand replace the vanilla drop logic.
 * TODO(fabric): Forge widened ArmorStand#brokenByPlayer to protected via an access transformer, so upstream just
 *  overrides it. Vanilla keeps it final, so the override becomes an injection that delegates back to the fancy stand.
 */
@Mixin(ArmorStand.class)
public class ArmorStandMixin {
  @Inject(method = "brokenByPlayer", at = @At("HEAD"), cancellable = true)
  private void tconstruct$brokenByPlayer(DamageSource source, CallbackInfo callback) {
    if ((Object) this instanceof FancyArmorStandEntity stand) {
      stand.dropFancyStand(source);
      callback.cancel();
    }
  }
}
