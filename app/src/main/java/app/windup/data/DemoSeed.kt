package app.windup.data

import app.windup.WindupApp
import app.windup.AppContainer
import app.windup.domain.Bag
import app.windup.domain.Catalog
import app.windup.domain.Engine
import app.windup.domain.Kit
import app.windup.domain.Pin
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.domain.date
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * The demo build's shelf: about four weeks of windings made through the real engine — mostly even days,
 * one evening when everything was crammed into twenty minutes, the acrobat built, two paint jobs found.
 */
object DemoSeed {
    suspend fun seedIfEmpty(app: WindupApp, c: AppContainer) {
        if (c.repo.refresh().pins.isNotEmpty()) return
        c.settings.update { it.copy(onboarded = true) }
        val (pins, bag) = build(System.currentTimeMillis(), ZoneId.systemDefault())
        c.repo.seed(pins, bag)
    }

    fun build(now: Long, zone: ZoneId): Pair<List<Pin>, Bag> {
        val rnd = Random(23)
        val today = now.date(zone)
        var s = State(emptyList(), Bag(kit = setOf(Kit.BACKPACK, Kit.KETTLEBELL, Kit.BAND)))
        var id = 1L
        val cram = 9
        for (back in 27 downTo 0) {
            val day = today.minusDays(back.toLong())
            val times: List<Int> = when {
                back == cram -> (0 until 7).map { 18 * 60 + 2 + it * 3 }
                back % 6 == 4 -> listOf(8 * 60 + 40, 13 * 60 + 15, 20 * 60 + 30)
                else -> {
                    val n = 6 + rnd.nextInt(5)
                    (0 until n).map { 7 * 60 + 25 + it * (14 * 60 / n) + rnd.nextInt(35) }
                }
            }
            for (m in times) {
                val at = day.atStartOfDay(zone).toInstant().toEpochMilli() + m * 60_000L
                if (at > now - 20 * 60_000L) break
                val toys = s.toys
                val stalest = toys.sortedBy { t -> s.last(t)?.at ?: 0L }.take(3)
                val t = stalest[rnd.nextInt(stalest.size)]
                val pool = Catalog.of(t).filter { it.kit == null || it.kit in s.bag.kit }
                val e = pool[rnd.nextInt(pool.size)]
                val done = if (rnd.nextDouble() < .82) e.target + (if (rnd.nextDouble() < .25) e.target / 2 + 1 else 0) else (e.target * .6).toInt().coerceAtLeast(1)
                val (out, bag) = Engine.pin(s, e, done, at, zone, id = id++)
                s = State(s.pins + out.pin, bag)
                if (Engine.canBuild(s) && back < 18) s = s.copy(bag = Engine.build(s))
            }
        }
        val b = s.bag.copy(paints = setOf(Toy.FROG, Toy.SOLDIER), painted = setOf(Toy.FROG), capsules = 2, dailyDay = today.toEpochDay())
        return s.pins to b
    }
}
