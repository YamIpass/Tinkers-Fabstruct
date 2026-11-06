import re
p = 'build/resolved/tools/client/ModifierClientEvents.java'
t = open(p, 'rb').read().decode('utf-8')
NL = chr(10)
# --- body fixes
t = t.replace('if (event.getItemStack().getItem() instanceof IModifiableDisplay) {',
              'if (stack.getItem() instanceof IModifiableDisplay) {', 1)
t = t.replace('event.getToolTip().removeIf(text -> {', 'lines.removeIf(text -> {', 1)
t = t.replace('static void onTooltipEvent(ItemStack stack, TooltipFlag context, List<Component> lines) {',
              'static void onTooltipEvent(ItemStack stack, TooltipFlag tooltipFlag, List<Component> lines) {', 1)
t = t.replace('''  @SubscribeEvent
  static void playerLoggedOut(LoggingOut event) {''',
'''  static void playerLoggedOut(Minecraft client) {''', 1)
t = t.replace('''  @SubscribeEvent
  public static void renderHotbar(RenderGuiOverlayEvent.Post event) {''',
'''  public static void renderHotbar(GuiGraphics graphics, float partialTicks) {''', 1)
t = t.replace('    if (mc.options.hideGui || event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || player == null || player != mc.getCameraEntity()) {',
              '    if (mc.options.hideGui || player == null || player != mc.getCameraEntity()) {', 1)
t = t.replace('''      GuiGraphics graphics = event.getGuiGraphics();
      float partialTicks = event.getPartialTick();
''', '', 1)
# renderHand needs the player local back
t = t.replace('''  static void renderHand(RenderHandCallback.RenderHandEvent event) {
    InteractionHand hand = event.getHand();''',
'''  static void renderHand(RenderHandCallback.RenderHandEvent event) {
    Player player = event.getPlayer();
    InteractionHand hand = event.getHand();''', 1)
# --- init registrations
t = t.replace('''    FieldOfViewEvents.MODIFY.register(ModifierClientEvents::handleZoom);
  }''',
'''    FieldOfViewEvents.MODIFY.register(ModifierClientEvents::handleZoom);
    HudRenderCallback.EVENT.register(ModifierClientEvents::renderHotbar);
    ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> playerLoggedOut(client));
  }''', 1)
# --- imports: single sorted block
lines = t.split(NL)
starts = [i for i, l in enumerate(lines) if l.startswith('import ')]
end = starts[-1]
block = [l for l in lines[starts[0]:end + 1] if l.startswith('import ')]
extra = [
    'import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents',
    'import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback',
    'import net.minecraft.client.player.AbstractClientPlayer',
    'import net.minecraft.network.chat.Component',
    'import net.minecraft.world.item.TooltipFlag',
]
names = set(l[7:-1] for l in block)
for e in extra:
    if e[7:-1] not in names:
        block.append(e)
        names.add(e[7:-1])
block = sorted(block)
lines = lines[:starts[0]] + block + lines[end + 1:]
t = NL.join(lines)
open(p, 'wb').write(t.replace(NL, chr(13) + NL).encode('utf-8'))
print('ok')
