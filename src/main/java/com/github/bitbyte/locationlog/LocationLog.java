package com.github.bitbyte.locationlog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

public final class LocationLog extends JavaPlugin {
    private int checkTimeTicks;
    int checkTimeHours = 0;
    int checkTimeMinutes = 0;
    int checkTimeSeconds = 0;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            checkTimeHours = 20 * 60 * 60 * getConfig().getInt("settings.checkhours");
        } catch (NumberFormatException | NullPointerException ex) {
            getLogger().info("Unable to utilize the set check hours. Defaulting to 0 hours.");
            getConfig().set("settings.checkhours", 0);
        }
        try {
            checkTimeMinutes = 20 * 60 * getConfig().getInt("settings.checkminutes");
        } catch (NumberFormatException | NullPointerException ex) {
            getLogger().info("Unable to utilize the set check minutes. Defaulting to 10 minutes.");
            getConfig().set("settings.checkminutes", 10);
        }
        try {
            checkTimeSeconds = 20 * getConfig().getInt("settings.checkseconds");
        } catch (NumberFormatException | NullPointerException ex) {
            getLogger().info("Unable to utilize the set check seconds. Defaulting to 0 seconds.");
            getConfig().set("settings.checkseconds", 0);
        }
        checkTimeTicks = checkTimeHours + checkTimeMinutes + checkTimeSeconds;
        if (checkTimeTicks <= 0) {
            getLogger().info("Check time setting is set to or below 0. Disabling Automatic logger.");
        }
        else {
            getLogger().info("Automatic logger is running every " + getConfig().getInt("settings.checkhours") + " hour(s), " + getConfig().getInt("settings.checkminutes") + " minute(s), " + getConfig().getInt("settings.checkseconds") + " second(s).");
            logger();
        }
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    private synchronized void logPlayerLocations() {
        List<String> players = getConfig().getStringList("specified-players");
        String listType = getConfig().getString("settings.listtype");
        boolean isBlacklist = listType.equalsIgnoreCase("blacklist");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            if ((isBlacklist && players.contains(playerUUID.toString())) || (!isBlacklist && !players.contains(playerUUID.toString()))) {
                continue;
            }
            if (player.hasPermission("locationlog.exempt")) {
                continue;
            }
            Location location = player.getLocation();
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();
            String logMessage = player.getName() + " is in the " + location.getWorld().getName() + " at X: " + String.format("%.2f", x) + ", Y: " + String.format("%.2f", y) + ", Z: " + String.format("%.2f", z);
            getLogger().info(logMessage);
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                if (p.hasPermission("locationlog.viewautolog")) {
                    p.sendPlainMessage(logMessage);
                }
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("loglocations")) {
            if (args.length == 0) {
                logPlayerLocations();
                sender.sendPlainMessage("Logged all players locations");
                return true;
            } else if (args.length == 1) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    Location location = targetPlayer.getLocation();
                    double x = location.getX();
                    double y = location.getY();
                    double z = location.getZ();
                    String playerLocationMsg = targetPlayer.getName() + " is in the " + location.getWorld().getName() + " at X: " + String.format("%.2f", x) + ", Y: " + String.format("%.2f", y) + ", Z: " + String.format("%.2f", z);
                    getLogger().info(playerLocationMsg);
                    if (sender.hasPermission("locationlog.viewlog")) {
                        sender.sendPlainMessage(playerLocationMsg);
                    }
                    return true;
                } else {
                    sender.sendPlainMessage("Player not found");
                    return false;
                }
            } else {
                sender.sendPlainMessage("Usage: /loglocations or /loglocations <player>");
                return false;
            }
        }
        if (command.getName().equalsIgnoreCase("logadd")) {
            if (args.length != 1) {
                sender.sendPlainMessage("Usage: /logadd <player>");
                return false;
            }
            String playerName = args[0];
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            UUID playerUUID = player.getUniqueId();
            List<String> players = getConfig().getStringList("specified-players");
            if (!players.contains(playerUUID.toString())) {
                players.add(playerUUID.toString());
                getConfig().set("specified-players", players);
                saveConfig();
                sender.sendPlainMessage(playerName + " has been added to the " + getConfig().getString("settings.listtype"));
            } else {
                sender.sendPlainMessage(playerName + " is already in the " + getConfig().getString("settings.listtype"));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("logremove")) {
            if (args.length != 1) {
                sender.sendPlainMessage("Usage: /removeplayer <player>");
                return false;
            }
            String playerName = args[0];
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            UUID playerUUID = player.getUniqueId();
            List<String> players = getConfig().getStringList("specified-players");
            if (players.contains(playerUUID.toString())) {
                players.remove(playerUUID.toString());
                getConfig().set("specified-players", players);
                saveConfig();
                sender.sendPlainMessage(playerName + " has been removed from the " + getConfig().getString("settings.listtype"));
            } else {
                sender.sendPlainMessage(playerName + " is not in the " + getConfig().getString("settings.listtype"));
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("setlisttype")) {
            if (args.length != 1) {
                sender.sendPlainMessage("Usage: /changelisttype <blacklist/whitelist>");
                return false;
            }
            getConfig().set("settings.listtype", args[0]);
            sender.sendPlainMessage("List type changed to " + args[0]);
            saveConfig();
            return true;
        }
        return false;
    }

    void logger() {
        new BukkitRunnable() {
            @Override
            public void run() {
                logPlayerLocations();
            }
        }.runTaskTimerAsynchronously(this, 0, checkTimeTicks);
    }
}