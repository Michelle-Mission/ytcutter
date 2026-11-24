package app.ytcutter.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.ytcutter.core.common.DeviceProfile
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val animate = DeviceProfile.of(LocalContext.current).animateBackdrop
    val angle = auroraAngle(animate)
    Canvas(modifier) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(
                    size.width * (0.25f + 0.12f * cos(angle)),
                    size.height * (0.15f + 0.08f * sin(angle))
                ),
                radius = size.minDimension * 0.85f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(
                    size.width * (0.80f + 0.10f * sin(angle * 0.7f)),
                    size.height * (0.45f + 0.10f * cos(angle * 0.7f))
                ),
                radius = size.minDimension * 0.75f
            )
        )
    }
}

@Composable
private fun auroraAngle(enabled: Boolean): Float {
    if (!enabled) return 0.6f
    val transition = rememberInfiniteTransition(label = "aurora")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "angle"
    )
    return angle
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.16f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    )
}