package com.yb.dragons;

import com.yb.dragons.event.CommonEvents;
import com.yb.dragons.network.ModNetwork;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bdragon — become a dragon after a few ingredient quests, switch between the hybrid and
 * dragon forms, and grow through 5 stages by defeating the wild dragons in their lairs.
 *
 * Inspired by Morph (iChun, LGPL-3.0), Dragon Mounts: Legacy (Kay9, GPL-3.0) and Even More Dragons
 * (EvilNick, MIT). All the code here is original or ported from Dragon Mounts.
 */
@Mod(YBDragons.MODID)
public final class YBDragons {
    public static final String MODID = "bdragon";
    public static final Logger LOGGER = LoggerFactory.getLogger("YBDragons");

    public YBDragons(FMLJavaModLoadingContext context) {
        var modBus = context.getModBusGroup();
        ModRegistry.register(modBus);
        ModNetwork.init();
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        CommonEvents.register();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.yb.dragons.client.ClientSetup.init(modBus);
        }
        LOGGER.info("Bdragon loaded — que le feu des dragons soit avec toi !");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
