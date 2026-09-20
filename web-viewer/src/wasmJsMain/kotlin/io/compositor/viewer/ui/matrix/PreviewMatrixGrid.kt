package io.compositor.viewer.ui.matrix

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.compositor.viewer.models.MatrixPreset
import io.compositor.viewer.models.PreviewItem
import io.compositor.viewer.ui.canvas.WasmPreviewImage

/**
 * Data specification for a matrix variant cell.
 */
data class MatrixCellVariant(
    val title: String,
    val subtitle: String,
    val previewId: String,
    val imageUrl: String?
)

/**
 * Multi-preview comparison matrix grid comparing variants side-by-side.
 */
@Composable
fun PreviewMatrixGrid(
    selectedPreview: PreviewItem?,
    allPreviews: List<PreviewItem>,
    preset: MatrixPreset,
    zoomScale: Float,
    panOffsetX: Float,
    panOffsetY: Float,
    isRendering: Boolean,
    onPan: (Float, Float) -> Unit,
    onSelectPreview: (String) -> Unit,
    onPresetChange: (MatrixPreset) -> Unit,
    getImageUrl: (String) -> String,
    modifier: Modifier = Modifier
) {
    val variants = computeMatrixVariants(selectedPreview, allPreviews, preset, getImageUrl)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F11))
    ) {
        // Preset selector bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Preset:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MatrixPreset.entries.forEach { p ->
                val isSelected = p == preset
                val bg = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val fg = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .clickable { onPresetChange(p) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = p.label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg)
                }
            }
        }

        // Zoomable and pannable grid canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onPan(dragAmount.x, dragAmount.y)
                    }
                }
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
        ) {
            if (variants.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select a preview to view the matrix.",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 320.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(variants, key = { "${it.previewId}_${it.title}" }) { variant ->
                        MatrixCard(
                            variant = variant,
                            isRendering = isRendering,
                            onClick = { onSelectPreview(variant.previewId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MatrixCard(
    variant: MatrixCellVariant,
    isRendering: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = variant.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = variant.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                WasmPreviewImage(
                    imageUrl = variant.imageUrl,
                    contentDescription = variant.title,
                    isRendering = isRendering
                )
            }
        }
    }
}

private fun computeMatrixVariants(
    selected: PreviewItem?,
    all: List<PreviewItem>,
    preset: MatrixPreset,
    getImageUrl: (String) -> String
): List<MatrixCellVariant> {
    if (selected == null) return emptyList()

    return when (preset) {
        MatrixPreset.THEME -> {
            listOf(
                MatrixCellVariant(
                    title = selected.definition.functionName,
                    subtitle = "Light Mode",
                    previewId = selected.id,
                    imageUrl = getImageUrl(selected.id)
                ),
                MatrixCellVariant(
                    title = selected.definition.functionName,
                    subtitle = "Dark Mode",
                    previewId = selected.id,
                    imageUrl = "${getImageUrl(selected.id)}&theme=dark"
                )
            )
        }
        MatrixPreset.FONT_SCALE -> {
            listOf(0.85f, 1.0f, 1.15f, 1.5f).map { scale ->
                MatrixCellVariant(
                    title = selected.definition.functionName,
                    subtitle = "${scale}x Typography",
                    previewId = selected.id,
                    imageUrl = "${getImageUrl(selected.id)}&fontScale=$scale"
                )
            }
        }
        MatrixPreset.GROUP -> {
            val groupName = selected.definition.parameters.group
            val groupPreviews = if (!groupName.isNullOrBlank()) {
                all.filter { it.definition.parameters.group == groupName }
            } else {
                listOf(selected)
            }
            groupPreviews.map { item ->
                MatrixCellVariant(
                    title = item.definition.functionName,
                    subtitle = item.definition.parameters.group ?: "Default",
                    previewId = item.id,
                    imageUrl = getImageUrl(item.id)
                )
            }
        }
    }
}
