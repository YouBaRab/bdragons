package com.yb.dragons.client.model;

import com.yb.dragons.YBDragons;
import com.yb.dragons.dragon.DragonType;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Winged tetrapod dragon. Geometry and texture layout come from Dragon Mounts: Legacy
 * (Nico Bergemann / Kay9, GPL-3.0) so that the Dragon Mounts and Even More Dragons textures fit.
 *
 * <p>Differences with the original: the neck and tail segments are real separate parts (the modern
 * deferred renderer cannot re-draw one part several times), extra parts were added for the
 * 5 evolution stages (second pair of horns, crown, nose horn, back spikes, tail spikes) and each race
 * has its own anatomy (see {@link Shape}): the volcanic fire dragon has a flame at the end of its
 * tail, the ice dragon an ice crown joining its horns, the water dragon fins and mossy horns, the
 * forest dragon branches and a tail of lianas, the nether dragon fungi, devil horns and three
 * trident tails, the chubby terra dragon a club tail and two pairs of wings, the sun dragon a halo,
 * the storm dragon lightning horns and the wither dragon a tail of bones.</p>
 *
 * <p>The race features use free areas of the 256x256 texture (see the {@code UV_*} constants),
 * painted for each race by tools/gen_textures.py.</p>
 */
public class DragonModel extends EntityModel<DragonRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(YBDragons.id("dragon"), "main");

    public static final int NECK_SIZE = 10;
    public static final int TAIL_SIZE = 10;
    public static final int VERTS_NECK = 7;
    public static final int VERTS_TAIL = 12;
    public static final int HEAD_OFS = -16;
    public static final int VINES = 8;
    public static final int MUSHROOMS = 13;
    public static final int HALO_SEGMENTS = 14;
    public static final int DORSAL = 5;
    public static final int CLOUDS = 5;
    public static final int BUBBLES = 9;
    public static final int FLAME_CREST = 7;
    public static final int PLATES = 5;
    public static final int MOSS_TUFTS = 10;
    public static final int FLOWERS = 9;

    // --- texture areas of the race features (u, v) ---
    public static final int[] UV_ICE = {38, 112};
    public static final int[] UV_BARK = {70, 112};
    public static final int[] UV_STEM = {102, 112};
    public static final int[] UV_CAP_RED = {110, 112};
    public static final int[] UV_CAP_BLUE = {110, 120};
    public static final int[] UV_DARK = {134, 112};
    public static final int[] UV_METAL = {166, 112};
    public static final int[] UV_LIGHT = {198, 112};
    public static final int[] UV_ELECTRIC = {230, 112};
    public static final int[] UV_BONE = {38, 128};
    public static final int[] UV_MOSS = {70, 128};
    public static final int[] UV_VINE = {102, 128};
    public static final int[] UV_LEAF = {134, 128};
    public static final int[] UV_FLAME = {140, 176};
    public static final int[] UV_EAR_FIN = {166, 176};
    public static final int[] UV_TAIL_FIN = {192, 176};
    public static final int[] UV_FANG = {166, 128};
    public static final int[] UV_CLOUD = {198, 128};
    public static final int[] UV_DORSAL = {222, 176};
    public static final int[] UV_LAVA = {230, 128};
    public static final int[] UV_ROCK = {140, 210};
    public static final int[] UV_GOLD = {226, 232};
    public static final int[] UV_SILVER = {196, 232};
    public static final int[] UV_SKULL_SMALL = {192, 88};
    public static final int[] UV_SKULL_BIG = {216, 88};
    public static final int[] UV_BLADE = {70, 144};
    public static final int[] UV_SWORD = {176, 56};
    public static final int[] UV_FLOWER = {222, 200};
    /** Alpha: palette of the alpha scale on the forehead of the hybrid (8 cells of 6x3 texels, 4 per row). */
    public static final int[] UV_ALPHA_MARK = {224, 152};

    // ------------------------------------------------------------------------------------------------------
    // Race anatomy
    // ------------------------------------------------------------------------------------------------------

    public enum Horns { CLASSIC, ICE_CROWN, MOSSY, BRANCHES, DEVIL, NOSE_ONLY, NONE, LIGHTNING }

    public enum TailTip { NONE, FLAME, FIN, TRIDENT, CLUB, CLOUD, SWORD }

    public enum TailBody { SCALES, VINES, BONES }

    /** What grows on the back: boiling lava and flames (fire), rock plates and gold (terra), a small garden (forest). */
    public enum Back { NONE, LAVA, ROCK, GARDEN }

    /** What replaces the spiky crown of the legendary stage: small skulls (wither), a crescent moon (moonlight). */
    public enum Crest { NONE, SKULLS, MOON }

    /**
     * The anatomy of a race.
     * @param maxTails    number of tails at the legendary stage
     * @param thickness   width of the tail(s)
     * @param tailHorns   tail spikes of the Dragon Mounts breed
     * @param middleScales one row of scales on the tail (else two rows)
     * @param serpent     long and slender body (sea serpent, storm serpent): thin body, long neck, long waving tail
     * @param bladeWings  wings made of blades of light (each finger is a sword, no membrane between them)
     */
    public record Shape(Horns horns, TailTip tip, TailBody tailBody, int maxTails, float thickness, boolean tailHorns,
                        boolean middleScales, boolean finEars, boolean fungi, boolean halo, boolean chubby, boolean fourWings,
                        boolean serpent, Back back, Crest crest, boolean bladeWings) {

        /** Number of tails at a stage: the extra tails grow at the legendary stage (nether: III and V). */
        public int tails(int stage) {
            if (maxTails == 3) return stage >= 5 ? 3 : stage >= 3 ? 2 : 1;
            if (maxTails == 2) return stage >= 5 ? 2 : 1;
            return 1;
        }
    }

    private static final Shape[] SHAPES = new Shape[DragonType.values().length];

    static {
        for (DragonType t : DragonType.values()) {
            SHAPES[t.ordinal()] = switch (t) {
                case FIRE -> new Shape(Horns.CLASSIC, TailTip.FLAME, TailBody.SCALES, 1, 1.0F, false, false, false, false, false, false, false, false,
                        Back.LAVA, Crest.NONE, false);
                case ICE -> new Shape(Horns.ICE_CROWN, TailTip.NONE, TailBody.SCALES, 2, 1.0F, false, true, false, false, false, false, false, false,
                        Back.NONE, Crest.NONE, false);
                case WATER -> new Shape(Horns.MOSSY, TailTip.FIN, TailBody.SCALES, 1, 0.8F, false, true, true, false, false, false, false, true,
                        Back.NONE, Crest.NONE, false);
                case FOREST -> new Shape(Horns.BRANCHES, TailTip.NONE, TailBody.VINES, 1, 1.0F, false, true, false, false, false, false, false, false,
                        Back.GARDEN, Crest.NONE, false);
                case NETHER -> new Shape(Horns.DEVIL, TailTip.TRIDENT, TailBody.SCALES, 3, 0.55F, false, true, false, true, false, false, false, false,
                        Back.NONE, Crest.NONE, false);
                case END -> new Shape(Horns.CLASSIC, TailTip.NONE, TailBody.SCALES, 2, 1.0F, false, true, false, false, false, false, false, false,
                        Back.NONE, Crest.NONE, false);
                case TERRA -> new Shape(Horns.NOSE_ONLY, TailTip.CLUB, TailBody.SCALES, 1, 1.4F, true, true, false, false, false, true, true, false,
                        Back.ROCK, Crest.NONE, false);
                case SUNLIGHT -> new Shape(Horns.NONE, TailTip.SWORD, TailBody.SCALES, 1, 1.0F, false, true, false, false, true, false, false, false,
                        Back.NONE, Crest.NONE, true);
                case STORM -> new Shape(Horns.LIGHTNING, TailTip.CLOUD, TailBody.SCALES, 1, 0.75F, false, true, false, false, false, false, false, true,
                        Back.NONE, Crest.NONE, false);
                case MOONLIGHT -> new Shape(Horns.CLASSIC, TailTip.NONE, TailBody.SCALES, 2, 1.0F, true, true, false, false, false, false, false, false,
                        Back.NONE, Crest.MOON, false);
                case WITHER -> new Shape(Horns.CLASSIC, TailTip.NONE, TailBody.BONES, 1, 1.0F, true, false, false, false, false, false, false, false,
                        Back.NONE, Crest.SKULLS, false);
            };
        }
    }

    public static Shape shape(DragonType type) {
        return SHAPES[type.ordinal()];
    }

    // ------------------------------------------------------------------------------------------------------
    // Parts
    // ------------------------------------------------------------------------------------------------------

    public final ModelPart body;
    public final ModelPart back;
    public final ModelPart backSpikes;
    public final ModelPart head;
    public final ModelPart jaw;
    /** Upper jaw and nostrils (separate from the skull so that it can be shorter: nether underbite). */
    public final ModelPart snout;
    public final ModelPart upperTeeth, lowerTeeth, sabers, whiskers;
    public final ModelPart[] saberTeeth = new ModelPart[2];
    public final ModelPart[] dorsalFins = new ModelPart[DORSAL];
    public final ModelPart[] clouds = new ModelPart[CLOUDS];
    /** Horns, crown and the head features of the races. */
    public final DragonHeadFeatures headFeatures;
    public final ModelPart[] neck = new ModelPart[VERTS_NECK];
    public final ModelPart[] neckScales = new ModelPart[VERTS_NECK];
    public final Tail[] tails = new Tail[3];
    public final ModelPart flame;
    public final ModelPart[][] vines = new ModelPart[VINES][VERTS_TAIL];
    public final ModelPart[][] vineLeaves = new ModelPart[VINES][VERTS_TAIL];

    public final ModelPart[] mushrooms = new ModelPart[MUSHROOMS];
    /** Fire: boiling lava bubbles and the crest of flames on the spine. */
    public final ModelPart[] bubbles = new ModelPart[BUBBLES];
    public final ModelPart[] flameCrest = new ModelPart[FLAME_CREST];
    /** Terra: rock plates on the back, with two gold crystals each. */
    public final ModelPart[] plates = new ModelPart[PLATES];
    public final ModelPart[][] plateCrystals = new ModelPart[PLATES][2];
    /** Forest: tufts of moss and flowers. */
    public final ModelPart[] mossTufts = new ModelPart[MOSS_TUFTS];
    public final ModelPart[] flowers = new ModelPart[FLOWERS];

    // [0]: right fore  [1]: right hind  [2]: left fore  [3]: left hind / [0] thigh [1] crus [2] foot [3] toe
    public final ModelPart[][] legs = new ModelPart[4][4];
    public final ModelPart[] wingArms;
    public final ModelPart[] wingForearms;
    public final ModelPart[][] wingFingers = new ModelPart[2][4];
    /** Sun: the blade of light carried by each finger. */
    public final ModelPart[][] wingBlades = new ModelPart[2][4];
    /** Second pair of wings of the terra dragon. */
    public final ModelPart[] rearWingArms;
    public final ModelPart[] rearWingForearms;
    public final ModelPart[][] rearWingFingers = new ModelPart[2][4];
    private final ModelPart[][] rearWingBlades = new ModelPart[2][4];

    /** One tail chain (up to three for the nether dragon), with the decoration of its end. */
    public static final class Tail {
        public final ModelPart[] segments = new ModelPart[VERTS_TAIL];
        public final ModelPart[] middleScales = new ModelPart[VERTS_TAIL];
        public final ModelPart[] leftScales = new ModelPart[VERTS_TAIL];
        public final ModelPart[] rightScales = new ModelPart[VERTS_TAIL];
        public final ModelPart[] leftSpikes = new ModelPart[VERTS_TAIL];
        public final ModelPart[] rightSpikes = new ModelPart[VERTS_TAIL];
        public final ModelPart[] vertebrae = new ModelPart[VERTS_TAIL];
        /** Placed by the animator at the end of the tail, turned like its last segment. */
        public final ModelPart tip;
        public final ModelPart fin;
        public final ModelPart trident;
        public final ModelPart club;
        public final ModelPart cloud;
        public final ModelPart sword;

        Tail(ModelPart root, String prefix) {
            for (int i = 0; i < VERTS_TAIL; i++) {
                ModelPart seg = root.getChild(prefix + i);
                segments[i] = seg;
                middleScales[i] = seg.getChild("middle_scale");
                leftScales[i] = seg.getChild("left_scale");
                rightScales[i] = seg.getChild("right_scale");
                leftSpikes[i] = seg.getChild("left_spike");
                rightSpikes[i] = seg.getChild("right_spike");
                vertebrae[i] = seg.getChild("vertebra");
            }
            tip = root.getChild(prefix + "tip");
            fin = tip.getChild("fin");
            trident = tip.getChild("trident");
            club = tip.getChild("club");
            cloud = tip.getChild("cloud");
            sword = tip.getChild("sword");
        }

        public void setVisible(boolean visible) {
            for (ModelPart p : segments) p.visible = visible;
            tip.visible = visible;
        }
    }

    public DragonModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.back = body.getChild("back");
        this.backSpikes = body.getChild("back_spikes");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.snout = head.getChild("snout");
        this.upperTeeth = snout.getChild("teeth");
        this.whiskers = snout.getChild("whiskers");
        this.lowerTeeth = jaw.getChild("teeth");
        this.sabers = jaw.getChild("sabers");
        this.saberTeeth[0] = sabers.getChild("saber_right");
        this.saberTeeth[1] = sabers.getChild("saber_left");
        this.headFeatures = new DragonHeadFeatures(head);
        for (int i = 0; i < VERTS_NECK; i++) {
            neck[i] = root.getChild("neck_" + i);
            neckScales[i] = neck[i].getChild("neck_scale");
        }
        tails[0] = new Tail(root, "tail_");
        tails[1] = new Tail(root, "tail2_");
        tails[2] = new Tail(root, "tail3_");
        flame = root.getChild("tail_flame");
        for (int v = 0; v < VINES; v++) {
            for (int i = 0; i < VERTS_TAIL; i++) {
                vines[v][i] = root.getChild("vine" + v + "_" + i);
                vineLeaves[v][i] = vines[v][i].getChild("leaf");
            }
        }

        for (int i = 0; i < MUSHROOMS; i++) mushrooms[i] = body.getChild("mushroom_" + i);
        for (int i = 0; i < DORSAL; i++) dorsalFins[i] = body.getChild("dorsal_" + i);
        for (int i = 0; i < CLOUDS; i++) clouds[i] = body.getChild("cloud_" + i);
        for (int i = 0; i < BUBBLES; i++) bubbles[i] = body.getChild("bubble_" + i);
        for (int i = 0; i < FLAME_CREST; i++) flameCrest[i] = body.getChild("crest_flame_" + i);
        for (int i = 0; i < PLATES; i++) {
            plates[i] = body.getChild("plate_" + i);
            plateCrystals[i][0] = plates[i].getChild("crystal_a");
            plateCrystals[i][1] = plates[i].getChild("crystal_b");
        }
        for (int i = 0; i < MOSS_TUFTS; i++) mossTufts[i] = body.getChild("moss_" + i);
        for (int i = 0; i < FLOWERS; i++) flowers[i] = body.getChild("flower_" + i);

        ModelPart rightWingArm = root.getChild("right_wing_arm");
        ModelPart leftWingArm = root.getChild("left_wing_arm");
        ModelPart rightWingForearm = rightWingArm.getChild("right_wing_forearm");
        ModelPart leftWingForearm = leftWingArm.getChild("left_wing_forearm");
        this.wingArms = new ModelPart[] {rightWingArm, leftWingArm};
        this.wingForearms = new ModelPart[] {rightWingForearm, leftWingForearm};
        for (int i = 1; i < 5; i++) {
            wingFingers[0][i - 1] = rightWingForearm.getChild("right_wing_finger_" + i);
            wingFingers[1][i - 1] = leftWingForearm.getChild("left_wing_finger_" + i);
            wingBlades[0][i - 1] = wingFingers[0][i - 1].getChild("blade");
            wingBlades[1][i - 1] = wingFingers[1][i - 1].getChild("blade");
        }
        ModelPart rearRightArm = root.getChild("rear_right_wing_arm");
        ModelPart rearLeftArm = root.getChild("rear_left_wing_arm");
        ModelPart rearRightFore = rearRightArm.getChild("rear_right_wing_forearm");
        ModelPart rearLeftFore = rearLeftArm.getChild("rear_left_wing_forearm");
        this.rearWingArms = new ModelPart[] {rearRightArm, rearLeftArm};
        this.rearWingForearms = new ModelPart[] {rearRightFore, rearLeftFore};
        for (int i = 1; i < 5; i++) {
            rearWingFingers[0][i - 1] = rearRightFore.getChild("rear_right_wing_finger_" + i);
            rearWingFingers[1][i - 1] = rearLeftFore.getChild("rear_left_wing_finger_" + i);
            rearWingBlades[0][i - 1] = rearWingFingers[0][i - 1].getChild("blade");
            rearWingBlades[1][i - 1] = rearWingFingers[1][i - 1].getChild("blade");
        }
        String[] parts = {"thigh", "crus", "foot", "toe"};
        for (int i = 0; i < legs.length; i++) {
            String name = (i < 2 ? "right_" : "left_") + (i % 2 == 0 ? "fore_" : "hind_");
            ModelPart parent = root;
            for (int j = 0; j < parts.length; j++) parent = legs[i][j] = parent.getChild(name + parts[j]);
        }
    }

    // ------------------------------------------------------------------------------------------------------
    // Geometry
    // ------------------------------------------------------------------------------------------------------

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        buildBody(root);
        buildNeck(root);
        buildHead(root);
        buildTail(root, "tail_");
        buildTail(root, "tail2_");
        buildTail(root, "tail3_");
        buildFlame(root);
        buildVines(root);
        buildWing(root, "", false, 4);
        buildWing(root, "", true, 4);
        buildWing(root, "rear_", false, 30);
        buildWing(root, "rear_", true, 30);
        buildLeg(root, false, false);
        buildLeg(root, true, false);
        buildLeg(root, false, true);
        buildLeg(root, true, true);
        return LayerDefinition.create(mesh, 256, 256);
    }

    static CubeListBuilder uv(int[] uv) {
        return CubeListBuilder.create().texOffs(uv[0], uv[1]);
    }

    /**
     * A straight bar from one point to another (in the parent space), with a square section.
     * Used for the crystals, branches, lightning bolts and bridges.
     */
    static void beam(PartDefinition parent, String name, int[] uv, float x0, float y0, float z0, float x1, float y1, float z1, float t) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        float pitch = (float) Math.asin(-dy / len);
        float yaw = (float) Mth.atan2(dx, dz);
        parent.addOrReplaceChild(name, uv(uv).addBox(-t / 2, -t / 2, 0, t, t, len), PartPose.offsetAndRotation(x0, y0, z0, pitch, yaw, 0));
    }

    private static void buildBody(PartDefinition root) {
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-12, 0, -16, 24, 24, 64)
                        .texOffs(0, 32).addBox(-1, -6, 10, 2, 6, 12).addBox(-1, -6, 30, 2, 6, 12),
                PartPose.offset(0, 4, 8));
        body.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 32).addBox(-1, -6, -10, 2, 6, 12), PartPose.ZERO);
        // elder+ : a taller ridge of spikes on the back (same texture area as the scales)
        body.addOrReplaceChild("back_spikes", CubeListBuilder.create()
                        .texOffs(0, 32).addBox(-1, -10, 0, 2, 10, 6)
                        .texOffs(0, 32).addBox(-1, -12, 20, 2, 12, 6)
                        .texOffs(0, 32).addBox(-1, -9, 38, 2, 9, 6),
                PartPose.ZERO);
        // nether fungi growing on the back and the flanks like warts (crimson, some warped)
        float[][] spots = {
                // x, y, z, side (0 top, -1 left flank, 1 right flank)
                {-7, 0, -8, 0}, {6, 0, 2, 0}, {-12, 8, 4, -1}, {12, 11, 12, 1}, {-5, 0, 10, 0}, {8, 0, 22, 0}, {-12, 12, 26, -1},
                {-8, 0, 32, 0}, {12, 7, 36, 1}, {4, 0, 42, 0}, {-12, 6, 44, -1}, {-3, 0, -12, 0}, {12, 14, -6, 1}};
        // water: a row of fins along the spine
        for (int i = 0; i < DORSAL; i++) {
            body.addOrReplaceChild("dorsal_" + i, uv(UV_DORSAL).addBox(0, -10, 0, 0, 10, 14),
                    PartPose.offsetAndRotation(0, 0.5F, -14 + i * 12.5F, 0, 0, 0));
        }
        // storm: storm clouds floating along the back
        float[][] puffs = {{-3, -10}, {3, 4}, {-2, 18}, {3, 31}, {-1, 43}};
        for (int i = 0; i < CLOUDS; i++) {
            body.addOrReplaceChild("cloud_" + i, uv(UV_CLOUD).addBox(-4, -4, -3, 7, 4, 7).addBox(-1, -6.5F, -2, 6, 5, 6)
                            .addBox(-6, -3, 1, 5, 3, 5).addBox(1, -2.5F, 3, 5, 3, 4),
                    PartPose.offsetAndRotation(puffs[i][0], -1, puffs[i][1], 0, i * 1.3F, 0));
        }
        for (int i = 0; i < MUSHROOMS; i++) {
            float[] s = spots[i];
            int[] cap = i % 4 == 3 ? UV_CAP_BLUE : UV_CAP_RED;
            CubeListBuilder m = uv(UV_STEM).addBox(-1, -4, -1, 2, 4, 2).texOffs(cap[0], cap[1]).addBox(-2.5F, -6, -2.5F, 5, 2, 5);
            float zRot = s[3] * Mth.HALF_PI;
            body.addOrReplaceChild("mushroom_" + i, m, PartPose.offsetAndRotation(s[0], s[1], s[2], (i % 3 - 1) * 0.2F, 0, zRot + (i % 2 == 0 ? 0.15F : -0.15F)));
        }

        // fire: bubbles of boiling lava swelling on the back (x, z; the first ones appear first)
        float[][] bubbles = {{-5, 14}, {6, 26}, {-7, 2}, {4, -6}, {-4, 36}, {8, 10}, {-9, 22}, {3, 44}, {-2, -12}};
        for (int i = 0; i < BUBBLES; i++) {
            body.addOrReplaceChild("bubble_" + i, uv(UV_LAVA).addBox(-2.5F, -3.5F, -2.5F, 5, 5, 5).addBox(-1.5F, -4.5F, -1.5F, 3, 1, 3),
                    PartPose.offset(bubbles[i][0], 1.5F, bubbles[i][1]));
        }
        // fire: a crest of flames along the spine
        float[] crest = {18, 4, 32, -8, 42, 11, 25};
        for (int i = 0; i < FLAME_CREST; i++) {
            buildFlameSheets(body.addOrReplaceChild("crest_flame_" + i, CubeListBuilder.create(), PartPose.offset(0, 0.5F, crest[i])));
        }
        // terra: plates of rock on the back, gold crystals growing out of them
        float[] plateZ = {14, 0, 28, -12, 40};
        for (int i = 0; i < PLATES; i++) {
            float tilt = i % 2 == 0 ? 0.12F : -0.12F;
            PartDefinition plate = body.addOrReplaceChild("plate_" + i, uv(UV_ROCK).addBox(-7, -3, -6, 14, 3, 12).addBox(-4, -5, -4, 8, 2, 8),
                    PartPose.offsetAndRotation(0, 0.8F, plateZ[i], tilt, 0, tilt * 0.6F));
            plate.addOrReplaceChild("crystal_a", uv(UV_GOLD).addBox(-1.5F, -8, -1.5F, 3, 8, 3).addBox(-0.75F, -10, -0.75F, 1.5F, 2, 1.5F),
                    PartPose.offsetAndRotation(-3, -4, -1, -0.2F, 0.6F, -0.35F));
            plate.addOrReplaceChild("crystal_b", uv(UV_GOLD).addBox(-1, -6, -1, 2, 6, 2).addBox(-0.5F, -7.5F, -0.5F, 1, 1.5F, 1),
                    PartPose.offsetAndRotation(3.5F, -4, 2, 0.25F, 0.3F, 0.4F));
        }
        // forest: tufts of moss (x, y, z, side) and small flowers
        float[][] moss = {{-6, 0, -10, 0}, {7, 0, 8, 0}, {-12, 10, 16, -1}, {3, 0, 23, 0}, {12, 9, 30, 1}, {-7, 0, 34, 0},
                {12, 13, 0, 1}, {-12, 6, 40, -1}, {6, 0, 44, 0}, {-2, 0, -14, 0}};
        for (int i = 0; i < MOSS_TUFTS; i++) {
            float[] s = moss[i];
            body.addOrReplaceChild("moss_" + i, uv(UV_MOSS).addBox(-3.5F, -1.5F, -2.5F, 7, 2, 5).addBox(-2, -2.5F, -1.5F, 4, 1, 3),
                    PartPose.offsetAndRotation(s[0], s[1] + 0.5F, s[2], 0, i * 0.9F, s[3] * Mth.HALF_PI));
        }
        float[][] flowers = {{3, -6}, {-6, 10}, {7, 17}, {-4, 27}, {5, 37}, {-8, 1}, {8, -12}, {-6, 44}, {1, 32}};
        for (int i = 0; i < FLOWERS; i++) {
            PartDefinition flower = body.addOrReplaceChild("flower_" + i, uv(UV_FLOWER).addBox(0, -6, -3, 0, 6, 6),
                    PartPose.offsetAndRotation(flowers[i][0], 0.5F, flowers[i][1], 0, i * 0.8F, 0));
            flower.addOrReplaceChild("cross", uv(UV_FLOWER).addBox(0, -6, -3, 0, 6, 6), PartPose.rotation(0, Mth.HALF_PI, 0));
        }
    }

    private static void buildNeck(PartDefinition root) {
        for (int i = 0; i < VERTS_NECK; i++) {
            PartDefinition neck = root.addOrReplaceChild("neck_" + i,
                    CubeListBuilder.create().texOffs(112, 88).addBox(-5, -5, -5, NECK_SIZE, NECK_SIZE, NECK_SIZE), PartPose.ZERO);
            neck.addOrReplaceChild("neck_scale", CubeListBuilder.create().texOffs(0, 0).addBox(-1, -7, -3, 2, 4, 6), PartPose.ZERO);
        }
    }

    private static PartDefinition buildHead(PartDefinition root) {
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-8, -8, 6 + HEAD_OFS, 16, 16, 16),  // upper head
                PartPose.ZERO);
        PartDefinition snout = head.addOrReplaceChild("snout", CubeListBuilder.create()
                        .texOffs(56, 88).addBox(-6, -1, -8 + HEAD_OFS, 12, 5, 16) // upper jaw
                        .texOffs(48, 0).addBox(-5, -3, -6 + HEAD_OFS, 2, 2, 4)   // nostril
                        .mirror().addBox(3, -3, -6 + HEAD_OFS, 2, 2, 4),         // nostril
                PartPose.ZERO);
        PartDefinition jaw = head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(0, 88).addBox(-6, 0, -16, 12, 4, 16),
                PartPose.offset(0, 4, 8 + HEAD_OFS));

        // water: rows of needle teeth that stay visible out of the closed mouth (deep sea style)
        CubeListBuilder upper = uv(UV_FANG);
        CubeListBuilder lower = uv(UV_FANG);
        for (int i = 0; i < 4; i++) {
            float z = -22.5F + i * 3.2F;
            float len = i == 0 ? 4 : 3;
            for (int side = -1; side <= 1; side += 2) {
                upper.addBox(side * 6.05F - 0.4F, 3.5F, z, 0.8F, len, 0.8F);
                lower.addBox(side * 6.05F - 0.4F, -len + 0.5F, z + 8 + 1.6F, 0.8F, len, 0.8F);
            }
        }
        snout.addOrReplaceChild("teeth", upper, PartPose.ZERO);
        jaw.addOrReplaceChild("teeth", lower, PartPose.ZERO);
        // nether: two saber teeth rising from the lower jaw on each side of the (shorter) upper jaw
        PartDefinition sabers = jaw.addOrReplaceChild("sabers", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "right" : "left";
            sabers.addOrReplaceChild("saber_" + n, uv(UV_FANG).addBox(-1, -6, -1, 2, 6, 2).addBox(-0.6F, -11, -0.6F, 1.2F, 5, 1.2F)
                    .addBox(-0.3F, -13, -0.3F, 0.6F, 2, 0.6F), PartPose.offsetAndRotation(side * 5.4F, 0.5F, -11, -0.12F, 0, side * 0.1F));
        }
        // storm: long glowing whiskers drooping from the snout
        PartDefinition whiskers = snout.addOrReplaceChild("whiskers", CubeListBuilder.create(), PartPose.ZERO);
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "right" : "left";
            beam(whiskers, "base_" + n, UV_ELECTRIC, side * 6, 1, -20, side * 13, 3.5F, -15, 0.9F);
            beam(whiskers, "mid_" + n, UV_ELECTRIC, side * 13, 3.5F, -15, side * 17, 9, -8, 0.8F);
            beam(whiskers, "end_" + n, UV_ELECTRIC, side * 17, 9, -8, side * 18, 15, -1, 0.7F);
        }

        DragonHeadFeatures.build(head);
        return head;
    }

    private static void buildTail(PartDefinition root, String prefix) {
        int hornThick = 3;
        float hornOfs = -(hornThick / 2f);
        for (int i = 0; i < VERTS_TAIL; i++) {
            PartDefinition tail = root.addOrReplaceChild(prefix + i,
                    CubeListBuilder.create().texOffs(152, 88).addBox(-5, -5, -5, TAIL_SIZE, TAIL_SIZE, TAIL_SIZE), PartPose.ZERO);
            CubeListBuilder scale = CubeListBuilder.create().texOffs(0, 0).addBox(-1, -8, -3, 2, 4, 6);
            tail.addOrReplaceChild("middle_scale", scale, PartPose.ZERO);
            tail.addOrReplaceChild("left_scale", scale, PartPose.rotation(0, 0, 0.785398f));
            tail.addOrReplaceChild("right_scale", scale, PartPose.rotation(0, 0, -0.785398f));
            tail.addOrReplaceChild("right_spike",
                    CubeListBuilder.create().texOffs(0, 117).addBox(hornOfs, hornOfs, hornOfs, hornThick, hornThick, 32),
                    PartPose.offsetAndRotation(0, hornOfs, TAIL_SIZE / 2f, -0.261799f, -2.53073f, 0));
            tail.addOrReplaceChild("left_spike",
                    CubeListBuilder.create().texOffs(0, 117).mirror().addBox(hornOfs, hornOfs, hornOfs, hornThick, hornThick, 32),
                    PartPose.offsetAndRotation(0, hornOfs, TAIL_SIZE / 2f, -0.261799f, 2.53073f, 0));
            // wither: a vertebra with its side processes instead of the flesh of the tail
            tail.addOrReplaceChild("vertebra", uv(UV_BONE).addBox(-3, -3, -3.5F, 6, 6, 7).addBox(-6, -1, -1, 12, 2, 2)
                    .addBox(-1, -7, -1, 2, 4, 2), PartPose.ZERO);
        }
        // end of the tail: the animator places it on the last segment; its -z axis continues the tail
        buildTailEnd(root.addOrReplaceChild(prefix + "tip", CubeListBuilder.create(), PartPose.ZERO));
    }

    /** The decorations of the end of a tail (pointing to -z): fish fin, trident, club. */
    static void buildTailEnd(PartDefinition tip) {
        // water: a fish fin
        tip.addOrReplaceChild("fin", uv(UV_TAIL_FIN).addBox(0, -10, -14, 0, 20, 14), PartPose.ZERO);
        // nether: a trident (three sharp prongs)
        tip.addOrReplaceChild("trident", uv(UV_METAL)
                .addBox(-0.75F, -0.75F, -9, 1.5F, 1.5F, 9)
                .addBox(-4.5F, -0.6F, -1.2F, 9, 1.2F, 1.2F)
                .addBox(-4.5F, -0.6F, -6, 1.2F, 1.2F, 5)
                .addBox(3.3F, -0.6F, -6, 1.2F, 1.2F, 5)
                .addBox(-0.4F, -0.4F, -11, 0.8F, 0.8F, 2)
                .addBox(-4.3F, -0.4F, -7.5F, 0.8F, 0.8F, 1.5F)
                .addBox(3.5F, -0.4F, -7.5F, 0.8F, 0.8F, 1.5F), PartPose.ZERO);
        // storm: the tail ends in a storm cloud
        tip.addOrReplaceChild("cloud", uv(UV_CLOUD).addBox(-4, -4, -9, 8, 7, 8).addBox(-6.5F, -2, -7, 5, 5, 5)
                .addBox(2, -3, -6, 5, 5, 5).addBox(-2, -6.5F, -7, 5, 4, 5).addBox(-3, -2, -13, 6, 5, 5), PartPose.ZERO);
        // sun: a sword of light (blade, cross guard and grip going into the tail)
        tip.addOrReplaceChild("sword", uv(UV_SWORD)
                .addBox(-0.5F, -2.5F, -26, 1, 5, 24)
                .addBox(-0.5F, -1.5F, -29, 1, 3, 3)
                .addBox(-0.5F, -0.5F, -31, 1, 1, 2)
                .addBox(-1.5F, -5, -2, 3, 10, 2)
                .addBox(-1, -1, 0, 2, 2, 4), PartPose.ZERO);
        // terra: a club of rock covered with warts
        tip.addOrReplaceChild("club", uv(UV_ROCK)
                .addBox(-5, -5, -10, 10, 10, 10)
                .addBox(-2, -6.5F, -7, 3, 2, 3).addBox(1.5F, -6, -3.5F, 2.5F, 2, 2.5F)
                .addBox(4.5F, -2, -8, 2, 3, 3).addBox(4.5F, 1, -3.5F, 2, 2.5F, 2.5F)
                .addBox(-6.5F, -3, -6, 2, 3, 3).addBox(-6.5F, 1.5F, -9, 2, 2.5F, 2.5F)
                .addBox(-1.5F, 4.5F, -5, 3, 2, 3).addBox(-2, -2, -11.5F, 3, 3, 2), PartPose.ZERO);
    }

    /** Fire: a flame burning at the end of the tail (two crossed sheets, always upright). */
    private static void buildFlame(PartDefinition root) {
        buildFlameSheets(root.addOrReplaceChild("tail_flame", CubeListBuilder.create(), PartPose.ZERO));
    }

    static void buildFlameSheets(PartDefinition flame) {
        flame.addOrReplaceChild("sheet1", uv(UV_FLAME).addBox(0, -16, -6, 0, 16, 12), PartPose.ZERO);
        flame.addOrReplaceChild("sheet2", uv(UV_FLAME).addBox(0, -16, -6, 0, 16, 12), PartPose.rotation(0, Mth.HALF_PI, 0));
    }

    /** Forest: the tail is a bundle of lianas leaving the body (one hitbox, several strands). */
    private static void buildVines(PartDefinition root) {
        for (int v = 0; v < VINES; v++) {
            for (int i = 0; i < VERTS_TAIL; i++) {
                PartDefinition seg = root.addOrReplaceChild("vine" + v + "_" + i, uv(UV_VINE).addBox(-1.5F, -1.5F, -5, 3, 3, 10), PartPose.ZERO);
                seg.addOrReplaceChild("leaf", uv(UV_LEAF).addBox(-4, 0, -4, 8, 0, 8),
                        PartPose.offsetAndRotation((v % 2 == 0 ? 2.5F : -2.5F), -1.5F, 0, 0.3F, 0.7F * v, 0.4F));
            }
        }
    }

    private static void buildWing(PartDefinition root, String prefix, boolean mirror, float z) {
        String dir = prefix + (mirror ? "left_" : "right_");
        CubeListBuilder arm = CubeListBuilder.create().mirror(mirror);
        box(arm.texOffs(0, 152), mirror, -28, -3, -3, 28, 6, 6);   // bone
        box(arm.texOffs(116, 232), mirror, -28, 0, 2, 28, 0, 24);  // skin
        CubeListBuilder foreArm = box(CubeListBuilder.create().mirror(mirror).texOffs(0, 164), mirror, -48, -2, -2, 48, 4, 4);
        CubeListBuilder finger = CubeListBuilder.create().mirror(mirror);
        box(finger.texOffs(0, 172), mirror, -70, -1, -1, 70, 2, 2);
        box(finger.texOffs(-49, 176), mirror, -70, 0, 1, 70, 0, 48);
        CubeListBuilder lastFinger = CubeListBuilder.create().mirror(mirror);
        box(lastFinger.texOffs(0, 172), mirror, -70, -1, -1, 70, 2, 2);
        box(lastFinger.texOffs(-32, 224), mirror, -70, 0, 1, 70, 0, 32);
        PartPose fingerPos = mirrorPos(-47, 0, 0, mirror);

        PartDefinition armPart = root.addOrReplaceChild(dir + "wing_arm", arm, mirrorPos(-10, 5, z, mirror));
        PartDefinition forePart = armPart.addOrReplaceChild(dir + "wing_forearm", foreArm, mirrorPos(-28, 0, 0, mirror));
        for (int j = 1; j < 4; j++) addBlade(forePart.addOrReplaceChild(dir + "wing_finger_" + j, finger, fingerPos), mirror);
        addBlade(forePart.addOrReplaceChild(dir + "wing_finger_4", lastFinger, fingerPos), mirror);
    }

    /**
     * Sun: a sword of light along a wing finger (grip, cross guard, long flat blade and its point), in the
     * plane of the wing. Hidden for the other races; the sun dragon hides the finger itself instead.
     */
    static void addBlade(PartDefinition finger, boolean mirror) {
        CubeListBuilder b = CubeListBuilder.create().mirror(mirror);
        b.texOffs(UV_SWORD[0], UV_SWORD[1]);
        mirrorBox(b, mirror, -5, -1, -1, 5, 2, 2);          // grip
        mirrorBox(b, mirror, -7, -1.5F, -4.5F, 2, 3, 9);    // cross guard
        b.texOffs(UV_BLADE[0], UV_BLADE[1]);
        mirrorBox(b, mirror, -73, -0.5F, -2.5F, 66, 1, 5);  // blade
        mirrorBox(b, mirror, -77, -0.5F, -1.5F, 4, 1, 3);   // point
        finger.addOrReplaceChild("blade", b, PartPose.ZERO);
    }

    /** A box given for the right side, moved to the other side of x = 0 for the left wing. */
    private static void mirrorBox(CubeListBuilder b, boolean mirror, float x, float y, float z, float w, float h, float d) {
        b.addBox(mirror ? -x - w : x, y, z, w, h, d);
    }

    private static void buildLeg(PartDefinition root, boolean hind, boolean mirror) {
        float baseLength = 26;
        String name = (mirror ? "left_" : "right_") + (hind ? "hind_" : "fore_");

        float thighPosX = -11, thighPosY = 18, thighPosZ = 4;
        int thighThick = 9;
        int thighLength = (int) (baseLength * (hind ? 0.9f : 0.77f));
        if (hind) {
            thighThick++;
            thighPosY -= 5;
        }
        float thighOfs = -(thighThick / 2f);
        PartDefinition thigh = root.addOrReplaceChild(name + "thigh", CubeListBuilder.create().texOffs(112, hind ? 29 : 0)
                .addBox(thighOfs, thighOfs, thighOfs, thighThick, thighLength, thighThick), mirrorPos(thighPosX, thighPosY, thighPosZ, mirror));

        int crusThick = thighThick - 2;
        int crusLength = (int) (baseLength * (hind ? 0.7f : 0.8f));
        if (hind) {
            crusThick--;
            crusLength -= 2;
        }
        float crusOfs = -(crusThick / 2f);
        PartDefinition crus = thigh.addOrReplaceChild(name + "crus", CubeListBuilder.create().texOffs(hind ? 152 : 148, hind ? 29 : 0)
                .addBox(crusOfs, crusOfs, crusOfs, crusThick, crusLength, crusThick), mirrorPos(0, thighLength + thighOfs, 0, mirror));

        int footWidth = crusThick + 2;
        int footHeight = 4;
        int footLength = (int) (baseLength * (hind ? 0.67f : 0.34f));
        float footOfsX = -(footWidth / 2f), footOfsY = -(footHeight / 2f), footOfsZ = footLength * -0.75f;
        PartDefinition foot = crus.addOrReplaceChild(name + "foot", CubeListBuilder.create().texOffs(hind ? 180 : 210, hind ? 29 : 0)
                .addBox(footOfsX, footOfsY, footOfsZ, footWidth, footHeight, footLength), mirrorPos(0, crusLength + (crusOfs / 2f), 0, mirror));

        int toeLength = (int) (baseLength * (hind ? 0.27f : 0.33f));
        foot.addOrReplaceChild(name + "toe", CubeListBuilder.create().texOffs(hind ? 215 : 176, hind ? 29 : 0)
                        .addBox(-(footWidth / 2f), -(footHeight / 2f), -toeLength, footWidth, footHeight, toeLength),
                mirrorPos(0, 0, footOfsZ - (footOfsY / 2f), mirror));
    }

    private static CubeListBuilder box(CubeListBuilder b, boolean mirror, float x, float y, float z, float w, float h, float d) {
        if (mirror) x = 0;
        return b.addBox(x, y, z, w, h, d);
    }

    private static PartPose mirrorPos(float x, float y, float z, boolean mirror) {
        return PartPose.offset(mirror ? -x : x, y, z);
    }

    // ------------------------------------------------------------------------------------------------------
    // Animation
    // ------------------------------------------------------------------------------------------------------

    /** Per race tail decoration, as defined by the Dragon Mounts / Even More Dragons breeds. */
    public static boolean middleTailScales(DragonType type) {
        return shape(type).middleScales();
    }

    public static boolean nativeTailHorns(DragonType type) {
        return shape(type).tailHorns();
    }

    private static void scale(ModelPart p, float s) {
        p.xScale = p.yScale = p.zScale = s;
    }

    @Override
    public void setupAnim(DragonRenderState state) {
        super.setupAnim(state);
        int stage = state.stage;
        Shape shape = shape(state.type);

        headFeatures.apply(shape, stage, state.ageInTicks, false);

        // --- mouth ---
        boolean nether = state.type == DragonType.NETHER;
        upperTeeth.visible = lowerTeeth.visible = state.type == DragonType.WATER && stage >= 2;
        sabers.visible = nether;
        float saber = 0.55F + 0.1125F * (stage - 1);
        for (ModelPart t : saberTeeth) {
            t.xScale = t.zScale = Math.min(1, saber + 0.2F);
            t.yScale = saber;
        }
        whiskers.visible = state.type == DragonType.STORM;
        whiskers.xScale = whiskers.yScale = whiskers.zScale = 0.6F + 0.1F * (stage - 1);
        if (nether) {
            // underbite: a shorter, narrower upper jaw over a big lower jaw
            snout.xScale = 0.85F;
            snout.zScale = 0.84F;
            jaw.xScale = 1.12F;
            jaw.yScale = 1.25F;
            jaw.zScale = 1.1F;
        }

        // --- body ---
        Back backKind = shape.back();
        int fungi = shape.fungi() ? new int[] {0, 2, 4, 7, 10, 13}[stage]
                : backKind == Back.GARDEN ? new int[] {0, 0, 0, 3, 5, 7}[stage] : 0;
        for (int i = 0; i < MUSHROOMS; i++) {
            mushrooms[i].visible = i < fungi;
            scale(mushrooms[i], backKind == Back.GARDEN ? 0.55F + 0.08F * stage : 0.8F + 0.1F * stage);
        }
        if (shape.chubby()) {
            body.xScale = 1.3F;
            body.yScale = 1.12F;
        }
        backSpikes.visible = stage >= 4 && !shape.serpent() && backKind != Back.ROCK && !(backKind == Back.LAVA && stage >= 3);
        back.visible = stage >= 2 && backKind != Back.ROCK && !(backKind == Back.LAVA && stage >= 3);

        // fire: the lava bubbles swell, then burst (each one with its own rhythm), and the crest of flames flickers
        int bubbleCount = backKind == Back.LAVA ? new int[] {0, 0, 3, 5, 7, 9}[stage] : 0;
        for (int i = 0; i < BUBBLES; i++) {
            float cycle = state.ageInTicks * (0.018F + 0.002F * (i % 3)) + i * 0.37F;
            cycle -= Mth.floor(cycle);
            ModelPart b = bubbles[i];
            b.visible = i < bubbleCount && cycle < 0.92F;
            float size = (0.35F + 0.85F * cycle) * (0.75F + 0.06F * stage);
            b.xScale = b.zScale = size;
            b.yScale = size * (0.8F + 0.25F * cycle);
        }
        int flames = backKind == Back.LAVA ? new int[] {0, 0, 0, 3, 5, 7}[stage] : 0;
        for (int i = 0; i < FLAME_CREST; i++) {
            ModelPart f = flameCrest[i];
            f.visible = i < flames;
            float s = 0.5F + 0.08F * stage + (i % 2 == 0 ? 0.06F : 0);
            f.xScale = f.zScale = s;
            f.yScale = s * (1 + Mth.sin(state.ageInTicks * 0.8F + i * 1.7F) * 0.12F);
            f.yRot = Mth.sin(state.ageInTicks * 0.11F + i) * 0.35F;
        }
        // terra: one more rock plate at each stage, gold crystals from stage III
        for (int i = 0; i < PLATES; i++) {
            plates[i].visible = backKind == Back.ROCK && i < stage;
            scale(plates[i], 0.8F + 0.05F * stage);
            plateCrystals[i][0].visible = stage >= 3;
            plateCrystals[i][1].visible = stage >= 4;
            scale(plateCrystals[i][0], stage >= 5 ? 1.2F : 0.9F);
            scale(plateCrystals[i][1], stage >= 5 ? 1.2F : 0.9F);
        }
        // forest: moss everywhere, flowers from stage II
        int tufts = backKind == Back.GARDEN ? new int[] {0, 3, 5, 7, 9, 10}[stage] : 0;
        for (int i = 0; i < MOSS_TUFTS; i++) {
            mossTufts[i].visible = i < tufts;
            scale(mossTufts[i], 0.8F + 0.06F * stage);
        }
        int blooms = backKind == Back.GARDEN ? new int[] {0, 0, 3, 5, 7, 9}[stage] : 0;
        for (int i = 0; i < FLOWERS; i++) {
            flowers[i].visible = i < blooms;
            scale(flowers[i], 0.75F + 0.07F * stage);
        }
        for (int i = 0; i < DORSAL; i++) {
            dorsalFins[i].visible = state.type == DragonType.WATER && i < Math.min(DORSAL, stage + 1);
            dorsalFins[i].yScale = 0.6F + 0.1F * stage;
        }
        for (int i = 0; i < CLOUDS; i++) {
            clouds[i].visible = state.type == DragonType.STORM && i < stage;
            scale(clouds[i], 0.75F + 0.07F * stage + 0.05F * Mth.sin(state.ageInTicks * 0.05F + i));
        }
        if (shape.serpent()) {
            // long and slender
            body.xScale = 0.66F;
            body.yScale = 0.72F;
        }

        // --- tails ---
        int tailCount = shape.tailBody() == TailBody.VINES ? 0 : shape.tails(stage);
        for (int i = 0; i < tails.length; i++) {
            Tail tail = tails[i];
            tail.setVisible(i < tailCount);
            tail.fin.visible = shape.tip() == TailTip.FIN;
            tail.trident.visible = shape.tip() == TailTip.TRIDENT && stage >= 2;
            tail.club.visible = shape.tip() == TailTip.CLUB;
            tail.cloud.visible = shape.tip() == TailTip.CLOUD && stage >= 2;
            tail.sword.visible = shape.tip() == TailTip.SWORD && stage >= 2;
        }
        flame.visible = shape.tip() == TailTip.FLAME && stage >= 2;
        for (ModelPart[] vine : vines) {
            for (ModelPart v : vine) v.visible = shape.tailBody() == TailBody.VINES;
        }

        // --- wings ---
        float wing = switch (stage) {
            case 1 -> 0.6F;
            case 2 -> 0.8F;
            case 3 -> 1.0F;
            case 4 -> 1.3F;
            default -> 1.55F;
        };
        for (ModelPart arm : wingArms) scale(arm, shape.serpent() ? wing * 0.8F : wing);
        if (shape.serpent()) {
            wingArms[0].x = -7;
            wingArms[1].x = 7;
        }
        boolean rear = shape.fourWings() && stage >= 3;
        for (ModelPart arm : rearWingArms) {
            arm.visible = rear;
            scale(arm, wing * (stage >= 5 ? 0.75F : 0.6F));
        }
        // sun: every finger becomes a blade of light (the finger and its membrane are not drawn)
        for (int w = 0; w < 2; w++) {
            for (int f = 0; f < 4; f++) {
                wingFingers[w][f].skipDraw = shape.bladeWings();
                wingBlades[w][f].visible = shape.bladeWings();
                rearWingBlades[w][f].visible = false;
            }
        }

        DragonAnimator animator = state.animator;
        if (animator != null) {
            animator.animate(this, state);
        }
    }
}
