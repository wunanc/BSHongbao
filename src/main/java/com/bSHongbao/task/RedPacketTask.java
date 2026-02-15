package com.bSHongbao.task;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.model.RedPacket;
import com.bSHongbao.util.SchedulerCompat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * 红包定时任务
 * Red Packet Scheduled Task
 */
public class RedPacketTask {
    private final BSHongbao plugin;
    private SchedulerCompat.CancellableTask repeatingTask;

    public RedPacketTask(BSHongbao plugin) {
        this.plugin = plugin;
    }

    public void run() {
        try {
            // 处理过期的红包
            processExpiredPackets();

            // 处理在线玩家的退款
            processOnlineRefunds();

        } catch (Exception e) {
            plugin.getLogger().severe("Error in RedPacketTask: " + e.getMessage());
            if (plugin.getConfigManager().isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 处理过期的红包
     */
    private void processExpiredPackets() {
        List<RedPacket> expiredPackets = plugin.getRedPacketManager().getExpiredPackets();

        for (RedPacket packet : expiredPackets) {
            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("Processing expired packet: " + packet.getId() + " by " + packet.getSenderName());
            }
            // 统一通过 RedPacketManager 进行原子过期处理与退款，避免重复逻辑
            plugin.getRedPacketManager().processExpiredPacket(packet.getId());
        }
    }

    /**
     * 处理在线玩家的退款
     */
    private void processOnlineRefunds() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getRedPacketManager().processRefund(player);
        }
    }

    /**
     * 启动定时任务
     */
    public void start() {
        // 每30秒运行一次（30 * 20 ticks）
        this.repeatingTask = SchedulerCompat.runAtFixedRateGlobal(plugin, this::run, 20L, 600L);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("RedPacketTask started with 30-second interval");
        }
    }

    /**
     * 停止定时任务
     */
    public void stop() {
        if (this.repeatingTask != null) {
            try {
                this.repeatingTask.cancel();
            } finally {
                this.repeatingTask = null;
            }

            if (plugin.getConfigManager().isDebugEnabled()) {
                plugin.getLogger().info("RedPacketTask stopped");
            }
        }
    }
}