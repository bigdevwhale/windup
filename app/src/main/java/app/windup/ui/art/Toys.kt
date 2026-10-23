package app.windup.ui.art

import app.windup.domain.Toy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

/** The tin colours. */
object Pal {
    val red = Ramp(0xff8a6a, 0xd24a32, 0x962c1f, 0x5c1a14)
    val brass = Ramp(0xfff2a8, 0xf0c040, 0xc08a20, 0x7a5410)
    val green = Ramp(0xb4e474, 0x6fae46, 0x3f7a30, 0x234a1e)
    val teal = Ramp(0x8ad8cc, 0x3fa098, 0x26665e, 0x153c38)
    val blue = Ramp(0xa0c4f2, 0x4d84c8, 0x2f5694, 0x1c3460)
    val navy = Ramp(0x6c7cb2, 0x2e3e6e, 0x1d2850, 0x10162c)
    val brown = Ramp(0xdc9c64, 0xa2643a, 0x6e3e20, 0x40220f)
    val skin = Ramp(0xffe6c8, 0xf4ba92, 0xcc8a62, 0x8a5638)
    val pink = Ramp(0xffcadc, 0xea86a6, 0xb45272, 0x702e44)
    val silver = Ramp(0xffffff, 0xd2cdc0, 0x968f80, 0x5a554c)
    val white = Ramp(0xffffff, 0xf4ecd8, 0xcbbfa2, 0x8a7e66)
    val orange = Ramp(0xffbc7c, 0xec7e30, 0xb4541c, 0x6a2e0e)
    val black = Ramp(0x70707e, 0x383842, 0x1e1e26, 0x0c0c10)
    val gold = Ramp(0xfff8c8, 0xffd24a, 0xd09a28, 0x8a5c10)
}

/** A toy's moving state: its cycle, its key, and where it is on its stretch of shelf. */
class St {
    var ph = 0.0
    var kph = .4
    var x = 0.0
    var dir = 1
    var turn = 0.0
    var flipped = false
}

class Parts(private val m: Map<String, Buf>, val c: Map<String, Ramp>) {
    operator fun get(k: String) = m.getValue(k)
}

/**
 * One toy's art: its parts, painted once per paint job and rust level, and how it moves. Parts are built
 * without outlines in local coordinates; [put] places an outlined part at those same coordinates.
 * The toy is drawn into a 48×48 frame with its feet on y = 46, centred on x = 24, facing right.
 */
abstract class ToyArt(
    val toy: Toy,
    val speed: Double,
    val shw: Int,
    val step: Double = 0.0,
    val range: Double = 0.0,
    val turns: Boolean = false,
) {
    abstract val roles: Map<String, Ramp>
    abstract val paint: Map<String, Ramp>
    open fun gait(u: Double) = u
    abstract fun build(c: Map<String, Ramp>): Map<String, Buf>

    /** Draws a frame; returns how high the toy is off the shelf. */
    abstract fun draw(b: Buf, p: Parts, st: St, run: Double): Int
    val main: Ramp get() = roles.values.first()
}

fun put(b: Buf, img: Buf, x: Int, y: Int) = b.draw(img, x - 1, y - 1)
fun bp(b: Buf, x: Int, y: Int, c: Int, w: Int = 1, h: Int = 1) = b.r(x, y, w, h, c)
private fun rnd(v: Double) = v.roundToInt()
private val TAU = 2 * PI

/** A two-pixel tin arm with an ink edge, from shoulder to hand. */
fun limb(b: Buf, x0: Int, y0: Int, x1: Int, y1: Int, r: Ramp) {
    val n = max(abs(x1 - x0), abs(y1 - y0)).coerceAtLeast(1)
    val pts = (0..n).map { i -> rnd(x0 + (x1 - x0) * i / n.toDouble()) to rnd(y0 + (y1 - y0) * i / n.toDouble()) }
    for ((x, y) in pts) bp(b, x - 2, y - 2, INK, 4, 4)
    for ((x, y) in pts) { bp(b, x - 1, y - 1, r[1], 2, 2); bp(b, x - 1, y - 1, r[0]) }
}

/** The wind-up key. Its bow narrows with the cosine of its turn, which reads as a key turning in depth. */
object Key {
    private fun bows(vertical: Boolean): Pair<Buf, Buf> {
        val g = if (vertical) Buf(5, 10) else Buf(10, 5)
        val (a, b) = if (vertical) (2.5 to 2.5) to (2.5 to 7.5) else (2.5 to 2.5) to (7.5 to 2.5)
        g.ell(a.first, a.second, 2.5, 2.5, Pal.brass, spec = true)
        g.ell(b.first, b.second, 2.5, 2.5, Pal.brass, spec = true)
        g.set(a.first.toInt(), a.second.toInt(), Pal.brass[3]); g.set(b.first.toInt(), b.second.toInt(), Pal.brass[3])
        return g.outlined() to g.darker(Pal.brass).outlined()
    }

    private val v = bows(true)
    private val h = bows(false)

    fun draw(b: Buf, x: Int, y: Int, side: Boolean, kph: Double) {
        val c = cos(kph)
        val k = max(.16, abs(c))
        if (side) {
            bp(b, x, y - 2, INK, 7, 4); bp(b, x, y - 1, Pal.brass[1], 6, 1); bp(b, x, y, Pal.brass[3], 6, 1)
            val hh = max(2, 2 * rnd(6 * k))
            b.drawScaled(if (c >= 0) v.first else v.second, x + 6, y - hh / 2, 7, hh)
        } else {
            bp(b, x - 2, y - 7, INK, 4, 8); bp(b, x - 1, y - 7, Pal.brass[1], 1, 7); bp(b, x, y - 7, Pal.brass[3], 1, 7)
            val ww = max(2, 2 * rnd(6 * k))
            b.drawScaled(if (c >= 0) h.first else h.second, x - ww / 2, y - 13, ww, 7)
        }
    }
}

private val NOTE = arrayOf(intArrayOf(0, 0, 1, 1), intArrayOf(0, 0, 1, 0), intArrayOf(0, 0, 1, 0), intArrayOf(1, 1, 1, 0), intArrayOf(1, 1, 1, 0))

fun drawNote(b: Buf, x: Int, y: Int, c: Int) {
    for (j in NOTE.indices) for (i in 0 until 4) if (NOTE[j][i] == 1) { bp(b, x + i - 1, y + j, INK); bp(b, x + i + 1, y + j, INK); bp(b, x + i, y + j - 1, INK); bp(b, x + i, y + j + 1, INK) }
    for (j in NOTE.indices) for (i in 0 until 4) if (NOTE[j][i] == 1) bp(b, x + i, y + j, c)
}

object Frog : ToyArt(Toy.FROG, speed = .8, shw = 13) {
    override val roles = mapOf("body" to Pal.green, "belly" to Pal.brass, "stripe" to Pal.red)
    override val paint = mapOf("body" to Pal.silver, "belly" to Pal.pink)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val body = c.getValue("body"); val belly = c.getValue("belly"); val stripe = c.getValue("stripe")
        val g = Buf(30, 20)
        g.ell(7.5, 5.5, 4.6, 4.6, body); g.ell(22.5, 5.5, 4.6, 4.6, body)
        g.ell(15.0, 12.0, 13.6, 7.2, body, spec = true)
        g.ell(15.0, 15.0, 9.0, 3.9, belly, spec = true, clip = { _, y -> y >= 13 })
        for (x in 5..23 step 4) { g.r(x, 7, 2, 1, stripe[1]); g.r(x, 8, 2, 1, stripe[2]) }
        for (x in 7..21 step 4) g.set(x, 10, stripe[1])
        for (ex in listOf(7.5, 22.5)) g.ell(ex, 5.0, 2.9, 2.9, Pal.white, spec = true)
        g.r(7, 3, 2, 3, INK); g.r(21, 3, 2, 3, INK); g.set(7, 3, WHITE); g.set(21, 3, WHITE)
        for (x in 10..20) g.set(x, 12, INK)
        g.set(9, 11, INK); g.set(21, 11, INK)
        g.r(5, 12, 2, 1, Pal.pink[1]); g.r(24, 12, 2, 1, Pal.pink[1])
        val fold = Buf(34, 9)
        fold.ell(5.0, 4.5, 5.0, 4.2, body, lift = -.15); fold.ell(29.0, 4.5, 5.0, 4.2, body, lift = -.15)
        for (x in listOf(1, 4, 7, 26, 29, 32)) fold.set(x, 8, body[3])
        val ext = Buf(34, 15)
        ext.ell(7.5, 6.0, 3.0, 6.2, body, lift = -.2); ext.ell(26.5, 6.0, 3.0, 6.2, body, lift = -.2)
        ext.ell(5.5, 12.6, 4.2, 1.8, body, lift = -.05); ext.ell(28.5, 12.6, 4.2, 1.8, body, lift = -.05)
        return mapOf("body" to g, "fold" to fold, "ext" to ext)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        var h = 0
        var sq = 0
        if (run > 0) when {
            u < .18 -> sq = 1
            u < .78 -> h = rnd(sin(PI * (u - .18) / .6) * 11)
            u < .86 -> sq = 1
        }
        Key.draw(b, 34, 35 - h + (if (h > 4) 0 else sq), true, st.kph)
        if (h > 4) { put(b, p["ext"], 7, 27 - h + 10); put(b, p["body"], 9, 26 - h) } else { put(b, p["fold"], 7, 38); put(b, p["body"], 9, 26 + sq) }
        return h
    }
}

object Monkey : ToyArt(Toy.MONKEY, speed = 1.35, shw = 12) {
    override val roles = mapOf("fur" to Pal.brown, "vest" to Pal.red, "hat" to Pal.red)
    override val paint = mapOf("vest" to Pal.blue, "hat" to Pal.blue, "fur" to Pal.orange)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val fur = c.getValue("fur"); val vest = c.getValue("vest"); val hat = c.getValue("hat")
        val g = Buf(26, 32)
        g.ell(8.0, 28.0, 4.6, 3.0, fur, lift = -.12); g.ell(18.0, 28.0, 4.6, 3.0, fur, lift = -.12)
        g.ell(5.5, 30.0, 2.6, 1.6, Pal.skin); g.ell(20.5, 30.0, 2.6, 1.6, Pal.skin)
        g.ell(13.0, 21.5, 7.2, 7.0, vest, spec = true)
        g.ell(13.0, 21.5, 2.6, 5.6, fur, lift = .08)
        for (y in listOf(17, 20, 23)) { g.set(9, y, Pal.gold[1]); g.set(9, y + 1, Pal.gold[3]); g.set(16, y, Pal.gold[1]); g.set(16, y + 1, Pal.gold[3]) }
        g.ell(5.2, 10.0, 2.7, 2.9, fur); g.ell(20.8, 10.0, 2.7, 2.9, fur); g.set(5, 10, Pal.skin[2]); g.set(20, 10, Pal.skin[2])
        g.ell(13.0, 10.0, 7.0, 6.2, fur, spec = true)
        g.ell(13.0, 11.6, 5.0, 4.1, Pal.skin, spec = true)
        g.r(10, 9, 1, 2, INK); g.r(15, 9, 1, 2, INK); g.set(10, 9, WHITE); g.set(15, 9, WHITE)
        g.r(12, 12, 2, 1, Pal.brown[2])
        g.set(10, 13, INK); g.r(11, 14, 4, 1, INK); g.set(15, 13, INK)
        g.box(10, 1, 6, 4, hat); g.ell(13.0, 1.2, 3.0, 1.1, hat, lift = .35)
        g.set(16, 1, Pal.gold[1]); g.set(17, 2, Pal.gold[1]); g.set(17, 3, Pal.gold[2]); g.r(17, 4, 1, 2, Pal.gold[1])
        val cym = Buf(7, 13)
        cym.ell(3.5, 6.5, 3.0, 6.0, Pal.gold, spec = true); cym.set(3, 6, Pal.gold[3]); cym.line(3, 1, 3, 4, Pal.gold[0])
        return mapOf("body" to g, "cym" to cym)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val d = if (run > 0) rnd(7 * abs(cos(PI * u)).pow(.45)) else 7
        val bob = if (d <= 1) 1 else 0
        Key.draw(b, 29, 34 + bob, true, st.kph)
        put(b, p["body"], 11, 14 + bob)
        val sy = 33 + bob
        val hy = sy - 1
        val hxL = 21 - rnd(d * 1.6)
        val hxR = 47 - hxL
        val fur = p.c.getValue("fur")
        limb(b, 18, sy, hxL, hy, fur); limb(b, 29, sy, hxR, hy, fur)
        put(b, p["cym"], hxL - 4, hy - 6); put(b, p["cym"], hxR - 2, hy - 6)
        if (d <= 1) {
            val y = 23 + bob
            for (x in listOf(21, 26)) { bp(b, x, y, Pal.skin[1], 1, 2); bp(b, x - 1, y + 1, INK, 3, 1) }
        }
        if (d == 0) {
            bp(b, 24, hy - 10, WHITE, 1, 2); bp(b, 24, hy + 7, WHITE, 1, 2)
            bp(b, 19, hy - 8, WHITE); bp(b, 29, hy - 8, WHITE); bp(b, 18, hy + 6, WHITE); bp(b, 30, hy + 6, WHITE)
        }
        return 0
    }
}

object Bear : ToyArt(Toy.BEAR, speed = .95, shw = 15, step = 7.0, range = 15.0, turns = true) {
    override val roles = mapOf("fur" to Pal.brown, "blanket" to Pal.red, "snout" to Pal.skin)
    override val paint = mapOf("fur" to Pal.white, "blanket" to Pal.teal)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val fur = c.getValue("fur"); val blanket = c.getValue("blanket"); val snout = c.getValue("snout")
        val g = Buf(35, 20)
        g.ell(2.6, 8.0, 2.0, 2.0, fur)
        g.ell(14.0, 11.0, 11.6, 7.6, fur, spec = true)
        g.ell(13.5, 8.0, 8.2, 4.6, blanket, spec = true, clip = { _, y -> y <= 9 })
        for (x in 6..21 step 2) { g.set(x, 10, Pal.gold[1]); g.set(x + 1, 10, Pal.gold[2]) }
        for (x in 9..18 step 3) g.set(x, 6, Pal.gold[1])
        g.ell(22.6, 3.4, 2.3, 2.3, fur); g.set(22, 3, snout[2])
        g.ell(26.0, 8.0, 6.0, 5.6, fur, spec = true)
        g.ell(30.6, 10.0, 3.0, 2.4, snout, spec = true)
        g.r(32, 8, 2, 2, INK); g.r(27, 6, 1, 2, INK); g.set(28, 6, WHITE); g.r(30, 12, 2, 1, INK)
        val leg = Buf(5, 9); leg.box(0, 0, 5, 8, fur); leg.r(0, 8, 5, 1, fur[3])
        val far = Buf(5, 9); far.box(0, 0, 5, 8, fur, lift = -.4); far.r(0, 8, 5, 1, fur[3])
        return mapOf("body" to g, "leg" to leg, "far" to far)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val bob = if (run > 0 && sin(4 * PI * u) > .35) -1 else 0
        fun leg(ph: Double): Pair<Int, Int> {
            if (run <= 0) return 0 to 0
            val s = sin(TAU * (u + ph))
            return rnd(1.6 * cos(TAU * (u + ph))) to (if (s > 0) rnd(2 * s) else 0)
        }
        val (a1, b1) = leg(.5); val (a2, b2) = leg(0.0)
        put(b, p["far"], 14 + a1, 38 - b1); put(b, p["far"], 31 + a2, 38 - b2)
        Key.draw(b, 18, 30 + bob, false, st.kph)
        put(b, p["body"], 7, 25 + bob)
        val (a3, b3) = leg(0.0); val (a4, b4) = leg(.5)
        put(b, p["leg"], 10 + a3, 38 - b3); put(b, p["leg"], 27 + a4, 38 - b4)
        return 0
    }
}

object Ballet : ToyArt(Toy.BALLET, speed = .55, shw = 10) {
    override val roles = mapOf("dress" to Pal.teal, "tutu" to Pal.pink, "hair" to Pal.brass, "base" to Pal.red)
    override val paint = mapOf("dress" to Pal.white, "tutu" to Pal.white, "hair" to Pal.black)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val dress = c.getValue("dress"); val tutu = c.getValue("tutu"); val hair = c.getValue("hair"); val baseR = c.getValue("base")
        fun fig(back: Boolean): Buf {
            val g = Buf(22, 34)
            g.ell(11.0, 5.0, 6.2, 4.6, Pal.skin); g.ell(11.0, 5.6, 4.1, 3.2, Pal.skin, erase = true)
            g.ell(11.0, 9.0, 3.5, 3.5, Pal.skin, spec = true)
            if (back) g.ell(11.0, 8.6, 3.7, 3.6, hair, spec = true) else g.ell(11.0, 7.2, 3.7, 2.2, hair, spec = true, clip = { _, y -> y <= 7 })
            g.ell(11.0, 4.6, 1.9, 1.6, hair, spec = true)
            if (!back) { g.set(9, 9, INK); g.set(12, 9, INK); g.set(8, 10, Pal.pink[1]); g.set(13, 10, Pal.pink[1]); g.set(10, 11, Pal.pink[2]); g.set(11, 11, Pal.pink[2]) }
            g.r(10, 12, 2, 1, Pal.skin[2])
            g.ell(11.0, 15.5, 3.3, 3.6, dress, spec = true)
            if (back) { g.set(10, 14, INK); g.set(12, 15, INK); g.set(10, 16, INK); g.set(12, 17, INK) }
            g.ell(11.0, 20.6, 9.4, 2.8, Pal.white, lift = -.1)
            g.ell(11.0, 19.4, 9.6, 2.5, tutu, spec = true)
            for (x in 3..19 step 2) g.set(x, 21, Pal.white[1])
            g.box(10, 22, 2, 9, Pal.skin)
            if (back) { g.line(10, 24, 7, 26, Pal.skin[1]); g.line(7, 26, 10, 28, Pal.skin[1]) } else { g.line(11, 24, 14, 26, Pal.skin[1]); g.line(14, 26, 11, 28, Pal.skin[1]) }
            g.r(10, 31, 2, 2, Pal.pink[1])
            return g
        }
        val base = Buf(22, 7)
        base.box(1, 2, 20, 4, baseR); base.ell(11.0, 2.0, 10.0, 1.9, Pal.brass, spec = true); base.r(1, 5, 20, 1, Pal.gold[2])
        return mapOf("front" to fig(false), "back" to fig(true), "base" to base)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val c = cos(st.ph * TAU)
        val k = .28 + .72 * abs(c)
        val dw = 2 * rnd(12 * k)
        Key.draw(b, 31, 43, true, st.kph)
        put(b, p["base"], 13, 40)
        b.drawScaled(if (c >= 0) p["front"] else p["back"], 24 - dw / 2, 8, dw, 36)
        if (run > 0) for (i in 0 until 2) {
            val v = (st.ph * .8 + i * .5).let { it - floor(it) }
            if (v < .85) drawNote(b, 31 + rnd(sin(v * 7) * 2) + i * 4, 26 - rnd(v * 22), if (i == 1) Pal.gold[1] else Pal.pink[0])
        }
        return 0
    }
}

object Soldier : ToyArt(Toy.SOLDIER, speed = 1.0, shw = 10) {
    override val roles = mapOf("coat" to Pal.red, "trousers" to Pal.navy, "drum" to Pal.white)
    override val paint = mapOf("coat" to Pal.navy, "trousers" to Pal.black)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val coat = c.getValue("coat"); val trousers = c.getValue("trousers"); val drum = c.getValue("drum")
        val g = Buf(24, 30)
        g.box(3, 16, 4, 8, coat); g.box(17, 16, 4, 8, coat); g.ell(5.0, 24.5, 1.8, 1.5, Pal.skin); g.ell(19.0, 24.5, 1.8, 1.5, Pal.skin)
        g.box(6, 15, 12, 11, coat, spec = true)
        g.line(7, 15, 16, 25, Pal.white[1]); g.line(16, 15, 7, 25, Pal.white[1])
        for (y in listOf(17, 19, 23)) g.set(12, y, Pal.gold[1])
        g.ell(6.5, 15.4, 2.2, 1.3, Pal.gold, spec = true); g.ell(17.5, 15.4, 2.2, 1.3, Pal.gold, spec = true)
        g.box(5, 22, 14, 6, drum, spec = true); g.r(5, 22, 14, 1, coat[1]); g.r(5, 27, 14, 1, coat[2])
        for (x in 5 until 19) g.set(x, 23 + abs(((x - 5) % 6) - 3), Pal.gold[2])
        g.ell(12.0, 22.0, 7.0, 1.3, drum, lift = .3)
        g.ell(12.0, 12.0, 3.7, 3.3, Pal.skin, spec = true)
        g.set(10, 12, INK); g.set(13, 12, INK); g.set(9, 13, Pal.red[1]); g.set(14, 13, Pal.red[1]); g.r(11, 14, 2, 1, Pal.red[2])
        g.ell(12.0, 5.4, 5.0, 5.6, Pal.black, spec = true); g.r(8, 10, 8, 1, Pal.black[2])
        g.line(8, 10, 9, 14, Pal.gold[1]); g.line(15, 10, 14, 14, Pal.gold[1])
        g.ell(16.5, 3.2, 1.4, 3.0, coat, spec = true)
        val leg = Buf(5, 12); leg.box(0, 0, 5, 9, trousers); leg.r(2, 0, 1, 9, coat[1]); leg.box(0, 9, 5, 3, Pal.black, spec = true)
        return mapOf("up" to g, "leg" to leg)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val s = sin(TAU * u)
        val lL = if (run > 0) rnd(3 * max(0.0, s)) else 0
        val lR = if (run > 0) rnd(3 * max(0.0, -s)) else 0
        val bob = if (lL > 1 || lR > 1) -1 else 0
        put(b, p["leg"], 18, 34 - lL); put(b, p["leg"], 25, 34 - lR)
        Key.draw(b, 30, 26 + bob, true, st.kph)
        put(b, p["up"], 12, 6 + bob)
        fun hit(at: Double) = if (run > 0) max(0.0, cos(TAU * (u - at))).pow(5) else 0.0
        for ((hand, tgt, raised, h) in listOf(Quad(17 to 30, 21 to 28, 12 to 22, hit(.25)), Quad(31 to 30, 27 to 28, 36 to 22, hit(.75)))) {
            val tx = rnd(raised.first + (tgt.first - raised.first) * h)
            val ty = rnd(raised.second + (tgt.second - raised.second) * h) + bob
            val hx = hand.first; val hy = hand.second + bob
            val n = max(abs(tx - hx), abs(ty - hy)).coerceAtLeast(1)
            val pts = (0..n).map { i -> rnd(hx + (tx - hx) * i / n.toDouble()) to rnd(hy + (ty - hy) * i / n.toDouble()) }
            for ((x, y) in pts) bp(b, x - 1, y - 1, INK, 3, 3)
            for ((x, y) in pts) bp(b, x, y, Pal.gold[1])
            bp(b, tx - 1, ty - 1, INK, 3, 3); bp(b, tx, ty, Pal.white[0])
            if (h > .92) { bp(b, tx - 3, ty + 1, WHITE); bp(b, tx + 3, ty + 1, WHITE) }
        }
        return 0
    }

    private data class Quad(val a: Pair<Int, Int>, val b: Pair<Int, Int>, val c: Pair<Int, Int>, val d: Double)
}

object Bird : ToyArt(Toy.BIRD, speed = .75, shw = 10, step = 6.0, range = 14.0, turns = true) {
    override val roles = mapOf("body" to Pal.brass, "wing" to Pal.orange)
    override val paint = mapOf("body" to Pal.red, "wing" to Pal.navy)
    override fun gait(u: Double) = when { u < .6 -> 0.0; u < .85 -> (u - .6) / .25; else -> 1.0 }
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val body = c.getValue("body"); val wing = c.getValue("wing")
        val g = Buf(22, 15)
        g.ell(3.4, 4.0, 3.3, 2.3, body, lift = -.1)
        g.ell(11.0, 8.6, 9.2, 6.0, body, spec = true); g.ell(9.4, 8.6, 5.6, 3.6, wing, spec = true)
        for ((x, y) in listOf(7 to 8, 9 to 9, 11 to 8, 8 to 10, 10 to 11)) g.set(x, y, wing[3])
        val head = Buf(14, 12)
        head.ell(6.0, 6.4, 5.6, 5.6, body, spec = true); head.set(5, 0, body[1]); head.set(6, 0, body[2]); head.set(4, 1, body[1])
        head.r(8, 4, 2, 2, INK); head.set(8, 4, WHITE); head.r(7, 8, 2, 1, Pal.pink[1])
        head.r(11, 5, 3, 1, Pal.orange[0]); head.r(11, 6, 2, 1, Pal.orange[2])
        val neck = Buf(7, 7); neck.ell(3.5, 3.5, 3.5, 3.5, body, lift = -.1)
        return mapOf("body" to g, "head" to head, "neck" to neck)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        var pk = 0.0
        var hop = 0
        if (run > 0) when {
            u < .18 -> pk = sin(PI * u / .18)
            u >= .24 && u < .42 -> pk = sin(PI * (u - .24) / .18)
            u >= .6 && u < .85 -> hop = rnd(sin(PI * (u - .6) / .25) * 3)
        }
        val y0 = -hop
        for (x in listOf(21, 25)) { bp(b, x, 43 + y0, INK, 1, 3); bp(b, x - 1, 46 + y0, Pal.orange[1], 3, 1) }
        val hx = 25 + rnd(pk * 3)
        val hy = 22 + y0 + rnd(pk * 7)
        put(b, p["neck"], rnd((hx + 3 + 26) / 2.0) - 1, rnd((hy + 4 + 30 + y0) / 2.0))
        Key.draw(b, 19, 33 + y0, false, st.kph)
        put(b, p["body"], 11, 29 + y0)
        put(b, p["head"], hx, hy)
        return hop
    }
}

object Robot : ToyArt(Toy.ROBOT, speed = 1.05, shw = 11) {
    override val roles = mapOf("metal" to Pal.silver, "accent" to Pal.red)
    override val paint = mapOf("metal" to Pal.gold, "accent" to Pal.teal)
    private val VISOR = rgb(0x1d2433)
    private val VISOR_HI = rgb(0x323c52)
    private val PANEL = rgb(0x262b36)
    private val PANEL_HI = rgb(0x3a4252)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val metal = c.getValue("metal"); val accent = c.getValue("accent")
        val g = Buf(24, 31)
        g.r(11, 1, 2, 4, Pal.black[2])
        g.ell(3.6, 10.0, 1.6, 2.2, accent, spec = true); g.ell(20.4, 10.0, 1.6, 2.2, accent, spec = true)
        g.box(5, 5, 14, 10, metal, spec = true); g.r(7, 7, 10, 4, VISOR); g.r(7, 7, 10, 1, VISOR_HI)
        for (x in 9..15 step 2) g.set(x, 13, metal[3])
        for ((x, y) in listOf(6 to 6, 17 to 6, 6 to 13, 17 to 13)) g.set(x, y, metal[3])
        g.r(10, 15, 4, 2, metal[2])
        g.box(4, 17, 16, 12, metal, spec = true); g.r(7, 19, 10, 7, PANEL); g.r(7, 19, 10, 1, PANEL_HI)
        g.ell(9.6, 22.6, 2.2, 2.2, Pal.white, spec = true)
        for ((x, y) in listOf(6 to 28, 11 to 28, 17 to 28)) g.set(x, y, metal[3])
        val arm = Buf(4, 11); arm.box(0, 0, 4, 8, metal); arm.ell(2.0, 9.2, 2.0, 1.8, accent, spec = true)
        val leg = Buf(6, 10); leg.box(1, 0, 4, 6, metal); leg.box(0, 6, 6, 4, accent, spec = true)
        return mapOf("up" to g, "arm" to arm, "leg" to leg)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val s = sin(TAU * u)
        val lL = if (run > 0) rnd(2 * max(0.0, s)) else 0
        val lR = if (run > 0) rnd(2 * max(0.0, -s)) else 0
        val bob = if (lL + lR > 1) -1 else 0
        val sw = if (run > 0) rnd(s) else 0
        put(b, p["leg"], 17, 36 - lL); put(b, p["leg"], 25, 36 - lR)
        Key.draw(b, 29, 19 + bob, true, st.kph)
        put(b, p["arm"], 12, 23 + bob + sw); put(b, p["arm"], 32, 23 + bob - sw)
        put(b, p["up"], 12, 6 + bob)
        val blink = run > 0 && floor(st.ph * 4).toInt() % 2 == 0
        bp(b, 23, 5 + bob, INK, 4, 3); bp(b, 24, 5 + bob, if (blink) Pal.gold[0] else Pal.red[1], 2, 2)
        val glow = if (run > 0) (if (sin(st.ph * 9) > 0) Pal.red[0] else Pal.orange[0]) else Pal.red[2]
        bp(b, 21, 14 + bob, glow, 2, 2); bp(b, 26, 14 + bob, glow, 2, 2)
        val a = st.ph * 5
        bp(b, 22 + rnd(cos(a) * 1.4), 29 + bob + rnd(sin(a) * 1.4), INK)
        val on = if (run > 0) floor(st.ph * 6).toInt() % 3 else -1
        listOf(Pal.red, Pal.gold, Pal.teal).forEachIndexed { i, r -> bp(b, 26, 26 + bob + i * 2, if (on == i) r[0] else r[3], 2, 1) }
        return 0
    }
}

object Penguin : ToyArt(Toy.PENGUIN, speed = 1.2, shw = 11, step = 2.5, range = 7.0) {
    override val roles = mapOf("body" to Pal.navy, "beak" to Pal.orange, "tie" to Pal.red)
    override val paint = mapOf("body" to Pal.black, "beak" to Pal.gold)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val body = c.getValue("body"); val beak = c.getValue("beak"); val tie = c.getValue("tie")
        val g = Buf(20, 24)
        g.ell(10.0, 12.6, 9.0, 11.0, body, spec = true)
        g.ell(10.0, 16.0, 5.6, 7.2, Pal.white, spec = true)
        g.ell(7.0, 7.6, 1.9, 1.9, Pal.white, spec = true); g.ell(13.0, 7.6, 1.9, 1.9, Pal.white, spec = true)
        g.set(7, 7, INK); g.set(13, 7, INK); g.set(7, 8, INK); g.set(13, 8, INK)
        g.ell(10.0, 10.4, 2.2, 1.4, beak, spec = true)
        g.r(7, 13, 2, 3, tie[1]); g.r(11, 13, 2, 3, tie[1]); g.r(9, 14, 2, 1, tie[2]); g.set(7, 15, tie[2]); g.set(12, 15, tie[2])
        for (y in listOf(17, 20)) g.set(10, y, Pal.white[3])
        val fl = Buf(4, 11); fl.ell(2.0, 5.5, 2.0, 5.5, body, lift = -.15)
        val ft = Buf(7, 3); ft.ell(3.5, 1.5, 3.5, 1.6, beak, spec = true)
        return mapOf("body" to g, "fl" to fl, "ft" to ft)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val r = if (run > 0) sin(TAU * u) else 0.0
        val dx = rnd(r * 1.2)
        val fL = if (r > .35) 1 else 0
        val fR = if (r < -.35) 1 else 0
        val flL = if (r > .6) 1 else 0
        val flR = if (r < -.6) 1 else 0
        put(b, p["ft"], 16, 43 - fL); put(b, p["ft"], 25, 43 - fR)
        Key.draw(b, 28 + dx, 24, true, st.kph)
        put(b, p["fl"], 11 + dx - flL, 28 - flL); put(b, p["fl"], 33 + dx + flR, 28 - flR)
        put(b, p["body"], 14 + dx, 20)
        return 0
    }
}

object Acro : ToyArt(Toy.ACRO, speed = .6, shw = 16) {
    override val roles = mapOf("shirt" to Pal.blue, "pants" to Pal.red, "frame" to Pal.brass)
    override val paint = mapOf("shirt" to Pal.red, "pants" to Pal.navy)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val shirt = c.getValue("shirt"); val pants = c.getValue("pants"); val frame = c.getValue("frame")
        val fr = Buf(30, 30)
        fr.box(1, 5, 3, 22, frame); fr.box(26, 5, 3, 22, frame); fr.box(0, 27, 5, 3, frame, lift = -.2); fr.box(25, 27, 5, 3, frame, lift = -.2)
        fr.r(3, 5, 24, 1, Pal.silver[0]); fr.r(3, 6, 24, 1, Pal.silver[2])
        fr.ell(2.5, 4.4, 2.0, 1.6, Pal.gold, spec = true); fr.ell(27.5, 4.4, 2.0, 1.6, Pal.gold, spec = true)
        val fg = Buf(11, 24)
        fg.r(4, 0, 3, 2, Pal.skin[1]); fg.r(4, 2, 1, 5, Pal.skin[1]); fg.r(6, 2, 1, 5, Pal.skin[2])
        fg.ell(5.5, 9.0, 3.2, 3.2, Pal.skin, spec = true); fg.ell(5.5, 7.2, 3.3, 1.9, Pal.brown, clip = { _, y -> y <= 8 })
        fg.set(4, 10, INK); fg.set(7, 10, INK)
        for (y in 12..17) fg.r(2, y, 7, 1, if (y % 2 == 1) Pal.white[1] else shirt[1])
        fg.r(2, 12, 1, 6, shirt[2])
        fg.r(3, 18, 2, 5, pants[1]); fg.r(6, 18, 2, 5, pants[2]); fg.r(3, 23, 2, 1, INK); fg.r(6, 23, 2, 1, INK)
        return mapOf("fr" to fr, "fg" to fg)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = st.ph - floor(st.ph)
        val th = if (run > 0) TAU * u - .55 * sin(TAU * u) else 0.0
        Key.draw(b, 34, 38, true, st.kph)
        put(b, p["fr"], 9, 17)
        b.drawRot(p["fg"], 24, 22, 7.0, 2.0, th)
        return 0
    }
}

/** All the toys' art, and the frames they're drawn through. */
object Toys {
    val art: Map<Toy, ToyArt> = listOf(Frog, Monkey, Bear, Ballet, Soldier, Bird, Robot, Penguin, Acro).associateBy { it.toy }
    operator fun get(t: Toy) = art.getValue(t)

    private val cache = HashMap<String, Parts>()

    /** Rust is drawn in quarter steps so the parts are rebuilt only a few times over the weeks. */
    fun parts(t: Toy, painted: Boolean, rust: Float): Parts = synchronized(cache) {
        val rq = (rust * 4).roundToInt() / 4f
        cache.getOrPut("$t$painted$rq") {
            val a = get(t)
            val c = if (painted) a.roles + a.paint else a.roles
            val raw = a.build(c)
            Parts(raw.mapValues { (k, g) -> g.rusted(rq, t.ordinal * 7 + k.length).outlined() }, c)
        }
    }

    /** Moves a toy on: its cycle at a steady pace, its walk along the shelf, a turn at the end of it. */
    fun step(a: ToyArt, st: St, dt: Double, run: Double) {
        if (st.turn > 0) {
            st.turn += dt / .42
            if (st.turn >= .5 && !st.flipped) { st.dir = -st.dir; st.flipped = true }
            if (st.turn >= 1) st.turn = 0.0
            st.kph += dt * 4 * run
            return
        }
        val prev = st.ph
        st.ph += dt * a.speed * run
        st.kph += dt * 4 * run
        if (a.step > 0 && run > 0) {
            fun g(p: Double) = floor(p) + a.gait(p - floor(p))
            st.x += st.dir * (g(st.ph) - g(prev)) * a.step
            if (abs(st.x) > a.range && sign(st.x).toInt() == st.dir) {
                if (a.turns) { st.turn = 1e-4; st.flipped = false } else st.dir = -st.dir
            }
        }
    }

    private val frame = Buf(48, 48)
    private val SHADOW = rgb(0x0e0804)

    /**
     * Draws a toy onto [dst] with its feet at (cx, by), [k] art pixels per toy pixel. [sil] paints it as a flat
     * silhouette, for a toy not built yet.
     */
    fun drawAt(dst: Buf, t: Toy, parts: Parts, cx: Int, by: Int, st: St, run: Double, k: Int = 1, sil: Int = 0, shadow: Boolean = true) = synchronized(frame) {
        val a = get(t)
        frame.clear()
        val lift = a.draw(frame, parts, st, run)
        val x = cx + if (a.step > 0) rnd(st.x * k) else 0
        if (shadow) {
            val w = max(4, rnd(a.shw * (1 - lift / 18.0)))
            for (i in -w..w) {
                val hh = if (abs(i) > w * .7) 1 else 2
                dst.r(x + i * k, by - k * (hh - 1), k, k * hh, SHADOW, .38f)
            }
        }
        val sx = if (st.turn > 0) max(.14, abs(cos(PI * st.turn))) else 1.0
        val dw = 2 * rnd(24 * sx) * k
        dst.drawScaled(frame, x - dw / 2, by - 46 * k, dw, 48 * k, flip = a.turns && st.dir < 0, tint = sil)
    }
}
