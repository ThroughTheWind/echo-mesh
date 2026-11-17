# Text field

**Purpose:** short typed input. Echo has almost none by design: no free text on reports (SPEC), so text fields appear only for local names (group name, nickname).

## Anatomy

Label above (`label-medium`, always visible, never a placeholder-only label), 72dp outlined box (`radius-md`, 2dp `outline`), input in `body-large`, optional trailing icon button, support text below in `on-surface-variant`.

## States

Enabled, focused (3dp `primary` border), error (3dp `error` border; the support text becomes the error message and says how to fix it), disabled.

## Rules

- Every field has support text saying who sees what is typed ("Only Crew sees it, encrypted").
- No autocomplete from the keyboard's learned words: set `KeyboardOptions(autoCorrect = false)` and `imeOptions` with `IME_FLAG_NO_PERSONALIZED_LEARNING` so names don't end up in the keyboard's dictionary.

**Compose:** M3 `OutlinedTextField` with `Modifier.heightIn(min = Touch.comfort)` and the keyboard options above.

## Tokens

| Token | Value |
|---|---|
| `--c-text-field-height` | `{touch-comfort}` |
| `--c-text-field-radius` | `{radius-md}` |
| `--c-text-field-border` | `{outline}` |
| `--c-text-field-border-width` | `2px` |
| `--c-text-field-focus-border` | `{primary}` |
| `--c-text-field-error-border` | `{error}` |
| `--c-text-field-label` | `{label-medium}` |
| `--c-text-field-input` | `{body-large}` |
| `--c-text-field-support` | `{on-surface-variant}` |
