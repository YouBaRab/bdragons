package com.yb.dragons.client.model;

import com.yb.dragons.dragon.DragonType;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/** Everything needed to draw one dragon (wild, town or player) for one frame. */
public class DragonRenderState extends LivingEntityRenderState {
    public DragonType type = DragonType.FIRE;
    /** 1..5 */
    public int stage = 1;
    /** Size relative to an adult Dragon Mounts dragon. */
    public float renderScale = 0.5F;
    public boolean sitting;
    public boolean breathing;
    public float partialTick;
    public float modelPitch;
    public float modelOffsetY = 1.5F;
    /** Alpha ritual: height of the levitation (blocks) and white glow of the body (0..1). */
    public float ritualLift;
    public float whiteFlash;
    @Nullable public DragonAnimator animator;
}
