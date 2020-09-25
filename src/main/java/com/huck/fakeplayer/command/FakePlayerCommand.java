package com.huck.fakeplayer.command;

import com.huck.fakeplayer.main.FakePlayerPlugin;
import com.huck.fakeplayer.controller.EntityMoveController;
import com.huck.fakeplayer.utils.NmsUtils;
import com.mojang.authlib.GameProfile;
import lombok.SneakyThrows;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class FakePlayerCommand implements CommandExecutor {

    private final FakePlayerPlugin plugin = FakePlayerPlugin.getPlugin();
    private Location location;

    public FakePlayerCommand() {
        final FileConfiguration config = plugin.getConfig();
        final String path = "location.";

        double x = config.getDouble(path + "x");
        double y = config.getDouble(path + "y");
        double z = config.getDouble(path + "z");
        float yaw = (float) config.getDouble(path + "yaw");
        float pitch = (float) config.getDouble(path + "pitch");

        if (y == -1337) this.location = null;
        else this.location = new Location(Bukkit.getWorld(config.getString(path + "world")), x, y, z, yaw, pitch);


        plugin.getCommand("fakeplayer").setExecutor(this);
    }

    @SneakyThrows
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;

        if (args.length == 0) {
            Object fake = null;

            try {
                if (location == null) {
                    player.sendMessage("§cUse /fakeplayer setlocation - Set npc spawn location");
                    fake = initSpawn(player.getLocation());
                } else {
                    fake = initSpawn(location);
                }

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                e.printStackTrace();
            }

            if (fake != null) {
                fake.getClass().getMethod("reset").invoke(fake);

                new EntityMoveController(fake);
            }

        } else {
            final String arg = args[0].toLowerCase();

            if (arg.equals("setlocation")) {
                this.location = player.getLocation();

                final FileConfiguration config = plugin.getConfig();
                final String path = "location.";

                config.set(path + "x", location.getX());
                config.set(path + "y", location.getY());
                config.set(path + "z", location.getZ());
                config.set(path + "yaw", location.getYaw());
                config.set(path + "pitch", location.getPitch());
                config.set(path + "world", location.getWorld().getName());

                plugin.saveConfig();

                player.sendMessage("§aLocation save.");
            }
        }

        return false;
    }

    // NMS IMPORTS
    private final Class<?> minecraftServerClass = NmsUtils.getNMSClass("MinecraftServer");
    private final Class<?> worldServerClass = NmsUtils.getNMSClass("World");
    private final Class<?> entityPlayerClass = NmsUtils.getNMSClass("EntityPlayer");
    private final Class<?> entityHuman = NmsUtils.getNMSClass("EntityHuman");
    private final Class<?> entityClass = NmsUtils.getNMSClass("Entity");
    private final Class<?> interactClass = NmsUtils.getNMSClass("PlayerInteractManager");
    private final Class<?> connectionClass = NmsUtils.getNMSClass("PlayerConnection");
    private final Class<?> networkClass = NmsUtils.getNMSClass("NetworkManager");
    private final Class<?> enumProtocolDirection = NmsUtils.getNMSClass("EnumProtocolDirection");
    private final Class<?> enumPlayerInfoClass = NmsUtils.getNMSClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");
    private final Class<?> namedEntityClass = NmsUtils.getNMSClass("PacketPlayOutNamedEntitySpawn");
    private final Class<?> headRotationClass = NmsUtils.getNMSClass("PacketPlayOutEntityHeadRotation");
    private final Class<?> playerInfo = NmsUtils.getNMSClass("PacketPlayOutPlayerInfo");
    private final GameProfile profile = new GameProfile(UUID.randomUUID(), "Bot");

    protected Object initSpawn(Location location) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        final Object nmsServer = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
        final Object nmsWorld = location.getWorld().getClass().getMethod("getHandle").invoke(location.getWorld());

        assert entityPlayerClass != null;
        Constructor<?> entityConstructor = entityPlayerClass.getConstructor(
                NmsUtils.getNMSClass("MinecraftServer"),
                NmsUtils.getNMSClass("WorldServer"),
                GameProfile.class,
                interactClass
        );

        assert interactClass != null;
        Constructor<?> interactConstructor = interactClass.getConstructor(worldServerClass);

        final Object entityPlayer = entityConstructor.newInstance(nmsServer, nmsWorld, profile, interactConstructor.newInstance(nmsWorld));

        entityPlayerClass.getMethod("setLocation", double.class, double.class, double.class, float.class, float.class)
                .invoke(entityPlayer, location.getX(), location.getY(), location.getZ(), location.getYaw(),
                        location.getPitch());

        entityPlayerClass.getField("noclip").setAccessible(true);
        entityPlayerClass.getField("noclip").setBoolean(entityPlayer, false);

        assert enumProtocolDirection != null;
        enumProtocolDirection.getField("SERVERBOUND").setAccessible(true);
        final Object serverbound = enumProtocolDirection.getField("SERVERBOUND").get(enumProtocolDirection);

        assert connectionClass != null;
        assert networkClass != null;
        Object playerConnection = connectionClass
                .getConstructor(minecraftServerClass, networkClass, entityPlayerClass)
                .newInstance(nmsServer, networkClass.getConstructor(enumProtocolDirection).newInstance(serverbound),
                        entityPlayer);

        entityPlayerClass.getField("playerConnection").set(entityPlayer, playerConnection);

        assert worldServerClass != null;
        worldServerClass.getMethod("addEntity", entityClass).invoke(nmsWorld, entityPlayer);

        assert enumPlayerInfoClass != null;
        final Object add = enumPlayerInfoClass.getField("ADD_PLAYER").get(enumPlayerInfoClass);
        final Object remove = enumPlayerInfoClass.getField("REMOVE_PLAYER").get(enumPlayerInfoClass);

        Object array = java.lang.reflect.Array.newInstance(entityPlayerClass, 1);
        Array.set(array, 0, entityPlayer);

        assert playerInfo != null;
        Object playerInfoAdd = playerInfo.getConstructor(enumPlayerInfoClass,
                Array.newInstance(entityPlayerClass, 0).getClass())
                .newInstance(add, array);

        assert namedEntityClass != null;
        Object namedEntitySpawn = namedEntityClass.getConstructor(entityHuman)
                .newInstance(entityPlayer);

        assert headRotationClass != null;
        Object headPosition = headRotationClass.getConstructor(entityClass, byte.class)
                .newInstance(entityPlayer, (byte) ((location.getYaw() * 256f) / 360f));

        Object playerInfoRemove = playerInfo.getConstructor(enumPlayerInfoClass,
                Array.newInstance(entityPlayerClass, 0).getClass())
                .newInstance(remove, array);

        Bukkit.getOnlinePlayers().forEach(player -> {
            NmsUtils.sendPacket(playerInfoAdd, player);
            NmsUtils.sendPacket(namedEntitySpawn, player);
            NmsUtils.sendPacket(headPosition, player);
            NmsUtils.sendPacket(playerInfoRemove, player);
        });

        return entityPlayer;
    }
}
