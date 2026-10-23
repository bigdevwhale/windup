package app.windup.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The tin toys. Each one keeps a family of movements and plays one note of the music box. The first nine
 * live on the shelf from the start (the acrobat once built); the rest come from the weekend fair.
 */
enum class Toy(val fem: Boolean, val fair: Boolean = false) {
    FROG(true), MONKEY(true), BEAR(false), BALLET(true), SOLDIER(false), BIRD(true), ROBOT(false), PENGUIN(false), ACRO(false),
    SKIER(false, true), HARE(false, true), DIVER(false, true), MINER(false, true), CLOWN(false, true), SAILOR(false, true);

    /** The acrobat is built from a blueprint in the workshop. */
    val blueprint get() = this == ACRO
    val note get() = ordinal

    companion object {
        val CORE = entries.filter { !it.fair }
        val FAIR = entries.filter { it.fair }
    }
}

/** Where you are right now: the shelf suggests what fits there. */
enum class Place { DESK, KETTLE, QUEUE, TEETH, CALL, OUTSIDE, KIT }

/** Kit someone may have at home; everything else needs only a chair, a wall, stairs and the floor. */
enum class Kit { KETTLEBELL, DUMBBELLS, BAND, BAR, ROPE, BACKPACK }

enum class Unit { REPS, SECS, FLOORS }

/** One movement. A ladder, when there is one, is the same movement made harder step by step. */
data class Ex(
    val key: String,
    val toy: Toy,
    val ru: String,
    val en: String,
    val cueRu: String,
    val cueEn: String,
    val target: Int,
    val unit: Unit,
    val places: Set<Place>,
    val kit: Kit? = null,
    val ladder: List<Pair<String, String>> = emptyList(),
    val step0: Int = 0,
    /** Reps the phone can count from the pocket: squats, jumps, marching. */
    val pocket: Boolean = false,
) {
    val hold get() = unit == Unit.SECS
}

/** A pin hammered into the day's drum: one winding, one short burst of movement. */
data class Pin(
    val id: Long,
    val at: Long,
    val toy: Toy,
    val ex: String,
    val done: Int,
    val target: Int,
    val gears: Int,
    val springs: Int,
    /** A third winding inside half an hour: the spring was overwound, no parts. */
    val over: Boolean,
)

/** The workshop's small state, next to the pins. */
data class Bag(
    val start: Long = 0,
    val spentGears: Int = 0,
    val spentSprings: Int = 0,
    val bonusGears: Int = 0,
    val bonusSprings: Int = 0,
    val capsules: Int = 0,
    /** The epoch day the daily capsule was last given. */
    val dailyDay: Long = -1,
    val built: Boolean = false,
    /** Paint jobs found in capsules, and the toys wearing theirs right now. */
    val paints: Set<Toy> = emptySet(),
    val painted: Set<Toy> = emptySet(),
    /** Ladder step per exercise key, where it moved from the catalogue's start. */
    val steps: Map<String, Int> = emptyMap(),
    val kit: Set<Kit> = setOf(Kit.BACKPACK),
    /** Each toy's metal: 0 tin, then brass, copper, silver, gold. */
    val alloys: Map<Toy, Int> = emptyMap(),
    /** Records won by finished scores; the album fills in order. */
    val records: Int = 0,
    /** The epoch days today's score and parade were last paid for. */
    val scoreDay: Long = -1,
    val paradeDay: Long = -1,
    /** Toys jammed by long neglect, with the day and count of repair sets done towards freeing them. */
    val jam: Map<Toy, Pair<Long, Int>> = emptyMap(),
    /** Fair toys bought, in the order they came home: that's their order on the new shelves. */
    val bought: List<Toy> = emptyList(),
    /** When each fair toy came home: its rust counts from then. */
    val boughtAt: Map<Toy, Long> = emptyMap(),
)

data class State(val pins: List<Pin>, val bag: Bag) {
    val gears get() = pins.sumOf { it.gears } + bag.bonusGears - bag.spentGears
    val springs get() = pins.sumOf { it.springs } + bag.bonusSprings - bag.spentSprings

    fun last(toy: Toy? = null): Pin? = pins.lastOrNull { toy == null || it.toy == toy }
    fun available(t: Toy) = when {
        t.blueprint -> bag.built
        t.fair -> t in bag.bought
        else -> true
    }
    val toys get() = Toy.entries.filter { available(it) }

    /** The shelf from left to right, top to bottom: the nine first, then the fair toys as they came. */
    val shelf get() = Toy.CORE + bag.bought
    fun step(e: Ex) = (bag.steps[e.key] ?: e.step0).coerceIn(0, (e.ladder.size - 1).coerceAtLeast(0))
    fun painted(t: Toy) = t in bag.painted
    fun alloy(t: Toy) = bag.alloys[t] ?: 0

    /** Sets done with a toy, the overwound ones aside: how well the movement is known. */
    fun mastery(t: Toy) = pins.count { it.toy == t && !it.over }

    companion object {
        val EMPTY = State(emptyList(), Bag())
    }
}

/** The waking day: the music box drum spans it, from [from] to [to] minutes after midnight. */
data class Wake(val from: Int = 7 * 60, val to: Int = 23 * 60) {
    val len get() = (to - from).coerceAtLeast(60)
}

fun Long.date(zone: ZoneId): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
fun Long.minuteOfDay(zone: ZoneId): Int = Instant.ofEpochMilli(this).atZone(zone).let { it.hour * 60 + it.minute }
