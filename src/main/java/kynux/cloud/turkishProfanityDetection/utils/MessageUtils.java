package kynux.cloud.turkishProfanityDetection.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MessageUtils {

    public static void sendMessage(@NotNull CommandSender receiver, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // & ile yazılmış renk kodlarını çevir
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        receiver.sendMessage(coloredMessage);
    }

    public static String replacePlaceholders(String message, Player player, String text) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        return message
                .replace("%player%", player != null ? player.getName() : "?")
                .replace("%message%", text != null ? text : "");
    }

    public static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
