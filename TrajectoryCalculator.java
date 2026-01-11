package org.lxveyanx.trajectorymod.client;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryCalculator {

    public static class Result {
        public List<Vec3d> points;
        public HitResult finalHit;
        public boolean isEntityHit;

        public Result(List<Vec3d> points, HitResult finalHit, boolean isEntityHit) {
            this.points = points;
            this.finalHit = finalHit;
            this.isEntityHit = isEntityHit;
        }
    }

    public static Result calculatePath(PlayerEntity player, ItemStack stack, float offsetDegrees, float tickDelta) {
        List<Vec3d> points = new ArrayList<>();
        World world = player.getWorld();

        Vec3d pos = player.getCameraPosVec(tickDelta);

        // --- Направление ---
        float yaw = player.getYaw(tickDelta);
        float pitch = player.getPitch(tickDelta);

        Quaternionf playerRotation = new Quaternionf()
                .rotationY((float) Math.toRadians(-yaw))
                .rotateX((float) Math.toRadians(pitch));

        Quaternionf spreadRotation = new Quaternionf()
                .rotationY((float) Math.toRadians(-offsetDegrees));

        Vector3f dir = new Vector3f(0, 0, 1);
        dir.rotate(spreadRotation);
        dir.rotate(playerRotation);

        Vec3d velocity = new Vec3d(dir).normalize().multiply(getInitialSpeed(player, stack));

        if (isThrowable(stack.getItem())) {
            Vec3d playerVelocity = player.getVelocity();
            velocity = velocity.add(playerVelocity.x, player.isOnGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
        }

        double gravity = getGravity(stack);
        double drag = 0.99;

        // Чуть увеличили хитбокс снаряда для удобства
        float projectileMargin = 0.3f;

        points.add(pos);
        HitResult finalHit = null;
        boolean entityHitFlag = false;

        // --- Симуляция ---
        for (int i = 0; i < 150; i++) {
            Vec3d nextPos = pos.add(velocity);

            // 1. Сначала проверяем БЛОК (Стену)
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    pos, nextPos, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, player
            ));

            // 2. Определяем точку, до которой ищем энтити
            // Если попали в стену - ищем только ДО стены. Если нет - до nextPos.
            Vec3d searchEnd = (blockHit.getType() != HitResult.Type.MISS) ? blockHit.getPos() : nextPos;

            // 3. Проверяем ЭНТИТИ на отрезке [pos -> searchEnd]
            EntityHitResult entityHit = ProjectileUtil.raycast(
                    player,
                    pos,
                    searchEnd,
                    new Box(pos, searchEnd).expand(1.0),
                    (entity) -> !entity.isSpectator() && entity.canHit(),
                    projectileMargin
            );

            // 4. Логика выбора попадания
            if (entityHit != null) {
                // Если ProjectileUtil что-то нашел на этом урезанном отрезке,
                // значит, энтити ГАРАНТИРОВАННО перед стеной.
                points.add(entityHit.getPos());
                finalHit = entityHit;
                entityHitFlag = true;
                break;
            } else if (blockHit.getType() != HitResult.Type.MISS) {
                // Энтити нет, но есть стена
                points.add(blockHit.getPos());
                finalHit = blockHit;
                break;
            }

            points.add(nextPos);
            pos = nextPos;
            velocity = velocity.multiply(drag).subtract(0, gravity, 0);
        }

        return new Result(points, finalHit, entityHitFlag);
    }

    private static boolean isThrowable(Item item) {
        return item instanceof SnowballItem || item instanceof EnderPearlItem || item instanceof EggItem;
    }

    private static float getInitialSpeed(PlayerEntity player, ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            float pull = BowItem.getPullProgress(player.getItemUseTime());
            return pull * 3.0f;
        }
        if (stack.getItem() instanceof CrossbowItem) return 3.15f;
        if (stack.getItem() instanceof TridentItem) return 2.5f;
        if (stack.getItem() instanceof SnowballItem || stack.getItem() instanceof EnderPearlItem) return 1.5f;
        return 1.5f;
    }

    private static double getGravity(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem) return 0.05;
        return 0.03;
    }
}