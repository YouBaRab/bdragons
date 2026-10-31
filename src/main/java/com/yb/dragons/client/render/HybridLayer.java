package com.yb.dragons.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yb.dragons.client.ClientAnimators;
import com.yb.dragons.client.ClientDragonData;
import com.yb.dragons.client.model.HybridModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Hybrid form: the player keeps his own skin, and grows the horns, wings and tail of his race
 * (with the glowing runes of the race).
 */
public class HybridLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private final HybridModel.Head head;
    private final HybridModel.Body body;

    public HybridLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent, EntityModelSet models) {
        super(parent);
        this.head = new HybridModel.Head(models.bakeLayer(HybridModel.HEAD_LAYER));
        this.body = new HybridModel.Body(models.bakeLayer(HybridModel.BODY_LAYER));
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords, AvatarRenderState state, float yRot, float xRot) {
        if (state.isInvisible) return;
        ClientDragonData.Info info = ClientDragonData.get(state.id);
        if (info == null || !info.isHybrid() || info.type() == null) return;

        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        HybridModel.State s = ClientAnimators.hybridState(state, info, partialTick);
        var texture = info.type().bodyTexture();
        var glow = RenderTypes.eyes(info.type().glowTexture());
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        PlayerModel model = this.getParentModel();

        poseStack.pushPose();
        model.head.translateAndRotate(poseStack);
        collector.submitModel(head, s, poseStack, head.renderType(texture), lightCoords, overlay, state.outlineColor);
        collector.order(1).submitModel(head, s, poseStack, glow, lightCoords, overlay, state.outlineColor);
        poseStack.popPose();

        poseStack.pushPose();
        model.body.translateAndRotate(poseStack);
        collector.submitModel(body, s, poseStack, body.renderType(texture), lightCoords, overlay, state.outlineColor);
        collector.order(1).submitModel(body, s, poseStack, glow, lightCoords, overlay, state.outlineColor);
        poseStack.popPose();
    }
}
