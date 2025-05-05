package org.unitedlands.util;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.unitedlands.UnitedWar;

import net.kyori.adventure.text.Component;

public class Messenger {

    private static final UnitedWar plugin;

    static {
        plugin = UnitedWar.getPlugin(UnitedWar.class);
    }

    public static void broadCastMessage(String message, boolean includePrefix) {

        Component component;
        if (includePrefix) {
            var prefix = plugin.getConfig().getString("messages.prefix");
            component = Component.text(prefix).append(Component.text(message));
        } else {
            component = Component.text(message);
        }
        Bukkit.getServer().broadcast(component);
    }

    public static void sendMessage(Player player, String message, boolean includePrefix) {
        Component component;
        if (includePrefix) {
            var prefix = plugin.getConfig().getString("messages.prefix");
            component = Component.text(prefix).append(Component.text(message));
        } else {
            component = Component.text(message);
        }
        player.sendMessage(component);
    }

    public static void sendMessageTemplate(Player player, String messageId, Map<String, String> replacements, boolean includePrefix) {

        var message = plugin.getConfig().getString("messages." + messageId);

        if (replacements != null) {
            for (var entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        if (message == null) {
            plugin.getLogger().warning("Message ID '" + messageId + "' not found in config.");
            return;
        }

        sendMessage(player, message, includePrefix);
    }

    public static void sendMessageListTemplate(Player player, String messageListId, Map<String, String> replacements, boolean includePrefix) {

        var messageList = plugin.getConfig().getStringList("messages." + messageListId);

        if (messageList == null || messageList.isEmpty()) {
            plugin.getLogger().warning("Message list ID '" + messageListId + "' not found or empty in config.");
            return;
        }

        for (String message : messageList) {
            if (replacements != null) {
                for (var entry : replacements.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            sendMessage(player, message, includePrefix);
        }
    }

    public static void broadcastMessageTemplate(String messageId, Map<String, String> replacements, boolean includePrefix) {

        var message = plugin.getConfig().getString("messages." + messageId);

        if (replacements != null) {
            for (var entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }

        if (message == null) {
            plugin.getLogger().warning("Message ID '" + messageId + "' not found in config.");
            return;
        }

        broadCastMessage(message, includePrefix);
    }

    public static void broadcastMessageListTemplate(String messageListId, Map<String, String> replacements, boolean includePrefix) {

        var messageList = plugin.getConfig().getStringList("messages." + messageListId);

        if (messageList == null || messageList.isEmpty()) {
            plugin.getLogger().warning("Message list ID '" + messageListId + "' not found or empty in config.");
            return;
        }

        for (String message : messageList) {
            if (replacements != null) {
                for (var entry : replacements.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                }
            }
            broadCastMessage(message, includePrefix);
        }
    }

}
