package kynux.cloud.turkishProfanityDetection.listeners;

import kynux.cloud.turkishProfanityDetection.TurkishProfanityDetection;
import kynux.cloud.turkishProfanityDetection.api.KynuxAIResponse;
import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import kynux.cloud.turkishProfanityDetection.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable; // Eksik import eklendi

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ChatListener implements Listener {
    private final TurkishProfanityDetection plugin;
    private final boolean cancelMessage;
    private final boolean logEnabled;
    private final boolean logConsole;
    private final boolean logFile;
    private final String logFilePath;
    private final boolean commandsEnabled;
    private final List<String> commands;
    private final boolean severityActionsEnabled;
    private final Map<Integer, List<String>> severityCommands;
    private final String bypassPermission;
    private final boolean statisticsEnabled;
    
    private final boolean rateLimitEnabled;
    private final int maxMessages;
    private final int timeWindow;
    private final String rateLimitAction;
    private final Map<UUID, MessageCounter> playerMessageCounts = new HashMap<>();
    
    public ChatListener(@NotNull TurkishProfanityDetection plugin) {
        this.plugin = plugin;
        
        this.cancelMessage = plugin.getConfig().getBoolean("actions.cancel-message", true);
        this.logEnabled = plugin.getConfig().getBoolean("actions.log.enabled", true);
        this.logConsole = plugin.getConfig().getBoolean("actions.log.console", true);
        this.logFile = plugin.getConfig().getBoolean("actions.log.file", true);
        this.logFilePath = plugin.getConfig().getString("actions.log.file-path", "logs/profanity.log");
        this.commandsEnabled = plugin.getConfig().getBoolean("actions.commands.enabled", true);
        this.commands = plugin.getConfig().getStringList("actions.commands.list");
        this.bypassPermission = plugin.getConfig().getString("permissions.bypass", "turkishprofanitydetection.bypass");
        this.statisticsEnabled = plugin.getConfig().getBoolean("statistics.enabled", true);
        
        this.severityActionsEnabled = plugin.getConfig().getBoolean("severity-actions.enabled", true);
        this.severityCommands = new HashMap<>();
        if (severityActionsEnabled) {
            for (int i = 1; i <= 5; i++) {
                List<String> levelCommands = plugin.getConfig().getStringList("severity-actions.levels." + i + ".commands");
                if (!levelCommands.isEmpty()) {
                    severityCommands.put(i, levelCommands);
                }
            }
        }
        
        this.rateLimitEnabled = plugin.getConfig().getBoolean("security.rate-limit.enabled", true);
        this.maxMessages = plugin.getConfig().getInt("security.rate-limit.max-messages", 5);
        this.timeWindow = plugin.getConfig().getInt("security.rate-limit.time-window", 10);
        this.rateLimitAction = plugin.getConfig().getString("security.rate-limit.action", "mute %player% 5m Spam");
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getMessage().trim().isEmpty()) {
            return;
        }
        
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        if (player.hasPermission(bypassPermission)) {
            return;
        }
        
        if (rateLimitEnabled && isRateLimited(player)) {
            event.setCancelled(true);
            
            if (rateLimitAction != null && !rateLimitAction.isEmpty()) {
                executeCommand(rateLimitAction.replace("%player%", player.getName()));
            }
            
            MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") + 
                    "Çok hızlı mesaj gönderiyorsunuz. Lütfen yavaşlayın.");
            return;
        }
        
        Boolean cachedResult = plugin.getFromCache(message);
        if (cachedResult != null) {
            if (cachedResult) {
                handleProfanityDetected(player, message, null);
            }
            return;
        }
        
        plugin.getThreadPool().submit(() -> {
            try {
                KynuxAIResponse aiResponse = plugin.getKynuxAIService().getChatCompletion(message);

                if (aiResponse == null) {
                    plugin.getLogger().warning("Kynux AI servisinden '" + message + "' için null yanıt alındı.");
                    plugin.addToCache(message, false);
                    return;
                }

                if (!aiResponse.isProfane()) {
                    plugin.addToCache(message, false);
                    return;
                }
                
                plugin.addToCache(message, true);
                
                handleProfanityDetected(player, message, aiResponse);

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Kynux AI ile küfür kontrolü sırasında hata: " + e.getMessage(), e);
                plugin.addToCache(message, false);
            }
        });
    }
    
    private void handleProfanityDetected(Player player, String message, @Nullable KynuxAIResponse aiResponse) {
        try {
            MessageUtils.sendMessage(player, plugin.getConfig().getString("messages.prefix") +
                    plugin.getConfig().getString("messages.blocked", "Mesajınız uygunsuz içerik nedeniyle engellendi."));

            if (aiResponse == null) {
                plugin.getLogger().info(player.getName() + " tarafından gönderilen mesaj (önbellekten) uygunsuz bulundu: " + message);
                if (commandsEnabled && !commands.isEmpty()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (String cmd : commands) {
                            String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                            executeCommand(finalCmd);
                        }
                    });
                }
                return;
            }
                
            if (statisticsEnabled) {
                plugin.getThreadPool().submit(() -> {
                    try {
                        ProfanityRecord record = new ProfanityRecord(
                                player.getUniqueId(),
                                player.getName(),
                                aiResponse.getDetectedWord(),
                                aiResponse.getCategory(),
                                aiResponse.getSeverity(),
                                Collections.singletonList(aiResponse.getDetectedWord()),
                                message,
                                true,
                                1.0,
                                plugin.getConfig().getString("kynux_api.model", "gpt-3.5-turbo"),
                                aiResponse.getActionRecommendation(),
                                aiResponse.isSafeForMinecraft(),
                                aiResponse.getAnalysisDetails()
                        );
                        
                        plugin.getProfanityStorage().addRecord(record);
                        
                        if (plugin.getConfig().getBoolean("actions.discord.enabled", false)) {
                            plugin.getThreadPool().submit(() -> plugin.getDiscordWebhook().sendProfanityAlert(record));
                        }
                        
                        if (plugin.getPlaceholderAPIHook() != null) {
                            plugin.getPlaceholderAPIHook().updatePlayerProfanityCount(player.getUniqueId());
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "İstatistik kaydedilirken hata: " + e.getMessage(), e);
                    }
                });
            }
            
            if (logEnabled) {
                plugin.getThreadPool().submit(() -> {
                    try {
                        String logMessage = String.format(
                                "%s tarafından gönderilen mesajda Kynux AI tarafından uygunsuz içerik tespit edildi: '%s'. Detaylar: Kelime: %s, Kategori: %s, Şiddet: %d, Aksiyon: %s, Analiz: %s",
                                player.getName(), message, aiResponse.getDetectedWord(), aiResponse.getCategory(),
                                aiResponse.getSeverity(), aiResponse.getActionRecommendation(), aiResponse.getAnalysisDetails()
                        );
                        
                        if (logConsole) {
                            plugin.getLogger().info(logMessage);
                        }
                        
                        if (logFile) {
                            logToFile(player.getName(), message, aiResponse);
                        }
                        
                        final String adminMessage = plugin.getConfig().getString("messages.prefix") +
                                plugin.getConfig().getString("messages.admin-alert", "&c%player% &fmuhtemel küfür kullandı: &7%message%")
                                        .replace("%player%", player.getName())
                                        .replace("%message%", message) +
                                        String.format(" &8(&eAI: %s, Şiddet: %d, Aksiyon: %s&8)",
                                                aiResponse.getDetectedWord(), aiResponse.getSeverity(), aiResponse.getActionRecommendation());
                        
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            for (Player admin : Bukkit.getOnlinePlayers()) {
                                if (admin.hasPermission("turkishprofanitydetection.admin")) {
                                    MessageUtils.sendMessage(admin, adminMessage);
                                }
                            }
                        });
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Log işlemleri sırasında hata: " + e.getMessage(), e);
                    }
                });
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    String actionRecommendation = aiResponse.getActionRecommendation();
                    if (actionRecommendation != null && !actionRecommendation.isEmpty() && !actionRecommendation.equalsIgnoreCase("none")) {
                        executeActionRecommendation(player, message, actionRecommendation);
                    } else if (severityActionsEnabled) {
                        int severity = aiResponse.getSeverity();
                        List<String> levelCommands = severityCommands.get(severity);
                        
                        if (levelCommands != null && !levelCommands.isEmpty()) {
                            for (String cmd : levelCommands) {
                                String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                                executeCommand(finalCmd);
                            }
                            return; 
                        }
                    }
                    
                    if (commandsEnabled && !commands.isEmpty() && (actionRecommendation == null || actionRecommendation.isEmpty() || actionRecommendation.equalsIgnoreCase("none"))) {
                        for (String cmd : commands) {
                            String finalCmd = cmd.replace("%player%", player.getName()).replace("%message%", message);
                            executeCommand(finalCmd);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Komut çalıştırılırken hata: " + e.getMessage(), e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "handleProfanityDetected sırasında genel hata: " + e.getMessage(), e);
        }
    }
    
    private void logToFile(String playerName, String message, KynuxAIResponse aiResponse) {
        if (logFilePath == null || logFilePath.isEmpty() || aiResponse == null) {
            return;
        }
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        File logFileInstance = new File(dataFolder, logFilePath);
        File parentDir = logFileInstance.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFileInstance, true))) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            writer.println(String.format("[%s] %s: '%s' (Kategori: %s, Şiddet: %d, Tespit: %s, Aksiyon: %s, Güvenli: %b, Analiz: %s) - Mesaj: \"%s\"",
                    timestamp, playerName, aiResponse.getDetectedWord(), aiResponse.getCategory(), aiResponse.getSeverity(),
                    aiResponse.getDetectedWord(), aiResponse.getActionRecommendation(), aiResponse.isSafeForMinecraft(),
                    aiResponse.getAnalysisDetails(), message));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Kynux AI küfür log dosyasına yazılırken hata: " + e.getMessage(), e);
        }
    }
    
    private void executeActionRecommendation(Player player, String message, String actionType) {
        String playerName = player.getName();
        
        switch (actionType.toLowerCase()) {
            case "warn":
                String warnMessage = plugin.getConfig().getString("action-recommendations.warn", 
                        "warn %player% Uygunsuz içerik tespit edildi. Lütfen dikkat ediniz.");
                executeCommand(warnMessage.replace("%player%", playerName));
                break;
                
            case "mute":
                String muteTime = plugin.getConfig().getString("action-recommendations.mute-time", "5m");
                String muteMessage = plugin.getConfig().getString("action-recommendations.mute", 
                        "mute %player% %time% Uygunsuz içerik");
                executeCommand(muteMessage.replace("%player%", playerName).replace("%time%", muteTime));
                break;
                
            case "kick":
                String kickMessage = plugin.getConfig().getString("action-recommendations.kick", 
                        "kick %player% Uygunsuz içerik nedeniyle sunucudan atıldınız.");
                executeCommand(kickMessage.replace("%player%", playerName));
                break;
                
            case "ban":
                String banTime = plugin.getConfig().getString("action-recommendations.ban-time", "1d");
                String banMessage = plugin.getConfig().getString("action-recommendations.ban", 
                        "tempban %player% %time% Uygunsuz içerik nedeniyle geçici olarak yasaklandınız.");
                executeCommand(banMessage.replace("%player%", playerName).replace("%time%", banTime));
                break;
                
            default:
                if (commandsEnabled && !commands.isEmpty()) {
                    for (String cmd : commands) {
                        String finalCmd = cmd.replace("%player%", playerName).replace("%message%", message);
                        executeCommand(finalCmd);
                    }
                }
                break;
        }
    }
    
    private void executeCommand(String command) {
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command));
    }
    
    private boolean isRateLimited(Player player) {
        if (!rateLimitEnabled || maxMessages <= 0 || timeWindow <= 0) {
            return false;
        }
        
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        MessageCounter counter = playerMessageCounts.computeIfAbsent(playerId, 
                k -> new MessageCounter(currentTime));
        
        if (currentTime - counter.startTime > TimeUnit.SECONDS.toMillis(timeWindow)) {
            counter.count = 1;
            counter.startTime = currentTime;
            return false;
        }
        
        counter.count++;
        return counter.count > maxMessages;
    }
    
    private static class MessageCounter {
        private int count;
        private long startTime;
        
        public MessageCounter(long startTime) {
            this.count = 1;
            this.startTime = startTime;
        }
    }
}
