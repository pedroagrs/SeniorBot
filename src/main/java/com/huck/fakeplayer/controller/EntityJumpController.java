package com.huck.fakeplayer.controller;

import com.huck.fakeplayer.main.FakePlayerPlugin;
import com.huck.fakeplayer.utils.NmsUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class EntityJumpController extends BukkitRunnable {

    private final Object entityPlayer;

    @Getter
    @Setter
    private boolean isJumping, isHighest;
    private double calcToHighest, calcToGravity;

    // Gravity stages:
    // 1 = 0.15523200451
    // 2 = 0.23052736891
    // 3 = 0.30431682745
    // 4 = 0.37663049823
    // 5 = 0.10408037809

    private final double gravity = -0.15523200451;
    private final boolean versionIsOld;
    private final String version;

    public EntityJumpController(Object entityPlayer, String version) {
        this.entityPlayer = entityPlayer;
        this.version = version;
        this.versionIsOld = version.startsWith("v1_8") || version.startsWith("v1_9") || version.startsWith("v1_10");

        runTaskTimer(FakePlayerPlugin.getPlugin(), 0L, 1L);
    }

    public void jump() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        if (versionIsOld)
            entityPlayer.getClass().getMethod("move", double.class, double.class, double.class).invoke(entityPlayer,
                    0, 0.41999998688, 0);
        else
            moveUpdated(0.41999998688);

        calcToHighest += 0.41999998688;
        calcToGravity = gravity;

        isJumping = true;
    }

    @SneakyThrows
    @Override
    public void run() {
        double highest = 1.24918707874468;

        boolean onGround = false;
        if (version.startsWith("v1_16"))
            onGround = (boolean) entityPlayer.getClass().getMethod("isOnGround").invoke(entityPlayer);
        else
            onGround = entityPlayer.getClass().getField("onGround").getBoolean(entityPlayer);

        if (onGround && isHighest) {
            // reset jump

            setJumping(false);
            setHighest(false);

            calcToGravity = gravity;
            calcToHighest = 0.0d;

        } else if (isHighest && isJumping) {

            // gravity
            calcToGravity += 0.08;

            if (versionIsOld)
                entityPlayer.getClass().getMethod("move", double.class, double.class, double.class)
                        .invoke(entityPlayer, 0, gravity - calcToGravity, 0);
            else
                moveUpdated(gravity - calcToGravity);

        } else if (calcToHighest < highest && isJumping) {

            // jump
            double jump = 0.24983741574;
            calcToHighest += jump;

            if (calcToHighest < highest)
                if (versionIsOld)
                    entityPlayer.getClass().getMethod("move", double.class, double.class, double.class)
                            .invoke(entityPlayer, 0, jump, 0);
                else
                    moveUpdated(jump);

        } else if (calcToHighest >= highest) {
            // prepare to gravity

            isHighest = true;

        }
    }

    protected void moveUpdated(double y) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {
        final Class<?> enumMoveTypeClass = NmsUtils.getNMSClass("EnumMoveType");
        assert enumMoveTypeClass != null;
        final Object move = enumMoveTypeClass.getEnumConstants()[0];

        if (version.startsWith("v1_14") || version.startsWith("v1_15") || version.startsWith("v1_16")) {
            final Class<?> vec3dClass = NmsUtils.getNMSClass("Vec3D");

            assert vec3dClass != null;
            final Object jumpVec3d = vec3dClass
                    .getConstructor(double.class, double.class, double.class)
                    .newInstance(0.0d, y, 0.0d);

            entityPlayer.getClass().getMethod("move", enumMoveTypeClass, vec3dClass)
                    .invoke(entityPlayer, move, jumpVec3d);

        } else entityPlayer.getClass().getMethod("move", enumMoveTypeClass, double.class, double.class, double.class)
                .invoke(entityPlayer, move, 0, y, 0);
    }
}
