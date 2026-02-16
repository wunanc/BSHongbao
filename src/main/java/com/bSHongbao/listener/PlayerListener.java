package com.bSHongbao.listener;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.gui.RedPacketCreationSession;
import com.bSHongbao.util.SchedulerCompat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

/**
 * 玩家事件监听器
 * Player Event Listener
 */
public class PlayerListener implements Listener {
    private final BSHongbao plugin;

    public PlayerListener(BSHongbao plugin) {
        this.plugin = plugin;
    }

    /**
     * 处理GUI点击事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        // 检查是否是红包GUI
        String guiTitle = plugin.getConfigManager().getGuiTitle();
        if (inventory.getSize() == 27 && guiTitle.equals(event.getView().getTitle())) {
            event.setCancelled(true); // 取消默认行为

            if (event.getCurrentItem() != null) {
                plugin.getRedPacketGUI().handleClick(player, event.getSlot(), event.getCurrentItem());
            }
        }
    }

    /**
     * 处理聊天事件（用于红包创建输入）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        RedPacketCreationSession session = plugin.getRedPacketGUI().getCreationSession(player.getUniqueId());

        if (session != null) {
            event.setCancelled(true);

            // 【关键修改】在 Folia 中，必须将逻辑切回玩家所在的区域线程
            // 使用我们之前写的 SchedulerCompat
            SchedulerCompat.runEntityTask(plugin, player, () -> {
                if (session.isExpired()) {
                    plugin.getRedPacketGUI().removeCreationSession(player.getUniqueId());
                    player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.packet-not-found")
                            .replace("红包不存在或已过期！", "输入超时，请重新开始！"));
                    return;
                }
                // 在区域线程安全地处理金额、份数、GUI、经济扣款
                plugin.getRedPacketGUI().handleChatInput(player, event.getMessage());
            }, () -> {
                // 如果玩家在处理过程中下线了
                plugin.getRedPacketGUI().removeCreationSession(player.getUniqueId());
            });
        }
    }

    /**
     * 处理命令预处理事件（用于红包领取）
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage();

        // 检查是否是红包领取命令
        if (command.startsWith("/bshongbao_claim ")) {
            event.setCancelled(true);

            String[] parts = command.split(" ");
            if (parts.length >= 2) {
                String packetId = parts[1];
                plugin.getChatManager().handleClaimCommand(event.getPlayer(), packetId);
            }
        }
    }

    /**
     * 处理玩家加入事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 延迟处理退款，确保玩家完全加载（Folia/非Folia 统一调度）
        SchedulerCompat.runLaterGlobal(plugin, () -> plugin.getRedPacketManager().processRefund(player), 20L);

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " joined, processing refunds");
        }
    }

    /**
     * 处理玩家退出事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 清理玩家的红包创建会话
        plugin.getRedPacketGUI().removeCreationSession(player.getUniqueId());

        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " quit, cleaned up sessions");
        }
    }
}