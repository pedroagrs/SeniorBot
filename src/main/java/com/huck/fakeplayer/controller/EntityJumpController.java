package com.huck.fakeplayer.controller;

import com.huck.fakeplayer.main.FakePlayerPlugin;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

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

    public EntityJumpController(Object entityPlayer) {
        this.entityPlayer = entityPlayer;

        runTaskTimer(FakePlayerPlugin.getPlugin(), 0L, 1L);
    }

    public void jump() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        entityPlayer.getClass().getMethod("move", double.class, double.class, double.class).invoke(entityPlayer,
                0, 0.41999998688, 0);

        calcToHighest += 0.41999998688;
        calcToGravity = gravity;

        isJumping = true;
    }

    @SneakyThrows
    @Override
    public void run() {
        double highest = 1.24918707874468;
        if (entityPlayer.getClass().getField("onGround").getBoolean(entityPlayer) && isHighest) {
            // reset jump

            setJumping(false);
            setHighest(false);

            calcToGravity = gravity;
            calcToHighest = 0.0d;

        } else if (isHighest && isJumping) {

            // gravity
            calcToGravity += 0.08;

            entityPlayer.getClass().getMethod("move", double.class, double.class, double.class)
                    .invoke(entityPlayer, 0, gravity - calcToGravity, 0);
        } else if (calcToHighest < highest && isJumping) {

            // jump
            double jump = 0.24983741574;
            calcToHighest += jump;

            if (calcToHighest < highest)
                entityPlayer.getClass().getMethod("move", double.class, double.class, double.class)
                        .invoke(entityPlayer, 0, jump, 0);

        } else if (calcToHighest >= highest) {
            // prepare to gravity

            isHighest = true;

        }
    }
}
