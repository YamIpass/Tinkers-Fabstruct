import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib

N = chr(10)
BASE = 'src/main/java/slimeknights/tconstruct/tools/'


def load(rel):
    lines, nl = patchlib.read(BASE + rel)
    return N.join(lines), nl


def save(rel, text, nl):
    patchlib.write(BASE + rel, text.split(N), nl)
    print('wrote', rel, len(text.split(N)))


def sub(text, old, new, count=1):
    assert old in text, 'NOT FOUND: %r' % old[:150]
    return text.replace(old, new, count)


def add_imports(text, fqs):
    have = set()
    for ln in text.split(N):
        if ln.startswith('import '):
            have.add(ln[len('import '):].rstrip(';').strip().rsplit('.', 1)[-1])
    missing = [fq for fq in fqs if fq.rsplit('.', 1)[-1] not in have]
    if not missing:
        return text
    out, done = [], False
    for ln in text.split(N):
        if not done and ln.startswith('import slimeknights.'):
            out.extend('import %s;' % fq for fq in missing)
            done = True
        out.append(ln)
    assert done, 'no import block'
    return N.join(out)


def rm_imports(text, fqs):
    drop = set(fqs)
    return N.join(ln for ln in text.split(N) if ln[len('import '):].rstrip(';').strip() not in drop)


# ---------------------------------------------------------------- FluidEffectProjectile
rel = 'entity/FluidEffectProjectile.java'
text, nl = load(rel)
text = rm_imports(text, [
    'net.minecraftforge.common.capabilities.ForgeCapabilities',
    'net.minecraftforge.event.ForgeEventFactory',
    'net.minecraftforge.items.IItemHandler',
    'net.minecraftforge.items.IItemHandlerModifiable',
])
text = add_imports(text, [
    'io.github.fabricators_of_create.porting_lib.entity.events.ProjectileImpactEvent',
    'io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage',
    'net.fabricmc.fabric.api.transfer.v1.item.ItemStorage',
    'net.fabricmc.fabric.api.transfer.v1.item.ItemVariant',
    'net.fabricmc.fabric.api.transfer.v1.storage.Storage',
])
text = sub(text, '''  private IItemHandlerModifiable getCannonInventory() {
    Level level = level();
    if (this.cannon != null && level.isLoaded(this.cannon)) {
      BlockEntity cannonBE = level.getBlockEntity(this.cannon);
      if (cannonBE != null && cannonBE.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(EmptyItemHandler.INSTANCE) instanceof IItemHandlerModifiable modifiable) {
        return modifiable;
      }
    }
    return null;
  }''', '''  private SlottedStackStorage getCannonInventory() {
    Level level = level();
    if (this.cannon != null && level.isLoaded(this.cannon)) {
      // TODO(fabric): Forge exposed the cannon inventory as a capability, Fabric exposes a sided item storage
      Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, this.cannon, null);
      if (storage instanceof SlottedStackStorage slots) {
        return slots;
      }
    }
    return null;
  }''')
text = sub(text, '''      IItemHandler handler = getCannonInventory();''', '''      SlottedStackStorage handler = getCannonInventory();''')
text = sub(text, '''      IItemHandlerModifiable handler = getCannonInventory();''', '''      SlottedStackStorage handler = getCannonInventory();''')
text = sub(text, '''    if (hitType != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hitResult)) {
      this.onHit(hitResult);
    }''', '''    if (hitType != HitResult.Type.MISS) {
      ProjectileImpactEvent impactEvent = new ProjectileImpactEvent(this, hitResult);
      impactEvent.sendEvent();
      if (impactEvent.getImpactResult() != ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE) {
        this.onHit(hitResult);
      }
    }''')
save(rel, text, nl)

# ---------------------------------------------------------------- SlimeskullItem
rel = 'item/SlimeskullItem.java'
text, nl = load(rel)
text = rm_imports(text, [
    'net.minecraftforge.client.extensions.common.IClientItemExtensions',
    'slimeknights.tconstruct.library.client.armor.ArmorModelManager.ArmorModelDispatcher',
    'java.util.function.Consumer',
])
text = add_imports(text, [
    'io.github.fabricators_of_create.porting_lib.client.armor.ArmorRenderer',
    'io.github.fabricators_of_create.porting_lib.client.armor.ArmorRendererRegistry',
    'io.github.fabricators_of_create.porting_lib.util.EnvExecutor',
    'net.fabricmc.api.EnvType',
    'net.fabricmc.api.Environment',
    'com.mojang.blaze3d.vertex.PoseStack',
    'net.minecraft.client.renderer.MultiBufferSource',
])
text = sub(text, '''    super(material, ArmorItem.Type.HELMET, properties);
    this.name = name;
  }''', '''    super(material, ArmorItem.Type.HELMET, properties);
    this.name = name;
    EnvExecutor.runWhenOn(EnvType.CLIENT, () -> this::initializeClient);
  }''')
text = sub(text, '''  @Override
  public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new ArmorModelDispatcher() {
      @Override
      protected ResourceLocation getName() {
        return name;
      }

      @Nonnull
      @Override
      public Model getGenericArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        return SlimeskullArmorModel.INSTANCE.setup(living, stack, original, getModel(stack));
      }
    });
  }
}''', '''  @Environment(EnvType.CLIENT)
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
}''')
text = add_imports(text, ['net.minecraft.client.renderer.texture.OverlayTexture'])
save(rel, text, nl)

# ---------------------------------------------------------------- HelmetChargingEffect
rel = 'modifiers/effect/HelmetChargingEffect.java'
text, nl = load(rel)
text = rm_imports(text, [
    'net.minecraftforge.client.extensions.common.IClientMobEffectExtensions',
    'java.util.function.Consumer',
])
text = sub(text, '''  @Override
  public void initializeClient(Consumer<IClientMobEffectExtensions> consumer) {''', '''  // TODO(fabric): Forge's IClientMobEffectExtensions has no Fabric equivalent yet; Porting Lib ships a
  //  MobEffectRenderer interface but no registry for it, so the custom helmet charging icon is not rendered
  @SuppressWarnings("unused")
  private void initializeClient() {''')
save(rel, text, nl)

# ---------------------------------------------------------------- SeveringModule
rel = 'modules/combat/SeveringModule.java'
text, nl = load(rel)
text = sub(text, 'public enum SeveringModule implements ModifierModule, ProcessLootModifierHook {' + N,
           'public enum SeveringModule implements ModifierModule, ProcessLootModifierHook {' + N
           + '  // TODO(fabric): forge:heads is a Forge convention tag; its data file still needs to be ported (data/forge/tags/items/heads.json)' + N
           + '  private static final TagKey<Item> HEADS = TagKey.create(Registries.ITEM, new ResourceLocation("forge", "heads"));' + N)
save(rel, text, nl)

# ---------------------------------------------------------------- TankInteractionModule
rel = 'modules/interaction/TankInteractionModule.java'
text, nl = load(rel)
text = rm_imports(text, [
    'net.minecraftforge.common.capabilities.ForgeCapabilities',
    'net.minecraftforge.fluids.capability.IFluidHandler',
    'slimeknights.mantle.util.LogicHelper',
])
text = add_imports(text, [
    'net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage',
    'net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant',
    'net.fabricmc.fabric.api.transfer.v1.storage.Storage',
    'net.fabricmc.fabric.api.transfer.v1.storage.StorageView',
    'net.fabricmc.fabric.api.transfer.v1.transaction.Transaction',
])
text = sub(text, '''    Direction face = context.getClickedFace();
    IFluidHandler cap = LogicHelper.orElseNull(te.getCapability(ForgeCapabilities.FLUID_HANDLER, face));''',
           '''    Direction face = context.getClickedFace();
    // TODO(fabric): Forge exposed the block fluid handler as a capability, Fabric exposes a sided fluid storage
    Storage<FluidVariant> cap = FluidStorage.SIDED.find(world, target, face);''')
text = sub(text, '''          int added = cap.fill(fluidStack, false);''',
           '''          int added;
          try (Transaction transaction = Transaction.openOuter()) {
            added = (int) cap.insert(FluidVariant.of(fluidStack.getFluid()), fluidStack.getAmount(), transaction);
            transaction.commit();
          }''')
text = sub(text, '''        FluidStack drained = cap.drain(TANK_HELPER.getCapacity(tool), false);''',
           '''        FluidStack drained;
        try (Transaction transaction = Transaction.openOuter()) {
          FluidVariant variant = firstFluid(cap);
          long amount = variant == null ? 0 : cap.extract(variant, TANK_HELPER.getCapacity(tool), transaction);
          drained = amount > 0 ? new FluidStack(variant, amount) : FluidStack.EMPTY;
          transaction.commit();
        }''')
text = sub(text, '''        FluidStack drained = cap.drain(new FluidStack(fluidStack, TANK_HELPER.getCapacity(tool) - fluidStack.getAmount()), false);''',
           '''        FluidStack drained;
        try (Transaction transaction = Transaction.openOuter()) {
          long amount = cap.extract(FluidVariant.of(fluidStack.getFluid()), TANK_HELPER.getCapacity(tool) - fluidStack.getAmount(), transaction);
          drained = amount > 0 ? new FluidStack(fluidStack, amount) : FluidStack.EMPTY;
          transaction.commit();
        }''')
text = sub(text, '''    return InteractionResult.sidedSuccess(world.isClientSide);
  }
}''', '''    return InteractionResult.sidedSuccess(world.isClientSide);
  }

  /** Gets the first fluid in the given storage, or null if empty */
  @Nullable
  private static FluidVariant firstFluid(Storage<FluidVariant> storage) {
    for (StorageView<FluidVariant> view : storage.nonEmptyViews()) {
      return view.getResource();
    }
    return null;
  }
}''')
save(rel, text, nl)

print('done')
