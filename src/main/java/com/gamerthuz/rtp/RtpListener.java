package com.gamerthuz.rtp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class RtpListener implements Listener {
    private final RtpPlugin plugin;

    public RtpListener(RtpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof MenuHolder holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getInventory()) return;
        holder.click(e);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof MenuHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!plugin.rtp().isWarming(p.getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("settings.cancel-on-move", true)) return;
        Location a = e.getFrom();
        Location b = e.getTo();
        if (b == null) return;
        if (a.getBlockX() == b.getBlockX() && a.getBlockY() == b.getBlockY() && a.getBlockZ() == b.getBlockZ()) {
            return;
        }
        plugin.rtp().cancelWarmup(p, true);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!plugin.rtp().isWarming(p.getUniqueId())) return;
        if (!plugin.getConfig().getBoolean("settings.cancel-on-damage", true)) return;
        plugin.rtp().cancelWarmup(p, true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.rtp().cleanup(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;
        if (!plugin.getConfig().getBoolean("settings.first-join.enabled", false)) return;
        RtpWorld rw = plugin.cfg().world(plugin.getConfig().getString("settings.first-join.world", "world"));
        Tier tier = plugin.cfg().tier(plugin.getConfig().getString("settings.first-join.tier", "near"));
        if (rw == null || tier == null) return;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline()) plugin.rtp().start(p, rw, tier, true);
        }, 40L);
    }
}
