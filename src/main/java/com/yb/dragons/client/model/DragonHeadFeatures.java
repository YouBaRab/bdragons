package com.yb.dragons.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import static com.yb.dragons.client.model.DragonModel.*;

/**
 * The horns of a dragon head and the head features of each race (ice crown, fins, branches, devil
 * horns, lightning bolts, halo...). Shared by the dragon model and, scaled down on the human head,
 * by the hybrid form, so both always look alike.
 *
 * <p>The geometry is written in the space of the Dragon Mounts head: the skull is the box
 * x -8..8, y -8..8, z -10..6 (the snout goes on to z -24).</p>
 */
public final class DragonHeadFeatures {
    public final ModelPart[] horns;       // 0,1 = original horns, 2,3 = elder horns
    public final ModelPart[] crown;       // legendary crown (3) + nose horn
    public final ModelPart iceCrown;
    public final ModelPart[] finEars = new ModelPart[2];
    public final ModelPart[] moss = new ModelPart[4];
    public final ModelPart[] branches = new ModelPart[2];
    public final ModelPart[] bigBranches = new ModelPart[2];
    public final ModelPart[] devilHorns = new ModelPart[2];
    public final ModelPart[] bolts = new ModelPart[2];
    public final ModelPart[] bigBolts = new ModelPart[2];
    public final ModelPart boltCrest;
    public final ModelPart halo;
    /** Wither: the crown of small skulls (0 = the big one in the middle). */
    public final ModelPart[] skulls = new ModelPart[5];
    /** Moonlight: the moonstone on the forehead and the crescent moon on the head. */
    public final ModelPart moonstone, crescent;

    public DragonHeadFeatures(ModelPart head) {
        horns = new ModelPart[] {head.getChild("horn1"), head.getChild("horn2"), head.getChild("horn3"), head.getChild("horn4")};
        crown = new ModelPart[] {head.getChild("crown_left"), head.getChild("crown_right"), head.getChild("crown_middle"), head.getChild("nose_horn")};
        iceCrown = head.getChild("ice_crown");
        finEars[0] = head.getChild("fin_ear_right");
        finEars[1] = head.getChild("fin_ear_left");
        moss[0] = horns[2].getChild("moss");
        moss[1] = horns[3].getChild("moss");
        moss[2] = horns[2].getChild("tip").getChild("moss");
        moss[3] = horns[3].getChild("tip").getChild("moss");
        branches[0] = head.getChild("branch_right");
        branches[1] = head.getChild("branch_left");
        bigBranches[0] = head.getChild("big_branch_right");
        bigBranches[1] = head.getChild("big_branch_left");
        devilHorns[0] = head.getChild("devil_right");
        devilHorns[1] = head.getChild("devil_left");
        bolts[0] = head.getChild("bolt_right");
        bolts[1] = head.getChild("bolt_left");
        bigBolts[0] = head.getChild("big_bolt_right");
        bigBolts[1] = head.getChild("big_bolt_left");
        boltCrest = head.getChild("bolt_crest");
        halo = head.getChild("halo");
        for (int i = 0; i < skulls.length; i++) skulls[i] = head.getChild("skull_" + i);
        moonstone = head.getChild("moonstone");
        crescent = head.getChild("crescent");
    }

    /** Adds every horn and race feature to the given head part. */
    public static void build(PartDefinition head) {
        int thick = 3;
        float ofs = -(thick / 2f);
        // original horns
        CubeListBuilder horn = CubeListBuilder.create().texOffs(28, 32).addBox(ofs, ofs, ofs, thick, thick, 12);
        CubeListBuilder hornM = CubeListBuilder.create().texOffs(28, 32).mirror().addBox(ofs, ofs, ofs, thick, thick, 12);
        head.addOrReplaceChild("horn1", horn, PartPose.offsetAndRotation(-5, -8, 0, 0.523599f, -0.523599f, 0));
        head.addOrReplaceChild("horn2", hornM, PartPose.offsetAndRotation(5, -8, 0, 0.523599f, 0.523599f, 0));
        // elder horns: a second pair of curved horns growing sideways from the skull, tips turned upwards
        CubeListBuilder horn2 = CubeListBuilder.create().texOffs(28, 32).addBox(-2, -2, 0, 4, 4, 11);
        CubeListBuilder horn2M = CubeListBuilder.create().texOffs(28, 32).mirror().addBox(-2, -2, 0, 4, 4, 11);
        CubeListBuilder tip = CubeListBuilder.create().texOffs(28, 32).addBox(-1.5f, -1.5f, 0, 3, 3, 9);
        CubeListBuilder tipM = CubeListBuilder.create().texOffs(28, 32).mirror().addBox(-1.5f, -1.5f, 0, 3, 3, 9);
        PartDefinition h3 = head.addOrReplaceChild("horn3", horn2, PartPose.offsetAndRotation(-7, -5, 2, 0.35f, -1.05f, 0));
        PartDefinition h4 = head.addOrReplaceChild("horn4", horn2M, PartPose.offsetAndRotation(7, -5, 2, 0.35f, 1.05f, 0));
        PartDefinition t3 = h3.addOrReplaceChild("tip", tip, PartPose.offsetAndRotation(0, 0, 10, 0.95f, 0.35f, 0));
        PartDefinition t4 = h4.addOrReplaceChild("tip", tipM, PartPose.offsetAndRotation(0, 0, 10, 0.95f, -0.35f, 0));
        // sea moss on the big horns of the water dragon: tufts on top and strands floating below
        CubeListBuilder hornMoss = uv(UV_MOSS).addBox(-2.6F, -3.2F, 1, 5.2F, 2, 4).addBox(-2.4F, -3.0F, 6.5F, 4.8F, 2, 3)
                .addBox(-0.5F, 2, 2, 1, 5, 1).addBox(-1.5F, 2, 7, 1, 4, 1).addBox(1, 2, 4.5F, 1, 6, 1);
        h3.addOrReplaceChild("moss", hornMoss, PartPose.ZERO);
        h4.addOrReplaceChild("moss", hornMoss, PartPose.ZERO);
        CubeListBuilder tipMoss = uv(UV_MOSS).addBox(-2, -2.6F, 0.5F, 4, 2, 3.5F).addBox(-0.5F, 1.5F, 1, 1, 4, 1);
        t3.addOrReplaceChild("moss", tipMoss, PartPose.ZERO);
        t4.addOrReplaceChild("moss", tipMoss, PartPose.ZERO);
        // legendary crown: a fan of three big spikes on top of the skull and a horn on the nose
        CubeListBuilder spike = CubeListBuilder.create().texOffs(28, 32).addBox(-1.5f, -1.5f, 0, 3, 3, 13);
        head.addOrReplaceChild("crown_left", spike, PartPose.offsetAndRotation(-4, -8, -4, 1.15f, -0.45f, 0));
        head.addOrReplaceChild("crown_right", spike, PartPose.offsetAndRotation(4, -8, -4, 1.15f, 0.45f, 0));
        head.addOrReplaceChild("crown_middle", CubeListBuilder.create().texOffs(28, 32).addBox(-1.5f, -1.5f, 0, 3, 3, 16),
                PartPose.offsetAndRotation(0, -8, -6, 1.0f, 0, 0));
        head.addOrReplaceChild("nose_horn", CubeListBuilder.create().texOffs(28, 32).addBox(-1.5f, -1.5f, 0, 3, 3, 9),
                PartPose.offsetAndRotation(0, -1, -20, 2.4f, 0, 0));
        buildRaceFeatures(head);
    }

    private static void buildRaceFeatures(PartDefinition head) {
        // --- ice: a crown of ice crystals on the skull, joined to the four horns ---
        PartDefinition crown = head.addOrReplaceChild("ice_crown", uv(UV_ICE)
                .addBox(-7, -9.5F, -9, 14, 1.5F, 1.5F)
                .addBox(-7, -9.5F, 3.5F, 14, 1.5F, 1.5F)
                .addBox(-7, -9.5F, -9, 1.5F, 1.5F, 14)
                .addBox(5.5F, -9.5F, -9, 1.5F, 1.5F, 14), PartPose.ZERO);
        // crystals: x, z, height, tilt sideways, tilt forwards
        float[][] crystals = {{0, -8.2F, 10, 0, -0.25F}, {-3.8F, -8.2F, 7, 0.12F, -0.2F}, {3.8F, -8.2F, 7, -0.12F, -0.2F},
                {-6.2F, -4.5F, 6, 0.35F, 0}, {6.2F, -4.5F, 6, -0.35F, 0}, {-6.2F, 0.5F, 7.5F, 0.3F, 0.15F}, {6.2F, 0.5F, 7.5F, -0.3F, 0.15F},
                {-3, 4.2F, 4.5F, 0.15F, 0.3F}, {3, 4.2F, 4.5F, -0.15F, 0.3F}};
        for (int i = 0; i < crystals.length; i++) {
            float[] c = crystals[i];
            crown.addOrReplaceChild("crystal_" + i, uv(UV_ICE).addBox(-1, -c[2], -1, 2, c[2], 2).addBox(-0.5F, -c[2] - 1.5F, -0.5F, 1, 1.5F, 1),
                    PartPose.offsetAndRotation(c[0], -9, c[1], c[4], 0.785F, c[3]));
        }
        // bridges of ice from the crown to the main horns and the elder horns
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            beam(crown, "bridge_horn_" + s, UV_ICE, side * 6.2F, -15, 0.5F, side * 9.2F, -12.5F, 6.5F, 1.2F);
            beam(crown, "bridge_ram_" + s, UV_ICE, side * 6.5F, -9.5F, 2.5F, side * 12, -7.5F, 5.5F, 1.4F);
        }

        // --- water: fins as ears ---
        head.addOrReplaceChild("fin_ear_right", uv(UV_EAR_FIN).addBox(0, -5, 0, 0, 10, 12), PartPose.offsetAndRotation(-8.1F, 1, -6, 0.1F, -0.8F, 0.25F));
        head.addOrReplaceChild("fin_ear_left", uv(UV_EAR_FIN).mirror().addBox(0, -5, 0, 0, 10, 12), PartPose.offsetAndRotation(8.1F, 1, -6, 0.1F, 0.8F, -0.25F));

        // --- forest: branches instead of horns, thin ones for the small horns and big ones for the elder horns ---
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            PartDefinition small = head.addOrReplaceChild("branch_" + s, CubeListBuilder.create(), PartPose.offset(side * 5, -8, 0));
            beam(small, "wood", UV_BARK, 0, 0, 0, side * 4.5F, -6.5F, 9, 2);
            beam(small, "twig", UV_BARK, side * 2.2F, -3.2F, 4.4F, side * 6, -7, 3, 1.1F);
            beam(small, "twig2", UV_BARK, side * 3.5F, -5, 7, side * 3, -10, 9, 1);
            leaf(small, "leaf", side * 6, -7, 3, 0.4F, 0.6F);
            leaf(small, "leaf2", side * 4.5F, -7, 9.5F, -0.3F, -0.2F);
            leaf(small, "leaf3", side * 3, -10.2F, 9, 0.2F, 0.9F);

            PartDefinition big = head.addOrReplaceChild("big_branch_" + s, CubeListBuilder.create(), PartPose.offset(side * 7, -5, 2));
            beam(big, "wood", UV_BARK, 0, 0, 0, side * 9, -3, 5.5F, 3);
            beam(big, "wood2", UV_BARK, side * 9, -3, 5.5F, side * 11.5F, -11, 8, 2.2F);
            beam(big, "twig", UV_BARK, side * 5, -1.5F, 3, side * 8, -6, -1, 1.4F);
            beam(big, "twig2", UV_BARK, side * 10.5F, -8, 7.2F, side * 15, -10, 4, 1.2F);
            beam(big, "twig3", UV_BARK, side * 11, -10, 7.8F, side * 10, -15, 11, 1.1F);
            leaf(big, "leaf", side * 8, -6.2F, -1, 0.3F, 0.4F);
            leaf(big, "leaf2", side * 15, -10, 4, -0.4F, 0.8F);
            leaf(big, "leaf3", side * 10, -15.2F, 11, 0.25F, -0.3F);
            leaf(big, "leaf4", side * 11.5F, -11.2F, 8, -0.2F, 1.3F);
        }

        // --- nether: two devil horns curving up and forwards ---
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            PartDefinition devil = head.addOrReplaceChild("devil_" + s, CubeListBuilder.create(), PartPose.offset(side * 4.5F, -8, -3));
            beam(devil, "base", UV_DARK, 0, 1, 0, side * 2.5F, -6, 2, 3.2F);
            beam(devil, "mid", UV_DARK, side * 2.5F, -5.5F, 1.8F, side * 2.8F, -10.5F, 0, 2.3F);
            beam(devil, "tip", UV_DARK, side * 2.8F, -10, 0.2F, side * 1.8F, -13.5F, -3, 1.4F);
        }

        // --- storm: lightning bolts instead of horns ---
        for (int side = -1; side <= 1; side += 2) {
            String s = side < 0 ? "right" : "left";
            PartDefinition bolt = head.addOrReplaceChild("bolt_" + s, CubeListBuilder.create(), PartPose.offset(side * 5, -8, 0));
            zigzag(bolt, side, new float[][] {{0, 0, 0}, {side * 1.0F, -4.5F, 4.5F}, {side * 4.5F, -3.5F, 6}, {side * 5.5F, -8.5F, 10}, {side * 7.5F, -8, 11}, {side * 8, -12, 13}}, 2.2F);
            PartDefinition big = head.addOrReplaceChild("big_bolt_" + s, CubeListBuilder.create(), PartPose.offset(side * 7, -5, 2));
            zigzag(big, side, new float[][] {{0, 0, 0}, {side * 5, -3, 2}, {side * 6, 1, 4}, {side * 11, -2.5F, 5.5F}, {side * 11.5F, 1.5F, 7}, {side * 16, -2, 8}}, 3);
        }
        PartDefinition crest = head.addOrReplaceChild("bolt_crest", CubeListBuilder.create(), PartPose.offset(0, -8, -4));
        zigzag(crest, 1, new float[][] {{0, 0, 0}, {0, -5, -2.5F}, {0, -5, 1.5F}, {0, -10, -1}, {0, -10, 3}, {0, -15, 1}}, 2.4F);

        // --- sun: a halo of light above the head ---
        PartDefinition halo = head.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offsetAndRotation(0, -19, -1, 0.2F, 0, 0));
        float radius = 8.5F;
        float seg = Mth.TWO_PI * radius / HALO_SEGMENTS + 0.5F;
        for (int i = 0; i < HALO_SEGMENTS; i++) {
            float a = Mth.TWO_PI * i / HALO_SEGMENTS;
            halo.addOrReplaceChild("ring_" + i, uv(UV_LIGHT).addBox(-0.9F, -0.9F, -seg / 2, 1.8F, 1.8F, seg),
                    PartPose.offsetAndRotation(Mth.cos(a) * radius, 0, Mth.sin(a) * radius, 0, -a, 0));
        }

        // --- wither: a crown of small black skulls on top of the head, the big one in the middle ---
        head.addOrReplaceChild("skull_0", uv(UV_SKULL_BIG).addBox(-4, -8, -4, 8, 8, 8), PartPose.offsetAndRotation(0, -7.5F, -6, -0.1F, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            head.addOrReplaceChild("skull_" + (side < 0 ? 1 : 2), uv(UV_SKULL_SMALL).addBox(-3, -6, -3, 6, 6, 6),
                    PartPose.offsetAndRotation(side * 5.5F, -7.5F, -5, -0.1F, side * 0.25F, side * 0.3F));
            head.addOrReplaceChild("skull_" + (side < 0 ? 3 : 4), uv(UV_SKULL_SMALL).addBox(-3, -6, -3, 6, 6, 6),
                    PartPose.offsetAndRotation(side * 9.5F, -5.5F, -4, -0.05F, side * 0.5F, side * 0.75F));
        }

        // --- moonlight: a moonstone on the forehead and a silver crescent moon standing on the head, horns up ---
        head.addOrReplaceChild("moonstone", uv(UV_SILVER).addBox(-1.5F, -1.5F, -0.6F, 3, 3, 1.2F),
                PartPose.offsetAndRotation(0, -4.5F, -10.2F, 0, 0, Mth.PI / 4));
        PartDefinition moon = head.addOrReplaceChild("crescent", CubeListBuilder.create(), PartPose.offset(0, -15, -5));
        beam(moon, "stem", UV_SILVER, 0, 7.5F, 0, 0, 4.5F, 0, 1.6F);
        int steps = 10;
        float moonRadius = 6;
        for (int i = 0; i < steps; i++) {
            // from one horn of the crescent (upper left) down to the bottom and up to the other horn
            float a0 = (-115 + 230F * i / steps) * Mth.DEG_TO_RAD;
            float a1 = (-115 + 230F * (i + 1) / steps) * Mth.DEG_TO_RAD;
            float mid = (a0 + a1) / 2;
            float thick = Math.max(0.7F, 2.6F * Mth.cos(mid * 0.85F));
            beam(moon, "arc_" + i, UV_SILVER, Mth.sin(a0) * moonRadius, Mth.cos(a0) * moonRadius, 0,
                    Mth.sin(a1) * moonRadius, Mth.cos(a1) * moonRadius, 0, thick);
        }
    }

    /** A leaf (flat square) at the end of a branch. */
    private static void leaf(PartDefinition parent, String name, float x, float y, float z, float xRot, float yRot) {
        parent.addOrReplaceChild(name, uv(UV_LEAF).addBox(-4, 0, -4, 8, 0, 8), PartPose.offsetAndRotation(x, y, z, xRot, yRot, 0.3F));
    }

    /** A broken line of bars through the given points (a lightning bolt), thinner towards its end. */
    private static void zigzag(PartDefinition parent, int side, float[][] points, float thickness) {
        for (int i = 0; i + 1 < points.length; i++) {
            float t = thickness * (1 - 0.12F * i);
            float[] a = points[i], b = points[i + 1];
            beam(parent, "seg_" + i, UV_ELECTRIC, a[0], a[1], a[2], b[0], b[1], b[2], t);
        }
    }


    private static void scale(ModelPart p, float s) {
        p.xScale = p.yScale = p.zScale = s;
    }

    /**
     * Shows the horns and features of the race at this stage.
     * @param hybrid on the human head of the hybrid form (no horn on the nose there)
     */
    public void apply(DragonModel.Shape shape, int stage, float ageInTicks, boolean hybrid) {
        float hornSize = switch (stage) {
            case 1 -> 0.35F;
            case 2 -> 0.7F;
            default -> 1.0F;
        };
        // stage size of the race horns (branches, devil horns, bolts...)
        float grow = 0.45F + 0.1375F * (stage - 1);
        boolean classic = shape.horns() == Horns.CLASSIC || shape.horns() == Horns.ICE_CROWN;
        for (int i = 0; i < 2; i++) {
            horns[i].visible = classic;
            horns[i].xScale = horns[i].yScale = Math.max(0.6F, hornSize);
            horns[i].zScale = hornSize;
        }
        boolean mossy = shape.horns() == Horns.MOSSY;
        horns[2].visible = horns[3].visible = (classic && stage >= 4) || mossy;
        if (mossy) {
            float s = 0.55F + 0.1125F * (stage - 1);
            scale(horns[2], s);
            scale(horns[3], s);
        }
        for (ModelPart m : moss) m.visible = mossy && stage >= 2;
        for (int i = 0; i < 3; i++) crown[i].visible = shape.horns() == Horns.CLASSIC && stage >= 5 && shape.crest() == Crest.NONE;
        ModelPart nose = crown[3];
        nose.visible = !hybrid && ((classic && stage >= 5) || shape.horns() == Horns.NOSE_ONLY);
        if (shape.horns() == Horns.NOSE_ONLY) scale(nose, 0.55F + 0.15F * (stage - 1));

        iceCrown.visible = shape.horns() == Horns.ICE_CROWN && stage >= 5;
        for (ModelPart f : finEars) {
            f.visible = shape.finEars();
            scale(f, 0.8F + 0.12F * (stage - 1));
        }
        for (ModelPart b : branches) {
            b.visible = shape.horns() == Horns.BRANCHES;
            scale(b, grow);
        }
        for (ModelPart b : bigBranches) {
            b.visible = shape.horns() == Horns.BRANCHES && stage >= 3;
            scale(b, stage == 3 ? 0.65F : stage == 4 ? 0.85F : 1.0F);
        }
        for (ModelPart d : devilHorns) {
            d.visible = shape.horns() == Horns.DEVIL;
            scale(d, grow);
        }
        for (ModelPart b : bolts) {
            b.visible = shape.horns() == Horns.LIGHTNING;
            scale(b, grow);
        }
        for (ModelPart b : bigBolts) {
            b.visible = shape.horns() == Horns.LIGHTNING && stage >= 4;
            scale(b, stage >= 5 ? 1.0F : 0.85F);
        }
        boltCrest.visible = shape.horns() == Horns.LIGHTNING && stage >= 5;
        halo.visible = shape.halo() && stage >= 3;
        scale(halo, stage == 3 ? 0.7F : stage == 4 ? 0.85F : 1.0F);
        halo.yRot = ageInTicks * 0.02F;

        // wither: one skull at stage III, three at stage IV, five at stage V
        int skullCount = shape.crest() == Crest.SKULLS ? new int[] {0, 0, 0, 1, 3, 5}[stage] : 0;
        for (int i = 0; i < skulls.length; i++) {
            skulls[i].visible = i < skullCount;
            scale(skulls[i], stage >= 5 ? 1.0F : 0.85F);
        }
        // moonlight: the moonstone from stage II, the crescent from stage III, growing until stage V
        moonstone.visible = shape.crest() == Crest.MOON && stage >= 2;
        crescent.visible = shape.crest() == Crest.MOON && stage >= 3;
        scale(crescent, stage == 3 ? 0.65F : stage == 4 ? 0.82F : 1.0F);

        if (hybrid) {
            // on a human head the big pieces are kept smaller, so the face stays visible
            for (int i = 0; i < 3; i++) scale(crown[i], 0.6F);
            if (!mossy) {
                scale(horns[2], 0.8F);
                scale(horns[3], 0.8F);
            }
            scale(iceCrown, 0.8F);
            for (ModelPart d : devilHorns) d.xScale = d.yScale = d.zScale = d.xScale * 0.85F;
            for (ModelPart b : bigBolts) b.xScale = b.yScale = b.zScale = b.xScale * 0.8F;
        }
    }
}
