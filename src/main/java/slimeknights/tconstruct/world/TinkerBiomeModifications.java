package slimeknights.tconstruct.world;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.function.Predicate;

/**
 * Port of the Forge biome modifiers that shipped as {@code data/tconstruct/forge/biome_modifier/*.json}.
 * <p>
 * Forge reads those JSON files and adds features/spawns to biomes while the biome registry is built. Fabric has no
 * biome modifier registry, so the same additions are declared here with {@link BiomeModifications}. The selectors
 * mirror the JSON predicates one for one so the generated world keeps the same ore, geodes and slime spawns.
 */
public final class TinkerBiomeModifications {
  private TinkerBiomeModifications() {}

  /** Registers every biome modification; called from {@link TinkerWorld#init()} during mod initialization. */
  public static void init() {
    // cobalt_ore.json - #minecraft:is_nether, step underground_decoration
    BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.UNDERGROUND_DECORATION, TinkerWorld.placedSmallCobaltOre);
    BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.UNDERGROUND_DECORATION, TinkerWorld.placedLargeCobaltOre);

    // earth_geode.json / ichor_geode.json - geode features in the overworld and the nether
    BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.LOCAL_MODIFICATIONS, TinkerWorld.placedEarthGeode);
    BiomeModifications.addFeature(BiomeSelectors.foundInTheNether(), GenerationStep.Decoration.LOCAL_MODIFICATIONS, TinkerWorld.placedIchorGeode);

    // ender_geode.json - #minecraft:is_end and not minecraft:the_end, so the geode lands on the outer islands
    BiomeModifications.addFeature(context -> context.canGenerateIn(LevelStem.END) && context.getBiomeKey() != Biomes.THE_END,
                                  GenerationStep.Decoration.LOCAL_MODIFICATIONS, TinkerWorld.placedEnderGeode);

    // sky_geode.json - overworld minus oceans, beaches and rivers (the geode floats high, it looks wrong over water)
    Predicate<BiomeSelectionContext> skyGeodeBiomes = context ->
      context.canGenerateIn(LevelStem.OVERWORLD)
      && !context.hasTag(BiomeTags.IS_OCEAN)
      && !context.hasTag(BiomeTags.IS_DEEP_OCEAN)
      && !context.hasTag(BiomeTags.IS_BEACH)
      && !context.hasTag(BiomeTags.IS_RIVER);
    BiomeModifications.addFeature(skyGeodeBiomes, GenerationStep.Decoration.LOCAL_MODIFICATIONS, TinkerWorld.placedSkyGeode);

    // spawn_overworld_slime.json - sky slimes anywhere in the overworld
    BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, TinkerWorld.skySlimeEntity.get(), 100, 2, 4);
    // spawn_terracube.json - terracubes in the overworld, excluding the forge no_default_monsters tag that Fabric does
    // not have; the tag only opts biomes like mushroom fields out of hostile spawns, which the spawn rules handle
    BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, TinkerWorld.terracubeEntity.get(), 10, 2, 4);
    // spawn_end_slime.json - ender slimes in the end
    BiomeModifications.addSpawn(BiomeSelectors.foundInTheEnd(), MobCategory.MONSTER, TinkerWorld.enderSlimeEntity.get(), 10, 2, 4);
  }
}
