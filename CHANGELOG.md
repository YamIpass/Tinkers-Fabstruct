# Changelog

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
