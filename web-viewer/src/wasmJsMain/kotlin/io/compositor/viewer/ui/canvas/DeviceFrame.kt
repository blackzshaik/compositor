package io.compositor.viewer.ui.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.compositor.viewer.models.DeviceProfile

/**
 * Hardware-accurate device frame container for mobile and tablet form factors.
 */
@Composable
fun DeviceFrame(
    profile: DeviceProfile,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val rawWidth = profile.screenWidthDp.dp
    val rawHeight = profile.screenHeightDp.dp

    val screenWidth = if (isLandscape) rawHeight else rawWidth
    val screenHeight = if (isLandscape) rawWidth else rawHeight
    val cornerRadius = profile.cornerRadiusDp.dp

    when (profile) {
        DeviceProfile.PIXEL_8 -> Pixel8Frame(
            width = screenWidth,
            height = screenHeight,
            cornerRadius = cornerRadius,
            isLandscape = isLandscape,
            modifier = modifier,
            content = content
        )
        DeviceProfile.GALAXY_S24_ULTRA -> GalaxyFrame(
            width = screenWidth,
            height = screenHeight,
            cornerRadius = cornerRadius,
            isLandscape = isLandscape,
            modifier = modifier,
            content = content
        )
        DeviceProfile.TABLET_10 -> TabletFrame(
            width = screenWidth,
            height = screenHeight,
            cornerRadius = cornerRadius,
            isLandscape = isLandscape,
            modifier = modifier,
            content = content
        )
        DeviceProfile.FRAMELESS -> FramelessContainer(
            width = screenWidth,
            height = screenHeight,
            modifier = modifier,
            content = content
        )
    }
}

@Composable
private fun Pixel8Frame(
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bezelPadding = 12.dp
    val outerRadius = cornerRadius + bezelPadding

    Box(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(outerRadius))
            .clip(RoundedCornerShape(outerRadius))
            .background(Color(0xFF1F1F1F))
            .border(3.dp, Color(0xFF383838), RoundedCornerShape(outerRadius))
            .padding(bezelPadding)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speaker slit
            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF0F0F0F))
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Screen Viewport
            Box(
                modifier = Modifier
                    .size(width, height)
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(Color.Black)
            ) {
                content()

                // Camera punchhole
                if (!isLandscape) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = 10.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D0D0D))
                            .border(1.dp, Color(0xFF222222), CircleShape)
                    )
                }

                // Home indicator bar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = (-8).dp)
                        .width(72.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

@Composable
private fun GalaxyFrame(
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bezel = 8.dp
    val outerRadius = cornerRadius + bezel

    Box(
        modifier = modifier
            .shadow(28.dp, RoundedCornerShape(outerRadius))
            .clip(RoundedCornerShape(outerRadius))
            .background(Color(0xFF141416))
            .border(2.dp, Color(0xFF4A4B50), RoundedCornerShape(outerRadius))
            .padding(bezel)
    ) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Black)
        ) {
            content()

            if (!isLandscape) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF080808))
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-6).dp)
                    .width(80.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun TabletFrame(
    width: Dp,
    height: Dp,
    cornerRadius: Dp,
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val bezel = 18.dp
    val outerRadius = cornerRadius + bezel

    Box(
        modifier = modifier
            .shadow(32.dp, RoundedCornerShape(outerRadius))
            .clip(RoundedCornerShape(outerRadius))
            .background(Color(0xFF222326))
            .border(3.dp, Color(0xFF33353A), RoundedCornerShape(outerRadius))
            .padding(bezel)
    ) {
        Box(
            modifier = Modifier
                .size(width, height)
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.Black)
        ) {
            content()

            val camAlignment = if (isLandscape) Alignment.TopCenter else Alignment.CenterEnd
            val camOffset = if (isLandscape) Modifier.offset(y = (-9).dp) else Modifier.offset(x = 9.dp)

            Box(
                modifier = Modifier
                    .align(camAlignment)
                    .then(camOffset)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF111111))
            )
        }
    }
}

@Composable
private fun FramelessContainer(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .size(width, height)
            .shadow(16.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
            .background(Color.Black)
    ) {
        content()
    }
}
