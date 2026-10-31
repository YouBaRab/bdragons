package com.yb.dragons.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;

/**
 * Procedural animation of the dragon (neck waves, flapping wings, walking legs, curling tail...).
 * Ported from Dragon Mounts: Legacy (Nico Bergemann / Kay9, GPL-3.0), adapted to the render-state
 * renderer of Minecraft 26.x and to player dragons. One instance per animated entity, client side only.
 */
public class DragonAnimator {
    // timing interp vars
    private final Lerped animTimer = new Lerped(0, -Float.MAX_VALUE, Float.MAX_VALUE);
    private final Lerped groundTimer = new Lerped(1, 0, 1);
    private final Lerped flutterTimer = new Lerped(0, 0, 1);
    private final Lerped walkTimer = new Lerped(0, 0, 1);
    private final Lerped sitTimer = new Lerped(0, 0, 1);
    private final Lerped jawTimer = new Lerped(0, 0, 1);
    private final Lerped speedTimer = new Lerped(1, 0, 1);

    private boolean initTrails;
    private final Trail yTrail = new Trail(8);
    private final Trail yawTrail = new Trail(16);
    private final Trail pitchTrail = new Trail(16);

    private boolean onGround = true;
    private boolean wingsDown;
    private boolean sitting;
    private double prevRenderYawOffset;
    private double yawAbs;
    private float moveSpeed;
    /** Set when the wings beat down, read by the renderer to play the flap sound. */
    public boolean flapped;

    // frame values
    private float partialTicks, moveTime, lookYaw, lookPitch;
    private float anim, ground, flutter, walk, sit, jaw, speed, animBase, cycleOfs, health = 1;

    private final float[] wingArm = new float[3];
    private final float[] wingForearm = new float[3];
    private final float[] wingArmFlutter = new float[3];
    private final float[] wingForearmFlutter = new float[3];
    private final float[] wingArmGlide = new float[3];
    private final float[] wingForearmGlide = new float[3];
    private final float[] wingArmGround = new float[3];
    private final float[] wingForearmGround = new float[3];
    private final float[] xGround = {0, 0, 0, 0};
    private final float[][] xGroundStand = {{0.8f, -1.5f, 1.3f, 0}, {-0.3f, 1.5f, -0.2f, 0}};
    private final float[][] xGroundSit = {{0.3f, -1.8f, 1.8f, 0}, {-0.8f, 1.8f, -0.9f, 0}};
    private final float[][][] xGroundWalk = {
            {{0.4f, -1.4f, 1.3f, 0}, {0.1f, 1.2f, -0.5f, 0}},
            {{1.2f, -1.6f, 1.3f, 0}, {-0.3f, 2.1f, -0.9f, 0.6f}},
            {{0.9f, -2.1f, 1.8f, 0.6f}, {-0.7f, 1.4f, -0.2f, 0}}};
    private final float[] xGroundWalk2 = {0, 0, 0, 0};
    private final float[] yGroundStand = {-0.25f, 0.25f};
    private final float[] yGroundSit = {0.1f, 0.35f};
    private final float[] yGroundWalk = {-0.1f, 0.1f};
    private final float[][] xAirAll = {{0, 0, 0, 0}, {0, 0, 0, 0}};
    private final float[] yAirAll = {-0.1f, 0.1f};

    /**
     * Client tick.
     * @param flying true when the dragon is in the air (not standing, not swimming)
     */
    public void tick(LivingEntity entity, boolean flying, boolean sitting, boolean breathing, float renderScale) {
        this.onGround = !flying;
        this.sitting = sitting;
        this.health = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 1;

        if (!initTrails) {
            yTrail.fill((float) entity.getY());
            yawTrail.fill(entity.yBodyRot);
            pitchTrail.fill(getModelPitch(0));
            initTrails = true;
        }
        if (entity.getHealth() <= 0) {
            animTimer.sync(); groundTimer.sync(); flutterTimer.sync(); walkTimer.sync(); sitTimer.sync(); jawTimer.sync();
            return;
        }

        float speedMax = 0.05f * Math.max(0.3F, renderScale);
        float xD = (float) (entity.getX() - entity.xo);
        float yD = (float) (entity.getY() - entity.yo);
        float zD = (float) (entity.getZ() - entity.zo);
        float speedEnt = xD * xD + zD * zD;
        float speedMulti = Mth.clamp(speedEnt / speedMax, 0, 1);

        float animAdd = 0.035f;
        if (!onGround) animAdd += (1 - speedMulti) * animAdd;
        animTimer.add(animAdd);

        float groundVal = groundTimer.get();
        if (onGround) {
            groundVal *= 0.95f;
            groundVal += 0.08f;
        } else {
            groundVal -= 0.1f;
        }
        groundTimer.set(groundVal);

        boolean flutterFlag = !onGround && (entity.horizontalCollision || yD > -0.1 || speedEnt < speedMax);
        flutterTimer.add(flutterFlag ? 0.1f : -0.1f);

        boolean walkFlag = moveSpeed > 0.1 && !sitting;
        walkTimer.add(walkFlag ? 0.1f : -0.1f);

        float sitVal = sitTimer.get();
        sitVal += sitting ? 0.1f : -0.1f;
        sitVal *= 0.95f;
        sitTimer.set(sitVal);

        jawTimer.add(breathing ? 0.2f : -0.2f);

        boolean speedFlag = speedEnt > speedMax || isNearGround(entity, renderScale);
        speedTimer.add(speedFlag ? 0.05f : -0.05f);

        double yawDiff = entity.yBodyRot - prevRenderYawOffset;
        prevRenderYawOffset = entity.yBodyRot;
        if (yawDiff < 180 && yawDiff > -180) yawAbs += yawDiff;

        yTrail.update((float) entity.getY());
        yawTrail.update((float) -yawAbs);
        pitchTrail.update(getModelPitch(0));
    }

    private static boolean isNearGround(LivingEntity entity, float renderScale) {
        if (entity.onGround()) return true;
        AABB box = entity.getBoundingBox();
        double depth = 3 * Math.max(0.4, renderScale);
        return !entity.level().noCollision(entity, new AABB(box.minX, box.minY - depth, box.minZ, box.maxX, box.minY, box.maxZ));
    }

    /** Applies the animation to the model, every frame. */
    public void animate(DragonModel model, DragonRenderState state) {
        this.partialTicks = state.partialTick;
        this.moveTime = state.walkAnimationPos;
        this.moveSpeed = state.walkAnimationSpeed * state.renderScale * 2.5F;
        this.lookYaw = Mth.clamp(state.yRot, -120, 120);
        this.lookPitch = Mth.clamp(state.xRot, -90, 90);

        anim = animTimer.get(partialTicks);
        ground = groundTimer.get(partialTicks);
        flutter = flutterTimer.get(partialTicks);
        walk = walkTimer.get(partialTicks);
        sit = sitTimer.get(partialTicks);
        jaw = jawTimer.get(partialTicks);
        speed = speedTimer.get(partialTicks);

        animBase = anim * ((float) Math.PI) * 2;
        cycleOfs = Mth.sin(animBase - 1) + 1;

        boolean newWingsDown = cycleOfs > 1;
        if (newWingsDown && !wingsDown && flutter != 0) flapped = true;
        wingsDown = newWingsDown;

        cycleOfs = (cycleOfs * cycleOfs + cycleOfs * 2) * 0.05f;
        cycleOfs *= lerpc(0.5f, 1, flutter);
        cycleOfs *= lerpc(1, 0.5f, ground);

        DragonModel.Shape shape = DragonModel.shape(state.type);
        animHeadAndNeck(model, state.stage, shape);
        if (shape.tailBody() == DragonModel.TailBody.VINES) {
            animVines(model, state);
        } else {
            int count = shape.tails(state.stage);
            float[] forks = count == 3 ? new float[] {-1, 0, 1} : count == 2 ? new float[] {-1, 1} : new float[] {0};
            for (int i = 0; i < count; i++) animTail(model.tails[i], state, shape, forks[i], count);
        }
        animFlame(model, state);
        animWings(model, shape);
        animLegs(model, shape);
    }

    private void animHeadAndNeck(DragonModel model, int stage, DragonModel.Shape shape) {
        float x = 0, y = 14, z = -8;
        float xRot = 0, yRot = 0;
        for (int i = 0; i < DragonModel.VERTS_NECK; i++) {
            ModelPart neck = model.neck[i];
            float vertMulti = (i + 1) / (float) DragonModel.VERTS_NECK;
            float baseRotX = Mth.cos(i * 0.45f + animBase) * 0.15f;
            baseRotX *= lerpc(0.2f, 1, flutter);
            baseRotX *= lerpc(1, 0.2f, sit);
            float ofsRotX = Mth.sin(vertMulti * ((float) Math.PI) * 0.9f) * 0.75f;

            xRot = baseRotX;
            xRot *= smooth(1, 0.5f, walk);
            xRot += (1 - speed) * vertMulti;
            xRot -= lerpc(0, ofsRotX, ground * health);
            yRot = (float) Math.toRadians(lookYaw) * vertMulti * speed;

            float v = lerpc(1.6f, 1, vertMulti);
            neck.x = x;
            neck.y = y;
            neck.z = z;
            neck.xRot = xRot;
            neck.yRot = yRot;
            neck.zRot = 0;
            neck.xScale = shape.chubby() ? v * 1.2F : shape.serpent() ? v * 0.8F : v;
            neck.yScale = shape.serpent() ? v * 0.8F : v;
            neck.zScale = 0.6f;
            // hatchlings have a smooth neck, elders a ridge of big spikes
            model.neckScales[i].visible = stage >= 2 && (i % 2 != 0 || i == 0);
            if (stage >= 4) {
                model.neckScales[i].yScale = 1.6F;
                model.neckScales[i].visible = true;
            }

            float neckSize = DragonModel.NECK_SIZE * 0.6f - 1.4f;
            if (shape.serpent()) {
                // a long neck, its segments longer so that they stay joined
                neckSize *= 1.4F;
                neck.zScale = 0.85F;
            }
            x -= Mth.sin(yRot) * Mth.cos(xRot) * neckSize;
            y += Mth.sin(xRot) * neckSize;
            z -= Mth.cos(yRot) * Mth.cos(xRot) * neckSize;
        }

        model.head.xRot = (float) Math.toRadians(lookPitch) + (1 - speed);
        model.head.yRot = yRot;
        model.head.zRot = 0;
        model.head.x = x;
        model.head.y = y;
        model.head.z = z;
        // young dragons have a big head
        float headScale = switch (stage) {
            case 1 -> 1.35F;
            case 2 -> 1.18F;
            default -> 1.0F;
        };
        model.head.xScale = model.head.yScale = model.head.zScale = headScale;

        model.jaw.xRot = jaw * 0.75f;
        model.jaw.xRot += (1 - Mth.sin(animBase)) * 0.1f * flutter;
    }

    /**
     * @param fork  -1 / 0 / +1 to spread the tails of a dragon that has several apart
     * @param count number of tails of the dragon
     */
    private void animTail(DragonModel.Tail tail, DragonRenderState state, DragonModel.Shape shape, float fork, int count) {
        int stage = state.stage;
        boolean middle = shape.middleScales();
        boolean nativeHorns = shape.tailHorns();
        boolean bones = shape.tailBody() == DragonModel.TailBody.BONES;
        boolean thin = shape.thickness() < 0.8F;
        float thickness = shape.thickness();
        float forkYaw = count > 1 ? fork * (count == 3 ? 0.6F : 0.5F) : 0;

        float x = 0, y = 16, z = 62;
        float rotXStand, rotYStand = 0, rotXSit, rotYSit, rotXAir = 0, rotYAir = 0;
        float xRot = 0, yRot = 0;
        int n = DragonModel.VERTS_TAIL;
        for (int i = 0; i < n; i++) {
            ModelPart seg = tail.segments[i];
            float vertMulti = (i + 1) / (float) n;
            float amp = 0.1f + i / (n * 2f);

            rotXStand = (i - n * 0.6f) * -amp * 0.4f;
            rotXStand += (Mth.sin(animBase * 0.2f) * Mth.sin(animBase * 0.37f) * 0.4f * amp - 0.1f) * (1 - sit);
            rotXSit = rotXStand * 0.8f;
            rotYStand = (rotYStand + Mth.sin(i * 0.45f + animBase * 0.5f + fork * 0.6f)) * amp * 0.4f;
            rotYSit = Mth.sin(vertMulti * ((float) Math.PI)) * ((float) Math.PI) * 1.2f - 0.5f;
            rotXAir -= Mth.sin(i * 0.45f + animBase) * 0.04f * lerpc(0.3f, 1, flutter);

            xRot = lerpc(rotXStand, rotXSit, sit);
            yRot = lerpc(rotYStand, rotYSit, sit);
            xRot = lerpc(rotXAir, xRot, ground);
            yRot = lerpc(rotYAir, yRot, ground);

            float angleLimit = 160 * vertMulti;
            float yawOfs = Mth.clamp(yawTrail.get(partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
            float pitchOfs = Mth.clamp(pitchTrail.get(partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
            xRot += (float) Math.toRadians(pitchOfs);
            xRot -= (1 - speed) * vertMulti * 2;
            yRot += (float) Math.toRadians(180 - yawOfs) + forkYaw * vertMulti * 1.4F;
            // serpents: the long tail waves from side to side
            if (shape.serpent()) yRot += Mth.sin(animBase * 0.6F - i * 0.55F) * 0.16F * (0.4F + vertMulti);

            // decorations: scales from the juvenile stage, spikes from the adult stage (none on bones and thin tails)
            boolean scales = stage >= 2 && !bones;
            tail.middleScales[i].visible = scales && middle;
            tail.leftScales[i].visible = scales && !middle;
            tail.rightScales[i].visible = scales && !middle;
            boolean spikes = stage >= 3 && i > n - 7 && i < n - 3 && !bones && !thin;
            if (stage >= 4 && nativeHorns && (i == 3 || i == 4)) spikes = true;
            // no spike on the sun dragon (no horn at all), the fish tail and the club tail
            if (shape.horns() == DragonModel.Horns.NONE || shape.tip() == DragonModel.TailTip.FIN
                    || shape.tip() == DragonModel.TailTip.CLUB) spikes = false;
            tail.leftSpikes[i].visible = tail.rightSpikes[i].visible = spikes;
            // wither: only the bones remain
            seg.skipDraw = bones;
            tail.vertebrae[i].visible = bones;

            float scale = lerpc(1.5f, 0.3f, vertMulti);
            seg.x = x;
            seg.y = y;
            seg.z = z;
            seg.xRot = xRot;
            seg.yRot = yRot;
            seg.zRot = 0;
            seg.xScale = seg.yScale = scale * thickness;
            seg.zScale = scale;

            float tailSize = DragonModel.TAIL_SIZE * scale - 0.7f;
            if (shape.serpent()) {
                tailSize *= 1.25F;
                seg.zScale = scale * 1.25F;
            }
            y += Mth.sin(xRot) * tailSize;
            z -= Mth.cos(yRot) * Mth.cos(xRot) * tailSize;
            x -= Mth.sin(yRot) * Mth.cos(xRot) * tailSize;
        }
        // end of the tail: the fin, the trident or the club continue the last segment
        ModelPart tip = tail.tip;
        tip.x = x;
        tip.y = y;
        tip.z = z;
        tip.xRot = xRot;
        tip.yRot = yRot;
        tip.zRot = 0;
        float tipScale = 0.55F + 0.1125F * (stage - 1);
        tipScale *= switch (shape.tip()) {
            case CLUB, TRIDENT -> 1.7F;
            case CLOUD -> 1.4F;
            case FIN -> 1.35F;
            case SWORD -> 1.25F;
            default -> 1.0F;
        };
        tip.xScale = tip.yScale = tip.zScale = tipScale;
        if (shape.tip() == DragonModel.TailTip.FIN) tip.zRot = Mth.sin(animBase * 0.8F) * 0.15F;
    }

    /** Fire: the flame at the end of the (single) tail, upright and flickering. */
    private void animFlame(DragonModel model, DragonRenderState state) {
        ModelPart flame = model.flame;
        if (!flame.visible) return;
        ModelPart tip = model.tails[0].tip;
        flame.x = tip.x;
        flame.y = tip.y + 1.5F;
        flame.z = tip.z;
        float s = 0.45F + 0.15F * (state.stage - 2);
        float flicker = Mth.sin(state.ageInTicks * 0.9F) * 0.06F + Mth.sin(state.ageInTicks * 2.3F) * 0.04F;
        flame.xScale = flame.zScale = s;
        flame.yScale = s * (1 + flicker);
        flame.yRot = Mth.sin(state.ageInTicks * 0.13F) * 0.4F;
    }

    /** Forest: a thick bundle of lianas leaving the body in two rows, each swaying with its own rhythm. */
    private void animVines(DragonModel model, DragonRenderState state) {
        int n = DragonModel.VERTS_TAIL;
        int stage = state.stage;
        int[] missing = {0, 2, 1, 3, 1, 0, 2, 3};
        for (int v = 0; v < DragonModel.VINES; v++) {
            float side = (v - (DragonModel.VINES - 1) / 2F) * 0.6F;
            float x = side * 2.4F, y = 16 + (v % 2 == 0 ? -1.8F : 1.8F), z = 61;
            float rotYStand = 0, rotXAir = 0;
            float phase = v * 1.3F;
            for (int i = 0; i < n; i++) {
                ModelPart seg = model.vines[v][i];
                float vertMulti = (i + 1) / (float) n;
                float amp = 0.1f + i / (n * 2f);
                float rotXStand = (i - n * 0.6f) * -amp * 0.4f
                        + (Mth.sin(animBase * 0.2f + phase) * Mth.sin(animBase * 0.37f) * 0.4f * amp - 0.1f) * (1 - sit);
                rotYStand = (rotYStand + Mth.sin(i * 0.5f + animBase * 0.5f + phase)) * amp * 0.45f;
                rotXAir -= Mth.sin(i * 0.45f + animBase + phase) * 0.04f * lerpc(0.3f, 1, flutter);
                float xRot = lerpc(rotXAir, lerpc(rotXStand, rotXStand * 0.8f, sit), ground);
                float yRot = lerpc(0, lerpc(rotYStand, Mth.sin(vertMulti * Mth.PI) * Mth.PI * 1.2f - 0.5f, sit), ground);

                float angleLimit = 160 * vertMulti;
                float yawOfs = Mth.clamp(yawTrail.get(partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
                float pitchOfs = Mth.clamp(pitchTrail.get(partialTicks, 0, i + 1) * 2, -angleLimit, angleLimit);
                xRot += (float) Math.toRadians(pitchOfs) + (v % 2 == 0 ? -0.04F : 0.04F) * vertMulti;
                xRot -= (1 - speed) * vertMulti * 2;
                yRot += (float) Math.toRadians(180 - yawOfs) + side * 0.07F * vertMulti;

                seg.visible = i < n - missing[v];
                seg.skipDraw = false;
                float thin = lerpc(1.1F, 0.4F, vertMulti);
                seg.x = x;
                seg.y = y;
                seg.z = z;
                seg.xRot = xRot;
                seg.yRot = yRot;
                seg.zRot = Mth.sin(animBase + i * 0.7F + phase) * 0.3F;
                seg.xScale = seg.yScale = thin;
                seg.zScale = 1;
                model.vineLeaves[v][i].visible = stage >= 2 && i % 3 == v % 3 && i > 0;

                float len = 9.4F;
                y += Mth.sin(xRot) * len;
                z -= Mth.cos(yRot) * Mth.cos(xRot) * len;
                x -= Mth.sin(yRot) * Mth.cos(xRot) * len;
            }
        }
    }

    private void animWings(DragonModel model, DragonModel.Shape shape) {
        float aSpeed = sit > 0 ? 0.6f : 1;
        float a1 = animBase * aSpeed * 0.35f;
        float a2 = animBase * aSpeed * 0.5f;
        float a3 = animBase * aSpeed * 0.75f;
        // folded on the ground: along the flank, never through the body or the legs (see WingFold)
        WingFold fold = WingFold.FOLD;

        if (ground < 1) {
            wingArmFlutter[0] = 0.125f - Mth.cos(animBase) * 0.2f;
            wingArmFlutter[1] = 0.25f;
            wingArmFlutter[2] = (Mth.sin(animBase) + 0.125f) * 0.8f;
            wingForearmFlutter[0] = 0;
            wingForearmFlutter[1] = -wingArmFlutter[1] * 2;
            wingForearmFlutter[2] = -(Mth.sin(animBase + 2) + 0.5f) * 0.75f;

            wingArmGlide[0] = -0.25f - Mth.cos(animBase * 2) * Mth.cos(animBase * 1.5f) * 0.04f;
            wingArmGlide[1] = 0.25f;
            wingArmGlide[2] = 0.35f + Mth.sin(animBase) * 0.05f;
            wingForearmGlide[0] = 0;
            wingForearmGlide[1] = -wingArmGlide[1] * 2;
            wingForearmGlide[2] = -0.25f + (Mth.sin(animBase + 2) + 0.5f) * 0.05f;
        }
        if (ground > 0) {
            // the folded wings breathe a little (only around the axis of the arm bone: they stay off the body)
            float breath = Mth.sin(a1) * Mth.sin(a2) * 0.015f + Mth.sin(moveTime * 0.5f) * 0.01f * walk;
            wingArmGround[0] = fold.arm[0] + breath;
            wingArmGround[1] = fold.arm[1];
            wingArmGround[2] = fold.arm[2];
            System.arraycopy(fold.forearm, 0, wingForearmGround, 0, 3);
        }

        slerpArrays(wingArmGlide, wingArmFlutter, wingArm, flutter);
        slerpArrays(wingForearmGlide, wingForearmFlutter, wingForearm, flutter);
        slerpArrays(wingArm, wingArmGround, wingArm, ground);
        slerpArrays(wingForearm, wingForearmGround, wingForearm, ground);

        // folded, the shoulders move to the top edge of the flanks: the arm and its membrane then stay out of the body
        float side = 12 * model.body.xScale + WingFold.SHOULDER_OUT;
        for (ModelPart[] arms : new ModelPart[][] {model.wingArms, model.rearWingArms}) {
            arms[0].x = lerpc(arms[0].x, -side, ground);
            arms[1].x = lerpc(arms[1].x, side, ground);
            arms[0].y = arms[1].y = lerpc(arms[0].y, WingFold.SHOULDER_Y, ground);
        }

        mirrorRotate(model.wingArms[0], model.wingArms[1], wingArm[0], wingArm[1], wingArm[2]);
        mirrorRotate(model.wingForearms[0], model.wingForearms[1], wingForearm[0], wingForearm[1], wingForearm[2]);
        // second pair (terra): the same beat, a little behind and tighter
        mirrorRotate(model.rearWingArms[0], model.rearWingArms[1], wingArm[0] * 0.9F + 0.1F, wingArm[1] * 0.95F, wingArm[2] * 0.85F);
        mirrorRotate(model.rearWingForearms[0], model.rearWingForearms[1], wingForearm[0], wingForearm[1], wingForearm[2] * 0.9F);

        float[] yUnfold = {0.1f, 0.9f, 1.7f, 2.5f};
        float rotX = 0;
        float width = lerpc(1, WingFold.FINGER_WIDTH, ground);
        for (int i = 0; i < model.wingFingers[0].length; i++) {
            rotX += 0.005f;
            float[] f = fold.fingers[i];
            float fx = smooth(rotX, f[0], ground), fy = smooth(yUnfold[i], f[1], ground), fz = smooth(0, f[2], ground);
            mirrorRotate(model.wingFingers[0][i], model.wingFingers[1][i], fx, fy, fz);
            mirrorRotate(model.rearWingFingers[0][i], model.rearWingFingers[1][i], fx, fy, fz);
            // the membranes of the fingers fold up like a fan
            for (int w = 0; w < 2; w++) {
                model.wingFingers[w][i].zScale = width;
                model.rearWingFingers[w][i].zScale = width;
            }
        }
    }

    private void animLegs(DragonModel model, DragonModel.Shape shape) {
        if (ground < 1) {
            float footAirOfs = cycleOfs * 0.1f;
            float footAirX = 0.75f + cycleOfs * 0.1f;
            xAirAll[0][0] = 1.3f + footAirOfs;
            xAirAll[0][1] = -(0.7f * speed + 0.1f + footAirOfs);
            xAirAll[0][2] = footAirX;
            xAirAll[0][3] = footAirX * 0.5f;
            xAirAll[1][0] = footAirOfs + 0.6f;
            xAirAll[1][1] = footAirOfs + 0.8f;
            xAirAll[1][2] = footAirX;
            xAirAll[1][3] = footAirX * 0.5f;
        }
        for (int i = 0; i < model.legs.length; i++) {
            ModelPart thigh = model.legs[i][0], crus = model.legs[i][1], foot = model.legs[i][2], toe = model.legs[i][3];
            thigh.z = (i % 2 == 0) ? 4 : 46;
            float[] xAir = xAirAll[i % 2];
            slerpArrays(xGroundStand[i % 2], xGroundSit[i % 2], xGround, sit);
            xGround[3] = -(xGround[0] + xGround[1] + xGround[2]);
            if (walk > 0) {
                splineArrays(moveTime * 0.2f, i > 1, xGroundWalk2, xGroundWalk[0][i % 2], xGroundWalk[1][i % 2], xGroundWalk[2][i % 2]);
                xGroundWalk2[3] -= xGroundWalk2[0] + xGroundWalk2[1] + xGroundWalk2[2];
                slerpArrays(xGround, xGroundWalk2, xGround, walk);
            }
            float yAir = yAirAll[i % 2];
            float yGround = smooth(yGroundStand[i % 2], yGroundSit[i % 2], sit);
            yGround = smooth(yGround, yGroundWalk[i % 2], walk);
            thigh.yRot = smooth(yAir, yGround, ground);
            thigh.xRot = smooth(xAir[0], xGround[0], ground);
            crus.xRot = smooth(xAir[1], xGround[1], ground);
            foot.xRot = smooth(xAir[2], xGround[2], ground);
            toe.xRot = smooth(xAir[3], xGround[3], ground);
            if (i > 1) thigh.yRot *= -1;
            if (shape.chubby()) thigh.xScale = thigh.zScale = 1.18F;
            if (shape.serpent()) {
                // slim legs under the slim body
                thigh.x = i < 2 ? -7.5F : 7.5F;
                thigh.xScale = thigh.zScale = 0.75F;
            }
        }
    }

    /** Body pitch in degrees: the dragon rears up when hovering and dives when falling. */
    public float getModelPitch(float pt) {
        float pitchMoving = Mth.clamp(yTrail.get(pt, 5, 0) * 10, -90, 90);
        return smooth(60, pitchMoving, speedTimer.get(pt));
    }

    public float getModelOffsetY() {
        return 1.5f + (-sit * 0.6f);
    }

    // ------------------------------------------------------------------------------------------------------

    /** Old Mth.clampedLerp(start, end, delta) semantics. */
    private static float lerpc(float a, float b, float x) {
        if (x <= 0) return a;
        if (x >= 1) return b;
        return a + (b - a) * x;
    }

    private static void mirrorRotate(ModelPart right, ModelPart left, float xRot, float yRot, float zRot) {
        right.xRot = xRot;
        right.yRot = yRot;
        right.zRot = zRot;
        left.xRot = xRot;
        left.yRot = -yRot;
        left.zRot = -zRot;
    }

    private static void slerpArrays(float[] a, float[] b, float[] c, float x) {
        if (x <= 0) {
            System.arraycopy(a, 0, c, 0, a.length);
            return;
        }
        if (x >= 1) {
            System.arraycopy(b, 0, c, 0, a.length);
            return;
        }
        for (int i = 0; i < c.length; i++) c[i] = smooth(a[i], b[i], x);
    }

    private static float smooth(float a, float b, float x) {
        if (x <= 0) return a;
        if (x >= 1) return b;
        x = x * x * (3 - 2 * x);
        return a * (1 - x) + b * x;
    }

    private static void splineArrays(float x, boolean shift, float[] result, float[]... nodes) {
        int i1 = (int) x % nodes.length;
        int i2 = (i1 + 1) % nodes.length;
        int i3 = (i1 + 2) % nodes.length;
        float[] a1 = nodes[i1], a2 = nodes[i2], a3 = nodes[i3];
        float xn = x % nodes.length - i1;
        if (shift) catmullRom(xn, result, a2, a3, a1, a2);
        else catmullRom(xn, result, a1, a2, a3, a1);
    }

    private static final float[][] CR = {
            {-0.5f, 1.5f, -1.5f, 0.5f},
            {1.0f, -2.5f, 2.0f, -0.5f},
            {-0.5f, 0.0f, 0.5f, 0.0f},
            {0.0f, 1.0f, 0.0f, 0.0f}};

    private static void catmullRom(float x, float[] result, float[]... knots) {
        int nspans = knots.length - 3;
        x = Mth.clamp(x, 0, 0.9999f) * nspans;
        int span = Math.min((int) x, knots.length - 3);
        x -= span;
        for (int i = 0; i < result.length; i++) {
            float k0 = knots[span][i], k1 = knots[span + 1][i], k2 = knots[span + 2][i], k3 = knots[span + 3][i];
            float c3 = CR[0][0] * k0 + CR[0][1] * k1 + CR[0][2] * k2 + CR[0][3] * k3;
            float c2 = CR[1][0] * k0 + CR[1][1] * k1 + CR[1][2] * k2 + CR[1][3] * k3;
            float c1 = CR[2][0] * k0 + CR[2][1] * k1 + CR[2][2] * k2 + CR[2][3] * k3;
            float c0 = CR[3][0] * k0 + CR[3][1] * k1 + CR[3][2] * k2 + CR[3][3] * k3;
            result[i] = ((c3 * x + c2) * x + c1) * x + c0;
        }
    }

    /** Float interpolated between the previous and the current tick. */
    private static final class Lerped {
        private float current, previous;
        private final float min, max;

        Lerped(float start, float min, float max) {
            this.min = min;
            this.max = max;
            this.current = this.previous = Mth.clamp(start, min, max);
        }

        float get(float x) { return lerpc(previous, current, x); }
        float get() { return current; }
        void set(float v) { previous = current; current = Mth.clamp(v, min, max); }
        void add(float v) { previous = current; current = Mth.clamp(current + v, min, max); }
        void sync() { previous = current; }
    }

    /** Small ring buffer of past values, used for the tail inertia. */
    private static final class Trail {
        private final float[] buffer;
        private int index;

        Trail(int size) { buffer = new float[size]; }

        void fill(float v) { Arrays.fill(buffer, v); }

        void update(float v) {
            index = (index + 1) % buffer.length;
            buffer[index] = v;
        }

        float get(float x, int offset) {
            int i = index - offset;
            int len = buffer.length - 1;
            return lerpc(buffer[i - 1 & len], buffer[i & len], x);
        }

        float get(float x, int offset1, int offset2) {
            return get(x, offset2) - get(x, offset1);
        }
    }
}
