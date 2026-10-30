package com.gamerthuz.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/** Shared Gamerthuz branding. Copy this class into every plugin you make. */
public final class Brand {
    public static final String AUTHOR = "Gamerthuz";
    // Change this to your own GitHub profile / repository link
    public static final String GITHUB = "https://github.com/Gamerthuz";
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Brand() {}

    public static Component prefix() {
        return MM.deserialize("<dark_gray>[<gradient:#ff9933:#138808>Gamerthuz</gradient><dark_gray>] ");
    }

    public static Component footer() {
        return MM.deserialize("<gray>Developed by <gradient:#ff9933:#138808><bold>" + AUTHOR + "</bold></gradient>");
    }

    public static void sendGithub(Player p) {
        p.sendMessage(MM.deserialize("<yellow>▶ Click here to visit <gold>" + AUTHOR + "<yellow> on GitHub")
                .clickEvent(ClickEvent.openUrl(GITHUB))
                .hoverEvent(HoverEvent.showText(Component.text(GITHUB))));
    }

    public static void banner(JavaPlugin plugin) {
        var log = plugin.getLogger();
        String line = "=========================================";
        log.info(line);
        log.info(" " + plugin.getPluginMeta().getName() + " v" + plugin.getPluginMeta().getVersion());
        log.info(" Developed by " + AUTHOR);
        log.info(line);
    }
}
