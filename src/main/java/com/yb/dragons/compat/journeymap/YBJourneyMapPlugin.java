package com.yb.dragons.compat.journeymap;

import com.yb.dragons.YBDragons;
import com.yb.dragons.client.ClientLairData;
import com.yb.dragons.dragon.DragonType;
import com.yb.dragons.network.ModNetwork;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.MarkerOverlay;
import journeymap.api.v2.client.model.MapImage;
import journeymap.api.v2.client.model.TextProperties;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;

import java.util.List;
import java.util.Map;

/**
 * JourneyMap integration (optional: this class is only loaded by JourneyMap itself).
 * Shows the dragon lairs the player knows or senses on the minimap and on the fullscreen map,
 * with an icon in the colours of the race and the rank of the dragon (I to V).
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class YBJourneyMapPlugin implements IClientPlugin {
    private IClientAPI api;
    private int shownVersion = -1;

    @Override
    public String getModId() {
        return YBDragons.MODID;
    }

    @Override
    public void initialize(IClientAPI api) {
        this.api = api;
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> refresh());
        YBDragons.LOGGER.info("JourneyMap detected: dragon lairs will be shown on the map");
    }

    /** Redraws all the markers when the server sent a new list of lairs. */
    private void refresh() {
        if (api == null) return;
        int version = ClientLairData.version();
        if (version == shownVersion) return;
        shownVersion = version;
        api.removeAll(YBDragons.MODID);
        if (!api.playerAccepts(YBDragons.MODID, DisplayType.Marker)) return;

        for (Map.Entry<String, List<ModNetwork.LairSync.Entry>> dim : ClientLairData.all().entrySet()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim.getKey()));
            for (ModNetwork.LairSync.Entry e : dim.getValue()) {
                try {
                    api.show(marker(dimension, e));
                } catch (Exception ex) {
                    YBDragons.LOGGER.warn("Cannot show the dragon lair marker: {}", ex.toString());
                }
            }
        }
    }

    private static MarkerOverlay marker(ResourceKey<Level> dimension, ModNetwork.LairSync.Entry e) {
        DragonType type = DragonType.byOrdinal(e.type());
        String rank = roman(e.rank());
        Identifier texture = YBDragons.id("textures/gui/lair/" + (e.defeated() ? "defeated" : type.id()) + ".png");
        // the stronger the dragon, the bigger the icon
        double size = 14 + e.rank() * 2;
        MapImage icon = new MapImage(texture, 32, 32).setDisplayWidth(size).setDisplayHeight(size)
                .setOpacity(e.known() ? 1.0F : 0.7F).centerAnchors();

        MarkerOverlay marker = new MarkerOverlay(YBDragons.MODID, new BlockPos(e.x(), 64, e.z()), icon);
        String title = Component.translatable("map.bdragon.lair", type.displayName(), rank).getString();
        if (e.defeated()) title += " " + Component.translatable("codex.bdragon.lairs.defeated").getString();
        else if (!e.known()) title += " " + Component.translatable("map.bdragon.sensed").getString();
        marker.setDimension(dimension);
        marker.setTitle(title);
        marker.setLabel(Component.translatable("map.bdragon.rank", rank).getString());
        marker.setDisplayOrder(1000 + e.rank());
        marker.setTextProperties(new TextProperties()
                .setColor(e.defeated() ? 0xAAAAAA : type.textColor())
                .setBackgroundColor(0x000000)
                .setBackgroundOpacity(0.5F)
                .setScale(1.0F)
                .setOffsetY((int) (size / 2) + 2));
        return marker;
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }
}
