package com.bSHongbao.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Scheduler compatibility wrapper for Spigot/Paper and Folia.
 * - On Folia: uses GlobalRegionScheduler via reflection (no compile-time dependency)
 * - On Spigot/Paper: falls back to BukkitScheduler (main thread)
 */
public final class SchedulerCompat {
    private static final boolean IS_FOLIA = detectFolia();

    private SchedulerCompat() {}

    private static boolean detectFolia() {
        try {
            // If Folia API is present
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        try {
            // Older detection approach
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isFolia() {
        return IS_FOLIA;
    }

    public interface CancellableTask {
        void cancel();
    }

    private static final class BukkitCancellable implements CancellableTask {
        private final BukkitTask delegate;
        private BukkitCancellable(BukkitTask delegate) { this.delegate = delegate; }
        @Override public void cancel() { if (delegate != null) delegate.cancel(); }
    }

    private static final class ReflectiveCancellable implements CancellableTask {
        private final Object scheduledTask; // io.papermc.paper.threadedregions.scheduler.ScheduledTask
        private final Method cancelMethod;
        private ReflectiveCancellable(Object scheduledTask) {
            this.scheduledTask = scheduledTask;
            Method m;
            try {
                m = scheduledTask.getClass().getMethod("cancel");
            } catch (Throwable t) {
                m = null;
            }
            this.cancelMethod = m;
        }
        @Override public void cancel() {
            try {
                if (scheduledTask != null && cancelMethod != null) {
                    cancelMethod.invoke(scheduledTask);
                }
            } catch (Throwable ignored) { }
        }
    }

    public static CancellableTask runGlobal(Plugin plugin, Runnable task) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(task, "task");
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Method getGRS = server.getClass().getMethod("getGlobalRegionScheduler");
                Object grs = getGRS.invoke(server);
                Class<?> grsClass = grs.getClass();
                // Prefer method name "run" (next tick)
                try {
                    Method run = grsClass.getMethod("run", Plugin.class, Consumer.class);
                    Object st = run.invoke(grs, plugin, (Consumer<Object>) scheduledTask -> task.run());
                    return new ReflectiveCancellable(st);
                } catch (NoSuchMethodException noRun) {
                    // Try alternative name "execute"
                    try {
                        Method exec = grsClass.getMethod("execute", Plugin.class, Runnable.class);
                        Object st = exec.invoke(grs, plugin, (Runnable) task::run);
                        return new ReflectiveCancellable(st);
                    } catch (Throwable t2) {
                        // Fallback to Bukkit
                    }
                }
            } catch (Throwable ignored) { }
        }
        return new BukkitCancellable(Bukkit.getScheduler().runTask(plugin, task));
    }

    public static CancellableTask runLaterGlobal(Plugin plugin, Runnable task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(task, "task");
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Method getGRS = server.getClass().getMethod("getGlobalRegionScheduler");
                Object grs = getGRS.invoke(server);
                Class<?> grsClass = grs.getClass();
                Method runDelayed = grsClass.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
                Object st = runDelayed.invoke(grs, plugin, (Consumer<Object>) scheduledTask -> task.run(), delayTicks);
                return new ReflectiveCancellable(st);
            } catch (Throwable ignored) { }
        }
        return new BukkitCancellable(Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    public static CancellableTask runAtFixedRateGlobal(Plugin plugin, Runnable task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(task, "task");
        if (IS_FOLIA) {
            try {
                Object server = Bukkit.getServer();
                Method getGRS = server.getClass().getMethod("getGlobalRegionScheduler");
                Object grs = getGRS.invoke(server);
                Class<?> grsClass = grs.getClass();
                Method runAtFixedRate = grsClass.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
                Object st = runAtFixedRate.invoke(grs, plugin, (Consumer<Object>) scheduledTask -> task.run(), initialDelayTicks, periodTicks);
                return new ReflectiveCancellable(st);
            } catch (Throwable ignored) { }
        }
        return new BukkitCancellable(Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelayTicks, periodTicks));
    }
}