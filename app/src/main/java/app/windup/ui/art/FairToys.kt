package app.windup.ui.art

import app.windup.domain.Toy
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/** The toys of the weekend fair. Same frame and rules as the shelf's first nine. */
private fun rnd(v: Double) = v.roundToInt()
private const val TAU = 2 * PI
private fun frac(v: Double) = v - floor(v)

private val CANVAS = Ramp(0xf0dfb4, 0xc9b07c, 0x8f7750, 0x5a4a30)
private val IRON = Ramp(0xa8a8b0, 0x6e6e78, 0x4a4a54, 0x2a2a32)
private val COAL = Ramp(0x5a5a64, 0x32323a, 0x1c1c22, 0x0c0c10)
private val GLASS = Ramp(0xd8f4f0, 0x8ad8cc, 0x4f9e98, 0x2c625e)
private val WOOD = Ramp(0xe0a868, 0xb07a46, 0x7a4c2a, 0x4a2c16)

/** A pole or a stick: one pixel of colour with an ink edge. */
private fun stick(b: Buf, x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
    val n = max(abs(x1 - x0), abs(y1 - y0)).coerceAtLeast(1)
    val pts = (0..n).map { i -> rnd(x0 + (x1 - x0) * i / n.toDouble()) to rnd(y0 + (y1 - y0) * i / n.toDouble()) }
    for ((x, y) in pts) bp(b, x - 1, y - 1, INK, 3, 3)
    for ((x, y) in pts) bp(b, x, y, c)
}

/** The skier glides along the shelf, poles pushing, legs swinging like skates. */
object Skier : ToyArt(Toy.SKIER, speed = .9, shw = 14, step = 6.0, range = 14.0, turns = true) {
    override val roles = mapOf("jacket" to Pal.red, "cap" to Pal.blue, "pants" to Pal.navy)
    override val paint = mapOf("jacket" to Pal.blue, "cap" to Pal.red)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val jacket = c.getValue("jacket"); val cap = c.getValue("cap"); val pants = c.getValue("pants")
        val head = Buf(10, 10)
        head.ell(5.0, 6.0, 3.6, 3.6, Pal.skin, spec = true)
        head.ell(5.0, 4.6, 4.0, 3.0, cap, spec = true, clip = { _, y -> y <= 5 })
        for (x in 1..8 step 2) head.set(x, 5, Pal.white[1])
        head.ell(2.2, 1.6, 1.7, 1.7, Pal.white, spec = true)
        head.set(7, 6, INK); head.set(7, 8, Pal.pink[1]); head.set(9, 7, Pal.skin[2])
        val torso = Buf(10, 11)
        torso.box(1, 0, 8, 10, jacket, spec = true)
        torso.r(1, 0, 8, 2, Pal.white[1]); torso.r(0, 1, 2, 4, Pal.white[2])
        for (y in 3..8 step 2) torso.set(6, y, jacket[0])
        torso.r(1, 8, 8, 1, jacket[3])
        val leg = Buf(4, 10); leg.box(0, 0, 4, 7, pants); leg.box(0, 7, 4, 3, Pal.black, spec = true)
        val ski = Buf(26, 3)
        ski.r(0, 1, 24, 1, WOOD[1]); ski.r(0, 2, 24, 1, WOOD[2]); ski.r(23, 0, 2, 1, WOOD[1]); ski.set(25, 0, WOOD[0]); ski.r(10, 0, 5, 1, Pal.red[1])
        return mapOf("head" to head, "torso" to torso, "leg" to leg, "ski" to ski)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = frac(st.ph)
        val s = if (run > 0) sin(TAU * u) else 0.0
        val bob = if (run > 0 && abs(s) > .7) 1 else 0
        val dx = rnd(3 * s)
        val jacket = p.c.getValue("jacket")
        // The back leg and pole first.
        val handB = (22 - dx) to (32 - bob)
        stick(b, handB.first, handB.second, handB.first - 6, 45, Pal.silver[2])
        limb(b, 23, 26 - bob, handB.first, handB.second, jacket)
        put(b, p["ski"], 10 - dx, 43); put(b, p["leg"], 21 - dx, 33 - bob)
        put(b, p["ski"], 12 + dx, 43); put(b, p["leg"], 23 + dx, 33 - bob)
        Key.draw(b, 21, 25 - bob, false, st.kph)
        put(b, p["torso"], 20, 23 - bob)
        put(b, p["head"], 21, 14 - bob)
        val handF = (26 + dx) to (32 - bob)
        stick(b, handF.first, handF.second, handF.first - 6, 45, Pal.silver[1])
        limb(b, 25, 26 - bob, handF.first, handF.second, jacket)
        return 0
    }
}

/** The hare does jumping jacks: arms and legs out with a hop, and back. */
object Hare : ToyArt(Toy.HARE, speed = 1.3, shw = 11) {
    override val roles = mapOf("fur" to Pal.white, "vest" to Pal.red)
    override val paint = mapOf("fur" to Pal.brown, "vest" to Pal.teal)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val fur = c.getValue("fur"); val vest = c.getValue("vest")
        val body = Buf(14, 14)
        body.ell(7.0, 7.5, 6.0, 6.5, fur, spec = true)
        body.ell(7.0, 5.5, 6.2, 4.0, vest, spec = true, clip = { _, y -> y <= 7 })
        body.ell(7.0, 9.5, 3.4, 3.8, Pal.white, lift = .2)
        for (y in listOf(3, 6)) body.set(7, y, Pal.gold[1])
        val head = Buf(14, 16)
        head.ell(4.0, 4.8, 1.9, 4.8, fur, spec = true); head.ell(10.0, 4.8, 1.9, 4.8, fur, spec = true)
        head.ell(4.0, 5.2, .8, 3.2, Pal.pink, flat = 1); head.ell(10.0, 5.2, .8, 3.2, Pal.pink, flat = 1)
        head.ell(7.0, 11.0, 5.2, 4.4, fur, spec = true)
        head.set(5, 10, INK); head.set(9, 10, INK); head.set(5, 9, WHITE)
        head.set(7, 12, Pal.pink[2]); head.set(6, 13, INK); head.set(8, 13, INK); head.set(7, 14, WHITE)
        head.set(3, 12, Pal.pink[1]); head.set(11, 12, Pal.pink[1])
        val foot = Buf(6, 3); foot.ell(3.0, 1.5, 3.0, 1.5, fur, lift = -.1)
        return mapOf("body" to body, "head" to head, "foot" to foot)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = frac(st.ph)
        val s = if (run > 0) abs(sin(PI * u)) else 0.0
        val hop = rnd(s * 4)
        val fur = p.c.getValue("fur")
        val lx = 21 - rnd(s * 5)
        limb(b, 21, 36 - hop, lx, 43 - hop, fur); limb(b, 26, 36 - hop, 47 - lx, 43 - hop, fur)
        put(b, p["foot"], lx - 3, 43 - hop); put(b, p["foot"], 47 - lx - 2, 43 - hop)
        Key.draw(b, 28, 31 - hop, true, st.kph)
        put(b, p["body"], 17, 24 - hop)
        val hx = rnd(16 - 3 * s); val hy = rnd(34 - 16 * s) - hop
        limb(b, 19, 27 - hop, hx, hy, fur); limb(b, 28, 27 - hop, 47 - hx, hy, fur)
        put(b, p["head"], 17, 10 - hop)
        return hop
    }
}

/** The diver breathes slowly in a brass helmet; bubbles rise as it breathes out. */
object Diver : ToyArt(Toy.DIVER, speed = .32, shw = 12) {
    override val roles = mapOf("helmet" to Pal.brass, "suit" to CANVAS)
    override val paint = mapOf("helmet" to Pal.silver)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val helmet = c.getValue("helmet"); val suit = c.getValue("suit")
        val h = Buf(18, 17)
        h.ell(9.0, 8.5, 8.5, 8.0, helmet, spec = true)
        h.ell(9.0, 9.0, 5.6, 5.2, helmet, lift = -.35)
        h.ell(9.0, 9.0, 4.4, 4.0, GLASS, spec = true)
        h.set(7, 9, GLASS[3]); h.set(11, 9, GLASS[3]); h.r(8, 11, 3, 1, GLASS[2])
        for ((x, y) in listOf(1 to 8, 16 to 8, 9 to 1, 4 to 3, 14 to 3)) h.set(x, y, helmet[3])
        val body = Buf(18, 16)
        body.box(1, 0, 16, 14, suit, spec = true)
        body.r(1, 0, 16, 2, helmet[1]); body.r(1, 2, 16, 1, helmet[3])
        body.r(1, 10, 16, 2, Pal.black[1])
        body.box(5, 4, 8, 5, suit, lift = .15); for (x in listOf(5, 12)) body.set(x, 5, helmet[1])
        val arm = Buf(5, 12); arm.box(0, 0, 5, 9, suit); arm.ell(2.5, 10.0, 2.5, 2.0, Pal.brown, spec = true)
        val boot = Buf(8, 5); boot.box(0, 0, 8, 4, Pal.black, spec = true); boot.r(0, 4, 8, 1, helmet[2])
        return mapOf("helmet" to h, "body" to body, "arm" to arm, "boot" to boot)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = frac(st.ph)
        val inhale = run > 0 && u < .5
        val bob = if (inhale && sin(TAU * u) > .3) -1 else 0
        put(b, p["boot"], 15, 41); put(b, p["boot"], 25, 41)
        Key.draw(b, 29, 31, true, st.kph)
        put(b, p["body"], 15, 26 + bob)
        put(b, p["arm"], 11, 28 + bob); put(b, p["arm"], 32, 28 + bob)
        put(b, p["helmet"], 15, 10 + bob)
        if (run > 0 && !inhale) for (i in 0 until 3) {
            val v = frac((u - .5) * 2 + i * .33)
            val x = 30 + rnd(sin(v * 9 + i) * 1.5) + i
            val y = 12 - rnd(v * 12)
            val r = if (i == 1) 2 else 1
            bp(b, x - r, y - r, INK, 2 * r + 1, 2 * r + 1); bp(b, x - r + 1, y - r + 1, GLASS[0], 2 * r - 1, 2 * r - 1)
            if (r == 2) bp(b, x - 1, y - 1, WHITE)
        }
        return 0
    }
}

/** The miner pushes a cart of coal along the shelf, the lamp on his helmet lit. */
object Miner : ToyArt(Toy.MINER, speed = .9, shw = 18, step = 5.0, range = 9.0, turns = true) {
    override val roles = mapOf("overalls" to Pal.blue, "helmet" to Pal.orange, "cart" to IRON)
    override val paint = mapOf("cart" to Pal.red, "overalls" to Pal.brown)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val overalls = c.getValue("overalls"); val helmet = c.getValue("helmet"); val cart = c.getValue("cart")
        val head = Buf(10, 10)
        head.ell(5.0, 5.8, 3.5, 3.5, Pal.skin, spec = true)
        head.ell(5.0, 3.8, 4.4, 2.8, helmet, spec = true, clip = { _, y -> y <= 4 })
        head.r(8, 2, 2, 2, Pal.gold[0]); head.set(6, 6, INK); head.r(6, 8, 3, 1, Pal.brown[3]); head.set(1, 6, Pal.skin[2])
        val torso = Buf(9, 11)
        torso.box(0, 0, 9, 10, overalls, spec = true); torso.r(2, 0, 1, 5, Pal.gold[2]); torso.r(6, 0, 1, 5, Pal.gold[2])
        torso.box(1, 0, 7, 3, Pal.red, lift = .1)
        val leg = Buf(4, 10); leg.box(0, 0, 4, 7, overalls, lift = -.1); leg.box(0, 7, 4, 3, Pal.black, spec = true)
        val tub = Buf(19, 12)
        tub.ell(9.5, 3.0, 7.5, 3.0, COAL, spec = true, clip = { _, y -> y <= 3 })
        tub.box(0, 3, 19, 7, cart, spec = true); tub.r(0, 3, 19, 1, cart[0]); tub.r(0, 9, 19, 1, cart[3])
        for (x in listOf(2, 9, 16)) tub.r(x, 4, 1, 5, cart[2])
        val wheel = Buf(6, 6); wheel.ell(3.0, 3.0, 3.0, 3.0, IRON, spec = true)
        return mapOf("head" to head, "torso" to torso, "leg" to leg, "tub" to tub, "wheel" to wheel)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = frac(st.ph)
        val s = if (run > 0) sin(TAU * u) else 0.0
        val lift = if (run > 0) rnd(abs(s)) else 0
        val overalls = p.c.getValue("overalls")
        put(b, p["leg"], 12 - rnd(2 * s), 35); put(b, p["leg"], 15 + rnd(2 * s), 35 - lift)
        Key.draw(b, 13, 25, false, st.kph)
        limb(b, 18, 27, 26, 31, overalls)
        put(b, p["torso"], 12, 25)
        put(b, p["head"], 14, 16 - lift)
        limb(b, 19, 28, 27, 32, overalls)
        put(b, p["tub"], 26, 30)
        // Spokes turn with the distance travelled.
        val a = st.ph * 3
        for (wx in listOf(29, 39)) {
            put(b, p["wheel"], wx - 3, 40)
            val cx = wx; val cy = 43
            bp(b, cx + rnd(cos(a) * 2), cy + rnd(sin(a) * 2), INK); bp(b, cx - rnd(cos(a) * 2), cy - rnd(sin(a) * 2), INK)
            bp(b, cx, cy, Pal.silver[0])
        }
        if (run > 0 && frac(st.ph * 2) < .7) bp(b, 25, 18 - lift, Pal.gold[0], 2, 1)
        return 0
    }
}

/** The clown keeps his balance on a unicycle, arms seesawing, wheel turning. */
object Clown : ToyArt(Toy.CLOWN, speed = 1.0, shw = 9, step = 3.0, range = 8.0) {
    override val roles = mapOf("left" to Pal.red, "right" to Pal.brass, "hair" to Pal.orange)
    override val paint = mapOf("left" to Pal.teal, "right" to Pal.pink, "hair" to Pal.green)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val left = c.getValue("left"); val right = c.getValue("right"); val hair = c.getValue("hair")
        val head = Buf(14, 14)
        head.ell(2.4, 8.0, 2.2, 2.8, hair, spec = true); head.ell(11.6, 8.0, 2.2, 2.8, hair, spec = true)
        head.ell(7.0, 8.5, 4.6, 4.6, Pal.white, spec = true)
        for (j in 0 until 4) head.r(7 - (3 - j) / 2 - 1, j, 2 + (3 - j) / 2 * 2, 1, left[if (j == 0) 0 else 1])
        head.r(4, 4, 7, 1, Pal.gold[1])
        head.ell(7.0, 9.4, 1.4, 1.4, Pal.red, spec = true)
        head.set(5, 7, INK); head.set(9, 7, INK); head.r(5, 11, 5, 1, Pal.red[2]); head.set(4, 10, Pal.red[2]); head.set(10, 10, Pal.red[2])
        val body = Buf(14, 12)
        body.ell(7.0, 6.0, 6.0, 6.0, left, spec = true, clip = { x, _ -> x < 7 })
        body.ell(7.0, 6.0, 6.0, 6.0, right, spec = true, clip = { x, _ -> x >= 7 })
        for (y in listOf(3, 6, 9)) body.set(7, y, WHITE)
        body.r(3, 0, 8, 1, Pal.white[1])
        val wheel = Buf(15, 15)
        wheel.ell(7.5, 7.5, 7.5, 7.5, Pal.black, spec = true); wheel.ell(7.5, 7.5, 5.6, 5.6, Pal.black, erase = true)
        wheel.ell(7.5, 7.5, 1.6, 1.6, Pal.silver, spec = true)
        return mapOf("head" to head, "body" to body, "wheel" to wheel)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val u = frac(st.ph)
        val r = if (run > 0) sin(TAU * u) else 0.0
        val dx = rnd(r * 1.2)
        val a = st.x / 6.0 + (if (run > 0) st.ph * 2 else 0.0)
        val cx = 24; val cy = 38
        put(b, p["wheel"], cx - 7, cy - 7)
        for (k in 0 until 3) {
            val an = a + k * TAU / 3
            b.line(cx, cy, cx + rnd(cos(an) * 5), cy + rnd(sin(an) * 5), Pal.silver[2])
        }
        bp(b, cx - 1, cy - 1, Pal.silver[0], 2, 2)
        stick(b, cx, cy, cx + dx, 30, Pal.silver[1])
        val left = p.c.getValue("left"); val right = p.c.getValue("right")
        val pa = a * 1.0
        limb(b, 21 + dx, 29, cx + rnd(cos(pa) * 3), cy + rnd(sin(pa) * 3), left)
        limb(b, 27 + dx, 29, cx - rnd(cos(pa) * 3), cy - rnd(sin(pa) * 3), right)
        Key.draw(b, 28 + dx, 23, true, st.kph)
        put(b, p["body"], 17 + dx, 18)
        val tilt = rnd(r * 4)
        limb(b, 19 + dx, 21, 11 + dx, 21 + tilt, left); limb(b, 29 + dx, 21, 37 + dx, 21 - tilt, right)
        bp(b, 10 + dx, 20 + tilt, WHITE, 2, 2); bp(b, 37 + dx, 20 - tilt, WHITE, 2, 2)
        put(b, p["head"], 17 + dx, 6)
        return 0
    }
}

/** The sailor turns his arms in big circles, the ribbons of his cap fluttering. */
object Sailor : ToyArt(Toy.SAILOR, speed = .8, shw = 10) {
    override val roles = mapOf("collar" to Pal.navy, "trousers" to Pal.navy, "tie" to Pal.red)
    override val paint = mapOf("collar" to Pal.red, "trousers" to Pal.white, "tie" to Pal.gold)
    override fun build(c: Map<String, Ramp>): Map<String, Buf> {
        val collar = c.getValue("collar"); val trousers = c.getValue("trousers"); val tie = c.getValue("tie")
        val head = Buf(12, 13)
        head.ell(6.0, 8.2, 4.2, 4.2, Pal.skin, spec = true)
        head.ell(6.0, 2.6, 5.6, 2.2, Pal.white, spec = true)
        head.ell(6.0, 1.6, 3.6, 1.4, Pal.white, lift = .2)
        head.r(1, 4, 10, 1, collar[1])
        head.set(4, 8, INK); head.set(8, 8, INK); head.r(5, 10, 3, 1, Pal.red[2]); head.set(3, 9, Pal.pink[1]); head.set(9, 9, Pal.pink[1])
        val torso = Buf(14, 13)
        torso.box(2, 0, 10, 12, Pal.white, spec = true)
        for (y in 3..11 step 2) torso.r(2, y, 10, 1, collar[1])
        torso.box(1, 0, 12, 3, collar, spec = true)
        torso.r(5, 0, 4, 2, Pal.white[1]); torso.r(6, 2, 2, 3, tie[1])
        val pants = Buf(10, 9); pants.box(0, 0, 10, 9, trousers, spec = true); pants.r(5, 3, 1, 6, INK)
        val shoe = Buf(5, 3); shoe.box(0, 0, 5, 3, Pal.black, spec = true)
        return mapOf("head" to head, "torso" to torso, "pants" to pants, "shoe" to shoe)
    }

    override fun draw(b: Buf, p: Parts, st: St, run: Double): Int {
        val a = if (run > 0) st.ph * TAU else PI / 2
        put(b, p["shoe"], 18, 43); put(b, p["shoe"], 25, 43)
        put(b, p["pants"], 19, 34)
        Key.draw(b, 29, 28, true, st.kph)
        val hx = 16 - rnd(cos(a) * 4)
        val hy = 26 - rnd(sin(a) * 7)
        put(b, p["torso"], 17, 22)
        limb(b, 19, 24, hx, hy, Pal.white); bp(b, hx - 1, hy - 1, Pal.skin[1], 2, 2)
        limb(b, 28, 24, 47 - hx, hy, Pal.white); bp(b, 47 - hx - 1, hy - 1, Pal.skin[1], 2, 2)
        put(b, p["head"], 18, 9)
        // Cap ribbons.
        val fl = if (run > 0 && frac(st.ph * 2) < .5) 1 else 0
        val ribbon = p.c.getValue("collar")[1]
        bp(b, 17, 14, ribbon, 1, 3); bp(b, 16 - fl, 16, ribbon, 1, 2)
        return 0
    }
}
