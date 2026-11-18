package io.github.throughthewind.echo.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.designsystem.R as DS

/** The filled primary button of a step or sheet, at the comfortable touch size. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = Touch.comfort)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = Touch.comfort)) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A line with a leading glyph: the protects / does-not-protect lists and the mesh facts. */
@Composable
fun IconLine(
    @DrawableRes icon: Int,
    tint: Color,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LocalSpacing.current.md), verticalAlignment = Alignment.Top) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(24.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

/** A top app bar with a back arrow (components/TopAppBar). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(
    title: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.size(Touch.min)) {
                Icon(painterResource(DS.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
            }
        },
    )
}
