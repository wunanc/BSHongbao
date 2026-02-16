package com.bSHongbao.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

/**
 * 深度兼容 Folia, Paper 和 Spigot 的调度器工具类
 */
public final class SchedulerCompat {
    private static final boolean IS_FOLIA = detectFolia();

    private SchedulerCompat() {}

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * 定义一个统一的可取消任务接口，解决 RedPacketTask 的报错
     */
    public interface CancellableTask {
        void cancel();
    }

    /**
     * 在实体（玩家）所在的区域线程执行任务
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable task, Runnable retiredTask) {
        Objects.requireNonNull(entity, "entity cannot be null");
        if (IS_FOLIA) {
            entity.getScheduler().run(plugin, scheduledTask -> task.run(), retiredTask);
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 全局执行任务
     */
    public static void runGlobal(Plugin plugin, Runnable task) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    /**
     * 延迟执行全局任务
     */
    public static void runLaterGlobal(Plugin plugin, Runnable task, long delayTicks) {
        if (IS_FOLIA) {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, scheduledTask -> task.run(), delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    /**
     * 循环执行全局任务
     * 返回 CancellableTask 以适配 RedPacketTask.java
     */
    public static CancellableTask runAtFixedRateGlobal(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        if (IS_FOLIA) {
            // Folia 环境
            Object scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, st -> task.run(), initialDelayTicks, periodTicks);
            return () -> {
                try {
                    scheduledTask.getClass().getMethod("cancel").invoke(scheduledTask);
                } catch (Exception ignored) {}
            };
        } else {
            // Spigot/Paper 环境
            BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks);
            return bukkitTask::cancel;
        }
    }
}