package io.github.throughthewind.echo.ui.maps

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.designsystem.theme.Touch
import io.github.throughthewind.echo.core.map.MapLibrary
import io.github.throughthewind.echo.core.map.MapSharing
import io.github.throughthewind.echo.ui.MapsState
import kotlinx.coroutines.launch
import io.github.throughthewind.echo.core.designsystem.R as DS

/**
 * The two ways to get a map (patterns/GetAMap): download it in the user's own browser, or open a
 * file from downloads or from another phone. Echo itself fetches nothing (ADR-0006). [onResult]
 * gets a sentence to show the user.
 */
@Composable
fun GetMapChoices(
    maps: MapsState,
    onResult: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val noBrowser = stringResource(R.string.get_map_no_browser)
    val added = stringResource(R.string.get_map_added, "%s")
    val refused = stringResource(R.string.get_map_refused, "%s")
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@rememberLauncherForActivityResult
            scope.launch {
                when (val imported = maps.import(context.contentResolver, uri)) {
                    is MapLibrary.Imported.Added -> onResult(added.format(imported.entry.name))
                    is MapLibrary.Imported.Refused -> onResult(refused.format(imported.reason))
                }
            }
        }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(LocalSpacing.current.sm)) {
        Choice(
            icon = DS.drawable.ic_download,
            title = stringResource(R.string.get_map_download),
            body = stringResource(R.string.get_map_download_body),
            onClick = {
                try {
                    context.startActivity(MapSharing.browse(MapSharing.downloadPages.first()))
                } catch (_: ActivityNotFoundException) {
                    onResult(noBrowser)
                }
            },
        )
        Choice(
            icon = DS.drawable.ic_folder_open,
            title = stringResource(R.string.get_map_open),
            body = stringResource(R.string.get_map_open_body),
            onClick = { picker.launch(MapSharing.pick()) },
        )
    }
}

@Composable
private fun Choice(
    icon: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .heightIn(min = Touch.comfort)
                .clickable(role = Role.Button, onClick = onClick),
    )
}
