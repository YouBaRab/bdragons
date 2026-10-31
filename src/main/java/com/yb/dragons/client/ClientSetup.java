package com.yb.dragons.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yb.dragons.ModRegistry;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.hud.DragonHud;
import com.yb.dragons.client.model.DragonModel;
import com.yb.dragons.client.model.HybridModel;
import com.yb.dragons.client.model.DemonModel;
import com.yb.dragons.client.model.SunSteedModel;
import com.yb.dragons.client.model.MoonSerpentModel;
import com.yb.dragons.client.model.StormCloudModel;
import com.yb.dragons.client.model.StormSerpentModel;
import com.yb.dragons.client.model.TortoiseCloudModel;
import com.yb.dragons.client.model.TortoiseModel;
import com.yb.dragons.client.render.DemonRenderer;
import com.yb.dragons.client.render.SunSteedRenderer;
import com.yb.dragons.client.render.MoonRenderer;
import com.yb.dragons.client.render.StormRenderer;
import com.yb.dragons.client.render.TortoiseRenderer;
import com.yb.dragons.entity.AlphaHitbox;
import net.minecraft.client.renderer.entity.NoopRenderer;
import com.yb.dragons.client.render.DragonEntityRenderer;
import com.yb.dragons.client.render.HybridLayer;
import com.yb.dragons.client.render.PlayerDragonRenderer;
import com.yb.dragons.client.screen.CodexScreen;
import com.yb.dragons.entity.WildDragon;
import com.yb.dragons.network.ModNetwork;
import com.yb.dragons.player.DragonClaws;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

/** Client-only registrations: renderers, keys, HUD, the player morph, the hybrid layer, gliding and input. */
public final class ClientSetup {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(YBDragons.id("keys"));
    public static final KeyMapping KEY_BREATH = new KeyMapping("key.bdragon.breath", InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping KEY_ROAR = new KeyMapping("key.bdragon.roar", InputConstants.KEY_V, CATEGORY);
    public static final KeyMapping KEY_FORM = new KeyMapping("key.bdragon.toggle_form", InputConstants.KEY_H, CATEGORY);
    public static final KeyMapping KEY_GRIMOIRE = new KeyMapping("key.bdragon.grimoire", InputConstants.KEY_G, CATEGORY);
    /** Power of the alpha (B by default; every key can be changed in Options > Controls). */
    public static final KeyMapping KEY_ALPHA_POWER = new KeyMapping("key.bdragon.alpha_power", InputConstants.KEY_B, CATEGORY);

    private static boolean breathWasDown;

    private ClientSetup() {}

    public static void init(BusGroup modBus) {
        // the hybrid claws dig as fast on the client (prediction of the breaking progress) as on the server
        DragonClaws.clientLevel = p -> p == Minecraft.getInstance().player ? ClientDragonData.own().clawLevel() : 0;
        // the crosshair of the Sylvan Tortoise and of the Tempest Dragon goes through their own hit zones
        AlphaHitbox.isLocalPlayer = id -> Minecraft.getInstance().player != null && Minecraft.getInstance().player.getId() == id;

        EntityRenderersEvent.RegisterLayerDefinitions.BUS.addListener(event -> {
            event.registerLayerDefinition(DragonModel.LAYER, DragonModel::createBodyLayer);
            event.registerLayerDefinition(HybridModel.HEAD_LAYER, HybridModel::createHeadLayer);
            event.registerLayerDefinition(HybridModel.BODY_LAYER, HybridModel::createBodyLayer);
            event.registerLayerDefinition(TortoiseModel.LAYER, TortoiseModel::createLayer);
            event.registerLayerDefinition(TortoiseCloudModel.LAYER, TortoiseCloudModel::createLayer);
            event.registerLayerDefinition(StormSerpentModel.LAYER, StormSerpentModel::createLayer);
            event.registerLayerDefinition(StormCloudModel.LAYER, StormCloudModel::createLayer);
            event.registerLayerDefinition(DemonModel.LAYER, DemonModel::createLayer);
            event.registerLayerDefinition(SunSteedModel.LAYER, SunSteedModel::createLayer);
            event.registerLayerDefinition(MoonSerpentModel.LAYER, MoonSerpentModel::createLayer);
        });

        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> {
            event.registerEntityRenderer(ModRegistry.WILD_DRAGON.get(), ctx -> {
                DragonEntityRenderer<WildDragon> r = new DragonEntityRenderer<>(ctx);
                PlayerDragonRenderer.setRenderer(r);
                // the alpha of the forest (players only): built with the same context
                PlayerDragonRenderer.setTortoise(new TortoiseRenderer(ctx));
                // the alpha of the storm (players only)
                PlayerDragonRenderer.setSerpent(new StormRenderer(ctx));
                // the alpha of the Nether (players only)
                PlayerDragonRenderer.setDemon(new DemonRenderer(ctx));
                // the alpha of the sunlight (players only)
                PlayerDragonRenderer.setSteed(new SunSteedRenderer(ctx));
                // the alpha of the moonlight (players only)
                PlayerDragonRenderer.setMoon(new MoonRenderer(ctx));
                return r;
            });
            // the hit zones of the giant alphas are invisible
            event.registerEntityRenderer(ModRegistry.ALPHA_HITBOX.get(), NoopRenderer::new);
        });

        // horns, wings and tail of the hybrid form, on the normal player renderers
        EntityRenderersEvent.AddLayers.BUS.addListener(event -> {
            for (PlayerModelType type : event.getModelTypes()) {
                AvatarRenderer<AbstractClientPlayer> renderer = event.getPlayerRenderer(type);
                if (renderer != null) renderer.addLayer(new HybridLayer(renderer, event.getEntityModels()));
            }
        });

        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(KEY_BREATH);
            event.register(KEY_ROAR);
            event.register(KEY_FORM);
            event.register(KEY_GRIMOIRE);
            event.register(KEY_ALPHA_POWER);
        });

        AddGuiOverlayLayersEvent.BUS.addListener(event ->
                event.getLayeredDraw().add(YBDragons.id("dragon_hud"), DragonHud::extract));

        RenderAvatarEvent.Pre.BUS.addListener((RenderAvatarEvent.Pre event) -> PlayerDragonRenderer.onRenderAvatar(event));

        // a full dragon has no human arms in first person (the hybrid keeps them)
        RenderArmEvent.BUS.addListener((RenderArmEvent event) -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return false;
            ClientDragonData.Info info = ClientDragonData.get(player.getId());
            return info != null && info.isActiveDragon();
        });

        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            ClientAnimators.tick();
            AlphaCinematic.tick();
            handleKeys();
        });

        // gliding is computed after the movement of the local player
        TickEvent.PlayerTickEvent.Post.BUS.addListener(event -> {
            if (event.player() instanceof LocalPlayer lp && lp == Minecraft.getInstance().player) ClientGlide.tick(lp);
        });

        // one-click museum world on the Create World screen
        ScreenEvent.Init.Post.BUS.addListener(MuseumWorldButton::onScreenInit);

        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> {
            ClientDragonData.clear();
            ClientLairData.clear();
            ClientAnimators.clear();
            ClientGlide.reset();
            AlphaCinematic.reset();
        });
    }

    private static void handleKeys() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        boolean powers = ClientDragonData.own().hasPowers();

        boolean breathDown = powers && KEY_BREATH.isDown() && mc.gui.screen() == null;
        if (breathDown != breathWasDown) {
            ModNetwork.sendToServer(new ModNetwork.Action(ModNetwork.Action.BREATH, breathDown));
            breathWasDown = breathDown;
        }
        while (KEY_ROAR.consumeClick()) {
            if (powers) ModNetwork.sendToServer(new ModNetwork.Action(ModNetwork.Action.ROAR, true));
        }
        while (KEY_ALPHA_POWER.consumeClick()) {
            if (ClientDragonData.own().isDragon()) ModNetwork.sendToServer(new ModNetwork.Action(ModNetwork.Action.ALPHA_POWER, true));
        }
        while (KEY_FORM.consumeClick()) {
            if (ClientDragonData.own().isDragon()) ModNetwork.sendToServer(new ModNetwork.Action(ModNetwork.Action.CYCLE_FORM, true));
        }
        while (KEY_GRIMOIRE.consumeClick()) {
            mc.gui.setScreen(new CodexScreen());
        }
    }
}
