"""Replace Forge's RegisterClientReloadListenersEvent registration with Fabric's
ResourceManagerHelper in the five library/client reload listeners.

Usage: py.sh port/fix_reload_listeners.py [--apply]
"""
import os
import re
import sys

ROOT = os.path.join(
    os.path.expanduser("~"), "Documents", "deepseek-harness", "default-workspace",
    "tconstruct-fabric", "src", "main", "java", "slimeknights", "tconstruct", "library", "client",
)
FABRIC_IMPORTS = (
    "import net.fabricmc.fabric.api.resource.ResourceManagerHelper;\n"
    "import net.fabricmc.fabric.api.resource.ResourceType;\n"
)

SIMPLE_INIT = """  public static void init() {
    // TODO(fabric): Forge fired RegisterClientReloadListenersEvent to register reload listeners; Fabric registers them directly.
    ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(%s);
  }"""

FILES = {
    "armor/ArmorModelManager.java": [
        (
            "  /**\n   * Initializes this manager, registering it with the resource manager\n   * @param manager  Manager\n   */\n"
            "  public static void init(RegisterClientReloadListenersEvent manager) {\n"
            "    manager.registerReloadListener(INSTANCE);\n  }",
            SIMPLE_INIT % "INSTANCE",
        ),
    ],
    "materials/MaterialTooltipCache.java": [
        (
            "  /** Called during the event to initialize the cache invalidators */\n"
            "  public static void init(RegisterClientReloadListenersEvent manager)  {\n"
            "    manager.registerReloadListener(RELOAD_LISTENER);\n  }",
            SIMPLE_INIT % "RELOAD_LISTENER",
        ),
    ],
    "model/DynamicTextureLoader.java": [
        (
            "  /** Registers this manager */\n"
            "  public static void init(RegisterClientReloadListenersEvent event) {\n"
            "    event.registerReloadListener(INSTANCE);\n  }",
            SIMPLE_INIT % "INSTANCE",
        ),
    ],
    "modifiers/ModifierModelManager.java": [
        (
            "  /**\n   * Initializes this manager, registering it with the resource manager\n   * @param manager  Manager\n   */\n"
            "  public static void init(RegisterClientReloadListenersEvent manager) {\n"
            "    manager.registerReloadListener(INSTANCE);\n  }",
            SIMPLE_INIT % "INSTANCE",
        ),
    ],
    "modifiers/ModifierIconManager.java": [
        (
            "  /**\n   * Initializes this manager, registering it relevant event busses\n   */\n"
            "  public static void init() {\n"
            "    IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();\n"
            "    bus.addListener(ModifierIconManager::onResourceManagerRegister);\n"
            "  }\n\n"
            "  /** Called on resource manager build to add the manager */\n"
            "  private static void onResourceManagerRegister(RegisterClientReloadListenersEvent manager) {\n"
            "    manager.registerReloadListener(INSTANCE);\n  }",
            SIMPLE_INIT % "INSTANCE",
        ),
    ],
}

DROP_IMPORTS = (
    "import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;\n",
    "import net.minecraftforge.eventbus.api.IEventBus;\n",
    "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n",
)

apply = "--apply" in sys.argv
for rel, pairs in FILES.items():
    path = os.path.join(ROOT, rel)
    with open(path, encoding="utf-8") as fh:
        text = fh.read()
    orig = text
    for old, new in pairs:
        if old not in text:
            print("MISS", rel)
            continue
        text = text.replace(old, new)
    for imp in DROP_IMPORTS:
        text = text.replace(imp, "")
    if "ResourceManagerHelper" not in text:
        text = text.replace("import net.minecraft.client.", FABRIC_IMPORTS + "import net.minecraft.client.", 1)
    if text != orig:
        print(("applied " if apply else "would fix ") + rel)
        if apply:
            with open(path, "w", encoding="utf-8", newline="") as fh:
                fh.write(text)
    else:
        print("unchanged " + rel)
