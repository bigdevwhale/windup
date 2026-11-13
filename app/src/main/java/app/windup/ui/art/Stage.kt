package app.windup.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput

/** Global switches for the art. */
object Art {
    /** Screenshots and renders want a still frame: no loop, a fixed moment. */
    @Volatile var still = false
}

/**
 * A scene, alive: [paint] draws a frame in scene units ([w]×[h], stretched across the full width), given the
 * seconds since the last frame and the time. Taps come back in scene units.
 */
@Composable
fun Stage(w: Int, h: Int, modifier: Modifier = Modifier, onTap: ((Float, Float) -> Unit)? = null, paint: (G, Double, Long) -> Unit) {
    var frame by remember { mutableLongStateOf(0L) }
    val clock = remember { LongArray(1) }
    val draw by rememberUpdatedState(paint)
    val tap by rememberUpdatedState(onTap)

    if (!Art.still) LaunchedEffect(Unit) { while (true) withFrameMillis { frame = it } }

    Canvas(
        modifier.fillMaxWidth().aspectRatio(w.toFloat() / h).clipToBounds().pointerInput(Unit) {
            detectTapGestures { o -> tap?.invoke(o.x / size.width * w, o.y / size.height * h) }
        },
    ) {
        val g = G(this, size.width / w)
        if (Art.still) {
            // Each frame repaints the whole scene, so the warm-up frames are simply painted over.
            repeat(30) { draw(g, .1, 3_000L + it * 100) }
            draw(g, .1, 6_000)
            return@Canvas
        }
        val ms = frame
        val dt = if (clock[0] == 0L) 0.0 else ((ms - clock[0]) / 1000.0).coerceIn(0.0, .1)
        clock[0] = ms
        draw(g, dt, ms)
    }
}
