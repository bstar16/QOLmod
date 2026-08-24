# AGENTS.md

# QOLmod

QOLmod is a small standalone client-side Quality of Life mod for Minecraft.

The project should feel cohesive and intentional rather than like a generic collection of client modules.

## Environment

- Minecraft: 1.21.11
- Java: 21
- Fabric
- Mod ID: `qolmod`
- Base package: `com.bstar.qolmod`

Primary development environments:
- Windows
- macOS
- IntelliJ IDEA

Always preserve cross-platform compatibility.

---

# Project Philosophy

QOLmod follows these principles:

> QOLmod doesn't add features because clients are expected to have them. Features exist because something encountered during real gameplay could be made better.

> Shared problems belong in the core; feature-specific problems stay in the feature.

> Automation should describe intent, not fight the player.

Prefer:
- focused quality-of-life features
- reusable architecture where a real shared problem exists
- contextual information
- restrained automation
- cohesive UX

Avoid:
- adding generic client modules simply because other clients have them
- speculative frameworks without a current need
- duplicated abstractions
- over-engineering
- feature-specific logic leaking into generic systems

QOLmod is intended to complement larger utility clients rather than duplicate them.

---

# Core Architecture

The core bootstrap should remain small.

Important systems include:

- `QOL`
- `QOLContext`
- `QOLEventBus`
- `FeatureManager`
- `ConfigManager`
- `KeybindManager`
- `FabricEventBridge`
- Automation system
- HUD system

Preserve existing architectural boundaries unless there is a concrete reason to change them.

Do not perform broad architectural rewrites during unrelated feature work.

---

# Features

Features should use the existing feature lifecycle and settings architecture.

Keep feature-specific behaviour inside the feature unless there is a proven reusable problem.

Do not create a generic framework purely because a second implementation might exist someday.

Storage Labels and Auto Duper are existing production features and should not be behaviourally changed during unrelated work.

---

# Automation

QOLmod has an existing automation kernel.

Important concepts include:

- `AutomationEngine`
- `AutomationWorkflow`
- `TaskContext`
- `InputController`
- `ControlledInput`
- `QOLTask`
- `TaskResult`
- workflow/task composition

Current automation intentionally supports one active workflow.

Do not add:
- workflow queues
- background workflow concurrency
- generic pathfinding frameworks
- generic interaction frameworks

unless a future task explicitly requires them.

Automation input ownership must remain safe and deterministic.

Emergency stop/reset behaviour must release controlled input correctly.

---

# Auto Duper

Auto Duper has already been migrated to the automation kernel.

Do not rewrite its sequence during unrelated work.

Current behaviour includes:
- donkey interaction
- inventory handling
- moving shulkers
- applying chest
- moving duplicated items
- dismounting
- cycle handling

HUD and UI code must not inspect Auto Duper internals directly.

Use adapters/publishers where generic presentation systems need Auto Duper information.

---

# GUI

The base QOLmod GUI is considered complete.

Do not redesign it unless explicitly requested.

Current visual identity:

- compact
- dark translucent glass
- cobalt accent
- Minecraft world visible behind the interface
- icon + label sidebar
- internal module settings view
- restrained structural lines
- minimal clutter

Settings open inside the main shell.

Module interaction:
- left click: toggle
- right click: open settings
- explicit settings affordances may also remain

Do not reintroduce external settings drawers.

Do not add fullscreen dimming or opaque menu backgrounds.

---

# HUD Philosophy

QOLmod HUD philosophy:

> Contextual information, not permanent information.

If nothing meaningful is happening, the QOLmod HUD should mostly disappear.

Do not add generic always-on HUD modules such as:
- FPS
- coordinates
- ping
- TPS
- armor
- potion effects
- keystrokes
- CPS

unless explicitly requested for a real QOL use case.

---

# Contextual Status HUD

The Phase 5 contextual HUD is generic.

Important concepts include:

- `HudManager`
- `StatusRegistry`
- `StatusCardData`
- `StatusProgress`
- `StatusStackWidget`

Status cards represent:

> Something is currently happening.

Status sources use stable IDs.

Future modules should be able to publish generic status without modifying HUD rendering code.

Do not make status rendering feature-specific.

The system must remain compatible with multiple future status sources even though the automation engine currently permits one active workflow.

---

# Notifications

Notifications are separate from status cards.

Notifications represent:

> Something happened and briefly needs attention.

Supported semantic types include:
- INFO
- SUCCESS
- WARNING
- ERROR

Important behaviour:
- transient
- keyed deduplication
- independent unkeyed entries
- finite lifetime
- compact stacking
- no persistent history

Notification rendering must remain generic.

Do not turn normal task activity into notification spam.

Long notification details should wrap cleanly.
Titles/details must never render outside their card.

---

# HUD Editor

The HUD editor supports presentation configuration for registered HUD widgets.

Current editable properties include:

- enabled
- anchor
- x/y offset
- scale
- opacity

Editor preview data must remain presentation-only and must not enter real gameplay registries.

The editor should support future HUD widgets through generic metadata/config rather than large hardcoded switch statements.

Anchor changes currently preserve visual screen position by recalculating offsets.

Do not change this behaviour unless explicitly requested.

---

# Rendering

Rendering effects must remain local to QOLmod UI/HUD surfaces.

Never:
- blur the entire world
- tint the entire framebuffer
- dim the entire screen for QOLmod glass
- apply fullscreen framebuffer effects for panel rendering

The world outside QOLmod glass should remain normal and sharp.

Prefer reusable panel-local rendering/material abstractions.

Avoid introducing expensive rendering passes without measuring whether they are necessary.

---

# UI Theme

Default identity:
- dark glass
- cobalt accent
- strong readable primary text
- readable secondary text
- restrained muted metadata
- subtle structural dividers

Do not hardcode new random colors if an appropriate semantic theme color exists.

Prefer semantic roles such as:
- primary text
- secondary text
- muted text
- accent
- structural divider
- subtle divider
- border

---

# Testing

For code changes, run:

    ./gradlew clean build

Run all tests.

Then run:

    git diff --check

Add tests for new logic where practical.

Prefer extracting coordinate/lifecycle/configuration logic into testable non-rendering classes when reasonable.

Do not create tests that tightly couple to implementation details without benefit.

---

# Visual Verification

Do NOT use Computer Use / desktop automation to claim Minecraft visual verification on the current Windows environment.

Minecraft cannot be reliably controlled there because of Windows integrity-level restrictions.

Verification policy:

Codex:
- build
- unit tests
- static checks
- runtime initialization
- logs
- non-destructive smoke tests

User:
- visual appearance
- interaction feel
- gameplay behaviour requiring real input/inventory state

Do not claim visual verification simply because Minecraft launched successfully.

Do not ask the user to manually manufacture artificial error/warning scenarios when automated tests or editor preview mode can validate them.

---

# Runtime Safety

Avoid destructive gameplay actions during automated smoke testing.

Do not automatically:
- alter inventories
- trigger exploit sequences
- manipulate existing worlds
- start Auto Duper against the user's real world state

Temporary non-shipping test hooks are acceptable when safe.

Remove them before final build.

---

# Configuration

Use the existing configuration system.

Do not introduce parallel config storage for new UI/HUD systems.

New config fields must:
- have safe defaults
- tolerate upgrades from previous config versions
- remain cross-platform

Avoid synchronous disk writes on every frame or every drag pixel.

Prefer saving on meaningful interaction completion when appropriate.

---

# Git / Cross-Machine Workflow

Development happens across Windows and macOS.

GitHub is the handoff point between machines.

Before switching machines:

    git status --untracked-files=all
    git add -A
    git status
    git commit
    git push
    git status

Do not assume:

> Everything up-to-date

means untracked files are safe.

Before starting work on another machine:

    git fetch origin
    git pull --ff-only
    git status

Do not force-push over legitimate remote work.

Use rebase/fetch when necessary.

The repository uses `.gitattributes` to keep line endings deterministic across platforms.

---

# Branching

Use meaningful phase branches for substantial milestones.

Completed milestones include:

- `phase-1-core`
- `phase-2-automation-kernel`
- `phase-3-autoduper-migration`
- `phase-4-ui`
- `phase-5a-contextual-hud`

Current Phase 5 work builds on the contextual HUD architecture.

Do not create a new branch for every tiny polish change.

---

# Scope Discipline

When given a focused task:

1. inspect the existing implementation first
2. identify the smallest clean change
3. preserve unrelated behaviour
4. do not automatically continue into the next planned feature
5. stop when the requested scope is complete

If something is explicitly deferred, leave it deferred.

---

# Currently Deferred

Unless explicitly requested, do not begin:

- generic permanent HUD modules
- notification history / notification center
- notification actions
- panic keybind
- new automation workflows
- donkey assistant
- llama finder
- mapart assistant integrations
- plugin/hot-load system
- generalized pathfinding system
- broad shader redesign

These may be future work but are not standing tasks.

---

# Development Style

Prefer:
- straightforward Java
- descriptive names
- small focused classes
- composition
- explicit ownership
- minimal coupling
- testable logic
- comments explaining non-obvious reasons, not obvious code

Avoid:
- giant manager classes
- unnecessary inheritance
- speculative abstraction layers
- duplicated state
- magic strings where stable semantic IDs/types are appropriate
- feature-specific switch statements inside generic renderers

When an existing abstraction already solves the problem, use it rather than creating a second version.

---

# Completion Reports

For substantial Codex tasks, report:

1. what changed
2. important architectural decisions
3. files/systems affected
4. behaviour intentionally preserved
5. tests/build results
6. anything requiring manual verification
7. anything intentionally deferred

Do not claim manual visual verification unless it was actually performed by the user.