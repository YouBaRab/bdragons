package com.yb.dragons.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.AlphaForest;
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
import org.joml.Vector3f;

/**
 * The Sylvan Tortoise, alpha of the forest: a giant alligator snapping turtle (head, beak, shell and legs after the
 * alligator snapping turtle of Alex's Mobs, by Alexthe666, LGPL) with the proportions of Bdragon. Its shell is
 * covered with spikes and carries a little plains biome: two grassy hills and five trees of different heights.
 *
 * <p>The model is the Blockbench model {@code tools/models/tortue_sylve_v2.bbmodel}; its layer is written by
 * {@code tools/gen_tortoise_model.py} (between the markers of {@link #createLayer}). It is built at a quarter of its
 * size (1 model pixel = 1/4 block, see {@link #SCALE}) and drawn 4 times bigger: about 17 blocks long with the beak
 * and the tail, 12 wide, 12 high with its trees.</p>
 *
 * <p>When it stays still (in the air or on the ground) for {@link TortoiseAnimator#RETRACT_AFTER} ticks, it slowly
 * pulls its head, its legs and its tail into its shell (only a look: nothing changes for the game).</p>
 */
public class TortoiseModel extends EntityModel<TortoiseRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(YBDragons.id("alpha_tortoise"), "main");
    /** World blocks per model block (16 model pixels). */
    public static final float SCALE = 4.0F;

    // --- pivots (blocks, frame of the tortoise: x to its left, y up from the ground, z forward) ----------------
    /** Origin of the model root: 1.5 model blocks above the ground = 6 blocks. */
    static final float[] ROOT = {0, 6.0F, 0};
    /** Shoulders and hips: front left, front right, hind left, hind right. */
    public static final float[][] LEGS = {{3.75F, 3.53F, 3.25F}, {-3.75F, 3.53F, 3.25F}, {3.75F, 3.53F, -3.5F}, {-3.75F, 3.53F, -3.5F}};
    /** The feet are this much further out than the hips (blocks). */
    public static final float FOOT_OUT = 0.85F;
    /**
     * The crowns of the five trees of the shell (blocks): x, height of the bottom of the leaves, z, half width
     * (the falling leaves).
     */
    public static final float[][] CROWNS = {{-1.875F, 10.25F, -2.125F, 1.0F}, {2.25F, 8.5F, -2.5F, 1.0F}, {0.75F, 9.25F, 0.25F, 1.0F},
            {-2.0F, 8.25F, 2.5F, 0.75F}, {2.125F, 9.0F, 2.375F, 0.75F}};

    // --- the retreat into the shell (model pixels) -----------------------------------------------------------
    /** The neck slides this far back: only the hook of the beak stays out of the shell. */
    static final float RETRACT_NECK = 16.5F;
    /** On the ground, the withdrawn shell sinks this far (it rests on the stubs of its feet). */
    static final float RETRACT_SINK = 7.0F;
    /** The tail slides this far forward: its tip stays out. */
    static final float RETRACT_TAIL = 3.5F;
    /** The legs shrink to this size under the rim of the shell: only the stubs of the feet stay out. */
    static final float RETRACT_LEG = 0.5F;

    public final ModelPart body, neck, head, jaw, tail;
    public final ModelPart[] thighs = new ModelPart[4];
    public final ModelPart[] shins = new ModelPart[4];

    public TortoiseModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        jaw = head.getChild("jaw");
        tail = body.getChild("tail");
        for (int i = 0; i < 4; i++) {
            thighs[i] = body.getChild("thigh" + i);
            shins[i] = thighs[i].getChild("shin");
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------------------------------------------

    /** Model pixels of a length in blocks. */
    static float px(float blocks) {
        return blocks * 4.0F;
    }

    /** The pose of a child part whose pivot is {@code child}, under a part whose pivot is {@code parent} (blocks, frame of the tortoise). */
    static PartPose at(float[] child, float[] parent) {
        return PartPose.offset(px(child[0] - parent[0]), px(parent[1] - child[1]), px(parent[2] - child[2]));
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // <generated by tools/gen_tortoise_model.py>
        PartDefinition p1 = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 50).addBox(-18.0F, 7.0F, -19.0F, 36.0F, 4.0F, 39.0F).texOffs(395, 113).addBox(-10.0F, 11.0F, -2.0F, 8.0F, 2.0F, 10.0F).texOffs(432, 113).addBox(3.0F, 11.0F, -12.0F, 9.0F, 2.0F, 9.0F).texOffs(193, 129).addBox(-14.0F, 11.0F, -15.0F, 7.0F, 1.0F, 7.0F).texOffs(222, 129).addBox(2.0F, 11.0F, 9.0F, 7.0F, 1.0F, 7.0F).texOffs(407, 141).addBox(-6.0F, 11.0F, -18.0F, 12.0F, 1.0F, 3.0F).texOffs(0, 0).addBox(-20.0F, 1.0F, -21.0F, 40.0F, 6.0F, 43.0F).texOffs(250, 0).addBox(-18.0F, -4.0F, -19.0F, 17.0F, 5.0F, 39.0F).texOffs(363, 0).addBox(1.0F, -4.0F, -19.0F, 17.0F, 5.0F, 39.0F).texOffs(167, 0).addBox(-1.0F, -5.0F, -19.0F, 2.0F, 6.0F, 39.0F).texOffs(151, 50).addBox(-15.0F, -8.0F, -16.0F, 14.0F, 4.0F, 33.0F).texOffs(246, 50).addBox(1.0F, -8.0F, -16.0F, 14.0F, 4.0F, 33.0F).texOffs(341, 50).addBox(-14.0F, -10.0F, -15.0F, 28.0F, 2.0F, 31.0F), PartPose.offset(0.0F, 4.0F, 0.0F));
        PartDefinition p2 = p1.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(460, 50).addBox(-7.0F, -3.4F, -7.0F, 14.0F, 8.0F, 10.0F).texOffs(154, 129).addBox(-6.0F, -4.4F, -6.0F, 12.0F, 1.0F, 7.0F), PartPose.offset(0.0F, 2.4F, -20.0F));
        PartDefinition p3 = p2.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 94).addBox(-6.5F, -5.0F, -9.4F, 13.0F, 7.0F, 11.0F).texOffs(251, 129).addBox(-5.0F, -3.0F, -12.4F, 10.0F, 5.0F, 3.0F).texOffs(438, 129).addBox(-1.5F, 0.0F, -13.9F, 3.0F, 4.5F, 2.0F).texOffs(278, 129).addBox(2.0F, -6.5F, -7.4F, 4.5F, 1.5F, 6.0F).texOffs(300, 129).addBox(-6.5F, -6.5F, -7.4F, 4.5F, 1.5F, 6.0F).texOffs(252, 147).addBox(6.5F, -3.0F, -6.4F, 0.5F, 2.0F, 2.0F).texOffs(258, 147).addBox(-7.0F, -3.0F, -6.4F, 0.5F, 2.0F, 2.0F).texOffs(350, 129).addBox(-4.0F, -6.0F, -5.4F, 8.0F, 1.0F, 6.0F), PartPose.offset(0.0F, -0.4F, -5.6F));
        p3.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(213, 94).addBox(-6.0F, -1.0F, -12.0F, 12.0F, 3.5F, 12.0F).texOffs(438, 141).addBox(-4.0F, 0.0F, -13.5F, 8.0F, 2.5F, 1.5F).texOffs(264, 147).addBox(-2.5F, 2.5F, -3.0F, 1.0F, 3.5F, 0.0F).texOffs(404, 141).addBox(-0.5F, 2.5F, -4.0F, 1.0F, 4.5F, 0.0F).texOffs(421, 147).addBox(1.5F, 2.5F, -3.0F, 1.0F, 2.5F, 0.0F), PartPose.offset(0.0F, 3.0F, 0.6F));
        p1.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(322, 129).addBox(-2.5F, -1.0F, -1.0F, 5.0F, 4.0F, 4.0F).texOffs(483, 129).addBox(-2.0F, -0.5F, 3.0F, 4.0F, 3.0F, 3.0F).texOffs(0, 141).addBox(-1.0F, 0.5F, 6.0F, 2.0F, 1.5F, 3.0F).texOffs(238, 147).addBox(-0.5F, -3.0F, 0.0F, 1.0F, 2.0F, 2.0F).texOffs(245, 147).addBox(-0.5F, -2.0F, 3.0F, 1.0F, 1.5F, 2.0F).texOffs(267, 147).addBox(-0.5F, -0.5F, 6.0F, 1.0F, 1.0F, 2.0F), PartPose.offset(0.0F, 7.0F, 20.0F));
        PartDefinition p6 = p1.addOrReplaceChild("thigh0", CubeListBuilder.create().texOffs(49, 94).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 7.0F, 10.0F), PartPose.offsetAndRotation(15.0F, 5.88F, -13.0F, 0.0F, 0.0F, -0.507F));
        p6.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(0, 113).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 6.0F, 9.0F).texOffs(223, 113).addBox(-5.5F, 6.0F, -5.5F, 11.0F, 2.0F, 10.0F).texOffs(242, 141).addBox(3.5F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(251, 141).addBox(0.83F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(260, 141).addBox(-1.84F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(269, 141).addBox(-4.51F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(315, 94).addBox(-5.25F, -2.5F, -5.25F, 10.5F, 4.5F, 10.5F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.507F));
        PartDefinition p8 = p1.addOrReplaceChild("thigh1", CubeListBuilder.create().texOffs(90, 94).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 7.0F, 10.0F), PartPose.offsetAndRotation(-15.0F, 5.88F, -13.0F, 0.0F, 0.0F, 0.507F));
        p8.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(37, 113).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 6.0F, 9.0F).texOffs(266, 113).addBox(-5.5F, 6.0F, -5.5F, 11.0F, 2.0F, 10.0F).texOffs(278, 141).addBox(3.5F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(287, 141).addBox(0.83F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(296, 141).addBox(-1.84F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(305, 141).addBox(-4.51F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(358, 94).addBox(-5.25F, -2.5F, -5.25F, 10.5F, 4.5F, 10.5F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, 0.0F, -0.507F));
        PartDefinition p10 = p1.addOrReplaceChild("thigh2", CubeListBuilder.create().texOffs(131, 94).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 7.0F, 10.0F), PartPose.offsetAndRotation(15.0F, 5.88F, 14.0F, 0.0F, 0.0F, -0.507F));
        p10.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(74, 113).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 6.0F, 9.0F).texOffs(309, 113).addBox(-5.5F, 6.0F, -5.5F, 11.0F, 2.0F, 10.0F).texOffs(314, 141).addBox(3.5F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(323, 141).addBox(0.83F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(332, 141).addBox(-1.84F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(341, 141).addBox(-4.51F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(401, 94).addBox(-5.25F, -2.5F, -5.25F, 10.5F, 4.5F, 10.5F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, 0.0F, 0.507F));
        PartDefinition p12 = p1.addOrReplaceChild("thigh3", CubeListBuilder.create().texOffs(172, 94).addBox(-5.0F, 0.0F, -5.0F, 10.0F, 7.0F, 10.0F), PartPose.offsetAndRotation(-15.0F, 5.88F, 14.0F, 0.0F, 0.0F, 0.507F));
        p12.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(111, 113).addBox(-4.5F, 0.0F, -4.5F, 9.0F, 6.0F, 9.0F).texOffs(352, 113).addBox(-5.5F, 6.0F, -5.5F, 11.0F, 2.0F, 10.0F).texOffs(350, 141).addBox(3.5F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(359, 141).addBox(0.83F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(368, 141).addBox(-1.84F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(377, 141).addBox(-4.51F, 6.5F, -8.5F, 1.0F, 1.5F, 3.0F).texOffs(444, 94).addBox(-5.25F, -2.5F, -5.25F, 10.5F, 4.5F, 10.5F), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 0.0F, 0.0F, -0.507F));
        p1.addOrReplaceChild("spikes", CubeListBuilder.create().texOffs(458, 141).addBox(20.0F, 3.0F, -16.0F, 3.0F, 2.0F, 2.0F).texOffs(424, 147).addBox(23.0F, 3.5F, -15.5F, 2.0F, 1.0F, 1.0F).texOffs(0, 147).addBox(20.0F, 3.0F, 16.0F, 3.0F, 2.0F, 2.0F).texOffs(452, 147).addBox(23.0F, 3.5F, 16.5F, 2.0F, 1.0F, 1.0F).texOffs(11, 147).addBox(-23.0F, 3.0F, -16.0F, 3.0F, 2.0F, 2.0F).texOffs(459, 147).addBox(-25.0F, 3.5F, -15.5F, 2.0F, 1.0F, 1.0F).texOffs(22, 147).addBox(-23.0F, 3.0F, -8.0F, 3.0F, 2.0F, 2.0F).texOffs(466, 147).addBox(-25.0F, 3.5F, -7.5F, 2.0F, 1.0F, 1.0F).texOffs(44, 147).addBox(-23.0F, 3.0F, 8.0F, 3.0F, 2.0F, 2.0F).texOffs(480, 147).addBox(-25.0F, 3.5F, 8.5F, 2.0F, 1.0F, 1.0F).texOffs(11, 141).addBox(14.0F, 3.0F, 22.0F, 2.0F, 2.0F, 3.0F).texOffs(274, 147).addBox(14.5F, 3.5F, 25.0F, 1.0F, 1.0F, 2.0F).texOffs(33, 141).addBox(-6.0F, 3.0F, 22.0F, 2.0F, 2.0F, 3.0F).texOffs(288, 147).addBox(-5.5F, 3.5F, 25.0F, 1.0F, 1.0F, 2.0F).texOffs(88, 147).addBox(18.0F, -2.5F, 4.0F, 3.0F, 2.0F, 2.0F).texOffs(0, 152).addBox(21.0F, -2.0F, 4.5F, 2.0F, 1.0F, 1.0F).texOffs(99, 147).addBox(18.0F, -2.5F, 12.0F, 3.0F, 2.0F, 2.0F).texOffs(7, 152).addBox(21.0F, -2.0F, 12.5F, 2.0F, 1.0F, 1.0F).texOffs(143, 147).addBox(-21.0F, -2.5F, 12.0F, 3.0F, 2.0F, 2.0F).texOffs(35, 152).addBox(-23.0F, -2.0F, -4.5F, 2.0F, 1.0F, 1.0F).texOffs(143, 147).addBox(-21.0F, -2.5F, -5.0F, 3.0F, 2.0F, 2.0F).texOffs(35, 152).addBox(-23.0F, -2.0F, 12.5F, 2.0F, 1.0F, 1.0F).texOffs(110, 141).addBox(3.0F, -2.5F, 20.0F, 2.0F, 2.0F, 3.0F).texOffs(337, 147).addBox(3.5F, -2.0F, 23.0F, 1.0F, 1.0F, 2.0F).texOffs(121, 141).addBox(-5.0F, -2.5F, 20.0F, 2.0F, 2.0F, 3.0F).texOffs(344, 147).addBox(-4.5F, -2.0F, 23.0F, 1.0F, 1.0F, 2.0F).texOffs(132, 141).addBox(-13.0F, -2.5F, 20.0F, 2.0F, 2.0F, 3.0F).texOffs(351, 147).addBox(-12.5F, -2.0F, 23.0F, 1.0F, 1.0F, 2.0F).texOffs(154, 147).addBox(15.0F, -7.0F, -9.0F, 3.0F, 2.0F, 2.0F).texOffs(42, 152).addBox(18.0F, -6.5F, -8.5F, 2.0F, 1.0F, 1.0F).texOffs(187, 147).addBox(-18.0F, -7.0F, -9.0F, 3.0F, 2.0F, 2.0F).texOffs(63, 152).addBox(-20.0F, -6.5F, -8.5F, 2.0F, 1.0F, 1.0F).texOffs(198, 147).addBox(-18.0F, -7.0F, 0.0F, 3.0F, 2.0F, 2.0F).texOffs(70, 152).addBox(-20.0F, -6.5F, 0.5F, 2.0F, 1.0F, 1.0F).texOffs(187, 141).addBox(7.0F, -7.0F, 17.0F, 2.0F, 2.0F, 3.0F).texOffs(386, 147).addBox(7.5F, -6.5F, 20.0F, 1.0F, 1.0F, 2.0F).texOffs(209, 141).addBox(-9.0F, -7.0F, 17.0F, 2.0F, 2.0F, 3.0F).texOffs(400, 147).addBox(-8.5F, -6.5F, 20.0F, 1.0F, 1.0F, 2.0F), PartPose.ZERO);
        p1.addOrReplaceChild("garden", CubeListBuilder.create().texOffs(469, 113).addBox(4.0F, -2.0F, -14.0F, 9.0F, 2.0F, 9.0F).texOffs(379, 129).addBox(6.0F, -4.0F, -12.0F, 5.0F, 2.0F, 5.0F).texOffs(262, 94).addBox(-14.0F, -2.0F, 2.0F, 13.0F, 2.0F, 13.0F).texOffs(0, 129).addBox(-12.0F, -4.0F, 4.0F, 9.0F, 2.0F, 9.0F).texOffs(400, 129).addBox(-10.0F, -6.0F, 6.0F, 5.0F, 2.0F, 5.0F).texOffs(341, 129).addBox(-8.5F, -12.0F, 7.5F, 2.0F, 6.0F, 2.0F).texOffs(157, 113).addBox(-11.5F, -17.0F, 4.5F, 8.0F, 6.0F, 8.0F).texOffs(449, 129).addBox(-9.5F, -19.0F, 6.5F, 4.0F, 2.0F, 4.0F).texOffs(148, 113).addBox(8.0F, -13.0F, 9.0F, 2.0F, 13.0F, 2.0F).texOffs(37, 129).addBox(5.0F, -7.0F, 6.0F, 8.0F, 3.0F, 8.0F).texOffs(129, 129).addBox(6.0F, -10.0F, 7.0F, 6.0F, 3.0F, 6.0F).texOffs(421, 129).addBox(7.0F, -13.0F, 8.0F, 4.0F, 3.0F, 4.0F).texOffs(386, 141).addBox(8.0F, -16.0F, 9.0F, 2.0F, 3.0F, 2.0F).texOffs(120, 129).addBox(2.0F, -8.0F, -2.0F, 2.0F, 8.0F, 2.0F).texOffs(190, 113).addBox(-1.0F, -13.0F, -5.0F, 8.0F, 6.0F, 8.0F).texOffs(466, 129).addBox(1.0F, -15.0F, -3.0F, 4.0F, 2.0F, 4.0F).texOffs(498, 129).addBox(-9.0F, -4.0F, -11.0F, 2.0F, 4.0F, 2.0F).texOffs(70, 129).addBox(-11.0F, -8.0F, -13.0F, 6.0F, 5.0F, 6.0F).texOffs(220, 147).addBox(-9.0F, -10.0F, -11.0F, 2.0F, 2.0F, 2.0F).texOffs(395, 141).addBox(7.5F, -7.0F, -10.5F, 2.0F, 3.0F, 2.0F).texOffs(95, 129).addBox(5.5F, -11.0F, -12.5F, 6.0F, 5.0F, 6.0F).texOffs(229, 147).addBox(7.5F, -13.0F, -10.5F, 2.0F, 2.0F, 2.0F), PartPose.offset(0.0F, -10.0F, 0.0F));
        return LayerDefinition.create(mesh, 512, 256);
        // </generated>
    }

    // ------------------------------------------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------------------------------------------

    @Override
    public void setupAnim(TortoiseRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float air = 1 - s.ground;
        // in its shell: k = 1 (smooth), out = what is left of the other movements
        float k = s.retract * s.retract * (3 - 2 * s.retract);
        float out = 1 - k;

        // body: bobbing in the air, pitch in the flight
        body.y += -px(s.bob);
        // on the ground, the shell withdrawn sinks onto its belly (the stubs of the feet touch the ground)
        body.y += RETRACT_SINK * k * s.ground;
        body.xRot = s.bodyPitch * Mth.DEG_TO_RAD;

        // head and neck follow the look (the neck takes a part of the turn)
        float yaw = Mth.clamp(s.yRot, -AlphaForest.HEAD_YAW_MAX, AlphaForest.HEAD_YAW_MAX) * Mth.DEG_TO_RAD * out;
        float pitch = Mth.clamp(s.xRot, AlphaForest.HEAD_PITCH_MIN, AlphaForest.HEAD_PITCH_MAX) * Mth.DEG_TO_RAD * out;
        float share = AlphaForest.NECK_SHARE;
        // like the vanilla heads: positive yaw = to the right, positive pitch = looking down
        neck.yRot = yaw * share;
        neck.xRot = pitch * share;
        head.yRot = yaw * (1 - share);
        head.xRot = pitch * (1 - share);
        // the head does not follow the pitch of the body (fully while it fires: the beam goes where the player looks)
        head.xRot -= s.bodyPitch * Mth.DEG_TO_RAD * (s.beam > 0 ? 1 : 0.6F) * out;
        jaw.xRot = (s.jaw * 0.55F + Mth.sin(age * 0.08F) * 0.02F) * out;
        // into the shell: the neck slides back, the head lowers a little
        neck.z += RETRACT_NECK * k;
        neck.y += 0.5F * k;
        head.xRot += 0.08F * k;

        // tail: slow sway, lifted in the flight; pulled under the shell
        tail.yRot = (Mth.sin(age * 0.06F) * 0.15F + Mth.sin(s.walkAnimationPos * 0.4F) * 0.1F * s.ground) * out;
        tail.xRot = (-0.25F * s.propel + 0.15F * s.descend) * out;
        tail.z -= RETRACT_TAIL * k;
        tail.xScale = tail.yScale = tail.zScale = 1 - 0.3F * k;

        // legs ------------------------------------------------------------------------------------------------
        float walkPos = s.walkAnimationPos * 0.55F;
        float walkAmp = Math.min(1, s.walkAnimationSpeed * 1.6F) * s.ground;
        float legScale = Mth.lerp(k, 1, RETRACT_LEG);
        for (int i = 0; i < 4; i++) {
            boolean front = i < 2, left = i % 2 == 0;
            // diagonal gait: front left with hind right
            float phase = walkPos + (i == 0 || i == 3 ? 0 : Mth.PI);
            float swing = Mth.sin(phase) * 0.38F * walkAmp;
            float lift = Math.max(0, Mth.cos(phase)) * 0.45F * walkAmp;

            // in the air: the legs hang and paddle slowly
            float paddle = Mth.sin(s.paddle + i * 1.7F) * 0.22F;
            // flight forward: front legs forward and up, hind legs backward and up (a jumping horse)
            float stretch = front ? -1.75F : 1.75F;
            float flutter = Mth.sin(s.paddle * 2 + i) * 0.06F;
            // descent: legs spread and ready to land
            float brace = front ? -0.35F : 0.3F;

            float x = swing * s.ground + paddle * s.hover + (stretch + flutter) * s.propel + (brace + paddle * 0.4F) * s.descend;
            float spread = (0.12F * s.hover + 0.2F * s.descend) * (left ? -1 : 1);
            ModelPart thigh = thighs[i];
            thigh.xRot = x * out;
            thigh.zRot += spread * out;
            // knee: bends to lift the foot when walking, folds a little in the propulsion
            shins[i].xRot = ((front ? -lift : lift) + (front ? 0.25F : -0.25F) * s.propel + 0.15F * air * (front ? -1 : 1) * s.hover) * out;
            // into the shell: the legs shrink up under the rim, a little towards the centre
            thigh.xScale = thigh.yScale = thigh.zScale = legScale;
            thigh.y -= 1.0F * k;
            thigh.x -= Math.signum(thigh.x) * 1.5F * k;
            thigh.z -= Math.signum(thigh.z) * 1.0F * k;
        }
    }

    /** Moves the pose from the root to the frame of the body. */
    public void toBody(PoseStack pose) {
        root.translateAndRotate(pose);
        body.translateAndRotate(pose);
    }

    /** Position of the mouth (start of the Beam of Life) after {@link #setupAnim}, in the frame of {@code pose}. */
    public Vector3f mouth(PoseStack pose) {
        pose.pushPose();
        toBody(pose);
        neck.translateAndRotate(pose);
        head.translateAndRotate(pose);
        Vector3f p = new Vector3f(0, px(-AlphaForest.MOUTH_DY) / 16F, px(-AlphaForest.MOUTH_DZ) / 16F);
        pose.last().pose().transformPosition(p);
        pose.popPose();
        return p;
    }
}
