package com.kestalkayden.magnethopper.client;

import com.kestalkayden.magnethopper.MagnetHopperNeoForge;
import com.kestalkayden.magnethopper.block.MagnetHopperBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

/**
 * Client-side placement preview — see Fabric counterpart for full docs.
 *
 * MC 26.2 migration: MultiBufferSource was removed. Custom geometry is now
 * submitted via SubmitCustomGeometryEvent (fired between particle submission
 * and opaque renders), which exposes a SubmitNodeCollector. Call
 * collector.submitCustomGeometry(poseStack, renderType, (pose, vc) -> { ... })
 * to batch line geometry without managing buffer flushing manually.
 */
@EventBusSubscriber(modid = MagnetHopperNeoForge.MOD_ID, value = Dist.CLIENT)
public final class MagnetHopperFieldRenderer {

    private static final float LINE_R = 0.30f;
    private static final float LINE_G = 1.00f;
    private static final float LINE_B = 0.70f;
    private static final float LINE_A = 0.90f;

    private static final float CONTOUR_R = 0.70f;
    private static final float CONTOUR_G = 1.00f;
    private static final float CONTOUR_B = 0.30f;
    private static final float CONTOUR_A = 1.00f;

    private static final float LINE_WIDTH = 2.0f;

    private MagnetHopperFieldRenderer() {}

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int radius = getHeldRadius(mc.player);
        if (radius <= 0) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        BlockPos placePos = bhr.getBlockPos().relative(bhr.getDirection());
        render(event, mc.level, placePos, radius);
    }

    private static int getHeldRadius(Player player) {
        int r = getRadiusForItem(player.getMainHandItem());
        if (r > 0) return r;
        return getRadiusForItem(player.getOffhandItem());
    }

    private static int getRadiusForItem(ItemStack stack) {
        if (stack.is(MagnetHopperBlocks.MAGNET_HOPPER_ITEM.get()))            return MagnetHopperBlocks.MAGNET_HOPPER.get().getTier().radius;
        if (stack.is(MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER_ITEM.get()))   return MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER.get().getTier().radius;
        if (stack.is(MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER_ITEM.get())) return MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER.get().getTier().radius;
        return 0;
    }

    private static void render(SubmitCustomGeometryEvent event,
                               ClientLevel level, BlockPos center, int radius) {
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        // MC 26.2: submit custom geometry through the SubmitNodeCollector.
        // submitCustomGeometry snapshots the current pose and provides a
        // Pose + VertexConsumer for the requested render type in the callback.
        event.getSubmitNodeCollector().submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vc) -> {
            renderCubeOutline(vc, pose, center, radius);
            renderSurfaceContour(vc, pose, level, center, radius);
        });

        poseStack.popPose();
    }

    private static void renderCubeOutline(VertexConsumer vc, PoseStack.Pose pose, BlockPos center, int radius) {
        float minX = center.getX() - radius;
        float minY = center.getY() - radius;
        float minZ = center.getZ() - radius;
        float maxX = center.getX() + radius + 1;
        float maxY = center.getY() + radius + 1;
        float maxZ = center.getZ() + radius + 1;

        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, minY, minZ, maxX, minY, minZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, minY, maxZ, maxX, minY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, minY, minZ, minX, minY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, maxY, maxZ, maxX, maxY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, maxY, minZ, minX, maxY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, minY, minZ, minX, maxY, minZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, minX, minY, maxZ, minX, maxY, maxZ);
        drawLine(vc, pose, LINE_R, LINE_G, LINE_B, LINE_A, maxX, minY, maxZ, maxX, maxY, maxZ);
    }

    private static void renderSurfaceContour(VertexConsumer vc, PoseStack.Pose pose, ClientLevel level,
                                              BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;

        contourAlongX(vc, pose, level, minX, maxX, minZ, minY, maxY, minZ);
        contourAlongX(vc, pose, level, minX, maxX, maxZ, minY, maxY, maxZ + 1);
        contourAlongZ(vc, pose, level, minZ, maxZ, minX, minY, maxY, minX);
        contourAlongZ(vc, pose, level, minZ, maxZ, maxX, minY, maxY, maxX + 1);
    }

    private static void contourAlongX(VertexConsumer vc, PoseStack.Pose pose, ClientLevel level,
                                       int xStart, int xEnd, int sampleZ, int minY, int maxY, float drawZ) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        float prevX = 0, prevY = 0;
        boolean hasPrev = false;
        for (int x = xStart; x <= xEnd; x++) {
            int topY = findTopmostSolid(level, mut, x, sampleZ, minY, maxY);
            if (topY == Integer.MIN_VALUE) { hasPrev = false; continue; }
            float currX = x + 0.5f;
            float currY = topY + 1.005f;
            if (hasPrev) {
                drawLine(vc, pose, CONTOUR_R, CONTOUR_G, CONTOUR_B, CONTOUR_A,
                    prevX, prevY, drawZ, currX, currY, drawZ);
            }
            prevX = currX; prevY = currY; hasPrev = true;
        }
    }

    private static void contourAlongZ(VertexConsumer vc, PoseStack.Pose pose, ClientLevel level,
                                       int zStart, int zEnd, int sampleX, int minY, int maxY, float drawX) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        float prevZ = 0, prevY = 0;
        boolean hasPrev = false;
        for (int z = zStart; z <= zEnd; z++) {
            int topY = findTopmostSolid(level, mut, sampleX, z, minY, maxY);
            if (topY == Integer.MIN_VALUE) { hasPrev = false; continue; }
            float currZ = z + 0.5f;
            float currY = topY + 1.005f;
            if (hasPrev) {
                drawLine(vc, pose, CONTOUR_R, CONTOUR_G, CONTOUR_B, CONTOUR_A,
                    drawX, prevY, prevZ, drawX, currY, currZ);
            }
            prevZ = currZ; prevY = currY; hasPrev = true;
        }
    }

    private static int findTopmostSolid(ClientLevel level, BlockPos.MutableBlockPos mut,
                                         int x, int z, int minY, int maxY) {
        for (int y = maxY; y >= minY; y--) {
            mut.set(x, y, z);
            if (!level.getBlockState(mut).isAir()) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static void drawLine(VertexConsumer vc, PoseStack.Pose pose,
                                  float r, float g, float b, float a,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2) {
        float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        float nx = dx / len, ny = dy / len, nz = dz / len;
        vc.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
        vc.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(LINE_WIDTH);
    }
}
