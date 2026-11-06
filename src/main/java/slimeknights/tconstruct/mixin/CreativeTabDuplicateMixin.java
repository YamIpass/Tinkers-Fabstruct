package slimeknights.tconstruct.mixin;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tolerates duplicate entries in a creative tab, matching Forge patched behavior.
 * <p>Vanilla throws {@code IllegalStateException} when the same stack is added twice to one tab.
 * Tinkers adds a few items twice on purpose (for example the pattern item in both the tool and
 * tool parts sections), which Forge allows and simply ignores.</p>
 */
@Mixin(targets = "net.minecraft.world.item.CreativeModeTab$ItemDisplayBuilder")
public class CreativeTabDuplicateMixin {
  @Inject(method = "accept", at = @At("HEAD"), cancellable = true)
  private void tconstruct$skipDuplicates(ItemStack stack, CreativeModeTab.TabVisibility visibility, CallbackInfo callback) {
    CreativeModeTab.ItemDisplayBuilder builder = (CreativeModeTab.ItemDisplayBuilder) (Object) this;
    if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY && builder.tabContents.contains(stack)) {
      callback.cancel();
    }
  }
}
