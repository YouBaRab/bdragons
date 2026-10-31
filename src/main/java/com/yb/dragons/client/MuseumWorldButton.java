package com.yb.dragons.client;

import com.yb.dragons.YBDragons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.List;
import java.util.Optional;

/**
 * A "Musée Bdragon" button on the Create World screen: one click creates the museum world
 * (flat world of 3 layers, creative, commands on). It uses the museum world type of the mod and,
 * if it is missing for any reason, builds the same flat world in code, so the museum is always
 * reachable.
 */
public final class MuseumWorldButton {
    public static final ResourceKey<WorldPreset> MUSEUM = ResourceKey.create(Registries.WORLD_PRESET, YBDragons.id("museum"));

    private MuseumWorldButton() {}

    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) return;
        Button create = findCreateButton(event.getListenersList());
        Component label = Component.translatable("museum.bdragon.button");
        int width = Minecraft.getInstance().font.width(label) + 16;
        int y = create != null ? create.getY() - 26 : screen.height - 60;
        Button button = Button.builder(label, b -> createMuseum(screen))
                .bounds(6, y, width, 20)
                .tooltip(Tooltip.create(Component.translatable("museum.bdragon.button.tooltip")))
                .build();
        event.addListener(button);
    }

    private static Button findCreateButton(List<GuiEventListener> listeners) {
        for (GuiEventListener l : listeners) {
            if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents t
                    && t.getKey().equals("selectWorld.create")) return b;
        }
        return null;
    }

    private static void createMuseum(CreateWorldScreen screen) {
        WorldCreationUiState ui = screen.getUiState();
        ui.setName(Component.translatable("generator.bdragon.museum").getString());
        ui.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
        ui.setAllowCommands(true);
        WorldCreationContext settings = ui.getSettings();
        Registry<WorldPreset> presets = settings.worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET);
        Optional<Holder.Reference<WorldPreset>> museum = presets.get(MUSEUM);
        if (museum.isPresent()) {
            ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(museum.get()));
        } else {
            YBDragons.LOGGER.warn("World preset {} not found, building the museum flat world in code", MUSEUM.identifier());
            presets.get(WorldPresets.FLAT).ifPresent(flat -> ui.setWorldType(new WorldCreationUiState.WorldTypeEntry(flat)));
            ui.updateDimensions((registryAccess, dimensions) -> {
                HolderGetter<Biome> biomes = registryAccess.lookupOrThrow(Registries.BIOME);
                HolderGetter<StructureSet> structures = registryAccess.lookupOrThrow(Registries.STRUCTURE_SET);
                HolderGetter<PlacedFeature> features = registryAccess.lookupOrThrow(Registries.PLACED_FEATURE);
                FlatLevelGeneratorSettings flat = FlatLevelGeneratorSettings.getDefault(biomes, structures, features)
                        .withBiomeAndLayers(List.of(
                                        new FlatLayerInfo(1, Blocks.BEDROCK),
                                        new FlatLayerInfo(1, Blocks.DIRT),
                                        new FlatLayerInfo(1, Blocks.GRASS_BLOCK)),
                                Optional.empty(), biomes.getOrThrow(Biomes.SUNFLOWER_PLAINS));
                return dimensions.replaceOverworldGenerator(registryAccess, new FlatLevelSource(flat));
            });
        }
        // same as clicking "Create New World"
        Button create = findCreateButton(List.copyOf(screen.children()));
        if (create != null) create.onPress(new MouseButtonInfo(0, 0));
    }
}
