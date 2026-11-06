package slimeknights.tconstruct.library.tools.capability;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.component.TransientComponent;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Capability to allow an entity to store modifiers, used on projectiles fired from modifiable items */
public class EntityModifierCapability implements EntityComponentInitializer {
  /** Default instance to use with orElse */
  public static final EntityModifiers EMPTY = new EntityModifiers() {
    @Override
    public ModifierNBT getModifiers() {
      return ModifierNBT.EMPTY;
    }

    @Override
    public void setModifiers(ModifierNBT nbt) {}

    @Override
    public void addModifiers(ModifierNBT nbt) {}
  };

  public EntityModifierCapability() {}

  /* Static helpers */

  /** List of predicates to check if the entity supports this capability */
  private static final List<Predicate<Entity>> ENTITY_PREDICATES = new ArrayList<>();

  /** Capability ID */
  private static final ResourceLocation ID = TConstruct.getResource("modifiers");
  /** Capability type */
  public static final ComponentKey<EntityModifiers> CAPABILITY = ComponentRegistry.getOrCreate(ID, EntityModifiers.class);

  /** Gets the capability for the entity or an empty instance if missing */
  public static EntityModifiers getCapability(Entity entity) {
    return CAPABILITY.maybeGet(entity).orElse(EMPTY);
  }

  /** Gets the data or an empty instance if missing */
  public static ModifierNBT getOrEmpty(Entity entity) {
    return getCapability(entity).getModifiers();
  }

  /** Checks if the given entity supports this capability */
  public static boolean supportCapability(Entity entity) {
    for (Predicate<Entity> entityPredicate : ENTITY_PREDICATES) {
      if (entityPredicate.test(entity)) {
        return true;
      }
    }
    return false;
  }

  /** Registers a predicate of entites that need this capability */
  public static void registerEntityPredicate(Predicate<Entity> predicate) {
    ENTITY_PREDICATES.add(predicate);
  }

  /** Registers this capability with relevant busses */
  public static void register() {
    // components are registered through the CCA entrypoint (see fabric.mod.json), nothing to do here
  }

  @Override
  public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
    // TODO(fabric): CCA registers components per entity class, so predicate based entity types cannot be registered;
    // projectiles are the only 3.12.1 users of this capability
    registry.registerFor(Projectile.class, CAPABILITY, projectile -> new Provider());
  }

  /** Capability provider instance */
  private static class Provider implements EntityModifiers {
    @Getter @Setter
    private ModifierNBT modifiers = ModifierNBT.EMPTY;

    @Override
    public void readFromNbt(CompoundTag tag) {
      modifiers = ModifierNBT.readFromNBT(tag.getList("modifiers", Tag.TAG_LIST));
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
      tag.put("modifiers", modifiers.serializeToNBT());
    }
  }

  /** Interface for callers to use */
  public interface EntityModifiers extends TransientComponent {
    /** Gets the stored modifiers */
    ModifierNBT getModifiers();

    /** Sets the stored modifiers */
    void setModifiers(ModifierNBT nbt);

    /** Adds additional modifiers to the stored modifiers */
    default void addModifiers(ModifierNBT nbt) {
      ModifierNBT existing = getModifiers();
      if (existing.isEmpty()) {
        setModifiers(nbt);
      } else {
        setModifiers(ModifierNBT.builder().add(existing).add(nbt).build());
      }
    }
  }
}
