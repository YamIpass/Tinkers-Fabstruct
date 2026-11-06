"""Rewires the three tool item classes to pass the incoming enchantment map into the hook.

Porting Lib patches EnchantmentHelper.getEnchantments to call CustomEnchantmentsItem#modifyEnchantments,
so pulling the enchantment map back out of the helper from inside that callback recurses forever.
"""
import io
import sys

FILES = [
    "src/main/java/slimeknights/tconstruct/library/tools/item/ModifiableItem.java",
    "src/main/java/slimeknights/tconstruct/library/tools/item/armor/ModifiableArmorItem.java",
    "src/main/java/slimeknights/tconstruct/library/tools/item/ranged/ModifiableLauncherItem.java",
]
OLD = "    enchantments.putAll(EnchantmentModifierHook.getAllEnchantments(stack));"
NEW = "    EnchantmentModifierHook.getAllEnchantments(stack, enchantments);"

changed = 0
for path in FILES:
    text = io.open(path, encoding="utf-8").read()
    count = text.count(OLD)
    if count:
        io.open(path, "w", encoding="utf-8", newline="").write(text.replace(OLD, NEW))
        changed += count
    print(path, count)
print("replaced", changed)
