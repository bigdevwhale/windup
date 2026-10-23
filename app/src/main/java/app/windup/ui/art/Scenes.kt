package app.windup.ui.art

import app.windup.domain.Toy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A pin as the drum draws it: minutes into the waking day, which toy's note, whether it was overwound. */
data class DrumPin(val min: Int, val toy: Toy, val over: Boolean)

/** The fixed backdrops: the shelf by the lamp, the workbench, the music box, the capsule machine. */
object Scenes {
    const val SW = 216
    const val SH = 216
    val PLANK = intArrayOf(66, 138, 210)
    val SLOT = intArrayOf(36, 108, 180)

    const val EW = 108
    const val EH = 72
    const val ETOP = 60

    const val BW = 216
    const val BH = 136
    const val TABLE = 120
    const val COMB = 58
    private const val CY0 = 6
    private const val CY1 = 56

    const val STRIP_W = 216
    const val STRIP_H = 40

    private val WARM = intArrayOf(rgb(0x33463a), rgb(0x4a5238), rgb(0x665c3a), rgb(0x857040))
    private val PAPER_A = rgb(0x21403c)
    private val PAPER_B = rgb(0x1b322f)
    private val PAPER_C = rgb(0x2f5a54)
    private val PAPER_D = rgb(0x3d6b5e)
    private val WOOD = intArrayOf(rgb(0xd29a5c), rgb(0xb07a46), rgb(0x8a5a32), rgb(0x7a4c2a), rgb(0x6e4426), rgb(0x4a2c16))
    private val DARK = rgb(0x2a170c)
    private val BRACKET = intArrayOf(rgb(0x3a2414), rgb(0x5a3820))

    /** Old wallpaper: stripes with a small printed diamond. */
    private fun paper(x: Int, y: Int): Int {
        val stripe = (x + 3) % 16 < 8
        val mx = (x + 3) % 16 - 3.5
        val my = (y % 14) - 6.5
        val d = abs(mx) + abs(my)
        return when {
            stripe && d == 0.0 -> PAPER_D
            stripe && d <= 1.5 -> PAPER_C
            stripe -> PAPER_A
            else -> PAPER_B
        }
    }

    /** Wallpaper under a lamp at (lx, ly): warm light in stepped, dithered rings. */
    fun wall(b: Buf, w: Int, h: Int, lx: Double, ly: Double, reach: Double, y0: Int = 0) {
        for (j in y0 until y0 + h) for (i in 0 until w) {
            val d = hypot((i - lx) / 1.25, j - ly)
            val l = (1 - d / reach).coerceIn(0.0, 1.0).pow(1.7)
            val lv = floor((l * 4.2 + bay(i, j) * .9).coerceIn(0.0, 4.4)).toInt()
            b.set(i, j, if (lv <= 0) paper(i, j) else WARM[(lv - 1).coerceAtMost(3)])
        }
    }

    /** A wooden shelf board seen a little from above, with grain and a shadow on the wall below. */
    fun plank(b: Buf, y: Int, w: Int) {
        for (i in 0 until w) {
            b.set(i, y, WOOD[0]); b.set(i, y + 1, WOOD[1])
            for (j in 2 until 6) {
                val g = sin(i * .09 + j * 1.7 + sin(i * .023) * 3) > .82
                b.set(i, y + j, if (g) WOOD[4] else if (j == 2) WOOD[2] else WOOD[3])
            }
            b.set(i, y + 6, WOOD[5])
            for (j in 7 until 10) if ((i + j) % 2 == 0 || j == 7) b.p(i, y + j, rgb(0x080402), .35f)
        }
    }

    private fun bracket(b: Buf, x: Int, y: Int) {
        b.r(x, y, 2, 8, BRACKET[0]); b.r(x + 2, y + 1, 3, 2, BRACKET[1]); b.r(x + 4, y + 2, 2, 2, BRACKET[0]); b.r(x, y + 8, 2, 1, INK)
    }

    val shelf: Buf by lazy {
        val b = Buf(SW, SH)
        wall(b, SW, SH, 108.0, 12.0, 190.0)
        for (y in PLANK) {
            plank(b, y, SW)
            if (y + 7 < SH) { bracket(b, 8, y + 7); bracket(b, SW - 14, y + 7) }
        }
        b.r(107, 0, 1, 5, rgb(0x0c0806))
        val shade = Buf(26, 10)
        for (j in 0 until 8) {
            val w = 5 + j * 1.5
            shade.box((13 - w).roundToInt(), j, (w * 2).roundToInt(), 1, Pal.teal, lift = .1 - j * .03)
        }
        shade.r(1, 8, 24, 1, Pal.gold[2]); shade.r(2, 8, 22, 1, Pal.gold[1])
        b.draw(shade.outlined(), 94, 3)
        b.r(103, 13, 10, 2, rgb(0xfff6c8)); b.r(105, 13, 6, 1, WHITE)
        b
    }

    /** The plate under a toy, in its metal, its bar showing the winding left and a dot per alloy step. */
    fun plate(b: Buf, cx: Int, by: Int, wind: Float, alloy: Int = 0) {
        val m = Pal.metals[alloy.coerceIn(0, Pal.metals.size - 1)]
        b.r(cx - 12, by + 1, 24, 6, INK); b.r(cx - 11, by + 2, 22, 4, m[2]); b.r(cx - 11, by + 2, 22, 1, m[0])
        b.r(cx - 9, by + 3, 18, 2, DARK)
        val n = (18 * wind).roundToInt()
        b.r(cx - 9, by + 3, n, 1, Pal.gold[1]); b.r(cx - 9, by + 4, n, 1, Pal.gold[2])
        for (i in 0 until alloy) b.set(cx - 2 * alloy + 1 + i * 4, by + 6, m[0])
    }

    private val FLAGS = listOf(Pal.red, Pal.brass, Pal.teal, Pal.pink, Pal.blue)

    /** Bunting across the top shelf: every toy was wound today. */
    fun garland(b: Buf, ms: Long) {
        val y0 = 10
        for (x in 2 until SW - 2) {
            val y = y0 + (sin(PI * x / (SW - 4)) * 14).roundToInt()
            b.set(x, y, INK)
            if (x % 12 == 6) {
                val f = FLAGS[(x / 12) % FLAGS.size]
                val sway = if (((ms / 400) + x / 12) % 2 == 0L) 0 else 1
                for (j in 0 until 6) for (i in -3 + j / 2..3 - j / 2) b.set(x + i + if (j > 2) sway else 0, y + 1 + j, if (j == 5 || i == -3 + j / 2 || i == 3 - j / 2) INK else f[if (i < 0) 0 else 1])
            }
        }
    }

    /** The question mark over the empty place a blueprint toy will take. */
    fun query(b: Buf, cx: Int, by: Int) {
        val c = rgb(0x9ab8b2)
        b.r(cx - 1, by - 30, 3, 2, c); b.r(cx + 1, by - 28, 2, 3, c); b.r(cx, by - 25, 2, 2, c); b.r(cx, by - 22, 2, 2, c)
    }

    val bench: Buf by lazy {
        val b = Buf(EW, EH)
        wall(b, EW, EH, 54.0, -6.0, 100.0)
        for (i in 0 until EW) for (j in ETOP until EH) {
            val g = sin(i * .12 + j * 1.3 + sin(i * .04) * 2) > .85
            b.set(i, j, when { j == ETOP -> WOOD[0]; j == ETOP + 1 -> WOOD[1]; g -> WOOD[4]; j > ETOP + 5 -> WOOD[3]; else -> WOOD[2] })
        }
        b
    }

    /** The progress of a set, as a brass gauge set in the bench. */
    fun gauge(b: Buf, p: Float) {
        b.r(8, ETOP + 5, EW - 16, 4, INK); b.r(9, ETOP + 6, EW - 18, 2, DARK)
        val n = ((EW - 18) * p.coerceIn(0f, 1f)).roundToInt()
        b.r(9, ETOP + 6, n, 1, Pal.gold[1]); b.r(9, ETOP + 7, n, 1, Pal.gold[2])
    }

    val box: Buf by lazy {
        val b = Buf(BW, BH)
        wall(b, BW, BH, 108.0, 44.0, 160.0)
        val wood = Buf(BW, 62); wood.box(0, 0, BW, 62, Pal.brown, lift = -.2); b.draw(wood, 0, 0)
        b.r(4, 4, BW - 8, 54, DARK)
        plank(b, TABLE, BW)
        b.r(0, TABLE + 7, BW, BH - TABLE - 7, WOOD[3])
        b
    }

    private fun drumRows(b: Buf, x0: Int, x1: Int, y0: Int, y1: Int) {
        for (j in y0 until y1) {
            val n = (j - y0 + .5) / (y1 - y0) * 2 - 1
            val l = -.7 * n + .6 * sqrt(1 - n * n)
            for (i in x0 until x1) b.set(i, j, tone(l + bay(i, j) * .2, Pal.brass, false))
        }
    }

    private fun pin(b: Buf, x: Int, y: Int, t: Toy, over: Boolean) {
        b.r(x - 1, y - 1, 4, 4, INK)
        b.r(x, y, 2, 2, if (over) Pal.silver[2] else Toys[t].main[1])
        b.p(x, y, WHITE, .8f)
    }

    /** The day as a flat strip of drum: time across, the toy's note down, and a red line at now. */
    /** Hours between the long ticks and the labels under the drum: whatever divides the day evenly. */
    fun hourStep(len: Int): Int {
        val hours = (len / 60).coerceAtLeast(1)
        return listOf(2, 3, 4).firstOrNull { hours % it == 0 } ?: 2
    }

    /** Where an hour of the day falls across the strip, 0..1 of its width. */
    fun stripX(min: Int, len: Int) = (6 + min.coerceIn(0, len) * (STRIP_W - 12) / len) / STRIP_W.toFloat()

    fun strip(b: Buf, pins: List<DrumPin>, len: Int, now: Int?) {
        val w = STRIP_W
        val h = STRIP_H
        drumRows(b, 0, w, 0, h)
        b.r(0, 0, w, 1, INK); b.r(0, h - 1, w, 1, INK)
        val hours = len / 60
        for (k in 0..hours) {
            val x = 6 + k * (w - 12) / hours.coerceAtLeast(1)
            val tall = if (k % hourStep(len) == 0) 4 else 2
            b.r(x, 1, 1, tall, Pal.brass[3]); b.r(x, h - 1 - tall, 1, tall, Pal.brass[3])
        }
        if (now != null) {
            val nx = 6 + now.coerceIn(0, len) * (w - 12) / len
            b.r(nx + 1, 1, w - nx - 1, h - 2, rgb(0x1e130c), .35f)
            b.r(nx, 0, 1, h, Pal.red[1]); b.r(nx - 1, 0, 3, 2, Pal.red[2])
        }
        for (p in pins) pin(b, 6 + p.min.coerceIn(0, len) * (w - 12) / len, 5 + (p.toy.ordinal * 3.5).roundToInt(), p.toy, p.over)
    }

    /**
     * The music box, playing: the drum turns under the comb at [play] minutes into the day; each tooth shakes
     * for a moment after its note ([plucked] holds the time of each toy's last note).
     */
    fun drum(b: Buf, pins: List<DrumPin>, len: Int, play: Double, plucked: LongArray, nowMs: Long, playing: Boolean = true) {
        val pxm = .6
        drumRows(b, COMB - 6, BW - 5, CY0, CY1)
        for (k in 0..len / 60) {
            val x = (COMB + (k * 60 - play) * pxm).roundToInt()
            if (x < COMB - 6 || x > BW - 6) continue
            b.r(x, CY0 + 1, 1, 3, Pal.brass[3]); b.r(x, CY1 - 4, 1, 3, Pal.brass[3])
        }
        for (p in pins) {
            val x = (COMB + (p.min - play) * pxm).roundToInt()
            if (x < COMB - 6 || x > BW - 7) continue
            pin(b, x, CY0 + 4 + (p.toy.ordinal * 4.7).roundToInt(), p.toy, p.over)
        }
        b.r(COMB - 7, CY0, 1, CY1 - CY0, INK); b.r(BW - 5, CY0, 1, CY1 - CY0, INK)
        for (j in CY0 until CY1) for (i in 6 until 16) b.set(i, j, tone(-.5 * ((i - 6) / 10.0 * 2 - 1) + .4 + bay(i, j) * .2, Pal.silver, false))
        for (t in Toy.entries) {
            val y = CY0 + 4 + (t.ordinal * 4.7).roundToInt()
            val pl = nowMs - plucked[t.ordinal] < 260
            val v = if (pl) (if ((nowMs / 45) % 2 == 0L) 1 else -1) else 0
            b.r(16, y - 1, COMB - 16 + v, 4, INK)
            b.r(16, y, COMB - 17 + v, 1, if (pl) WHITE else Pal.silver[1])
            b.r(16, y + 1, COMB - 17 + v, 1, Pal.silver[2])
        }
        b.r(COMB, 2, 1, CY1 - 2, Pal.red[1])
        if (playing && play > 0) b.r(4, CY1 + 1, ((BW - 8) * (play / len).coerceIn(0.0, 1.0)).roundToInt(), 1, Pal.gold[1])
    }

    // The capsule machine.
    const val MW = 48
    const val MH = 64
    private val CAP_COLORS = listOf(Pal.red, Pal.brass, Pal.teal, Pal.pink, Pal.blue, Pal.green)
    val capsules: List<Buf> by lazy {
        CAP_COLORS.map { c ->
            val g = Buf(7, 7)
            g.ell(3.5, 3.5, 3.5, 3.5, c, spec = true, clip = { _, y -> y <= 3 })
            g.ell(3.5, 3.5, 3.5, 3.5, Pal.white, spec = true, clip = { _, y -> y > 3 })
            g.r(0, 3, 7, 1, c[2])
            g.outlined()
        }
    }
    private val machine: Buf by lazy {
        val g = Buf(46, 62)
        g.ell(23.0, 19.0, 17.0, 17.0, Pal.white, lift = .1); g.ell(23.0, 19.0, 15.4, 15.4, Pal.white, erase = true)
        g.box(15, 1, 16, 3, Pal.red, spec = true); g.r(22, 0, 2, 1, Pal.gold[1])
        g.box(7, 34, 32, 4, Pal.red, spec = true); g.box(9, 38, 28, 22, Pal.red, spec = true); g.box(5, 58, 36, 4, Pal.red, lift = -.25)
        g.ell(23.0, 45.0, 5.0, 5.0, Pal.silver, spec = true); g.ell(23.0, 45.0, 2.2, 2.2, Pal.silver, lift = -.3)
        g.r(16, 52, 14, 6, DARK); g.r(16, 52, 14, 1, Pal.red[3])
        g.outlined()
    }
    private val GLASS = rgb(0xd8ece6)
    private val GLASS_EDGE = rgb(0xb8d6ce)
    private val SPOTS = listOf(13 to 24, 20 to 27, 27 to 25, 33 to 22, 16 to 17, 24 to 19, 30 to 15, 18 to 10, 26 to 11, 12 to 30, 22 to 32, 30 to 30)

    /** The machine at a moment: capsules rattle while the crank turns, then one drops into the tray. */
    fun machine(b: Buf, bg: Int, shake: Double, crank: Double, drop: Int?, dropY: Int, ms: Long) {
        b.r(0, 0, MW, MH, bg)
        for (j in 0 until MH) for (i in 0 until MW) {
            val d = hypot(i + .5 - 24, j + .5 - 20)
            if (d < 15.4) b.set(i, j, if (d > 12) GLASS_EDGE else GLASS)
        }
        SPOTS.forEachIndexed { k, (x, y) ->
            val sx = (sin(ms / 60.0 + k * 1.7) * shake * 1.4).roundToInt()
            val sy = (kotlin.math.cos(ms / 70.0 + k) * shake).roundToInt()
            b.draw(capsules[k % capsules.size], x - 4 + sx, y - 4 + sy)
        }
        for (j in 0 until MH) for (i in 0 until MW) {
            val d = hypot(i + .5 - 24, j + .5 - 20)
            if (d in 13.8..15.6 && (i - 24) * -.6 + (j - 20) * -.8 > 4) b.p(i, j, WHITE, .55f)
        }
        b.r(15, 10, 2, 4, WHITE, .8f); b.r(17, 8, 2, 2, WHITE, .8f)
        b.draw(machine, -1, 0)
        val cs = kotlin.math.cos(crank)
        val w = 2 * (6 * abs(cs).coerceAtLeast(.15)).roundToInt()
        b.r(24 - w / 2 - 1, 45, w + 2, 3, INK); b.r(24 - w / 2, 46, w, 1, if (cs > 0) Pal.gold[1] else Pal.gold[2])
        if (drop != null) b.draw(capsules[drop % capsules.size], 19, dropY - 1)
    }
}
