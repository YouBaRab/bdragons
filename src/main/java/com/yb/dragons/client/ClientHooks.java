package com.yb.dragons.client;

import com.yb.dragons.client.screen.CodexScreen;
import net.minecraft.client.Minecraft;

/** Small entry points called from common code, only on the physical client. */
public final class ClientHooks {
    private ClientHooks() {}

    public static void openGrimoire() {
        Minecraft.getInstance().gui.setScreen(new CodexScreen());
    }
}
