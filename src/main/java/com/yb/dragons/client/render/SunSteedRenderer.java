package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.model.SunSteedModel;
import com.yb.dragons.client.model.SunSteedRenderState;
import com.yb.dragons.dragon.AlphaSun;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * Draws the Sun Steed, the alpha of the sunlight (used by {@link PlayerDragonRenderer} for the players who are this
 * alpha in their dragon form): the model, and its sun, halo and eyes shining in the dark.
 */
public class SunSteedRenderer extends LivingEntityRenderer<LivingEntity, SunSteedRenderState, SunSteedModel> {
    public static final Identifier TEXTURE = YBDragons.id("textures/entity/alpha/sun_steed.png");
    public static final Identifier GLOW = YBDragons.id("textures/entity/alpha/sun_steed_glow.png");

    public SunSteedRenderer(EntityRendererProvider.Context context) {
        super(context, new SunSteedModel(context.bakeLayer(SunSteedModel.LAYER)), 3.0F);
        addLayer(new GlowLayer(this));
    }

    @Override
    public SunSteedRenderState createRenderState() {
        return new SunSteedRenderState();
    }

    @Override
    public Identifier getTextureLocation(SunSteedRenderState state) {
        return TEXTURE;
    }

    /**
     * The entity scale attribute of the player is cancelled: the size is fully given by the model (drawn
     * {@link AlphaSun#MODEL_SCALE} times smaller than in the game) and the growth of the ritual.
     */
    @Override
    protected void setupRotations(SunSteedRenderState state, PoseStack pose, float bodyRot, float entityScale) {
        super.setupRotations(state, pose, bodyRot, entityScale);
        float k = 1.0F / Math.max(0.01F, entityScale);
        if (state.ritualLift != 0) pose.translate(0.0F, state.ritualLift * k, 0.0F);
        float s = AlphaSun.MODEL_SCALE * state.grow * k;
        pose.scale(s, s, s);
    }

    @Override
    protected float getWhiteOverlayProgress(SunSteedRenderState state) {
        return state.whiteFlash;
    }

    @Override
    protected float getShadowRadius(SunSteedRenderState state) {
        return 3.0F * state.grow * (0.4F + 0.6F * state.ground);
    }

    /** The sun, the halo and the eyes shine in the dark. */
    static final class GlowLayer extends RenderLayer<SunSteedRenderState, SunSteedModel> {
        GlowLayer(RenderLayerParent<SunSteedRenderState, SunSteedModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, SunSteedRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
