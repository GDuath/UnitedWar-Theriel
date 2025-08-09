package org.unitedlands.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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

    public static void sendMessage(CommandSender sender, String message, boolean includePrefix) {
        Component component;
        if (includePrefix) {
            var prefix = plugin.getConfig().getString("messages.prefix");
            component = Component.text(prefix).append(Component.text(message));
        } else {
            component = Component.text(message);
        }
        sender.sendMessage(component);
    }

    public static void sendMessageTemplate(CommandSender sender, String messageId, Map<String, String> replacements,
            boolean includePrefix) {

        var message = plugin.getConfig().getString("messages." + messageId);

        if (replacements != null) {
            for (var entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }

        if (message == null) {
            plugin.getLogger().warning("Message ID '" + messageId + "' not found in config.");
            return;
        }

        sendMessage(sender, message, includePrefix);
    }

    public static void sendMessageListTemplate(CommandSender sender, String messageListId,
            Map<String, String> replacements,
            boolean includePrefix) {

        var messageList = plugin.getConfig().getStringList("messages." + messageListId);

        if (messageList == null || messageList.isEmpty()) {
            plugin.getLogger().warning("Message list ID '" + messageListId + "' not found or empty in config.");
            return;
        }

        for (String message : messageList) {
            if (replacements != null) {
                for (var entry : replacements.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}",
                            entry.getValue() != null ? entry.getValue() : "");
                }
            }
            sendMessage(sender, message, includePrefix);
        }
    }

    public static void broadcastMessageTemplate(String messageId, Map<String, String> replacements,
            boolean includePrefix) {

        var message = plugin.getConfig().getString("messages." + messageId);

        if (replacements != null) {
            for (var entry : replacements.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }

        if (message == null) {
            plugin.getLogger().warning("Message ID '" + messageId + "' not found in config.");
            return;
        }

        broadCastMessage(message, includePrefix);
    }

    public static void broadcastMessageListTemplate(String messageListId, Map<String, String> replacements,
            boolean includePrefix) {

        var messageList = plugin.getConfig().getStringList("messages." + messageListId);

        if (messageList == null || messageList.isEmpty()) {
            plugin.getLogger().warning("Message list ID '" + messageListId + "' not found or empty in config.");
            return;
        }

        for (String message : messageList) {
            if (replacements != null) {
                for (var entry : replacements.entrySet()) {
                    message = message.replace("{" + entry.getKey() + "}",
                            entry.getValue() != null ? entry.getValue() : "");
                }
            }
            broadCastMessage(message, includePrefix);
        }
    }

    public static void sendDiscordEmbed(String embed, Map<String, String> replacements) {
        try {
            var webhookUrl = plugin.getConfig().getString("discord.webhook_url");
            if (webhookUrl == null)
                return;

            URL url = URI.create(webhookUrl).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            var pingRole = plugin.getConfig().getString("discord.ping-role-id");
            if (pingRole != null && pingRole.length() > 0) {
                replacements.put("ping-role", "<@&" + pingRole + ">");
            } else {
                replacements.put("ping-role", "");
            }

            if (replacements != null) {
                for (var entry : replacements.entrySet()) {
                    embed = embed.replace("{" + entry.getKey() + "}",
                            entry.getValue() != null ? entry.getValue() : "");
                }
            }

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = embed.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != 204) {
                System.out.println("Failed to send Discord embed. Response code: " + responseCode);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
