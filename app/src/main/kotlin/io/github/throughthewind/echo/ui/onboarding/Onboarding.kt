package io.github.throughthewind.echo.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalGlanceType
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.ui.MapsState
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.maps.GetMapChoices
import io.github.throughthewind.echo.ui.protects.ProtectsLists
import io.github.throughthewind.echo.ui.protects.WhatEchoProtects
import kotlinx.coroutines.launch
import io.github.throughthewind.echo.core.designsystem.R as DS

private const val STEPS = 4

/**
 * First launch (patterns/OnboardingFlow, FR-1.1, AC-1): welcome; what Echo protects and what it
 * doesn't; get a map; before you go. It asks for no permission: those come in context
 * (FR-1.2). The first two steps cannot be skipped.
 */
@Composable
fun Onboarding(
    maps: MapsState,
    onDone: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var readingEverything by rememberSaveable { mutableStateOf(false) }
    if (readingEverything) {
        BackHandler { readingEverything = false }
        WhatEchoProtects(onBack = { readingEverything = false })
        return
    }
    BackHandler(enabled = step > 0) { step-- }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(LocalSpacing.current.xxl),
            verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.lg),
        ) {
            Text(
                stringResource(R.string.onboarding_step, step + 1, STEPS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (step) {
                0 -> Welcome(onNext = { step = 1 })
                1 -> Protects(onNext = { step = 2 }, onReadEverything = { readingEverything = true })
                2 -> GetAMap(maps, onNext = { step = 3 }, onResult = { scope.launch { snackbar.showSnackbar(it) } })
                else -> ThingsToKnow(onDone)
            }
            SnackbarHost(snackbar)
        }
    }
}

@Composable
private fun Title(text: String) {
    Text(text, style = LocalGlanceType.current.title, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Welcome(onNext: () -> Unit) {
    Icon(
        painterResource(DS.drawable.ic_map),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(64.dp),
    )
    Title(stringResource(R.string.onboarding_welcome_title))
    Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.size(LocalSpacing.current.lg))
    PrimaryButton(stringResource(R.string.onboarding_get_started), onNext)
}

@Composable
private fun Protects(
    onNext: () -> Unit,
    onReadEverything: () -> Unit,
) {
    Title(stringResource(R.string.onboarding_protects_title))
    ProtectsLists(
        protects =
            listOf(
                R.string.onboarding_protects_1,
                R.string.onboarding_protects_2,
                R.string.onboarding_protects_3,
                R.string.onboarding_protects_4,
            ),
        doesNot = listOf(R.string.onboarding_not_1, R.string.onboarding_not_2, R.string.onboarding_not_3, R.string.onboarding_not_4),
    )
    PrimaryButton(stringResource(R.string.onboarding_understand), onNext)
    SecondaryButton(stringResource(R.string.onboarding_read_everything), onReadEverything)
}

@Composable
private fun GetAMap(
    maps: MapsState,
    onNext: () -> Unit,
    onResult: (String) -> Unit,
) {
    Title(stringResource(R.string.onboarding_map_title))
    Text(stringResource(R.string.onboarding_map_body), style = MaterialTheme.typography.bodyLarge)
    GetMapChoices(maps, onResult)
    if (maps.current != null) {
        PrimaryButton(stringResource(R.string.continue_), onNext)
    } else {
        SecondaryButton(stringResource(R.string.onboarding_skip), onNext)
    }
}

@Composable
private fun ThingsToKnow(onDone: () -> Unit) {
    val muted = MaterialTheme.colorScheme.onSurface
    Title(stringResource(R.string.onboarding_know_title))
    IconLine(DS.drawable.ic_sensors_off, muted, stringResource(R.string.onboarding_know_mesh))
    IconLine(DS.drawable.ic_timer, muted, stringResource(R.string.onboarding_know_expiry))
    IconLine(DS.drawable.ic_visibility_off, muted, stringResource(R.string.onboarding_know_discreet))
    IconLine(DS.drawable.ic_delete_forever, muted, stringResource(R.string.onboarding_know_wipe))
    PrimaryButton(stringResource(R.string.onboarding_open_map), onDone)
}
