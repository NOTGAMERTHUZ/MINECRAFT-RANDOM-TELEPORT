package com.gamerthuz.rtp;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/** Saves how many times each player used RTP. */
public final class PlayerData {
    private final RtpPlugin plugin;
    private final File file;
    private final YamlConfiguration yml;

    public PlayerData(RtpPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yml = YamlConfiguration.loadConfiguration(file);
    }

    public int count(UUID id) {
        return yml.getInt("players." + id + ".count", 0);
    }

    public void increment(UUID id) {
        yml.set("players." + id + ".count", count(id) + 1);
        save();
    }

    public synchronized void save() {
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save data.yml: " + e.getMessage());
        }
    }
}
