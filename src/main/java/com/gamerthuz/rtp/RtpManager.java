package com.gamerthuz.rtp;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static com.gamerthuz.rtp.Messages.ph;

public final class RtpManager {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final Set<Material> UNSAFE = EnumSet.of(
            Material.LAVA, Material.WATER, Material.FIRE, Material.SOUL_FIRE, Material.CACTUS,
            Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE, Material.COBWEB,
            Material.SWEET_BERRY_BUSH, Material.POWDER_SNOW, Material.WITHER_ROSE,
            Material.POINTED_DRIPSTONE, Material.BEDROCK);

    private final RtpPlugin plugin;
    private final Set<UUID> busy = new HashSet<>();
    private final Map<UUID, BukkitTask> warmups = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Location> previous = new HashMap<>();

    public RtpManager(RtpPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- queries ----------

    public long cooldownLeft(Player p) {
        if (p.hasPermission("gamerthuz.rtp.bypass.cooldown")) return 0;
        long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        return left > 0 ? (left + 999) / 1000 : 0;
    }

    public Location previous(Player p) {
        return previous.get(p.getUniqueId());
    }

    public boolean isWarming(UUID id) {
        return warmups.containsKey(id);
    }

    /** {min, max} after clamping to the world border. */
    public int[] range(World w, Tier t) {
        double half = w.getWorldBorder().getSize() / 2.0;
        int cap = (int) Math.max(50, Math.min(Integer.MAX_VALUE / 4.0, half - 16));
        int max = Math.min(t.max(), cap);
        int min = Math.max(0, Math.min(t.min(), max - 1));
        return new int[]{min, max};
    }

    public double cost(Player p, RtpWorld rw, Tier t) {
        double c = t.cost() * rw.costMultiplier();
        if (c <= 0 || p.hasPermission("gamerthuz.rtp.bypass.cost") || !plugin.economy().available()) return 0;
        return c;
    }

    // ---------- entry points ----------

    public void quick(Player p) {
        Tier tier = plugin.cfg().tier(plugin.getConfig().getString("settings.quick-tier", "normal"));
        if (tier == null && !plugin.cfg().tiers().isEmpty()) tier = plugin.cfg().tiers().get(0);
        RtpWorld rw = plugin.cfg().world(p.getWorld().getName());
        if (rw == null || !rw.enabled()) {
            for (RtpWorld candidate : plugin.cfg().worlds()) {
                if (candidate.enabled() && candidate.world() != null
                        && plugin.groups().sameGroup(p.getWorld(), candidate.world())) {
                    rw = candidate;
                    break;
                }
            }
        }
        if (rw == null || tier == null) {
            plugin.messages().send(p, "world-unavailable");
            return;
        }
        start(p, rw, tier, false);
    }

    public void start(Player p, RtpWorld rw, Tier tier, boolean force) {
        Messages m = plugin.messages();
        UUID id = p.getUniqueId();
        World w = rw.world();

        if (busy.contains(id)) {
            m.send(p, "busy");
            return;
        }
        if (!rw.enabled() || w == null) {
            m.send(p, "world-unavailable");
            return;
        }
        if (!force) {
            if (!plugin.groups().allowed(p.getWorld())) {
                m.send(p, "world-disabled");
                return;
            }
            if (!plugin.groups().sameGroup(p.getWorld(), w)) {
                m.send(p, "cross-world-blocked");
                return;
            }
        }
        double cost = 0;
        if (!force) {
            if (!rw.permission().isEmpty() && !p.hasPermission(rw.permission())) {
                m.send(p, "no-permission");
                return;
            }
            long left = cooldownLeft(p);
            if (left > 0) {
                m.send(p, "cooldown", ph("seconds", String.valueOf(left)));
                return;
            }
            cost = cost(p, rw, tier);
            if (cost > 0 && !plugin.economy().has(p, cost)) {
                m.send(p, "not-enough-money", ph("cost", plugin.economy().format(cost)));
                return;
            }
        }

        busy.add(id);
        m.send(p, "searching");
        Gui.sound(p, "block.beacon.activate", 0.6f, 1.6f);

        int warmup = (force || p.hasPermission("gamerthuz.rtp.bypass.warmup"))
                ? 0 : Math.max(0, plugin.getConfig().getInt("settings.warmup", 3));
        final double finalCost = cost;
        int[] r = range(w, tier);

        find(w, rw, r[0], r[1]).thenAccept(loc -> plugin.sync(() -> {
            if (!p.isOnline()) {
                busy.remove(id);
                return;
            }
            if (loc == null) {
                busy.remove(id);
                m.send(p, "search-failed");
                return;
            }
            if (warmup <= 0) {
                teleport(p, loc, finalCost, force);
            } else {
                warmup(p, loc, finalCost, force, warmup);
            }
        }));
    }

    public void back(Player p) {
        Messages m = plugin.messages();
        Location prev = previous.get(p.getUniqueId());
        if (prev == null || prev.getWorld() == null) {
            m.send(p, "back-none");
            return;
        }
        if (!plugin.groups().allowed(p.getWorld()) || !plugin.groups().sameGroup(p.getWorld(), prev.getWorld())) {
            m.send(p, "cross-world-blocked");
            return;
        }
        Location from = p.getLocation().clone();
        p.teleportAsync(prev).thenAccept(ok -> plugin.sync(() -> {
            if (Boolean.TRUE.equals(ok)) {
                previous.put(p.getUniqueId(), from);
                Gui.sound(p, "entity.enderman.teleport", 1f, 1f);
                m.send(p, "back-done");
            }
        }));
    }

    // ---------- warmup ----------

    private void warmup(Player p, Location loc, double cost, boolean force, int seconds) {
        plugin.messages().send(p, "warmup-start", ph("seconds", String.valueOf(seconds)));
        BukkitTask task = new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    warmups.remove(p.getUniqueId());
                    busy.remove(p.getUniqueId());
                    cancel();
                    return;
                }
                if (left <= 0) {
                    warmups.remove(p.getUniqueId());
                    cancel();
                    teleport(p, loc, cost, force);
                    return;
                }
                p.showTitle(Title.title(
                        MM.deserialize("<gradient:#ff9933:#138808><bold>" + left + "</bold></gradient>"),
                        MM.deserialize("<gray>Don't move!"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(100))));
                Gui.sound(p, "block.note_block.hat", 1f, 1.5f);
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        warmups.put(p.getUniqueId(), task);
    }

    public void cancelWarmup(Player p, boolean notify) {
        BukkitTask t = warmups.remove(p.getUniqueId());
        if (t != null) {
            t.cancel();
            busy.remove(p.getUniqueId());
            if (notify) {
                plugin.messages().send(p, "warmup-cancelled");
                p.clearTitle();
            }
        }
    }

    public void cleanup(UUID id) {
        BukkitTask t = warmups.remove(id);
        if (t != null) t.cancel();
        busy.remove(id);
    }

    // ---------- teleport ----------

    private void teleport(Player p, Location loc, double cost, boolean force) {
        Messages m = plugin.messages();
        UUID id = p.getUniqueId();

        if (!force && (!plugin.groups().allowed(p.getWorld())
                || !plugin.groups().sameGroup(p.getWorld(), loc.getWorld()))) {
            busy.remove(id);
            m.send(p, "cross-world-blocked");
            return;
        }

        boolean charged = false;
        if (!force && cost > 0) {
            if (!plugin.economy().withdraw(p, cost)) {
                busy.remove(id);
                m.send(p, "not-enough-money", ph("cost", plugin.economy().format(cost)));
                return;
            }
            charged = true;
            m.send(p, "cost-charged", ph("cost", plugin.economy().format(cost)));
        }
        final boolean wasCharged = charged;
        Location from = p.getLocation().clone();

        p.teleportAsync(loc).thenAccept(ok -> plugin.sync(() -> {
            busy.remove(id);
            if (!Boolean.TRUE.equals(ok)) {
                if (wasCharged) plugin.economy().deposit(p, cost);
                m.send(p, "search-failed");
                return;
            }
            previous.put(id, from);
            if (!force) {
                long cd = Math.max(0, plugin.getConfig().getLong("settings.cooldown", 60));
                cooldowns.put(id, System.currentTimeMillis() + cd * 1000);
            }
            plugin.data().increment(id);

            from.getWorld().spawnParticle(Particle.PORTAL, from.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
            p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
            Gui.sound(p, "entity.enderman.teleport", 1f, 1f);

            String x = String.valueOf(loc.getBlockX());
            String y = String.valueOf(loc.getBlockY());
            String z = String.valueOf(loc.getBlockZ());
            p.showTitle(Title.title(
                    MM.deserialize("<gradient:#ff9933:#138808><bold>Teleported!</bold></gradient>"),
                    MM.deserialize("<gray>X: <white>" + x + " <gray>Y: <white>" + y + " <gray>Z: <white>" + z),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1800), Duration.ofMillis(500))));
            m.send(p, "teleported", ph("x", x), ph("y", y), ph("z", z), ph("world", loc.getWorld().getName()));
        }));
    }

    // ---------- finding a safe spot ----------

    private Location center(World w, String mode) {
        return switch (mode.toLowerCase()) {
            case "spawn" -> w.getSpawnLocation();
            case "zero" -> new Location(w, 0, 0, 0);
            default -> w.getWorldBorder().getCenter();
        };
    }

    private CompletableFuture<Location> find(World w, RtpWorld rw, int min, int max) {
        CompletableFuture<Location> result = new CompletableFuture<>();
        int attempts = Math.max(1, plugin.getConfig().getInt("settings.max-attempts", 25));
        attempt(w, center(w, rw.center()), min, max, attempts, result);
        return result;
    }

    private void attempt(World w, Location c, int min, int max, int left, CompletableFuture<Location> result) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double angle = rnd.nextDouble(Math.PI * 2);
        double lo = (double) min * min;
        double hi = (double) max * max;
        double radius = Math.sqrt(lo + rnd.nextDouble() * (hi - lo));
        int x = (int) Math.floor(c.getX() + Math.cos(angle) * radius);
        int z = (int) Math.floor(c.getZ() + Math.sin(angle) * radius);

        Runnable retry = () -> {
            if (left > 1) attempt(w, c, min, max, left - 1, result);
            else result.complete(null);
        };

        if (!w.getWorldBorder().isInside(new Location(w, x, 64, z))) {
            retry.run();
            return;
        }
        w.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> plugin.sync(() -> {
            Location loc;
            try {
                loc = safeSpot(w, x, z);
            } catch (Exception ex) {
                loc = null;
            }
            if (loc != null) result.complete(loc);
            else retry.run();
        })).exceptionally(ex -> {
            result.complete(null);
            return null;
        });
    }

    private boolean solidSafe(Block b) {
        Material t = b.getType();
        return t.isSolid() && !UNSAFE.contains(t);
    }

    private boolean free(Block b) {
        return b.isPassable() && !UNSAFE.contains(b.getType());
    }

    private Location safeSpot(World w, int x, int z) {
        int floorY = Integer.MIN_VALUE;

        if (w.getEnvironment() == World.Environment.NETHER) {
            int top = Math.min(w.getLogicalHeight() - 6, 120);
            for (int y = top; y > 32; y--) {
                if (solidSafe(w.getBlockAt(x, y, z)) && free(w.getBlockAt(x, y + 1, z))
                        && free(w.getBlockAt(x, y + 2, z))) {
                    floorY = y;
                    break;
                }
            }
        } else {
            int y0 = w.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            for (int dy = 0; dy >= -1; dy--) {
                int cand = y0 + dy;
                if (solidSafe(w.getBlockAt(x, cand, z)) && free(w.getBlockAt(x, cand + 1, z))
                        && free(w.getBlockAt(x, cand + 2, z))) {
                    floorY = cand;
                    break;
                }
            }
        }
        if (floorY == Integer.MIN_VALUE || floorY <= w.getMinHeight() || floorY + 3 >= w.getMaxHeight()) {
            return null;
        }

        // no lava / fire next to the landing spot (only in chunks that are already loaded)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!w.isChunkLoaded((x + dx) >> 4, (z + dz) >> 4)) continue;
                for (int dy = -1; dy <= 2; dy++) {
                    Material t = w.getBlockAt(x + dx, floorY + dy, z + dz).getType();
                    if (t == Material.LAVA || t == Material.FIRE || t == Material.SOUL_FIRE
                            || t == Material.MAGMA_BLOCK) {
                        return null;
                    }
                }
            }
        }
        if (blockedBiome(w, x, floorY, z)) return null;
        return new Location(w, x + 0.5, floorY + 1, z + 0.5);
    }

    private boolean blockedBiome(World w, int x, int y, int z) {
        List<String> list = plugin.getConfig().getStringList("settings.blocked-biomes");
        if (list.isEmpty()) return false;
        try {
            String key = w.getBiome(x, y, z).key().value();
            for (String s : list) {
                if (s.equalsIgnoreCase(key)) return true;
            }
        } catch (Exception ignored) {
            // can't read biome, allow
        }
        return false;
    }
}
