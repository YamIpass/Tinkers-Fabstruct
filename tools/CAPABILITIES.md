# Capability convention for the Fabric port

Tinkers' Construct 3.12.1 is written against the Forge capability system. Fabric has no
capabilities; instead we use **Fabric Transfer API** plus **Porting Lib** helpers that are
already on the classpath (see `libs/`).

This document is the single ruling for every slice. Do not invent a different scheme.

## 1. Block entity side (providers)

Every block entity that upstream declares as a capability provider exposes **plain Java
methods** instead of `ICapabilityProvider`:

```java
@Nullable
public Storage<FluidVariant> getFluidStorage(@Nullable Direction side) { ... }

@Nullable
public Storage<ItemVariant> getItemStorage(@Nullable Direction side) { ... }
```

- Return `null` when the block entity does not offer that capability on that side.
- The actual registration happens **once**, in the common initializer next to where the
  block entity type is registered:

```java
FluidStorage.SIDED.registerForBlockEntity((be, dir) -> be.getFluidStorage(dir), MyBlockEntities.TANK);
ItemStorage.SIDED.registerForBlockEntity((be, dir) -> be.getItemStorage(dir), MyBlockEntities.TANK);
```

- Delete `getCapability`, `ICapabilityProvider`, `LazyOptional`, `Capability` imports and
  leave a `// TODO(fabric): capability provider replaced by Fabric Transfer` comment only
  where something non-obvious was dropped.

## 2. Same-block or internal access

Inside the mod (a block entity asking its own component, a menu asking its block entity)
call the method directly: `tank.getFluidStorage(null)`. Never do a world lookup for a
block entity you already hold.

## 3. Neighbour / world lookup

| Forge | Fabric |
| --- | --- |
| `world.getCapability(ForgeCapabilities.FLUID_HANDLER, pos, side)` | `FluidStorage.SIDED.find(world, pos, side)` |
| `world.getCapability(ForgeCapabilities.ITEM_HANDLER, pos, side)` | `ItemStorage.SIDED.find(world, pos, side)` (or `StorageProvider.createForItems(world, pos)` for inventory blocks) |
| `stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)` | `FluidStorage.ITEM.find(stack, null)` |
| `stack.getCapability(ForgeCapabilities.ITEM_HANDLER)` | `ItemStorage.ITEM.find(stack, null)` |

`find` returns `null` when the target does not support the capability — that replaces the
empty `LazyOptional`.

## 4. Type mapping

| Forge | Fabric / Porting Lib |
| --- | --- |
| `IFluidHandler` | `Storage<FluidVariant>` |
| `IFluidHandlerItem` | `Storage<FluidVariant>` on an item stack (`FluidStorage.ITEM`) |
| `IItemHandler` | `Storage<ItemVariant>` / `SlottedStorage<ItemVariant>` |
| `IItemHandlerModifiable` | `SlottedStorage<ItemVariant>` (`setSlot` instead of `setStackInSlot`) |
| `ItemStackHandler` | `io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler` |
| `ItemHandlerHelper.insertItem(...)` | `TransferUtil.insertItem(storage, stack)` |
| `FluidStack` (Forge) | `net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant` + amount, **or** Porting Lib's `FluidStack` when the surrounding upstream code is fluid-centric (`ios` rich helpers). Pick per file; never mix both in one class. |
| `BucketItem`, `EmptyFluidHandler` | `Storage<FluidVariant>` wrapper |
| `FluidUtil.tryEmptyContainer(...)` | `FluidStorage.ITEM` interaction or Porting Lib `FluidUtil` equivalent |

## 5. Multi-tank shapes

Upstream's `SmelteryTank`/`IFluidTankUpdater` style classes keep their own list-of-tanks
API. Bridge them through a single `Storage<FluidVariant>` view (one storage that fans out
to the tank list) rather than one storage per tank. Porting Lib's `FluidTank`
(`io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank`) already gives
`insert`/`extract`/`getFluid`, and `FluidTankBase` in `library/fluid/` is the upstream
class ported on top of it.

## 6. Blocks (not block entities)

For blocks that only expose a capability by delegating to their block entity, do nothing:
the block entity registration above is enough. For blocks with no block entity, return
`Storage` from a `BlockEntity`-less `SIDED` lookup object only if upstream did so; otherwise
skip and TODO.
