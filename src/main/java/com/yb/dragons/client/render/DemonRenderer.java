package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yb.dragons.YBDragons;
import com.yb.dragons.client.model.DemonModel;
import com.yb.dragons.client.model.DemonRenderState;
import com.yb.dragons.dragon.AlphaNether;
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

/**
 * Draws the Demon of the Nether, the alpha of the Nether (used by {@link PlayerDragonRenderer} for the players who are
 * this alpha in their dragon form): the model, its glowing eyes and lava, and the circle of incantation of its
 * Territory, drawn in fire above it while it raises its hand.
 */
public class DemonRenderer extends LivingEntityRenderer<LivingEntity, DemonRenderState, DemonModel> {
    public static final Identifier TEXTURE = YBDragons.id("textures/entity/alpha/nether_demon.png");
    public static final Identifier GLOW = YBDragons.id("textures/entity/alpha/nether_demon_glow.png");
    /** The circle of incantation: height above the feet and radius (blocks). */
    static final float CIRCLE_HEIGHT = 8.5F, CIRCLE_RADIUS = 3.2F;

    public DemonRenderer(EntityRendererProvider.Context context) {
        super(context, new DemonModel(context.bakeLayer(DemonModel.LAYER)), 1.6F);
        addLayer(new GlowLayer(this));
    }

    @Override
    public DemonRenderState createRenderState() {
        return new DemonRenderState();
    }

    @Override
    public Identifier getTextureLocation(DemonRenderState state) {
        return TEXTURE;
    }

    /**
     * The entity scale attribute of the player is cancelled: the size is fully given by the model (drawn
     * {@link AlphaNether#MODEL_SCALE} times bigger) and the growth of the ritual.
     */
    @Override
    protected void setupRotations(DemonRenderState state, PoseStack pose, float bodyRot, float entityScale) {
        super.setupRotations(state, pose, bodyRot, entityScale);
        float k = 1.0F / Math.max(0.01F, entityScale);
        if (state.ritualLift != 0) pose.translate(0.0F, state.ritualLift * k, 0.0F);
        float s = AlphaNether.MODEL_SCALE * state.grow * k;
        pose.scale(s, s, s);
    }

    @Override
    protected float getWhiteOverlayProgress(DemonRenderState state) {
        return state.whiteFlash;
    }

    @Override
    protected float getShadowRadius(DemonRenderState state) {
        return 1.6F * state.grow * (1 - 0.5F * (state.fly + state.hover));
    }

    @Override
    public void submit(DemonRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.castAge >= 0 && state.castAge < AlphaNether.CAST_TICKS + 25 && !state.isInvisible) submitCircle(state, pose, collector);
        super.submit(state, pose, collector, camera);
    }

    // ------------------------------------------------------------------------------------------------------
    // The circle of incantation
    // ------------------------------------------------------------------------------------------------------

    /**
     * A circle of fire above the demon: an outer ring drawn in the first two seconds, an inner ring, runes between
     * them, then a five pointed star; it turns slowly and fades out at the end of the incantation.
     */
    private void submitCircle(DemonRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        float age = state.castAge;
        float draw = Mth.clamp(age / 40F, 0, 1);
        float star = Mth.clamp((age - 40) / 25F, 0, 1);
        float fade = Mth.clamp((AlphaNether.CAST_TICKS + 25 - age) / 25F, 0, 1) * Mth.clamp(age / 6F, 0, 1);
        if (fade <= 0) return;
        float h = CIRCLE_HEIGHT * state.grow, r = CIRCLE_RADIUS * state.grow;
        float spin = age * 0.02F;
        float pulse = 0.85F + 0.15F * Mth.sin(age * 0.4F);
        collector.submitCustomGeometry(pose, RenderTypes.lightning(), (p, vc) -> {
            // the outer and inner rings, drawn around as the incantation goes on
            ring(p, vc, h, r, 0.14F, spin, draw, 1.0F, 0.45F, 0.08F, 0.9F * fade * pulse);
            ring(p, vc, h, r * 1.08F, 0.05F, -spin, draw, 1.0F, 0.75F, 0.25F, 0.7F * fade);
            ring(p, vc, h, r * 0.72F, 0.1F, -spin * 1.5F, draw, 1.0F, 0.3F, 0.05F, 0.8F * fade * pulse);
            // runes between the rings
            int runes = 12;
            for (int i = 0; i < runes; i++) {
                if (i >= runes * draw) break;
                float a = spin * 2 + i * Mth.TWO_PI / runes;
                float cx = Mth.cos(a) * r * 0.86F, cz = Mth.sin(a) * r * 0.86F, s = 0.16F * (i % 3 == 0 ? 1.4F : 1.0F);
                quad(p, vc, cx - s, h, cz - s, cx + s, h, cz - s, cx + s, h, cz + s, cx - s, h, cz + s, 1.0F, 0.85F, 0.35F, 0.9F * fade);
            }
            // the star: five lines joining the points of the inner ring
            if (star > 0) {
                for (int i = 0; i < 5; i++) {
                    float a0 = spin * -1.5F + i * Mth.TWO_PI / 5, a1 = a0 + 2 * Mth.TWO_PI / 5;
                    float x0 = Mth.cos(a0) * r * 0.72F, z0 = Mth.sin(a0) * r * 0.72F;
                    float x1 = Mth.cos(a1) * r * 0.72F, z1 = Mth.sin(a1) * r * 0.72F;
                    line(p, vc, x0, z0, x0 + (x1 - x0) * star, z0 + (z1 - z0) * star, h, 0.07F, 1.0F, 0.6F, 0.15F, 0.85F * fade * pulse);
                }
            }
        });
    }

    /** A flat ring of the given width, drawn from its start angle over {@code part} of its turn (both faces). */
    private static void ring(PoseStack.Pose p, VertexConsumer vc, float y, float r, float width, float start, float part,
                             float red, float green, float blue, float alpha) {
        int n = 72;
        int count = (int) Math.ceil(n * part);
        for (int i = 0; i < count; i++) {
            float a0 = start + i * Mth.TWO_PI / n, a1 = start + Math.min(i + 1, n * part) * Mth.TWO_PI / n;
            float ri = r - width, ro = r + width;
            quad(p, vc, Mth.cos(a0) * ri, y, Mth.sin(a0) * ri, Mth.cos(a0) * ro, y, Mth.sin(a0) * ro,
                    Mth.cos(a1) * ro, y, Mth.sin(a1) * ro, Mth.cos(a1) * ri, y, Mth.sin(a1) * ri, red, green, blue, alpha);
        }
    }

    /** A flat line from (x0, z0) to (x1, z1) at height y (both faces). */
    private static void line(PoseStack.Pose p, VertexConsumer vc, float x0, float z0, float x1, float z1, float y, float width,
                             float red, float green, float blue, float alpha) {
        float dx = x1 - x0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4F) return;
        float nx = -dz / len * width, nz = dx / len * width;
        quad(p, vc, x0 - nx, y, z0 - nz, x0 + nx, y, z0 + nz, x1 + nx, y, z1 + nz, x1 - nx, y, z1 - nz, red, green, blue, alpha);
    }

    /** A quad seen from both sides. */
    private static void quad(PoseStack.Pose p, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz, float r, float g, float b, float a) {
        vc.addVertex(p, ax, ay, az).setColor(r, g, b, a);
        vc.addVertex(p, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(p, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(p, dx, dy, dz).setColor(r, g, b, a);
        vc.addVertex(p, dx, dy, dz).setColor(r, g, b, a);
        vc.addVertex(p, cx, cy, cz).setColor(r, g, b, a);
        vc.addVertex(p, bx, by, bz).setColor(r, g, b, a);
        vc.addVertex(p, ax, ay, az).setColor(r, g, b, a);
    }

    /** The eyes and the lava of its skin shine in the dark. */
    static final class GlowLayer extends RenderLayer<DemonRenderState, DemonModel> {
        GlowLayer(RenderLayerParent<DemonRenderState, DemonModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, DemonRenderState state, float yRot, float xRot) {
            if (state.isInvisible || state.deathTime > 0) return;
            collector.order(1).submitModel(getParentModel(), state, pose, RenderTypes.eyes(GLOW), light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }
}
