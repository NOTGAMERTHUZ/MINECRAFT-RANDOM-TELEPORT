package com.gamerthuz.rtp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;

/** One world entry from config.yml. */
public record RtpWorld(String key, String display, Material icon, List<String> description,
                       String center, double costMultiplier, String permission, boolean enabled) {

    public World world() {
        return Bukkit.getWorld(key);
    }
}
