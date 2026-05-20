package zeki.productions.shorts.ui.components

import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Lean Transformable Modifier.
 * lockRotation is set to true to ensure vertical/horizontal drag stability.
 */
@Composable
fun Modifier.pinchToZoom(scale: Float, onScaleChange: (Float) -> Unit): Modifier {
    val state = rememberTransformableState { zoomChange, _, _ ->
        onScaleChange((scale * zoomChange).coerceIn(1f, 3f))
    }
    return this.then(
        Modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
            )
            .transformable(state = state, lockRotationOnZoomPan = true)
    )
}