# Hold-to-confirm button

**Purpose:** an irreversible action that must be deliberate but fast. Used only by the [panic wipe](../../patterns/PanicWipeConfirm/README.md).

## Anatomy

`touch-hero` (88dp), pill, `error-container` with `on-error-container` label. While held, an `error` fill grows left to right over `hold-to-confirm` (2 s); the filled part carries a second copy of the label in `on-error`, clipped to the fill, so the text stays readable over both colours.

## Behaviour

- Starts on finger down; letting go at any point empties the fill and does nothing.
- `WipeTick` haptic every `hold-tick` (0.5 s), except in discreet mode.
- The label counts down ("Keep holding · 1.2 s").

## Accessibility

TalkBack: custom action "Wipe everything" using the platform's double-tap-and-hold; the countdown is announced once.

**Compose:** custom, `Modifier.pointerInput { detectTapGestures(onPress = …) }` driving an `Animatable`.

## Tokens

| Token | Value |
|---|---|
| `--c-hold-height` | `{touch-hero}` |
| `--c-hold-duration` | `{hold-to-confirm}` |
| `--c-hold-tick` | `{hold-tick}` |
| `--c-hold-container` | `{error-container}` |
| `--c-hold-content` | `{on-error-container}` |
| `--c-hold-fill` | `{error}` |
| `--c-hold-fill-content` | `{on-error}` |
