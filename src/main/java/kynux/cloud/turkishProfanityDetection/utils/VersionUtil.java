package kynux.cloud.turkishProfanityDetection.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

/**
 * Farklı Minecraft sürümleri için uyumluluk sağlayan yardımcı sınıf.
 */
public class VersionUtil {
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    
    // Sürüm bilgilerini hemen yükle
    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String[] versionParts = version.substring(1).split("_");
        
        MAJOR_VERSION = Integer.parseInt(versionParts[0]);
        MINOR_VERSION = Integer.parseInt(versionParts[1]);
    }
    
    /**
     * Sunucunun kullandığı Minecraft sürümünün ana numarasını döndürür.
     *
     * @return Sürüm ana numarası (örn. 1.8.8 için 1)
     */
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    /**
     * Sunucunun kullandığı Minecraft sürümünün alt numarasını döndürür.
     *
     * @return Sürüm alt numarası (örn. 1.8.8 için 8)
     */
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }
    
    /**
     * Sürüm 1.13 veya daha yeni mi kontrol eder.
     *
     * @return 1.13+ sürüm ise true
     */
    public static boolean isVersion1_13Plus() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 13);
    }
    
    /**
     * Sürüm 1.8 veya daha yeni mi kontrol eder.
     *
     * @return 1.8+ sürüm ise true
     */
    public static boolean isVersion1_8Plus() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 8);
    }
    
    /**
     * Materyal ismini sürüme göre dönüştürür.
     *
     * @param newMaterial 1.13+ sürüm materyal adı
     * @param legacyMaterial 1.13 öncesi sürüm materyal adı
     * @param data 1.13 öncesi için data değeri
     * @return Uygun Material
     */
    @SuppressWarnings("deprecation")
    public static Material getMaterialByVersion(String newMaterial, String legacyMaterial, byte data) {
        if (isVersion1_13Plus()) {
            try {
                return Material.valueOf(newMaterial);
            } catch (IllegalArgumentException e) {
                try {
                    // Eğer ismi belirtilen materyal bulunamazsa, alternatif materyal döndür
                    return Material.valueOf(legacyMaterial);
                } catch (IllegalArgumentException ex) {
                    return Material.STONE; // En son çare
                }
            }
        } else {
            try {
                return Material.valueOf(legacyMaterial);
            } catch (IllegalArgumentException e) {
                return Material.STONE; // En son çare
            }
        }
    }
    
    /**
     * Oyuncu kafası materyal tipini döndürür (1.8+ sürümler).
     *
     * @return Oyuncu kafası materyal tipi
     */
    public static Material getPlayerHeadMaterial() {
        return getMaterialByVersion("PLAYER_HEAD", "SKULL_ITEM", (byte) 3);
    }
    
    /**
     * Bariyer materyal tipini döndürür.
     *
     * @return Bariyer materyal tipi
     */
    public static Material getBarrierMaterial() {
        return getMaterialByVersion("BARRIER", "BEDROCK", (byte) 0);
    }
    
    /**
     * Tekrarlayıcı materyal tipini döndürür.
     *
     * @return Tekrarlayıcı materyal tipi
     */
    public static Material getRepeaterMaterial() {
        return getMaterialByVersion("REPEATER", "DIODE", (byte) 0);
    }
    
    /**
     * Kitap materyal tipini döndürür.
     *
     * @return Kitap materyal tipi
     */
    public static Material getBookMaterial() {
        return getMaterialByVersion("BOOK", "BOOK", (byte) 0);
    }
    
    /**
     * Siyah cam panel materyal tipini döndürür.
     *
     * @return Siyah cam panel materyal tipi
     */
    public static Material getBlackGlassPaneMaterial() {
        return getMaterialByVersion("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (byte) 15);
    }
    
    /**
     * Ok materyal tipini döndürür.
     *
     * @return Ok materyal tipi
     */
    public static Material getArrowMaterial() {
        return getMaterialByVersion("ARROW", "ARROW", (byte) 0);
    }
    
    /**
     * Belirli bir renkte yün materyal tipini döndürür.
     *
     * @param color Renk (RED, ORANGE, YELLOW, LIME, GREEN)
     * @return Renkli yün materyal tipi
     */
    public static Material getColoredWoolMaterial(String color) {
        byte data = 0;
        switch (color.toUpperCase()) {
            case "RED":
                data = 14;
                break;
            case "ORANGE":
                data = 1;
                break;
            case "YELLOW":
                data = 4;
                break;
            case "LIME":
                data = 5;
                break;
            case "GREEN":
                data = 13;
                break;
            default:
                data = 0; // Beyaz yün
        }
        
        return getMaterialByVersion(color + "_WOOL", "WOOL", data);
    }
    
    /**
     * Oyuncu kafası item'i oluşturur ve oyuncu ismini atar.
     *
     * @param playerName Oyuncu adı
     * @return Oyuncu kafası ItemStack'i
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createPlayerHead(String playerName) {
        Material material = getPlayerHeadMaterial();
        ItemStack item = isVersion1_13Plus() ? new ItemStack(material) : new ItemStack(material, 1, (short) 3);
        
        try {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                if (isVersion1_8Plus()) {
                    meta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
                } else {
                    meta.setOwner(playerName);
                }
                item.setItemMeta(meta);
            }
        } catch (Exception e) {
            // İşlem başarısız olursa sessizce devam et
        }
        
        return item;
    }
    
    /**
     * 1.13 öncesi sürümler için data değeri ile item oluşturur.
     *
     * @param material Materyal
     * @param data Data değeri
     * @return Oluşturulan ItemStack
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createItem(@NotNull Material material, byte data) {
        if (isVersion1_13Plus()) {
            return new ItemStack(material);
        } else {
            return new ItemStack(material, 1, data);
        }
    }
    
    /**
     * Sürüme göre uygun ItemStack oluşturur.
     *
     * @param newMaterial 1.13+ sürüm materyal adı
     * @param legacyMaterial 1.13 öncesi sürüm materyal adı
     * @param data 1.13 öncesi için data değeri
     * @return Oluşturulan ItemStack
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createItemByVersion(String newMaterial, String legacyMaterial, byte data) {
        Material material = getMaterialByVersion(newMaterial, legacyMaterial, data);
        
        if (isVersion1_13Plus()) {
            return new ItemStack(material);
        } else {
            return new ItemStack(material, 1, data);
        }
    }
}
