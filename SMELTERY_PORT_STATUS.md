# smeltery/** port status (Forge 3.12.1 → Fabric 1.20.1)

Slice: upstream `slimeknights/tconstruct/smeltery/**` (150 files, 19,927 lines) → same relative
paths under `tconstruct-fabric/src/main/java/slimeknights/tconstruct/smeltery/`.

## 1. File counts (target, 148 files)

| sub-package | files | | sub-package | files |
|---|---|---|---|---|
| `smeltery/` (root: TinkerSmeltery, SmelteryClientEvents, package-info) | 3 | | `block/entity/multiblock/` | 7 |
| `block/` | 11 | | `block/entity/tank/` | 8 |
| `block/component/` | 14 | | `client/render/` | 10 |
| `block/controller/` | 9 | | `client/screen/` | 6 |
| `block/entity/` | 11 | | `client/screen/module/` | 7 |
| `block/entity/component/` | 6 | | `client/util/` | 3 |
| `block/entity/controller/` | 6 | | `data/` | 3 |
| `block/entity/inventory/` | 7 | | `item/` | 7 |
| `block/entity/module/` | 8 | | `menu/` | 6 |
| `block/entity/module/alloying/` | 6 | | `network/` | 8 |

150 upstream − 148 target = **2 skipped** (see §2). Every other upstream file, including all
`package-info.java` files, exists at its original path with names, javadoc and member order kept.

## 2. Skipped files (datagen-only, per instruction 5)

- `data/FluidContainerTransferProvider.java` — implements Forge `DataProvider` (fluid-container
  transfer JSON datagen).
- `data/SmelteryRecipeProvider.java` — implements Forge `DataProvider` (recipe datagen).

No game content was dropped. `TinkerSmeltery.gatherData(GatherDataEvent)` was removed with a
`// TODO(fabric)` note; `commonSetup()` and `registerSerializers()` were split out of the Forge
`FMLCommonSetupEvent`/`RegisterEvent` handlers so a Fabric initializer can call them.

## 3. Mechanical port work done

`tools/port.pl` (ordered literal/regex rule list) + `tools/addimports.pl` were applied to all
files: Mojang→yarn class/member/package renames, `Level`→`World`, `Container`→`Inventory`,
`BlockState.getValue`→`get`, `AABB`→`Box`, `PoseStack`→`MatrixStack`, `GuiGraphics`→`DrawContext`,
`MenuType`→`ScreenHandlerType`, `AbstractContainerMenu`→`ScreenHandler`, `Properties`→
`AbstractBlock.Settings`, `Shapes.*`→`VoxelShapes.*`, `Block.box`→`Block.createCuboidShape`,
`FluidAction`→`boolean simulate`, Forge `FluidStack`→
`io.github.fabricators_of_create.porting_lib.fluids.FluidStack`, `LazyOptional`→
`io.github.fabricators_of_create.porting_lib.util.LazyOptional`, `RegistryObject`→
`io.github.fabricators_of_create.porting_lib.util.RegistryObject`.

`SmelteryTank` was additionally hand-fixed: `implements IFluidHandler` dropped (kept verbatim
multi-tank API `getTanks/getFluidInTank/getTankCapacity/isFluidValid` + `fill/drain(..., boolean
simulate)`), `long`→`int` casts for Porting Lib `FluidStack.getAmount()`, and a `TODO(fabric)`
header explaining that the `Storage<FluidVariant>` bridge lands with the block-entity layer.

Compile state with `tools/check_slice.sh /tmp/srcs.txt`: **1942 errors across 120 of 148 files**
(the number includes artefacts of compiling a slice without its sibling packages; see §4).

## 4. Remaining work, by category

1. **Forge capability plumbing — the bulk (~45 files).** `ICapabilityProvider.getCapability(...)`
   + `ForgeCapabilities.FLUID_HANDLER/ITEM_HANDLER` + `LazyOptional` must become per-BE
   `getFluidStorage(@Nullable Direction)` / `getItemStorage(@Nullable Direction)` methods plus
   `FluidStorage.SIDED` / `StorageProvider.createForItems(...)` lookups at call sites. Files:
   `block/ChannelBlock.java`, `block/entity/{CastingBlockEntity, CastingTankBlockEntity,
   ChannelBlockEntity, FaucetBlockEntity, FluidCannonBlockEntity, GaugeBlockEntity,
   HeaterBlockEntity, ProxyTankBlockEntity}.java`,
   `block/entity/component/{DuctBlockEntity, SmelteryInputOutputBlockEntity, TankBlockEntity}.java`,
   `block/entity/controller/{AlloyerBlockEntity, HeatingStructureBlockEntity,
   MelterBlockEntity}.java`, `block/entity/tank/ProxyItemTank.java`,
   `block/entity/inventory/DuctItemHandler.java`,
   `block/entity/module/{MultitankFuelModule, SolidFuelModule}.java`,
   `block/entity/module/alloying/MixerAlloyTank.java`,
   `item/{CopperCanFluidHandler, CopperCanItem, TankItem, TankItemFluidHandler}.java`,
   `menu/{AlloyerContainerMenu, HeatingStructureContainerMenu, MelterContainerMenu,
   SingleItemContainerMenu}.java`.
2. **Mantle 1.11 symbols (keep imports verbatim, listed as owed):**
   `mantle.data.loadable.Loadables` / `LoadableRecipeSerializer` (32 uses in `TinkerSmeltery`),
   `TypeAwareRecipeSerializer` (25), `mantle.block.GaugeBlock` (2 + `RenderingGaugeBlock`),
   `mantle.network.packet.BlockEntityPacket` (all 7 packets in `network/`),
   `mantle.client.render.{FluidCuboid, RenderItem, ChannelFluids, FaucetFluid}` (client renderers),
   `mantle.registration.object.CastItemObject`, `mantle.util.CombatHelper`,
   `mantle.fluid.transfer.IFluidContainerTransfer.TransferDirection`,
   `RetexturedHelper.addTagVariants`.
3. **Client render/screen library** (`library/client/**` is still empty): `AbstractContainerScreen`
   → `HandledScreen`, `MantleRenderTypes`, `FluidRenderer`, module screens. 27 files in
   `client/**`.
4. **Other slices' classes** referenced by name only: `TinkerFluids`, `TinkerCommons`,
   `TinkerToolParts`, `TinkerTags`, `ITEMS.registerCast(ItemObject, Item.Settings)`.
5. **Forge-only remnants to replace or annotate:** `net.minecraftforge.network.NetworkEvent.Context`
   (packets), `net.minecraftforge.client.model.data.ModelData` (2 BEs), `EmptyFluidHandler` (8),
   `ForgeHooks` (2), `IClientFluidTypeExtensions` (3), `SidedInvWrapper` (2),
   `mining.ServerPlayerGameMode` refs in `HeaterBlockEntity`/`FluidCannonBlockEntity`.

## 5. Compile-harness caveat

`tools/check_slice.sh` compiles only this slice with `-sourcepath`; files pulled in implicitly get
no annotation processing, so Lombok-generated members of *sibling* classes appear unresolved. A
share of the 1942 errors (notably `method does not override`, `cannot find symbol getFluids`) are
that artefact rather than port defects.

## 6. Upstream issues spotted (left verbatim)

- `block/entity/tank/SmelteryTank.java`: upstream comment `// TODO: does this replace
  ISmelteryTankHandler?` kept as-is.
- `block/entity/tank/CastingFluidHandler.java#getCapacity()` returns `fluid.getAmount()` when
  `capacity == 0` (i.e. capacity only becomes meaningful after the first fill) — kept verbatim.
- `block/entity/tank/ChannelSideTank` asserts `side.getAxis() != Axis.Y` but still accepts a `Y`
  direction parameter at every call site — kept verbatim.
