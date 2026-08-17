# QOLmod

QOLmod is a small, client-side quality-of-life mod for Minecraft 1.21.11. It uses Java 21, Fabric Loader, and Fabric API, with no Meteor Client dependency.

QOLmod doesn't add features because clients are expected to have them. Features exist because something encountered during real gameplay could be made better.

## Development

Build with:

```sh
./gradlew build
```

To use a custom username for a local development client, add `dev_username=YourName`
to your user-local `~/.gradle/gradle.properties`. Personal usernames are intentionally
not stored in this repository.

## Architecture

The current rewrite is focused on a small, explicit core architecture. `QOL` owns the runtime services, `QOLContext` provides consistent access to client state, and `FabricEventBridge` translates Fabric callbacks into typed internal events. `FeatureManager` owns feature state transitions and reset reasons; `QOLFeature` provides lifecycle-scoped subscriptions, typed settings, and a lightweight status model. Config and keybind handling sit behind these core services.

The event and automation paths are:

```text
Fabric events -> QOL event bus -> features / automation engine
Automation engine -> workflow -> tasks -> InputController -> Minecraft
```

The automation engine runs exactly zero or one workflow on client ticks. Workflows have a beginning and an end, tasks are sequential and non-blocking, and `InputController` tracks every synthetic key hold against the active workflow's opaque owner. Completion, cancellation, failure, panic, death, world leave, and shutdown all release that owner's inputs.

Manual W/A/S/D, jump, or sneak input cancels a workflow while it owns player input. Detection reads the physical key or mouse binding through GLFW rather than the synthetic Minecraft key state, and is disabled while a screen is open. GLFW scan-code-only bindings cannot be polled reliably and are therefore not treated as manual override. `USE` and `ATTACK` support held-key behaviour; they do not synthesize discrete click counts.

In-game automation diagnostics are available through `/qol automation status` and `/qol automation cancel`. `/qol panic` exercises the full panic cleanup path.

AutoDuper runs as a composed automation workflow through the shared automation engine. Its legacy integer-stage sequencer was removed after controlled gameplay parity validation. Storage Labels uses the internal event architecture and keeps its separate data file and compatibility loading.

Shared problems belong in the core; feature-specific problems stay in the feature.

Automation should describe intent, not fight the player.

The automation kernel is intentionally minimal. Additional controllers or scheduling capabilities will be added only when real QOLmod features require them.
