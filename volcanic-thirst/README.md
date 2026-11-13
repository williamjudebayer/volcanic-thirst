# Volcanic Thirst (Fabric 1.21.11)

Thirst system for the Volcanic Ruins civilization event. Install on **both server and clients**
(the server runs the logic; the client draws the meter). Requires Fabric API.

## Build
```
./gradlew build          # (gradlew.bat build on Windows)
```
The mod jar is `build/libs/volcanic-thirst-1.0.0.jar` (not the `-sources` one).
Uses the official FabricMC 1.21.11 template: Loom, Mojang mappings, Fabric API 0.141.6+1.21.11, Java 21.

## How it works
| Thing | Behavior |
|---|---|
| Meter | 10 water droplets directly above the hunger bar (each = 10%, half droplet = 5%). Turn cyan while Hydration is active, jiggle below 20%. Moves up a row when air bubbles show. Hidden in creative/spectator and with F1. |
| Drain | Default: full meter lasts 30 min (1% every 18s), twice as fast while sprinting. Changeable in-game, per biome too (see commands). |
| Below 20% | Can't sprint (same feel as vanilla low hunger). |
| At 0% | 1 damage (half a heart) every 4s. |
| Unpurified Water (vanilla water bottle, renamed) | +10% thirst, **Hydration I** for 60s, 30% chance Hunger (30s), 15% chance Nausea (10s). |
| Purified Water | +25% thirst, **Hydration II** for 60s, no debuffs. |
| Hydration I | Thirst doesn't drain. |
| Hydration II | Thirst doesn't drain **and** restores +1% every 6s (+10% over the full minute). |
| Hydrating biome | Within 12 blocks → Hydration I for 60s, refreshed every 2s while nearby. |
| Death | Thirst resets to 100% on respawn. Saved across logouts/restarts. |

### Brewing
- **Fuel:** magma cream only (blaze powder no longer works as fuel).
- **Recipe:** water bottle + **blaze powder** in the ingredient slot → Purified Water.
- Every vanilla potion recipe is removed — purified water is the only thing a brewing stand can make.

## Commands (operators only)
| Command | Does |
|---|---|
| `/thirst set [players] <0-100>` | Set thirst (shortcut: `/setthirst`) |
| `/thirst add [players] <-100..100>` | Add or remove thirst (shortcut: `/addthirst`) |
| `/thirst get [player]` | Show thirst (shortcut: `/getthirst`) |
| `/thirst drainrate` | List the default rate and every biome-specific rate |
| `/thirst drainrate <minutes>` | Default rate: a full meter lasts `<minutes>` (decimals OK, `0` = no drain) |
| `/thirst drainrate reset` | Default back to 30 minutes |
| `/thirst drainrate biome <biome> <minutes>` | Rate for one biome, e.g. `/thirst drainrate biome minecraft:basalt_deltas 10` |
| `/thirst drainrate biome <biome> default` | Remove that biome's rate (it uses the default again) |
| `/thirst biome` | Show the biome you're standing in and its current rate |
| `/effect give @s volcanic_thirst:hydration 60 0` | Hydration I (use `1` at the end for Hydration II) |

Drain rates are saved to `config/volcanic_thirst.json` on the server, so they survive restarts.
You can also edit that file while the server is off.

## Setting the hydrating biome
Edit `src/main/resources/data/volcanic_thirst/tags/worldgen/biome/hydrating_biomes.json`:
```json
{ "replace": false, "values": ["minecraft:river", "minecraft:lush_caves"] }
```
Or, without rebuilding, add a server datapack containing that same file path and run `/reload`.

## Tuning
All numbers (drain speed, restore amounts, debuff chances, durations, radius, damage) are in
`src/main/java/com/volcanicruins/thirst/ThirstConfig.java`.
