package com.gamerthuz.rtp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Every GUI screen of the plugin. */
public final class Menus {
    private static final String TITLE = "<gradient:#ff9933:#138808><bold>RTP</bold></gradient> <dark_gray>» <white>";

    private Menus() {}

    private static String costText(RtpPlugin plugin, double cost) {
        return cost <= 0 ? "<green>Free" : "<gold>" + plugin.economy().format(cost);
    }

    private static String cooldownText(long left) {
        return left > 0 ? "<gray>Cooldown: <red>" + left + "s" : "<gray>Cooldown: <green>ready";
    }

    // ===================== WORLD SELECT =====================

    public static void worlds(RtpPlugin plugin, Player p) {
        if (!plugin.groups().allowed(p.getWorld())) {
            plugin.messages().send(p, "world-disabled");
            return;
        }
        RtpManager rtp = plugin.rtp();
        List<RtpWorld> list = new ArrayList<>(plugin.cfg().worlds());
        list.removeIf(rw -> !plugin.groups().sameGroup(p.getWorld().getName(), rw.key()));

        MenuHolder h = Gui.create(TITLE + "Choose World", 6);
        Gui.frame(plugin, h, 6);

        // player info
        List<String> info = new ArrayList<>();
        info.add("");
        info.add("<gray>Total RTPs: <white>" + plugin.data().count(p.getUniqueId()));
        if (plugin.economy().available()) {
            info.add("<gray>Balance: <green>" + plugin.economy().format(plugin.economy().balance(p)));
        }
        info.add(cooldownText(rtp.cooldownLeft(p)));
        h.set(4, Gui.head(p, "<gradient:#ff9933:#138808><bold>" + p.getName(), info), null);

        // worlds (up to 14, two rows of 7)
        int shown = Math.min(list.size(), 14);
        for (int row = 0; row * 7 < shown; row++) {
            int from = row * 7;
            int to = Math.min(shown, from + 7);
            List<Integer> slots = Gui.centered(to - from, 22 + 9 * row);
            for (int i = 0; i < slots.size(); i++) {
                RtpWorld rw = list.get(from + i);
                World w = rw.world();
                boolean ok = rw.enabled() && w != null;
                boolean locked = !rw.permission().isEmpty() && !p.hasPermission(rw.permission());

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.addAll(rw.description());
                lore.add("");
                if (ok) {
                    lore.add("<gray>Players here: <white>" + w.getPlayers().size());
                    double size = w.getWorldBorder().getSize();
                    if (size < 1_000_000) lore.add("<gray>World border: <white>±" + (int) (size / 2) + " blocks");
                    lore.add(cooldownText(rtp.cooldownLeft(p)));
                    lore.add("");
                    lore.add(locked ? "<red>Locked" : "<yellow>Click to choose distance");
                } else {
                    lore.add("<red>This world is not available.");
                }

                Material icon = ok ? rw.icon() : Material.BARRIER;
                h.set(slots.get(i), Gui.item(icon, rw.display(), lore, ok && !locked && w.equals(p.getWorld())), e -> {
                    if (!ok) {
                        Gui.sound(p, "entity.villager.no", 1f, 1f);
                        return;
                    }
                    if (locked) {
                        plugin.messages().send(p, "no-permission");
                        return;
                    }
                    plugin.sync(() -> ranges(plugin, p, rw));
                });
            }
        }

        // bottom controls
        Location prev = rtp.previous(p);
        if (prev != null) {
            h.set(46, Gui.item(Material.ENDER_PEARL, "<light_purple><bold>Previous location",
                    List.of("", "<gray>Go back to where you were", "<gray>before your last RTP.", "", "<yellow>Click to return"), false),
                    e -> {
                        p.closeInventory();
                        rtp.back(p);
                    });
        } else {
            h.set(46, Gui.item(Material.GRAY_DYE, "<gray><bold>No previous location", List.of(), false), null);
        }
        h.set(47, Gui.item(Material.CLOCK, "<green><bold>Quick RTP",
                List.of("", "<gray>Random teleport in your", "<gray>current world.", "", "<yellow>Click"), false),
                e -> {
                    p.closeInventory();
                    rtp.quick(p);
                });
        h.set(52, Gui.item(Material.OAK_DOOR, "<red><bold>Close", List.of(), false), e -> p.closeInventory());

        p.openInventory(h.getInventory());
        Gui.sound(p, "block.ender_chest.open", 0.6f, 1.3f);
    }

    // ===================== DISTANCE SELECT =====================

    public static void ranges(RtpPlugin plugin, Player p, RtpWorld rw) {
        RtpManager rtp = plugin.rtp();
        World w = rw.world();
        if (w == null) {
            worlds(plugin, p);
            return;
        }
        if (!plugin.groups().allowed(p.getWorld())) {
            plugin.messages().send(p, "world-disabled");
            return;
        }
        if (!plugin.groups().sameGroup(p.getWorld(), w)) {
            plugin.messages().send(p, "cross-world-blocked");
            return;
        }
        List<Tier> tiers = plugin.cfg().tiers();

        MenuHolder h = Gui.create(TITLE + rw.display(), 5);
        Gui.frame(plugin, h, 5);

        List<String> head = new ArrayList<>();
        head.add("");
        head.addAll(rw.description());
        head.add("");
        head.add(cooldownText(rtp.cooldownLeft(p)));
        h.set(4, Gui.item(rw.icon(), rw.display(), head, true), null);

        List<Integer> slots = Gui.centered(tiers.size(), 22);
        for (int i = 0; i < slots.size(); i++) {
            Tier t = tiers.get(i);
            int[] r = rtp.range(w, t);
            double cost = rtp.cost(p, rw, t);

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(t.lore());
            lore.add("");
            lore.add("<gray>Distance: <white>" + r[0] + " - " + r[1] + " blocks");
            lore.add("<gray>Cost: " + costText(plugin, cost));
            lore.add("");
            lore.add("<yellow>Click to teleport!");
            h.set(slots.get(i), Gui.item(t.icon(), t.name(), lore, false), e -> {
                p.closeInventory();
                rtp.start(p, rw, t, false);
            });
        }

        h.set(39, Gui.item(Material.ARROW, "<yellow><bold>« Back", List.of(), false),
                e -> plugin.sync(() -> worlds(plugin, p)));
        h.set(41, Gui.item(Material.OAK_DOOR, "<red><bold>Close", List.of(), false), e -> p.closeInventory());

        p.openInventory(h.getInventory());
        Gui.sound(p, "ui.button.click", 0.7f, 1.2f);
    }
}
