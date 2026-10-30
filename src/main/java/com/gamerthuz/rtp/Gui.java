package com.gamerthuz.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/** Small helpers shared by all menus. */
public final class Gui {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Gui() {}

    public static Component c(String s) {
        return MM.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static ItemStack item(Material m, String name, List<String> lore, boolean glow) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        if (glow) meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack head(OfflinePlayer p, String name, List<String> lore) {
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) it.getItemMeta();
        meta.setOwningPlayer(p);
        meta.displayName(c(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(c(s));
        meta.lore(l);
        it.setItemMeta(meta);
        return it;
    }

    private static ItemStack pane(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        meta.setHideTooltip(true);
        it.setItemMeta(meta);
        return it;
    }

    public static MenuHolder create(String title, int rows) {
        MenuHolder h = new MenuHolder();
        Inventory inv = Bukkit.createInventory(h, rows * 9, MM.deserialize(title));
        h.setInventory(inv);
        return h;
    }

    /** Evenly spaced slots centered on a slot (max 7 items per row). */
    public static List<Integer> centered(int n, int center) {
        n = Math.max(0, Math.min(n, 7));
        int spacing = n <= 4 ? 2 : 1;
        int start = center - ((n - 1) * spacing) / 2;
        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(start + i * spacing);
        return out;
    }

    /** Tricolor border + Gamerthuz brand star at bottom center. */
    public static void frame(RtpPlugin plugin, MenuHolder h, int rows) {
        int size = rows * 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            boolean edge = row == 0 || row == rows - 1 || col == 0 || col == 8;
            Material m = !edge ? Material.GRAY_STAINED_GLASS_PANE
                    : (row == 0 ? Material.ORANGE_STAINED_GLASS_PANE
                    : (row == rows - 1 ? Material.LIME_STAINED_GLASS_PANE : Material.WHITE_STAINED_GLASS_PANE));
            h.set(i, pane(m), null);
        }
        ItemStack star = item(Material.NETHER_STAR,
                "<gradient:#ff9933:#138808><bold>" + Brand.AUTHOR + "</bold></gradient>",
                List.of("<gray>Developed by <white>" + Brand.AUTHOR,
                        "<dark_gray>" + plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion(),
                        "",
                        "<yellow>Click to visit GitHub"),
                true);
        h.set(size - 5, star, e -> {
            Player p = (Player) e.getWhoClicked();
            p.closeInventory();
            Brand.sendGithub(p);
        });
    }

    public static void sound(Player p, String name, float volume, float pitch) {
        try {
            Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(name));
            if (s != null) p.playSound(p.getLocation(), s, volume, pitch);
        } catch (Exception ignored) {
            // bad sound name
        }
    }
}
