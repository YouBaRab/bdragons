package com.yb.dragons.client.model;

import com.yb.dragons.YBDragons;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The magic clouds that carry the Sylvan Tortoise in the air (drawn translucent, texture {@code forest_tortoise_clouds.png}).
 * <ul>
 *   <li>Hovering: under each of the 4 paws, a cushion of puffs gathers and swirls.</li>
 *   <li>Descending: the cushions slide down under the tortoise, stretch and spread (a slow, heavy descent).</li>
 *   <li>Flying forward: they stay behind the feet and fade away.</li>
 *   <li>Withdrawn into its shell: the cushions rise under the belly and carry the shell.</li>
 *   <li>On the ground: no clouds.</li>
 * </ul>
 */
public class TortoiseCloudModel extends EntityModel<TortoiseRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(YBDragons.id("alpha_tortoise_clouds"), "main");
    public static final int PUFFS = 9;
    /** Texture of the puffs (64 x 32). */
    static final int[] UV_CLOUD = {0, 0};
    /**
     * Puffs of one cushion: size (pixels of the model, 1 = 1/4 block), distance from the centre (blocks), height
     * (blocks), and the angle where it starts (radians): a big puff under the paw, a ring around it, two small outer
     * ones and two small ones that climb around the foot.
     */
    private static final float[][] PUFF = {
            {10, 6, 10, 0, 0, 0}, {7, 5, 7, 1.5F, 0.45F, 0}, {7, 6, 7, 1.5F, 0.1F, 1.57F}, {7, 5, 7, 1.5F, 0.55F, 3.14F},
            {7, 6, 7, 1.5F, 0.15F, 4.71F}, {5, 4, 5, 2.4F, -0.25F, 0.8F}, {5, 4, 5, 2.4F, 0.4F, 3.9F},
            {4, 3, 4, 0.9F, 1.05F, 2.2F}, {4, 3, 4, 0.9F, 0.9F, 5.3F}};

    private final ModelPart[] cushions = new ModelPart[4];
    private final ModelPart[][] puffs = new ModelPart[4][PUFFS];

    public TortoiseCloudModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        for (int i = 0; i < 4; i++) {
            cushions[i] = root.getChild("cushion" + i);
            for (int k = 0; k < PUFFS; k++) puffs[i][k] = cushions[i].getChild("puff" + k);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 4; i++) {
            float[] leg = TortoiseModel.LEGS[i];
            // under the paw (the foot is further out than the hip)
            float x = leg[0] + (leg[0] > 0 ? TortoiseModel.FOOT_OUT : -TortoiseModel.FOOT_OUT);
            float[] at = {x, 0, leg[2]};
            PartDefinition cushion = root.addOrReplaceChild("cushion" + i, CubeListBuilder.create(), TortoiseModel.at(at, TortoiseModel.ROOT));
            for (int k = 0; k < PUFFS; k++) {
                float[] p = PUFF[k];
                cushion.addOrReplaceChild("puff" + k, CubeListBuilder.create().texOffs(UV_CLOUD[0], UV_CLOUD[1])
                        .addBox(-p[0] / 2, -p[1] / 2, -p[2] / 2, p[0], p[1], p[2]), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(TortoiseRenderState s) {
        super.setupAnim(s);
        float hover = s.hover, descend = s.descend, propel = s.propel;
        // in its shell: the cushions rise to the belly (the legs are in) and gather a little towards the centre
        float shell = s.retract * s.retract * (3 - 2 * s.retract);
        // the clouds carry the tortoise when it stays still or goes down; flying forward, it leaves them behind
        float visible = Mth.clamp(hover + descend - propel * 0.5F, 0, 1);
        for (int i = 0; i < 4; i++) {
            ModelPart c = cushions[i];
            c.visible = visible > 0.02F;
            if (!c.visible) continue;
            // follows the bobbing of the body; slides down while descending, stays behind while flying forward
            c.y += TortoiseModel.px(-s.bob + 0.7F + 1.9F * descend - 2.1F * shell);
            c.z += TortoiseModel.px(1.5F * propel);
            c.x -= Math.signum(c.x) * TortoiseModel.px(0.6F * shell);
            float spin = s.swirl * (i % 2 == 0 ? 1 : -1) + i * 1.3F;
            for (int k = 0; k < PUFFS; k++) {
                float[] p = PUFF[k];
                ModelPart puff = puffs[i][k];
                float a = spin + p[5];
                // the puffs gather when hovering, spread when descending
                float r = p[3] * (0.8F + 0.2F * Mth.sin(s.swirl * 0.7F + k) + 0.5F * descend);
                puff.x = TortoiseModel.px(Mth.cos(a) * r);
                puff.z = TortoiseModel.px(Mth.sin(a) * r);
                float bobPuff = Mth.sin(s.ageInTicks * 0.11F + k * 1.9F + i) * 0.12F;
                // descending: the puffs trail downwards (the lower the outer ones)
                puff.y = TortoiseModel.px(-(p[4] * (1 - 0.6F * descend) + bobPuff) + descend * 0.4F * (k % 3));
                float size = visible * (0.85F + 0.15F * Mth.sin(s.ageInTicks * 0.09F + k));
                puff.xScale = puff.zScale = size * (1 + 0.25F * descend + 0.2F * shell);
                puff.yScale = size * (1 + 0.6F * descend);
                puff.yRot = a * 0.5F;
            }
        }
    }
}
