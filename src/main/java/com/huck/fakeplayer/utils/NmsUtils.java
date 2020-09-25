package com.huck.fakeplayer.utils;

import com.google.common.collect.Maps;
import com.huck.fakeplayer.main.FakePlayerPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class NmsUtils {

    private static FakePlayerPlugin plugin = FakePlayerPlugin.getPlugin();

    public static int getFreeEntityId() {
        try {
            Class<?> entityClass = getNMSClass("Entity");
            assert entityClass != null;
            Field entityCountField = entityClass.getDeclaredField("entityCount");
            entityCountField.setAccessible(true);
            if (entityCountField.getType() == int.class) {
                int id = entityCountField.getInt(null);
                entityCountField.setInt(null, id + 1);
                return id;
            } else if (entityCountField.getType() == AtomicInteger.class) {
                return ((AtomicInteger) entityCountField.get(null)).incrementAndGet();
            }

            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean sendPacket(Object packet, Player player) {
        try {
            if (packet == null) {
                plugin.getLogger().warning("Failed to send packet: Packet is null");
                return false;
            }

            Class<?> packetClass = getNMSClass("Packet");
            if (packetClass == null) {
                plugin.getLogger().warning("Failed to send packet: Could not find Packet class");
                return false;
            }

            Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = nmsPlayer.getClass().getField("playerConnection").get(nmsPlayer);


            playerConnection.getClass().getMethod("sendPacket", packetClass).invoke(playerConnection, packet);

            return true;
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
            plugin.getLogger().severe("Failed to send packet " + packet.getClass().getName());
            return false;
        }
    }


    public static Class<?> getNMSClass(String className) {
        try {
            return Class.forName("net.minecraft.server." + getServerVersion() + "." + className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Class<?> getCraftBukkitClass(String className) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + getServerVersion() + "." + className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static String getServerVersion() {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();

        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }


    public static int getMajorVersion() {
        return Integer.parseInt(getServerVersion().split("_")[1]);
    }

    public static int getRevision() {
        return Integer.parseInt(getServerVersion().substring(getServerVersion().length() - 1));
    }
}
