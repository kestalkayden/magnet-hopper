package com.kestalkayden.magnethopper.client;

import com.kestalkayden.magnethopper.block.MagnetHopperBlock;
import com.kestalkayden.magnethopper.block.MagnetHopperBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side placement preview: when the player holds a Magnet Hopper, draws
 *   (1) the cube wireframe of the magnet's reach (visible above ground), and
 *   (2) a terrain-following contour along the 4 footprint perimeter edges so
 *       the bottom of the area is always legible even when terrain hides the
 *       cube's lower edges.
 */
public final class MagnetHopperFieldRenderer {

    // Cube wireframe — cyan-mint
    private static final float LINE_R = 0.30f;
    private static final float LINE_G = 1.00f;
    private static final float LINE_B = 0.70f;
    private static final float LINE_A = 0.90f;

    // Surface contour — slightly warmer green so it reads distinct from the cube edges
    private static final float CONTOUR_R = 0.70f;
    private static final float CONTOUR_G = 1.00f;
    private static final float CONTOUR_B = 0.30f;
    private static final float CONTOUR_A = 1.00f;

    private static final float LINE_WIDTH = 2.0f;

    private MagnetHopperFieldRenderer() {}

    public static void register() {
        LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(MagnetHopperFieldRenderer::onAfterTranslucentFeatures);
    }

    private static void onAfterTranslucentFeatures(LevelRenderContext ctx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        int radius = getHeldRadius(mc.player);
        if (radius <= 0) return;

        HitResult hit = mc.hitResult;
        if (!(hit instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return;

        BlockPos placePos = bhr.getBlockPos().relative(bhr.getDirection());
        render(ctx, mc.level, placePos, radius);
    }

    /** Returns the tier-specific radius of the magnet hopper the player is holding, or 0/-1 if none. */
    private static int getHeldRadius(Player player) {
        int r = getRadiusForItem(player.getMainHandItem());
        if (r > 0) return r;
        return getRadiusForItem(player.getOffhandItem());
    }

    private static int getRadiusForItem(ItemStack stack) {
        if (stack.is(MagnetHopperBlocks.MAGNET_HOPPER_ITEM))            return MagnetHopperBlocks.MAGNET_HOPPER.getTier().radius;
        if (stack.is(MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER_ITEM))   return MagnetHopperBlocks.ADVANCED_MAGNET_HOPPER.getTier().radius;
        if (stack.is(MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER_ITEM)) return MagnetHopperBlocks.INDUSTRIAL_MAGNET_HOPPER.getTier().radius;
        return 0;
    }

    private static void render(LevelRenderContext ctx, ClientLevel level, BlockPos center, int radius) {
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        PoseStack poseStack = ctx.poseStack();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer vc = ctx.bufferSource().getBuffer(RenderTypes.lines());
        PoseStack.Pose pose = poseStack.last();

        renderCubeOutline(vc, pose, center, radius);
        renderSurfaceContour(vc, pose, level, center, radius);

        poseStack.popPose();
        ctx.bufferSource().endBatch(RenderTypes.lines());
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

    /**
     * Walks the 4 perimeter edges of the footprint, sampling the topmost solid block in each
     * column along the way, and connects the sample points with segments. Result: a contour line
     * that adapts to terrain — flat ground gives a clean square; hills/valleys give a stepped contour.
     */
    private static void renderSurfaceContour(VertexConsumer vc, PoseStack.Pose pose, ClientLevel level,
                                              BlockPos center, int radius) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;

        // South edge: walking +X at z = minZ.       Line drawn at world z = minZ (the south face of cube).
        contourAlongX(vc, pose, level, minX, maxX, minZ, minY, maxY, minZ);
        // North edge: walking +X at z = maxZ.       Line drawn at world z = maxZ + 1 (the north face of cube).
        contourAlongX(vc, pose, level, minX, maxX, maxZ, minY, maxY, maxZ + 1);
        // West edge: walking +Z at x = minX.        Line drawn at world x = minX.
        contourAlongZ(vc, pose, level, minZ, maxZ, minX, minY, maxY, minX);
        // East edge: walking +Z at x = maxX.        Line drawn at world x = maxX + 1.
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
            // Draw at the center of the block in X (x + 0.5), at world Y of the top face (topY + 1)
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

    /** Returns the Y of the topmost solid (non-air) block at (x, z) within [minY, maxY], or Integer.MIN_VALUE. */
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
