package org.lxveyanx.trajectorymod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryRenderer {

    private static final RenderLayer NEON_GLOW = RenderLayer.of(
            "trajectory_neon",
            VertexFormats.POSITION_COLOR,
            VertexFormat.DrawMode.TRIANGLES,
            256,
            false,
            true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.ALWAYS_DEPTH_TEST)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .build(false)
    );

    public static void init() {
        WorldRenderEvents.LAST.register(TrajectoryRenderer::render);
    }

    private static void render(WorldRenderContext context) {
        TrajectoryConfig config = TrajectoryConfig.get();
        if (!config.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden || !client.options.getPerspective().isFirstPerson()) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!shouldRenderFor(stack, client.player, config)) return;

        float tickDelta = client.getRenderTickCounter().getTickDelta(true);

        boolean hasMultishot = false;
        if (stack.getItem() instanceof CrossbowItem) {
            var registry = client.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var entry = registry.getOptional(Enchantments.MULTISHOT).orElse(null);
            if (entry != null && EnchantmentHelper.getLevel(entry, stack) > 0) {
                hasMultishot = true;
            }
        }

        List<PathContext> pathsToRender = new ArrayList<>();
        if (hasMultishot) {
            pathsToRender.add(new PathContext(TrajectoryCalculator.calculatePath(client.player, stack, -10, tickDelta), 0.5f));
            pathsToRender.add(new PathContext(TrajectoryCalculator.calculatePath(client.player, stack, 10, tickDelta), 0.5f));
            pathsToRender.add(new PathContext(TrajectoryCalculator.calculatePath(client.player, stack, 0, tickDelta), 1.0f));
        } else {
            pathsToRender.add(new PathContext(TrajectoryCalculator.calculatePath(client.player, stack, 0, tickDelta), 1.0f));
        }

        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getPos();
        VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buffer = immediate.getBuffer(NEON_GLOW);

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        Quaternionf cameraRotation = context.camera().getRotation();
        float animationTime = (System.currentTimeMillis() % 100000) / 1000.0f; // Увеличил цикл времени

        for (PathContext ctx : pathsToRender) {
            boolean hitEntity = ctx.result.isEntityHit;

            renderNeonPath(buffer, matrices, ctx.result.points, cameraRotation, ctx.alpha, hitEntity, animationTime, config);

            if (ctx.result.finalHit != null) {
                Direction side;
                Vec3d hitPos = ctx.result.finalHit.getPos();

                if (ctx.result.finalHit instanceof BlockHitResult blockHit) {
                    side = blockHit.getSide();
                } else if (ctx.result.finalHit instanceof EntityHitResult entityHit) {
                    side = getEntityHitSide(entityHit.getEntity(), hitPos);
                } else {
                    side = Direction.UP;
                }

                // Передаем cameraPos для расчета дистанции
                renderCyberMarker(buffer, matrices.peek().getPositionMatrix(), hitPos, side, hitEntity, animationTime, config, cameraPos);
            }
        }

        matrices.pop();
        immediate.draw(NEON_GLOW);
    }

    private record PathContext(TrajectoryCalculator.Result result, float alpha) {}

    private static void renderNeonPath(VertexConsumer buffer, MatrixStack matrices, List<Vec3d> points, Quaternionf cameraRotation, float alphaMult, boolean hitEntity, float time, TrajectoryConfig config) {
        if (points.isEmpty()) return;

        int cStart = config.colorStart;
        int cEnd = config.colorEnd;
        int r1 = (cStart >> 16) & 0xFF; int g1 = (cStart >> 8) & 0xFF; int b1 = cStart & 0xFF;
        int r2 = (cEnd >> 16) & 0xFF;   int g2 = (cEnd >> 8) & 0xFF;   int b2 = cEnd & 0xFF;

        int hitColor = config.markerColorEntity;
        int rh = (hitColor >> 16) & 0xFF; int gh = (hitColor >> 8) & 0xFF; int bh = hitColor & 0xFF;

        for (int i = 4; i < points.size(); i++) {
            if (i % 2 != 0) continue;

            Vec3d point = points.get(i);
            float ratio = (float) i / points.size();

            float waveSpeed = 3.0f;
            float waveOffset = (time * waveSpeed) % 1.0f;
            float dist = Math.abs(ratio - waveOffset);
            if (dist > 0.5f) dist = 1.0f - dist;
            float pulseFactor = Math.max(0, 1.0f - (dist * 5.0f));

            int r, g, b;
            if (hitEntity) {
                r = Math.min(255, (int)(255 * (1 - ratio) + rh * ratio) + (int)(50 * pulseFactor));
                g = Math.min(255, (int)(50 * (1 - ratio) + gh * ratio));
                b = Math.min(255, (int)(0 * (1 - ratio) + bh * ratio));
            } else {
                r = Math.min(255, (int)(r1 * (1 - ratio) + r2 * ratio) + (int)(50 * pulseFactor));
                g = Math.min(255, (int)(g1 * (1 - ratio) + g2 * ratio) + (int)(50 * pulseFactor));
                b = Math.min(255, (int)(b1 * (1 - ratio) + b2 * ratio) + (int)(50 * pulseFactor));
            }

            float baseSize = 0.05f * (1.0f - (ratio * 0.2f));
            float size = baseSize + (0.02f * pulseFactor);

            matrices.push();
            matrices.translate(point.x, point.y, point.z);
            matrices.multiply(cameraRotation);
            Matrix4f matrix = matrices.peek().getPositionMatrix();

            renderCircle(buffer, matrix, 0, 0, 0, size * 2.0f, r, g, b, (int)(60 * alphaMult));

            int coreR = Math.min(255, r + 150);
            int coreG = Math.min(255, g + 150);
            int coreB = Math.min(255, b + 150);
            renderCircle(buffer, matrix, 0, 0, 0, size * 0.6f, coreR, coreG, coreB, 255);

            matrices.pop();
        }
    }

    private static void renderCyberMarker(VertexConsumer buffer, Matrix4f matrix, Vec3d pos, Direction side, boolean hitEntity, float time, TrajectoryConfig config, Vec3d cameraPos) {
        int color = hitEntity ? config.markerColorEntity : config.markerColorBlock;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        double off = 0.02;

        // --- ХАОТИЧНОЕ ВРАЩЕНИЕ ---
        // Комбинируем Sin и Cos с разными частотами, чтобы получить "рваное" движение
        float chaos = (float) (Math.sin(time * 1.5) + Math.cos(time * 2.7) * 0.8);
        float rotation = chaos * 2.5f;

        // --- МАСШТАБИРОВАНИЕ (Proximity Scale) ---
        // Чем ближе к игроку, тем меньше маркер.
        double distance = pos.distanceTo(cameraPos);
        float scale = (float) MathHelper.clamp(distance / 4.0, 0.2, 1.0); // Min 0.2 (близко), Max 1.0 (далеко)

        double cx = pos.x; double cy = pos.y; double cz = pos.z;
        Vec3d u, v;

        switch (side) {
            case UP:    cy += off; u = new Vec3d(1, 0, 0); v = new Vec3d(0, 0, 1); break;
            case DOWN:  cy -= off; u = new Vec3d(1, 0, 0); v = new Vec3d(0, 0, 1); break;
            case NORTH: cz -= off; u = new Vec3d(1, 0, 0); v = new Vec3d(0, 1, 0); break;
            case SOUTH: cz += off; u = new Vec3d(1, 0, 0); v = new Vec3d(0, 1, 0); break;
            case EAST:  cx += off; u = new Vec3d(0, 1, 0); v = new Vec3d(0, 0, 1); break;
            case WEST:  cx -= off; u = new Vec3d(0, 1, 0); v = new Vec3d(0, 0, 1); break;
            default: return;
        }

        // УБРАЛ ТЕНЬ (RenderFlatCircle с черным цветом удален)

        // Ядро (Центральная точка)
        float pulse = (float) Math.sin(time * 10.0f) * 0.5f + 0.5f;
        float centerSize = (0.15f + (0.05f * pulse)) * scale; // Применяем scale

        renderFlatCircle(buffer, matrix, cx, cy, cz, u, v, centerSize, r, g, b, 255);
        renderFlatCircle(buffer, matrix, cx, cy, cz, u, v, centerSize * 1.5f, r, g, b, 100);

        // Кольцо (Одно, хаотичное)
        float ringRadius = 0.45f * scale; // Применяем scale
        int segments = 3;
        float arcSize = (float) Math.PI * 2 / segments * 0.25f; // Дуги чуть короче

        for (int i = 0; i < segments; i++) {
            float angleBase = rotation + ((float) Math.PI * 2 * i / segments);
            // Яркая часть дуги
            renderArc(buffer, matrix, cx, cy, cz, u, v, ringRadius, 0.04f * scale, angleBase, angleBase + arcSize, r, g, b, 200);
            // Полупрозрачный "хвост" дуги (для красоты)
            renderArc(buffer, matrix, cx, cy, cz, u, v, ringRadius, 0.04f * scale, angleBase - 0.2f, angleBase, r, g, b, 50);
        }
    }

    private static void renderCircle(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, float radius, int r, int g, int b, int a) {
        int segments = 8;
        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;
            float x1 = x + (float)Math.cos(angle1) * radius;
            float y1 = y + (float)Math.sin(angle1) * radius;
            float x2 = x + (float)Math.cos(angle2) * radius;
            float y2 = y + (float)Math.sin(angle2) * radius;
            buffer.vertex(matrix, x, y, z).color(r, g, b, a);
            buffer.vertex(matrix, x1, y1, z).color(r, g, b, a);
            buffer.vertex(matrix, x2, y2, z).color(r, g, b, a);
        }
    }

    private static void renderFlatCircle(VertexConsumer buffer, Matrix4f matrix, double cx, double cy, double cz, Vec3d u, Vec3d v, float radius, int r, int g, int b, int a) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            double angle1 = (2 * Math.PI * i) / segments;
            double angle2 = (2 * Math.PI * (i + 1)) / segments;
            Vec3d p1 = getVec(cx, cy, cz, u, v, radius, angle1);
            Vec3d p2 = getVec(cx, cy, cz, u, v, radius, angle2);
            buffer.vertex(matrix, (float)cx, (float)cy, (float)cz).color(r, g, b, a);
            buffer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, a);
        }
    }

    private static void renderArc(VertexConsumer buffer, Matrix4f matrix, double cx, double cy, double cz, Vec3d u, Vec3d v, float radius, float width, float startAngle, float endAngle, int r, int g, int b, int a) {
        int segments = 6;
        for (int i = 0; i < segments; i++) {
            float t1 = (float)i / segments;
            float t2 = (float)(i + 1) / segments;
            double ang1 = startAngle + (endAngle - startAngle) * t1;
            double ang2 = startAngle + (endAngle - startAngle) * t2;
            Vec3d p1_in = getVec(cx, cy, cz, u, v, radius - width, ang1);
            Vec3d p2_in = getVec(cx, cy, cz, u, v, radius - width, ang2);
            Vec3d p1_out = getVec(cx, cy, cz, u, v, radius + width, ang1);
            Vec3d p2_out = getVec(cx, cy, cz, u, v, radius + width, ang2);
            buffer.vertex(matrix, (float)p1_in.x, (float)p1_in.y, (float)p1_in.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p1_out.x, (float)p1_out.y, (float)p1_out.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p2_out.x, (float)p2_out.y, (float)p2_out.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p1_in.x, (float)p1_in.y, (float)p1_in.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p2_out.x, (float)p2_out.y, (float)p2_out.z).color(r, g, b, a);
            buffer.vertex(matrix, (float)p2_in.x, (float)p2_in.y, (float)p2_in.z).color(r, g, b, a);
        }
    }

    private static Vec3d getVec(double cx, double cy, double cz, Vec3d u, Vec3d v, double radius, double angle) {
        return new Vec3d(
                cx + (u.x * Math.cos(angle) + v.x * Math.sin(angle)) * radius,
                cy + (u.y * Math.cos(angle) + v.y * Math.sin(angle)) * radius,
                cz + (u.z * Math.cos(angle) + v.z * Math.sin(angle)) * radius
        );
    }

    private static Direction getEntityHitSide(Entity entity, Vec3d hitPos) {
        Box box = entity.getBoundingBox();
        double minDist = Double.MAX_VALUE;
        Direction closest = Direction.UP;
        double dYUp = Math.abs(box.maxY - hitPos.y); if (dYUp < minDist) { minDist = dYUp; closest = Direction.UP; }
        double dYDown = Math.abs(box.minY - hitPos.y); if (dYDown < minDist) { minDist = dYDown; closest = Direction.DOWN; }
        double dXEast = Math.abs(box.maxX - hitPos.x); if (dXEast < minDist) { minDist = dXEast; closest = Direction.EAST; }
        double dXWest = Math.abs(box.minX - hitPos.x); if (dXWest < minDist) { minDist = dXWest; closest = Direction.WEST; }
        double dZSouth = Math.abs(box.maxZ - hitPos.z); if (dZSouth < minDist) { minDist = dZSouth; closest = Direction.SOUTH; }
        double dZNorth = Math.abs(box.minZ - hitPos.z); if (dZNorth < minDist) { minDist = dZNorth; closest = Direction.NORTH; }
        return closest;
    }

    private static boolean shouldRenderFor(ItemStack stack, net.minecraft.entity.player.PlayerEntity player, TrajectoryConfig config) {
        Item item = stack.getItem();
        if (item instanceof TridentItem) return config.showTrident && player.isUsingItem();
        if (item instanceof BowItem) return config.showOthers && player.isUsingItem();
        if (item instanceof CrossbowItem) return config.showCrossbow && CrossbowItem.isCharged(stack);
        if (item instanceof SnowballItem || item instanceof EnderPearlItem) return config.showOthers;
        return false;
    }
}