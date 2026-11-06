"""Second mechanical rename pass for the library/client port.

Rewrites remaining Mojang/Forge imports to their yarn/Fabric/Porting-Lib/Mantle
counterparts and renames the simple names in the file body when needed.
Skips Icons.java and GuiUtil.java (ported by another agent).
"""
import os
import re
import sys

ROOT = os.path.join("src", "main", "java", "slimeknights", "tconstruct", "library", "client")
SKIP = {"Icons.java", "GuiUtil.java"}

PL = "io.github.fabricators_of_create.porting_lib"
MANTLE = "slimeknights.mantle"

# old fqcn -> (new fqcn, old simple name, new simple name)
IMPORTS = {
    "com.mojang.math.Transformation": ("net.minecraft.util.math.AffineTransformation", "Transformation", "AffineTransformation"),
    "net.minecraft.client.color.item.ItemColor": ("net.minecraft.client.color.item.ItemColorProvider", "ItemColor", "ItemColorProvider"),
    "net.minecraftforge.fluids.FluidStack": (PL + ".fluids.FluidStack", "FluidStack", "FluidStack"),
    "net.minecraftforge.fluids.FluidType": (PL + ".fluids.FluidType", "FluidType", "FluidType"),
    "net.minecraftforge.fluids.FluidUtil": (PL + ".util.FluidUtil", "FluidUtil", "FluidUtil"),
    "net.minecraftforge.fluids.capability.templates.FluidTank": (PL + ".transfer.fluid.FluidTank", "FluidTank", "FluidTank"),
    "net.minecraftforge.common.ToolActions": (PL + ".tool.ToolActions", "ToolActions", "ToolActions"),
    "net.minecraftforge.client.model.data.ModelData": (MANTLE + ".client.model.ModelData", "ModelData", "ModelData"),
    "net.minecraftforge.client.model.data.ModelProperty": (MANTLE + ".client.model.ModelProperty", "ModelProperty", "ModelProperty"),
    "net.minecraftforge.client.model.geometry.IGeometryLoader": (PL + ".models.geometry.IGeometryLoader", "IGeometryLoader", "IGeometryLoader"),
    "net.minecraftforge.client.model.geometry.IUnbakedGeometry": (PL + ".models.geometry.IUnbakedGeometry", "IUnbakedGeometry", "IUnbakedGeometry"),
    "net.minecraftforge.client.model.geometry.IGeometryBakingContext": (PL + ".models.geometry.IGeometryBakingContext", "IGeometryBakingContext", "IGeometryBakingContext"),
    "net.minecraftforge.client.model.SimpleModelState": (PL + ".models.geometry.SimpleModelState", "SimpleModelState", "SimpleModelState"),
    "net.minecraftforge.client.model.CompositeModel": (PL + ".models.CompositeModel", "CompositeModel", "CompositeModel"),
    "net.minecraftforge.client.model.QuadTransformers": (PL + ".models.QuadTransformers", "QuadTransformers", "QuadTransformers"),
    "net.minecraftforge.client.model.DynamicFluidContainerModel": (PL + ".models.DynamicFluidContainerModel", "DynamicFluidContainerModel", "DynamicFluidContainerModel"),
    "net.minecraft.data.PackOutput": ("net.minecraft.data.DataOutput", "PackOutput", "DataOutput"),
    "net.minecraft.data.CachedOutput": ("net.minecraft.data.DataWriter", "CachedOutput", "DataWriter"),
    "net.minecraft.server.packs.resources.ResourceManagerReloadListener": ("net.minecraft.resource.ResourceManagerReloadListener", "ResourceManagerReloadListener", "ResourceManagerReloadListener"),
    "net.minecraftforge.api.distmarker.Dist": ("net.fabricmc.api.EnvType", "Dist", "EnvType"),
    "net.minecraftforge.api.distmarker.OnlyIn": ("net.fabricmc.api.Environment", "OnlyIn", "Environment"),
    "net.minecraft.util.FastColor.ABGR32": ("net.minecraft.util.math.ColorHelper.Abgr", "ABGR32", "Abgr"),
    "net.minecraft.util.FastColor.ARGB32": ("net.minecraft.util.math.ColorHelper.Argb", "ARGB32", "Argb"),
}

# import lines removed entirely (annotations not on the classpath)
DROP = {
    "com.google.errorprone.annotations.CanIgnoreReturnValue",
    "com.google.errorprone.annotations.CheckReturnValue",
}


def rename_body(text, old, new):
    if old == new:
        return text
    return re.sub(r"(?<![\w.])" + re.escape(old) + r"\b", new, text)


def main():
    changed = 0
    for dirpath, _dirnames, filenames in os.walk(ROOT):
        for name in filenames:
            if not name.endswith(".java") or name in SKIP:
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8") as f:
                text = f.read()
            original = text
            out_lines = []
            # first pass: rename simple names in the whole file body (imports are rewritten below)
            for line in text.split("\n"):
                stripped = line.strip()
                if stripped.startswith("import "):
                    fqcn = stripped[len("import "):].rstrip(";").strip()
                    if fqcn in IMPORTS:
                        _new_fqcn, old_simple, new_simple = IMPORTS[fqcn]
                        if old_simple != new_simple:
                            text = rename_body(text, old_simple, new_simple)
            # second pass: rewrite the import lines themselves
            for line in text.split("\n"):
                stripped = line.strip()
                if stripped.startswith("import "):
                    fqcn = stripped[len("import "):].rstrip(";").strip()
                    if fqcn in DROP:
                        continue
                    if fqcn in IMPORTS:
                        line = "import " + IMPORTS[fqcn][0] + ";"
                    elif fqcn.startswith("net.minecraftforge.fluids."):
                        # any remaining forge fluid class: move to porting lib, same simple name
                        rest = fqcn[len("net.minecraftforge.fluids."):]
                        line = "import " + PL + ".fluids." + rest + ";"
                out_lines.append(line)
            text = "\n".join(out_lines)
            if text != original:
                with open(path, "w", encoding="utf-8") as f:
                    f.write(text)
                changed += 1
    print("-- files changed:", changed)
    return 0


if __name__ == "__main__":
    sys.exit(main())
