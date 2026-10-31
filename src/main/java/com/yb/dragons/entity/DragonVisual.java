package com.yb.dragons.entity;

import com.yb.dragons.dragon.DragonType;

/** What the client renderer needs to draw a dragon entity. */
public interface DragonVisual {
    DragonType getDragonType();

    /** 1..5, decides the physical features (horns, wings, double tail...). */
    int getVisualStage();

    boolean isBreathing();

    boolean isDragonSitting();

    /** Size relative to an adult Dragon Mounts dragon. */
    float getRenderScale();
}
