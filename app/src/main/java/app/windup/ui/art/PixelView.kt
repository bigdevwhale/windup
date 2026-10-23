package app.windup.ui.art

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Global switches for the art. */
object Art {
    /** Screenshots and renders want a still frame: no loop, a fixed moment. */
    @Volatile var still = false
}

/**
 * A pixel scene, alive: [paint] draws a frame into the buffer, given the seconds since the last one and the
 * time; the buffer is shown with hard pixels across the full width. Taps come back in art pixels.
 */
@Composable
fun PixelView(w: Int, h: Int, modifier: Modifier = Modifier, onTap: ((Float, Float) -> Unit)? = null, paint: (Buf, Double, Long) -> Unit) {
    val buf = remember(w, h) { Buf(w, h) }
    val bitmap = remember(w, h) { Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var redraw by remember { mutableIntStateOf(0) }
    val draw by rememberUpdatedState(paint)
    val tap by rememberUpdatedState(onTap)

    fun frame(dt: Double, ms: Long) {
        draw(buf, dt, ms)
        bitmap.setPixels(buf.d, 0, w, 0, 0, w, h)
        redraw++
    }

    if (Art.still) {
        remember(w, h) { repeat(30) { draw(buf, .1, 3_000L + it * 100) }; frame(.1, 6_000); 0 }
    } else {
        LaunchedEffect(w, h) {
            var last = 0L
            while (true) {
                withFrameNanos { ns ->
                    val ms = ns / 1_000_000
                    // About 30 frames a second is plenty for pixel art and kind to the battery.
                    if (last == 0L || ms - last >= 30) {
                        val dt = if (last == 0L) 0.0 else ((ms - last) / 1000.0).coerceAtMost(.1)
                        last = ms
                        frame(dt, ms)
                    }
                }
            }
        }
    }

    Canvas(
        modifier.fillMaxWidth().aspectRatio(w.toFloat() / h).pointerInput(Unit) {
            detectTapGestures { o -> tap?.invoke(o.x / size.width * w, o.y / size.height * h) }
        },
    ) {
        @Suppress("UNUSED_EXPRESSION") redraw
        drawImage(image, IntOffset.Zero, IntSize(w, h), IntOffset.Zero, IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
    }
}
