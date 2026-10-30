package com.gamerthuz.rtp;

import org.bukkit.OfflinePlayer;

/** Safe wrapper: the plugin works even if Vault is not installed. */
public final class EconomyHook {

    public interface Bridge {
        double balance(OfflinePlayer p);

        boolean has(OfflinePlayer p, double amount);

        boolean withdraw(OfflinePlayer p, double amount);

        boolean deposit(OfflinePlayer p, double amount);

        String format(double amount);
    }

    private final RtpPlugin plugin;
    private Bridge bridge;

    public EconomyHook(RtpPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean available() {
        if (bridge != null) return true;
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        try {
            bridge = VaultBridge.create(plugin);
        } catch (Throwable t) {
            bridge = null;
        }
        return bridge != null;
    }

    public double balance(OfflinePlayer p) {
        return available() ? bridge.balance(p) : 0;
    }

    public boolean has(OfflinePlayer p, double amount) {
        return available() && bridge.has(p, amount);
    }

    public boolean withdraw(OfflinePlayer p, double amount) {
        return available() && bridge.withdraw(p, amount);
    }

    public void deposit(OfflinePlayer p, double amount) {
        if (available()) bridge.deposit(p, amount);
    }

    public String format(double amount) {
        return available() ? bridge.format(amount) : String.valueOf(amount);
    }
}
