# Silo Client (Fabric)

Silo Client is a client-side Minecraft 1.21.1 Fabric mod with a dark, three-column module menu. The UI takes visual cues from Vape V4's category and module navigation. The module inventory references the public Krypton module catalogue; Silo is an independent project and is not affiliated with either client.

- **Right Shift** opens the menu; **O** toggles Storage ESP.
- **Storage ESP** draws outlines, tracers, or both for selected chests, trapped chests, ender chests, barrels, and shulker boxes. Range is adjustable from 8 to 128 blocks.
- **Sus Chunk Finder** counts selected storage block entities in loaded chunks through the full vertical range. Set the per-chunk threshold from 2 to 64 and customize the alert with `{x}`, `{z}`, and `{count}`.
- **Suspicious ESP** uses a warning color for storage in chunks meeting that threshold.
- **Chunk Finder** notifies when nearby chunks first appear in the client-loaded set. Its message supports `{x}` and `{z}`.
- **Relog Reminder** sends a configurable reminder every 5 to 120 minutes by chat, actionbar, or both. It reminds the player to reconnect; it does not automate reconnecting.
- Settings are saved to `config/silo-client.json`.

## Module catalogue status

The menu lists the modules present in the referenced Krypton catalogue across Combat, Misc, Donut, Base Finding, Render, and Client. Only the five Silo modules described above currently have game behavior and module-specific settings. Other entries are labelled **Catalogue entry only**; their saved switches do not perform actions. The Krypton inventory is a feature reference, not copied implementation code.

The scanner only sees block entities in chunks already sent to the client. It cannot inspect unopened container contents or reveal chunks the server has not loaded for the player. Chunk threshold means storage blocks per chunk, not items inside storage.

## Build

Requires Java 21, Fabric Loader, and Fabric API. Run `gradle build`; the installable jar is written to `build/libs/`. The included GitHub Actions workflow builds the project and uploads a `silo-client-fabric` artifact.

## References

- [Vape Quick Start](https://docs.vape.gg/intro/quick-start) — category navigation and module/settings interaction pattern.
- [Krypton Client module catalogue](https://kryptonclients.org/) — names and broad category inventory used for reference.
- [Fabric 1.21.1 notes](https://www.fabricmc.net/2024/05/31/121.html) — version-specific Fabric rendering context.

