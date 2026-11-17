# Callout

**Purpose:** an inline banner inside a screen or sheet: context the user should read before acting.

## Variants

| Variant | Colours | Use |
|---|---|---|
| Info | `surface-container-high` | Explanations, honesty notes |
| Warning | `blocked-container` | Risks of the action in front of you |
| Danger | `error-container` | Something is broken and needs a fix |

## Anatomy

`radius-md`, leading `icon-sm`, `body-medium` text, optional inline action in bold at the end. No close button: if it's worth showing, it stays while the condition holds.

**Compose:** a `Surface` with the role colours; not an M3 component.

## Tokens

| Token | Value |
|---|---|
| `--c-callout-radius` | `{radius-md}` |
| `--c-callout-info-container` | `{surface-container-high}` |
| `--c-callout-warn-container` | `{blocked-container}` |
| `--c-callout-warn-content` | `{on-blocked-container}` |
| `--c-callout-danger-container` | `{error-container}` |
| `--c-callout-danger-content` | `{on-error-container}` |
