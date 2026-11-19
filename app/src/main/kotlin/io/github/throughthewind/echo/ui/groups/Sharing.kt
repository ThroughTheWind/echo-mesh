package io.github.throughthewind.echo.ui.groups

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.component.groupColor
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.map.OwnPosition
import io.github.throughthewind.echo.core.mesh.group.MemberPosition
import io.github.throughthewind.echo.core.mesh.node.Sharing
import io.github.throughthewind.echo.core.mesh.node.SharingStopped
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.core.model.GeoPoint
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.initials
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** The durations the share sheet offers (FR-10.1); two hours is the default. Null: until the mesh is off. */
enum class ShareFor(
    val duration: Duration?,
    @param:StringRes val label: Int,
) {
    HALF_HOUR(30.minutes, R.string.share_30_min),
    HOUR(1.hours, R.string.share_1_hour),
    TWO_HOURS(2.hours, R.string.share_2_hours),
    UNTIL_OFF(null, R.string.share_until_off),
}

/** Over this, a member's marker is drawn stale (patterns/PositionSharing). */
val STALE_AFTER: Duration = 2.minutes

/** How often the last fix is handed to the mesh again: the platform sends nothing while you stand still. */
private val RESTAMP: Duration = 30.seconds

/**
 * Hands the phone's own fix to the mesh while it shares with any group, and only while the app is on
 * screen: never background location (NFR-10). The fix stays in memory.
 */
@Composable
fun ShareOwnPosition(app: App) {
    val sharing by app.mesh.sharing.collectAsStateWithLifecycle()
    val on = sharing.isNotEmpty()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(on) {
        if (!on) return@LaunchedEffect
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last: GeoPoint? = null
            launch {
                OwnPosition.updates(context).collect { fix ->
                    val point = GeoPoint(fix.latitude, fix.longitude)
                    last = point
                    app.mesh.updatePosition(point)
                }
            }
            while (true) {
                delay(RESTAMP)
                last?.let { app.mesh.updatePosition(it) }
            }
        }
    }
}

/**
 * Starting to share (patterns/PositionSharing, step 1): the group, a duration, what the group will see,
 * and location asked in context if it is missing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareSheet(
    groupName: String,
    onStart: (Duration?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var choice by rememberSaveable { mutableStateOf(ShareFor.TWO_HOURS) }
    var refused by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.any { it }) onStart(choice.duration) else refused = true
        }
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                stringResource(R.string.share_title, groupName),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Column {
                ShareFor.entries.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.min)
                            .selectable(selected = choice == option, role = Role.RadioButton, onClick = { choice = option }),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        RadioButton(selected = choice == option, onClick = null)
                        Text(stringResource(option.label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Callout(stringResource(R.string.share_callout, groupName))
            if (refused) {
                Text(
                    stringResource(R.string.share_needs_location),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            PrimaryButton(stringResource(R.string.share_start), {
                if (hasLocation(context)) {
                    onStart(choice.duration)
                } else {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }
            })
        }
    }
}

/** One bar per group shared with, under the top bar on every map view, each with its own Stop (FR-10.4). */
@Composable
fun SharingBars(
    sharing: List<Sharing>,
    now: Instant,
    onStop: (Bytes) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
        sharing.forEach { share ->
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = LocalSpacing.current.lg),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.sharing_bar, share.groupName, sharingLeft(share, now)),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onStop(share.groupId) }, modifier = Modifier.heightIn(min = Touch.min)) {
                        Text(stringResource(R.string.sharing_stop), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/** "1 h 42 left", "12 min left", or "until the mesh is off". */
@Composable
fun sharingLeft(
    share: Sharing,
    now: Instant,
): String {
    val until = share.until ?: return stringResource(R.string.sharing_until_off)
    val left = (until - now).coerceAtLeast(Duration.ZERO)
    val minutes = left.inWholeMinutes.toInt()
    return if (minutes >= 60) {
        stringResource(R.string.sharing_left_hours, minutes / 60, minutes % 60)
    } else {
        stringResource(R.string.sharing_left_minutes, minutes.coerceAtLeast(1))
    }
}

/** Why sharing stopped by itself, for the snackbar the next time the map is visible (FR-10.1). */
@Composable
fun SharingStopped.message(): String =
    stringResource(
        when (why) {
            SharingStopped.Why.TIME_UP -> R.string.sharing_stopped_time
            SharingStopped.Why.MESH_OFF -> R.string.sharing_stopped_mesh
            SharingStopped.Why.GROUP_GONE -> R.string.sharing_stopped_group
        },
        groupName,
    )

/** "40 s ago", "3 min ago". */
@Composable
fun seenAgo(
    at: Instant,
    now: Instant,
): String {
    val age = (now - at).coerceAtLeast(Duration.ZERO)
    return if (age < 1.minutes) {
        stringResource(R.string.age_seconds, age.inWholeSeconds.toInt())
    } else {
        stringResource(R.string.age_minutes, age.inWholeMinutes.toInt())
    }
}

/**
 * A member's marker (patterns/PositionSharing, step 4): their initials in the group's colour with the
 * age under it, faded once stale. It disappears when the position expires; the caller filters those.
 */
@Composable
fun MemberMarker(
    position: MemberPosition,
    colourIndex: Int,
    now: Instant,
    modifier: Modifier = Modifier,
) {
    val stale = now - position.at > STALE_AFTER
    val age = seenAgo(position.at, now)
    val description = stringResource(R.string.marker_described, position.nickname, position.groupName, age)
    Column(
        modifier
            .alpha(if (stale) STALE_ALPHA else 1f)
            .semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = groupColor(LocalStreetColors.current, colourIndex),
            modifier = Modifier.size(MARKER_DP.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(initials(position.nickname), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.surface)
            }
        }
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.small) {
            Text(age, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** The marker's circle, in dp; half of it is where the position is. */
const val MARKER_DP = 36

private const val STALE_ALPHA = 0.5f

internal fun hasLocation(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
