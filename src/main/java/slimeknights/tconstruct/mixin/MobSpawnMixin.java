package slimeknights.tconstruct.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import slimeknights.tconstruct.library.json.loot.equipment.MobEquipmentManager;

/**
 * Spawn hook for mob equipment.
 *
 * <p>Upstream listened to Forge's {@code MobSpawnEvent.FinalizeSpawn} and cancelled vanilla finalization when it
 * applied generated equipment. Fabric has no cancellable spawn event, so the same callback is injected into
 * {@link Mob#finalizeSpawn} instead: an applied spawn group is returned from the mixin, which skips the vanilla
 * finalization exactly like the cancelled Forge event did.</p>
 */
@Mixin(Mob.class)
public class MobSpawnMixin {
  @Inject(method = "finalizeSpawn", at = @At("HEAD"), cancellable = true)
  private void tconstructFinalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, SpawnGroupData spawnData, CompoundTag spawnTag, CallbackInfoReturnable<SpawnGroupData> callback) {
    if (MobEquipmentManager.applyFinalizeSpawn((Mob) (Object) this, level, spawnType, spawnData, spawnTag)) {
      callback.setReturnValue(spawnData);
    }
  }
}
