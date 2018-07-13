package com.mcmiddleearth.devinfo;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

@SuppressWarnings("unused")
public class DevInfo extends JavaPlugin {

    static ChatColor errorColor = ChatColor.RED;

    static HttpServer httpServer;

    public void onEnable() {
        this.saveDefaultConfig();
        this.getCommand("devinfo").setExecutor(new Commands());
        try {
            httpServer = new HttpServer(this.getConfig().getInt("infoPort"));
            httpServer.start();
            httpServer.deployPasswords.add(this.getConfig().getString("deploy"));
        } catch (IOException ex) {
            System.out.println("IO except");
        }
        new Thread(new Runnable() {
            public void run() {
                runCompression();
            }
        }).start();
    }

    private void runCompression() {
        try {
            System.out.println("Running compression async");
            Process compress = Runtime.getRuntime().exec("tar cfvz plugins/world.tar.gz world");
            compress.waitFor();
            System.out.println("Compression complete");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    static class Permissions {
        static final Permission web = new Permission("devinfo.web", PermissionDefault.OP);

        static final Permission plugins = new Permission("devinfo.web", PermissionDefault.OP);

        static final Permission debug = new Permission("devinfo.web", PermissionDefault.OP);

        static final Permission debug_console = new Permission("devinfo.web", PermissionDefault.OP);
    }

    static class Commands implements CommandExecutor {

        private void registerWebUser(Player user, String password) {
            httpServer.registeredUsers.put(password, user);
        }

        private void sendAllPluginVersions(Player player) {
            for(Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
                sendPluginVersion(player, plugin);
            }
        }

        private void sendPluginVersion(Player player, Plugin plugin) {
            player.sendMessage(plugin.getName() + " -> " + plugin.getDescription().getVersion());
        }

        private void registerSenderForDebug(CommandSender sender, String channel, DevInfoLogger.DebugLevel level) {
            DevInfoLogger.registerSub(channel, sender, level);
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if(!(sender instanceof Player)) {
                sender.sendMessage("Only allowed for players");
                return true;
            }

            Player player = (Player) sender;

            if(args.length >= 2 && player.hasPermission(Permissions.web) &&
                    args[0].equalsIgnoreCase("register")) {
                player.sendMessage("Registered account for " + player.getName());
                registerWebUser(player, args[1]);
                return true;
            }

            if(args.length >= 1 && player.hasPermission(Permissions.plugins) &&
                    args[0].equalsIgnoreCase("version")) {
                if(args.length == 1) {
                    sendAllPluginVersions(player);
                    return true;
                }
                Plugin plugin = Bukkit.getPluginManager().getPlugin(args[1]);
                if(plugin == null) {
                    player.sendMessage(errorColor + "Cannot find plugin " + args[1]);
                } else {
                    sendPluginVersion(player, plugin);
                }
                return true;
            }

            if(args.length >= 2 && player.hasPermission(Permissions.debug_console) &&
                    args[0].equalsIgnoreCase("console")) {
                String level = (args.length >= 3 ? args[2] : "ERROR");
                registerSenderForDebug(Bukkit.getConsoleSender(), args[1], DevInfoLogger.DebugLevel.valueOf(level));
                sender.sendMessage("Registered console to channel " + args[1] + " log level " + level);
                Bukkit.getConsoleSender().sendMessage("Registered to channel " + args[1] + " log level " + level);
                return true;
            }

            if(args.length >= 1 && player.hasPermission(Permissions.debug)) {
                String level = (args.length >= 2 ? args[1] : "ERROR");
                registerSenderForDebug(player, args[0], DevInfoLogger.DebugLevel.valueOf(level));
                sender.sendMessage("Registered to channel " + args[0] + " log level " + level);
                return true;
            }

            return false;
        }
    }
}
