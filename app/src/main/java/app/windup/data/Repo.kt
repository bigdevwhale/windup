package app.windup.data

import app.windup.domain.Bag
import app.windup.domain.Capsule
import app.windup.domain.Engine
import app.windup.domain.Ex
import app.windup.domain.Kit
import app.windup.domain.Outcome
import app.windup.domain.Pin
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.domain.Wake
import app.windup.domain.date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import kotlin.random.Random

/** The shelf on disk, and the live picture of it in [state]. */
class Repo(db: ShelfDb, private val clock: () -> Long = System::currentTimeMillis, private val zone: () -> ZoneId = ZoneId::systemDefault) {
    private val dao = db.dao()
    private val lock = Mutex()
    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state

    suspend fun refresh(): State = lock.withLock { load().also { _state.value = it } }

    private suspend fun load(): State {
        val pins = dao.pins().map { Pin(it.id, it.at, Toy.valueOf(it.toy), it.ex, it.done, it.target, it.gears, it.springs, it.over) }
        return State(pins, Bags.read(dao.meta().associate { it.k to it.v }))
    }

    private suspend fun current(): State = _state.value ?: load()

    private suspend fun save(b: Bag) = dao.meta(Bags.write(b))

    /** A set is done: the pin goes into the drum. */
    suspend fun pin(e: Ex, done: Int, bonus: Boolean, wake: Wake = Wake(), at: Long = clock()): Outcome = lock.withLock {
        val s = current()
        val (out, bag) = Engine.pin(s, e, done, at, zone(), bonus, wake = wake)
        val p = out.pin
        val id = dao.pin(PinRow(at = p.at, toy = p.toy.name, ex = p.ex, done = p.done, target = p.target, gears = p.gears, springs = p.springs, over = p.over), Bags.write(bag))
        _state.value = load()
        out.copy(pin = p.copy(id = id))
    }

    /** The morning capsule, given once a day on opening the app. */
    suspend fun daily(at: Long = clock()) = lock.withLock {
        val s = current()
        val b = Engine.daily(s.bag, at.date(zone()))
        if (b != s.bag) { save(b); _state.value = load() }
    }

    suspend fun open(rnd: Random = Random.Default): Capsule = lock.withLock {
        val (c, b) = Engine.open(current(), rnd)
        save(b); _state.value = load(); c
    }

    suspend fun alloy(t: Toy) = lock.withLock {
        val s = current()
        if (Engine.canAlloy(s, t)) { save(Engine.alloy(s, t)); _state.value = load() }
    }

    suspend fun build() = lock.withLock { save(Engine.build(current())); _state.value = load() }

    suspend fun buyPaint(t: Toy) = lock.withLock {
        val s = current()
        if (Engine.canPaint(s, t)) { save(Engine.buyPaint(s, t)); _state.value = load() }
    }

    suspend fun paint(t: Toy, on: Boolean) = lock.withLock {
        val b = current().bag
        if (t !in b.paints) return@withLock
        save(b.copy(painted = if (on) b.painted + t else b.painted - t)); _state.value = load()
    }

    suspend fun kit(k: Kit, on: Boolean) = lock.withLock {
        val b = current().bag
        save(b.copy(kit = if (on) b.kit + k else b.kit - k)); _state.value = load()
    }

    /** Moves a ladder by hand, when the step it suggests is too easy or too hard. */
    suspend fun step(e: Ex, step: Int) = lock.withLock {
        val b = current().bag
        save(b.copy(steps = b.steps + (e.key to step.coerceIn(0, e.ladder.size - 1)))); _state.value = load()
    }

    suspend fun start(at: Long = clock()) = lock.withLock {
        val b = current().bag
        if (b.start == 0L) { save(b.copy(start = at)); _state.value = load() }
    }

    /** For the demo: pins straight in, with the bag they leave behind. */
    suspend fun seed(pins: List<Pin>, bag: Bag) = lock.withLock {
        for (p in pins) dao.insert(PinRow(at = p.at, toy = p.toy.name, ex = p.ex, done = p.done, target = p.target, gears = p.gears, springs = p.springs, over = p.over))
        save(bag); _state.value = load()
    }
}

/** The bag kept as plain key–value rows. */
object Bags {
    fun read(m: Map<String, String>): Bag {
        fun toys(k: String) = m[k].orEmpty().split(',').mapNotNull { n -> Toy.entries.firstOrNull { it.name == n } }.toSet()
        return Bag(
            start = m["start"]?.toLongOrNull() ?: 0,
            spentGears = m["spentGears"]?.toIntOrNull() ?: 0,
            spentSprings = m["spentSprings"]?.toIntOrNull() ?: 0,
            bonusGears = m["bonusGears"]?.toIntOrNull() ?: 0,
            bonusSprings = m["bonusSprings"]?.toIntOrNull() ?: 0,
            capsules = m["capsules"]?.toIntOrNull() ?: 0,
            dailyDay = m["dailyDay"]?.toLongOrNull() ?: -1,
            built = m["built"] == "1",
            paints = toys("paints"),
            painted = toys("painted"),
            steps = m["steps"].orEmpty().split(',').filter { '=' in it }.associate { it.substringBefore('=') to (it.substringAfter('=').toIntOrNull() ?: 0) },
            kit = m["kit"]?.let { v -> v.split(',').mapNotNull { n -> Kit.entries.firstOrNull { it.name == n } }.toSet() } ?: Bag().kit,
            alloys = pairs(m["alloys"]).mapNotNull { (k, v) -> toy(k)?.let { it to (v.toIntOrNull() ?: 0) } }.toMap(),
            records = m["records"]?.toIntOrNull() ?: 0,
            scoreDay = m["scoreDay"]?.toLongOrNull() ?: -1,
            paradeDay = m["paradeDay"]?.toLongOrNull() ?: -1,
            jam = pairs(m["jam"]).mapNotNull { (k, v) ->
                val parts = v.split(':')
                val t = toy(k)
                if (t == null || parts.size != 2) null else t to ((parts[0].toLongOrNull() ?: 0L) to (parts[1].toIntOrNull() ?: 0))
            }.toMap(),
        )
    }

    private fun toy(n: String) = Toy.entries.firstOrNull { it.name == n }
    private fun pairs(v: String?) = v.orEmpty().split(',').filter { '=' in it }.map { it.substringBefore('=') to it.substringAfter('=') }

    fun write(b: Bag): List<MetaRow> = listOf(
        "start" to b.start.toString(), "spentGears" to b.spentGears.toString(), "spentSprings" to b.spentSprings.toString(),
        "bonusGears" to b.bonusGears.toString(), "bonusSprings" to b.bonusSprings.toString(), "capsules" to b.capsules.toString(),
        "dailyDay" to b.dailyDay.toString(), "built" to if (b.built) "1" else "0",
        "paints" to b.paints.joinToString(",") { it.name }, "painted" to b.painted.joinToString(",") { it.name },
        "steps" to b.steps.entries.joinToString(",") { "${it.key}=${it.value}" }, "kit" to b.kit.joinToString(",") { it.name },
        "alloys" to b.alloys.entries.joinToString(",") { "${it.key.name}=${it.value}" }, "records" to b.records.toString(),
        "scoreDay" to b.scoreDay.toString(), "paradeDay" to b.paradeDay.toString(),
        "jam" to b.jam.entries.joinToString(",") { "${it.key.name}=${it.value.first}:${it.value.second}" },
    ).map { MetaRow(it.first, it.second) }
}
