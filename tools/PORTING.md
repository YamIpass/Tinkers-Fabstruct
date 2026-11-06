# Porting upstream Tinkers' Construct (Forge, Mojang mappings) to this Fabric project

Goal: copy upstream 3.12.1 sources **verbatim** where possible. Only the platform layer changes:
Mojang mappings → yarn mappings, Forge APIs → Fabric/Mantle/Porting Lib equivalents.

Upstream reference tree (Forge 3.12.1, Mojang mappings):
  `ref/TinkersConstruct/src/main/java/slimeknights/tconstruct/...`

Target tree (this project, yarn 1.20.1):
  `tconstruct-fabric/src/main/java/slimeknights/tconstruct/...`

Vendored Fabric libraries (already on the compile classpath, production-mapped):
  `tconstruct-fabric/libs/Mantle-1.20.1-1.9.291.jar` — Fabric port of Mantle, **same package names**
  `tconstruct-fabric/libs/{base,common,core,transfer,items,fluids,networking,models,model_loader,...}-2.3.14+1.20.1.jar` — Porting Lib

Use `javap -classpath tconstruct-fabric/libs/<jar> <class>` to confirm any Mantle/Porting Lib API before relying on it.

## Class renames (Mojang → yarn 1.20.1)

```
net.minecraft.core.BlockPos                     → net.minecraft.util.math.BlockPos
net.minecraft.core.Direction                    → net.minecraft.util.math.Direction
net.minecraft.core.Holder                       → net.minecraft.registry.entry.RegistryEntry
net.minecraft.core.NonNullList                  → net.minecraft.util.collection.DefaultedList
net.minecraft.core.RegistryAccess               → net.minecraft.registry.DynamicRegistryManager
net.minecraft.core.registries.BuiltInRegistries → net.minecraft.registry.Registries
net.minecraft.core.registries.Registries        → net.minecraft.registry.RegistryKeys
net.minecraft.nbt.CompoundTag                    → net.minecraft.nbt.NbtCompound
net.minecraft.nbt.Tag                           → net.minecraft.nbt.NbtElement
net.minecraft.nbt.ListTag                       → net.minecraft.nbt.NbtList
net.minecraft.network.FriendlyByteBuf           → net.minecraft.network.PacketByteBuf
net.minecraft.network.chat.Component            → net.minecraft.text.Text
net.minecraft.network.chat.MutableComponent     → net.minecraft.text.MutableText
net.minecraft.network.chat.TextColor            → net.minecraft.text.style.TextColor
net.minecraft.resources.ResourceLocation         → net.minecraft.util.Identifier
net.minecraft.server.level.ServerPlayer         → net.minecraft.server.network.ServerPlayerEntity
net.minecraft.server.level.ServerLevel          → net.minecraft.server.world.ServerWorld
net.minecraft.sounds.SoundEvent                 → net.minecraft.sound.SoundEvent
net.minecraft.sounds.SoundEvents                → net.minecraft.sound.SoundEvents
net.minecraft.tags.TagKey                       → net.minecraft.registry.tag.TagKey
net.minecraft.tags.ItemTags                     → net.minecraft.registry.tag.ItemTags
net.minecraft.util.Mth                          → net.minecraft.util.math.MathHelper
net.minecraft.util.FormattedCharSequence        → net.minecraft.text.OrderedText
net.minecraft.world.Container                   → net.minecraft.inventory.Inventory
net.minecraft.world.InteractionHand             → net.minecraft.util.Hand
net.minecraft.world.InteractionResult           → net.minecraft.util.ActionResult
net.minecraft.world.MenuProvider                → net.minecraft.screen.NamedScreenHandlerFactory
net.minecraft.world.entity.player.Inventory     → net.minecraft.entity.player.PlayerInventory
net.minecraft.world.entity.player.Player        → net.minecraft.entity.player.PlayerEntity
net.minecraft.world.entity.player.StackedContents → net.minecraft.recipe.RecipeMatcher
net.minecraft.world.entity.decoration.ArmorStand → net.minecraft.entity.decoration.ArmorStandEntity
net.minecraft.world.inventory.AbstractContainerMenu → net.minecraft.screen.ScreenHandler
net.minecraft.world.inventory.CraftingContainer → net.minecraft.inventory.CraftingInventory
net.minecraft.world.inventory.InventoryMenu     → net.minecraft.screen.PlayerScreenHandler
net.minecraft.world.inventory.MenuType          → net.minecraft.screen.ScreenHandlerType
net.minecraft.world.inventory.Slot              → net.minecraft.screen.slot.Slot
net.minecraft.world.item.ItemStack              → net.minecraft.item.ItemStack
net.minecraft.world.item.Item                   → net.minecraft.item.Item
net.minecraft.world.item.Items                  → net.minecraft.item.Items
net.minecraft.world.level.Level                 → net.minecraft.world.World
net.minecraft.world.level.block.*               → net.minecraft.block.*
net.minecraft.world.level.block.entity.*        → net.minecraft.block.entity.*
net.minecraft.world.level.block.state.BlockState → net.minecraft.block.BlockState
net.minecraft.world.phys.AABB                   → net.minecraft.util.math.Box
net.minecraft.world.item.crafting.*             → net.minecraft.recipe.*
net.minecraft.client.gui.GuiGraphics            → net.minecraft.client.gui.DrawContext
net.minecraft.client.gui.screens.Screen         → net.minecraft.client.gui.screen.Screen
net.minecraft.client.gui.screens.inventory.InventoryScreen → net.minecraft.client.gui.screen.ingame.InventoryScreen
net.minecraft.client.gui.components.Button       → net.minecraft.client.gui.widget.ButtonWidget
net.minecraft.client.gui.components.EditBox      → net.minecraft.client.gui.widget.TextFieldWidget
net.minecraft.client.gui.components.Renderable   → net.minecraft.client.gui.Drawable
net.minecraft.client.gui.components.events.GuiEventListener → net.minecraft.client.gui.Element
net.minecraft.client.Minecraft                   → net.minecraft.client.MinecraftClient
net.minecraft.client.renderer.Rect2i             → net.minecraft.client.util.math.Rect2i
net.minecraft.client.gui.screens.MenuScreens    → net.minecraft.client.gui.screen.ingame.HandledScreens
```

## Member renames

```
level / getLevel()          → world / getWorld()
isClientSide / isClientSide() → getWorld().isClient()
player.containerMenu        → player.currentScreenHandler
player.getUUID()            → player.getUuid()
player.tickCount            → player.age
entity.blockPosition()      → entity.getBlockPos()
be.getBlockPos()            → be.getPos()
pos.relative(dir)           → pos.offset(dir)
Direction.Plane.HORIZONTAL  → Direction.Type.HORIZONTAL
ItemStack.matches(a, b)     → ItemStack.areEqual(a, b)
stack.getCount/setCount     → same
container.getContainerSize()→ inventory.size()
container.getItem(i)        → inventory.getStack(i)
container.setItem(i, s)     → inventory.setStack(i, s)
container.setChanged()      → inventory.markDirty()
container.clearContent()    → inventory.clear()
container.stillValid(p)     → inventory.canPlayerUse(p)
container.removeItem(i, n)  → inventory.removeStack(i, n)
```

## Forge API replacements

```
net.minecraftforge.items.IItemHandler / IItemHandlerModifiable
    → io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage
      (verify with javap; Porting Lib also ships io.github.fabricators_of_create.porting_lib.transfer.item.IItemHandler)
net.minecraftforge.items.ItemStackHandler
    → io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler
net.minecraftforge.items.ItemHandlerHelper
    → io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper
net.minecraftforge.common.util.LazyOptional
    → io.github.fabricators_of_create.porting_lib.util.LazyOptional
net.minecraftforge.event.ForgeEventFactory  → vanilla + Fabric API (note what the call was for)
net.minecraftforge.network.*                → Mantle slimeknights.mantle.network (PacketHandler) or Fabric networking
net.minecraftforge.api.distmarker.Dist / FMLEnvironment.dist
    → net.fabricmc.api.EnvType / net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType()
    (or simply Fabric's client entrypoint split)
net.minecraftforge.registries.* / RegistryObject
    → net.minecraft.registry.Registries + Registry.register, or Fabric registry helpers
```

Mantle classes keep their exact package and names — do not rename them.

## Rules

1. Keep upstream class names, field names, method names, javadoc and comment structure. Do not redesign.
2. Only change what will not compile. Prefer the smallest edit at the call site.
3. If a dependency (class you need) is not ported yet, keep the reference: another slice is porting it right now
   with the same name and package. Do **not** create a substitute or a stub with a different name.
4. Do not run gradle; the integrator compiles.
5. Report, per file: `path — clean | adapted(what) | blocked(missing class)`.
