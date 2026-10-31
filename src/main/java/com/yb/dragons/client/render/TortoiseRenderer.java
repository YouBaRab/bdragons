package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.model.TortoiseCloudModel;
import com.yb.dragons.client.model.TortoiseModel;
import com.yb.dragons.client.model.TortoiseRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Draws the Sylvan Tortoise, the alpha of the forest (used by {@link PlayerDragonRenderer} for the players
 * who are this alpha in their dragon form): the model (with the little plains biome of its shell), its glowing eyes,
 * the magic clouds under its paws and the Beam of Life.
 */
public class TortoiseRenderer extends LivingEntityRenderer<LivingEntity, TortoiseRenderState, TortoiseModel> {
    public static final Identifier TEXTURE = YBDragons.id("textures/entity/alpha/forest_tortoise.png");
    public static final Identifier GLOW = YBDragons.id("textures/entity/alpha/forest_tortoise_glow.png");
    public static final Identifier CLOUDS = YBDragons.id("textures/entity/alpha/forest_tortoise_clouds.png");

    public TortoiseRenderer(EntityRendererProvider.Context context) {
        super(context, new TortoiseModel(context.bakeLayer(TortoiseModel.LAYER)), 6.0F);
        addLayer(new GlowLayer(this));
        addLayer(new CloudLayer(this, new TortoiseCloudModel(context.bakeLayer(TortoiseCloudModel.LAYER))));
    }

    @Override
    public TortoiseRenderState createRenderState() {
        return new TortoiseRenderState();
    }

    @Override
    public Identifier getTextureLocation(TortoiseRenderState state) {
        return TEXTURE;
    }

    /**
     * The entity scale attribute of the player (6) is cancelled: the size is fully given by the model
     * (drawn {@link TortoiseModel#SCALE} times bigger) and the growth of the ritual.
     */
    @Override
    protected void setupRotations(TortoiseRenderState state, PoseStack pose, float bodyRot, float entityScale) {
        super.setupRotations(state, pose, bodyRot, entityScale);
        float k = 1.0F / Math.max(0.01F, entityScale);
        if (state.ritualLift != 0) pose.translate(0.0F, state.ritualLift * k, 0.0F);
        float s = TortoiseModel.SCALE * state.grow * k;
        pose.scale(s, s, s);
    }

    @Override
    protected float getWhiteOverlayProgress(TortoiseRenderState state) {
        return state.whiteFlash;
    }

    @Override
    protected float getShadowRadius(TortoiseRenderState state) {
        return 6.0F * state.grow;
    }

    @Override
    public void submit(TortoiseRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.beam > 0.01F && state.beamEnd != null && !state.isInvisible) submitBeam(state, pose, collector, camera);
        super.submit(state, pose, collector, camera);
    }

    // ------------------------------------------------------------------------------------------------------
    // The Beam of Life
    // ------------------------------------------------------------------------------------------------------

    /** The mouth of the model, in the frame of the entity (the same transforms as {@link LivingEntityRenderer#submit}). */
    private Vec3 mouth(TortoiseRenderState state) {
        PoseStack p = new PoseStack();
        p.scale(state.scale, state.scale, state.scale);
        setupRotations(state, p, state.bodyRot, state.scale);
        p.scale(-1.0F, -1.0F, 1.0F);
        scale(state, p);
        p.translate(0.0F, -1.501F, 0.0F);
        model.setupAnim(state);
        Vector3f m = model.mouth(p);
        return new Vec3(m.x, m.y, m.z);
    }

    private void submitBeam(TortoiseRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Vec3 from = mouth(state);
        Vec3 to = state.beamEnd;
        Vec3 cam = camera.pos.subtract(state.x, state.y, state.z);
        float t = state.ageInTicks, power = state.beam;
        float pulse = 1 + 0.12F * Mth.sin(t * 0.9F) + 0.06F * Mth.sin(t * 2.3F);
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
            // a wide soft glow, a green body and a white-gold core (additive: it shines)
            ribbon(p, vc, from, to, cam, 1.05F * power * pulse, 0.30F, 0.95F, 0.25F, 0.22F * power);
            ribbon(p, vc, from, to, cam, 0.55F * power * pulse, 0.55F, 1.0F, 0.40F, 0.45F * power);
            ribbon(p, vc, from, to, cam, 0.20F * power, 1.0F, 1.0F, 0.80F, 0.90F * power);
            // pulses of light running from the mouth to the end
            Vec3 axis = to.subtract(from);
            double length = axis.length();
            if (length > 1) {
                Vec3 dir = axis.scale(1 / length);
                int count = Math.min(48, (int) (length / 4) + 2);
                for (int i = 0; i < count; i++) {
                    double f = ((t * 0.9 + i * 4.0) % length);
                    Vec3 a = from.add(dir.scale(f)), b = from.add(dir.scale(Math.min(length, f + 1.2)));
                    ribbon(p, vc, a, b, cam, 0.42F * power, 0.85F, 1.0F, 0.55F, 0.55F * power);
                }
            }
            // a bright flower of light where it touches the ground
            if (state.beamHit) {
                Vec3 up = new Vec3(0, 1.6 * power * pulse, 0);
                ribbon(p, vc, to.subtract(up), to.add(up), cam, 1.4F * power * pulse, 0.6F, 1.0F, 0.45F, 0.35F * power);
            }
        });
    }

    /**
     * A strip of light from {@code a} to {@code b}, always facing the camera, cut in pieces so that a very long
     * beam stays facing the camera everywhere.
     */
    private static void ribbon(PoseStack.Pose pose, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float width,
                               float r, float g, float bl, float alpha) {
        Vec3 axis = b.subtract(a);
        double length = axis.length();
        if (length < 1.0E-3) return;
        int pieces = Math.max(1, Math.min(96, (int) Math.ceil(length / 6.0)));
        Vec3 step = axis.scale(1.0 / pieces);
        for (int i = 0; i < pieces; i++) {
            Vec3 p0 = a.add(step.scale(i)), p1 = a.add(step.scale(i + 1));
            Vec3 mid = p0.add(p1).scale(0.5);
            Vec3 side = axis.cross(mid.subtract(cam));
            double sl = side.length();
            if (sl < 1.0E-6) continue;
            side = side.scale(width * 0.5 / sl);
            vertex(pose, vc, p0.subtract(side), r, g, bl, alpha);
            vertex(pose, vc, p0.add(side), r, g, bl, alpha);
            vertex(pose, vc, p1.add(side), r, g, bl, alpha);
            vertex(pose, vc, p1.subtract(side), r, g, bl, alpha);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vc, Vec3 p, float r, float g, float b, float a) {
        vc.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(r, g, b, a);
    }

    // ------------------------------------------------------------------------------------------------------
    // Layers
    // ------------------------------------------------------------------------------------------------------

    /** The glowing yellow eyes. */
    static final class GlowLayer extends RenderLayer<TortoiseRenderState, TortoiseModel> {
        GlowLayer(RenderLayerParent<TortoiseRenderState, TortoiseModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, TortoiseRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    /** The magic clouds under the paws (translucent). */
    static final class CloudLayer extends RenderLayer<TortoiseRenderState, TortoiseModel> {
        private final TortoiseCloudModel clouds;

        CloudLayer(RenderLayerParent<TortoiseRenderState, TortoiseModel> parent, TortoiseCloudModel clouds) {
            super(parent);
            this.clouds = clouds;
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, TortoiseRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.hover + state.descend - state.propel * 0.5F < 0.02F) return;
            collector.order(2).submitModel(clouds, state, pose, RenderTypes.entityTranslucent(CLOUDS), light, OverlayTexture.NO_OVERLAY,
                    state.outlineColor);
        }
    }
}
