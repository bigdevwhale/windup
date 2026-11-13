package app.windup.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Everything the statistics page shows, worked out at once from the pins. */
data class Stats(
    /** Windings that counted (overwound ones aside), and the overwound ones. */
    val pins: Int,
    val over: Int,
    /** Days with at least one pin, ever. */
    val activeDays: Int,
    /** Days in a row with a pin, up to today (or yesterday, while today is still open), and the longest such run. */
    val streak: Int,
    val bestStreak: Int,
    /** Pins since Monday, and over the same days of last week. */
    val thisWeek: Int,
    val lastWeek: Int,
    /** Pins per day over the last [DAYS] days, oldest first; today last. */
    val days: List<Int>,
    /** Average evenness (0..100) over the finished days with pins among the last [DAYS]. */
    val evenness: Int,
    /** Average pins on a day you moved, over the last [DAYS] days. */
    val perActiveDay: Double,
    /** Pins by hour of the day, 0..23, all time. */
    val hours: IntArray,
    /** Pins per toy, most first; toys never wound are left out. */
    val toys: List<Pair<Toy, Int>>,
    val reps: Int,
    val seconds: Int,
    val floors: Int,
    /** Parts the pins earned, all time. */
    val gears: Int,
    val springs: Int,
    /** The day with the most pins, and how many. */
    val bestDay: LocalDate?,
    val bestDayPins: Int,
) {
    val peakHour get() = hours.indices.maxByOrNull { hours[it] }?.takeIf { hours[it] > 0 }

    companion object {
        const val DAYS = 14

        fun of(s: State, today: LocalDate, zone: ZoneId, wake: Wake): Stats {
            val good = s.pins.filter { !it.over }
            val byDay = good.groupBy { it.at.date(zone) }
            val active = byDay.keys.sorted()

            var best = 0
            var run = 0
            var prev: LocalDate? = null
            for (d in active) {
                run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1
                best = maxOf(best, run)
                prev = d
            }
            var streak = 0
            var d = if (today in byDay) today else today.minusDays(1)
            while (d in byDay) { streak++; d = d.minusDays(1) }

            val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
            fun inRange(from: LocalDate, to: LocalDate) = byDay.filterKeys { !it.isBefore(from) && it.isBefore(to) }.values.sumOf { it.size }

            val window = (DAYS - 1 downTo 0).map { today.minusDays(it.toLong()) }
            val counts = window.map { byDay[it]?.size ?: 0 }
            val readings = window.dropLast(1).mapNotNull { day ->
                byDay[day]?.let { p -> Days.read(p.map { (it.at.minuteOfDay(zone) - wake.from).coerceIn(0, wake.len) }, wake.len).even }
            }
            val activeInWindow = counts.count { it > 0 }

            val hours = IntArray(24)
            for (p in good) hours[p.at.minuteOfDay(zone) / 60]++

            var reps = 0; var secs = 0; var floors = 0
            for (p in good) when (Catalog.ex(p.ex)?.unit) {
                Unit.REPS -> reps += p.done
                Unit.SECS -> secs += p.done
                Unit.FLOORS -> floors += p.done
                null -> Unit
            }
            val top = byDay.maxByOrNull { it.value.size }

            return Stats(
                pins = good.size,
                over = s.pins.size - good.size,
                activeDays = active.size,
                streak = streak,
                bestStreak = best,
                thisWeek = inRange(weekStart, today.plusDays(1)),
                lastWeek = inRange(weekStart.minusDays(7), today.minusDays(6)),
                days = counts,
                evenness = if (readings.isEmpty()) 0 else readings.average().toInt(),
                perActiveDay = if (activeInWindow == 0) 0.0 else counts.sum().toDouble() / activeInWindow,
                hours = hours,
                toys = good.groupingBy { it.toy }.eachCount().toList().sortedByDescending { it.second },
                reps = reps, seconds = secs, floors = floors,
                gears = s.pins.sumOf { it.gears },
                springs = s.pins.sumOf { it.springs },
                bestDay = top?.key,
                bestDayPins = top?.value?.size ?: 0,
            )
        }
    }
}
