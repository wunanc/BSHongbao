package com.bSHongbao.manager;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.model.RedPacket;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 红包管理器
 * Red Packet Manager
 */
public class RedPacketManager {
    private final BSHongbao plugin;
    private final Map<String, RedPacket> activePackets;
    private final Map<String, List<String>> playerPendingRefunds;
    
    public RedPacketManager(BSHongbao plugin) {
        this.plugin = plugin;
        this.activePackets = new ConcurrentHashMap<>();
        this.playerPendingRefunds = new ConcurrentHashMap<>();
        
        // 过期检查由 RedPacketTask 统一调度，避免重复处理
    }
    
    /**
     * 创建红包
     */
    public RedPacket createRedPacket(String senderId, String senderName, 
                                   RedPacket.RedPacketType type, 
                                   BigDecimal totalAmount, int count) {
        long expireMinutes = plugin.getConfig().getLong("redpacket.expiration-minutes", 5);
        RedPacket packet = new RedPacket(senderId, senderName, type, totalAmount, count, expireMinutes);
        activePackets.put(packet.getId(), packet);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Created red packet: " + packet.getId() + " by " + senderName);
        }
        
        return packet;
    }
    
    /**
     * 领取红包
     */
    public BigDecimal claimRedPacket(String packetId, String playerId) {
        RedPacket packet = activePackets.get(packetId);
        if (packet == null) {
            return null;
        }
        
        if (packet.isExpired()) {
            // 使用原子移除与处理，避免重复退款
            processExpiredPacket(packetId);
            return null;
        }
        
        BigDecimal amount = packet.claim(playerId);
        
        // 如果红包被领完，从活跃列表中移除
        if (packet.isFullyClaimed()) {
            activePackets.remove(packetId);
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Red packet " + packetId + " fully claimed");
            }
        }
        
        return amount;
    }
    
    /**
     * 获取红包
     */
    public RedPacket getRedPacket(String packetId) {
        return activePackets.get(packetId);
    }
    
    /**
     * 检查红包是否存在
     */
    public boolean hasRedPacket(String packetId) {
        return activePackets.containsKey(packetId);
    }
    
    /**
     * 处理过期红包（原子）
     * 通过从 activePackets 中原子移除，确保只处理一次，避免重复退款
     */
    public void processExpiredPacket(String packetId) {
        RedPacket packet = activePackets.remove(packetId);
        if (packet == null) {
            return; // 已被其他流程处理
        }
        
        // 再次校验过期状态（防止非过期被误处理）
        if (!packet.isExpired()) {
            // 放回 map（如果此刻又被领取完也没关系）
            activePackets.putIfAbsent(packetId, packet);
            return;
        }
        
        handleExpiredPacket(packet);
    }
    
    /**
     * 实际的过期处理逻辑
     */
    private void handleExpiredPacket(RedPacket packet) {
        if (packet.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            // 添加到待退还列表
            playerPendingRefunds.computeIfAbsent(packet.getSenderId(), k -> new ArrayList<>())
                    .add(packet.getId() + ":" + packet.getRemainingAmount().toString());
            
            // 如果玩家在线，立即退还
            Player sender = Bukkit.getPlayer(UUID.fromString(packet.getSenderId()));
            if (sender != null && sender.isOnline()) {
                processRefund(sender);
            }
        }
        
        packet.setExpired();
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Red packet " + packet.getId() + " expired");
        }
    }
    
    /**
     * 处理玩家上线时的退款
     */
    public void processRefund(Player player) {
        String playerId = player.getUniqueId().toString();
        List<String> refunds = playerPendingRefunds.get(playerId);
        
        if (refunds == null || refunds.isEmpty()) {
            return;
        }
        
        BigDecimal totalRefund = BigDecimal.ZERO;
        for (String refundData : refunds) {
            String[] parts = refundData.split(":");
            if (parts.length == 2) {
                BigDecimal amount = new BigDecimal(parts[1]);
                totalRefund = totalRefund.add(amount);
            }
        }
        
        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            // 退还金额
            if (plugin.getEconomyManager().deposit(player, totalRefund)) {
                String message = plugin.getConfigManager().getMessage("messages.chat.packet-expired")
                        .replace("{amount}", totalRefund.toString());
                player.sendMessage(message);
                
                // 清除退款记录
                playerPendingRefunds.remove(playerId);
                
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Refunded " + totalRefund + " to " + player.getName());
                }
            } else {
                // 如果存款失败，记录警告
                plugin.getLogger().warning("Failed to process refund for player " + player.getName());
            }
        }
    }
    
    /**
     * 获取过期的红包列表
     */
    public List<RedPacket> getExpiredPackets() {
        List<RedPacket> expiredPackets = new ArrayList<>();
        
        for (RedPacket packet : activePackets.values()) {
            if (packet.isExpired()) {
                expiredPackets.add(packet);
            }
        }
        
        return expiredPackets;
    }
    
    /**
     * 移除红包
     */
    public void removeRedPacket(String packetId) {
        activePackets.remove(packetId);
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Removed red packet: " + packetId);
        }
    }
    
    /**
     * 添加待退款
     */
    public void addPendingRefund(String playerId, BigDecimal amount) {
        playerPendingRefunds.computeIfAbsent(playerId, k -> new ArrayList<>())
                .add(UUID.randomUUID().toString() + ":" + amount.toString());
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Added pending refund of " + amount + " for player " + playerId);
        }
    }
    
    /**
     * 处理所有退款（插件关闭时调用）
     */
    public void processAllRefunds() {
        // 处理所有过期红包
        List<RedPacket> expiredPackets = getExpiredPackets();
        for (RedPacket packet : expiredPackets) {
            // 使用统一的原子处理
            processExpiredPacket(packet.getId());
        }
        
        // 尝试为在线玩家处理退款
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            processRefund(player);
        }
        
        // 记录未处理的退款
        if (!playerPendingRefunds.isEmpty()) {
            plugin.getLogger().info("There are " + playerPendingRefunds.size() + " players with pending refunds that will be processed when they come online.");
        }
    }
    
    /**
     * 获取活跃红包数量
     */
    public int getActivePacketCount() {
        return activePackets.size();
    }
    
    /**
     * 获取所有活跃红包
     */
    public Collection<RedPacket> getActivePackets() {
        return new ArrayList<>(activePackets.values());
    }
    
    /**
     * 获取玩家待退款数量
     */
    public int getPendingRefundCount(String playerId) {
        List<String> refunds = playerPendingRefunds.get(playerId);
        return refunds != null ? refunds.size() : 0;
    }
    
    /**
     * 获取玩家待退款总金额
     */
    public BigDecimal getPendingRefundAmount(String playerId) {
        List<String> refunds = playerPendingRefunds.get(playerId);
        if (refunds == null || refunds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (String refundData : refunds) {
            String[] parts = refundData.split(":");
            if (parts.length == 2) {
                BigDecimal amount = new BigDecimal(parts[1]);
                totalAmount = totalAmount.add(amount);
            }
        }
        
        return totalAmount;
    }
    
    /**
     * 清理所有数据（插件卸载时调用）
     */
    public void cleanup() {
        // 处理所有退款
        processAllRefunds();
        
        // 清理数据
        activePackets.clear();
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("RedPacketManager cleanup completed");
        }
    }
}