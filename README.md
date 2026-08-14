# QOLmod

QOLmod is a small, client-side quality-of-life mod for Minecraft 1.21.11. It uses Java 21, Fabric Loader, and Fabric API, with no Meteor Client dependency.

QOLmod doesn't add features because clients are expected to have them. Features exist because something encountered during real gameplay could be made better.

## Development

Build with:

```sh
./gradlew build
```

The current rewrite is focused on the core architecture. `QOL` owns the runtime services, `QOLContext` provides consistent access to valid client state, and `FabricEventBridge` translates Fabric callbacks into typed internal events. `FeatureManager` owns feature state transitions and reset reasons; `QOLFeature` provides lifecycle-scoped subscriptions, typed settings, and a lightweight status model. Config and keybind handling sit behind these core services.

AutoDuper and Storage Labels remain present as legacy gameplay implementations while they are migrated incrementally. Storage Labels keeps its separate data file and existing compatibility loading. `TestFeature` is temporary validation for the new event, lifecycle, settings, reset, and status APIs.

Shared problems belong in the core; feature-specific problems stay in the feature.

Automation should describe intent, not fight the player.
