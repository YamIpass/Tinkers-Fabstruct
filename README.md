# Tinkers' Fabstruct

An unofficial **Fabric** port of [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct) **3.12.1** for **Minecraft 1.20.1**.

中文名：**匠魂织布**（Tinkers' Fabstruct）。

This is not a rewrite or a "spiritual successor": the Java code is the upstream 3.12.1 source, adapted to the Fabric platform. Where Fabric has no equivalent of a Forge API, the smallest possible loader-level shim is used, and gameplay values are left untouched. Data files (`data/**`) and assets (`assets/**`) come straight out of the upstream 3.12.1 jar.

> Not affiliated with or endorsed by SlimeKnights. "Tinkers' Construct" is their work, released under the MIT license. See [Credits](#credits).

## Status

| Area | State |
| --- | --- |
| Tools, parts, modifiers, materials | Working |
| Tables: crafting station, part builder, tinker station, modifier station | Working |
| Smeltery, foundry, melter, faucet, drain, casting table/basin, tanks | Working |
| Alloys, ore/entity melting, fluid buckets and tank items | Working |
| Slime islands, sky slimes, biome spawns | Working |
| Automated server self-test (`-Dtconstruct.selftest=true`) | 60+ checks pass |
| Client smoke test (world load, GUIs, models, no missing textures) | Passes |

Known gaps are documented in [`PORT_STATUS.md`](PORT_STATUS.md); they are almost all "Fabric has no such API" notes, plus a handful of mod-interop conveniences that upstream only implements through Forge events.

## Requirements

- Minecraft **1.20.1**
- Fabric Loader **0.20.1** or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.92.12+1.20.1** or newer
- Java **17** or newer (Java 21 recommended)

Everything else — Mantle (Fabric fork), Porting Lib, Cardinal Components, Star, fluid libs — is nested inside the jar with Jar-in-Jar, so no extra downloads are needed.

## Installation

1. Install Fabric Loader for 1.20.1.
2. Drop `Tinkers-Fabstruct-1.20.1-<version>.jar` and Fabric API into `.minecraft/mods`.
3. Start the game.

**Do not** install this next to Forge Tinkers' Construct, and do not install two copies of this mod: the mod id is `tconstruct` on purpose, so existing data packs, world data and addon recipes that reference `tconstruct:` keep working.

## Building from source

```bash
./gradlew build            # jar lands in build/libs
./gradlew runServer -Pselftest=true   # headless integration test suite
```

The build needs JDK 21 and a network connection for the first Loom run. All non-Maven dependencies are committed under `libs/`, and the resource pipeline scripts live in `tools/` (see `tools/rebuild_resources.sh`).

## Credits

| Component | License | Notes |
| --- | --- | --- |
| Tinkers' Construct 3.12.1 | MIT | Code, data and assets by SlimeKnights |
| Mantle (Fabric fork) | MIT | [Alpha-s-Stuff/Mantle](https://github.com/Alpha-s-Stuff/Mantle) |
| Porting Lib 2.3.16 | LGPL-3.0 | Bundled unmodified, sources at [Fabricators-of-Create/Porting-Lib](https://github.com/Fabricators-of-Create/Porting-Lib) |
| Star, Cardinal Components, Dripstone Fluid Lib, Milk Lib, MixinSquared, Reach Entity Attributes | MIT | |
| NightConfig 3.6.6 | LGPL-3.0 | |
| jsr305 3.0.2 | Apache-2.0 | |

Full license texts for this project are in [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE); both are also shipped inside the jar under `META-INF/`.

## License

MIT. The port keeps the upstream copyright notice and adds one for the port contributors.
