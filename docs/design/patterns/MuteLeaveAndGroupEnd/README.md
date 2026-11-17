# Mute, leave and group end

**Purpose:** the controls people have over a group once it exists (SPEC FR-8.5, FR-8.6, FR-9.4, AC-16, AC-24).

## Mute a member (FR-9.4)

Tap a member in the [roster](../../screens/GroupRoster/README.md): a sheet with their avatar, state, **Show on map** (if sharing) and **Mute {name}**. "Muting hides Sage's reports, votes and position on this phone only. Sage stays in Crew and isn't told." A muted member stays in the roster as "Muted · hidden on this phone" with **Unmute**. Their pins disappear from this map within one refresh (AC-24).

## A member you don't trust

There's no remove (FR-8.5). The roster's warning callout says what to do instead, using the threat model's words: "Think someone in Crew can't be trusted, or a member's phone was taken? You can't remove anyone. Make a new group in person and stop using Crew."

## Leave a group

From the roster's bottom: a [dialog](../../components/Dialog/README.md) "Leave Crew? Its keys are deleted from this phone. To come back, someone has to show you a code again." Leaving needs no hold: it's not an emergency action, and it's recoverable in person.

## A group ends (FR-8.6)

At the end of its lifetime the keys are deleted on every phone. The groups list shows an info callout "Crew ended. Its keys were deleted from this phone and every other member's." once, then the group is gone. An hour before the end, the roster header reads "ends in 1 h".
