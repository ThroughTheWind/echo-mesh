package io.github.throughthewind.echo.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.mesh.group.GroupBook
import io.github.throughthewind.echo.core.mesh.group.GroupInfo
import io.github.throughthewind.echo.core.mesh.group.MemberInfo
import io.github.throughthewind.echo.core.mesh.group.MemberPosition
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.core.mesh.node.Sharing
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.ui.common.BackBar
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.rememberNow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import io.github.throughthewind.echo.core.designsystem.R as DS

/** Where the groups screens are. No navigation library: a handful of pages fit in a `when`. */
private sealed interface Page {
    data object List : Page

    data object Create : Page

    data class Roster(
        val id: Bytes,
    ) : Page

    data class Code(
        val id: Bytes,
    ) : Page

    data object Scan : Page
}

/**
 * The groups screens (WI-017, WI-022): the list, creating a group, a group's roster, showing a join
 * code and scanning one. Built to screens/GroupRoster, patterns/CreateGroup and patterns/QrJoinFlow;
 * sharing your position (patterns/PositionSharing) and muting a member (FR-9.4) live on the roster.
 */
@Composable
fun GroupsScreen(
    app: App,
    onBack: () -> Unit,
) {
    var page by remember { mutableStateOf<Page>(Page.List) }
    BackHandler(enabled = page != Page.List) { page = Page.List }
    val groups by app.mesh.groups.collectAsStateWithLifecycle()
    val meshState by app.mesh.state.collectAsStateWithLifecycle()
    val sharing by app.mesh.sharing.collectAsStateWithLifecycle()
    val positions by app.mesh.positions.collectAsStateWithLifecycle()
    val channel = app.mesh.groupChannel
    val toList = { page = Page.List }
    when (val current = page) {
        Page.List -> {
            GroupList(
                groups = groups,
                meshOn = meshState is MeshState.On,
                hasRadio = channel != null,
                onBack = onBack,
                onOpen = { page = Page.Roster(it) },
                onCreate = { page = Page.Create },
                onScan = { page = Page.Scan },
            )
        }

        Page.Create -> {
            CreateGroup(
                onBack = toList,
                onCreate = { name, lifetime, nickname ->
                    channel?.create(name, nickname, lifetime)?.let { page = Page.Roster(it.id) }
                },
            )
        }

        is Page.Roster -> {
            val group = groups.firstOrNull { it.id == current.id }
            if (group == null) {
                LaunchedEffect(Unit) { toList() }
            } else {
                GroupRoster(
                    group = group,
                    meshOn = meshState is MeshState.On,
                    sharing = sharing.firstOrNull { it.groupId == group.id },
                    positions = positions.filter { it.groupId == group.id },
                    onShare = { duration -> app.mesh.startSharing(group.id, duration?.let { Clock.System.now() + it }) },
                    onStopSharing = { app.mesh.stopSharing(group.id) },
                    onMute = { member, muted -> channel?.mute(group.id, member, muted) },
                    onBack = toList,
                    onAdd = { page = Page.Code(group.id) },
                    onLeave = {
                        channel?.leave(group.id)
                        page = Page.List
                    },
                )
            }
        }

        is Page.Code -> {
            val group = groups.firstOrNull { it.id == current.id }
            if (group == null || channel == null) {
                LaunchedEffect(Unit) { toList() }
            } else {
                ShowCode(group = group, channel = channel, onBack = { page = Page.Roster(group.id) })
            }
        }

        Page.Scan -> {
            if (channel == null) {
                LaunchedEffect(Unit) { toList() }
            } else {
                ScanToJoin(
                    channel = channel,
                    full = groups.size >= GroupBook.MAX_GROUPS,
                    onBack = toList,
                    onJoined = { page = Page.Roster(it) },
                )
            }
        }
    }
}

@Composable
private fun GroupList(
    groups: List<GroupInfo>,
    meshOn: Boolean,
    hasRadio: Boolean,
    onBack: () -> Unit,
    onOpen: (Bytes) -> Unit,
    onCreate: () -> Unit,
    onScan: () -> Unit,
) {
    GroupPage(title = stringResource(R.string.groups_title), onBack = onBack) {
        if (groups.isEmpty()) Text(stringResource(R.string.groups_empty), style = MaterialTheme.typography.bodyLarge)
        groups.forEachIndexed { index, group ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.comfort)
                    .clickable { onOpen(group.id) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
            ) {
                Monogram(group.name, index)
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.groups_row, group.members.size, ends(group.expiresAt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val full = groups.size >= GroupBook.MAX_GROUPS
        if (!hasRadio) {
            Text(stringResource(R.string.groups_no_radio), style = MaterialTheme.typography.bodyMedium)
        } else {
            if (full) Text(stringResource(R.string.groups_full), style = MaterialTheme.typography.bodyMedium)
            PrimaryButton(stringResource(R.string.groups_create), onCreate, enabled = !full)
            if (!meshOn) Text(stringResource(R.string.groups_mesh_needed), style = MaterialTheme.typography.bodyMedium)
            SecondaryButton(stringResource(R.string.groups_join), onScan)
        }
        IconLine(DS.drawable.ic_info, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.groups_cant_remove))
    }
}

private val lifetimes: List<Pair<Int, Duration>> =
    listOf(
        R.string.lifetime_6h to 6.hours,
        R.string.lifetime_24h to 24.hours,
        R.string.lifetime_3d to 3.days,
        R.string.lifetime_7d to 7.days,
    )

@Composable
private fun CreateGroup(
    onBack: () -> Unit,
    onCreate: suspend (name: String, lifetime: Duration, nickname: String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var lifetime by remember { mutableStateOf(GroupBook.DEFAULT_LIFETIME) }
    var nickname by remember { mutableStateOf(Nicknames.random()) }
    GroupPage(title = stringResource(R.string.create_title), onBack = onBack) {
        BoundedField(name, { name = it }, stringResource(R.string.create_name_label), stringResource(R.string.create_name_help))
        Text(
            stringResource(R.string.create_lifetime),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
            lifetimes.forEach { (label, value) ->
                FilterChip(
                    selected = lifetime == value,
                    onClick = { lifetime = value },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.heightIn(min = Touch.min),
                )
            }
        }
        Callout(stringResource(R.string.create_lifetime_help))
        NicknameField(nickname) { nickname = it }
        val ready = Nicknames.fits(name) && Nicknames.fits(nickname)
        PrimaryButton(
            text =
                if (name.isBlank()) {
                    stringResource(
                        R.string.create_button_plain,
                    )
                } else {
                    stringResource(R.string.create_button, name.trim())
                },
            onClick = { scope.launch { onCreate(name.trim(), lifetime, nickname.trim()) } },
            enabled = ready,
        )
    }
}

@Composable
private fun GroupRoster(
    group: GroupInfo,
    meshOn: Boolean,
    sharing: Sharing?,
    positions: List<MemberPosition>,
    onShare: (Duration?) -> Boolean,
    onStopSharing: () -> Unit,
    onMute: suspend (Bytes, Boolean) -> Unit,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onLeave: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var confirmLeave by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var memberSheet by remember { mutableStateOf<Bytes?>(null) }
    val now = rememberNow()
    GroupPage(title = group.name, onBack = onBack) {
        Text(
            stringResource(R.string.roster_ends, ends(group.expiresAt)),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShareRow(
            groupName = group.name,
            sharing = sharing,
            meshOn = meshOn,
            now = now,
            onToggle = { on -> if (on) choosing = true else onStopSharing() },
        )
        Text(
            stringResource(R.string.roster_members),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        group.roster.forEachIndexed { index, member ->
            val shared = positions.filter { it.member == member.id && now < it.expiresAt }.maxByOrNull { it.at }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = Touch.comfort)
                    .clickable(enabled = !member.isMe) { memberSheet = member.id },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
            ) {
                val faded = member.muted || (shared != null && now - shared.at > STALE_AFTER)
                Box(Modifier.alpha(if (faded) STALE_ROW_ALPHA else 1f)) {
                    Monogram(member.nickname, index)
                }
                Column {
                    Text(
                        if (member.isMe) stringResource(R.string.roster_you, member.nickname) else member.nickname,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    val state =
                        when {
                            member.muted -> stringResource(R.string.member_muted)
                            shared != null -> stringResource(R.string.member_sharing, seenAgo(shared.at, now))
                            else -> null
                        }
                    if (state != null) {
                        Text(state, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        // Once anyone is muted, say what muting cannot do (screens/GroupRoster).
        if (group.roster.any { it.muted }) Callout(stringResource(R.string.roster_muted_callout), warning = true)
        if (!meshOn) Text(stringResource(R.string.groups_mesh_needed), style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(stringResource(R.string.roster_add), onAdd, enabled = meshOn)
        SecondaryButton(stringResource(R.string.roster_leave, group.name), { confirmLeave = true })
        IconLine(DS.drawable.ic_info, MaterialTheme.colorScheme.onSurfaceVariant, stringResource(R.string.groups_cant_remove))
    }
    group.roster.firstOrNull { it.id == memberSheet }?.let { member ->
        MemberSheet(
            member = member,
            groupName = group.name,
            onMute = { muted -> scope.launch { onMute(member.id, muted) } },
            onDismiss = { memberSheet = null },
        )
    }
    if (choosing) {
        ShareSheet(
            groupName = group.name,
            onStart = { duration ->
                choosing = false
                onShare(duration)
            },
            onDismiss = { choosing = false },
        )
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.leave_title, group.name)) },
            text = { Text(stringResource(R.string.leave_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    scope.launch { onLeave() }
                }) {
                    Text(stringResource(R.string.leave_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private const val STALE_ROW_ALPHA = 0.5f

/**
 * A member's sheet (patterns/MuteLeaveAndGroupEnd): Mute hides their reports, votes and position on
 * this phone only (FR-9.4); they stay in the group and are not told.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberSheet(
    member: MemberInfo,
    groupName: String,
    onMute: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = spacing.xxl)
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(member.nickname, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (member.muted) {
                Text(stringResource(R.string.member_muted), style = MaterialTheme.typography.bodyLarge)
                PrimaryButton(stringResource(R.string.member_unmute, member.nickname), {
                    onMute(false)
                    onDismiss()
                })
            } else {
                Text(
                    stringResource(R.string.member_mute_body, member.nickname, groupName),
                    style = MaterialTheme.typography.bodyLarge,
                )
                SecondaryButton(stringResource(R.string.member_mute, member.nickname), {
                    onMute(true)
                    onDismiss()
                })
            }
        }
    }
}

/**
 * "Share my position" (screens/GroupRoster): off by default every time (FR-10.1). Turning it on asks
 * for a duration first; turning it off stops at once, with no confirm (FR-10.4).
 */
@Composable
private fun ShareRow(
    groupName: String,
    sharing: Sharing?,
    meshOn: Boolean,
    now: Instant,
    onToggle: (Boolean) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(value = sharing != null, enabled = meshOn || sharing != null, role = Role.Switch, onValueChange = onToggle),
    ) {
        Row(
            Modifier.padding(LocalSpacing.current.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.roster_share), style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        sharing != null -> stringResource(R.string.roster_share_on, groupName, sharingLeft(sharing, now))
                        meshOn -> stringResource(R.string.roster_share_off)
                        else -> stringResource(R.string.share_mesh_off)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(checked = sharing != null, onCheckedChange = null, enabled = meshOn || sharing != null)
        }
    }
}

/** A scrolling page under a back bar, with the design's spacing. */
@Composable
internal fun GroupPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(topBar = { BackBar(title, onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(LocalSpacing.current.xxl),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
            content = content,
        )
    }
}

/** A warning or guidance callout (components/Callout), on the blocked container for warnings. */
@Composable
internal fun Callout(
    text: String,
    warning: Boolean = false,
) {
    val street = LocalStreetColors.current
    Surface(
        color = if (warning) street.blockedContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (warning) street.onBlockedContainer else MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(LocalSpacing.current.lg), horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md)) {
            Icon(painterResource(if (warning) DS.drawable.ic_warning else DS.drawable.ic_info), contentDescription = null)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** A text field that refuses more than a nickname's 32 UTF-8 bytes, and says so. */
@Composable
internal fun BoundedField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    help: String,
) {
    val tooLong = value.trim().encodeToByteArray().size > Nicknames.MAX_BYTES
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(if (tooLong) stringResource(R.string.nickname_too_long) else help) },
        isError = tooLong,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The nickname for one group, with a random default and a button for another (FR-8.4). */
@Composable
internal fun NicknameField(
    nickname: String,
    onChange: (String) -> Unit,
) {
    BoundedField(nickname, onChange, stringResource(R.string.nickname_label), stringResource(R.string.nickname_help))
    TextButton(onClick = { onChange(Nicknames.random()) }, modifier = Modifier.heightIn(min = Touch.min)) {
        Icon(painterResource(DS.drawable.ic_sync), contentDescription = null)
        Text(stringResource(R.string.nickname_new), modifier = Modifier.padding(start = LocalSpacing.current.sm))
    }
}

/** Initials in a group-coloured circle (components/AvatarAndBadge). */
@Composable
private fun Monogram(
    name: String,
    index: Int,
) {
    val street = LocalStreetColors.current
    val colour = listOf(street.group1, street.group2, street.group3, street.group4, street.group5)[index % 5]
    Surface(shape = MaterialTheme.shapes.extraLarge, color = colour, modifier = Modifier.size(48.dp)) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Text(
                name
                    .split(' ')
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}

/** When a group ends, in the phone's own date and time format. */
internal fun ends(at: Instant): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(at.toEpochMilliseconds()))
