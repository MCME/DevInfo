package com.mcmiddleearth.devinfo;

import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.HashMap;

public class DevInfoLogger {

    public static final HashMap<String, HashMap<CommandSender, DevInfoLogger.DebugLevel>> subs = new HashMap<>();

    final String channel;

    public DevInfoLogger(String channel) {
        this.channel = channel;
    }

    public void log(DevInfoLogger.DebugLevel level, Object... message) {
        DevInfoLogger.log(channel, level, message);
    }

    public void log(int level, Object... message) {
        DevInfoLogger.log(channel, level, message);
    }

    public static void log(String channel, int level, Object... message) {
        DevInfoLogger.DebugLevel lvl = Arrays.stream(DevInfoLogger.DebugLevel.values()).filter(e -> e.id == level).findFirst().orElse(DevInfoLogger.DebugLevel.INFO);
        DevInfoLogger.log(channel, lvl, message);
    }

    public static void log(String channel, DevInfoLogger.DebugLevel level, Object... message) {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < message.length; i++) {
            sb.append(message[i]);
            if(i < message.length - 1) {
                sb.append(" ");
            }
        }
        subs.getOrDefault(channel, new HashMap<>()).forEach((sub, lvl) -> {
            if(level.id <= lvl.id) {
                sub.sendMessage(sb.toString());
            }
        });
    }

    public static void registerSub(String channel, CommandSender sender, DevInfoLogger.DebugLevel level){
        Utils.mapFindOrCreate(DevInfoLogger.subs, channel, new HashMap<>()).put(sender, level);
    }

    enum DebugLevel {
        OFF(0), ERROR(1), TRACE(2), WARN(3), INFO(4), DEBUG(5);

        int id;

        DebugLevel(int id) {
            this.id = id;
        }
    }
}
