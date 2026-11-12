# Tinkers' Fabstruct 0.3.0 — JEI integration

Unofficial, source-level Fabric port of **Tinkers' Construct 3.12.1** for **Minecraft 1.20.1**.
Not affiliated with SlimeKnights.

## Highlights

**JEI support, finally.** The port never had a JEI plugin, so JEI could only show this mod's items
inside vanilla crafting recipes. This release adds the mod's own workstation categories:

| Category | Recipes |
|---|---|
| Tool building | 26 |
| Tool modification | 206 |
| Modifiers | 270 |
| Part builder | 204 |
| Molding | 58 |
| Modifier worktable | 25 |

Recipe transfer, catalysts, and the material-variant recipe extensions came along with them.

**Readable JEI tag pages.** JEI titles tag pages from `tag.<registry>.<namespace>.<path>` and falls
back to the raw id, which is why pages used to read `#tconstruct:tooltips/metal`. 885 tags of this mod
now have English and Chinese names (`Tooltips · Metal` / 「工具提示 · 金属」).

## Files

| File | Size | SHA-1 |
|---|---|---|
| `Tinkers-Fabstruct-1.20.1-0.3.0.jar` | 31,745,570 B | `44FEA74D15D1F194B6F173B60F41F495AB5F4573` |

## Requirements

- Minecraft **1.20.1**
- Fabric Loader **0.20.1+**
- Fabric API **0.92.12+1.20.1**
- Java **17+**
- JEI is optional. If you want the recipe categories, install JEI 15.74+ for Fabric yourself; it is a
  compile-only dependency and is not bundled.

## Upgrading

Replace the old jar. The mod id stays `tconstruct` on purpose (worlds and datapacks index by it), so
existing saves keep working; do not run this next to the Forge Tinkers' Construct.

## Verification

- Server self-test (`-Dtconstruct.selftest=true`): all checks pass, including the fluid-unit
  boundary checks added in 0.2.0.
- Client run: `[jei] station recipes: tool building=26, tool modification=206, modifiers=270,
  part builder=204, molding=58, worktable=25`, no unknown ingredient or category errors.

## Not yet

Fluid recipe categories (melting, casting, alloying, entity melting, severing, fuel) are still
missing: Fabric JEI wants `FabricTypes.FLUID_STACK` + `JeiFluidIngredient`, upstream code uses
Forge's `FluidStack`. Next stage.

## License

MIT, same as upstream Tinkers' Construct (© SlimeKnights) and Mantle. Full notice in
`META-INF/LICENSE` and `META-INF/NOTICE` inside the jar. Bundled third-party licenses: Porting Lib
(LGPL-3.0, unmodified), Fabric API, and the vendored Mantle fork (MIT).
