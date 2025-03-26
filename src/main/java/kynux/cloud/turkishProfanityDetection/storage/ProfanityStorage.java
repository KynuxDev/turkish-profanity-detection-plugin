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

/**
 * Küfür kayıtlarını saklamak ve yönetmek için kullanılan sınıf.
 */
public class ProfanityStorage {
    private final JavaPlugin plugin;
    private final Logger logger;
    private final String storageType;
    private final int dataRetentionDays;
    private final Map<UUID, List<ProfanityRecord>> playerRecords = new ConcurrentHashMap<>();
    private final File storageFile;
    private final DatabaseManager databaseManager;
    
    /**
     * Depolama servisini başlatır.
     *
     * @param plugin Eklenti ana sınıfı
     */
    public ProfanityStorage(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        FileConfiguration config = plugin.getConfig();
        this.storageType = config.getString("statistics.storage-type", "mysql").toLowerCase();
        this.dataRetentionDays = config.getInt("statistics.data-retention-days", 30);
        
        // Veritabanı yöneticisini oluştur
        this.databaseManager = new DatabaseManager(plugin, config);
        
        // Depolama dosyası
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            logger.severe("Eklenti veri klasörü oluşturulamadı!");
        }
        
        this.storageFile = new File(dataFolder, "profanity_records.dat");
        
        // Veriyi yükle
        loadData();
        
        // Periyodik temizlik işlemi
        if (dataRetentionDays > 0) {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::cleanupOldData, 20 * 60 * 60, 20 * 60 * 60 * 24); // Günde bir kez
        }
        
        // Başlangıç ​​mesajı
        logger.info("Profanity Storage başlatıldı. Depolama tipi: " + storageType);
    }
    
    /**
     * Yeni bir küfür kaydı ekler.
     *
     * @param record Eklenecek küfür kaydı
     */
    public void addRecord(@NotNull ProfanityRecord record) {
        UUID playerId = record.getPlayerId();
        
        // Bellekteki kayıtları güncelle
        playerRecords.computeIfAbsent(playerId, k -> new ArrayList<>()).add(record);
        
        // Depolama metoduna göre kaydet
        if (storageType.equals("mysql")) {
            // Asenkron olarak veritabanına kaydet
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.addRecord(record);
            });
        } else {
            // Asenkron olarak dosyaya kaydet
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    /**
     * Bir oyuncunun tüm küfür kayıtlarını getirir.
     *
     * @param playerId Oyuncu UUID
     * @return Oyuncunun küfür kayıtları listesi
     */
    public List<ProfanityRecord> getPlayerRecords(UUID playerId) {
        if (storageType.equals("mysql")) {
            // MySQL'den al (cache kontrolü yapılabilir)
            if (!playerRecords.containsKey(playerId)) {
                List<ProfanityRecord> dbRecords = databaseManager.getPlayerRecords(playerId);
                if (!dbRecords.isEmpty()) {
                    playerRecords.put(playerId, dbRecords);
                }
            }
        }
        
        return playerRecords.getOrDefault(playerId, new ArrayList<>());
    }
    
    /**
     * Tüm oyuncuların küfür kayıtlarını getirir.
     *
     * @return Tüm küfür kayıtları
     */
    public Map<UUID, List<ProfanityRecord>> getAllRecords() {
        if (storageType.equals("mysql")) {
            // Tam bir veritabanı yüklemesi yap
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            if (!dbRecords.isEmpty()) {
                // Belleği tamamen temizle ve veritabanındaki kayıtlarla değiştir
                playerRecords.clear();
                playerRecords.putAll(dbRecords);
            }
        }
        
        return new HashMap<>(playerRecords);
    }
    
    /**
     * Bir oyuncunun tüm küfür kayıtlarını temizler.
     *
     * @param playerId Oyuncu UUID
     */
    public void clearPlayerRecords(UUID playerId) {
        // Bellekten temizle
        playerRecords.remove(playerId);
        
        if (storageType.equals("mysql")) {
            // Veritabanından da temizle
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.clearPlayerRecords(playerId);
            });
        } else {
            // Dosyaya kaydet
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    /**
     * Tüm küfür kayıtlarını temizler.
     */
    public void clearAllRecords() {
        // Bellekten temizle
        playerRecords.clear();
        
        if (storageType.equals("mysql")) {
            // Veritabanından da temizle
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                databaseManager.clearAllRecords();
            });
        } else {
            // Dosyaya kaydet
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveData);
        }
    }
    
    /**
     * Belirli bir süre öncesine ait kayıtları temizler.
     */
    private void cleanupOldData() {
        if (dataRetentionDays <= 0) {
            return; // Sınırsız saklama, temizlik yapma
        }
        
        LocalDateTime cutoffDate = LocalDateTime.now().minus(dataRetentionDays, ChronoUnit.DAYS);
        
        if (storageType.equals("mysql")) {
            // Veritabanından temizle
            databaseManager.cleanupOldData(dataRetentionDays);
            
            // Tam bir veritabanı yüklemesi yaparak belleği güncelle
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            playerRecords.clear();
            playerRecords.putAll(dbRecords);
        } else {
            // Dosya tabanlı temizlik
            boolean dataChanged = false;
            
            // Her oyuncu için kayıtları kontrol et
            for (UUID playerId : new HashSet<>(playerRecords.keySet())) {
                List<ProfanityRecord> records = playerRecords.get(playerId);
                
                if (records != null) {
                    int initialSize = records.size();
                    
                    // Kesim tarihinden önce olan kayıtları filtrele
                    List<ProfanityRecord> filteredRecords = records.stream()
                            .filter(record -> record.getTimestamp().isAfter(cutoffDate))
                            .collect(Collectors.toList());
                    
                    if (filteredRecords.size() < initialSize) {
                        playerRecords.put(playerId, filteredRecords);
                        dataChanged = true;
                    }
                    
                    // Eğer hiç kayıt kalmadıysa, oyuncuyu listeden kaldır
                    if (filteredRecords.isEmpty()) {
                        playerRecords.remove(playerId);
                    }
                }
            }
            
            // Eğer veri değiştiyse kaydet
            if (dataChanged) {
                saveData();
                logger.info(dataRetentionDays + " günden eski küfür kayıtları temizlendi.");
            }
        }
    }
    
    /**
     * Verileri disk üzerine kaydeder.
     */
    private void saveData() {
        // MySQL'de saklıyorsak bu metodu kullanmaya gerek yok
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
    
    /**
     * Verileri diskten yükler.
     */
    @SuppressWarnings("unchecked")
    private void loadData() {
        if (storageType.equals("mysql")) {
            // Veritabanından yükle
            Map<UUID, List<ProfanityRecord>> dbRecords = databaseManager.getAllRecords();
            playerRecords.clear();
            playerRecords.putAll(dbRecords);
            logger.info("Küfür kayıtları MySQL veritabanından başarıyla yüklendi.");
            return;
        }
        
        // Dosyadan yükle
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
    
    /**
     * Yapılandırma değiştiğinde verileri yeniden yükler.
     */
    public void reload() {
        playerRecords.clear();
        loadData();
    }
    
    /**
     * Eklenti kapatılırken yapılacak işlemler.
     */
    public void shutdown() {
        // Veritabanı bağlantısını kapat
        if (storageType.equals("mysql")) {
            databaseManager.close();
        } else {
            // Son kayıtları dosyaya yaz
            saveData();
        }
    }
    
    /**
     * Veriyi serileştirilmiş bir formata dönüştürür.
     *
     * @return Serileştirilmiş veri Map'i
     */
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
    
    /**
     * Serileştirilmiş veriyi ProfanityRecord nesnelerine dönüştürür.
     *
     * @param data Serileştirilmiş veri
     */
    @SuppressWarnings("unchecked")
    private void deserializeData(Map<String, Object> data) {
        // Şu an için temel deserializasyon işlemi - ileride genişletilebilir
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
