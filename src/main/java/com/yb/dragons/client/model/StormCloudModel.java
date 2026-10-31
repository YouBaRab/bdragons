package com.yb.dragons.client.model;

import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.StormSpine;
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
 * The storm clouds of the Tempest Dragon (drawn translucent, with their own small texture): a small cloud held by the
 * talons of each hand when it moves in the air (they grip the clouds that carry the serpent). Still in the air, its
 * hands let them go: the wind of its tornado holds it up (particles, see ClientAnimators). Their places come from the pose
 * of the serpent ({@link StormSpine.Pose#grip}), solved before the frame.
 */
public class StormCloudModel extends EntityModel<StormRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(YBDragons.id("alpha_storm_clouds"), "main");
    /** Puffs of a hand cloud: size (pixels) and place around the talons (pixels). */
    private static final float[][] GRIP_PUFF = {
            {12, 8, 12, 0, 0, 0}, {8, 7, 8, 6, -1, 2.5F}, {8, 7, 8, -6, -0.5F, 2}, {7, 5, 7, 1.5F, 2, 7.5F}, {7, 5, 7, -1.5F, -3, -5}};
    private final ModelPart[] grips = new ModelPart[2];
    private final ModelPart[][] gripPuffs = new ModelPart[2][GRIP_PUFF.length];
    private final float[] grip = new float[3];

    public StormCloudModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        for (int i = 0; i < 2; i++) {
            grips[i] = root.getChild("grip" + i);
            for (int k = 0; k < GRIP_PUFF.length; k++) gripPuffs[i][k] = grips[i].getChild("puff" + k);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        for (int i = 0; i < 2; i++) {
            PartDefinition g = root.addOrReplaceChild("grip" + i, CubeListBuilder.create(), PartPose.ZERO);
            for (int k = 0; k < GRIP_PUFF.length; k++) {
                float[] p = GRIP_PUFF[k];
                g.addOrReplaceChild("puff" + k, CubeListBuilder.create().texOffs(0, 0).addBox(-p[0] / 2, -p[1] / 2, -p[2] / 2, p[0], p[1], p[2]),
                        PartPose.offset(p[3], p[4], p[5]));
            }
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    /** How much the talons hold their clouds: in the air, when it moves (still, it lets them go). */
    static float hold(StormRenderState s) {
        return s.air * (1 - s.coil);
    }

    /** True when there is a cloud to draw. */
    public static boolean visible(StormRenderState s) {
        return hold(s) > 0.03F;
    }

    @Override
    public void setupAnim(StormRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        // the clouds of the talons: in the air, when it moves
        float hold = hold(s);
        for (int i = 0; i < 2; i++) {
            ModelPart g = grips[i];
            g.visible = hold > 0.03F;
            if (!g.visible) continue;
            s.pose.grip(i, grip);
            g.x = grip[0];
            g.y = grip[1];
            g.z = grip[2];
            // flying forward, the clouds stream back behind the talons
            float stream = s.fly + s.descend;
            for (int k = 0; k < GRIP_PUFF.length; k++) {
                float[] p = GRIP_PUFF[k];
                ModelPart puff = gripPuffs[i][k];
                float a = age * 0.15F + k * 1.7F + i * 2.0F;
                puff.x = p[3] * (1 + 0.2F * Mth.sin(a));
                puff.y = p[4] + Mth.sin(a * 0.8F) * 0.8F;
                puff.z = p[5] + stream * (2.0F + k * 2.2F);
                float size = hold * (0.85F + 0.15F * Mth.sin(a * 0.6F));
                puff.xScale = puff.yScale = size;
                puff.zScale = size * (1 + 0.5F * stream);
                puff.yRot = a * 0.3F;
            }
        }
    }
}
