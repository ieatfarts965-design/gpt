# Storage ESP (Fabric)

Client-side mod for Minecraft 1.21.1. The settings panel takes visual cues from the supplied dark, sidebar-based UI reference.

- **Right Shift** opens settings; **O** toggles ESP
- ESP range is adjustable from 8 to 128 blocks
- Render style cycles through **OUTLINE** (box outlines), **LINES** (tracers), and **BOTH**
- The suspect-chunk finder counts chests, trapped chests, ender chests, barrels, and shulker boxes in chunks currently loaded by the client. Set the threshold from 2 to 64 containers per chunk.
- The scan includes all vertical levels in loaded chunks, including deep underground
- Alert text supports `{x}`, `{z}`, and `{count}`. Relog reminders can be set for 10, 15, 30, or 60 minutes.
- Requires Fabric Loader, Fabric API, and Java 21

The client cannot inspect inventories in unopened containers or chunks the server has not sent. The threshold is therefore containers per loaded chunk, not hidden items inside containers. Relogging may refresh which chunks are loaded, but does not reveal chunks outside server view distance.

Build with Gradle using `gradle build`; copy the jar from `build/libs/` into the Minecraft `mods` folder.

A GitHub Actions workflow is included at .github/workflows/build.yml. It builds on pushes, pull requests, or a manual workflow run and uploads the installable jar as an Actions artifact.

