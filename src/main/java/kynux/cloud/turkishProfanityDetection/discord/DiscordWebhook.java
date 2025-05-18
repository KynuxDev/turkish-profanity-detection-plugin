package kynux.cloud.turkishProfanityDetection.discord;

import com.google.gson.JsonObject;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import kynux.cloud.turkishProfanityDetection.utils.MessageUtils;
import okhttp3.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordWebhook {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final OkHttpClient client;
    private final boolean enabled;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final Color embedColor;
    private final int notifySeverityLevel;
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public DiscordWebhook(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        FileConfiguration config = plugin.getConfig();
        
        this.enabled = config.getBoolean("actions.discord.enabled", false);
        this.webhookUrl = config.getString("actions.discord.webhook-url", "");
        this.username = config.getString("actions.discord.username", "Küfür Koruması");
        this.avatarUrl = config.getString("actions.discord.avatar-url", "");
        
        String colorHex = config.getString("actions.discord.embed-color", "#FF0000").replace("#", "");
        Color parsedColor;
        try {
            parsedColor = Color.decode("#" + colorHex);
        } catch (NumberFormatException e) {
            logger.warning("Geçersiz embed renk kodu: " + colorHex + ". Varsayılan kırmızı kullanılıyor.");
            parsedColor = Color.RED;
        }
        this.embedColor = parsedColor;
        
        this.notifySeverityLevel = config.getInt("actions.discord.notify-severity-level", 3);
        
        this.client = new OkHttpClient();
        
        if (enabled && (webhookUrl == null || webhookUrl.isEmpty())) {
            logger.warning("Discord webhook etkinleştirildi fakat webhook URL'si tanımlanmamış!");
        }
    }

    public void sendProfanityAlert(@NotNull ProfanityRecord record) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            return;
        }
        
        if (record.getSeverityLevel() < notifySeverityLevel) {
            return;
        }
        
        JsonObject json = new JsonObject();
        
        if (username != null && !username.isEmpty()) {
            json.addProperty("username", username);
        }
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.addProperty("avatar_url", avatarUrl);
        }
        
        JsonObject embed = new JsonObject();
        
        int colorValue = embedColor.getRed() << 16 | embedColor.getGreen() << 8 | embedColor.getBlue();
        embed.addProperty("color", colorValue);
        
        embed.addProperty("title", "Küfür Tespit Edildi");
        
        String description = String.format(
                "**Oyuncu:** %s\n" +
                "**Kelime:** %s\n" +
                "**Kategori:** %s\n" +
                "**Şiddet:** %d/5\n" +
                "**Zaman:** %s\n" +
                "**AI Tespiti:** %s\n\n" +
                "**Orijinal Mesaj:**\n```%s```",
                record.getPlayerName(),
                MessageUtils.escapeJson(record.getWord()),
                record.getCategory(),
                record.getSeverityLevel(),
                record.getFormattedTimestamp(),
                record.isAiDetected() ? "Evet" : "Hayır",
                MessageUtils.escapeJson(record.getOriginalMessage())
        );
        embed.addProperty("description", description);
        
        JsonObject footer = new JsonObject();
        footer.addProperty("text", "Turkish Profanity Detection • v" + plugin.getDescription().getVersion());
        embed.add("footer", footer);
        
        JsonObject embedsArray = new JsonObject();
        embedsArray.add("embeds", embed);
        json.add("embeds", embedsArray);
        
        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(body)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.log(Level.WARNING, "Discord webhook gönderimi başarısız: " + e.getMessage(), e);
            }
            
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (!response.isSuccessful()) {
                    try {
                        logger.warning("Discord webhook hatası: " + response.code() + " - " + 
                                (response.body() != null ? response.body().string() : "Boş yanıt"));
                    } catch (IOException e) {
                        logger.warning("Discord webhook yanıtı okunurken hata: " + e.getMessage());
                    }
                }
                response.close();
            }
        });
    }
}
