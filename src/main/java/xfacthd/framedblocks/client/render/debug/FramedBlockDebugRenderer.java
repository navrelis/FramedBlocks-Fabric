package xfacthd.framedblocks.client.render.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.*;
import xfacthd.framedblocks.platform.event.ModEvents;
import xfacthd.framedblocks.platform.Platform;
import xfacthd.framedblocks.api.block.blockentity.FramedBlockEntity;
import xfacthd.framedblocks.api.render.debug.AttachDebugRenderersEvent;
import xfacthd.framedblocks.api.render.debug.BlockDebugRenderer;
import xfacthd.framedblocks.common.config.DevToolsConfig;

import java.util.*;

public final class FramedBlockDebugRenderer
{
    private static final Map<BlockEntityType<? extends FramedBlockEntity>, Set<BlockDebugRenderer<? extends FramedBlockEntity>>> RENDERERS_BY_TYPE = new IdentityHashMap<>();

    private static boolean outlineEffectRequested = false;

    @SuppressWarnings("unchecked")
    public static void render(DeltaTracker delta, Camera camera, PoseStack poseStack)
    {
        if (DevToolsConfig.VIEW.isDoubleBlockPartHitDebugRendererEnabled())
        {
            outlineEffectRequested = true;
        }

        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult blockHit)) return;

        ClientLevel level = Objects.requireNonNull(Minecraft.getInstance().level);
        BlockPos pos = blockHit.getBlockPos();
        if (!(level.getBlockEntity(pos) instanceof FramedBlockEntity be)) return;

        Set<BlockDebugRenderer<? extends FramedBlockEntity>> renderers = RENDERERS_BY_TYPE.get(be.getType());
        if (renderers.isEmpty()) return;

        Vec3 offset = Vec3.atLowerCornerOf(pos).subtract(camera.getPosition());
        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        float partialTick = delta.getGameTimeDeltaPartialTick(false);
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource();
        for (BlockDebugRenderer<? extends FramedBlockEntity> renderer : renderers)
        {
            if (!renderer.isEnabled()) continue;

            poseStack.pushPose();
            ((BlockDebugRenderer<FramedBlockEntity>) renderer).render(be, blockHit, partialTick, poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
        buffer.endBatch();
    }

    /**
     * {@return whether the entity outline post-processing effect needs to be run for the current frame even if
     * vanilla did not request it} Resets the request
     */
    public static boolean consumeOutlineEffectRequest()
    {
        boolean requested = outlineEffectRequested;
        outlineEffectRequested = false;
        return requested;
    }

    public static void init()
    {
        if (Platform.isProduction()) return;

        ModEvents.post(new AttachDebugRenderersEvent((type, renderer) ->
                RENDERERS_BY_TYPE.computeIfAbsent(type, $ -> new ReferenceOpenHashSet<>()).add(renderer)
        ));
    }

    private FramedBlockDebugRenderer() { }
}
