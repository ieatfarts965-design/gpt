# Vanta Client (Fabric)

Vanta Client is a client-side Fabric mod for Minecraft 1.21.1. It has a dark purple, three-column module menu shaped by the supplied Vape V4 reference. The module inventory references the public Krypton catalogue; Vanta is an independent project and is not affiliated with either client.

- **Right Shift** opens the menu; **O** toggles Storage ESP.
- **Storage ESP** draws outlines, tracers, or both for selected chests, trapped chests, ender chests, barrels, and shulker boxes. Range is adjustable from 8 to 128 blocks.
- **Sus Chunk Finder** counts selected storage block entities in chunks already loaded by the client, through the full vertical range. Set its threshold from 2 to 64 and customize its alert with `{x}`, `{z}`, and `{count}`.
- **Suspicious ESP** uses a warning color for storage in chunks meeting that threshold.
- **Chunk Finder** notifies when nearby chunks first appear in the client-loaded set. Its message supports `{x}` and `{z}`.
- **Empty Chunk Relog Prompt** checks the current loaded chunk when you reach Y -20. If no selected storage block entities are present, Vanta asks you to relog once. After reconnect, it checks that same chunk after it has loaded; if still empty, it sends **“Nothing found.”** once and suppresses further prompts.
- **Fullbright** raises local gamma while enabled and restores the previous value when disabled.
- **Sprint** requests the sprint state while enabled; normal Minecraft movement limits still apply.
- **HUD** shows Vanta, coordinates, and FPS.
- **Player ESP** and **Mob ESP** draw boxes for loaded entities in render range.
- **Auto Eat** consumes the best food in your hotbar at a configurable hunger threshold, then restores your selected slot.
- **Auto Tool** picks the fastest hotbar tool while you hold attack on a block, then restores your selected slot.
- **Weather Notifier** reports rain/thunderstorm transitions and clearing weather.
- **Block Entity Debug** shows the latest loaded-storage and flagged-chunk scan counts in the HUD.
- **Auto Clicker** repeats your held attack input at a configurable rate while retaining Minecraft's attack cooldown.
- **Trigger Bot** attacks a living entity under your crosshair at vanilla cooldown speed.
- **Auto Walk** holds forward using normal client movement input.
- **Player Chunks** outlines the chunk footprints of nearby players already loaded by the client.
- **Spawner Protect** outlines loaded mob spawners and reports each newly found position once.
- **Target HUD** displays the living crosshair target's name and health bar.
- **Netherite Finder** outlines ancient debris between Y 8 and 23 in chunks already loaded by the client.
- **Hole ESP** outlines nearby two-block-high cavities with solid floors and walls.
- **Light Finder** marks nearby air blocks at or below a configurable light level.
- **Block ESP** outlines nearby loaded diamond ore, emerald ore, and ancient debris.
- **Auto Mine** holds normal attack while your crosshair points at a block.
- **Auto Jump Reset** requests a normal jump when you take damage while grounded.
- **Block Notifier** watches the block under your crosshair and reports state changes.
- **Jump Circles** draws a fading ring after a landing.
- **Static Hitboxes** draws fixed-size outlines around nearby loaded players.
- **Real Hitbox** draws current collision bounds around nearby loaded living entities.
- **Auto Log** disconnects once when health reaches the configurable threshold.
- **Combat tools** include aim assist, anchor interactions, automatic crystal placement and attacks, offhand use, crystal placement markers, chest-slot elytra swaps, target hitbox visuals, mace selection/attacks, shield-breaker axe selection, trident selection, and the local no-hit-delay option. Server rules still control accepted hits and damage.
- **Other utilities** include a local fake-player stand-in, movable client-only freecam, independent free look, nearby name list, jukebox music labels, name masking in local chat/tab list, and a local replacement skin preview.
- **Chunk tools** include prime storage alerts, tunnel markers, and seed-based slime-chunk outlines for loaded chunks.
- **Local previews** include configurable fake payment text and fake statistics HUD text. They are never sent to the server.
- **Integrations** include a configurable Discord Rich Presence connection and local radio playback for direct PCM WAV/AIFF URLs.
- Settings are saved to `config/vanta-client.json`.

## Module catalogue status

The menu contains all 83 modules in the referenced Krypton catalogue across Combat, Misc, Donut, Base Finding, Render, and Client. Eighty-two have implemented behavior or an explicit local integration. **Proxy** is visibly unavailable in this build because proxy routing was requested alongside ban bypass; no proxy or ban-bypass behavior is included. The catalogue is a feature reference, not copied implementation code.

Vanta sees only block entities in chunks already sent to the client. It cannot inspect unopened container contents or reveal chunks the server has not loaded. The Y -20 prompt counts selected storage block entities in the loaded chunk, not hidden items inside storage. The seed finder requires a numeric seed you already know. Discord Presence requires Discord Desktop and a numeric application ID on Windows. Radio accepts direct PCM WAV/AIFF streams, depending on the system audio device. Fake Pay and Fake Stats only display local preview text. Name and skin protection only affect local client rendering. No stealth, anti-cheat evasion, or ban-bypass behavior is included.

## Build

Requires Java 21, Fabric Loader, and Fabric API. Run `gradle build`; the installable jar is written to `build/libs/`. The included GitHub Actions workflow builds the project and uploads a `vanta-client-fabric` artifact.

## References

- [Vape Quick Start](https://docs.vape.gg/intro/quick-start) — category navigation and module/settings interaction pattern.
- [Krypton Client module catalogue](https://kryptonclients.org/) — names and broad category inventory used for reference.
- [Fabric 1.21.1 notes](https://www.fabricmc.net/2024/05/31/121.html) — version-specific Fabric rendering context.

