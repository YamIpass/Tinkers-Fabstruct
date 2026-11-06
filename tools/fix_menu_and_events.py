import os, sys
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
import patchlib

NL = chr(10)
ROOT = 'src/main/java/slimeknights/tconstruct/tools/'
fails, done = [], []


def patch(rel, edits, drop=(), add=()):
  try:
    lines, nl = patchlib.read(ROOT + rel)
    text = NL.join(lines)
    for old, new in edits:
      if old not in text:
        raise AssertionError('missing: ' + old[:80])
      text = text.replace(old, new)
    lines = text.split(NL)
    if drop:
      lines = patchlib.rm_imports(lines, list(drop))
    if add:
      lines = patchlib.add_imports(lines, list(add))
    patchlib.write(ROOT + rel, lines, nl)
    done.append(rel)
  except Exception as e:
    fails.append((rel, str(e)))


# --- item handler storage instead of the Forge capability (see tools/CAPABILITIES.md)
patch('menu/ToolContainerMenu.java',
      [('  private final IItemHandler itemHandler;', '  private final SlottedStackStorage itemHandler;'),
       ('public ToolContainerMenu(int id, Inventory playerInventory, ItemStack stack, IItemHandler itemHandler, int slotIndex) {',
        'public ToolContainerMenu(int id, Inventory playerInventory, ItemStack stack, SlottedStackStorage itemHandler, int slotIndex) {'),
       ('protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, IItemHandler handler, int slotIndex) {',
        'protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, SlottedStackStorage handler, int slotIndex) {'),
       ('    IItemHandler handler;' + NL
        + '    if (stack.hasTag() && stack.is(TinkerTags.Items.MODIFIABLE)) {' + NL
        + '      handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).filter(cap -> cap instanceof IItemHandlerModifiable).orElse(EmptyItemHandler.INSTANCE);' + NL
        + '      // wrong number of slots means something went wrong, use a dummy' + NL
        + '      if (handler.getSlots() != size) {',
        '    SlottedStackStorage handler;' + NL
        + '    if (stack.hasTag() && stack.is(TinkerTags.Items.MODIFIABLE)) {' + NL
        + '      Storage<ItemVariant> found = ItemItemStorages.ITEM.find(stack, ContainerItemContext.withConstant(stack));' + NL
        + '      handler = found instanceof SlottedStackStorage storage ? storage : EmptyItemHandler.INSTANCE;' + NL
        + '      // wrong number of slots means something went wrong, use a dummy' + NL
        + '      if (handler.getSlotCount() != size) {'),
       ('protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, IItemHandler handler, int slotIndex, CraftingType craftingType, boolean includeOffhand) {',
        'protected ToolContainerMenu(@Nullable MenuType<?> type, int id, Inventory playerInventory, ItemStack stack, SlottedStackStorage handler, int slotIndex, CraftingType craftingType, boolean includeOffhand) {'),
       ('    int slots = itemHandler.getSlots();', '    int slots = itemHandler.getSlotCount();'),
       ('    public ToolContainerSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {',
        '    public ToolContainerSlot(SlottedStackStorage itemHandler, int index, int xPosition, int yPosition) {'),
       ('      ((IItemHandlerModifiable) this.getItemHandler()).setStackInSlot(index, stack);',
        '      ((SlottedStackStorage) this.getItemHandler()).setStackInSlot(index, stack);')],
      drop=['net.minecraftforge.common.capabilities.ForgeCapabilities', 'net.minecraftforge.items.IItemHandler',
            'net.minecraftforge.items.IItemHandlerModifiable'],
      add=['io.github.fabricators_of_create.porting_lib.transfer.item.ItemItemStorages',
           'io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage',
           'net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext',
           'net.fabricmc.fabric.api.transfer.v1.item.ItemVariant',
           'net.fabricmc.fabric.api.transfer.v1.storage.Storage'])

# --- lazy + break speed event replacements
patch('modifiers/traits/DamageSpeedTradeModifier.java', [],
      drop=['net.minecraftforge.common.util.Lazy', 'net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed'],
      add=['io.github.fabricators_of_create.porting_lib.common.util.Lazy',
           'io.github.fabricators_of_create.porting_lib.entity.events.PlayerEvents.BreakSpeed'])

# --- milk finish hook through the Porting Lib event
patch('modifiers/traits/skull/StrongBonesModifier.java',
      [('  public StrongBonesModifier() {' + NL
        + '    // TODO: move this out of constructor to generalized logic' + NL
        + '    MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEntityUseItemEvent.Finish.class, StrongBonesModifier::onItemFinishUse);' + NL
        + '  }',
        '  public StrongBonesModifier() {' + NL
        + '    // TODO: move this out of constructor to generalized logic' + NL
        + '    LivingEntityUseItemEvents.LIVING_USE_ITEM_FINISH.register((entity, stack, remaining, result) -> {' + NL
        + '      onItemFinishUse(entity, result);' + NL
        + '      return result;' + NL
        + '    });' + NL
        + '  }'),
       ('      didSomething = simulate.execute() ? living.addEffect(effect) : living.canBeAffected(effect);',
        '      didSomething = !simulate ? living.addEffect(effect) : living.canBeAffected(effect);'),
       ('      didSomething |= simulate.execute() ? living.addEffect(effect) : living.canBeAffected(effect);',
        '      didSomething |= !simulate ? living.addEffect(effect) : living.canBeAffected(effect);'),
       ('  private static void onItemFinishUse(LivingEntityUseItemEvent.Finish event) {' + NL
        + '    LivingEntity living = event.getEntity();' + NL
        + '    if (event.getItem().getItem() == Items.MILK_BUCKET) {',
        '  private static void onItemFinishUse(LivingEntity living, ItemStack item) {' + NL
        + '    if (item.getItem() == Items.MILK_BUCKET) {')],
      drop=['net.minecraftforge.common.MinecraftForge', 'net.minecraftforge.event.entity.living.LivingEntityUseItemEvent',
            'net.minecraftforge.eventbus.api.EventPriority'],
      add=['io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityUseItemEvents'])

# --- boolean simulate replaces the FluidAction enum
patch('modules/ranged/ammo/SmashingModule.java',
      [('    if (simulate.execute()) {', '    if (!simulate) {'),
       ('          if (simulate.execute()) {', '          if (!simulate) {'),
       ('            if (simulate.execute()) {', '            if (!simulate) {')])

print('OK', len(done))
for d in done:
  print('  done', d)
print('FAIL', len(fails))
for f, e in fails:
  print('  fail', f, '->', e)
