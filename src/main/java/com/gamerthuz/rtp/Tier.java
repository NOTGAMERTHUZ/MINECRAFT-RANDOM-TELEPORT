package com.gamerthuz.rtp;

import org.bukkit.Material;

import java.util.List;

/** A distance tier (Near / Normal / Far ...). */
public record Tier(String key, String name, Material icon, int min, int max, double cost, List<String> lore) {}
