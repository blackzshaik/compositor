package io.compositor.viewer.ui.topbar

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.compositor.viewer.models.DeviceProfile
import io.compositor.viewer.models.ViewMode

/**
 * Top app bar with theme, font scale, device, and live connection status controls.
 */
@Composable
fun InspectorTopBar(
    isConnected: Boolean,
    viewMode: ViewMode,
    deviceProfile: DeviceProfile,
    zoomScale: Float,
    isDarkTheme: Boolean,
    fontScale: Float,
    isLandscape: Boolean,
    isInspectorMode: Boolean,
    isSidebarExpanded: Boolean,
    onToggleSidebar: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onDeviceProfileChange: (DeviceProfile) -> Unit,
    onZoomChange: (Float) -> Unit,
    onResetZoom: () -> Unit,
    onToggleTheme: () -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onToggleOrientation: () -> Unit,
    onToggleInspector: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Title, sidebar toggle, connection status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TopBarPillButton(
                    text = if (isSidebarExpanded) "«" else "»",
                    onClick = onToggleSidebar
                )

                Text(
                    text = "Compositor",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ConnectionStatusBadge(isConnected = isConnected)
            }

            // Center: Device profile, View mode, Zoom controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Mode Toggle
                ViewModeSegmentedToggle(
                    viewMode = viewMode,
                    onViewModeChange = onViewModeChange
                )

                // Device selector
                DeviceProfileSelector(
                    currentProfile = deviceProfile,
                    onSelectProfile = onDeviceProfileChange
                )

                // Zoom controls
                ZoomControlButtons(
                    currentZoom = zoomScale,
                    onZoomChange = onZoomChange,
                    onResetZoom = onResetZoom
                )
            }

            // Right: Inspector controls (Theme, Font scale, Orientation, Inspector toggle, Refresh)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Theme toggle
                TopBarPillButton(
                    text = if (isDarkTheme) "🌙 Dark" else "☀️ Light",
                    onClick = onToggleTheme
                )

                // Font scale slider control
                FontScaleControl(
                    fontScale = fontScale,
                    onFontScaleChange = onFontScaleChange
                )

                // Orientation toggle
                TopBarPillButton(
                    text = if (isLandscape) "🔄 Land" else "📱 Port",
                    onClick = onToggleOrientation
                )

                // Inspector Mode toggle
                TopBarPillButton(
                    text = if (isInspectorMode) "🔍 Active" else "🔍 Inspect",
                    isSelected = isInspectorMode,
                    onClick = onToggleInspector
                )

                // Refresh button
                ElevatedButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Render", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBadge(isConnected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateColor(
        initialValue = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
        targetValue = if (isConnected) Color(0xFF81C784) else Color(0xFFE57373),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(pulseAlpha)
        )
        Text(
            text = if (isConnected) "Live" else "Disconnected",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ViewModeSegmentedToggle(
    viewMode: ViewMode,
    onViewModeChange: (ViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(2.dp)
    ) {
        TopBarSegment(
            text = "Device",
            isSelected = viewMode == ViewMode.SINGLE,
            onClick = { onViewModeChange(ViewMode.SINGLE) }
        )
        TopBarSegment(
            text = "Matrix",
            isSelected = viewMode == ViewMode.MATRIX,
            onClick = { onViewModeChange(ViewMode.MATRIX) }
        )
    }
}

@Composable
private fun DeviceProfileSelector(
    currentProfile: DeviceProfile,
    onSelectProfile: (DeviceProfile) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        TopBarPillButton(
            text = currentProfile.displayName,
            onClick = { isExpanded = !isExpanded }
        )

        if (isExpanded) {
            Surface(
                modifier = Modifier
                    .padding(top = 40.dp)
                    .width(160.dp),
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 8.dp
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    DeviceProfile.entries.forEach { profile ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    onSelectProfile(profile)
                                    isExpanded = false
                                }
                                .padding(8.dp)
                        ) {
                            Text(
                                text = profile.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (profile == currentProfile) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomControlButtons(
    currentZoom: Float,
    onZoomChange: (Float) -> Unit,
    onResetZoom: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TopBarPillButton(text = "-", onClick = { onZoomChange(currentZoom - 0.15f) })
        TopBarPillButton(
            text = "${(currentZoom * 100).toInt()}%",
            onClick = onResetZoom
        )
        TopBarPillButton(text = "+", onClick = { onZoomChange(currentZoom + 0.15f) })
    }
}

@Composable
private fun FontScaleControl(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(130.dp)
    ) {
        Text(
            text = "${(fontScale * 10).toInt() / 10.0}x",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(28.dp)
        )
        Slider(
            value = fontScale,
            onValueChange = onFontScaleChange,
            valueRange = 0.85f..1.5f,
            steps = 3,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TopBarSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

@Composable
private fun TopBarPillButton(
    text: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val fg = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}
