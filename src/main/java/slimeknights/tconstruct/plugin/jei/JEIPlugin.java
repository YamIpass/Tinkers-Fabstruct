package slimeknights.tconstruct.plugin.jei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.category.extensions.IExtendableRecipeCategory;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import slimeknights.mantle.recipe.helper.RecipeHelper;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.TConstruct;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.ModifierManager;
import slimeknights.tconstruct.library.recipe.TinkerRecipeTypes;
import slimeknights.tconstruct.library.recipe.display.FilteredRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapedMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.material.ShapelessMaterialsRecipe;
import slimeknights.tconstruct.library.recipe.modifiers.ModifierRecipeLookup;
import slimeknights.tconstruct.library.recipe.modifiers.adding.IDisplayModifierRecipe;
import slimeknights.tconstruct.library.recipe.molding.MoldingRecipe;
import slimeknights.tconstruct.library.recipe.partbuilder.IDisplayPartBuilderRecipe;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayCraftingTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.IDisplayToolTinkering;
import slimeknights.tconstruct.library.recipe.tinkerstation.building.ToolBuildingRecipe;
import slimeknights.tconstruct.library.recipe.worktable.IModifierWorktableRecipe;
import slimeknights.tconstruct.library.tools.SlotType;
import slimeknights.tconstruct.library.tools.SlotType.SlotCount;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.item.IModifiableDisplay;
import slimeknights.tconstruct.library.tools.layout.StationSlotLayoutLoader;
import slimeknights.tconstruct.library.tools.nbt.MaterialNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import slimeknights.tconstruct.library.tools.part.IMaterialItem;
import slimeknights.tconstruct.plugin.jei.material.ShapedMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.material.ShapelessMaterialsExtension;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierBookmarkIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierRecipeCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ModifierWorktableCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientHelper;
import slimeknights.tconstruct.plugin.jei.modifiers.SlotIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringCategory;
import slimeknights.tconstruct.plugin.jei.modifiers.ToolTinkeringExtension;
import slimeknights.tconstruct.plugin.jei.partbuilder.MaterialItemList;
import slimeknights.tconstruct.plugin.jei.partbuilder.PartBuilderCategory;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientHelper;
import slimeknights.tconstruct.plugin.jei.partbuilder.PatternIngredientRenderer;
import slimeknights.tconstruct.plugin.jei.transfer.TinkerStationTransferInfo;
import slimeknights.tconstruct.plugin.jei.transfer.ToolInventoryTransferInfo;
import slimeknights.tconstruct.plugin.jei.util.PotionSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolPartSubtypeInterpreter;
import slimeknights.tconstruct.plugin.jei.util.ToolSubtypeInterpreter;
import slimeknights.tconstruct.smeltery.TinkerSmeltery;
import slimeknights.tconstruct.smeltery.block.component.SearedTankBlock.TankType;
import slimeknights.tconstruct.smeltery.item.CopperCanItem;
import slimeknights.tconstruct.smeltery.item.TankItem;
import slimeknights.tconstruct.tables.TinkerTables;
import slimeknights.tconstruct.tools.TinkerModifiers;
import slimeknights.tconstruct.tools.item.CreativeSlotItem;
import slimeknights.tconstruct.tools.item.ModifierCrystalItem;
import slimeknights.tconstruct.tables.recipe.CraftingTableRepairKitRecipe;
import slimeknights.tconstruct.tables.recipe.TinkerStationRepairRecipe;

/**
 * JEI plugin for the station recipes (tinker station, part builder, modifier table, molding).
 *
 * <p>Fluid based recipe categories (melting, casting, alloying, entity melting, severing, fuel) are
 * not registered yet: the Fabric JEI build exposes fluids through {@code FabricTypes.FLUID_STACK}
 * and {@code JeiFluidIngredient} rather than the Forge fluid ingredient type the upstream plugin was
 * written against, so those categories need a rewrite rather than a straight copy.</p>
 */
@JeiPlugin
public class JEIPlugin implements IModPlugin {
  @Override
  public ResourceLocation getPluginUid() {
    return TConstructJEIConstants.PLUGIN;
  }

  @Override
  public void registerCategories(IRecipeCategoryRegistration registry) {
    IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
    registry.addRecipeCategories(new MoldingRecipeCategory(guiHelper));
    registry.addRecipeCategories(new ModifierRecipeCategory(guiHelper));
    registry.addRecipeCategories(new ToolBuildingCategory(guiHelper));
    registry.addRecipeCategories(new ToolTinkeringCategory(guiHelper));
    ToolTinkeringExtension.prepareDrawables(guiHelper);
    registry.addRecipeCategories(new PartBuilderCategory(guiHelper));
    registry.addRecipeCategories(new ModifierWorktableCategory(guiHelper));
  }

  @Override
  public void registerIngredients(IModIngredientRegistration registration) {
    List<ModifierEntry> modifiers = Collections.emptyList();
    if (Config.CLIENT.showModifiersInJEI.get()) {
      modifiers = ModifierRecipeLookup.getRecipeModifierList();
    }
    registration.register(TConstructJEIConstants.MODIFIER_TYPE, modifiers, new ModifierIngredientHelper(), ModifierBookmarkIngredientRenderer.INSTANCE);
    registration.register(TConstructJEIConstants.MATERIAL_TYPE, List.of(), new slimeknights.tconstruct.plugin.jei.material.MaterialIngredientHelper(), slimeknights.tconstruct.plugin.jei.material.MaterialIconIngredientRenderer.INSTANCE);
    registration.register(TConstructJEIConstants.PATTERN_TYPE, List.of(), new PatternIngredientHelper(), PatternIngredientRenderer.INSTANCE);
    List<SlotCount> slots = SlotType.getAllSlotTypes().stream().map(type -> new SlotCount(type, 1)).toList();
    SlotIngredientRenderer.clearCache();
    registration.register(TConstructJEIConstants.SLOT_TYPE, slots, new SlotIngredientHelper(), SlotIngredientRenderer.INGREDIENT);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registry) {
    IExtendableRecipeCategory<CraftingRecipe, ICraftingCategoryExtension> craftingCategory = registry.getCraftingCategory();
    craftingCategory.addCategoryExtension(ShapedMaterialsRecipe.class, ShapedMaterialsExtension::create);
    craftingCategory.addCategoryExtension(ShapelessMaterialsRecipe.class, ShapelessMaterialsExtension::shapeless);
    craftingCategory.addCategoryExtension(IDisplayCraftingTinkering.class, ToolTinkeringExtension::new);
  }

  /** Finds the first recipe of the given type */
  @Nullable
  private static <T, C extends net.minecraft.world.Container, R extends net.minecraft.world.item.crafting.Recipe<C>> T findFirst(RecipeManager manager, RecipeType<R> type, Class<T> clazz) {
    return manager.byType(type).values().stream().filter(clazz::isInstance).map(clazz::cast).findFirst().orElse(null);
  }

  @Override
  public void registerRecipes(IRecipeRegistration register) {
    Level level = Minecraft.getInstance().level;
    assert level != null;
    RegistryAccess access = level.registryAccess();
    RecipeManager manager = level.getRecipeManager();

    // molding
    List<MoldingRecipe> moldingRecipes = new ArrayList<>();
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_TABLE.get(), MoldingRecipe.class));
    moldingRecipes.addAll(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MOLDING_BASIN.get(), MoldingRecipe.class));
    register.addRecipes(TConstructJEIConstants.MOLDING, moldingRecipes);

    // modifiers
    List<IDisplayModifierRecipe> modifierRecipes = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayModifierRecipe.class)
      .stream()
      .sorted((r1, r2) -> {
        SlotType t1 = r1.getSlotType();
        SlotType t2 = r2.getSlotType();
        String n1 = t1 == null ? "zzzzzzzzzz" : t1.getName();
        String n2 = t2 == null ? "zzzzzzzzzz" : t2.getName();
        return n1.compareTo(n2);
      }).collect(Collectors.toList());
    register.addRecipes(TConstructJEIConstants.MODIFIERS, modifierRecipes);
    List<IDisplayToolTinkering> toolModification = FilteredRecipe.unfiltered(RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), IDisplayToolTinkering.class));
    register.addRecipes(TConstructJEIConstants.TOOL_MODIFICATION, toolModification);

    // tool building
    List<ToolBuildingRecipe> toolBuilding = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.TINKER_STATION.get(), ToolBuildingRecipe.class)
      .stream()
      .sorted(Comparator.comparingInt(recipe -> StationSlotLayoutLoader.getInstance().get(recipe.getLayoutSlotId()).getSortIndex()))
      .toList();
    register.addRecipes(TConstructJEIConstants.TOOL_BUILDING, toolBuilding);

    // part builder
    MaterialItemList.setRecipes(List.of());
    List<IDisplayPartBuilderRecipe> partBuilder = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.PART_BUILDER.get(), IDisplayPartBuilderRecipe.class);
    register.addRecipes(TConstructJEIConstants.PART_BUILDER, partBuilder);

    // modifier worktable
    List<IModifierWorktableRecipe> worktable = RecipeHelper.getJEIRecipes(access, manager, TinkerRecipeTypes.MODIFIER_WORKTABLE.get(), IModifierWorktableRecipe.class)
      .stream().filter(recipe -> {
        if (recipe.getModifierOptions(null).isEmpty()) {
          TConstruct.LOG.debug("Hiding Modifier Worktable recipe {} as it has no valid modifiers", recipe.getId());
          return false;
        }
        return true;
      }).toList();
    register.addRecipes(TConstructJEIConstants.MODIFIER_WORKTABLE, worktable);

    TConstruct.LOG.info("[jei] station recipes: tool building={}, tool modification={}, modifiers={}, part builder={}, molding={}, worktable={}",
      toolBuilding.size(), toolModification.size(), modifierRecipes.size(), partBuilder.size(), moldingRecipes.size(), worktable.size());

    // copy tinker station repair recipes to the crafting table
    CraftingTableRepairKitRecipe craftingRepair = findFirst(manager, RecipeType.CRAFTING, CraftingTableRepairKitRecipe.class);
    TinkerStationRepairRecipe tinkerRepair = findFirst(manager, TinkerRecipeTypes.TINKER_STATION.get(), TinkerStationRepairRecipe.class);
    if (craftingRepair != null && tinkerRepair != null) {
      ResourceLocation id = craftingRepair.getId();
      List<IDisplayCraftingTinkering> recipes = tinkerRepair.getRecipes(access);
      List<CraftingRecipe> newRecipes = new ArrayList<>();
      TinkerStationRepairRecipe.setCraftingId(recipes, id);
      for (IDisplayCraftingTinkering recipe : recipes) {
        if (!recipe.isFiltered()) {
          newRecipes.add(recipe);
        }
      }
      if (!recipes.isEmpty()) {
        register.addRecipes(RecipeTypes.CRAFTING, newRecipes);
      }
    }
  }

  /** Adds all entries from the given modifier tag to catalysts for the given recipe type. */
  private static void addModifierCatalyst(IRecipeCatalystRegistration registry, TagKey<Modifier> tag, mezz.jei.api.recipe.RecipeType<?>... types) {
    for (Modifier modifier : ModifierManager.getTagValues(tag)) {
      registry.addRecipeCatalyst(TConstructJEIConstants.MODIFIER_TYPE, new ModifierEntry(modifier, 1), types);
    }
  }

  /** Adds a table as a catalyst */
  private static void addTableCatalyst(IRecipeCatalystRegistration registry, ItemLike table, TagKey<Item> tag, boolean addDefault, mezz.jei.api.recipe.RecipeType<?>... types) {
    List<ItemStack> list = new ArrayList<>();
    if (addDefault) {
      list.add(new ItemStack(table));
    }
    RetexturedHelper.addTagVariants(stack -> {
      list.add(stack);
      return false;
    }, table, tag);
    Consumer<IIngredientAcceptor<?>> ingredientAdder = acceptor -> acceptor.addItemStacks(list);
    for (mezz.jei.api.recipe.RecipeType<?> type : types) {
      registry.addRecipeCatalyst(type, ingredientAdder);
    }
  }

  @Override
  public void registerRecipeCatalysts(IRecipeCatalystRegistration registry) {
    // tables
    addTableCatalyst(registry, TinkerTables.craftingStation, ItemTags.LOGS, true, RecipeTypes.CRAFTING);
    addTableCatalyst(registry, TinkerTables.partBuilder, ItemTags.PLANKS, true, TConstructJEIConstants.PART_BUILDER);
    addTableCatalyst(registry, TinkerTables.tinkerStation, ItemTags.PLANKS, true, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.tinkersAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.scorchedAnvil, TinkerTags.Items.ANVIL_METAL, false, TConstructJEIConstants.MODIFIERS, TConstructJEIConstants.TOOL_BUILDING, TConstructJEIConstants.TOOL_MODIFICATION);
    addTableCatalyst(registry, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK, true, TConstructJEIConstants.MODIFIER_WORKTABLE);

    // modifiers shown as crafting ingredients
    addModifierCatalyst(registry, TinkerTags.Modifiers.CRAFTING, RecipeTypes.CRAFTING);
  }

  @Override
  public void registerItemSubtypes(ISubtypeRegistration registry) {
    // retexturable blocks
    IIngredientSubtypeInterpreter<ItemStack> tables = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        return RetexturedHelper.getTextureName(stack);
      }
      return IIngredientSubtypeInterpreter.NONE;
    };
    registry.registerSubtypeInterpreter(TinkerTables.craftingStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.partBuilder.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.tinkerStation.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerTables.modifierWorktable.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.smelteryController.asItem(), tables);
    registry.registerSubtypeInterpreter(TinkerSmeltery.foundryController.asItem(), tables);

    // anvils have both texture and material blocks
    IIngredientSubtypeInterpreter<ItemStack> anvils = (stack, context) -> {
      if (context == UidContext.Ingredient) {
        String name = RetexturedHelper.getTextureName(stack);
        if (!name.isEmpty()) {
          return '#' + name;
        }
        return ToolPartSubtypeInterpreter.INSTANCE.apply(stack, UidContext.Ingredient);
      }
      return IIngredientSubtypeInterpreter.NONE;
    };
    registry.registerSubtypeInterpreter(TinkerTables.tinkersAnvil.asItem(), anvils);
    registry.registerSubtypeInterpreter(TinkerTables.scorchedAnvil.asItem(), anvils);

    // potions (item form only, the fluid form needs the Fabric fluid ingredient type)
    registry.registerSubtypeInterpreter(slimeknights.tconstruct.fluids.TinkerFluids.potion.asItem(), (PotionSubtypeInterpreter<ItemStack>) ItemStack::getTag);

    // parts
    for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
      registry.registerSubtypeInterpreter(item.value(), ToolPartSubtypeInterpreter.INSTANCE);
    }

    // tools
    for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MULTIPART_TOOL)) {
      Item item = holder.value();
      registry.registerSubtypeInterpreter(item, holder.is(TinkerTags.Items.SINGLEPART_TOOL) ? ToolSubtypeInterpreter.FIRST : ToolSubtypeInterpreter.INGREDIENT);
    }

    // fluid containers have types based on fluid, don't bother with different sizes
    registry.registerSubtypeInterpreter(TinkerSmeltery.copperCan.get(), (stack, context) -> CopperCanItem.getSubtype(stack));
    IIngredientSubtypeInterpreter<ItemStack> tankInterpreter = (stack, context) -> TankItem.getSubtype(stack);
    for (TankType type : TankType.values()) {
      registry.registerSubtypeInterpreter(TinkerSmeltery.searedTank.get(type).asItem(), tankInterpreter);
      registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedTank.get(type).asItem(), tankInterpreter);
    }
    registry.registerSubtypeInterpreter(TinkerSmeltery.searedLantern.asItem(), tankInterpreter);
    registry.registerSubtypeInterpreter(TinkerSmeltery.scorchedLantern.asItem(), tankInterpreter);

    registry.registerSubtypeInterpreter(TinkerModifiers.creativeSlotItem.get(), (stack, context) -> {
      SlotType slotType = CreativeSlotItem.getSlot(stack);
      return slotType != null ? slotType.getName() : "";
    });
    registry.registerSubtypeInterpreter(TinkerModifiers.modifierCrystal.get(), (stack, context) -> {
      ModifierId id = ModifierCrystalItem.getModifier(stack);
      return id == null ? "" : id.toString();
    });
  }

  @Override
  public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
    IRecipeTransferHandlerHelper helper = registration.getTransferHelper();
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.MODIFIERS, helper), TConstructJEIConstants.MODIFIERS);
    registration.addRecipeTransferHandler(new TinkerStationTransferInfo<>(TConstructJEIConstants.TOOL_BUILDING, helper), TConstructJEIConstants.TOOL_BUILDING);
    registration.addRecipeTransferHandler(new ToolInventoryTransferInfo(helper), RecipeTypes.CRAFTING);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
    IIngredientManager manager = jeiRuntime.getIngredientManager();
    List<ItemStack> removeItems = new ArrayList<>();
    Consumer<ItemStack> removeItem = removeItems::add;
    List<ItemStack> addItems = new ArrayList<>();
    Consumer<ItemStack> addItem = addItems::add;
    List<ItemStack> hideItems = new ArrayList<>();
    Consumer<ItemStack> hideItem = hideItems::add;
    List<ItemStack> showItems = new ArrayList<>();
    Consumer<ItemStack> showItem = showItems::add;

    // shown via the modifiers / slots instead
    removeItems.add(new ItemStack(TinkerModifiers.modifierCrystal));
    ModifierCrystalItem.addVariants(removeItem);
    removeItems.add(new ItemStack(TinkerModifiers.creativeSlotItem));
    TinkerModifiers.creativeSlotItem.get().addVariants(removeItem);

    // tool config filters to 1 material, easiest to just remove all then add back the 1
    String showOnlyTools = Config.CLIENT.showOnlyToolMaterial.get();
    if (!showOnlyTools.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.MODIFIABLE)) {
        if (item.value() instanceof IModifiable modifiable) {
          ToolBuildHandler.addVariants(hideItem, modifiable, "");
          ToolBuildHandler.addVariants(showItem, modifiable, showOnlyTools);
        }
      }
    }
    // parts work the same as tools
    String showOnlyParts = Config.CLIENT.showOnlyPartMaterial.get();
    if (!showOnlyParts.isEmpty()) {
      for (Holder<Item> item : BuiltInRegistries.ITEM.getTagOrEmpty(TinkerTags.Items.TOOL_PARTS)) {
        if (item.value() instanceof IMaterialItem part) {
          part.addVariants(hideItem, "");
          part.addVariants(showItem, showOnlyParts);
        }
      }
    }

    // tables have their creative tabs hidden, so variants only show if we add them back
    Predicate<ItemStack> addVariants = stack -> {
      addItems.add(stack);
      return false;
    };
    if (Config.CLIENT.showAllTableVariants.get()) {
      RetexturedHelper.addTagVariants(addVariants, TinkerTables.craftingStation, ItemTags.LOGS);
      RetexturedHelper.addTagVariants(addVariants, TinkerTables.partBuilder, ItemTags.PLANKS);
      RetexturedHelper.addTagVariants(addVariants, TinkerTables.tinkerStation, ItemTags.PLANKS);
      RetexturedHelper.addTagVariants(addVariants, TinkerTables.modifierWorktable, TinkerTags.Items.WORKSTATION_ROCK);
    }
    if (Config.CLIENT.showAllAnvilVariants.get()) {
      ((IMaterialItem) TinkerTables.tinkersAnvil.asItem()).addVariants(addItem, "");
      ((IMaterialItem) TinkerTables.scorchedAnvil.asItem()).addVariants(addItem, "");
    }

    manager.removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, removeItems);
    if (!addItems.isEmpty()) {
      manager.addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, addItems);
    }
    if (!hideItems.isEmpty()) {
      jeiRuntime.getIngredientVisibility().hideIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hideItems, List.of(UidContext.Ingredient));
    }
    if (!showItems.isEmpty()) {
      jeiRuntime.getIngredientVisibility().unhideIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, showItems, List.of(UidContext.Ingredient));
    }
  }
}
