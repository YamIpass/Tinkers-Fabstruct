# Fix guide — TConstruct 3.12.1 → Fabric 1.20.1 (loader port only)

Read this fully before editing anything. It is the contract every fix agent works under.

## What this project is

`tconstruct-fabric/` is Tinkers' Construct **3.12.1** (the Forge 1.20.1 release) copied **verbatim** into a
Fabric 1.20.1 mod. The gameplay code (recipes, stats, numbers, structure of the mod) must stay identical to
3.12.1. Only loader-facing glue may change: Forge APIs → Fabric / Mantle / Porting Lib equivalents.

- Minecraft 1.20.1, **Mojang official mappings** (`loom.officialMojangMappings()`), so vanilla names are
  `net.minecraft.world.level.Level`, `CompoundTag`, `getLevel()`, `ItemStack`, `BlockPos`, …
- Mod id `tconstruct`, package `slimeknights.tconstruct`.
- Fabric API 0.92.12, Fabric Loader 0.20.1, Fabric Loom 1.8.13, Gradle 8.10.2, Java 17 target.
- `libs/` contains the mod's JiJ dependencies, all Fabric builds:
  - `Mantle-1.20.1-1.11.DEV.eb1e9a5.jar` — Fabric port of Mantle (`slimeknights.mantle.*`)
  - 33 × Porting Lib `2.3.16-beta.81+1.20.1` (`io.github.fabricators_of_create.porting_lib.*`)
- `src/main/resources/tconstruct.accesswidener` exists and is wired into Loom; it already opens the
  vanilla members the mod needs. Do not add entries unless you truly need one, and say so in your report.

## The reference implementation (most important resource)

`ref/Hephaestus-1.20.1/src/main/java/slimeknights/tconstruct/` is a **complete, compiling Fabric port of
Tinkers' Construct 3.6.4** (project "Hephaestus", MIT, same package names, same Mojang mappings, Mantle
1.9.291 + PortingLib 2.3.15).

For any file you must fix: open the same path under `ref/Hephaestus-1.20.1/src/main/java/` first.

- If the reference has the file, **use its loader glue verbatim** (imports, capability wiring, event
  registration, packet classes, registry calls). Keep our 3.12.1 gameplay logic and fields.
- If 3.12.1 added methods/classes the reference does not know, port them yourself in the same style.
- If the reference simply does not have the file (new in 3.6.4 → 3.12.1), port it from
  `ref/TinkersConstruct/src/main/java/` (3.12.1 Forge source) using the patterns you just learned.

`ref/TinkersConstruct/` is the untouched Forge 3.12.1 source tree — the behavioural ground truth.

## Hard rules

1. **Never change gameplay**: no recipe ids/values, no material stats, no balance numbers, no removing
   features. If something cannot work in Fabric, stub the loader side and leave a
   `// TODO(fabric): <what is missing>` comment.
2. **No new gameplay classes.** Fix files in place; do not rewrite a class into a simpler substitute.
3. Do not run Gradle (one build at a time, run by the coordinator). Do not run `git` commands.
4. Do not touch files outside your assigned package. If a fix genuinely needs a change in another
   package, note it in your final report instead.
5. Keep imports tidy; no wildcard imports; keep the file's existing formatting (2-space indent).
6. `TODO(fabric)` markers are acceptable and expected where Fabric has no equivalent.

## Replacement cheat sheet (Forge → Fabric, for 1.20.1 Mojang mappings)

| Forge | Use instead |
| --- | --- |
| `net.minecraftforge.fml.common.Mod`, `FMLJavaModLoadingContext`, `IEventBus` | Fabric entrypoints (`ModInitializer`, `ClientModInitializer`); call module `init()` static methods directly |
| `net.minecraftforge.eventbus.api.SubscribeEvent`, `MinecraftForge.EVENT_BUS` | Fabric API events (`ServerLifecycleEvents`, `PlayerBlockBreakEvents`, `UseBlockCallback`, `ServerTickEvents`, `EntitySleepEvents`, `LootTableEvents`, `CommandRegistrationCallback`, …) or Porting Lib `BaseEvent` (`io.github.fabricators_of_create.porting_lib.core.event.BaseEvent`) |
| `DeferredRegister`, `RegistryObject`, `IForgeRegistry` | direct `Registry.register(...)` / `BuiltInRegistries`, or a small holder class as the reference does |
| `ForgeRegistries.X` | `BuiltInRegistries.X` |
| `ForgeHooks`, `ForgeEventFactory` | the Fabric event that replaced them, or the vanilla call directly |
| `LazyOptional<T>` | `@Nullable T` / `Optional<T>` (Mantle's capability helpers return nullable) |
| `Capability`, `CapabilityManager`, `ICapabilityProvider` | Porting Lib `FluidStorage.SIDED` / `ItemStorage.SIDED`, or Fabric data attachments (`AttachmentRegistry`) as the reference does |
| `IFluidHandler`, `IFluidHandlerItem`, `FluidStack` (Forge) | Porting Lib `io.github.fabricators_of_create.porting_lib.fluids.FluidStack`, `FluidTank`, `FluidStorage`, `FluidAction` |
| Forge `FluidAction` | **Does not exist on this classpath** (verified: no `FluidAction` in PortingLib 2.3.16 or Mantle 1.11). Use a `boolean simulate` parameter in your own interfaces: `FluidAction.SIMULATE` → `true`, `FluidAction.EXECUTE` → `false`, and drop the enum parameter from the signature (rename `action` → `simulate`). Porting Lib `FluidStack` amounts are **`long`**, not `int`. |
| `IItemHandler`, `ItemStackHandler` | Porting Lib `transfer.item.ItemStackHandler`, `SlottedStackStorage`, `TransferUtil` |
| `NetworkEvent.Context`, `SimpleChannel`, `PacketDistributor` | Mantle `slimeknights.mantle.network.NetworkWrapper` + `ISimplePacket`; `IThreadsafePacket` for client-bound work; packet classes take `FriendlyByteBuf` in the constructor and implement `encode` / `handle` |
| `slimeknights.mantle.network.packet.BlockEntityPacket` (missing in Mantle 1.11) | implement `IThreadsafePacket` directly with a `BlockPos` field — see `ref/Hephaestus-1.20.1/.../smeltery/network/StructureUpdatePacket.java` |
| `DistExecutor`, `@OnlyIn`, `Side` | `FabricLoader.getInstance().getEnvironmentType()`, `EnvType.CLIENT`, or move the code to a client-only entrypoint |
| `ModList.get().isLoaded("jei")` | optional mod integrations were deleted; drop the branch |
| `@ObjectHolder`, `Item.Properties.tab` | `ItemGroupEvents.modifyEntriesEvent(...)` as in `shared/TinkerCommons.java` |
| `ICondition`, `CraftingHelper` conditions | not supported at runtime; keep the JSON field, ignore it, add `// TODO(fabric)` |
| `forge:...` tags in code | `TagKey.create(Registries.X, new ResourceLocation("c", ...))` — the resource pipeline already maps Forge tags to `c:` tags (see `tools/gen_forge_tags.py`) |
| `RenderTypeGroup`, `CompositeModel.Baked.Builder#addQuads(RenderTypeGroup, ...)` | Porting Lib's builder only has `addQuads(BakedQuad...)`; drop the render type |
| `net.minecraftforge.client.model.*` custom loaders | Porting Lib `model.loader` / `model_loader` packages, or a `ModelResourceLocation`-free `UnbakedModel`; reference port has working examples |

## Style of the fix

Our files were copied with Mojang mappings and their Forge wiring still in place, so the diff against the
reference is usually mechanical: replace imports and the loader calls, keep the body. When you touch a
file, read it fully first, then compare with the reference, then edit surgically.

## Reporting

When done, report in one message: files touched (count + the biggest 10), what remains broken in your
package and why, and anything you had to stub.
