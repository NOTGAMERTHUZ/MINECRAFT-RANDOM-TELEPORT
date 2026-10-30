package com.gamerthuz.rtp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Only loaded when Vault is present. */
final class VaultBridge implements EconomyHook.Bridge {
    private final Economy eco;

    private VaultBridge(Economy eco) {
        this.eco = eco;
    }

    static VaultBridge create(RtpPlugin plugin) {
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new VaultBridge(rsp.getProvider());
    }

    @Override
    public double balance(OfflinePlayer p) {
        return eco.getBalance(p);
    }

    @Override
    public boolean has(OfflinePlayer p, double amount) {
        return eco.has(p, amount);
    }

    @Override
    public boolean withdraw(OfflinePlayer p, double amount) {
        return eco.withdrawPlayer(p, amount).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer p, double amount) {
        return eco.depositPlayer(p, amount).transactionSuccess();
    }

    @Override
    public String format(double amount) {
        return eco.format(amount);
    }
}
