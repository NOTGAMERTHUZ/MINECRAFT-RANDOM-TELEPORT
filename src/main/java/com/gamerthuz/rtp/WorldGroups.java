package com.gamerthuz.rtp;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * World groups for Multiverse servers.
 * The plugin only works INSIDE one enabled group and never across two different groups.
 * Worlds that are not listed in any group are treated as disabled.
 */
public final class WorldGroups {
    public record Group(String id, String display, boolean enabled) {}

    private final Map<String, Group> byId = new LinkedHashMap<>();
    private final Map<String, Group> byWorld = new HashMap<>();

    public static WorldGroups load(JavaPlugin plugin) {
        WorldGroups g = new WorldGroups();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("world-groups");
        if (sec == null) {
            plugin.getLogger().warning("No 'world-groups' in config.yml - the plugin will not work in any world!");
            return g;
        }
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            Group group = new Group(id.toLowerCase(), s.getString("display", id), s.getBoolean("enabled", true));
            g.byId.put(group.id(), group);
            for (String w : s.getStringList("worlds")) g.byWorld.put(w.toLowerCase(), group);
            plugin.getLogger().info("World group '" + group.id() + "' ("
                    + (group.enabled() ? "enabled" : "disabled") + "): " + s.getStringList("worlds"));
        }
        return g;
    }

    public Group of(String worldName) {
        return worldName == null ? null : byWorld.get(worldName.toLowerCase());
    }

    public Group of(World w) {
        return w == null ? null : of(w.getName());
    }

    /** The group of this world, but only if that group is enabled. */
    public Group active(World w) {
        Group g = of(w);
        return g != null && g.enabled() ? g : null;
    }

    public boolean allowed(World w) {
        return active(w) != null;
    }

    public boolean sameGroup(World a, World b) {
        Group x = of(a);
        Group y = of(b);
        return x != null && y != null && x.id().equals(y.id());
    }

    public boolean sameGroup(String a, String b) {
        Group x = of(a);
        Group y = of(b);
        return x != null && y != null && x.id().equals(y.id());
    }
}
