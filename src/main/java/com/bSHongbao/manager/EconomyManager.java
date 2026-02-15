package com.bSHongbao.manager;

import com.bSHongbao.BSHongbao;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 经济系统管理器
 * Economy System Manager
 */
public class EconomyManager {
    private final BSHongbao plugin;
    private Economy economy;
    private boolean economyEnabled;
    
    public EconomyManager(BSHongbao plugin) {
        this.plugin = plugin;
        this.economyEnabled = false;
        setupEconomy();
    }
    
    /**
     * 设置经济系统
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found! Economy features will be disabled.");
            return;
        }
        
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy plugin found! Please install an economy plugin like EssentialsX.");
            return;
        }
        
        economy = rsp.getProvider();
        economyEnabled = true;
        
        plugin.getLogger().info("Economy system initialized with: " + economy.getName());
    }
    
    /**
     * 检查经济系统是否可用
     */
    public boolean isEconomyEnabled() {
        return economyEnabled && economy != null;
    }
    
    /**
     * 获取玩家余额
     */
    public BigDecimal getBalance(Player player) {
        if (!isEconomyEnabled()) {
            return BigDecimal.ZERO;
        }
        
        try {
            double balance = economy.getBalance(player);
            return BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get balance for player " + player.getName() + ": " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 检查玩家是否有足够余额
     */
    public boolean hasEnough(Player player, BigDecimal amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        try {
            return economy.has(player, amount.doubleValue());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check balance for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从玩家账户扣除金额
     */
    public boolean withdraw(Player player, BigDecimal amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        try {
            net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount.doubleValue());
            
            if (response.transactionSuccess()) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Withdrew " + amount + " from " + player.getName() + ". New balance: " + response.balance);
                }
                return true;
            } else {
                plugin.getLogger().warning("Failed to withdraw " + amount + " from " + player.getName() + ": " + response.errorMessage);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Exception during withdrawal for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 向玩家账户存入金额
     */
    public boolean deposit(Player player, BigDecimal amount) {
        if (!isEconomyEnabled()) {
            return false;
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        
        try {
            net.milkbowl.vault.economy.EconomyResponse response = economy.depositPlayer(player, amount.doubleValue());
            
            if (response.transactionSuccess()) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Deposited " + amount + " to " + player.getName() + ". New balance: " + response.balance);
                }
                return true;
            } else {
                plugin.getLogger().warning("Failed to deposit " + amount + " to " + player.getName() + ": " + response.errorMessage);
                return false;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Exception during deposit for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 格式化金额显示
     */
    public String formatAmount(BigDecimal amount) {
        if (!isEconomyEnabled()) {
            return amount.toString();
        }
        
        try {
            return economy.format(amount.doubleValue());
        } catch (Exception e) {
            return amount.toString();
        }
    }
    
    /**
     * 获取货币名称（单数）
     */
    public String getCurrencyNameSingular() {
        if (!isEconomyEnabled()) {
            return "金币";
        }
        
        try {
            String name = economy.currencyNameSingular();
            return name != null && !name.isEmpty() ? name : "金币";
        } catch (Exception e) {
            return "金币";
        }
    }
    
    /**
     * 获取货币名称（复数）
     */
    public String getCurrencyNamePlural() {
        if (!isEconomyEnabled()) {
            return "金币";
        }
        
        try {
            String name = economy.currencyNamePlural();
            return name != null && !name.isEmpty() ? name : "金币";
        } catch (Exception e) {
            return "金币";
        }
    }
    
    /**
     * 验证金额格式
     */
    public BigDecimal parseAmount(String amountStr) {
        try {
            BigDecimal amount = new BigDecimal(amountStr).setScale(2, RoundingMode.HALF_UP);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 验证数量格式
     */
    public Integer parseCount(String countStr) {
        try {
            int count = Integer.parseInt(countStr);
            if (count <= 0) {
                return null;
            }
            return count;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}