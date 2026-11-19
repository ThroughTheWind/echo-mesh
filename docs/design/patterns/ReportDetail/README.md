# Report detail

**Purpose:** the sheet that opens from a pin, or from a row in [Reports near you](../../screens/ReportsNearYou/README.md): what it is, where, how long it lasts, who vouches for it, and Still there / Gone (SPEC F6, FR-5.5, FR-6.x, US-10, US-11, AC-7, AC-8).

## Anatomy

- **Header:** the pin (with ring and badges), type in `glance-title`, then "About 120 m north-east · reported 6 min ago". Distance rounds to 10 m under 100 m and 50 m above; ages to the minute.
- **[Expiry](../../components/ExpiryIndicator/README.md) bar:** "11 min left of 15". A confirmation refreshes it, up to twice the default lifetime (FR-6.3).
- **Provenance box**, the honest answer to "should I trust this?":
  - Group: `surface-container-high` with a 4dp `group-N` bar and a lock. "**From Sage, in Crew.** Signed by Sage's phone and encrypted to your group. Relayed 2 hops." A group report names the member (threat model §7: "A group report shows which member sent it"), which is what makes [mute](../MuteLeaveAndGroupEnd/README.md) possible.
  - Public: a 2dp **dashed** `outline` box and an info icon. "**From a phone nearby.** Anyone running Echo can send these, and it could be fake. Arrived directly." (or "Relayed 4 hops", FR-5.5). No sender, ever.
- **Votes line**, split into group and nearby (FR-6.2): "Crew: 2 confirmed · Nearby: 3 confirmed, 1 said gone". Never "verified" (FR-6.5).
- **Actions:** **Still there** (tonal) and **Gone** (outlined), side by side, one vote per report per phone (FR-6.1): after voting the chosen button reads "You said gone" or "You said still there" and both lock. With no votes yet the votes line reads "No votes yet." A public report's group votes are labelled with the group's name when the phone is in one group, "Your groups" otherwise.

## Variants

- **Your own report:** no voting; **Withdraw report** (`danger-tonal`, FR-4.5). Withdrawal spreads like a report and removes the pin everywhere.
- **Probably gone** (FR-6.4): the pin and header fade, and a callout says which rule faded it:
  - public votes: "Faded because far more nearby phones said it's gone. Those votes can be faked, so it stays on the map until it expires."
  - a group member: "Faded because someone in Crew said it's gone and nobody in Crew has confirmed it since."
  Nothing removes a pin before expiry except its author's withdrawal.

## Accessibility

The sheet's heading is the type; the provenance box is read before the actions.
