package com.bSHongbao;

import com.bSHongbao.command.RedPacketCommand;
import com.bSHongbao.listener.PlayerListener;
import com.bSHongbao.manager.*;
import com.bSHongbao.gui.RedPacketGUI;
import com.bSHongbao.task.RedPacketTask;
import com.bSHongbao.util.PlgColor;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class BSHongbao extends JavaPlugin {
    
    // 管理器实例
    private ConfigManager configManager;
    private EconomyManager economyManager;
    private RedPacketManager redPacketManager;
    private ChatManager chatManager;
    private RedPacketGUI redPacketGUI;
    
    // 任务实例
    private RedPacketTask redPacketTask;
    
    // Vault Economy
    private Economy economy;

    @Override
    public void onEnable() {
        // 检查Vault插件
        if (!setupEconomy()) {
            getLogger().severe("未找到Vault插件或经济插件！插件将被禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        if (!configManager.validateConfig()) {
            getLogger().severe("配置文件验证失败！插件将被禁用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        PlgColor.init(this);

        // 初始化其他管理器
        economyManager = new EconomyManager(this);
        redPacketManager = new RedPacketManager(this);
        chatManager = new ChatManager(this);
        redPacketGUI = new RedPacketGUI(this);
        
        // 注册命令
        RedPacketCommand commandExecutor = new RedPacketCommand(this);
        getCommand("BSHongbao").setExecutor(commandExecutor);
        getCommand("BSHongbao").setTabCompleter(commandExecutor);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        // 启动定时任务
        redPacketTask = new RedPacketTask(this);
        redPacketTask.start();
        
        getLogger().info("BSHongbao 插件已启用！版本: " + getDescription().getVersion());
        
        if (configManager.isDebugEnabled()) {
            getLogger().info("调试模式已启用");
        }
    }

    @Override
    public void onDisable() {
        // 停止定时任务
        if (redPacketTask != null) {
            redPacketTask.stop();
        }
        
        // 处理所有过期红包的退款
        if (redPacketManager != null) {
            redPacketManager.processAllRefunds();
        }
        
        // 清理GUI会话
        if (redPacketGUI != null) {
            redPacketGUI.clearAllSessions();
        }
        
        getLogger().info("BSHongbao 插件已禁用！");
    }
    
    /**
     * 设置Vault经济系统
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        
        economy = rsp.getProvider();
        return economy != null;
    }
    
    // Getter方法
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public RedPacketManager getRedPacketManager() {
        return redPacketManager;
    }
    
    public ChatManager getChatManager() {
        return chatManager;
    }
    
    public RedPacketGUI getRedPacketGUI() {
        return redPacketGUI;
    }
    
    public Economy getEconomy() {
        return economy;
    }
}
