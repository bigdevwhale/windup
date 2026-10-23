package app.windup

import app.windup.data.Bags
import app.windup.data.DemoSeed
import app.windup.domain.Bag
import app.windup.domain.Capsule
import app.windup.domain.Catalog
import app.windup.domain.Days
import app.windup.domain.Engine
import app.windup.domain.Kit
import app.windup.domain.Place
import app.windup.domain.Rules
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.domain.Wake
import app.windup.notify.Nudge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

class EngineTest {
    private val zone = ZoneId.of("Europe/Prague")
    private val day = LocalDate.of(2026, 10, 23)
    private fun at(h: Int, m: Int = 0) = day.atStartOfDay(zone).toInstant().toEpochMilli() + (h * 60 + m) * 60_000L
    private val chair = Catalog.ex("frog.chair")!!
    private val plank = Catalog.ex("bear.plank")!!

    private fun State.add(e: app.windup.domain.Ex, done: Int, t: Long, bonus: Boolean = false): Pair<State, app.windup.domain.Outcome> {
        val (o, b) = Engine.pin(this, e, done, t, zone, bonus, id = pins.size + 1L)
        return State(pins + o.pin, b) to o
    }

    @Test fun catalogue_is_sound() {
        val keys = Catalog.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        for (t in Toy.entries) assertTrue("$t has movements", Catalog.of(t).size >= if (t.fair) 4 else 5)
        for (e in Catalog.all) {
            assertTrue(e.key, e.target > 0 && e.places.isNotEmpty())
            if (e.ladder.isNotEmpty()) assertTrue(e.key, e.step0 in e.ladder.indices)
            if (e.kit != null) assertTrue(e.key, Place.KIT in e.places || Place.OUTSIDE in e.places)
        }
        // Every place has something for a shelf without kit.
        val s = State(emptyList(), Bag(kit = emptySet(), built = true, bought = Toy.FAIR))
        for (p in Place.entries - Place.KIT) assertTrue("$p", Catalog.all.any { Engine.fits(s, it, p) })
    }

    @Test fun even_pins_pay_and_overwinding_does_not() {
        var s = State.EMPTY
        var o: app.windup.domain.Outcome
        s.add(chair, 10, at(8)).also { s = it.first; o = it.second }
        assertEquals(4, o.pin.gears) // first pin: 1 + full + even
        assertEquals(1, o.pin.springs)
        s.add(plank, 30, at(8, 20)).also { s = it.first; o = it.second }
        assertEquals(2, o.pin.gears) // too soon for the even bonus
        assertEquals(0, o.pin.springs)
        s.add(chair, 4, at(8, 25)).also { s = it.first; o = it.second }
        assertTrue(o.pin.over) // third inside half an hour
        assertEquals(0, o.pin.gears)
        s.add(chair, 10, at(10)).also { s = it.first; o = it.second }
        assertFalse(o.pin.over)
        assertEquals(4, o.pin.gears)
        assertFalse(o.capsule) // the fourth pin; capsules fall on every third
    }

    @Test fun capsules_fall_on_every_third_pin_of_a_day() {
        var s = State.EMPTY
        val caps = (0 until 7).map { i -> s.add(chair, 10, at(8 + i * 2)).also { s = it.first }.second.capsule }
        assertEquals(listOf(false, false, true, false, false, true, false), caps)
        assertEquals(2, s.bag.capsules)
    }

    @Test fun ladders_climb_when_a_set_runs_long() {
        var s = State.EMPTY
        assertEquals(1, s.step(chair))
        val (s2, o) = s.add(chair, 15, at(9))
        assertEquals(2, o.stepUp)
        assertEquals(2, s2.step(chair))
        val (_, o2) = s2.add(chair, 14, at(12))
        assertNull(o2.stepUp)
    }

    @Test fun wind_runs_two_hours_and_rust_comes_after_days() {
        val (s, _) = State.EMPTY.add(chair, 10, at(9))
        assertEquals(1f, Engine.wind(s, Toy.FROG, at(9)), 1e-4f)
        assertEquals(.5f, Engine.wind(s, Toy.FROG, at(10)), 1e-4f)
        assertEquals(0f, Engine.wind(s, Toy.FROG, at(11, 30)), 1e-4f)
        assertEquals(0f, Engine.wind(s, Toy.BEAR, at(9)), 1e-4f)
        val later = at(9) + 10 * Rules.DAY_MS
        assertEquals(.5f, Engine.rust(s, Toy.FROG, later), 1e-3f)
        assertEquals(.5f, Engine.rust(s, Toy.BEAR, later), 1e-3f) // counted from the first day
        assertEquals(0f, Engine.rust(s, Toy.ACRO, later), 1e-3f) // not built yet
    }

    @Test fun suggestions_fit_the_place_and_prefer_stopped_toys() {
        val (s, _) = State.EMPTY.add(chair, 10, at(9))
        val picks = Engine.suggest(s, Place.DESK, at(9, 5), Random(1))
        assertEquals(3, picks.size)
        assertEquals(picks.size, picks.map { it.toy }.toSet().size)
        assertTrue(picks.all { Place.DESK in it.places })
        assertFalse(picks.any { it.toy == Toy.FROG || it.toy == Toy.ACRO })
        // Kit you don't have stays out.
        assertTrue(Engine.suggest(State(emptyList(), Bag(kit = emptySet())), Place.KIT, at(9)).isEmpty())
        assertTrue(Engine.suggest(State(emptyList(), Bag(kit = setOf(Kit.KETTLEBELL))), Place.KIT, at(9)).all { it.kit == Kit.KETTLEBELL })
    }

    @Test fun the_acrobat_is_built_from_parts() {
        val s = State(emptyList(), Bag(bonusGears = 20, bonusSprings = 3))
        assertTrue(Engine.canBuild(s))
        val b = Engine.build(s)
        val s2 = State(emptyList(), b)
        assertTrue(s2.available(Toy.ACRO))
        assertEquals(5, s2.gears)
        assertEquals(0, s2.springs)
        assertFalse(Engine.canBuild(s2))
    }

    @Test fun capsules_open_into_something() {
        var s = State(emptyList(), Bag(capsules = 40))
        val seen = mutableSetOf<String>()
        repeat(40) { i ->
            val (c, b) = Engine.open(s, Random(i))
            seen += c::class.simpleName!!
            if (c is Capsule.Paint) assertTrue(c.toy in b.paints && c.toy in b.painted)
            s = s.copy(bag = b)
        }
        assertEquals(0, s.bag.capsules)
        assertEquals(setOf("Challenge", "Paint", "Parts"), seen)
        val d = Engine.daily(Bag(), day)
        assertEquals(1, d.capsules)
        assertEquals(d, Engine.daily(d, day))
    }

    @Test fun days_read_like_tunes() {
        val even = Days.read(listOf(30, 150, 280, 400, 520, 640, 760, 880), 960)
        assertEquals(100, even.even)
        val cram = Days.read(listOf(660, 663, 667, 671, 676), 960)
        assertTrue(cram.even < 30)
        assertEquals(16, cram.span)
        assertEquals(660, cram.longest)
        assertEquals(0, Days.read(emptyList(), 300).even)
    }

    @Test fun the_bag_survives_the_disk() {
        val b = Bag(start = 5, spentGears = 15, bonusSprings = 2, capsules = 3, dailyDay = 20000, built = true,
            paints = setOf(Toy.FROG, Toy.BIRD), painted = setOf(Toy.BIRD), steps = mapOf("frog.chair" to 3), kit = setOf(Kit.BAR, Kit.ROPE),
            alloys = mapOf(Toy.FROG to 2, Toy.ACRO to 4), records = 5, scoreDay = 20001, paradeDay = 20002, jam = mapOf(Toy.BEAR to (20003L to 2)))
        assertEquals(b, Bags.read(Bags.write(b).associate { it.k to it.v }))
        assertEquals(Bag(), Bags.read(emptyMap()))
    }

    @Test fun reminders_keep_to_the_waking_day() {
        val wake = Wake(7 * 60, 23 * 60)
        val (s, _) = State.EMPTY.add(chair, 10, at(9))
        assertEquals(at(10, 30), Nudge.due(s, wake, 90, at(9, 1), zone))
        assertEquals(at(12), Nudge.due(s, wake, 90, at(9, 1), zone, unanswered = 1))
        assertEquals(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 8 * 3600_000L, Nudge.due(s, wake, 90, at(9, 1), zone, unanswered = 3))
        // A late pin pushes the ask into the next morning.
        val (late, _) = State.EMPTY.add(chair, 10, at(22))
        assertEquals(day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 8 * 3600_000L, Nudge.due(late, wake, 90, at(22, 1), zone))
        assertNull(Nudge.due(s, wake, 0, at(9, 1), zone))
        assertNotNull(Nudge.due(State.EMPTY, wake, 90, at(6), zone))
    }

    @Test fun the_demo_shelf_is_lived_in() {
        val now = at(16)
        val (pins, bag) = DemoSeed.build(now, zone)
        val s = State(pins, bag)
        assertTrue(pins.size > 120)
        assertTrue(bag.built)
        assertTrue(pins.all { it.at < now })
        assertTrue(s.gears >= 0 && s.springs >= 0)
        assertTrue(pins.any { it.over })
    }

    @Test fun a_score_played_in_its_quarters_wins_a_record() {
        val wake = Wake(7 * 60, 23 * 60)
        val notes = Engine.score(State.EMPTY, day, wake)
        assertEquals(4, notes.size)
        assertEquals(4, notes.map { it.toy }.toSet().size)
        assertEquals(notes, Engine.score(State.EMPTY, day, wake))
        var s = State.EMPTY
        var last: app.windup.domain.Outcome? = null
        for (n in notes) {
            val e = Catalog.of(n.toy).first()
            val m = wake.from + (n.from + n.to) / 2
            s.add(e, e.target, at(m / 60, m % 60)).also { s = it.first; last = it.second }
        }
        // The add helper uses the default waking day, the same as here.
        assertEquals(0, last!!.record)
        assertEquals(1, s.bag.records)
        // Paid once a day.
        val (_, again) = s.add(chair, 10, at(22, 50))
        assertNull(again.record)
    }

    @Test fun every_toy_in_a_day_parades() {
        var s = State.EMPTY
        var o: app.windup.domain.Outcome? = null
        s.toys.forEachIndexed { i, t -> s.add(Catalog.of(t).first(), 5, at(7 + i, 40)).also { s = it.first; o = it.second } }
        assertTrue(o!!.parade)
        assertEquals(1, s.bag.capsules - s.pins.size / Rules.CAPSULE_EVERY)
    }

    @Test fun a_jammed_toy_stands_up_after_three_sets_in_a_day() {
        val (s0, _) = State.EMPTY.add(chair, 10, at(9))
        val later = at(9) + 20 * Rules.DAY_MS
        assertTrue(Engine.jammed(s0, Toy.FROG, later))
        var s = s0
        val reps = (0 until 3).map { i -> s.add(chair, 10, later + i * 3 * 3600_000L).also { s = it.first }.second }
        assertEquals(listOf(1, 2, 3), reps.map { it.repair })
        assertTrue(reps.last().freed)
        assertFalse(Engine.jammed(s, Toy.FROG, later + 10 * 3600_000L))
    }

    @Test fun metals_need_parts_and_practice() {
        val rich = Bag(bonusGears = 2000, bonusSprings = 100)
        assertFalse(Engine.canAlloy(State(emptyList(), rich), Toy.FROG)) // no sets with it yet
        var s = State.EMPTY
        repeat(10) { i -> s.add(chair, 10, at(7, 0) + i * 3 * 3600_000L).also { s = it.first } }
        s = s.copy(bag = s.bag.copy(bonusGears = 2000, bonusSprings = 100))
        assertTrue(Engine.canAlloy(s, Toy.FROG))
        val g = s.gears
        s = s.copy(bag = Engine.alloy(s, Toy.FROG))
        assertEquals(1, s.alloy(Toy.FROG))
        assertEquals(g - Rules.ALLOY_GEARS[0], s.gears)
        assertFalse(Engine.canAlloy(s, Toy.FROG)) // copper needs 30 sets
    }

    @Test fun the_fair_opens_on_weekends_with_two_toys_in_turn() {
        val sat = LocalDate.of(2026, 10, 24)
        assertTrue(Engine.fairOpen(sat) && Engine.fairOpen(sat.plusDays(1)))
        assertFalse(Engine.fairOpen(day))
        assertEquals(Engine.fairToys(sat), Engine.fairToys(sat.plusDays(1)))
        assertEquals(Engine.fairToys(sat), Engine.fairToys(day)) // a weekday shows the coming pair
        val seen = (0 until 3).map { Engine.fairToys(sat.plusWeeks(it.toLong())) }
        assertEquals(6, seen.flatMap { listOf(it.first, it.second) }.toSet().size)
        assertEquals(Engine.fairToys(sat), Engine.fairToys(sat.plusWeeks(3)))
    }

    @Test fun fair_toys_are_bought_and_join_the_shelf() {
        val sat = LocalDate.of(2026, 10, 24)
        val (a, b) = Engine.fairToys(sat)
        var s = State(emptyList(), Bag(bonusGears = 1000, bonusSprings = 50))
        assertFalse(s.available(a))
        assertFalse(Engine.canBuyFair(s, a, day)) // closed on a weekday
        val other = Toy.FAIR.first { it != a && it != b }
        assertFalse(Engine.canBuyFair(s, other, sat)) // not on the stall
        s = s.copy(bag = Engine.buyFair(s, b, sat))
        s = s.copy(bag = Engine.buyFair(s, a, sat))
        assertTrue(s.available(a) && s.available(b))
        assertEquals(Toy.CORE + listOf(b, a), s.shelf)
        assertFalse(Engine.canBuyFair(s, a, sat)) // once only
        assertEquals(1000 - Rules.FAIR_PRICE.getValue(a).first - Rules.FAIR_PRICE.getValue(b).first, s.gears)
        assertEquals(s.bag, Bags.read(Bags.write(s.bag).associate { it.k to it.v }))
    }
}
