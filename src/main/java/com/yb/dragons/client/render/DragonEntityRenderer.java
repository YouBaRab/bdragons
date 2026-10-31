package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yb.dragons.client.ClientAnimators;
import com.yb.dragons.client.model.DragonAnimator;
import com.yb.dragons.client.model.DragonModel;
import com.yb.dragons.client.model.DragonRenderState;
import com.yb.dragons.entity.DragonVisual;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * Renderer of the wild and town dragons. The same instance also draws the players in dragon
 * form (see {@link PlayerDragonRenderer}) so every dragon of the game looks and moves the same way.
 */
public class DragonEntityRenderer<T extends Mob & DragonVisual> extends MobRenderer<T, DragonRenderState, DragonModel> {

    public DragonEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new DragonModel(context.bakeLayer(DragonModel.LAYER)), 1.0F);
        this.addLayer(new GlowLayer(this));
    }

    @Override
    public DragonRenderState createRenderState() {
        return new DragonRenderState();
    }

    @Override
    public void extractRenderState(T entity, DragonRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.type = entity.getDragonType();
        state.stage = entity.getVisualStage();
        state.renderScale = entity.getRenderScale();
        state.sitting = entity.isDragonSitting();
        state.breathing = entity.isBreathing();
        state.partialTick = partialTicks;
        DragonAnimator animator = ClientAnimators.get(entity);
        state.animator = animator;
        state.modelPitch = animator.getModelPitch(partialTicks);
        state.modelOffsetY = animator.getModelOffsetY();
    }

    @Override
    public Identifier getTextureLocation(DragonRenderState state) {
        return state.type.bodyTexture();
    }

    @Override
    protected void setupRotations(DragonRenderState state, PoseStack poseStack, float bodyRot, float entityScale) {
        super.setupRotations(state, poseStack, bodyRot, entityScale);
        applyDragonTransform(state, poseStack, entityScale);
    }

    /**
     * Size, height and pitch of the model (from Dragon Mounts). The entity scale attribute is
     * cancelled because the visual size is fully decided by {@link DragonRenderState#renderScale}.
     */
    public static void applyDragonTransform(DragonRenderState state, PoseStack poseStack, float entityScale) {
        // the levitation of the alpha ritual (in blocks, so divided by the entity scale already applied)
        if (state.ritualLift != 0) poseStack.translate(0.0F, state.ritualLift / Math.max(0.01F, entityScale), 0.0F);
        float s = state.renderScale / Math.max(0.01F, entityScale);
        poseStack.scale(s, s, s);
        poseStack.translate(0.0F, state.modelOffsetY, -1.5F);
        poseStack.translate(0.0F, 1.5F, 0.5F);
        poseStack.rotateDegrees(Axis.XP, state.modelPitch);
        poseStack.translate(0.0F, -1.5F, -0.5F);
    }

    @Override
    protected float getShadowRadius(DragonRenderState state) {
        return 1.6F * state.renderScale;
    }

    /** The body glows white before the awakening of the alpha (like a creeper about to explode). */
    @Override
    protected float getWhiteOverlayProgress(DragonRenderState state) {
        return state.whiteFlash;
    }

    /** Glowing eyes and runes of each race (emissive, visible in the dark). */
    public static class GlowLayer extends RenderLayer<DragonRenderState, DragonModel> {
        public GlowLayer(RenderLayerParent<DragonRenderState, DragonModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, DragonRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.eyes(state.type.glowTexture()),
                    lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
