# GamerthuzRTP

An advanced random-teleport plugin for **Paper 26.2** with a multi-world GUI, by **Gamerthuz**.

## Features
- **World selector GUI** (Overworld, Nether, End and any world you add) with live player counts, border size and cooldown
- **Distance tier GUI** (Near / Normal / Far / Extreme) with distance range and cost shown
- **Quick RTP** and **Previous location** buttons, plus `/rtp back`
- **Safe landing**: async chunk loading, no lava / water / fire / cactus, 2-block clearance, blocked biomes, world-border aware
- Warmup countdown with titles, cancel on move or damage, cooldown
- Optional **cost** per tier and per world through Vault
- Per-world permissions, first-join RTP for new players, admin `/rtp force` (for NPCs, portals, command blocks)
- All messages editable in `config.yml`, Maharashtra tricolor GUI theme

## World groups (Multiverse)
RTP only works **inside your own world group** (Survival + Survival_nether + Survival_the_end). Players in Lifesteal or PvP can never RTP into Survival and vice versa.
The world selector only shows the worlds of your own group. Configure groups in `config.yml`.

## Requirements
- Paper 26.2 and Java 25
- [Vault](https://www.spigotmc.org/resources/vault.34315/) + an economy plugin (optional, only for costs)

## Commands
| Command | Description |
|---|---|
| `/rtp` | Open the GUI |
| `/rtp <world> [range]` | Open that world's range menu or teleport directly |
| `/rtp back` | Return to your previous location |
| `/rtp force <player> [world] [range]` | Admin: RTP a player (no cooldown, cost or warmup) |
| `/rtp reload` | Reload config (admin) |

Aliases: `/wild`, `/randomtp`

## Permissions
`gamerthuz.rtp.use` (default: everyone) · `gamerthuz.rtp.back` · `gamerthuz.rtp.bypass.cooldown` · `gamerthuz.rtp.bypass.warmup` · `gamerthuz.rtp.bypass.cost` · `gamerthuz.rtp.admin`

## Setup note
World keys in `config.yml` must match your real world folder names (default: `world`, `world_nether`, `world_the_end`).

## Build
```
mvn package
```

---
Developed by **Gamerthuz** · Licensed under MIT
