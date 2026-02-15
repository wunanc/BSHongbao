package com.bSHongbao.manager;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.model.RedPacket;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.math.BigDecimal;

/**
 * 聊天管理器
 * Chat Manager
 */
public class ChatManager {
    private final BSHongbao plugin;
    
    public ChatManager(BSHongbao plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 广播红包消息
     */
    public void broadcastRedPacket(RedPacket packet) {
        String message = plugin.getConfigManager().getRawMessage("messages.chat.packet-sent")
                .replace("{player}", packet.getSenderName())
                .replace("{type}", packet.getType().getDisplayName());
        
        // 创建可点击的消息
        TextComponent mainMessage = new TextComponent(message.replace("[点击领取]", ""));
        
        TextComponent clickableText = new TextComponent("[点击领取]");
        clickableText.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        clickableText.setBold(true);
        
        // 设置点击事件
        clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bshongbao_claim " + packet.getId()));
        
        // 设置悬停提示
        ComponentBuilder hoverBuilder = new ComponentBuilder("点击领取红包\n")
                .color(net.md_5.bungee.api.ChatColor.YELLOW)
                .append("类型: " + packet.getType().getDisplayName() + "\n")
                .color(net.md_5.bungee.api.ChatColor.WHITE)
                .append("总金额: " + plugin.getEconomyManager().formatAmount(packet.getTotalAmount()) + "\n")
                .color(net.md_5.bungee.api.ChatColor.WHITE)
                .append("总份数: " + packet.getTotalCount() + "\n")
                .color(net.md_5.bungee.api.ChatColor.WHITE)
                .append("剩余: " + packet.getRemainingCount() + " 份")
                .color(net.md_5.bungee.api.ChatColor.GRAY);
        
        clickableText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverBuilder.create()));
        
        // 组合消息
        TextComponent fullMessage = new TextComponent(mainMessage);
        fullMessage.addExtra(clickableText);
        
        // 发送给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(fullMessage);
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Broadcasted red packet: " + packet.getId() + " by " + packet.getSenderName());
        }
    }
    
    /**
     * 广播红包被领取的消息
     */
    public void broadcastPacketClaimed(RedPacket packet, String claimerName, BigDecimal amount) {
        String message = plugin.getConfigManager().getRawMessage("messages.chat.packet-claimed")
                .replace("{claimer}", claimerName)
                .replace("{sender}", packet.getSenderName())
                .replace("{type}", packet.getType().getDisplayName())
                .replace("{amount}", plugin.getEconomyManager().formatAmount(amount));
        
        // 发送给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info(claimerName + " claimed red packet " + packet.getId() + " for " + amount);
        }
    }
    
    /**
     * 处理红包领取命令
     */
    public boolean handleClaimCommand(Player player, String packetId) {
        RedPacket packet = plugin.getRedPacketManager().getRedPacket(packetId);
        
        if (packet == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.packet-not-found"));
            return true;
        }
        
        // 检查是否是自己发的红包
        if (packet.getSenderId().equals(player.getUniqueId().toString())) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.already-claimed").replace("您已经领取过这个红包了！", "不能领取自己发的红包！"));
            return true;
        }
        
        // 尝试领取红包
        BigDecimal amount = plugin.getRedPacketManager().claimRedPacket(packetId, player.getUniqueId().toString());
        
        if (amount == null) {
            // 检查具体原因
            if (packet.isExpired()) {
                player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.packet-not-found"));
            } else if (packet.getClaimedPlayers().containsKey(player.getUniqueId().toString())) {
                player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.already-claimed"));
            } else if (packet.isFullyClaimed()) {
                player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.packet-not-found").replace("红包不存在或已过期！", "红包已被领完！"));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.packet-not-found"));
            }
            return true;
        }
        
        // 发放金额
        if (plugin.getEconomyManager().deposit(player, amount)) {
            // 发送成功消息给领取者
            String successMsg = plugin.getConfigManager().getMessage("messages.success.packet-claimed", 
                    "{amount}", plugin.getEconomyManager().formatAmount(amount));
            player.sendMessage(successMsg);
            
            // 广播领取消息
            broadcastPacketClaimed(packet, player.getName(), amount);
            
            return true;
        } else {
            // 存款失败，需要退还到红包
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.invalid-amount").replace("请输入有效的金额！", "系统错误，请联系管理员！"));
            plugin.getLogger().severe("Failed to deposit " + amount + " to player " + player.getName() + " for red packet " + packetId);
            return true;
        }
    }
    
    /**
     * 发送红包过期通知
     */
    public void sendExpirationNotice(Player player, BigDecimal refundAmount) {
        String message = plugin.getConfigManager().getMessage("messages.chat.packet-expired", 
                "{amount}", plugin.getEconomyManager().formatAmount(refundAmount));
        player.sendMessage(message);
    }
    
    /**
     * 格式化消息颜色
     */
    public String formatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    /**
     * 发送带前缀的消息
     */
    public void sendPrefixedMessage(Player player, String message) {
        player.sendMessage(plugin.getConfigManager().getPrefix() + formatMessage(message));
    }
    
    /**
     * 广播带前缀的消息
     */
    public void broadcastPrefixedMessage(String message) {
        String formattedMessage = plugin.getConfigManager().getPrefix() + formatMessage(message);
        Bukkit.broadcastMessage(formattedMessage);
    }
}