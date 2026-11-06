package slimeknights.tconstruct.library.tools;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.tools.TinkerTools;

import javax.annotation.Nullable;

/** Item entity that will never die */
public class IndestructibleItemEntity extends ItemEntity {
  /** Modifier key to make a tool spawn an indestructable entity */
  public static final ResourceLocation INDESTRUCTIBLE_ENTITY = TConstruct.getResource("indestructible");

  public IndestructibleItemEntity(EntityType<? extends IndestructibleItemEntity> entityType, Level world) {
    super(entityType, world);
    // TODO(fabric): Forge's ItemEntity#lifespan (set to Integer.MAX_VALUE so age can never reach it) has no vanilla
    //  equivalent; vanilla only offers the -32768 sentinel age through setUnlimitedLifetime(), which stops the spin
    //  animation that the lifespan field kept alive.
    this.setUnlimitedLifetime();
  }

  public IndestructibleItemEntity(Level worldIn, double x, double y, double z, ItemStack stack) {
    this(TinkerTools.indestructibleItem.get(), worldIn);
    this.setPos(x, y, z);
    this.setYRot(this.random.nextFloat() * 360.0F);
    this.setDeltaMovement(this.random.nextDouble() * 0.2D - 0.1D, 0.2D, this.random.nextDouble() * 0.2D - 0.1D);
    this.setItem(stack);
  }

  @Override
  public Packet<ClientGamePacketListener> getAddEntityPacket() {
    // TODO(fabric): Forge NetworkHooks.getEntitySpawningPacket has no Fabric or Porting Lib equivalent;
    //  the custom spawn packet has to be registered through Fabric networking by the entity slice
    return super.getAddEntityPacket();
  }

  /** Copies the pickup delay from another entity */
  public void setPickupDelayFrom(Entity reference) {
    if (reference instanceof ItemEntity itemEntity) {
      this.setPickUpDelay(itemEntity.pickupDelay);
    }
    setDeltaMovement(reference.getDeltaMovement());
  }

  @Override
  public boolean fireImmune() {
    return true;
  }

  @Override
  public boolean isInvulnerableTo(DamageSource pSource) {
    // prevent any damage besides out of world
    return !pSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
  }

  /** Checks if the given stack has a custom entity */
  public static boolean hasCustomEntity(ItemStack stack) {
    return ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY);
  }

  /**
   * Creates an indestructible item entity from the given item stack (if needed). Intended to be called from the item entity creation hook (TODO(fabric): no Fabric equivalent for custom dropped entities)
   * @param world     World instance
   * @param original  Original entity
   * @param stack     Stack to drop
   * @return  indestructible entity, or null if the stack is not marked indestructible
   */
  @Nullable
  public static Entity createFrom(Level world, Entity original, ItemStack stack) {
    if (ModifierUtil.checkVolatileFlag(stack, INDESTRUCTIBLE_ENTITY)) {
      IndestructibleItemEntity entity = new IndestructibleItemEntity(world, original.getX(), original.getY(), original.getZ(), stack);
      entity.setPickupDelayFrom(original);
      return entity;
    }
    return null;
  }
}
