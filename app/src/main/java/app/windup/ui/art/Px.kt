package app.windup.ui.art

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A small ARGB pixel buffer every scene and toy part is painted into, one art pixel per cell. Screens
 * scale it up with nearest-neighbour filtering. Colours are 0xAARRGGBB ints; 0 is clear.
 */
class Buf(val w: Int, val h: Int) {
    val d = IntArray(w * h)

    fun copyFrom(o: Buf) = System.arraycopy(o.d, 0, d, 0, d.size)
    fun clear() = d.fill(0)

    fun get(x: Int, y: Int): Int = if (x < 0 || y < 0 || x >= w || y >= h) 0 else d[y * w + x]

    fun set(x: Int, y: Int, c: Int) {
        if (x < 0 || y < 0 || x >= w || y >= h) return
        d[y * w + x] = c
    }

    /** Paints a pixel, blending by [a] (0..1) over what is there. */
    fun p(x: Int, y: Int, c: Int, a: Float = 1f) {
        if (x < 0 || y < 0 || x >= w || y >= h || a <= 0f || c == 0) return
        val i = y * w + x
        if (a >= 1f) { d[i] = c or OPAQUE; return }
        val o = d[i]
        if (o ushr 24 == 0) { d[i] = (c and 0xFFFFFF) or ((a * 255).toInt() shl 24); return }
        val r = ch(o, 16) + ((ch(c, 16) - ch(o, 16)) * a).toInt()
        val g = ch(o, 8) + ((ch(c, 8) - ch(o, 8)) * a).toInt()
        val b = ch(o, 0) + ((ch(c, 0) - ch(o, 0)) * a).toInt()
        d[i] = (max(o ushr 24, (a * 255).toInt()) shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun r(x: Int, y: Int, w: Int, h: Int, c: Int, a: Float = 1f) {
        for (j in 0 until h) for (i in 0 until w) p(x + i, y + j, c, a)
    }

    /** Copies [src] over this buffer at (x, y), skipping clear pixels. */
    fun draw(src: Buf, x: Int, y: Int) {
        for (j in 0 until src.h) {
            val yy = y + j
            if (yy < 0 || yy >= h) continue
            for (i in 0 until src.w) {
                val c = src.d[j * src.w + i]
                if (c ushr 24 == 0) continue
                val xx = x + i
                if (xx < 0 || xx >= w) continue
                if (c ushr 24 == 0xFF) d[yy * w + xx] = c else p(xx, yy, c, (c ushr 24) / 255f)
            }
        }
    }

    /** Draws [src] stretched into [dw]×[dh] at (x, y), nearest neighbour; [flip] mirrors it. [tint] paints every pixel one colour. */
    fun drawScaled(src: Buf, x: Int, y: Int, dw: Int, dh: Int, flip: Boolean = false, tint: Int = 0) {
        if (dw <= 0 || dh <= 0) return
        for (j in 0 until dh) {
            val yy = y + j
            if (yy < 0 || yy >= h) continue
            val sy = (j * src.h / dh).coerceIn(0, src.h - 1)
            for (i in 0 until dw) {
                val xx = x + i
                if (xx < 0 || xx >= w) continue
                var sx = (i * src.w / dw).coerceIn(0, src.w - 1)
                if (flip) sx = src.w - 1 - sx
                val c = src.d[sy * src.w + sx]
                if (c ushr 24 == 0) continue
                if (tint != 0) d[yy * w + xx] = tint or OPAQUE
                else if (c ushr 24 == 0xFF) d[yy * w + xx] = c else p(xx, yy, c, (c ushr 24) / 255f)
            }
        }
    }

    /** Draws [src] turned by [angle] round the point (px, py) of this buffer; (ox, oy) is that pivot inside [src]. */
    fun drawRot(src: Buf, px: Int, py: Int, ox: Double, oy: Double, angle: Double) {
        val c = cos(-angle)
        val s = sin(-angle)
        val r = (src.w + src.h).toDouble()
        val x0 = (px - r).toInt(); val x1 = (px + r).toInt()
        val y0 = (py - r).toInt(); val y1 = (py + r).toInt()
        for (yy in max(0, y0)..minOf(h - 1, y1)) for (xx in max(0, x0)..minOf(w - 1, x1)) {
            val dx = xx + .5 - px
            val dy = yy + .5 - py
            val sx = floor(dx * c - dy * s + ox).toInt()
            val sy = floor(dx * s + dy * c + oy).toInt()
            if (sx < 0 || sy < 0 || sx >= src.w || sy >= src.h) continue
            val col = src.d[sy * src.w + sx]
            if (col ushr 24 != 0) d[yy * w + xx] = col
        }
    }

    companion object {
        const val OPAQUE = 0xFF000000.toInt()
        fun ch(c: Int, sh: Int) = (c shr sh) and 0xFF
    }
}

fun rgb(hex: Long): Int = (hex or 0xFF000000).toInt()

/** The ink every outline is drawn with. */
val INK = rgb(0x1e130c)
val WHITE = rgb(0xffffff)

fun mix(a: Int, b: Int, t: Float): Int {
    fun one(sh: Int) = (Buf.ch(a, sh) + (Buf.ch(b, sh) - Buf.ch(a, sh)) * t).roundToInt().coerceIn(0, 255)
    return Buf.OPAQUE or (one(16) shl 16) or (one(8) shl 8) or one(0)
}

private val B4 = intArrayOf(0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5)

/** The 4×4 ordered-dither threshold at a pixel, centred: −0.5..0.5. */
fun bay(x: Int, y: Int): Double = B4[((y % 4 + 4) % 4) * 4 + ((x % 4 + 4) % 4)] / 16.0 - .5

fun hash(i: Int, j: Int, k: Int = 0): Double {
    var h = i * 374761393 + j * 668265263 + k * 1442695041
    h = (h xor (h ushr 13)) * 1274126177
    return ((h xor (h ushr 16)).toLong() and 0xFFFFFFFFL) / 4294967296.0
}

fun vn(x: Double, y: Double, s: Int): Double {
    val i = floor(x).toInt()
    val j = floor(y).toInt()
    val fx = x - i
    val fy = y - j
    val a = hash(i, j, s)
    val b = hash(i + 1, j, s)
    val c = hash(i, j + 1, s)
    val d = hash(i + 1, j + 1, s)
    val ux = fx * fx * (3 - 2 * fx)
    val uy = fy * fy * (3 - 2 * fy)
    return a + (b - a) * ux + (c - a) * uy + (a - b - c + d) * ux * uy
}

/** A four-tone ramp for tin: highlight, base, shade, deep shade. */
class Ramp(vararg hex: Long) {
    val c = IntArray(4) { rgb(hex[it]) }
    operator fun get(i: Int) = c[i]
}

/** Light from the upper left, four tones and a white glint. */
fun tone(l: Double, r: Ramp, spec: Boolean) = when {
    spec && l > 1.02 -> WHITE
    l > .74 -> r[0]
    l > .34 -> r[1]
    l > -.02 -> r[2]
    else -> r[3]
}

/** A shaded ellipse, like a pressed tin dome. */
fun Buf.ell(
    cx: Double, cy: Double, rx: Double, ry: Double, r: Ramp,
    spec: Boolean = false, lift: Double = 0.0, flat: Int = -1, erase: Boolean = false, clip: ((Int, Int) -> Boolean)? = null,
) {
    for (y in floor(cy - ry - 1).toInt()..(cy + ry + 1).toInt()) for (x in floor(cx - rx - 1).toInt()..(cx + rx + 1).toInt()) {
        val nx = (x + .5 - cx) / rx
        val ny = (y + .5 - cy) / ry
        val r2 = nx * nx + ny * ny
        if (r2 > 1) continue
        if (clip != null && !clip(x, y)) continue
        if (erase) { set(x, y, 0); continue }
        val l = -.42 * nx - .58 * ny + .7 * sqrt(1 - r2) + bay(x, y) * .18 + lift
        p(x, y, if (flat >= 0) r[flat] else tone(l, r, spec))
    }
}

/** A shaded block, rounded across like a tin cylinder, its top edge catching the light. */
fun Buf.box(x0: Int, y0: Int, w: Int, h: Int, r: Ramp, spec: Boolean = false, lift: Double = 0.0) {
    for (y in y0 until y0 + h) for (x in x0 until x0 + w) {
        val nx = ((x - x0) + .5) / w * 2 - 1
        val l = -.4 * nx + .72 * sqrt(1 - nx * nx * .85) + bay(x, y) * .18 + lift + (if (y == y0) .35 else 0.0) - (if (y == y0 + h - 1) .25 else 0.0)
        p(x, y, tone(l, r, spec))
    }
}

fun Buf.line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
    var x = x0; var y = y0
    val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
    val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
    var e = dx + dy
    while (true) {
        p(x, y, c)
        if (x == x1 && y == y1) break
        val e2 = 2 * e
        if (e2 >= dy) { e += dy; x += sx }
        if (e2 <= dx) { e += dx; y += sy }
    }
}

/** A copy one pixel bigger on every side with an ink outline round the shape. */
fun Buf.outlined(): Buf {
    val o = Buf(w + 2, h + 2)
    for (y in 0 until h) for (x in 0 until w) o.d[(y + 1) * o.w + x + 1] = d[y * w + x]
    val add = ArrayList<Int>()
    for (y in 0 until o.h) for (x in 0 until o.w) {
        if (o.get(x, y) != 0) continue
        if (o.get(x - 1, y) != 0 || o.get(x + 1, y) != 0 || o.get(x, y - 1) != 0 || o.get(x, y + 1) != 0) add.add(y * o.w + x)
    }
    for (i in add) o.d[i] = INK
    return o
}

private val RUST = intArrayOf(rgb(0x7a3616), rgb(0xa8521f), rgb(0xc27838))

/** Blotches of rust over everything but the ink, [r] 0..1. */
fun Buf.rusted(r: Float, seed: Int): Buf {
    if (r <= 0f) return this
    val o = Buf(w, h)
    o.copyFrom(this)
    for (y in 0 until h) for (x in 0 until w) {
        val c = o.d[y * w + x]
        if (c == 0 || c == INK) continue
        val n = vn(x * .32, y * .32, seed)
        if (n < r * .5) o.d[y * w + x] = if (n < r * .22) RUST[0] else if (hash(x, y, seed) < .5) RUST[1] else RUST[2]
    }
    return o
}

/** Recolours a part by moving each tone one step darker, for the back of a key. */
fun Buf.darker(r: Ramp): Buf {
    val o = Buf(w, h)
    for (i in d.indices) o.d[i] = when (d[i]) { r[0], WHITE -> r[1]; r[1] -> r[2]; r[2], r[3] -> r[3]; else -> d[i] }
    return o
}
