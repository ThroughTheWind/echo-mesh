package io.github.throughthewind.echo.ui.protects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.LocalStreetColors
import io.github.throughthewind.echo.ui.common.BackBar
import io.github.throughthewind.echo.ui.common.IconLine
import io.github.throughthewind.echo.core.designsystem.R as DS

// protects_5, the panic wipe, is listed once the wipe ships (WI-024): the page never claims a
// protection the installed version does not have.
private val protects =
    listOf(R.string.protects_1, R.string.protects_2, R.string.protects_3, R.string.protects_4, R.string.protects_6)
private val doesNot =
    listOf(
        R.string.not_protects_1,
        R.string.not_protects_2,
        R.string.not_protects_3,
        R.string.not_protects_4,
        R.string.not_protects_5,
        R.string.not_protects_6,
    )

/**
 * The complete list of what Echo protects and what it doesn't, word for word from threat model §7
 * (screens/WhatEchoProtects, FR-1.3). Green checks and amber warnings, never a red cross.
 */
@Composable
fun WhatEchoProtects(onBack: () -> Unit) {
    Scaffold(topBar = { BackBar(stringResource(R.string.protects_title), onBack) }) { padding ->
        ProtectsLists(
            protects = protects,
            doesNot = doesNot,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(LocalSpacing.current.xxl),
        )
    }
}

/** The two lists, shared with onboarding's shorter version. */
@Composable
fun ProtectsLists(
    protects: List<Int>,
    doesNot: List<Int>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val street = LocalStreetColors.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        Text(
            stringResource(R.string.protects_heading),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        protects.forEach { IconLine(DS.drawable.ic_check_circle, street.safe, stringResource(it)) }
        Text(
            stringResource(R.string.not_protects_heading),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = spacing.lg).semantics { heading() },
        )
        doesNot.forEach { IconLine(DS.drawable.ic_warning, street.blocked, stringResource(it)) }
    }
}
