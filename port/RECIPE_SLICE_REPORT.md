# library/recipe/** port report (Tinkers' Construct 3.12.1 Forge → Fabric 1.20.1)

Slice: `slimeknights.tconstruct.library.recipe.**`
Source: `ref/TinkersConstruct/src/main/java/slimeknights/tconstruct/library/recipe`
Target: `tconstruct-fabric/src/main/java/slimeknights/tconstruct/library/recipe`

## 1. Files written — 175 upstream files, all present

| package dir | files |
|---|---|
| modifiers/adding | 20 |
| casting | 15 |
| material | 13 |
| melting | 12 |
| casting/material | 12 |
| tinkerstation/repairing | 10 |
| tinkerstation/building | 10 |
| partbuilder | 10 |
| display | 9 |
| ingredient | 8 |
| tinkerstation | 7 |
| worktable | 6 |
| modifiers/severing | 5 |
| fuel | 5 |
| alloying | 5 |
| partbuilder/recycle | 4 |
| molding | 4 |
| entitymelting | 4 |
| modifiers | 3 |
| casting/container | 3 |
| casting/potion | 2 |
| root (TinkerRecipeTypes, TagPredicate, SingleItemContainer, RecipeResult, RandomItem, ITinkerableContainer, FluidValues, package-info) | 8 |

Total in target dir = 181 = 175 upstream + 6 `smeltery/*` (other slice, untouched).
`smeltery/` + `library/recipe/smeltery/` were not modified.

## 2. Deletions (superseded hand-written prototypes)

- `MaterialRecipe.java` → replaced by upstream `material/MaterialRecipe.java`
- `PartBuildingRecipe.java` → replaced by `partbuilder/{PartRecipe,ItemPartRecipe}.java`
- `ToolBuildingRecipe.java` → replaced by `tinkerstation/building/ToolBuildingRecipe.java`
- `ModifierRecipe.java` → replaced by `modifiers/adding/ModifierRecipe.java`
- `IncrementalModifierRecipe.java` → replaced by `modifiers/adding/IncrementalModifierRecipe.java`
- `RepairRecipe.java` → replaced by `tinkerstation/repairing/*` recipes
- `MaterialItemRegistry.java` → replaced by `material/MaterialRecipeCache.java`
- `TinkerRecipes.java` → replaced by `TinkerRecipeTypes.java`

## 3. Skipped files / non-code

- DECISION: the 34 datagen-only builder files were **ported rather than skipped** (rule 1 copy-first; they are part of the 175 and the rest of the datagen tree references them), but they are not needed at runtime (builders/consumer wrappers only): `alloying/AlloyRecipeBuilder`, `casting/ItemCastingRecipeBuilder`, `casting/PotionCastingRecipeBuilder`, `casting/container/ContainerFillingRecipeBuilder`, `casting/material/{CompositeCastingRecipeBuilder,MaterialCastingRecipeBuilder,MaterialFluidRecipeBuilder,PartSwapCastingRecipeBuilder}`, `entitymelting/EntityMeltingRecipeBuilder`, `fuel/MeltingFuelBuilder`, `material/{MaterialRecipeBuilder,MaterialsConsumerBuilder,ShapedMaterialConsumerBuilder}`, `melting/{MaterialMeltingRecipeBuilder,MeltingRecipeBuilder}`, `modifiers/adding/{AbstractModifierRecipeBuilder,AbstractMultilevelModifierRecipeBuilder,IncrementalModifierRecipeBuilder,ModifierRecipeBuilder,MultilevelIncrementalModifierRecipeBuilder,MultilevelModifierRecipeBuilder,OverslimeModifierRecipeBuilder,SwappableModifierRecipeBuilder}`, `modifiers/severing/SeveringRecipeBuilder`, `molding/MoldingRecipeBuilder`, `partbuilder/{ItemPartRecipeBuilder,PartRecipeBuilder}`, `partbuilder/recycle/{PartBuilderRecycleBuilder,PartBuilderToolRecycleBuilder}`, `tinkerstation/building/{MaterialSwappingRecipeBuilder,ToolBuildingRecipeBuilder}`, `tinkerstation/repairing/{ModifierMaterialRepairRecipeBuilder,ModifierRepairRecipeBuilder}`, `worktable/{AbstractSizedIngredientRecipeBuilder,ModifierSetWorktableRecipeBuilder}`.
  They are NOT skipped from the copy (rule 1), only flagged as unused at runtime; they still contain `getRecipes(DynamicRegistryManager)` / `serializeRecipeData` Mantle-1.11 TODO annotations.
- `package-info.java` (root) was copied too.
- No non-Java resources live under this source path (recipe JSON lives in `src/main/resources/data/tconstruct/recipes`, not this slice).

## 4. Substitutions beyond PORTING.md

| # | upstream | fabric |
|---|---|---|
| 1 | `FluidStack.getAmount()` int | Porting Lib returns `long` → `(int)` cast at 10 sites in 9 files (`casting/ICastingRecipe`, `casting/material/{CompositeCastingRecipe,MaterialCastingLookup,MaterialCastingRecipe,PartSwapCastingRecipe}`, `melting/{DisplayMeltingRecipe ×2,MaterialMeltingRecipe,IMeltingContainer,DamageableMeltingRecipe}`) |
| 2 | `FluidType.BUCKET_VOLUME` | local `FluidValues.BUCKET_VOLUME = 1000` (no Porting Lib constant) |
| 3 | `fluid.getFluidType()` | `((FluidExtension) fluid).getFluidType()` (mixin extension) |
| 4 | `FluidIngredient.getAmount(Fluid)` int | Mantle 1.9.291 returns `long` → `(int)` cast (AlloyRecipe, ItemCastingRecipe, MaterialFluidRecipe, PotionCastingRecipe, MeltingFuel) |
| 5 | `ForgeRegistries.POTIONS/FLUIDS.getValues()` | `Registries.POTION.stream().toList()` / `Registries.FLUID` |
| 6 | `ItemStack.getCraftingRemainingItem()` | `stack.getItem().hasRecipeRemainder() ? new ItemStack(stack.getItem().getRecipeRemainder()) : ItemStack.EMPTY` |
| 7 | `shrink/getContainerSize/getItem/setItem/setChanged/getTag/setTag/setHoverName` | `decrement/size/getStack/setStack/markDirty/getNbt/setNbt/setCustomName` |
| 8 | `DeferredRegister` + `init(IEventBus)` (TinkerRecipeTypes) | `LazyRegistrar.create(Registries.RECIPE_TYPE, MOD_ID)` + `register()`; **integrator must call `TinkerRecipeTypes.register()`** |
| 9 | `FinishedRecipe` API in datagen builders | `RecipeJsonProvider`: `getId→getRecipeId`, `getType→getSerializer`, `serializeRecipeData→serialize`, `serializeAdvancement→toAdvancementJson` |
| 10 | `isSpecial()` / `category()` | `isIgnoredInRecipeBook()` / `getCategory()` |
| 11 | `BuiltInRegistries.*`, `getKey`, `getTagOrEmpty`, `getHolder`, `Potions.EMPTY_ID`, `RegistryKey.location()`, `TagKey.location()`, `writeResourceLocation` | `Registries.*`, `getId`, `iterateEntries`, `getEntry`, `Potions.EMPTY_KEY`, `getValue()`, `id()`, `writeIdentifier` |
| 12 | `Ingredient.getItems()` in casting recipes | `getMatchingStacks()` |
| 13 | `RegistryEntry.is(TagKey)` | `isIn(TagKey)` |

## 5. Unresolved symbols (blocked, upstream code kept + `// TODO(fabric)`)

- Mantle 1.11 only (82 files): `slimeknights.mantle.data.loadable.*` (RecordLoadable, ContextKey, Loadable, Loadables, LoadableField, IntLoadable, BooleanLoadable, IngredientLoadable, `.record`, `.field`, `.primitive`, `.common`).
- Mantle 1.11 serializers/outputs (36 files): `LoadableRecipeSerializer`, `TypeAwareRecipeSerializer`, `SimpleRecipeSerializer`, `FluidOutput`, `ItemOutput#getTag`.
- Mantle 1.11 signatures (upstream code unchanged, annotated instead): `ICommonRecipe#getToastSymbol()` (5), `IMultiRecipe#getRecipes(DynamicRegistryManager)` (21), `LoggingRecipeSerializer` used as interface (3 — 1.9.291 has it as abstract class). Whole slice carries **45 `// TODO(fabric)` notes across 37 files**.
- Other slices: MaterialVariant/MaterialVariantId/IMaterial*/IMaterialValue (materials), ModifierEntry/ModifierId/SlotType/SlotCount/LazyModifier/ModifierSalvage/ModifierHooks/IModifiableDisplay (modifiers), IToolStackView/ToolStack/LazyToolStack/ModDataNBT/ToolDataNBT/ModifierNBT/MaterialNBT/ToolDefinition (tools), SimpleCache, IntRange, DuelSidedListener/RecipeCacheInvalidator (common), `Pattern`, `TinkerSmeltery` (smeltery).
- Forge-only with no Fabric equivalent in vendored jars (14 files): `IFluidHandler` + `FluidAction`, `ForgeCapabilities.FLUID_HANDLER_ITEM`, `net.minecraftforge.fluids.FluidUtil`, `AbstractIngredient`, `IIngredientSerializer`, `VanillaIngredientSerializer`, `CraftingHelper`, `FriendlyByteBuf.writeId/readById`.
- yarn package-private: `ShapedRecipe#readSymbols/getPattern/removePadding/createPatternMatrix` (used in `material/ShapedMaterialsRecipe.java`) need local copies later.

## 6. Integration tasks outside this slice (dangling references to deleted prototypes)

- `TConstruct.java:14` import `library.recipe.TinkerRecipes`; `:70` `TinkerRecipes.register();` → replace with `TinkerRecipeTypes.register();`
- `platform/DataReloadListener.java:20-21` imports `MaterialItemRegistry`, `ModifierRecipe`; `:73` `MaterialItemRegistry.rebuild(manager);`; `:212` `ModifierRecipe.readModifierId(...)`; `:213` `ModifierRecipe.readCost(...)`
- `shared/SelfTest.java:20-27` imports + usages at `:160` (RepairRecipe), `:201/:217` (IncrementalModifierRecipe), `:269` (ModifierRecipe), `:317` (TinkerRecipes.MATERIAL_TYPE), `:329` (PartBuildingRecipe), `:347` (ToolBuildingRecipe)

## 7. Verification

javac harness (no gradle): `tconstruct-fabric/port/{build_cp.sh,compile_recipe.sh}`; classpath from yarn MC jar + yarn-remapped Mantle/Porting-Lib jars in `build/loom-cache/remapped_working` + gradle-cached libs.
- `clean` subset (77 sources, 104 excluded): **255 errors / 25 files** — every error is `cannot find symbol` / `package does not exist` for symbols owned by other slices or Mantle 1.11; none attributable to a wrong yarn rename, and the only error kinds are `cannot find symbol` (232) + missing cross-slice packages (23).
- full slice (181 sources): **2988 errors / 131 files** (2023 `cannot find symbol`, 93 `method does not override` = lombok/other-slice artifacts, rest = missing `mantle.data.loadable.*` packages), dominated by `mantle.data.loadable` packages + cross-slice symbols; lombok-generated accessors disappear when Mantle-blocked files are in the batch (harness artifact, verify with `clean` only).
- harness note: `io.netty.handler.codec.DecoderException` (used by `RandomItem.java`) needed `netty-codec` on the javac classpath; with it added, `RandomItem.java` compiles cleanly.
