"""Ports DoubleJumpHandler to Fabric events (no reference counterpart: 3.12.1-only file)."""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import patchlib as P

REL = 'src/main/java/slimeknights/tconstruct/tools/logic/DoubleJumpHandler.java'
lines, nl = P.read(REL)

lines = P.rm_imports(lines, [
    'net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent',
    'net.minecraftforge.event.entity.living.LivingFallEvent',
    'net.minecraftforge.eventbus.api.SubscribeEvent',
    'net.minecraftforge.fml.common.Mod.EventBusSubscriber',
    'net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus',
])
body = nl.join(lines)
body = body.replace('@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = Bus.FORGE)' + nl, '')
body = body.replace('  @SubscribeEvent' + nl, '')
body = body.replace(
    '  private DoubleJumpHandler() {}' + nl,
    '  private DoubleJumpHandler() {}' + nl + nl +
    '  /** Registers the event handlers, replacement for the Forge event bus subscriber */' + nl +
    '  public static void init() {' + nl +
    '    LivingEntityEvents.LivingJumpEvent.JUMP.register(DoubleJumpHandler::onJump);' + nl +
    '    LivingEntityEvents.FALL.register(DoubleJumpHandler::onLand);' + nl +
    '  }' + nl)
body = body.replace('static void onJump(LivingJumpEvent event)', 'static void onJump(LivingEntityEvents.LivingJumpEvent event)')
body = body.replace('static void onLand(LivingFallEvent event)', 'static void onLand(FallEvent event)')
body = body.replace('living.getCapability(PersistentDataCapability.CAPABILITY).ifPresent(data -> data.remove(JUMPS));',
                    'PersistentDataCapability.CAPABILITY.maybeGet(living).ifPresent(data -> data.remove(JUMPS));')
body = body.replace('event.getEntity().getCapability(PersistentDataCapability.CAPABILITY).ifPresent(data -> data.remove(JUMPS));',
                    'PersistentDataCapability.CAPABILITY.maybeGet(event.getEntity()).ifPresent(data -> data.remove(JUMPS));')
lines = body.split(nl)
lines = P.add_imports(lines, [
    'io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents',
    'io.github.fabricators_of_create.porting_lib.entity.events.LivingEntityEvents.Fall.FallEvent',
])
P.write(REL, lines, nl)
print('done', len(lines), 'lines')
