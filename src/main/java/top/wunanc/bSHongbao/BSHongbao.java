package top.wunanc.bSHongbao;

import org.bukkit.plugin.java.JavaPlugin;
import org.bstats.bukkit.Metrics;

public final class BSHongbao extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        Metrics metrics = new Metrics(this, 28781);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
