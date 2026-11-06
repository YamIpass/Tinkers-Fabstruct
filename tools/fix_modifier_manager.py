"""Port ModifierManager + JsonRedirect off Forge crafting conditions."""
import pathlib, re, sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BASE = ROOT / "src/main/java/slimeknights/tconstruct"

# ---------- JsonRedirect ----------
p = BASE / "library/json/JsonRedirect.java"
s = p.read_text(encoding="utf-8")
s = s.replace(
    "// TODO(fabric): net.minecraftforge.* import below has no Fabric/Porting Lib equivalent; kept verbatim for a later compat layer.\n"
    "import net.minecraftforge.common.crafting.CraftingHelper;\n"
    "import net.minecraftforge.common.crafting.conditions.ICondition;\n", "")
s = s.replace("import com.google.gson.JsonObject;",
              "import com.google.gson.JsonElement;\nimport com.google.gson.JsonObject;")
s = s.replace("/** Represents a redirect in a material or modifier JSON */",
              "/**\n * Represents a redirect in a material or modifier JSON.\n * TODO(fabric): Forge crafting conditions have no Fabric equivalent, so the condition is kept as raw JSON and is\n * not evaluated by the port yet.\n */")
s = s.replace("  @Nullable\n  private final ICondition condition;",
              "  @Nullable\n  private final JsonObject condition;")
s = s.replace("""    if (condition != null) {
      json.add("condition", CraftingHelper.serialize(condition));
    }""",
              """    if (condition != null) {
      json.add("condition", condition);
    }""")
s = s.replace("""    ICondition condition = null;
    if (json.has("condition")) {
      condition = CraftingHelper.getCondition(json);
    }""",
              """    JsonObject condition = null;
    JsonElement conditionElement = json.get("condition");
    if (conditionElement != null && conditionElement.isJsonObject()) {
      condition = conditionElement.getAsJsonObject();
    }""")
p.write_text(s, encoding="utf-8")

# ---------- ModifierManager ----------
p = BASE / "library/modifiers/ModifierManager.java"
s = p.read_text(encoding="utf-8")

# imports
drop_imports = [
    "import net.minecraftforge.common.MinecraftForge;\n",
    "import net.minecraftforge.common.crafting.CraftingHelper;\n",
    "import net.minecraftforge.common.crafting.conditions.ICondition;\n",
    "import net.minecraftforge.common.crafting.conditions.ICondition.IContext;\n",
    "import net.minecraftforge.event.AddReloadListenerEvent;\n",
    "import net.minecraftforge.event.OnDatapackSyncEvent;\n",
    "import net.minecraftforge.eventbus.api.Event;\n",
    "import net.minecraftforge.fml.ModLoader;\n",
    "import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;\n",
    "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n",
    "import net.minecraftforge.fml.loading.FMLLoader;\n",
]
for imp in drop_imports:
    s = s.replace(imp, "")

anchor = "import net.minecraft.util.profiler.Profiler;"
assert anchor in s
s = s.replace(anchor, anchor + """
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import slimeknights.tconstruct.common.network.TinkerNetwork;
import slimeknights.tconstruct.library.network.UpdateModifiersPacket;
import io.github.fabricators_of_create.porting_lib.core.event.BaseEvent;""", 1)

# condition context field
s = s.replace("  private IContext conditionContext = IContext.EMPTY;\n", "")

# init()
old_init = re.search(r"  public void init\(\) \{\n(?:.*?\n)*?  \}\n", s)
assert old_init, "init() not found"
new_init = """  public void init() {
    ResourceManagerHelper helper = ResourceManagerHelper.get(ResourceType.SERVER_DATA);
    helper.registerReloadListener(new IdentifiableListener("modifiers", this));
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> JsonUtils.syncPackets(handler.player, false,
      new UpdateModifiersPacket(this.dynamicModifiers, this.tags, this.enchantmentMap, this.enchantmentTagMap)));
    // TODO(fabric): Forge mod event bus has no Fabric equivalent; addon registration runs eagerly at construction
    this.fireRegistryEvent();
  }
"""
s = s[:old_init.start()] + new_init + s[old_init.end():]

# fireRegistryEvent: drop ModLoader event generator
s = s.replace("    ModLoader.get().runEventGenerator(ModifierRegistrationEvent::new);\n",
              "    // TODO(fabric): Forge mod event bus has no Fabric equivalent\n")

# addDataPackListeners method removal
m = re.search(r"  /\*\* Adds listeners to the data pack[^\n]*\n  private void addDataPackListeners\(AddReloadListenerEvent event\) \{\n(?:.*?\n)*?  \}\n\n", s)
if m:
    s = s[:m.start()] + s[m.end():]

# production check
s = s.replace("FMLLoader.isProduction()", "!FabricLoader.getInstance().isDevelopmentEnvironment()")

# redirect condition check
s = s.replace("""          ICondition redirectCondition = redirect.getCondition();
          if (redirectCondition == null || redirectCondition.test(conditionContext)) {""",
              """          // TODO(fabric): condition is raw JSON in the port, so redirects are always taken
          if (true) {""")

# json condition check
s = s.replace("""      if (json.has("condition") && !CraftingHelper.getCondition(net.minecraft.util.JsonHelper.getObject(json, "condition")).test(conditionContext)) {
        return null;
      }""",
              """      // TODO(fabric): crafting conditions are not evaluated by the port, so conditional modifiers always load""")

# context builder usage
s = s.replace("contextBuilder(key).put(ContextKey.CONDITION_CONTEXT, conditionContext).build()",
              "contextBuilder(key).build()")

# loaded events
s = s.replace("MinecraftForge.EVENT_BUS.post(new ModifiersLoadedEvent());",
              "new ModifiersLoadedEvent().sendEvent();")
s = s.replace("public static class ModifiersLoadedEvent extends Event {}",
              "public static class ModifiersLoadedEvent extends BaseEvent {\n    /** TODO(fabric): no listener list yet; posting is a no-op */\n    public void sendEvent() {}\n  }")

# append identifiable listener record at end of class (before final closing brace)
assert s.rstrip().endswith("}")
idx = s.rstrip().rfind("}")
s = s[:idx] + """
  /** Wrapper so the reload listener can expose a Fabric id */
  private record IdentifiableListener(String id, net.minecraft.resource.ResourceReloader listener) implements IdentifiableResourceReloadListener {
    @Override
    public Identifier getFabricId() {
      return new Identifier("tconstruct", id);
    }

    @Override
    public java.util.concurrent.CompletableFuture<?> reload(net.minecraft.resource.ResourceManager manager, net.minecraft.util.profiler.Profiler profiler, java.util.concurrent.Executor executor, java.util.concurrent.Executor executor2) {
      return listener.reload(manager, profiler, executor, executor2);
    }
  }
""" + s[idx:]

p.write_text(s, encoding="utf-8")
print("patched ModifierManager + JsonRedirect")
