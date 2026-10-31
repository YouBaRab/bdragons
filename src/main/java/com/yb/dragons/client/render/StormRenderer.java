package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.model.StormCloudModel;
import com.yb.dragons.client.model.StormRenderState;
import com.yb.dragons.client.model.StormSerpentModel;
import com.yb.dragons.dragon.AlphaStorm;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the Tempest Dragon, the alpha of the storm (used by {@link PlayerDragonRenderer} for the players who are this
 * alpha in their dragon form): the serpent, its glowing lightning cracks, eyes, crests, whiskers and talons, and the
 * storm clouds it holds in its talons when it moves in the air.
 */
public class StormRenderer extends LivingEntityRenderer<LivingEntity, StormRenderState, StormSerpentModel> {
    public static final Identifier TEXTURE = YBDragons.id("textures/entity/alpha/storm_serpent.png");
    public static final Identifier GLOW = YBDragons.id("textures/entity/alpha/storm_serpent_glow.png");
    public static final Identifier CLOUDS = YBDragons.id("textures/entity/alpha/storm_clouds.png");

    public StormRenderer(EntityRendererProvider.Context context) {
        super(context, new StormSerpentModel(context.bakeLayer(StormSerpentModel.LAYER)), 2.5F);
        addLayer(new GlowLayer(this));
        addLayer(new CloudLayer(this, new StormCloudModel(context.bakeLayer(StormCloudModel.LAYER))));
    }

    @Override
    public StormRenderState createRenderState() {
        return new StormRenderState();
    }

    @Override
    public Identifier getTextureLocation(StormRenderState state) {
        return TEXTURE;
    }

    /**
     * The entity scale attribute of the player is cancelled: the size is fully given by the model (drawn
     * {@link AlphaStorm#MODEL_SCALE} times bigger) and the growth of the ritual.
     */
    @Override
    protected void setupRotations(StormRenderState state, PoseStack pose, float bodyRot, float entityScale) {
        super.setupRotations(state, pose, bodyRot, entityScale);
        float k = 1.0F / Math.max(0.01F, entityScale);
        if (state.ritualLift != 0) pose.translate(0.0F, state.ritualLift * k, 0.0F);
        float s = AlphaStorm.MODEL_SCALE * state.grow * k;
        pose.scale(s, s, s);
    }

    @Override
    protected float getWhiteOverlayProgress(StormRenderState state) {
        return state.whiteFlash;
    }

    @Override
    protected float getShadowRadius(StormRenderState state) {
        return 2.5F * state.grow * (1 - 0.6F * state.air);
    }

    @Override
    public void submit(StormRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        // the whole body is posed once for this frame (the model and its clouds read the same pose)
        state.solve();
        super.submit(state, pose, collector, camera);
    }

    /** The lightning cracks, the eyes, the crests, the whiskers and the talons shine in the dark. */
    static final class GlowLayer extends RenderLayer<StormRenderState, StormSerpentModel> {
        GlowLayer(RenderLayerParent<StormRenderState, StormSerpentModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, StormRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    /** The storm clouds (translucent): in the talons, when it moves in the air. */
    static final class CloudLayer extends RenderLayer<StormRenderState, StormSerpentModel> {
        private final StormCloudModel clouds;

        CloudLayer(RenderLayerParent<StormRenderState, StormSerpentModel> parent, StormCloudModel clouds) {
            super(parent);
            this.clouds = clouds;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, StormRenderState state, float yRot, float xRot) {
            if (state.isInvisible || !StormCloudModel.visible(state)) return;
            collector.order(2).submitModel(clouds, state, pose, RenderTypes.entityTranslucent(CLOUDS), light, OverlayTexture.NO_OVERLAY,
                    state.outlineColor);
        }
    }
}
