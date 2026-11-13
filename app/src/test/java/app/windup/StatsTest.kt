package app.windup

import app.windup.domain.Bag
import app.windup.domain.Catalog
import app.windup.domain.Pin
import app.windup.domain.State
import app.windup.domain.Stats
import app.windup.domain.Toy
import app.windup.domain.Wake
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StatsTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val today = LocalDate.of(2026, 11, 13) // a Friday

    private var id = 0L
    private fun pin(daysAgo: Int, hour: Int, toy: Toy = Toy.FROG, over: Boolean = false): Pin {
        val ex = Catalog.of(toy).first()
        val at = today.minusDays(daysAgo.toLong()).atStartOfDay(zone).toInstant().toEpochMilli() + hour * 3_600_000L
        return Pin(++id, at, toy, ex.key, ex.target, ex.target, 2, 0, over)
    }

    @Test fun countsStreaksWeeksAndHours() {
        val pins = listOf(
            pin(0, 9), pin(0, 13, Toy.BEAR), pin(1, 10), pin(2, 10), // today and the two days before
            pin(5, 9), pin(6, 9), pin(7, 9), pin(8, 9), // an older run of four
            pin(0, 13, over = true),
        )
        val s = Stats.of(State(pins, Bag()), today, zone, Wake())
        assertEquals(8, s.pins)
        assertEquals(1, s.over)
        assertEquals(3, s.streak)
        assertEquals(4, s.bestStreak)
        // Monday the 9th to today: days 0..4 ago.
        assertEquals(4, s.thisWeek)
        // Last Monday to last Friday: days 11..7 ago.
        assertEquals(2, s.lastWeek)
        assertEquals(Stats.DAYS, s.days.size)
        assertEquals(2, s.days.last())
        assertEquals(5, s.hours[9])
        assertEquals(Toy.FROG to 7, s.toys.first())
        assertEquals(9, s.peakHour)
        assertEquals(18, s.gears)
    }

    @Test fun streakWaitsForTodayUntilItsOver() {
        val s = Stats.of(State(listOf(pin(1, 9), pin(2, 9)), Bag()), today, zone, Wake())
        assertEquals(2, s.streak)
    }
}
