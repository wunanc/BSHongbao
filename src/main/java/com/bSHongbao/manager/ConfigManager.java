package com.bSHongbao.manager;

import com.bSHongbao.BSHongbao;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;

/**
 * 配置管理器
 * Configuration Manager
 */
public class ConfigManager {
    private final BSHongbao plugin;
    private FileConfiguration config;
    
    public ConfigManager(BSHongbao plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载配置文件
     */
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        plugin.getLogger().info("Configuration loaded successfully");
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        loadConfig();
    }
    
    /**
     * 获取最小红包总金额
     */
    public BigDecimal getMinTotalAmount() {
        double amount = config.getDouble("redpacket.min-total-amount", 1000.0);
        return BigDecimal.valueOf(amount);
    }
    
    /**
     * 获取最大红包份数
     */
    public int getMaxPacketCount() {
        return config.getInt("redpacket.max-packet-count", 50);
    }
    
    /**
     * 获取红包过期时间（分钟）
     */
    public long getExpirationMinutes() {
        return config.getLong("redpacket.expiration-minutes", 5);
    }
    
    /**
     * 获取最小单个红包金额
     */
    public BigDecimal getMinSingleAmount() {
        double amount = config.getDouble("redpacket.min-single-amount", 0.01);
        return BigDecimal.valueOf(amount);
    }
    
    /**
     * 获取消息前缀
     */
    public String getPrefix() {
        String prefix = config.getString("messages.prefix", "&6[红包] &r");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public String getSimplePrefix() {
        return config.getString("messages.simple-prefix", "&6[红包] &r");
    }
    
    /**
     * 获取GUI标题
     */
    public String getGuiTitle() {
        String title = config.getString("messages.gui.main-title", "&c&l红包系统");
        return ChatColor.translateAlternateColorCodes('&', title);
    }
    
    /**
     * 获取消息并格式化
     */
    public String getMessage(String path) {
        String message = config.getString(path, "消息语句未找到: " + path + "尝试删除配置文件重新生成或者是在github上找到缺失部分");
        return getPrefix() + ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取消息并替换占位符
     */
    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        
        // 替换占位符
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        
        return message;
    }
    
    /**
     * 获取原始消息（不带前缀）
     */
    public String getRawMessage(String path) {
        String message = config.getString(path, "消息语句未找到: " + path + "尝试删除配置文件重新生成或者是在github上找到缺失部分");
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 获取物品材质
     */
    public String getItemMaterial(String path) {
        return config.getString(path, "STONE");
    }
    
    /**
     * 获取物品名称
     */
    public String getItemName(String path) {
        String name = config.getString(path, "Unknown Item");
        return ChatColor.translateAlternateColorCodes('&', name);
    }
    
    /**
     * 获取物品描述
     */
    public String[] getItemLore(String path) {
        java.util.List<String> loreList = config.getStringList(path);
        String[] lore = new String[loreList.size()];
        
        for (int i = 0; i < loreList.size(); i++) {
            lore[i] = ChatColor.translateAlternateColorCodes('&', loreList.get(i));
        }
        
        return lore;
    }
    
    /**
     * 获取物品槽位
     */
    public int getItemSlot(String path) {
        return config.getInt(path, 0);
    }
    
    /**
     * 检查是否启用调试模式
     */
    public boolean isDebugEnabled() {
        return config.getBoolean("debug", false);
    }
    
    /**
     * 获取数据库类型
     */
    public String getDatabaseType() {
        return config.getString("database.type", "file");
    }
    
    /**
     * 获取文件数据库路径
     */
    public String getFileDatabasePath() {
        return config.getString("database.file.path", "data/redpackets.yml");
    }
    
    /**
     * 验证配置完整性
     */
    public boolean validateConfig() {
        boolean valid = true;
        
        // 检查必要的配置项
        if (getMinTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            plugin.getLogger().warning("Invalid min-total-amount in config: " + getMinTotalAmount());
            valid = false;
        }
        
        if (getMaxPacketCount() <= 0) {
            plugin.getLogger().warning("Invalid max-packet-count in config: " + getMaxPacketCount());
            valid = false;
        }
        
        if (getExpirationMinutes() <= 0) {
            plugin.getLogger().warning("Invalid expiration-minutes in config: " + getExpirationMinutes());
            valid = false;
        }
        
        if (getMinSingleAmount().compareTo(BigDecimal.ZERO) <= 0) {
            plugin.getLogger().warning("Invalid min-single-amount in config: " + getMinSingleAmount());
            valid = false;
        }
        
        return valid;
    }
}