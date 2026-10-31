package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.model.MoonSerpentModel;
import com.yb.dragons.client.model.StormRenderState;
import com.yb.dragons.dragon.AlphaStorm;
import com.yb.dragons.dragon.StormSpine;
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
 * Draws the Moon Dragon, the alpha of the moonlight (used by {@link PlayerDragonRenderer} for the players who are this
 * alpha in their dragon form): the serpent of the night sky, its moons, stars, platform and eyes shining in the dark.
 */
public class MoonRenderer extends LivingEntityRenderer<LivingEntity, StormRenderState, MoonSerpentModel> {
    public static final Identifier TEXTURE = YBDragons.id("textures/entity/alpha/moon_dragon.png");
    public static final Identifier GLOW = YBDragons.id("textures/entity/alpha/moon_dragon_glow.png");

    public MoonRenderer(EntityRendererProvider.Context context) {
        super(context, new MoonSerpentModel(context.bakeLayer(MoonSerpentModel.LAYER)), 2.5F);
        addLayer(new GlowLayer(this));
    }

    @Override
    public StormRenderState createRenderState() {
        return new StormRenderState();
    }

    @Override
    public Identifier getTextureLocation(StormRenderState state) {
        return TEXTURE;
    }

    /** The scale attribute of the player is cancelled, like for the Tempest Dragon. */
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
        // the short neck of the Ender Dragon
        state.pose.lengths = StormSpine.MOON_LENGTHS;
        state.solve();
        super.submit(state, pose, collector, camera);
    }

    /** The moons, the stars, the platform and the eyes shine in the dark. */
    static final class GlowLayer extends RenderLayer<StormRenderState, MoonSerpentModel> {
        GlowLayer(RenderLayerParent<StormRenderState, MoonSerpentModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, StormRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
