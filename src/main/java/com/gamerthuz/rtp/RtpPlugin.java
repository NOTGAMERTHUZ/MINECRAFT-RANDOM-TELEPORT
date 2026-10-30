package com.gamerthuz.rtp;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class RtpPlugin extends JavaPlugin {
    private Messages messages;
    private PlayerData data;
    private EconomyHook economy;
    private RtpManager rtp;
    private RtpConfig cfg;
    private WorldGroups groups;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Brand.banner(this);

        groups = WorldGroups.load(this);
        cfg = RtpConfig.load(this);
        messages = new Messages(this);
        data = new PlayerData(this);
        economy = new EconomyHook(this);
        rtp = new RtpManager(this);

        getServer().getPluginManager().registerEvents(new RtpListener(this), this);

        PluginCommand cmd = getCommand("rtp");
        if (cmd != null) {
            RtpCommand handler = new RtpCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        for (RtpWorld w : cfg.worlds()) {
            if (w.enabled() && w.world() == null) {
                getLogger().warning("World '" + w.key() + "' from config.yml is not loaded on this server.");
            }
        }
    }

    public void reloadAll() {
        reloadConfig();
        groups = WorldGroups.load(this);
        cfg = RtpConfig.load(this);
    }

    /** Run on the next tick (safe for opening menus from click events). */
    public void sync(Runnable task) {
        getServer().getScheduler().runTask(this, task);
    }

    public Messages messages() { return messages; }

    public PlayerData data() { return data; }

    public EconomyHook economy() { return economy; }

    public RtpManager rtp() { return rtp; }

    public RtpConfig cfg() { return cfg; }

    public WorldGroups groups() { return groups; }
}
