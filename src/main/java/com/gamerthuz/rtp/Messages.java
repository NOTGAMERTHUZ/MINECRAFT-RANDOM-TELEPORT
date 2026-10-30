package com.gamerthuz.rtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class Messages {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final RtpPlugin plugin;

    public Messages(RtpPlugin plugin) {
        this.plugin = plugin;
    }

    public static TagResolver ph(String key, String value) {
        return Placeholder.unparsed(key, value);
    }

    public Component get(String key, TagResolver... resolvers) {
        String raw = plugin.getConfig().getString("messages." + key, "<red>Missing message: " + key);
        return Brand.prefix().append(MM.deserialize(raw, TagResolver.resolver(resolvers)));
    }

    public void send(CommandSender to, String key, TagResolver... resolvers) {
        to.sendMessage(get(key, resolvers));
    }
}
