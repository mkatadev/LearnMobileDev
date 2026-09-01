package pl.prodevcode.learnmobiledev.presentation.infographics

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.Image

/**
 * A picture that zooms and pans like one in a photo gallery.
 *
 * Three gestures, and the reasoning behind each:
 *
 * - **Pinch** scales around the point between the fingers, not the centre of the screen.
 *   Anchoring to the centre is the classic shortcut here, and it makes the detail under the
 *   fingers slide away exactly when the user is trying to look at it.
 * - **Drag** pans, but only within the picture. Unbounded panning lets the image be flung
 *   off-screen with no way back, which reads as the app losing it.
 * - **Double tap** toggles between fitting the screen and [DOUBLE_TAP_SCALE], because
 *   pinching to a useful zoom is fiddly one-handed and this is the gesture people already
 *   try first.
 *
 * The zoom state is local `remember` on purpose, which is the opposite of the rule the rest
 * of this app follows. It is scoped to the viewer being open and losing it costs nothing a
 * user would call a bug: the picture is still there, still open, merely un-zoomed. What
 * *would* be a bug — the viewer closing itself on rotation — is why `openedId` lives in the
 * store instead.
 */
@Composable
fun ZoomableImage(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        val viewport = IntSize(constraints.maxWidth, constraints.maxHeight)

        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        // The drawn size after `ContentScale.Fit`, which is what the pan limits must be
        // measured against — not the bitmap's pixel size, which is far larger here.
        val fitted = remember(bitmap, viewport) { bitmap.fitInside(viewport) }

        fun clamp(candidate: Offset, atScale: Float): Offset {
            // At or below fit-scale there is nothing to pan to: the picture is fully
            // visible, so it stays centred instead of drifting under the finger.
            val maxX = max(0f, (fitted.width * atScale - viewport.width) / 2f)
            val maxY = max(0f, (fitted.height * atScale - viewport.height) / 2f)
            return Offset(
                x = candidate.x.coerceIn(-maxX, maxX),
                y = candidate.y.coerceIn(-maxY, maxY),
            )
        }

        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bitmap, viewport) {
                    detectTapGestures(
                        onDoubleTap = { tap ->
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = DOUBLE_TAP_SCALE
                                // Zoom towards what was tapped, so the double tap lands on
                                // the detail the user pointed at rather than the middle.
                                val focus = tap - viewport.center()
                                offset = clamp(-focus * (DOUBLE_TAP_SCALE - 1f), DOUBLE_TAP_SCALE)
                            }
                        },
                    )
                }
                .pointerInput(bitmap, viewport) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val next = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)

                        // Keep the point under the fingers still: as the scale changes, the
                        // offset has to move with the centroid's distance from the centre.
                        val focus = centroid - viewport.center()
                        val adjusted = (offset + focus) * (next / scale) - focus

                        scale = next
                        offset = clamp(adjusted + pan, next)
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

private fun IntSize.center(): Offset = Offset(width / 2f, height / 2f)

/**
 * The size the bitmap is actually drawn at under [ContentScale.Fit].
 *
 * Panning limits derived from the bitmap's own dimensions would be wrong by the fit ratio,
 * which for a tall infographic on a phone is most of the picture.
 */
private fun ImageBitmap.fitInside(viewport: IntSize): Size2D {
    if (width == 0 || height == 0 || viewport.width == 0 || viewport.height == 0) {
        return Size2D(0f, 0f)
    }
    val ratio = min(
        viewport.width.toFloat() / width,
        viewport.height.toFloat() / height,
    )
    return Size2D(width * ratio, height * ratio)
}

private data class Size2D(val width: Float, val height: Float)

/** Below 1x the picture would be smaller than the screen it already fits. */
private const val MIN_SCALE = 1f

/** Beyond this the source pixels are guesswork, and the gesture stops feeling anchored. */
private const val MAX_SCALE = 6f

private const val DOUBLE_TAP_SCALE = 2.5f
