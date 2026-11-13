package app.windup.ui.art

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.roundToInt

fun rgb(hex: Long): Int = (hex or 0xFF000000).toInt()

/** The ink of the old prints: a warm near-black. */
val INK = rgb(0x1e130c)
val WHITE = rgb(0xffffff)

fun col(c: Int, a: Float = 1f) = Color(c or (0xFF shl 24)).copy(alpha = a)

fun hash(i: Int, j: Int, k: Int = 0): Double {
    var h = i * 374761393 + j * 668265263 + k * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL) / 4294967296.0
}

fun frac(v: Double) = v - floor(v)

/** The painted art, decoded once from the drawables and kept for the life of the process. */
@SuppressLint("DiscouragedApi")
object Img {
    private lateinit var app: Context
    private val cache = HashMap<String, ImageBitmap>()

    fun init(context: Context) { app = context.applicationContext }

    operator fun get(name: String): ImageBitmap = synchronized(cache) {
        cache.getOrPut(name) {
            val id = app.resources.getIdentifier(name, "drawable", app.packageName)
            check(id != 0) { "No art named $name" }
            val opts = BitmapFactory.Options().apply { inScaled = false }
            BitmapFactory.decodeResource(app.resources, id, opts).also { it.prepareToDraw() }.asImageBitmap()
        }
    }
}

/** Colour filters for metal, rust and gilt, as colour matrices. */
object Tints {
    private fun lum(r: Float, g: Float, b: Float, add: Float = 0f) = floatArrayOf(
        .30f * r, .59f * r, .11f * r, 0f, add,
        .30f * g, .59f * g, .11f * g, 0f, add,
        .30f * b, .59f * b, .11f * b, 0f, add,
        0f, 0f, 0f, 1f, 0f,
    )
    private val ID = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
    private fun mix(a: FloatArray, b: FloatArray, t: Float) = FloatArray(20) { a[it] + (b[it] - a[it]) * t }

    /** Tin, brass, copper, silver, gold, as the key is drawn in each (the key art is brass). */
    private val METALS = listOf(lum(.95f, .95f, 1f, 6f), ID, lum(1.25f, .72f, .52f), lum(1.18f, 1.18f, 1.24f, 18f), lum(1.45f, 1.1f, .4f, 8f))
    private val RUST = lum(1.05f, .6f, .34f, -6f)
    private val GOLD = lum(1.5f, 1.12f, .42f, 10f)

    private val metalCache = METALS.map { ColorFilter.colorMatrix(ColorMatrix(it)) }
    private val darkMetal = METALS.map { ColorFilter.colorMatrix(ColorMatrix(it.copyOf().also { m -> for (i in listOf(0, 1, 2, 5, 6, 7, 10, 11, 12)) m[i] *= .7f })) }

    fun metal(i: Int, back: Boolean = false) = (if (back) darkMetal else metalCache)[i.coerceIn(0, METALS.size - 1)]

    private val looks = HashMap<Int, ColorFilter?>()

    /** A toy's own filter: some rust over it (0..1, in quarter steps), gilt if it's golden. */
    fun look(rust: Float, gold: Boolean): ColorFilter? = synchronized(looks) {
        val rq = (rust * 4).roundToInt()
        looks.getOrPut(rq * 2 + if (gold) 1 else 0) {
            if (rq == 0 && !gold) return@getOrPut null
            var m = if (gold) mix(ID, GOLD, .8f) else ID
            if (rq > 0) m = mix(m, RUST, rq / 4f * .7f)
            ColorFilter.colorMatrix(ColorMatrix(m))
        }
    }

    fun sil(c: Int) = ColorFilter.tint(col(c), BlendMode.SrcIn)
}

/**
 * Drawing in scene units: [k] pixels to the unit. Images are drawn smooth; every shape is anti-aliased.
 */
class G(val d: DrawScope, val k: Float) {
    fun u(v: Number) = v.toFloat() * k
    fun o(x: Number, y: Number) = Offset(u(x), u(y))
    fun s(w: Number, h: Number) = Size(u(w), u(h))

    fun image(img: ImageBitmap, x: Number, y: Number, w: Number, h: Number, alpha: Float = 1f, filter: ColorFilter? = null) {
        d.drawImage(
            img, IntOffset.Zero, IntSize(img.width, img.height),
            IntOffset(u(x).roundToInt(), u(y).roundToInt()), IntSize(u(w).roundToInt().coerceAtLeast(1), u(h).roundToInt().coerceAtLeast(1)),
            alpha = alpha, colorFilter = filter, filterQuality = FilterQuality.Medium,
        )
    }

    /**
     * Draws a framed picture stretched to w×h without stretching its corners: the outer [src] pixels of each side
     * become [dst] units, the edges stretch along their length, the middle both ways.
     */
    fun nine(img: ImageBitmap, x: Number, y: Number, w: Number, h: Number, src: IntArray, dst: FloatArray) {
        val sx = intArrayOf(0, src[0], img.width - src[2], img.width)
        val sy = intArrayOf(0, src[1], img.height - src[3], img.height)
        val x0 = u(x); val y0 = u(y); val x3 = x0 + u(w); val y3 = y0 + u(h)
        val dx = floatArrayOf(x0, x0 + u(dst[0]), x3 - u(dst[2]), x3)
        val dy = floatArrayOf(y0, y0 + u(dst[1]), y3 - u(dst[3]), y3)
        for (j in 0..2) for (i in 0..2) {
            val ox = dx[i].roundToInt(); val oy = dy[j].roundToInt()
            d.drawImage(
                img, IntOffset(sx[i], sy[j]), IntSize(sx[i + 1] - sx[i], sy[j + 1] - sy[j]),
                IntOffset(ox, oy), IntSize((dx[i + 1].roundToInt() - ox).coerceAtLeast(1), (dy[j + 1].roundToInt() - oy).coerceAtLeast(1)),
                filterQuality = FilterQuality.Medium,
            )
        }
    }

    fun rect(x: Number, y: Number, w: Number, h: Number, c: Color) = d.drawRect(c, o(x, y), s(w, h))
    fun rect(x: Number, y: Number, w: Number, h: Number, b: Brush) = d.drawRect(b, o(x, y), s(w, h))
    fun round(x: Number, y: Number, w: Number, h: Number, r: Number, c: Color) = d.drawRoundRect(c, o(x, y), s(w, h), CornerRadius(u(r)))
    fun round(x: Number, y: Number, w: Number, h: Number, r: Number, b: Brush) = d.drawRoundRect(b, o(x, y), s(w, h), CornerRadius(u(r)))
    fun outline(x: Number, y: Number, w: Number, h: Number, r: Number, c: Color, width: Number) =
        d.drawRoundRect(c, o(x, y), s(w, h), CornerRadius(u(r)), style = Stroke(u(width)))
    fun oval(cx: Number, cy: Number, rx: Number, ry: Number, c: Color) = d.drawOval(c, o(cx.toFloat() - rx.toFloat(), cy.toFloat() - ry.toFloat()), s(rx.toFloat() * 2, ry.toFloat() * 2))
    fun oval(cx: Number, cy: Number, rx: Number, ry: Number, b: Brush) = d.drawOval(b, o(cx.toFloat() - rx.toFloat(), cy.toFloat() - ry.toFloat()), s(rx.toFloat() * 2, ry.toFloat() * 2))
    fun circle(cx: Number, cy: Number, r: Number, c: Color) = d.drawCircle(c, u(r), o(cx, cy))
    fun circle(cx: Number, cy: Number, r: Number, b: Brush) = d.drawCircle(b, u(r), o(cx, cy))
    fun line(x0: Number, y0: Number, x1: Number, y1: Number, c: Color, width: Number = .6) = d.drawLine(c, o(x0, y0), o(x1, y1), u(width), StrokeCap.Round)
    fun path(p: Path, c: Color) = d.drawPath(p, c)
    fun path(p: Path, c: Color, width: Number) = d.drawPath(p, c, style = Stroke(u(width), cap = StrokeCap.Round))
    fun pathOf(vararg pts: Pair<Number, Number>) = Path().apply {
        pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(u(x), u(y)) else lineTo(u(x), u(y)) }
        close()
    }

    /** Draws [block] moved to (x, y), turned by [deg] and scaled round that point. */
    inline fun at(x: Number, y: Number, deg: Float = 0f, sx: Float = 1f, sy: Float = 1f, block: G.() -> Unit) {
        val px = u(x); val py = u(y)
        d.withTransform({
            translate(px, py)
            if (deg != 0f) rotate(deg, Offset.Zero)
            if (sx != 1f || sy != 1f) scale(sx, sy, Offset.Zero)
            translate(-px, -py)
        }) { block() }
    }

    /** Draws [block] moved by (dx, dy). */
    inline fun shift(dx: Number, dy: Number, block: G.() -> Unit) {
        val x = u(dx); val y = u(dy)
        d.withTransform({ translate(x, y) }) { block() }
    }

    /** Fills a rectangle with [img] repeated (mirrored, so no seams), [tile] units to a repeat. */
    fun tiled(img: ImageBitmap, x: Number, y: Number, w: Number, h: Number, tile: Number) {
        val bmp = img.asAndroidBitmap()
        val sh = BitmapShader(bmp, Shader.TileMode.MIRROR, Shader.TileMode.MIRROR)
        val sc = u(tile) / bmp.width
        sh.setLocalMatrix(Matrix().apply { setScale(sc, sc); postTranslate(u(x), u(y)) })
        val p = Paint().apply { isAntiAlias = true }
        p.asFrameworkPaint().apply { shader = sh; isFilterBitmap = true }
        d.drawIntoCanvas { it.drawRect(u(x), u(y), u(x) + u(w), u(y) + u(h), p) }
    }

    /** Text, centred on x, its baseline at y. */
    fun text(s: String, x: Number, y: Number, size: Number, c: Color, bold: Boolean = true) {
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb((c.alpha * 255).toInt(), (c.red * 255).toInt(), (c.green * 255).toInt(), (c.blue * 255).toInt())
            textSize = u(size); textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        d.drawIntoCanvas { it.nativeCanvas.drawText(s, u(x), u(y), p) }
    }
}
