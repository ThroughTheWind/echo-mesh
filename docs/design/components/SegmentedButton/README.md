# Segmented button

**Purpose:** choose one of two or three options that are always visible together, such as the report audience ("Everyone nearby" / "Crew only") or the theme.

## Anatomy

64dp tall, `radius-lg`, 2dp `outline` border; the selected segment fills with `secondary-container`. Optional leading `icon-sm`. Labels in `label-medium`, never truncated: if a label doesn't fit, use a list of radio buttons instead.

## Accessibility

A `radiogroup` with one `radio` per segment; the group has a label ("Send to").

**Compose:** M3 `SingleChoiceSegmentedButtonRow` with height `Touch.min`.

## Tokens

| Token | Value |
|---|---|
| `--c-segmented-height` | `{touch-min}` |
| `--c-segmented-radius` | `{radius-lg}` |
| `--c-segmented-border` | `{outline}` |
| `--c-segmented-selected-container` | `{secondary-container}` |
| `--c-segmented-selected-content` | `{on-secondary-container}` |
