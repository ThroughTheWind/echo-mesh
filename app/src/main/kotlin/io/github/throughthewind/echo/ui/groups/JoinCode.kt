package io.github.throughthewind.echo.ui.groups

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.throughthewind.echo.R
import io.github.throughthewind.echo.core.designsystem.theme.LocalSpacing
import io.github.throughthewind.echo.core.mesh.group.GroupChannel
import io.github.throughthewind.echo.core.mesh.group.GroupInfo
import io.github.throughthewind.echo.core.mesh.group.JoinOffer
import io.github.throughthewind.echo.core.mesh.wire.Bytes
import io.github.throughthewind.echo.ui.common.PrimaryButton
import io.github.throughthewind.echo.ui.common.SecondaryButton
import io.github.throughthewind.echo.ui.rememberNow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock
import kotlin.time.Duration

/**
 * The show step of patterns/QrJoinFlow: a warning first, then a one-time code black on white at full
 * brightness, screenshots blocked. The code lasts [GroupChannel.INVITATION_LIFETIME] and is replaced
 * when it expires or is used; the phone says who joined, with the threat model's guidance.
 */
@Composable
internal fun ShowCode(
    group: GroupInfo,
    channel: GroupChannel,
    onBack: () -> Unit,
) {
    SecureAndBright()
    var offer by remember { mutableStateOf<JoinOffer?>(null) }
    var issuedAt by remember { mutableStateOf(Clock.System.now()) }
    var generation by remember { mutableIntStateOf(0) }
    var joined by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(generation) {
        offer = channel.invite(group.id)
        issuedAt = Clock.System.now()
    }
    LaunchedEffect(group.id) {
        channel.joins.collect {
            if (it.groupId == group.id && it.throughThisPhone) {
                joined = it.nickname
                generation++
            }
        }
    }
    DisposableEffect(group.id) {
        onDispose { CoroutineScope(Dispatchers.Default).launch { channel.cancelInvite(group.id) } }
    }
    val now = rememberNow()
    val left = (GroupChannel.INVITATION_LIFETIME - (now - issuedAt)).coerceAtLeast(Duration.ZERO)
    val expired = left == Duration.ZERO
    LaunchedEffect(expired) { if (expired) generation++ }

    GroupPage(title = stringResource(R.string.code_title), onBack = onBack) {
        Callout(stringResource(R.string.code_warning, group.name), warning = true)
        val code = offer
        val bitmap = remember(code) { code?.let { qrBitmap(it.encode()) } }
        // Black on white in every theme, Discreet included: scanners need it.
        Surface(color = Color.White, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.code_description, group.name),
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.padding(LocalSpacing.current.lg),
                )
            }
        }
        val seconds = left.inWholeSeconds
        Text(
            stringResource(R.string.code_countdown, String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)),
            style = MaterialTheme.typography.bodyLarge,
        )
        joined?.let {
            Callout(stringResource(R.string.code_joined, it, group.name) + "\n" + stringResource(R.string.code_joined_check))
        }
    }
}

/** Where a newcomer is in the join. */
private sealed interface ScanStep {
    data object Ready : ScanStep

    data object Camera : ScanStep

    data object Joining : ScanStep

    data class Joined(
        val group: GroupInfo,
    ) : ScanStep

    data object Failed : ScanStep
}

/**
 * The scan step of patterns/QrJoinFlow. The nickname is picked first, because the join carries it
 * to the group (ADR-0009); then the camera, asked for in context; then the join over one hop.
 */
@Composable
internal fun ScanToJoin(
    channel: GroupChannel,
    full: Boolean,
    onBack: () -> Unit,
    onJoined: (Bytes) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by remember { mutableStateOf<ScanStep>(ScanStep.Ready) }
    var nickname by remember { mutableStateOf(Nicknames.random()) }
    var notEcho by remember { mutableStateOf(false) }
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            denied = !ok
        }
    var joining by remember { mutableStateOf<Job?>(null) }

    GroupPage(title = stringResource(R.string.scan_title), onBack = onBack) {
        when (val current = step) {
            ScanStep.Ready -> {
                if (full) {
                    Text(stringResource(R.string.join_full), style = MaterialTheme.typography.bodyLarge)
                } else {
                    NicknameField(nickname) { nickname = it }
                    PrimaryButton(stringResource(R.string.scan_start), { step = ScanStep.Camera }, enabled = Nicknames.fits(nickname))
                }
            }

            ScanStep.Camera -> {
                when {
                    granted -> {
                        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                            QrScanner(
                                onText = { text ->
                                    val offer = JoinOffer.decode(text)
                                    if (offer == null) {
                                        notEcho = true
                                        false
                                    } else {
                                        step = ScanStep.Joining
                                        joining =
                                            scope.launch {
                                                val group = channel.join(offer, nickname.trim())
                                                step = if (group != null) ScanStep.Joined(group) else ScanStep.Failed
                                            }
                                        true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                            )
                        }
                        Text(
                            stringResource(if (notEcho) R.string.scan_not_echo else R.string.scan_hint),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    denied -> {
                        Text(stringResource(R.string.scan_camera_denied), style = MaterialTheme.typography.bodyLarge)
                        PrimaryButton(stringResource(R.string.scan_open_settings), {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                            )
                        })
                    }

                    else -> {
                        Text(stringResource(R.string.scan_camera_body), style = MaterialTheme.typography.bodyLarge)
                        PrimaryButton(stringResource(R.string.scan_camera_allow), { ask.launch(Manifest.permission.CAMERA) })
                    }
                }
            }

            ScanStep.Joining -> {
                Text(stringResource(R.string.joining), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(stringResource(R.string.joining_body), style = MaterialTheme.typography.bodyLarge)
                SecondaryButton(stringResource(R.string.cancel), {
                    joining?.cancel()
                    step = ScanStep.Ready
                })
            }

            is ScanStep.Joined -> {
                Text(stringResource(R.string.joined_title, current.group.name), style = MaterialTheme.typography.titleLarge)
                Callout(stringResource(R.string.joined_body, current.group.name, ends(current.group.expiresAt)))
                PrimaryButton(stringResource(R.string.done), { onJoined(current.group.id) })
            }

            ScanStep.Failed -> {
                Callout(stringResource(R.string.join_failed), warning = true)
                PrimaryButton(stringResource(R.string.join_again), {
                    notEcho = false
                    step = ScanStep.Camera
                })
            }
        }
    }
}

/**
 * Camera frames to text, with CameraX and ZXing only: no Play Services, no network. [onText] runs
 * on the main thread for each code read until it returns true; the camera stops when this leaves
 * the screen. Frames are never stored.
 */
@Composable
private fun QrScanner(
    onText: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val done = remember { AtomicBoolean(false) }
    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(context)
        onDispose {
            future.addListener({ future.get().unbindAll() }, ContextCompat.getMainExecutor(context))
            executor.shutdown()
        }
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            val view = PreviewView(viewContext)
            val future = ProcessCameraProvider.getInstance(viewContext)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis =
                    ImageAnalysis
                        .Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                val main = ContextCompat.getMainExecutor(viewContext)
                analysis.setAnalyzer(executor) { image ->
                    val text = if (done.get()) null else readQr(image)
                    if (text != null) main.execute { if (!done.get() && onText(text)) done.set(true) }
                }
                provider.unbindAll()
                provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(viewContext))
            view
        },
    )
}

/** The text of a QR code in [image]'s luminance plane, or null. Always closes the frame. */
private fun readQr(image: ImageProxy): String? =
    image.use {
        val plane = it.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining()).also { data -> buffer.get(data) }
        val source = PlanarYUVLuminanceSource(bytes, plane.rowStride, it.height, 0, 0, it.width, it.height, false)
        try {
            QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: ReaderException) {
            null
        }
    }

/** [text] as a QR code, black on white, one pixel per module plus a quiet zone; scaled crisp by the UI. */
private fun qrBitmap(text: String): ImageBitmap {
    val matrix =
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 2),
        )
    val width = matrix.width
    val height = matrix.height
    val pixels = IntArray(width * height) { if (matrix[it % width, it / width]) BLACK else WHITE }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

/** Screenshots and the recents thumbnail blocked, and the screen at full brightness, while shown. */
@Composable
private fun SecureAndBright() {
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        val window = activity?.window
        val before = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window?.let {
            it.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            it.attributes = it.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        }
        onDispose {
            window?.let {
                it.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                it.attributes = it.attributes.apply { screenBrightness = before }
            }
        }
    }
}
