package com.huck.fakeplayer.controller;

import com.huck.fakeplayer.main.FakePlayerPlugin;
import lombok.Getter;
import lombok.SneakyThrows;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;

@Getter
public class EntityMoveController extends BukkitRunnable {

    private final Object player;
    private int step;

    private final EntityJumpController jumpController;

    public EntityMoveController(Object player) {
        this.player = player;
        this.step = 0;
        this.jumpController = new EntityJumpController(player);

        runTaskTimer(FakePlayerPlugin.getPlugin(), 4L, 20L);
    }

    public void nextStep() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (jumpController.isJumping()) return;

        switch (step) {
            case 1:
            case 2:
                jumpController.jump();
                break;
            case 3:
            case 5:
                player.getClass().getMethod("setSneaking", boolean.class).invoke(player, true);
                break;
            case 6:
                player.getClass().getMethod("bw").invoke(player);
                break;
            case 4:
            case 7:
                player.getClass().getMethod("setSneaking", boolean.class).invoke(player, false);
                break;
        }

        if (step == 7) {
            this.jumpController.cancel();
            cancel();
        }

        step++;
    }

    @SneakyThrows
    @Override
    public void run() {
        nextStep();
    }
}
