# ReportDetail

The sheet that opens from a pin: what it is, where, how long it lasts, who vouches for it, and Still there / Gone (SPEC F6, FR-5.5, FR-6.x).

**Compose:** `ReportDetailSheet(report, onConfirm, onDismiss, onWithdraw)`.

## Anatomy

- **Header:** the pin (static, with ring and badges), type in `glance-title`, then `label-medium` "About 120 m north-east · reported 6 min ago". Distance is rounded (10 m steps under 100 m, 50 m above), ages to the minute.
- **ExpiryIndicator bar:** "11 min left of 15".
- **Provenance box:** the honest answer to "should I trust this?"
  - Group: `surface-container-high` with a 4dp `group-N` bar, lock icon. "**From Crew.** Signed by a member's phone and encrypted to your group. Relayed 2 hops."
  - Public: 2dp **dashed** `outline` box, info icon. "**From a phone nearby.** Anyone running the app can send these, and it could be wrong or fake. Relayed 4 hops."
- **Votes line:** split into group and nearby (FR-6.2). "N nearby phones confirmed", never "verified" (FR-6.5).
- **Actions** (bottom, side by side, `touch-comfort`): **Still there** (tonal) and **Gone** (outlined). After voting, your choice shows as "You said gone" and the other button stays available to change your mind until expiry.

## Variants

- **Your own report:** no voting; one `danger-tonal` "Withdraw report" (FR-4.5).
- **Probably gone:** header line says so with counts, plus a callout "Shown faded, not removed: votes from strangers can be faked in bulk, so you decide."

## Rules

- Never shows a sender, a device, or anything that could identify one.
- The sheet opens at half height over the map so the pin stays visible above it.
- Final provenance wording comes from the threat model (SPEC NFR-14).
