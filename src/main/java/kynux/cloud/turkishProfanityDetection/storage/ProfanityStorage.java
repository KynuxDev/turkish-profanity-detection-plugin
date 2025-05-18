package kynux.cloud.turkishProfanityDetection.storage;

import kynux.cloud.turkishProfanityDetection.model.ProfanityRecord;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProfanityStorage {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final String storageType;
    private final int dataRetentionDays;
    private final Map<UUID, List<ProfanityRecord>> playerRecords = new ConcurrentHashMap<>();
    private final File storageFile;
    private final DatabaseManager databaseManager;
    
    public ProfanityStorage(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        FileConfiguration config = plugin.getConfig();
        this.storageType = config.getString("statistics.storage-type", "mysql").toLowerCase();
        this.dataRetentionDays = config.getInt("statistics.data-retention-days", 30);
        
        this.databaseManager = new DatabaseManager(plugin, config);
        
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Eklenti veri klasörü oluşturulamadı!");
        }
        
        this.storageFile = new File(dataFolder, "profanity_records.dat");
        
        loadData();
        
        if (dataRetentionDays > 0) {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupOldData, 20 * 60 * 60, 20 * 60 * 60 * 24);
        }
        
        logger.info("Profanity Storage başlatıldı. Depolama tipi: " + storageType);
    }
    
    public void addRecord(@NotNull ProfanityRecord record) {
        UUID playerId = record.getPlayerId();
        
        playerRecords.computeIfAbsent(playerId, k -> new ArrayList<>()).add(record);
        
        if (storageType.equals("mysql")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.addRecord(record);
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    public List<ProfanityRecord> getPlayerRecords(UUID playerId) {
        if (storageType.equals("mysql")) {
            if (!playerRecords.containsKey(playerId)) {
                List<ProfanityRecord> dbRecords = databaseManager.getPlayerRecords(playerId);
                if (!dbRecords.isEmpty()) {
                    playerRecords.put(playerId, dbRecords);
                }
            }
        }
        
        return playerRecords.getOrDefault(playerId, new ArrayList<>());
    }
    
    public Map<UUID, List<ProfanityRecord>> getAllRecords() {
        if (storageType.equals("mysql")) {
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            if (!dbRecords.isEmpty()) {
                playerRecords.clear();
                playerRecords.putAll(dbRecords);
            }
        }
        
        return new HashMap<>(playerRecords);
    }
    
    public void clearPlayerRecords(UUID playerId) {
        playerRecords.remove(playerId);
        
        if (storageType.equals("mysql")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.clearPlayerRecords(playerId);
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    public void clearAllRecords() {
        playerRecords.clear();
        
        if (storageType.equals("mysql")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.clearAllRecords();
            });
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    private void cleanupOldData() {
        if (dataRetentionDays <= 0) {
            return;
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minus(dataRetentionDays, ChronoUnit.DAYS);
        
        if (storageType.equals("mysql")) {
            databaseManager.cleanupOldData(dataRetentionDays);
            
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            playerRecords.clear();
            playerRecords.putAll(dbRecords);
        } else {
            boolean dataChanged = false;
            
            for (UUID playerId : new HashSet<>(playerRecords.keySet())) {
                List<ProfanityRecord> records = playerRecords.get(playerId);
                
                if (records != null) {
                    int initialSize = records.size();
                    
                    List<ProfanityRecord> filteredRecords = records.stream()
                            .filter(record -> record.getTimestamp().isAfter(cutoffDate))
                            .collect(Collectors.toList());
                    
                    if (filteredRecords.size() < initialSize) {
                        playerRecords.put(playerId, filteredRecords);
                        dataChanged = true;
                    }
                    
                    if (filteredRecords.isEmpty()) {
                        playerRecords.remove(playerId);
                    }
                }
            }
            
            if (dataChanged) {
                saveData();
                logger.info(dataRetentionDays + " günden eski küfür kayıtları temizlendi.");
            }
        }
    }
    
    private void saveData() {
        if (storageType.equals("mysql")) {
            return;
        }
        
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storageFile))) {
            oos.writeObject(serializeData());
            logger.fine("Küfür kayıtları başarıyla kaydedildi.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Küfür kayıtları kaydedilirken hata: " + e.getMessage(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void loadData() {
        if (storageType.equals("mysql")) {
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            playerRecords.clear();
            playerRecords.putAll(dbRecords);
            logger.info("Küfür kayıtları MySQL veritabanından başarıyla yüklendi.");
            return;
        }
        
        if (!storageFile.exists()) {
            logger.info("Küfür kayıtları dosyası bulunamadı. Yeni dosya oluşturulacak.");
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storageFile))) {
            Map<String, Object> data = (Map<String, Object>) ois.readObject();
            deserializeData(data);
            logger.info("Küfür kayıtları başarıyla yüklendi.");
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Küfür kayıtları yüklenirken hata: " + e.getMessage(), e);
        }
    }
    
    public void reload() {
        playerRecords.clear();
        loadData();
    }
    
    public void shutdown() {
        if (storageType.equals("mysql")) {
            databaseManager.close();
        } else {
            saveData();
        }
    }
    
    private Map<String, Object> serializeData() {
        Map<String, Object> data = new HashMap<>();
        data.put("version", 1);
        data.put("timestamp", System.currentTimeMillis());
        
        Map<String, List<Map<String, Object>>> serializedRecords = new HashMap<>();
        
        for (Map.Entry<UUID, List<ProfanityRecord>> entry : playerRecords.entrySet()) {
            String playerUUID = entry.getKey().toString();
            List<Map<String, Object>> recordsList = new ArrayList<>();
            
            for (ProfanityRecord record : entry.getValue()) {
                Map<String, Object> recordMap = new HashMap<>();
                recordMap.put("playerName", record.getPlayerName());
                recordMap.put("word", record.getWord());
                recordMap.put("category", record.getCategory());
                recordMap.put("severityLevel", record.getSeverityLevel());
                recordMap.put("detectedWords", record.getDetectedWords());
                recordMap.put("originalMessage", record.getOriginalMessage());
                recordMap.put("timestamp", record.getFormattedTimestamp());
                recordMap.put("aiDetected", record.isAiDetected());
                
                recordsList.add(recordMap);
            }
            
            serializedRecords.put(playerUUID, recordsList);
        }
        
        data.put("records", serializedRecords);
        return data;
    }
    
    @SuppressWarnings("unchecked")
    private void deserializeData(Map<String, Object> data) {
        if (data.containsKey("records")) {
            Map<String, List<Map<String, Object>>> records = (Map<String, List<Map<String, Object>>>) data.get("records");
            
            for (Map.Entry<String, List<Map<String, Object>>> entry : records.entrySet()) {
                try {
                    UUID playerId = UUID.fromString(entry.getKey());
                    List<ProfanityRecord> playerRecordsList = new ArrayList<>();
                    
                    for (Map<String, Object> recordMap : entry.getValue()) {
                        String playerName = (String) recordMap.get("playerName");
                        String word = (String) recordMap.get("word");
                        String category = (String) recordMap.get("category");
                        int severityLevel = ((Number) recordMap.get("severityLevel")).intValue();
                        List<String> detectedWords = (List<String>) recordMap.get("detectedWords");
                        String originalMessage = (String) recordMap.get("originalMessage");
                        boolean aiDetected = (boolean) recordMap.get("aiDetected");
                        
                        ProfanityRecord record = new ProfanityRecord(
                                playerId, playerName, word, category, severityLevel,
                                detectedWords, originalMessage, aiDetected);
                        playerRecordsList.add(record);
                    }
                    
                    if (!playerRecordsList.isEmpty()) {
                        playerRecords.put(playerId, playerRecordsList);
                    }
                } catch (IllegalArgumentException e) {
                    logger.warning("Geçersiz UUID formatı: " + entry.getKey());
                }
            }
        }
    }
}
