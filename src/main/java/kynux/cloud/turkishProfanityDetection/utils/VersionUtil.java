package kynux.cloud.turkishProfanityDetection.utils;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

public class VersionUtil {
    private static final int MAJOR_VERSION;
    private static final int MINOR_VERSION;
    
    static {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        String[] versionParts = version.substring(1).split("_");
        
        MAJOR_VERSION = Integer.parseInt(versionParts[0]);
        MINOR_VERSION = Integer.parseInt(versionParts[1]);
    }
    
    public static int getMajorVersion() {
        return MAJOR_VERSION;
    }
    
    public static int getMinorVersion() {
        return MINOR_VERSION;
    }
    
    public static boolean isVersion1_13Plus() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 13);
    }
    
    public static boolean isVersion1_8Plus() {
        return MAJOR_VERSION > 1 || (MAJOR_VERSION == 1 && MINOR_VERSION >= 8);
    }
    
    @SuppressWarnings("deprecation")
    public static Material getMaterialByVersion(String newMaterial, String legacyMaterial, byte data) {
        if (isVersion1_13Plus()) {
            try {
                return Material.valueOf(newMaterial);
            } catch (IllegalArgumentException e) {
                try {
                    return Material.valueOf(legacyMaterial);
                } catch (IllegalArgumentException ex) {
                    return Material.STONE;
                }
            }
        } else {
            try {
                return Material.valueOf(legacyMaterial);
            } catch (IllegalArgumentException e) {
                return Material.STONE;
            }
        }
    }
    
    public static Material getPlayerHeadMaterial() {
        return getMaterialByVersion("PLAYER_HEAD", "SKULL_ITEM", (byte) 3);
    }
    
    public static Material getBarrierMaterial() {
        return getMaterialByVersion("BARRIER", "BEDROCK", (byte) 0);
    }
    
    public static Material getRepeaterMaterial() {
        return getMaterialByVersion("REPEATER", "DIODE", (byte) 0);
    }
    
    public static Material getBookMaterial() {
        return getMaterialByVersion("BOOK", "BOOK", (byte) 0);
    }
    
    public static Material getBlackGlassPaneMaterial() {
        return getMaterialByVersion("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE", (byte) 15);
    }
    
    public static Material getArrowMaterial() {
        return getMaterialByVersion("ARROW", "ARROW", (byte) 0);
    }
    
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
                data = 0;
        }
        
        return getMaterialByVersion(color + "_WOOL", "WOOL", data);
    }
    
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
        }
        
        return item;
    }
    
    @SuppressWarnings("deprecation")
    public static ItemStack createItem(@NotNull Material material, byte data) {
        if (isVersion1_13Plus()) {
            return new ItemStack(material);
        } else {
            return new ItemStack(material, 1, data);
        }
    }
    
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
