# Avatar and badge

## Avatar

**Purpose:** identify a group member or a group without a photo. Echo never shows photos or contact names.

- 40dp circle (48dp large), `secondary-container` with two initials of the nickname in `label-medium` 800.
- A 3dp ring in the member's group colour (`group-N`), or `primary` for you.
- **Stale** (not seen for 2 minutes): dashed-looking `outline` ring and 70% opacity.

## Badge

**Purpose:** a small count or dot on another element.

- 24dp, `inverse-surface`, `label-small`, tabular figures. Counts cap at "20+".
- A 12dp `error` dot for "something new" without a number.
- Always included in the parent's content description ("Groups, 1 new member").

## Tokens

| Token | Value |
|---|---|
| `--c-avatar-size` | `40px` |
| `--c-avatar-size-large` | `48px` |
| `--c-avatar-container` | `{secondary-container}` |
| `--c-avatar-content` | `{on-secondary-container}` |
| `--c-avatar-ring` | `{primary}` |
