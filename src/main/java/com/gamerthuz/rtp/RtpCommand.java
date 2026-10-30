package com.gamerthuz.rtp;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static com.gamerthuz.rtp.Messages.ph;

public final class RtpCommand implements CommandExecutor, TabCompleter {
    private final RtpPlugin plugin;

    public RtpCommand(RtpPlugin plugin) {
        this.plugin = plugin;
    }

    private String tierNames() {
        List<String> names = new ArrayList<>();
        for (Tier t : plugin.cfg().tiers()) names.add(t.key());
        return String.join(", ", names);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        Messages m = plugin.messages();
        String a0 = args.length > 0 ? args[0].toLowerCase() : "";

        switch (a0) {
            case "reload" -> {
                if (!s.hasPermission("gamerthuz.rtp.admin")) {
                    m.send(s, "no-permission");
                    return true;
                }
                plugin.reloadAll();
                m.send(s, "reloaded");
                return true;
            }
            case "about" -> {
                s.sendMessage(Brand.prefix().append(Component.text(
                        plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion())));
                s.sendMessage(Brand.footer());
                return true;
            }
            case "force" -> {
                force(s, args);
                return true;
            }
            default -> { }
        }

        if (!(s instanceof Player p)) {
            m.send(s, "player-only");
            return true;
        }

        switch (a0) {
            case "" -> Menus.worlds(plugin, p);
            case "back" -> {
                if (!p.hasPermission("gamerthuz.rtp.back")) {
                    m.send(p, "no-permission");
                } else {
                    plugin.rtp().back(p);
                }
            }
            default -> {
                RtpWorld rw = plugin.cfg().world(args[0]);
                if (rw == null) {
                    m.send(p, "unknown-world");
                } else if (args.length > 1) {
                    Tier t = plugin.cfg().tier(args[1]);
                    if (t == null) m.send(p, "unknown-tier", ph("tiers", tierNames()));
                    else plugin.rtp().start(p, rw, t, false);
                } else {
                    Menus.ranges(plugin, p, rw);
                }
            }
        }
        return true;
    }

    private void force(CommandSender s, String[] args) {
        Messages m = plugin.messages();
        if (!s.hasPermission("gamerthuz.rtp.admin")) {
            m.send(s, "no-permission");
            return;
        }
        if (args.length < 2) {
            m.send(s, "usage-force");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            m.send(s, "player-not-found");
            return;
        }
        RtpWorld rw = args.length > 2 ? plugin.cfg().world(args[2]) : plugin.cfg().world(target.getWorld().getName());
        if (rw == null) {
            m.send(s, "unknown-world");
            return;
        }
        Tier t = args.length > 3
                ? plugin.cfg().tier(args[3])
                : plugin.cfg().tier(plugin.getConfig().getString("settings.quick-tier", "normal"));
        if (t == null) {
            m.send(s, "unknown-tier", ph("tiers", tierNames()));
            return;
        }
        plugin.rtp().start(target, rw, t, true);
        m.send(s, "forced", ph("player", target.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("back");
            out.add("about");
            if (s.hasPermission("gamerthuz.rtp.admin")) {
                out.add("reload");
                out.add("force");
            }
            for (RtpWorld w : plugin.cfg().worlds()) out.add(w.key());
            filter(out, args[0]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("force")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            filter(out, args[1]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("force")) {
            for (RtpWorld w : plugin.cfg().worlds()) out.add(w.key());
            filter(out, args[2]);
        } else if ((args.length == 2 && plugin.cfg().world(args[0]) != null)
                || (args.length == 4 && args[0].equalsIgnoreCase("force"))) {
            for (Tier t : plugin.cfg().tiers()) out.add(t.key());
            filter(out, args[args.length - 1]);
        }
        return out;
    }

    private static void filter(List<String> list, String prefix) {
        String p = prefix.toLowerCase();
        list.removeIf(x -> !x.toLowerCase().startsWith(p));
    }
}
