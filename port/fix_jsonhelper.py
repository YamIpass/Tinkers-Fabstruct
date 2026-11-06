"""Normalise JsonHelper usage in the library/client slice.

The mechanical rename turned Mojang's GsonHelper into yarn's JsonHelper, but Mojang
uses getAsX/convertToJsonObject names while yarn uses getX/asObject, and Mantle also
ships a slimeknights.mantle.util.JsonHelper with extra helpers. Result: ambiguous
imports and unknown methods. This script:

  * keeps ONE imported type: net.minecraft.util.JsonHelper
  * renames Mojang-style calls to yarn-style calls
  * rewrites Mantle-only helpers to fully qualified calls

Usage: py.sh port/fix_jsonhelper.py [--apply]
"""
import os
import re
import sys

ROOT = os.path.join(
    os.path.expanduser("~"), "Documents", "deepseek-harness", "default-workspace",
    "tconstruct-fabric", "src", "main", "java", "slimeknights", "tconstruct", "library", "client",
)
SKIP = {"Icons.java", "GuiUtil.java"}
MAN = "slimeknights.mantle.util.JsonHelper"

RENAMES = [
    ("JsonHelper.getAsString(", "JsonHelper.getString("),
    ("JsonHelper.getAsBoolean(", "JsonHelper.getBoolean("),
    ("JsonHelper.getAsJsonArray(", "JsonHelper.getArray("),
    ("JsonHelper.getAsJsonObject(", "JsonHelper.getObject("),
    ("JsonHelper.getAsInt(", "JsonHelper.getInt("),
    ("JsonHelper.getAsFloat(", "JsonHelper.getFloat("),
    ("JsonHelper.getAsDouble(", "JsonHelper.getDouble("),
    ("JsonHelper.getAsLong(", "JsonHelper.getLong("),
    ("JsonHelper.convertToJsonObject(", "JsonHelper.asObject("),
    ("JsonHelper.convertToFloat(", "JsonHelper.asFloat("),
    # Mantle-only helpers keep working through a fully qualified call
    ("JsonHelper.getResourceLocation(", MAN + ".getResourceLocation("),
    ("JsonHelper.parseResourceLocation(", MAN + ".convertToResourceLocation("),
    ("JsonHelper.parseList(", MAN + ".parseList("),
    ("JsonHelper.getFileInAllDomainsAndPacks(", MAN + ".getFileInAllDomainsAndPacks("),
    ("JsonHelper.DEFAULT_GSON", MAN + ".DEFAULT_GSON"),
]

apply = "--apply" in sys.argv
changed = 0
for dirpath, _dirnames, filenames in os.walk(ROOT):
    for fn in filenames:
        if not fn.endswith(".java") or fn in SKIP:
            continue
        path = os.path.join(dirpath, fn)
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        orig = text
        if "import " + MAN + ";" in text:
            text = text.replace("import " + MAN + ";\n", "")
            text = text.replace("import " + MAN + ";", "")
            if "import net.minecraft.util.JsonHelper;" not in text:
                # insert after the package line's import block end
                m = list(re.finditer(r"^import .*;$", text, re.M))
                if m:
                    pos = m[-1].end()
                    text = text[:pos] + "\nimport net.minecraft.util.JsonHelper;" + text[pos:]
                else:
                    text = re.sub(r"(^package .*;$)", r"\1\n\nimport net.minecraft.util.JsonHelper;", text, count=1, flags=re.M)
        for old, new in RENAMES:
            text = text.replace(old, new)
        if text != orig:
            changed += 1
            rel = os.path.relpath(path, ROOT)
            print(("applied " if apply else "would fix ") + rel)
            if apply:
                with open(path, "w", encoding="utf-8", newline="") as fh:
                    fh.write(text)
print("-- files:", changed)
