package io.github.throughthewind.echo.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.throughthewind.echo.App
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.data.Settings
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.mesh.node.MeshState
import io.github.throughthewind.echo.ui.MapsState
import io.github.throughthewind.echo.ui.common.BackBar
import io.github.throughthewind.echo.ui.map.GetMapSheet
import io.github.throughthewind.echo.ui.wipe.LocalPanicWipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.provider.Settings as AndroidSettings

/** The theme choices, by the design system's `ThemePref` names (foundations/Themes). */
private val THEMES =
    listOf(
        "Auto" to R.string.settings_theme_auto,
        "Dark" to R.string.settings_theme_dark,
        "Sun" to R.string.settings_theme_sun,
    )

/** The vibration choices, by the design system's `HapticsLevel` names (foundations/Motion). */
private val HAPTICS =
    listOf("Off" to R.string.settings_haptics_off, "Light" to R.string.settings_haptics_light, "Strong" to R.string.settings_haptics_strong)

private const val SOURCE = "github.com/ThroughTheWind/echo-mesh"

private enum class Choice { Theme, Haptics }

/**
 * Settings (screens/SettingsScreen, WI-074): one scrolling list, every change applied at once.
 * Turning the mesh on goes back to the map, which asks for the permissions in context first
 * ([onTurnMeshOn]); turning it off happens here.
 */
@Composable
fun SettingsScreen(
    app: App,
    maps: MapsState,
    onBack: () -> Unit,
    onTurnMeshOn: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenProtects: () -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val settings by app.settings.settings.collectAsStateWithLifecycle()
    val meshState by app.mesh.state.collectAsStateWithLifecycle()
    val diagnosticsOn by app.mesh.diagnosticsOn.collectAsStateWithLifecycle()
    var background by remember { mutableStateOf(exempt(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { background = exempt(context) }
    var choice by rememberSaveable { mutableStateOf<Choice?>(null) }
    var getMap by rememberSaveable { mutableStateOf(false) }
    val discreet = settings.discreet
    val on = stringResource(R.string.settings_on)
    val off = stringResource(R.string.settings_off)

    fun save(change: (Settings) -> Settings) {
        scope.launch { withContext(Dispatchers.IO) { app.settings.update(change) } }
    }

    Scaffold(
        topBar = { BackBar(stringResource(R.string.settings_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = LocalSpacing.current.xxl),
        ) {
            Section(R.string.settings_mesh_section)
            val meshOn = meshState != MeshState.Off
            SwitchRow(
                stringResource(if (meshOn) R.string.mesh_switch_on else R.string.mesh_switch_off),
                stringResource(R.string.mesh_switch_support),
                meshOn,
            ) { next -> if (next) onTurnMeshOn() else scope.launch { app.meshOff() } }
            SettingRow(
                stringResource(R.string.settings_background),
                stringResource(if (background) R.string.settings_background_on else R.string.settings_background_off),
            ) { open(context, Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            SettingRow(stringResource(R.string.diagnostics_title), if (diagnosticsOn) on else off, onClick = onOpenDiagnostics)

            Section(R.string.settings_groups_section)
            SettingRow(stringResource(R.string.groups_title), stringResource(R.string.settings_groups_support), onClick = onOpenGroups)
            SettingRow(stringResource(R.string.settings_maps), stringResource(R.string.settings_maps_support)) { getMap = true }

            Section(R.string.settings_display_section)
            SettingRow(
                stringResource(R.string.settings_theme),
                if (discreet) {
                    stringResource(R.string.settings_theme_overridden)
                } else {
                    stringResource(THEMES.firstOrNull { it.first == settings.theme }?.second ?: R.string.settings_theme_auto)
                },
            ) { choice = Choice.Theme }
            SwitchRow(stringResource(R.string.settings_discreet), stringResource(R.string.settings_discreet_support), discreet) {
                scope.launch { app.setDiscreet(it) }
            }
            // Discreet mode overrides large pins: the row shows the value in force, and why.
            SwitchRow(
                stringResource(R.string.settings_large_pins),
                stringResource(if (discreet) R.string.settings_overridden else R.string.settings_large_pins_support),
                settings.largePins && !discreet,
                enabled = !discreet,
            ) { next -> save { it.copy(largePins = next) } }

            Section(R.string.settings_privacy_section)
            SettingRow(stringResource(R.string.protects_title), null, onClick = onOpenProtects)

            Section(R.string.settings_feedback_section)
            SettingRow(
                stringResource(R.string.settings_haptics),
                if (discreet) {
                    stringResource(R.string.settings_overridden)
                } else {
                    stringResource(HAPTICS.firstOrNull { it.first == settings.haptics }?.second ?: R.string.settings_haptics_light)
                },
                enabled = !discreet,
            ) { choice = Choice.Haptics }

            Section(R.string.settings_language_section)
            LanguageRow(context)

            Section(R.string.settings_wipe_section)
            LocalPanicWipe.current?.let { wipe ->
                SettingRow(stringResource(R.string.wipe_button), null, color = MaterialTheme.colorScheme.error, onClick = wipe)
            }

            Section(R.string.settings_about_section)
            SettingRow(stringResource(R.string.settings_version), version(context))
            SettingRow(stringResource(R.string.settings_licence), stringResource(R.string.settings_licence_value))
            SettingRow(stringResource(R.string.settings_source), SOURCE)
            SettingRow(stringResource(R.string.settings_security), stringResource(R.string.settings_security_value))
        }
    }

    when (choice) {
        Choice.Theme -> {
            ChoiceSheet(stringResource(R.string.settings_theme), THEMES, settings.theme, onDismiss = { choice = null }) { picked ->
                save { it.copy(theme = picked) }
            }
        }

        Choice.Haptics -> {
            ChoiceSheet(stringResource(R.string.settings_haptics), HAPTICS, settings.haptics, onDismiss = { choice = null }) { picked ->
                save { it.copy(haptics = picked) }
            }
        }

        null -> {
            Unit
        }
    }
    if (getMap) GetMapSheet(maps, onResult = { scope.launch { snackbar.showSnackbar(it) } }, onDismiss = { getMap = false })
}

/** A section heading: `label-medium` in `primary` (screens/SettingsScreen). */
@Composable
private fun Section(title: Int) {
    val spacing = LocalSpacing.current
    Text(
        stringResource(title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .padding(start = spacing.lg, end = spacing.lg, top = spacing.xl, bottom = spacing.sm)
                .semantics { heading() },
    )
}

/** A row that opens something, or only shows a value when [onClick] is null (components/ListItem). */
@Composable
private fun SettingRow(
    title: String,
    support: String?,
    enabled: Boolean = true,
    color: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick) else Modifier
    ListItem(
        headlineContent = { Text(title, color = color) },
        supportingContent = support?.let { { Text(it) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().heightIn(min = Touch.min).then(clickable),
    )
}

/** A row with a switch; the whole row toggles it. */
@Composable
private fun SwitchRow(
    title: String,
    support: String,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(support) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Touch.min)
                .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
    )
}

/**
 * The in-app language override (FR-18.1): Android 13 and later keep a language per app, and this
 * opens that setting. Before 13, Echo follows the phone's language.
 */
@Composable
private fun LanguageRow(context: Context) {
    val current =
        context.resources.configuration.locales[0]
            .displayName
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        SettingRow(stringResource(R.string.settings_language), stringResource(R.string.settings_language_support, current)) {
            open(context, Intent(AndroidSettings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        }
    } else {
        SettingRow(stringResource(R.string.settings_language), stringResource(R.string.settings_language_follows, current))
    }
}

/** More than two options: a sheet of radio buttons (screens/SettingsScreen). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceSheet(
    title: String,
    options: List<Pair<String, Int>>,
    selected: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val spacing = LocalSpacing.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(bottom = spacing.xxl)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = spacing.xxl).semantics { heading() },
            )
            for ((name, label) in options) {
                val isSelected = name == selected
                ListItem(
                    headlineContent = { Text(stringResource(label)) },
                    leadingContent = { RadioButton(selected = isSelected, onClick = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = Touch.min)
                            .selectable(selected = isSelected, role = Role.RadioButton) {
                                onPick(name)
                                onDismiss()
                            },
                )
            }
        }
    }
}

/** Android lets Echo run with the screen off (the battery optimisation exemption). */
private fun exempt(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun version(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "?"

private fun open(
    context: Context,
    intent: Intent,
) {
    runCatching { context.startActivity(intent) }
}
