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
- Settings are saved to `config/vanta-client.json`.

## Module catalogue status

The menu lists the modules present in the referenced Krypton catalogue across Combat, Misc, Donut, Base Finding, Render, and Client. Ten modules currently have game behavior and module-specific controls. Other entries are marked **Catalogue entry only**; their saved switches do not perform actions. The catalogue is a feature reference, not copied implementation code.

Vanta sees only block entities in chunks already sent to the client. It cannot inspect unopened container contents or reveal chunks the server has not loaded. The Y -20 prompt counts selected storage block entities in the loaded chunk, not hidden items inside storage. Vanta does not include stealth, anti-cheat evasion, or ban-bypass behavior.

## Build

Requires Java 21, Fabric Loader, and Fabric API. Run `gradle build`; the installable jar is written to `build/libs/`. The included GitHub Actions workflow builds the project and uploads a `vanta-client-fabric` artifact.

## References

- [Vape Quick Start](https://docs.vape.gg/intro/quick-start) — category navigation and module/settings interaction pattern.
- [Krypton Client module catalogue](https://kryptonclients.org/) — names and broad category inventory used for reference.
- [Fabric 1.21.1 notes](https://www.fabricmc.net/2024/05/31/121.html) — version-specific Fabric rendering context.

