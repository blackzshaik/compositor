package io.compositor.viewer.ui.inspector

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.compositor.viewer.models.ElementBounds

import androidx.compose.foundation.layout.BoxWithConstraints

/**
 * Interactive canvas overlay drawing element bounding boxes and handling element inspection.
 */
@Composable
fun ElementInspectorOverlay(
    rootBounds: ElementBounds?,
    isInspectorMode: Boolean,
    hoveredBounds: ElementBounds?,
    selectedBounds: ElementBounds?,
    onHoverElement: (ElementBounds?) -> Unit,
    onSelectElement: (ElementBounds?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isInspectorMode) return

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val scaleX = if (rootBounds != null && rootBounds.width > 0) containerW / rootBounds.width else 1f
        val scaleY = if (rootBounds != null && rootBounds.height > 0) containerH / rootBounds.height else 1f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(rootBounds, scaleX, scaleY) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Move) {
                                val pos = event.changes.firstOrNull()?.position
                                if (pos != null && rootBounds != null) {
                                    val match = findDeepestMatch(rootBounds, pos.x / scaleX, pos.y / scaleY)
                                    onHoverElement(match)
                                }
                            }
                        }
                    }
                }
                .pointerInput(rootBounds, scaleX, scaleY) {
                    detectTapGestures { offset ->
                        if (rootBounds != null) {
                            val match = findDeepestMatch(rootBounds, offset.x / scaleX, offset.y / scaleY)
                            onSelectElement(match)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                hoveredBounds?.let { b ->
                    drawRect(
                        color = Color(0x334285F4),
                        topLeft = Offset(b.left.toFloat() * scaleX, b.top.toFloat() * scaleY),
                        size = Size(b.width.toFloat() * scaleX, b.height.toFloat() * scaleY)
                    )
                    drawRect(
                        color = Color(0xFF4285F4),
                        topLeft = Offset(b.left.toFloat() * scaleX, b.top.toFloat() * scaleY),
                        size = Size(b.width.toFloat() * scaleX, b.height.toFloat() * scaleY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                selectedBounds?.let { b ->
                    drawRect(
                        color = Color(0x4434A853),
                        topLeft = Offset(b.left.toFloat() * scaleX, b.top.toFloat() * scaleY),
                        size = Size(b.width.toFloat() * scaleX, b.height.toFloat() * scaleY)
                    )
                    drawRect(
                        color = Color(0xFF34A853),
                        topLeft = Offset(b.left.toFloat() * scaleX, b.top.toFloat() * scaleY),
                        size = Size(b.width.toFloat() * scaleX, b.height.toFloat() * scaleY),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}

/**
 * Inspector side sheet displaying detailed properties of the selected Composable.
 */
@Composable
fun ElementInspectorSheet(
    element: ElementBounds?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (element == null) return

    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Element Inspector",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "X",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onClose)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(14.dp))

            InspectorPropertyRow(label = "Component", value = element.className.substringAfterLast('.'))
            InspectorPropertyRow(label = "Full Class", value = element.className, isMonospace = true)
            InspectorPropertyRow(label = "Width", value = "${element.width} px")
            InspectorPropertyRow(label = "Height", value = "${element.height} px")
            InspectorPropertyRow(label = "Position", value = "(${element.left}, ${element.top})")
            InspectorPropertyRow(label = "Children", value = "${element.children.size} nodes")

            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(10.dp)
            ) {
                Column {
                    Text(
                        text = "Semantics & Accessibility",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Role: Composable Node\nInteractive: true",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectorPropertyRow(label: String, value: String, isMonospace: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun findDeepestMatch(bounds: ElementBounds, x: Float, y: Float): ElementBounds? {
    if (x < bounds.left || x > bounds.left + bounds.width ||
        y < bounds.top || y > bounds.top + bounds.height
    ) {
        return null
    }

    for (child in bounds.children) {
        val childMatch = findDeepestMatch(child, x, y)
        if (childMatch != null) return childMatch
    }

    return bounds
}
