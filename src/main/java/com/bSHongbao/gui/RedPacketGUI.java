package com.bSHongbao.gui;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.model.RedPacket;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 红包GUI界面
 * Red Packet GUI Interface
 */
public class RedPacketGUI {
    private final BSHongbao plugin;
    private final Map<UUID, RedPacketCreationSession> creationSessions;
    
    public RedPacketGUI(BSHongbao plugin) {
        this.plugin = plugin;
        this.creationSessions = new HashMap<>();
    }
    
    /**
     * 打开主界面
     */
    public void openMainGUI(Player player) {
        String title = plugin.getConfigManager().getGuiTitle();
        Inventory inventory = Bukkit.createInventory(null, 27, title); // 9x3 = 27 slots
        
        // 填充装饰物品
        fillDecorationItems(inventory);
        
        // 添加普通红包选项
        ItemStack normalPacket = createNormalPacketItem();
        int normalSlot = plugin.getConfigManager().getItemSlot("items.normal-packet.slot");
        inventory.setItem(normalSlot, normalPacket);
        
        // 添加拼手气红包选项
        ItemStack luckyPacket = createLuckyPacketItem();
        int luckySlot = plugin.getConfigManager().getItemSlot("items.lucky-packet.slot");
        inventory.setItem(luckySlot, luckyPacket);
        
        player.openInventory(inventory);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Opened red packet GUI for player: " + player.getName());
        }
    }
    
    /**
     * 填充装饰物品
     */
    private void fillDecorationItems(Inventory inventory) {
        String materialName = plugin.getConfigManager().getItemMaterial("items.decoration.material");
        Material material;
        
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        
        ItemStack decoration = new ItemStack(material);
        ItemMeta meta = decoration.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName("items.decoration.name"));
            decoration.setItemMeta(meta);
        }
        
        // 填充除了红包选项位置外的所有槽位
        int normalSlot = plugin.getConfigManager().getItemSlot("items.normal-packet.slot");
        int luckySlot = plugin.getConfigManager().getItemSlot("items.lucky-packet.slot");
        
        for (int i = 0; i < 27; i++) {
            if (i != normalSlot && i != luckySlot) {
                inventory.setItem(i, decoration);
            }
        }
    }
    
    /**
     * 创建普通红包物品
     */
    private ItemStack createNormalPacketItem() {
        String materialName = plugin.getConfigManager().getItemMaterial("items.normal-packet.material");
        Material material;
        
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.RED_WOOL;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName("items.normal-packet.name"));
            String[] lore = plugin.getConfigManager().getItemLore("items.normal-packet.lore");
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 创建拼手气红包物品
     */
    private ItemStack createLuckyPacketItem() {
        String materialName = plugin.getConfigManager().getItemMaterial("items.lucky-packet.material");
        Material material;
        
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.GOLD_INGOT;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName("items.lucky-packet.name"));
            String[] lore = plugin.getConfigManager().getItemLore("items.lucky-packet.lore");
            meta.setLore(Arrays.asList(lore));
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 处理GUI点击事件
     */
    public void handleClick(Player player, int slot, ItemStack clickedItem) {
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        
        int normalSlot = plugin.getConfigManager().getItemSlot("items.normal-packet.slot");
        int luckySlot = plugin.getConfigManager().getItemSlot("items.lucky-packet.slot");
        
        if (slot == normalSlot) {
            // 点击普通红包
            startRedPacketCreation(player, RedPacket.RedPacketType.NORMAL);
        } else if (slot == luckySlot) {
            // 点击拼手气红包
            startRedPacketCreation(player, RedPacket.RedPacketType.LUCKY);
        }
        
        player.closeInventory();
    }
    
    /**
     * 开始红包创建流程
     */
    private void startRedPacketCreation(Player player, RedPacket.RedPacketType type) {
        UUID playerId = player.getUniqueId();
        
        // 创建会话
        RedPacketCreationSession session = new RedPacketCreationSession(type);
        creationSessions.put(playerId, session);
        
        // 提示输入金额
        String message = plugin.getConfigManager().getMessage("messages.prompts.enter-amount");
        String cancelHint = plugin.getConfigManager().getRawMessage("messages.prompts.cancel-hint");
        
        player.sendMessage(message);
        player.sendMessage(cancelHint);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Started " + type.name() + " red packet creation for player: " + player.getName());
        }
    }
    
    /**
     * 处理聊天输入
     */
    public boolean handleChatInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        RedPacketCreationSession session = creationSessions.get(playerId);
        
        if (session == null) {
            return false; // 没有活跃的创建会话
        }
        
        // 检查是否取消
        if ("cancel".equalsIgnoreCase(message.trim())) {
            creationSessions.remove(playerId);
            player.sendMessage(plugin.getConfigManager().getMessage("messages.success.packet-created").replace("成功创建红包！", "已取消红包创建"));
            return true;
        }
        
        if (session.getStep() == RedPacketCreationSession.Step.WAITING_FOR_AMOUNT) {
            return handleAmountInput(player, session, message);
        } else if (session.getStep() == RedPacketCreationSession.Step.WAITING_FOR_COUNT) {
            return handleCountInput(player, session, message);
        }
        
        return false;
    }
    
    /**
     * 处理金额输入
     */
    private boolean handleAmountInput(Player player, RedPacketCreationSession session, String message) {
        java.math.BigDecimal amount = plugin.getEconomyManager().parseAmount(message.trim());
        
        if (amount == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.invalid-amount"));
            return true;
        }
        
        // 检查最小金额
        java.math.BigDecimal minAmount = plugin.getConfigManager().getMinTotalAmount();
        if (amount.compareTo(minAmount) < 0) {
            String errorMsg = plugin.getConfigManager().getMessage("messages.errors.amount-too-low", "{min}", minAmount.toString());
            player.sendMessage(errorMsg);
            return true;
        }
        
        // 检查余额
        if (!plugin.getEconomyManager().hasEnough(player, amount)) {
            String errorMsg = plugin.getConfigManager().getMessage("messages.errors.insufficient-funds", "{amount}", amount.toString());
            player.sendMessage(errorMsg);
            return true;
        }
        
        session.setAmount(amount);
        session.setStep(RedPacketCreationSession.Step.WAITING_FOR_COUNT);
        
        // 提示输入份数
        String promptMsg = plugin.getConfigManager().getMessage("messages.prompts.enter-count");
        String cancelHint = plugin.getConfigManager().getRawMessage("messages.prompts.cancel-hint");
        
        player.sendMessage(promptMsg);
        player.sendMessage(cancelHint);
        
        return true;
    }
    
    /**
     * 处理份数输入
     */
    private boolean handleCountInput(Player player, RedPacketCreationSession session, String message) {
        Integer count = plugin.getEconomyManager().parseCount(message.trim());
        
        if (count == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.invalid-count"));
            return true;
        }
        
        // 检查最大份数
        int maxCount = plugin.getConfigManager().getMaxPacketCount();
        if (count > maxCount) {
            String errorMsg = plugin.getConfigManager().getMessage("messages.errors.count-too-high", "{max}", String.valueOf(maxCount));
            player.sendMessage(errorMsg);
            return true;
        }
        
        // 完成红包创建
        completeRedPacketCreation(player, session, count);
        creationSessions.remove(player.getUniqueId());
        
        return true;
    }
    
    /**
     * 完成红包创建
     */
    private void completeRedPacketCreation(Player player, RedPacketCreationSession session, int count) {
        java.math.BigDecimal amount = session.getAmount();
        RedPacket.RedPacketType type = session.getType();
        
        // 扣除金额
        if (!plugin.getEconomyManager().withdraw(player, amount)) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.insufficient-funds", "{amount}", amount.toString()));
            return;
        }
        
        // 创建红包
        RedPacket packet = plugin.getRedPacketManager().createRedPacket(
                player.getUniqueId().toString(),
                player.getName(),
                type,
                amount,
                count
        );
        
        // 发送成功消息
        String successMsg = plugin.getConfigManager().getMessage("messages.success.packet-created", 
                "{amount}", amount.toString(),
                "{count}", String.valueOf(count));
        player.sendMessage(successMsg);
        
        // 广播红包消息
        plugin.getChatManager().broadcastRedPacket(packet);
    }
    
    /**
     * 获取创建会话
     */
    public RedPacketCreationSession getCreationSession(UUID playerId) {
        return creationSessions.get(playerId);
    }
    
    /**
     * 移除创建会话
     */
    public void removeCreationSession(UUID playerId) {
        creationSessions.remove(playerId);
    }
    
    /**
     * 清理所有会话
     */
    public void clearAllSessions() {
        creationSessions.clear();
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Cleared all GUI sessions");
        }
    }
    
    /**
     * 清理所有会话
     */
    public void cleanup() {
        creationSessions.clear();
    }
}