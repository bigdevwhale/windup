package app.windup.domain

import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.random.Random

/** The numbers of the workshop. */
object Rules {
    /** A full winding keeps a toy going for two hours. */
    const val RUN_MIN = 120

    /** A pin this long after the last one keeps the drum even: two extra gears. */
    const val EVEN_MIN = 45

    /** A finished set this long after the last pin also gives a spring. */
    const val SPRING_MIN = 60

    /** The third winding inside this window overwinds the spring. */
    const val OVER_MIN = 30
    const val OVER_COUNT = 2

    /** Every third pin of a day drops a capsule; one more comes each morning. */
    const val CAPSULE_EVERY = 3

    const val ACRO_GEARS = 15
    const val ACRO_SPRINGS = 3

    /** A paint job bought outright, for a toy whose capsule hasn't turned up. */
    const val PAINT_GEARS = 40
    const val PAINT_SPRINGS = 2

    /** Rust starts after a few idle days and covers a toy by about two and a half weeks. */
    const val RUST_FREE_DAYS = 3.0
    const val RUST_FULL_DAYS = 17.0

    /** The drum is read in two-hour slots: a slot with a pin is an even one. */
    const val SLOT_MIN = 120

    /** Doing half as many again as asked moves a ladder one step up. */
    const val STEP_UP = 1.5

    const val MIN_MS = 60_000L
    const val DAY_MS = 24 * 3600_000L
}

/** What a pin brought. */
data class Outcome(
    val pin: Pin,
    /** Minutes since the previous pin, or null for the very first. */
    val gap: Long?,
    val full: Boolean,
    val stepUp: Int?,
    val capsule: Boolean,
    val bonus: Boolean,
)

sealed interface Capsule {
    data class Challenge(val ex: Ex) : Capsule
    data class Paint(val toy: Toy) : Capsule
    data object Parts : Capsule
}

object Engine {
    /** How much winding a toy has left, 1 right after a set, 0 two hours later. */
    fun wind(s: State, t: Toy, now: Long): Float {
        val last = s.last(t) ?: return 0f
        return (1f - (now - last.at) / (Rules.RUN_MIN * Rules.MIN_MS).toFloat()).coerceIn(0f, 1f)
    }

    /** Rust for a toy left alone: none for three days, all over after seventeen. */
    fun rust(s: State, t: Toy, now: Long): Float {
        if (!s.available(t)) return 0f
        val since = s.last(t)?.at ?: s.bag.start.takeIf { it > 0 } ?: return 0f
        val days = (now - since) / Rules.DAY_MS.toDouble()
        return ((days - Rules.RUST_FREE_DAYS) / (Rules.RUST_FULL_DAYS - Rules.RUST_FREE_DAYS)).coerceIn(0.0, 1.0).toFloat()
    }

    fun fits(s: State, e: Ex, p: Place): Boolean =
        s.available(e.toy) && p in e.places && (e.kit == null || e.kit in s.bag.kit || p == Place.OUTSIDE && e.kit == Kit.BAR)

    /** Three movements for where you are, one per toy, the stopped and rusty toys first. */
    fun suggest(s: State, p: Place, now: Long, rnd: Random = Random.Default): List<Ex> {
        val scored = Catalog.all.filter { fits(s, it, p) }.map { it to (1 - wind(s, it.toy, now)) + rust(s, it.toy, now) * 1.2 + rnd.nextDouble() * .5 }
        return scored.sortedByDescending { it.second }.map { it.first }.distinctBy { it.toy }.take(3)
    }

    /** Hammers a pin for a set of [e] with [done] reps or seconds. Returns what it brought and the new bag. */
    fun pin(s: State, e: Ex, done: Int, now: Long, zone: ZoneId, bonus: Boolean = false, id: Long = 0): Pair<Outcome, Bag> {
        val prev = s.last()
        val gap = prev?.let { (now - it.at) / Rules.MIN_MS }
        val over = s.pins.count { now - it.at < Rules.OVER_MIN * Rules.MIN_MS } >= Rules.OVER_COUNT
        val target = e.target
        val full = done >= target
        var gears = 0
        var springs = 0
        if (!over) {
            gears = 1 + (if (full) 1 else 0) + (if (gap == null || gap >= Rules.EVEN_MIN) 2 else 0)
            if (full && (gap == null || gap >= Rules.SPRING_MIN)) springs = 1
            if (bonus) gears *= 2
        }
        val pin = Pin(id, now, e.toy, e.key, done, target, gears, springs, over)
        var bag = s.bag
        if (bag.start == 0L) bag = bag.copy(start = now)
        val today = now.date(zone)
        val todayCount = s.pins.count { it.at.date(zone) == today } + 1
        val capsule = todayCount % Rules.CAPSULE_EVERY == 0
        if (capsule) bag = bag.copy(capsules = bag.capsules + 1)
        var up: Int? = null
        if (e.ladder.isNotEmpty() && done >= ceil(target * Rules.STEP_UP).toInt()) {
            val st = s.step(e)
            if (st < e.ladder.size - 1) {
                up = st + 1
                bag = bag.copy(steps = bag.steps + (e.key to st + 1))
            }
        }
        return Outcome(pin, gap, full, up, capsule, bonus) to bag
    }

    /** One capsule a day just for coming back. */
    fun daily(b: Bag, today: LocalDate): Bag =
        if (b.dailyDay == today.toEpochDay()) b else b.copy(capsules = b.capsules + 1, dailyDay = today.toEpochDay())

    /** Turns the crank: half the time a challenge for double parts, then a paint job, then a handful of parts. */
    fun open(s: State, rnd: Random = Random.Default): Pair<Capsule, Bag> {
        require(s.bag.capsules > 0)
        val b = s.bag.copy(capsules = s.bag.capsules - 1)
        val r = rnd.nextDouble()
        val unpainted = s.toys.filter { it !in b.paints }
        return when {
            r < .5 -> {
                val pool = Catalog.all.filter { s.available(it.toy) && (it.kit == null || it.kit in b.kit) }
                Capsule.Challenge(pool[rnd.nextInt(pool.size)]) to b
            }
            r < .8 && unpainted.isNotEmpty() -> {
                val t = unpainted[rnd.nextInt(unpainted.size)]
                Capsule.Paint(t) to b.copy(paints = b.paints + t, painted = b.painted + t)
            }
            else -> Capsule.Parts to b.copy(bonusGears = b.bonusGears + 3, bonusSprings = b.bonusSprings + 1)
        }
    }

    fun canBuild(s: State) = !s.bag.built && s.gears >= Rules.ACRO_GEARS && s.springs >= Rules.ACRO_SPRINGS

    fun canPaint(s: State, t: Toy) = s.available(t) && t !in s.bag.paints && s.gears >= Rules.PAINT_GEARS && s.springs >= Rules.PAINT_SPRINGS

    fun buyPaint(s: State, t: Toy): Bag {
        require(canPaint(s, t))
        return s.bag.copy(paints = s.bag.paints + t, painted = s.bag.painted + t, spentGears = s.bag.spentGears + Rules.PAINT_GEARS, spentSprings = s.bag.spentSprings + Rules.PAINT_SPRINGS)
    }

    fun build(s: State): Bag {
        require(canBuild(s))
        return s.bag.copy(built = true, spentGears = s.bag.spentGears + Rules.ACRO_GEARS, spentSprings = s.bag.spentSprings + Rules.ACRO_SPRINGS)
    }
}

/** A day on the drum: its pins, placed by minutes since waking. */
data class Day(val date: LocalDate, val pins: List<Pin>) {
    fun minutes(w: Wake, zone: ZoneId) = pins.map { (it.at.minuteOfDay(zone) - w.from).coerceIn(0, w.len) }
}

data class Reading(val pins: Int, val even: Int, val longest: Int, val span: Int)

object Days {
    fun of(s: State, zone: ZoneId): List<Day> =
        s.pins.groupBy { it.at.date(zone) }.map { (d, p) -> Day(d, p.sortedBy { it.at }) }.sortedByDescending { it.date }

    /**
     * How a day sounds: the share of two-hour slots holding a pin (up to [elapsed] minutes into the waking
     * day), the longest stretch without one and how widely the pins are spread.
     */
    fun read(mins: List<Int>, elapsed: Int): Reading {
        val el = elapsed.coerceAtLeast(1)
        if (mins.isEmpty()) return Reading(0, 0, el, 0)
        val slots = ceil(el / Rules.SLOT_MIN.toDouble()).toInt().coerceAtLeast(1)
        val got = (0 until slots).count { w -> mins.any { it >= w * Rules.SLOT_MIN && it < (w + 1) * Rules.SLOT_MIN } }
        val m = mins.sorted()
        var gap = m.first()
        for (i in 1 until m.size) gap = maxOf(gap, m[i] - m[i - 1])
        gap = maxOf(gap, el - m.last())
        return Reading(m.size, (got * 100 / slots).coerceAtMost(100), gap, m.last() - m.first())
    }
}
