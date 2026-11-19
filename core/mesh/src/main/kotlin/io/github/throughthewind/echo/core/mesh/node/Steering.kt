package io.github.throughthewind.echo.core.mesh.node

/**
 * A public steering report from another phone (extended spec FR-15.2, threat model §7 N7): it tells
 * people where to go or that a place is safe, anyone nearby could have sent it, and a fake one can
 * lead people into a trap. The screens show it as "Unconfirmed: could be a trap" on the pin, in the
 * list and in the detail, whatever its votes say.
 */
val ShownReport.couldBeATrap: Boolean
    get() = report.type.steering && group == null && !mine

/**
 * Whether the public ("nearby") confirmation count is hidden (FR-15.3): on every public steering
 * report, since a flooder can mint public confirmations. Group members' confirmations still show,
 * and dismissals and "probably gone" are unchanged (FR-15.4).
 */
val ShownReport.publicConfirmationsHidden: Boolean
    get() = report.type.steering && group == null

/** The confirmations the screens may show: the group's, and the public ones unless [publicConfirmationsHidden]. */
val ShownReport.confirmationsShown: Int
    get() = tally.groupConfirmed + if (publicConfirmationsHidden) 0 else tally.nearbyConfirmed
