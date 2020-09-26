package com.huck.fakeplayer.controller;

import com.huck.fakeplayer.main.FakePlayerPlugin;
import com.huck.fakeplayer.utils.NmsUtils;
import lombok.Getter;
import lombok.SneakyThrows;

import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Getter
public class EntityMoveController extends BukkitRunnable {

    private final Object player;
    private int step;

    private final EntityJumpController jumpController;
    private final boolean isLastVersions;

    private final String version;

    public EntityMoveController(Object player, String version) {
        this.player = player;
        this.step = 0;
        this.version = version;
        this.isLastVersions = version.startsWith("v1_14") || version.startsWith("v1_15") || version.startsWith("v1_16");

        this.jumpController = new EntityJumpController(player, version);

        runTaskTimer(FakePlayerPlugin.getPlugin(), 4L, 20L);
    }

    public void nextStep() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, InstantiationException {

        if (jumpController.isJumping()) return;

        switch (step) {
            case 1:
            case 2:
                jumpController.jump();
                break;
            case 3:
            case 5:
                player.getClass().getMethod("setSneaking", boolean.class).invoke(player, true);

                if (isLastVersions)
                    setSneakingLastVersions(true);

                break;
            case 6:
                if (version.startsWith("v1_8")) // MAIN_HAND
                    player.getClass().getMethod("bw").invoke(player);
                else { // MAIN_HAND / OFF_HAND
                    final Class<?> enumHandClass = NmsUtils.getNMSClass("EnumHand");

                    assert enumHandClass != null;
                    final Object mainHand = enumHandClass.getEnumConstants()[0];

                    if (version.startsWith("v1_16"))
                        player.getClass().getMethod("swingHand", enumHandClass).invoke(player, mainHand);
                    else
                        player.getClass().getMethod("a", enumHandClass).invoke(player, mainHand);
                }
                break;
            case 4:
            case 7:
                player.getClass().getMethod("setSneaking", boolean.class).invoke(player, false);

                if (isLastVersions)
                    setSneakingLastVersions(false);
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

    private final Class<?> entityHumanClass = NmsUtils.getNMSClass("Entity");
    private final Class<?> entityPoseClass = NmsUtils.getNMSClass("EntityPose");

    protected void setSneakingLastVersions(boolean flag) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method;

        int toggle = 0;

        if (flag) toggle = 5;

        assert entityHumanClass != null;
        if (version.startsWith("v1_14")) {
            method = entityHumanClass.getDeclaredMethod("b", entityPoseClass);
        } else {
            method = entityHumanClass.getDeclaredMethod("setPose", entityPoseClass);
        }

        method.setAccessible(true);

        assert entityPoseClass != null;
        method.invoke(player, entityPoseClass.getEnumConstants()[toggle]);
    }
}
