# Changelog

## 0.3.0 — JEI integration

Third public build. The port had no JEI plugin at all, so JEI only ever showed the mod's items
inside vanilla crafting recipes; its own stations were invisible. This build adds them.

### JEI plugin (first stage)

- Six new recipe categories: tool building, tool modification, modifiers, part builder, molding and
  the modifier worktable. Loaded counts on a fresh world: 26 / 206 / 270 / 204 / 58 / 25.
- Ingredient types for material variants, modifiers, patterns and slot types, so the categories can
  render the same icons the block GUIs use.
- Recipe transfer handlers for the tinker station and the tool inventory, plus catalysts for the four
  worktables, the anvil and the modifier worktable.
- Material recipe extensions (`shaped` / `shapeless` / tool tinkering) so the material-variant crafting
  recipes are grouped with the station recipes instead of dangling in vanilla categories.

### JEI tag pages

- JEI titles tag pages with the key `tag.<registry>.<namespace>.<path>` and falls back to the raw id
  (`#tconstruct:tooltips/metal`). Upstream ships none of those keys. 885 tags of this mod now have
  readable names in English and Chinese — for example `tooltips/metal` → "Tooltips · Metal" /
  「工具提示 · 金属」 and `casts/single_use/ingot` → 「铸模 · 一次性 · 锭」.
- The small grey line under the title is JEI's own raw id rendering and cannot be translated.

### Build

- JEI is a compile-only dependency: players bring their own JEI, nothing is nested into the jar.
  A local `libs-compile/` jar is preferred for offline builds, with Modrinth's maven as the fallback
  for a fresh clone.
- The bundled Mantle jar no longer advertises a `jei_mod_plugin` entry point it does not ship, which
  made JEI log `Mantle specified an invalid entrypoint`.
- Added a minimal `slimeknights.mantle.plugin.jei.MantleJEIConstants` shim: upstream's material and
  tool-tinkering extensions need it, but the trimmed Mantle jar does not contain the class.

### Not yet

- Fluid recipe categories (melting, casting, alloying, entity melting, severing, fuel) still are not
  registered. Fabric JEI represents fluids as `FabricTypes.FLUID_STACK` +
  `JeiFluidIngredient(Fluid, long, CompoundTag)`, while upstream's plugins were written against
  Forge's `FluidStack`; that rewrite is the next stage.

## 0.2.0 — fluid units, fuel gauge and soul glass

Second public build. Everything below is loader plumbing; no recipe, stat or gameplay number was changed.

### Fluid units at the Fabric boundary

- Block entities now publish **droplets** to the outside world, as Fabric expects, while every recipe inside the mod keeps counting **millibuckets**. New `MillibucketStorageBlockEntity` / `MillibucketStorage` do the conversion in one place instead of at each call site.
- Covered block entities: casting table, casting tank, channel, proxy tank, smeltery fluid IO, tank, alloyer, heating structure (smeltery/foundry controllers) and melter. 24 internal `FluidStorage.SIDED` lookups were rerouted through `FluidStorages.raw(...)` so melting, casting, alloying and fuel keep working in millibuckets.
- Bucket right click on tanks, drains and controllers goes through `FluidStorages.interactWithTankRaw(...)`: pouring one bucket stores 1000 mB, and an empty bucket takes a full bucket back out.
- Tank item tooltips gained the missing translation keys (`block.tconstruct.tank.fluid` / `.mb` / `.ingots` / `.mixed`) in `en_us` and `zh_cn`.

### Controller fuel gauge

- The fuel gauge in the smeltery/foundry controller can be drained again with an empty bucket: the fuel module was reading droplet amounts where Mantle's `fillStack` expects millibuckets, so extraction always failed.
- Mantle's `fillStack` now accepts **both** conventions (81000 droplets or 1000 mB per bucket) instead of assuming one, so tanks from other mods keep working.
- `playUISound` no longer sends a packet to a `ServerPlayer` without a connection, which used to abort the transfer.

### GUI

- Fluid amounts in the melter/casting-tank gauges and in the tank gauge renderer are converted from droplets to millibuckets, so `1000 mB` is no longer shown as `81B`.
- Mantle's fluid tooltip units were corrected (`bucket = 1000`, `millibucket = 1`), so tooltips no longer read `7B 2900 droplet`.
- `MelterScreen` regained the `mouseClicked` override that was lost in the port: left click a fluid to fill the held item, left/right click the fuel gauge to drain or fill it.

### Soul glass

- Standing inside soul glass no longer turns the screen black: vanilla draws that overlay with a hard-coded dark colour, so the mixin cancels it and a HUD layer draws the soul glass texture with its own alpha (`textures/block/soul_glass_overlay.png`).
- Applies to plain soul glass, seared soul glass and scorched soul glass.

### Diagnostics

- Removed the `[tank] droplet view ... raw ...` chat probe and the `/tconstruct tank` diagnostic command that shipped in the previous test build.
- New self tests: `checkDropletView`, `checkCastingTankBucket`, `checkSmelteryTankBucket` and `checkFuelSlotDrain` (the last one builds a smeltery, pours a lava bucket into the fuel tank, drives `clickMenuButton(player, 1)` with an empty bucket in hand and asserts a lava bucket comes back).

### Verification

- `./gradlew runServer -Pselftest=true`: 60+ assertions, all pass.
- Client smoke test: world load, stations, smeltery GUI, tank interactions, soul glass overlay, no model bake failures.

## 0.1.0 — first public build

First unofficial Fabric port of Tinkers' Construct 3.12.1 for Minecraft 1.20.1.

### Content

- Full upstream content set: 26 tool building recipes, 45 tool definitions, 99 materials, 300 dynamic modifiers, all tool parts, casts and patterns.
- Four stations — crafting station, part builder, tinker station, modifier station — plus the modifier recipes, repair recipes and repair kits.
- Smeltery stack: smeltery, foundry, melter, faucet, drain, casting table, casting basin, seared and scorched tanks, all alloys, ore/entity melting and casting recipes.
- Slime islands, sky slimes, biome spawn modifiers, and the port's own advancements tree (1697 entries load).

### Platform work

- Java sources stay at upstream 3.12.1; only the loader-facing layers were adapted (Mantle Fabric fork bundled with Jar-in-Jar, Porting Lib for Forge-style helpers, Fabric API for tags/transfer/networking/render APIs).
- Forge-only hooks that Fabric has no equivalent for were reimplemented at the smallest scope: connected-texture block models, fluid type attributes, block entity sync, projectile ammo selection, creative tab duplicate handling.

### Verification

- `./gradlew runServer -Pselftest=true` runs an integration suite covering recipes, tool assembly, modifiers, repair, armor plating, smeltery multiblocks, alloying, melting, entity melting, casting table/basin, faucet, drain, tank items, fluid buckets and biome injection. All checks pass.
- Client smoke test: world load, all station GUIs, tool/part models, tinted glass and fluid rendering, no missing textures and no model bake failures.
- Jar ships `LICENSE` and `NOTICE` under `META-INF/`, including the licenses of every nested library.

### Known limitations

- `TODO(fabric)` notes remain where Fabric lacks the corresponding API; see `PORT_STATUS.md`. They are documentation of the port boundary, not disabled gameplay.
- Forge-only mod interop (crafting player context, Forge capability bridges) is not available on Fabric by definition.
