package com.bSHongbao.command;

import com.bSHongbao.BSHongbao;
import com.bSHongbao.util.PlgColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 红包命令处理器
 * Red Packet Command Handler
 */
public class RedPacketCommand implements CommandExecutor, TabCompleter {
    private final BSHongbao plugin;
    
    public RedPacketCommand(BSHongbao plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // 检查是否为玩家
        if (!(sender instanceof Player)) {
            //sender.sendMessage("§c只有玩家可以使用此命令！");
            PlgColor.sendPrefixedMessage(sender, PlgColor.RED + "只有玩家可以使用此命令！");
            return true;
        }
        
        Player player = (Player) sender;
        
        // 检查权限
        if (!player.hasPermission("bshongbao.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.no-permission"));
            return true;
        }
        
        // 检查经济系统
        if (!plugin.getEconomyManager().isEconomyEnabled()) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.no-permission").replace("您没有权限使用此命令！", "经济系统未启用！"));
            return true;
        }
        
        // 处理命令参数
        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "open":
                handleOpenCommand(player);
                break;
            case "reload":
                handleReloadCommand(player);
                break;
            case "info":
                handleInfoCommand(player);
                break;
            case "help":
                sendHelpMessage(player);
                break;
            default:
                sendHelpMessage(player);
                break;
        }
        
        return true;
    }
    
    /**
     * 处理open命令
     */
    private void handleOpenCommand(Player player) {
        plugin.getRedPacketGUI().openMainGUI(player);
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Player " + player.getName() + " opened red packet GUI");
        }
    }
    
    /**
     * 处理reload命令
     */
    private void handleReloadCommand(Player player) {
        if (!player.hasPermission("bshongbao.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.no-permission"));
            return;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            //player.sendMessage(plugin.getConfigManager().getMessage("messages.success.packet-created").replace("成功创建红包！", "配置文件重载成功！"));
            PlgColor.sendPrefixedMessage(player, PlgColor.convertLegacyToMiniMessage(plugin.getConfigManager().getRawMessage("messages.success.reload-success")));

            plugin.getLogger().info( player.getName()+"重载了配置文件");
        } catch (Exception e) {
            //player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.invalid-amount").replace("请输入有效的金额！", "配置文件重载失败！"));
            PlgColor.sendPrefixedMessage(player, PlgColor.convertLegacyToMiniMessage(plugin.getConfigManager().getRawMessage("messages.errors.reload-error")));
            plugin.getLogger().warning("Failed to reload configuration: " + e.getMessage());
        }
    }
    
    /**
     * 处理info命令
     */
    private void handleInfoCommand(Player player) {
        if (!player.hasPermission("bshongbao.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("messages.errors.no-permission"));
            return;
        }
        
        int activePackets = plugin.getRedPacketManager().getActivePacketCount();
        int pendingRefunds = plugin.getRedPacketManager().getPendingRefundCount(player.getUniqueId().toString());
        
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§e=== 红包系统信息 ===");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7活跃红包数量: §a" + activePackets);
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7您的待退款数量: §a" + pendingRefunds);
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7插件版本: §a" + plugin.getDescription().getVersion());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7经济系统: §a" + (plugin.getEconomyManager().isEconomyEnabled() ? "已启用" : "未启用"));
    }
    
    /**
     * 发送帮助消息
     */
    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§e=== 红包系统帮助 ===");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7/BSHongbao open §f- 打开红包GUI");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§7/BSHongbao help §f- 显示帮助信息");
        
        if (player.hasPermission("bshongbao.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c=== 管理员命令 ===");
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§7/BSHongbao reload §f- 重载配置文件");
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§7/BSHongbao info §f- 查看系统信息");
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("open", "help");
            
            // 添加管理员命令
            if (sender.hasPermission("bshongbao.admin")) {
                subCommands = Arrays.asList("open", "help", "reload", "info");
            }
            
            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        }
        
        return completions;
    }
}