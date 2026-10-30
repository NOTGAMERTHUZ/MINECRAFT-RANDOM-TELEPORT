package com.gamerthuz.rtp;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parsed worlds and tiers. */
public final class RtpConfig {
    private final Map<String, RtpWorld> worlds = new LinkedHashMap<>();
    private final Map<String, Tier> tiers = new LinkedHashMap<>();

    public static RtpConfig load(RtpPlugin plugin) {
        RtpConfig c = new RtpConfig();

        ConfigurationSection ws = plugin.getConfig().getConfigurationSection("worlds");
        if (ws != null) {
            for (String key : ws.getKeys(false)) {
                ConfigurationSection s = ws.getConfigurationSection(key);
                if (s == null) continue;
                c.worlds.put(key.toLowerCase(), new RtpWorld(
                        key,
                        s.getString("display-name", key),
                        material(plugin, s.getString("icon", "GRASS_BLOCK")),
                        s.getStringList("description"),
                        s.getString("center", "border"),
                        s.getDouble("cost-multiplier", 1.0),
                        s.getString("permission", ""),
                        s.getBoolean("enabled", true)));
            }
        }

        ConfigurationSection rs = plugin.getConfig().getConfigurationSection("ranges");
        if (rs != null) {
            for (String key : rs.getKeys(false)) {
                ConfigurationSection s = rs.getConfigurationSection(key);
                if (s == null) continue;
                int min = Math.max(0, s.getInt("min", 100));
                int max = Math.max(min + 1, s.getInt("max", 1000));
                c.tiers.put(key.toLowerCase(), new Tier(
                        key,
                        s.getString("name", key),
                        material(plugin, s.getString("icon", "LEATHER_BOOTS")),
                        min, max,
                        s.getDouble("cost", 0),
                        s.getStringList("lore")));
            }
        }
        return c;
    }

    private static Material material(RtpPlugin plugin, String name) {
        Material m = name == null ? null : Material.matchMaterial(name);
        if (m == null) {
            plugin.getLogger().warning("Unknown material in config.yml: " + name);
            return Material.STONE;
        }
        return m;
    }

    public List<RtpWorld> worlds() {
        return new ArrayList<>(worlds.values());
    }

    public RtpWorld world(String name) {
        return name == null ? null : worlds.get(name.toLowerCase());
    }

    public List<Tier> tiers() {
        return new ArrayList<>(tiers.values());
    }

    public Tier tier(String name) {
        return name == null ? null : tiers.get(name.toLowerCase());
    }
}
