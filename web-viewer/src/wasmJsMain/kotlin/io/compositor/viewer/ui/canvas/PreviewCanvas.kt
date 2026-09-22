package io.compositor.viewer.ui.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.compositor.viewer.models.DeviceProfile
import io.compositor.viewer.models.PreviewItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import org.jetbrains.skia.Image

/**
 * Interactive preview canvas with hardware bezel viewport and zoom/pan controls.
 */
@Composable
fun PreviewCanvas(
    previewItem: PreviewItem?,
    imageUrl: String?,
    deviceProfile: DeviceProfile,
    isLandscape: Boolean,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    isRendering: Boolean,
    onPan: (Float, Float) -> Unit,
    isInspectorMode: Boolean = false,
    modifier: Modifier = Modifier,
    overlayContent: @Composable () -> Unit = {}
) {
    val dragModifier = if (!isInspectorMode) {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                onPan(dragAmount.x, dragAmount.y)
            }
        }
    } else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        ) {
            DeviceFrame(
                profile = deviceProfile,
                isLandscape = isLandscape
            ) {
                // Viewport content
                Box(modifier = Modifier.fillMaxSize()) {
                    if (previewItem == null) {
                        CanvasEmptyPlaceholder(message = "Select a preview from the sidebar.")
                    } else {
                        WasmPreviewImage(
                            imageUrl = imageUrl,
                            contentDescription = previewItem.definition.functionName,
                            isRendering = isRendering
                        )
                    }

                    // Interactive overlay (Element inspector or error cards)
                    overlayContent()
                }
            }
        }
    }
}

@Composable
fun WasmPreviewImage(
    imageUrl: String?,
    contentDescription: String,
    isRendering: Boolean,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(imageUrl) { mutableStateOf<ImageBitmap?>(null) }
    var hasError by remember(imageUrl) { mutableStateOf(false) }
    var isFetching by remember(imageUrl) { mutableStateOf(false) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            bitmap = null
            return@LaunchedEffect
        }
        isFetching = true
        hasError = false
        try {
            val client = HttpClient()
            val bytes = client.get(imageUrl).body<ByteArray>()
            client.close()
            bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) {
            hasError = true
        } finally {
            isFetching = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E22)),
        contentAlignment = Alignment.Center
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (bitmap == null && !isFetching && !hasError) {
            CanvasEmptyPlaceholder(message = "Ready to render.")
        }

        if (hasError && bitmap == null) {
            Text(
                text = "Preview image not available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (isRendering || isFetching) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF282A36).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, Color(0xFF44475A)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Compiling & Rendering...",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Applying live Compose changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAAA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CanvasEmptyPlaceholder(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}
