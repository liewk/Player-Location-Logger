package com.github.bitbyte.locationlog;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class LocationLog extends JavaPlugin {
    static int checkTimeTicks;
    static int checkTimeHours;
    static int checkTimeMinutes;
    static int checkTimeSeconds;
    private YamlDocument config;
    @Override
    public void onEnable() {
        if (getConfig().getBoolean("settings.bstats")) {
            Metrics metrics = new Metrics(this, 18168);
        }
        try {
            config = YamlDocument.create(new File(getDataFolder(), "config.yml"), getResource("config.yml"),
                    GeneralSettings.DEFAULT, LoaderSettings.builder().setAutoUpdate(true).build(), DumperSettings.DEFAULT, UpdaterSettings.builder().setVersioning(new BasicVersioning("version")).build());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
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
            getLogger().info("Check time settings are set to or below 0. Disabling Automatic logger.");
        }
        else {
            getLogger().info("Automatic logger is running every " + getConfig().getInt("settings.checkhours") + " hour(s), " + getConfig().getInt("settings.checkminutes") + " minute(s), " + getConfig().getInt("settings.checkseconds") + " second(s).");
            logger();
        }
    }

    @Override
    public void onDisable() {
        try {
            config.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void logPlayerLocations() {
        List<String> players = getConfig().getStringList("specified-players");
        String listType = getConfig().getString("settings.listtype");
        boolean isBlacklist = listType.equalsIgnoreCase("blacklist");
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            UUID playerUUID = player.getUniqueId();
            if ((isBlacklist && players.contains(playerUUID.toString())) || (!isBlacklist && !players.contains(playerUUID.toString()))) continue;
            if (player.hasPermission("locationlog.exempt")) continue;
            Location location = player.getLocation();
            int x = (int) location.getX();
            int y = (int) location.getY();
            int z = (int) location.getZ();
            boolean shortenLog = getConfig().getBoolean("settings.shorten-log");
            StringBuilder logMessageBuilder = new StringBuilder();
            logMessageBuilder.append(player.getName())
                    .append(shortenLog ? ": " : " is in ")
                    .append(location.getWorld().getName());
            if (shortenLog) {
                logMessageBuilder.append(", XYZ: ")
                        .append(x).append(' ')
                        .append(y).append(' ')
                        .append(z);
            } else {
                logMessageBuilder.append(" at X: ")
                        .append(x)
                        .append(", Y: ")
                        .append(y)
                        .append(", Z: ")
                        .append(z);
            }
            String logMessage = logMessageBuilder.toString();
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
        switch (command.getName().toLowerCase()) {
            case "loglocations":
            if (args.length == 0) {
                logPlayerLocations();
                sender.sendPlainMessage("Logged all players locations");
                return true;
            } else if (args.length == 1) {
                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    Location location = targetPlayer.getLocation();
                    int x = (int) location.getX();
                    int y = (int) location.getY();
                    int z = (int) location.getZ();
                    boolean shortenLog = getConfig().getBoolean("settings.shorten-log");
                    StringBuilder logMessageBuilder = new StringBuilder();
                    logMessageBuilder.append(targetPlayer.getName())
                            .append(shortenLog ? ": " : " is in ")
                            .append(location.getWorld().getName());
                    if (shortenLog) {
                        logMessageBuilder.append(", XYZ: ")
                                .append(x).append(' ')
                                .append(y).append(' ')
                                .append(z);
                    } else {
                        logMessageBuilder.append(" at X: ")
                                .append(x)
                                .append(", Y: ")
                                .append(y)
                                .append(", Z: ")
                                .append(z);
                    }
                    String playerLogMessage = logMessageBuilder.toString();
                    getLogger().info(playerLogMessage);
                    if (sender.hasPermission("locationlog.viewlog")) {
                        sender.sendPlainMessage(playerLogMessage);
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
            case "logadd":
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
            case "logremove":
            if (args.length != 1) {
                sender.sendPlainMessage("Usage: /logremove <player>");
                return false;
            }
            String playerName1 = args[0];
            OfflinePlayer player1 = Bukkit.getOfflinePlayer(playerName1);
            UUID playerUUID1 = player1.getUniqueId();
            List<String> players1 = getConfig().getStringList("specified-players");
            if (players1.contains(playerUUID1.toString())) {
                players1.remove(playerUUID1.toString());
                getConfig().set("specified-players", players1);
                saveConfig();
                sender.sendPlainMessage(playerName1 + " has been removed from the " + getConfig().getString("settings.listtype"));
            } else {
                sender.sendPlainMessage(playerName1 + " is not in the " + getConfig().getString("settings.listtype"));
            }
            return true;
            case "setlisttype":
            if (args.length != 1) {
                sender.sendPlainMessage("Usage: /setlisttype <blacklist/whitelist>");
                return false;
            }
            getConfig().set("settings.listtype", args[0]);
            sender.sendPlainMessage("List type changed to " + args[0]);
            try {
                config.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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