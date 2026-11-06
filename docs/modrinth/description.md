# Tinkers' Fabstruct

**Unofficial Fabric port of Tinkers' Construct 3.12.1 for Minecraft 1.20.1.**

The Java code is the upstream 3.12.1 source, adapted to the Fabric platform — not a rewrite. Gameplay values, data files and assets are the upstream ones; only the loader-facing layers were changed, plus the smallest possible shims where Fabric has no equivalent of a Forge API.

## What you get

- **Tools and modifiers** — all 26 tool building recipes, 45 tool definitions, 99 materials and 300 modifiers, with the full stat/module stack of 3.12.1.
- **Stations** — crafting station, part builder, tinker station, modifier station, plus repair recipes and repair kits.
- **Smeltery stack** — smeltery, foundry, melter, faucet, drain, casting table, casting basin, seared and scorched tanks, every alloy, ore and entity melting.
- **World content** — slime islands, sky slimes, biome spawns, advancements.

## Requirements

- Minecraft **1.20.1**
- Fabric Loader **0.20.1+**
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.92.12+1.20.1**
- Java **17+**

Everything else (Mantle Fabric fork, Porting Lib, fluid and component libraries) is nested inside the jar — no extra downloads.

## Notes

- The mod id is `tconstruct` on purpose, so existing worlds, data packs and addon recipes keep working. Do not install it next to the Forge original, and do not install two copies.
- Client and server are both supported (`environment: *`).
- Known gaps are documented in the repository's `PORT_STATUS.md` — they are almost all "Fabric has no such API" notes.

## Credits

Tinkers' Construct 3.12.1 — SlimeKnights (MIT). Mantle Fabric fork — Alpha-s-Stuff (MIT). Porting Lib — Fabricators-of-Create (LGPL-3.0, bundled unmodified). Other nested libraries are MIT, LGPL-3.0 or Apache-2.0 as listed in the repository's `NOTICE`.

Not affiliated with or endorsed by SlimeKnights.
