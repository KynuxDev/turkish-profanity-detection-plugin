package kynux.cloud.turkishProfanityDetection.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Mesaj işlemleri için yardımcı sınıf.
 */
public class MessageUtils {

    /**
     * Bir oyuncuya veya konsola renkli mesaj gönderir.
     *
     * @param receiver Mesajı alacak kişi
     * @param message  Gönderilecek mesaj
     */
    public static void sendMessage(@NotNull CommandSender receiver, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        
        // & ile yazılmış renk kodlarını çevir
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        receiver.sendMessage(coloredMessage);
    }
    
    /**
     * Bir mesajdaki değişkenleri değerleriyle değiştirir.
     *
     * @param message Orijinal mesaj
     * @param player  Oyuncu
     * @param text    Metin
     * @return Değişkenler değiştirildikten sonraki mesaj
     */
    public static String replacePlaceholders(String message, Player player, String text) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        
        return message
                .replace("%player%", player != null ? player.getName() : "?")
                .replace("%message%", text != null ? text : "");
    }
    
    /**
     * Bir JSON metni içindeki özel karakterleri kaçış karakterleriyle değiştirir.
     *
     * @param text JSON formatlaması için kaçılacak metin
     * @return Kaçış karakterleri eklenmiş metin
     */
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
