package app.windup.ui.art

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import app.windup.domain.Rules
import app.windup.domain.Toy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

/** The tin colours a few scene details are drawn in. */
object Pal {
    /** The plates' metals: tin, brass, copper, silver, gold — light, base, deep. */
    val metals = listOf(
        intArrayOf(rgb(0xe6e2d8), rgb(0xa9a49a), rgb(0x6f6a62)),
        intArrayOf(rgb(0xfff2a8), rgb(0xf0c040), rgb(0xc08a20)),
        intArrayOf(rgb(0xffc49a), rgb(0xd9824a), rgb(0x9c5229)),
        intArrayOf(rgb(0xffffff), rgb(0xd2cdc0), rgb(0x968f80)),
        intArrayOf(rgb(0xfff8c8), rgb(0xffd24a), rgb(0xd09a28)),
    )
    val red = rgb(0xd24a32)
    val gold = rgb(0xffd24a)
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

/** How a toy moves as a whole: it hops, bobs, walks, spins, swings or breathes. */
enum class Move { HOP, BOB, DRUM, WALK, SPIN, SWING, BREATHE }

/**
 * One toy's art: its painting (plain and painted), how big it stands, how it moves, and where its key goes in,
 * as fractions of the picture from its top left (the toys face right, so the key is in the back, on the left).
 */
class ToyArt(
    val toy: Toy,
    val speed: Double,
    val h: Float,
    val move: Move,
    val shw: Int,
    val color: Int,
    val step: Double = 0.0,
    val range: Double = 0.0,
    val turns: Boolean = false,
    val kx: Float = .14f,
    val ky: Float = .5f,
    val gait: (Double) -> Double = { it },
) {
    val name = toy.name.lowercase()
}

/** How a toy looks today: its paint, its rust, gilt. */
data class Look(val painted: Boolean, val rust: Float, val gold: Boolean) {
    val filter: ColorFilter? get() = Tints.look(rust, gold)
}

/** All the toys' art, and the frames they're drawn through. */
object Toys {
    private val birdGait: (Double) -> Double = { u -> when { u < .6 -> 0.0; u < .85 -> (u - .6) / .25; else -> 1.0 } }

    val art: Map<Toy, ToyArt> = listOf(
        ToyArt(Toy.FROG, .8, 30f, Move.HOP, 14, rgb(0x6fae46), kx = .1f, ky = .42f),
        ToyArt(Toy.MONKEY, 1.35, 44f, Move.DRUM, 12, rgb(0xa2643a), kx = .2f, ky = .48f),
        ToyArt(Toy.BEAR, .95, 32f, Move.WALK, 16, rgb(0x8a5a32), step = 7.0, range = 14.0, turns = true, kx = .1f, ky = .38f),
        ToyArt(Toy.BALLET, .55, 48f, Move.SPIN, 9, rgb(0x3fa098), kx = .4f, ky = .9f),
        ToyArt(Toy.SOLDIER, 1.0, 46f, Move.DRUM, 10, rgb(0xd24a32), kx = .16f, ky = .45f),
        ToyArt(Toy.BIRD, .75, 28f, Move.WALK, 10, rgb(0xf0c040), step = 6.0, range = 14.0, turns = true, kx = .3f, ky = .45f, gait = birdGait),
        ToyArt(Toy.ROBOT, 1.05, 46f, Move.BOB, 11, rgb(0xa9a49a), kx = .25f, ky = .45f),
        ToyArt(Toy.PENGUIN, 1.2, 36f, Move.WALK, 11, rgb(0x2e3e6e), step = 2.5, range = 7.0, kx = .1f, ky = .45f),
        ToyArt(Toy.ACRO, .6, 48f, Move.SWING, 17, rgb(0x4d84c8), kx = .05f, ky = .86f),
        ToyArt(Toy.SKIER, .9, 42f, Move.WALK, 15, rgb(0xec7e30), step = 6.0, range = 14.0, turns = true, kx = .32f, ky = .4f),
        ToyArt(Toy.HARE, 1.3, 44f, Move.HOP, 11, rgb(0xcbbfa2), kx = .3f, ky = .55f),
        ToyArt(Toy.DIVER, .32, 44f, Move.BREATHE, 12, rgb(0xc08a20), kx = .12f, ky = .38f),
        ToyArt(Toy.MINER, .9, 36f, Move.WALK, 19, rgb(0x2f5694), step = 5.0, range = 9.0, turns = true, kx = .15f, ky = .35f),
        ToyArt(Toy.CLOWN, 1.0, 46f, Move.BOB, 9, rgb(0xb45272), step = 3.0, range = 8.0, kx = .25f, ky = .5f),
        ToyArt(Toy.SAILOR, .8, 44f, Move.BOB, 10, rgb(0x1d2850), kx = .25f, ky = .45f),
    ).associateBy { it.toy }

    operator fun get(t: Toy) = art.getValue(t)

    fun parts(t: Toy, painted: Boolean, rust: Float, gold: Boolean = false) = Look(painted, rust, gold)

    fun image(t: Toy, painted: Boolean) = Img["toy_${get(t).name}${if (painted) "_p" else ""}"]

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

    /** Toys whose own motion is the trick, or who'd drop what they carry. */
    private val NO_TRICK = setOf(Toy.BALLET, Toy.ACRO, Toy.MINER, Toy.CLOWN)

    /** Copper and better toys do a somersault every sixth cycle; the spinner and the acrobat have their own. */
    private fun trick(t: Toy, st: St, alloy: Int): Double? {
        if (alloy < Rules.COPPER || t in NO_TRICK) return null
        val n = floor(st.ph).toInt()
        return if (n % 6 == 5) st.ph - n else null
    }

    /** The size a toy is drawn at, [k] times its own height, never wider than a slot. */
    fun size(t: Toy, painted: Boolean, k: Float = 1f): Pair<Float, Float> {
        val img = image(t, painted)
        val asp = img.width.toFloat() / img.height
        var hh = get(t).h * k
        var ww = hh * asp
        val maxW = 62f * k
        if (ww > maxW) { ww = maxW; hh = ww / asp }
        return ww to hh
    }

    private val SHADOW = Color(0xFF0E0804)

    /**
     * Draws a toy with its feet at (cx, by), [k] times its usual size. [sil] paints it as a flat silhouette of
     * that colour, for a toy not built yet; [alloy] picks its key's metal and its tricks; a [jam]med toy lies on
     * its side.
     */
    fun drawAt(
        g: G, t: Toy, look: Look, cx: Int, by: Int, st: St, run: Double, k: Float = 1f, sil: Int = 0, shadow: Boolean = true,
        alloy: Int = 0, jam: Boolean = false,
    ) {
        val a = get(t)
        val img = image(t, look.painted && sil == 0)
        val (ww, hh) = size(t, look.painted && sil == 0, k)
        val r = if (jam) 0.0 else run
        val u = frac(st.ph)
        val w2 = 2 * PI * u
        var lift = 0.0
        var deg = 0.0
        var sx = 1.0
        var sy = 1.0
        var pivotTop = false
        when (a.move) {
            Move.HOP -> if (u < .55) sy = 1 - .09 * sin(PI * u / .55) * r else {
                val j = sin(PI * (u - .55) / .45)
                lift = j * 11 * k * r; deg = -j * 9 * r; sy = 1 + .05 * j * r
            }
            Move.BOB -> { lift = abs(sin(w2)) * 2.2 * k * r; deg = sin(w2) * 5 * r }
            Move.DRUM -> { lift = abs(sin(2 * w2)) * 1.6 * k * r; deg = sin(w2) * 3 * r; sy = 1 - .03 * abs(sin(2 * w2)) * r }
            Move.WALK -> { lift = abs(sin(2 * w2)) * 1.6 * k * r; deg = sin(2 * w2) * 4.5 * r }
            Move.SPIN -> sx = if (r > 0) cos(w2).let { if (abs(it) < .12) .12 * sign(it + 1e-9) else it } else 1.0
            Move.SWING -> { deg = sin(w2) * 7 * r; pivotTop = true }
            Move.BREATHE -> { val b = sin(w2) * r; sx = 1 + .035 * b; sy = 1 + .05 * b }
        }
        val tr = if (jam || r <= 0) null else trick(t, st, alloy)
        if (tr != null) {
            val e = tr * tr * (3 - 2 * tr)
            lift = max(lift, sin(PI * tr) * 12 * k)
            deg = 360 * e
        }
        if (st.turn > 0) sx *= max(.14, abs(cos(PI * st.turn)))
        if (a.turns && st.dir < 0) sx = -sx
        val x = cx + if (a.step > 0) (st.x * k).toFloat() else 0f
        if (shadow) {
            val sw = max(4.0, a.shw * k * (1 - lift / (20 * k))).toFloat()
            g.oval(x, by.toFloat() + .4f, sw, 2.6f * k, Brush.radialGradient(listOf(SHADOW.copy(alpha = .5f), SHADOW.copy(alpha = 0f)), g.o(x, by + .4f), g.u(sw)))
        }
        val filter = if (sil != 0) Tints.sil(sil) else look.filter
        val top = by - hh
        val left = x - ww / 2
        fun body() {
            // The key goes in at the back; it turns about its shaft, its bow narrowing as it turns edge-on.
            val kc = cos(st.kph)
            val kl = max(11f, hh * .36f)
            val key = Img["scene_key"]
            val kh = kl * key.height / key.width
            val ax = left + ww * a.kx
            val ay = top + hh * a.ky
            g.at(ax, ay, sy = max(.16, abs(kc)).toFloat()) {
                // The key art points its shaft left; mirrored, the bow sits out behind the toy.
                g.at(ax, ay, sx = -1f) {
                    g.image(key, ax - kl * .02f, ay - kh / 2, kl, kh, filter = if (sil != 0) filter else Tints.metal(alloy, back = kc < 0))
                }
            }
            g.image(img, left, top, ww, hh, filter = filter)
        }
        if (jam) {
            // Tipped over: on its side on the board, or a walker on its back.
            g.shift(0, if (a.turns) 0f else (hh - ww) / 2) { g.at(x, by - hh / 2, deg = if (a.turns) 180f else -90f) { body() } }
            return
        }
        val pivotY = when { tr != null -> by - hh / 2; pivotTop -> top; else -> by.toFloat() }
        g.shift(0, -lift) {
            g.at(x, pivotY, deg = deg.toFloat()) {
                g.at(x, by, sx = sx.toFloat(), sy = sy.toFloat()) { body() }
            }
        }
        // Silver and gold toys glint while they run.
        if (alloy >= Rules.SILVER && r > 0 && sil == 0) for (i in 0 until 3) {
            val v = frac(st.ph * 1.3 + i / 3.0)
            if (v > .45) continue
            val seed = floor(st.ph * 1.3 + i / 3.0).toInt() * 7 + i
            val gx = x + ((hash(seed, 1) - .5) * ww * .8).toFloat()
            val gy = by - (hh * (.15 + hash(seed, 2) * .7)).toFloat()
            val c = col(if (alloy >= Rules.GOLD) 0xfff8c8 else WHITE)
            val rr = (1.6 + 1.6 * sin(PI * v / .45)).toFloat() * k
            glint(g, gx, gy, rr, c)
        }
    }

    fun glint(g: G, x: Float, y: Float, r: Float, c: Color) {
        val t = r * .22f
        g.path(g.pathOf(x to y - r, x + t to y - t, x + r to y, x + t to y + t, x to y + r, x - t to y + t, x - r to y, x - t to y - t), c)
    }

    /** How far a resting toy's key reaches out left of its picture, at its usual size. */
    fun keyReach(t: Toy, painted: Boolean): Float {
        val (ww, hh) = size(t, painted)
        return max(0f, max(11f, hh * .36f) * .98f - ww * get(t).kx)
    }
}
