# Foldable Test Matrix

Manual validation matrix for flip/fold form factors and large-screen behavior.

## Devices and modes
- Compact phone width (folded portrait).
- Expanded inner display (unfolded portrait/landscape).
- Tabletop posture (half-opened).
- Split-screen multi-window (app width compact/medium).

## Core flows
- Launch app -> open `Checks` -> open receipt -> back navigation.
- `Checks` -> `Scan` -> save receipt -> open new receipt.
- `Analytics` loading states and dialogs.
- `Settings` -> `DataBackup` and back stack restore.

## Transition checks
- Folded -> unfolded on `Checks` with list scroll position retained.
- Unfolded -> folded on `Receipt` without data reload loop.
- Rotation changes do not reset selection/dialog state.
- Multi-window resize does not cause clipped controls near hinge area.

## Acceptance criteria
- Navigation remains predictable and `launchSingleTop/restoreState` works.
- No critical controls overlap system insets or hinge area.
- Two-pane layouts are shown only when policy enables them.
- No crashes during posture or width-class transitions.
