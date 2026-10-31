package com.yb.dragons.client.model;

import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.client.model.Model;
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
 * The dragon parts of the hybrid form (half human, half dragon, like a Zoan of One Piece):
 * horns on the head, wings and a tail on the back. Two models: one drawn with the head of the
 * player model, one with its body. They use the UV layout of the dragon model, so every race keeps
 * its own colours, scales and glowing runes.
 *
 * <p>Each stage adds its visible feature, like the full dragon: horns that grow (I-III), a second
 * pair of curved horns and back spikes (IV), a crown (V); the wings get bigger at every stage. The
 * horns, crowns and tails follow the anatomy of the race (see {@link DragonModel.Shape}): ice crown,
 * fins, branches, devil horns, lightning, halo, skulls, crescent moon, flame, trident tails, club, sword of light,
 * lianas, bones... The sun hybrid has blades of light instead of wing membranes.</p>
 *
 * <p>The alpha hybrid wears the alpha scale of its race on the forehead: a small shield of voxels in the
 * colours of the race, with a golden rim and a ridge in the middle (see {@link #ALPHA_MARK}).</p>
 */
public final class HybridModel {
    public static final ModelLayerLocation HEAD_LAYER = new ModelLayerLocation(YBDragons.id("hybrid_head"), "main");
    public static final ModelLayerLocation BODY_LAYER = new ModelLayerLocation(YBDragons.id("hybrid_body"), "main");
    public static final int TAIL_SEGMENTS = 9;

    private HybridModel() {}

    /** Everything one hybrid player needs for one frame (the deferred renderer poses the model later). */
    public static final class State {
        public DragonType type = DragonType.FIRE;
        public int stage = 1;
        public float ageInTicks;
        /** 0 = wings folded on the back, 1 = wings wide open (in the air). */
        public float wingSpread;
        public boolean gliding;
        public float walkPos;
        public float walkSpeed;
        public boolean crouching;
        /** The alpha of its race: the alpha scale shines on its forehead. */
        public boolean alpha;
    }

    /**
     * The alpha scale of the forehead, one character per voxel (row 0 at the top):
     * d / G / g = dark / mid / light gold (the rim), D / M / L / H = dark / mid / light / highlight colour of the race,
     * R = the ridge in the middle, the most prominent part.
     */
    static final String[] ALPHA_MARK = {
            "..gggG...",
            ".gLLMMDG.",
            "gLHLRMMDd",
            "gLLLRMMDd",
            "GMLMRMDDd",
            ".GMMRMDd.",
            ".GMMRDDd.",
            "..GMRDd..",
            "..GDRDd..",
            "...GRd...",
            "....d...."};
    private static final String MARK_CELLS = "dGgDMLHR";

    // ------------------------------------------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------------------------------------------

    /**
     * The horns and race features are those of the dragon head ({@link DragonHeadFeatures}), drawn
     * at half size on the human head: the dragon skull (16 pixels) is mapped on the 8 pixels of the head.
     */
    public static LayerDefinition createHeadLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition dragon = root.addOrReplaceChild("dragon", CubeListBuilder.create(), PartPose.offset(0, -4, 1).withScale(0.5F));
        DragonHeadFeatures.build(dragon);
        buildAlphaMark(root);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /**
     * The alpha scale, in front of the forehead (just in front of the hat layer of the skin). Every voxel
     * is a small box whose colour comes from one cell of the palette painted at {@link DragonModel#UV_ALPHA_MARK};
     * the rim sticks out a little, the scale a little more, the ridge the most: a relief you see from the side.
     */
    private static void buildAlphaMark(PartDefinition root) {
        CubeListBuilder b = CubeListBuilder.create();
        for (int row = 0; row < ALPHA_MARK.length; row++) {
            String line = ALPHA_MARK[row];
            for (int col = 0; col < line.length(); col++) {
                char c = line.charAt(col);
                int cell = MARK_CELLS.indexOf(c);
                if (cell < 0) continue;
                float depth = cell <= 2 ? 1.0F : c == 'R' ? 1.8F : c == 'H' ? 1.55F : 1.4F;
                b.texOffs(DragonModel.UV_ALPHA_MARK[0] + (cell % 4) * 6, DragonModel.UV_ALPHA_MARK[1] + (cell / 4) * 3)
                        .addBox(col - line.length() / 2F, row, -depth, 1, 1, depth);
            }
        }
        // 0.4 pixel per voxel: 3.6 x 4.4 pixels of the 8 x 8 face, from the top of the forehead down between the eyes
        root.addOrReplaceChild("alpha_scale", b, PartPose.offset(0, -8.2F, -4.55F).withScale(0.4F));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // wings: the dragon wing geometry, turned so that "towards the tail" means "down the back"
        buildWing(root, false);
        buildWing(root, true);
        // spikes along the spine (elder+): the back scale of Dragon Mounts (2x6x12 at UV 0,32), scaled down
        CubeListBuilder ridge = CubeListBuilder.create().texOffs(0, 32).addBox(-1, -6, 0, 2, 6, 12);
        for (int i = 0; i < 3; i++) {
            root.addOrReplaceChild("ridge_" + i, ridge, PartPose.offsetAndRotation(0, 1.5F + i * 3.2F, 2.0F, -2.0F, 0, 0));
        }
        buildTail(root, "tail_", true);
        buildTail(root, "tail2_", false);
        buildTail(root, "tail3_", false);
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void buildWing(PartDefinition root, boolean mirror) {
        String dir = mirror ? "left_" : "right_";
        // the root is placed on the shoulder blade; its rotation turns the dragon frame into the human frame
        PartDefinition wingRoot = root.addOrReplaceChild(dir + "wing_root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(mirror ? 1.6F : -1.6F, 2.0F, 2.1F, -Mth.HALF_PI + 0.15F, 0, 0));
        CubeListBuilder arm = CubeListBuilder.create().mirror(mirror);
        box(arm.texOffs(0, 152), mirror, -28, -3, -3, 28, 6, 6);
        box(arm.texOffs(116, 232), mirror, -28, 0, 2, 28, 0, 24);
        CubeListBuilder foreArm = box(CubeListBuilder.create().mirror(mirror).texOffs(0, 164), mirror, -48, -2, -2, 48, 4, 4);
        CubeListBuilder finger = CubeListBuilder.create().mirror(mirror);
        box(finger.texOffs(0, 172), mirror, -70, -1, -1, 70, 2, 2);
        box(finger.texOffs(-49, 176), mirror, -70, 0, 1, 70, 0, 48);
        CubeListBuilder lastFinger = CubeListBuilder.create().mirror(mirror);
        box(lastFinger.texOffs(0, 172), mirror, -70, -1, -1, 70, 2, 2);
        box(lastFinger.texOffs(-32, 224), mirror, -70, 0, 1, 70, 0, 32);
        PartPose fingerPos = PartPose.offset(mirror ? 47 : -47, 0, 0);

        PartDefinition armPart = wingRoot.addOrReplaceChild(dir + "wing_arm", arm, PartPose.ZERO);
        PartDefinition forePart = armPart.addOrReplaceChild(dir + "wing_forearm", foreArm, PartPose.offset(mirror ? 28 : -28, 0, 0));
        for (int j = 1; j < 4; j++) DragonModel.addBlade(forePart.addOrReplaceChild(dir + "wing_finger_" + j, finger, fingerPos), mirror);
        DragonModel.addBlade(forePart.addOrReplaceChild(dir + "wing_finger_4", lastFinger, fingerPos), mirror);
    }

    private static void buildTail(PartDefinition root, String prefix, boolean flame) {
        // the tail leaves the lower back, going down and backwards
        PartDefinition tailRoot = root.addOrReplaceChild(prefix + "root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0, 10.5F, 1.6F, -0.9F, 0, 0));
        for (int i = 0; i < TAIL_SEGMENTS; i++) {
            PartDefinition seg = tailRoot.addOrReplaceChild(prefix + i,
                    CubeListBuilder.create().texOffs(152, 88).addBox(-5, -5, -5, 10, 10, 10), PartPose.ZERO);
            seg.addOrReplaceChild("scale", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -8, -3, 2, 4, 6), PartPose.ZERO);
            // wither: bones only / forest: a liana
            seg.addOrReplaceChild("vertebra", DragonModel.uv(DragonModel.UV_BONE).addBox(-3, -3, -3.5F, 6, 6, 7).addBox(-6, -1, -1, 12, 2, 2)
                    .addBox(-1, -7, -1, 2, 4, 2), PartPose.ZERO);
            seg.addOrReplaceChild("vine", DragonModel.uv(DragonModel.UV_VINE).addBox(-3, -3, -5, 6, 6, 10), PartPose.ZERO);
            seg.addOrReplaceChild("right_spike", CubeListBuilder.create().texOffs(0, 117).addBox(-1.5F, -1.5F, -1.5F, 3, 3, 32),
                    PartPose.offsetAndRotation(0, -1.5F, 5, -0.26F, -2.53F, 0));
            seg.addOrReplaceChild("left_spike", CubeListBuilder.create().texOffs(0, 117).mirror().addBox(-1.5F, -1.5F, -1.5F, 3, 3, 32),
                    PartPose.offsetAndRotation(0, -1.5F, 5, -0.26F, 2.53F, 0));
        }
        DragonModel.buildTailEnd(tailRoot.addOrReplaceChild(prefix + "tip", CubeListBuilder.create(), PartPose.ZERO));
        if (flame) DragonModel.buildFlameSheets(tailRoot.addOrReplaceChild(prefix + "flame", CubeListBuilder.create(), PartPose.ZERO));
    }

    private static CubeListBuilder box(CubeListBuilder b, boolean mirror, float x, float y, float z, float w, float h, float d) {
        if (mirror) x = 0;
        return b.addBox(x, y, z, w, h, d);
    }

    // ------------------------------------------------------------------------------------------------------
    // Models
    // ------------------------------------------------------------------------------------------------------

    /** Horns, crown and race features, drawn in the space of the player's head. */
    public static final class Head extends Model<State> {
        private final DragonHeadFeatures features;
        private final ModelPart alphaScale;

        public Head(ModelPart root) {
            super(root, RenderTypes::entityCutout);
            features = new DragonHeadFeatures(root.getChild("dragon"));
            alphaScale = root.getChild("alpha_scale");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            features.apply(DragonModel.shape(s.type), s.stage, s.ageInTicks, true);
            alphaScale.visible = s.alpha;
            // the alpha scale takes the place of the moonstone of the moonlight hybrid
            if (s.alpha) features.moonstone.visible = false;
        }
    }

    /** Wings, spine spikes and tail(s), drawn in the space of the player's body. */
    public static final class Body extends Model<State> {
        private final ModelPart[] wingRoots = new ModelPart[2];
        private final ModelPart[] wingArms = new ModelPart[2];
        private final ModelPart[] wingForearms = new ModelPart[2];
        private final ModelPart[][] wingFingers = new ModelPart[2][4];
        private final ModelPart[][] wingBlades = new ModelPart[2][4];
        private final ModelPart[] ridge = new ModelPart[3];
        private final TailParts[] tails = new TailParts[3];

        private static final class TailParts {
            final ModelPart root;
            final ModelPart[] segments = new ModelPart[TAIL_SEGMENTS];
            final ModelPart[] scales = new ModelPart[TAIL_SEGMENTS];
            final ModelPart[] spikesL = new ModelPart[TAIL_SEGMENTS];
            final ModelPart[] spikesR = new ModelPart[TAIL_SEGMENTS];
            final ModelPart[] vertebrae = new ModelPart[TAIL_SEGMENTS];
            final ModelPart[] vines = new ModelPart[TAIL_SEGMENTS];
            final ModelPart tip, fin, trident, club, cloud, sword;
            final ModelPart flame;

            TailParts(ModelPart model, String prefix) {
                root = model.getChild(prefix + "root");
                for (int i = 0; i < TAIL_SEGMENTS; i++) {
                    segments[i] = root.getChild(prefix + i);
                    scales[i] = segments[i].getChild("scale");
                    spikesL[i] = segments[i].getChild("left_spike");
                    spikesR[i] = segments[i].getChild("right_spike");
                    vertebrae[i] = segments[i].getChild("vertebra");
                    vines[i] = segments[i].getChild("vine");
                }
                tip = root.getChild(prefix + "tip");
                fin = tip.getChild("fin");
                trident = tip.getChild("trident");
                club = tip.getChild("club");
                cloud = tip.getChild("cloud");
                sword = tip.getChild("sword");
                flame = root.hasChild(prefix + "flame") ? root.getChild(prefix + "flame") : null;
            }
        }

        public Body(ModelPart root) {
            super(root, RenderTypes::entityCutout);
            String[] dirs = {"right_", "left_"};
            for (int w = 0; w < 2; w++) {
                wingRoots[w] = root.getChild(dirs[w] + "wing_root");
                wingArms[w] = wingRoots[w].getChild(dirs[w] + "wing_arm");
                wingForearms[w] = wingArms[w].getChild(dirs[w] + "wing_forearm");
                for (int f = 0; f < 4; f++) {
                    wingFingers[w][f] = wingForearms[w].getChild(dirs[w] + "wing_finger_" + (f + 1));
                    wingBlades[w][f] = wingFingers[w][f].getChild("blade");
                }
            }
            for (int i = 0; i < 3; i++) ridge[i] = root.getChild("ridge_" + i);
            tails[0] = new TailParts(root, "tail_");
            tails[1] = new TailParts(root, "tail2_");
            tails[2] = new TailParts(root, "tail3_");
        }

        @Override
        public void setupAnim(State s) {
            super.setupAnim(s);
            int stage = s.stage;
            float t = s.ageInTicks;

            // --- wings: bigger at every stage ---
            float size = switch (stage) {
                case 1 -> 0.11F;
                case 2 -> 0.14F;
                case 3 -> 0.17F;
                case 4 -> 0.20F;
                default -> 0.235F;
            };
            float spread = Mth.clamp(s.wingSpread, 0, 1);
            float flap = s.gliding ? Mth.sin(t * 0.25F) * 0.05F : Mth.sin(t * 0.6F) * 0.35F * spread * (1 - spread * 0.5F);
            float breathe = Mth.sin(t * 0.08F) * 0.03F;
            for (int w = 0; w < 2; w++) {
                float side = w == 0 ? 1 : -1;
                ModelPart rootPart = wingRoots[w];
                rootPart.xScale = rootPart.yScale = rootPart.zScale = size;
                // folded (ground pose of the dragon) -> open (glide pose of the dragon)
                float armX = lerp(spread, 0.0F, -0.25F);
                float armY = lerp(spread, 1.4F + breathe, 0.25F);
                float armZ = lerp(spread, 0.8F, 0.35F + flap);
                ModelPart arm = wingArms[w];
                arm.xRot = armX;
                arm.yRot = armY * side;
                arm.zRot = armZ * side;
                ModelPart fore = wingForearms[w];
                fore.xRot = 0;
                fore.yRot = lerp(spread, -2.8F, -0.5F) * side;
                fore.zRot = lerp(spread, 0.0F, -0.25F - flap * 0.5F) * side;
                float[] fold = {2.7F, 2.8F, 2.9F, 3.0F};
                float[] open = {0.1F, 0.9F, 1.7F, 2.5F};
                for (int f = 0; f < 4; f++) {
                    ModelPart finger = wingFingers[w][f];
                    finger.xRot = 0.005F * (f + 1);
                    finger.yRot = lerp(spread, fold[f], open[f]) * side;
                    finger.zRot = 0;
                    // sun: blades of light instead of the finger and its membrane
                    finger.skipDraw = DragonModel.shape(s.type).bladeWings();
                    wingBlades[w][f].visible = finger.skipDraw;
                }
                if (s.crouching) rootPart.y += 1.5F;
            }

            // --- back spikes (elder+) ---
            for (int i = 0; i < ridge.length; i++) {
                ModelPart p = ridge[i];
                p.visible = stage >= 4;
                p.xScale = 0.5F;
                p.yScale = i == 1 ? 0.6F : 0.45F;
                p.zScale = 0.28F;
            }

            // --- tail(s): as many as the dragon of the race (the forest hybrid has three lianas) ---
            DragonModel.Shape shape = DragonModel.shape(s.type);
            boolean vines = shape.tailBody() == DragonModel.TailBody.VINES;
            int count = vines ? 3 : shape.tails(stage);
            float[] forks = count == 3 ? new float[] {-1, 0, 1} : count == 2 ? new float[] {-1, 1} : new float[] {0};
            for (int i = 0; i < tails.length; i++) {
                tails[i].root.visible = i < count;
                if (i < count) animTail(tails[i], s, shape, forks[i], count);
            }
        }

        private static void animTail(TailParts tail, State s, DragonModel.Shape shape, float fork, int count) {
            int stage = s.stage;
            boolean vines = shape.tailBody() == DragonModel.TailBody.VINES;
            boolean bones = shape.tailBody() == DragonModel.TailBody.BONES;
            int length = vines ? 4 + stage - (fork == 0 ? 0 : 1) : 4 + stage;
            int segs = Math.min(TAIL_SEGMENTS, length);
            float stageScale = 0.75F + stage * 0.07F;
            float thickness = vines ? 0.55F : shape.thickness();
            boolean spikesAllowed = !bones && !vines && shape.thickness() >= 0.8F && shape.horns() != DragonModel.Horns.NONE
                    && shape.tip() != DragonModel.TailTip.FIN && shape.tip() != DragonModel.TailTip.CLUB;
            float t = s.ageInTicks;
            boolean middleScales = shape.middleScales();
            float spread = count == 3 ? (vines ? 0.22F : 0.5F) : 0.45F;
            tail.root.yRot = fork * spread;
            if (s.crouching) tail.root.xRot -= 0.3F;
            if (s.gliding) tail.root.xRot += 0.7F;

            float x = 0, y = 0, z = 0, xRot = 0, yRot = 0;
            for (int i = 0; i < TAIL_SEGMENTS; i++) {
                ModelPart seg = tail.segments[i];
                if (i >= segs) {
                    seg.visible = false;
                    continue;
                }
                seg.visible = true;
                float f = i / (float) segs;
                float scale = Mth.lerp(f, 0.34F, 0.11F) * stageScale;
                // slow sway, faster when walking; the tail curls down along its length
                float phase = vines ? fork * 1.7F : 0;
                float sway = Mth.sin(t * 0.09F - i * 0.5F + phase) * 0.12F + Mth.sin(s.walkPos * 0.6F - i * 0.4F) * 0.18F * Math.min(1, s.walkSpeed);
                xRot = 0.10F * i + (s.gliding ? -0.08F * i : 0);
                yRot = sway * (0.4F + f) + fork * 0.08F * i;
                seg.x = x;
                seg.y = y;
                seg.z = z;
                seg.xRot = xRot;
                seg.yRot = yRot;
                seg.zRot = vines ? Mth.sin(t * 0.1F + i * 0.7F + phase) * 0.3F : 0;
                seg.xScale = seg.yScale = scale * thickness;
                seg.zScale = scale;
                seg.skipDraw = bones || vines;
                tail.vertebrae[i].visible = bones;
                tail.vines[i].visible = vines;
                tail.scales[i].visible = stage >= 2 && middleScales && !bones && !vines;
                boolean spikes = spikesAllowed && stage >= 3 && i >= segs - 3 && i < segs - 1;
                tail.spikesL[i].visible = tail.spikesR[i].visible = spikes;
                tail.spikesL[i].zScale = tail.spikesR[i].zScale = 0.75F;

                // next segment along the rotated +z axis of this one
                float len = 10 * scale - 0.4F;
                y -= Mth.sin(xRot) * len;
                z += Mth.cos(yRot) * Mth.cos(xRot) * len;
                x += Mth.sin(yRot) * Mth.cos(xRot) * len;
            }
            // end of the tail: fin, trident or club (their -z axis must continue the tail)
            ModelPart tip = tail.tip;
            tip.x = x;
            tip.y = y;
            tip.z = z;
            tip.xRot = -xRot;
            tip.yRot = yRot + Mth.PI;
            float tipScale = (0.55F + 0.1125F * (stage - 1)) * 0.35F * switch (shape.tip()) {
                case CLUB, TRIDENT -> 1.7F;
                case CLOUD -> 1.4F;
                case FIN -> 1.35F;
                case SWORD -> 1.25F;
                default -> 1.0F;
            };
            tip.xScale = tip.yScale = tip.zScale = tipScale;
            tail.fin.visible = shape.tip() == DragonModel.TailTip.FIN;
            tail.trident.visible = shape.tip() == DragonModel.TailTip.TRIDENT && stage >= 2;
            tail.club.visible = shape.tip() == DragonModel.TailTip.CLUB;
            tail.cloud.visible = shape.tip() == DragonModel.TailTip.CLOUD && stage >= 2;
            tail.sword.visible = shape.tip() == DragonModel.TailTip.SWORD && stage >= 2;
            if (tail.flame != null) {
                ModelPart flame = tail.flame;
                flame.visible = shape.tip() == DragonModel.TailTip.FLAME && stage >= 2;
                flame.x = x;
                flame.y = y;
                flame.z = z;
                flame.xRot = -tail.root.xRot; // upright whatever the angle of the tail
                float fs = (0.45F + 0.15F * (stage - 2)) * 0.32F;
                flame.xScale = flame.zScale = fs;
                flame.yScale = fs * (1 + Mth.sin(t * 0.9F) * 0.06F);
                flame.yRot = Mth.sin(t * 0.13F) * 0.4F;
            }
        }

        private static float lerp(float t, float a, float b) {
            float k = t * t * (3 - 2 * t);
            return a + (b - a) * k;
        }
    }
}
