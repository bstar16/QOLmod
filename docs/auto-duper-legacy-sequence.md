# AutoDuper legacy sequence

This document records the behavior of the former `DupeSequencer` before the
Phase 3 automation migration. It remains as the parity record for the legacy
implementation, including quirks that were evaluated during migration.

## Lifecycle and cycle accounting

- Enabling AutoDuper resets the sequencer to stage 0 and clears its item move
  plan and tick delay.
- With `mount-without-chest` enabled, startup remembers the selected hotbar
  slot and selects the first slot that does not contain a chest.
- A cycle becomes active when the feature observes stage 1. It is counted when
  the sequencer later returns to stage 0.
- `cycles = 0` repeats indefinitely. A positive value stops after exactly that
  many observed returns to stage 0.
- Normal finite completion currently disables the feature through the error
  path. Internal sequencer failures reset the sequencer, but can leave the
  feature enabled and inert because the feature cannot observe `running`.

## Per-cycle sequence

1. Unless `mount-without-chest` is enabled, scan hotbar slots 0 through 8 for
   the first vanilla chest, select it, and wait `keypress-delay`. Missing chest
   calls the sequencer's internal stop path.
2. Find the closest unoccupied `DonkeyEntity` at a squared distance strictly
   less than 25. Rotate the client player's yaw and pitch toward it. In
   `mount-without-chest` mode, require a chest somewhere in player inventory
   slots 0 through 35. Interact with the donkey using the main hand and wait
   `keypress-delay`.
3. Check `player.getVehicle() instanceof DonkeyEntity`. If false, return to the
   interaction step and retry indefinitely. If true, wait `mount-delay`.
   Starting a cycle already mounted skips this delay.
4. While mounted on a donkey, send `ClientCommandC2SPacket` with
   `OPEN_INVENTORY`, then wait `inventory-delay`.
5. Require `HorseScreen`. If it is absent, return to the open-inventory step
   and retry indefinitely. Once present, wait `keypress-delay`.
6. Build an item move plan. Scan horse-handler slots 17 through 52 for shulker
   boxes and pair them, in order, with empty slots 2 through 16. Move each pair
   with a `PICKUP` click on the source, one clear client tick, then a `PICKUP`
   click on the destination and another clear tick. Invalid or newly empty
   planned slots are skipped. Wait `move-items-delay` after the plan finishes.
7. Require a chest in hotbar slots 0 through 8, select it, and interact with the
   mounted donkey using the main hand while the horse screen remains open. In
   `mount-without-chest` mode, remember the previously selected slot and switch
   back to a non-chest hotbar slot immediately after interacting. Wait
   `chest-apply-delay`.
8. Inspect slots 2 through 16 and quick-move each nonempty stack back to the
   player. When `shulkers-only` is enabled, only stacks whose item identifier
   contains `shulker_box` are moved. This option does not change the earlier
   player-to-donkey move plan. Wait `move-items-delay`.
9. Close the current handled screen and wait `keypress-delay`.
10. If still mounted on a donkey, directly press the Minecraft sneak key and
    wait `dismount-delay`. Release the key, wait `dismount-delay`, and check for
    `player.getVehicle() == null`. Retry the press/release sequence until the
    player dismounts. In `mount-without-chest` mode, finish on a non-chest
    hotbar slot.

Each configured delay uses `max(0, (int) (seconds * 20))`, which truncates
fractional ticks. Setting the legacy `tickDelay` to `N` consumes the next `N`
client ticks before the following stage executes.

## Packets, interactions, and slot assumptions

- Mounting and chest application use
  `ClientPlayerInteractionManager.interactEntity(..., Hand.MAIN_HAND)`.
- Opening the mount inventory directly sends
  `ClientCommandC2SPacket(OPEN_INVENTORY)`.
- Item movement uses `clickSlot` with either `PICKUP` or `QUICK_MOVE`.
- Screen close uses `ClientPlayerEntity.closeHandledScreen()`.
- Dismount relies on vanilla processing a held sneak key.
- Donkey cargo is assumed to occupy handler slots 2 through 16 and player
  inventory is assumed to occupy slots 17 through 52. These ranges are part of
  the exploit behavior and are not inferred dynamically.
- Only vanilla `Items.CHEST` is accepted. The initial
  `mount-without-chest` check searches all 36 player inventory slots, but the
  later apply step still requires the chest to be in the hotbar.
- Shulker detection is identifier-string based (`contains("shulker_box")`).

## Legacy failure and cleanup behavior

- Missing client state, an explicitly detected inventory close during stages
  4 through 7, or finite completion disables the feature through its error
  path.
- A missing donkey or chest invokes `DupeSequencer.stop()`, which resets its
  internal running flag but does not reliably disable the feature.
- Mount confirmation, inventory opening, and dismount have no timeouts.
- Losing `HorseScreen` during item movement or chest application can leave the
  stage running forever, especially if another screen replaced it.
- Closing the screen before stage 8 can leave the close stage running forever.
- Reset directly releases the Minecraft sneak key. No other screen, cursor,
  mount, rotation, or selected-slot state is restored.

## Runtime migration correction

Controlled testing found that this server's exploit requires the chest to be
selected before and during mounting. The legacy `mount-without-chest` option
therefore creates a sequence that runs to completion without producing the
dupe. Phase 3 removes that non-working user-facing setting and always uses the
validated chest-held sequence. After controlled runtime parity validation, the
legacy sequencer and its dormant config accessor were removed.
