package app.windup

import androidx.test.core.app.ApplicationProvider
import app.windup.data.Repo
import app.windup.data.Settings
import app.windup.data.ShelfDb
import app.windup.data.Transfer
import app.windup.domain.Bag
import app.windup.domain.Kit
import app.windup.domain.Pin
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.domain.Wake
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TransferTest {
    private val bag = Bag(
        start = 1_000, spentGears = 3, capsules = 2, built = true, paints = setOf(Toy.FROG), painted = setOf(Toy.FROG),
        steps = mapOf("squat" to 2), kit = setOf(Kit.BAND), alloys = mapOf(Toy.BEAR to 2), records = 4,
        jam = mapOf(Toy.ROBOT to (20_000L to 1)), bought = listOf(Toy.HARE, Toy.SKIER), boughtAt = mapOf(Toy.HARE to 5L, Toy.SKIER to 6L),
    )
    private val pins = listOf(
        Pin(7, 2_000, Toy.FROG, "squat", 10, 10, 2, 0, false),
        Pin(9, 3_000, Toy.HARE, "jacks", 20, 20, 2, 1, true),
    )
    private val settings = Settings(onboarded = true, wake = Wake(6 * 60, 22 * 60), nudge = 120, sound = false, look = 2)

    @Test fun roundTripsTheWholeShelf() {
        val snap = Transfer.read(Transfer.write(State(pins, bag), settings, at = 42))
        assertEquals(pins.map { it.copy(id = 0) }, snap.pins)
        assertEquals(bag, snap.bag)
        assertEquals(42L, snap.at)
        assertEquals(settings, snap.applyTo(Settings()))
    }

    @Test fun replacesWhatThePhoneHad() = runBlocking {
        val repo = Repo(ShelfDb.memory(ApplicationProvider.getApplicationContext()))
        repo.seed(listOf(Pin(0, 500, Toy.BEAR, "plank", 30, 30, 1, 0, false)), Bag(capsules = 9))
        val snap = Transfer.read(Transfer.write(State(pins, bag), settings))
        repo.replace(snap.pins, snap.bag)
        val s = repo.refresh()
        assertEquals(pins.map { it.copy(id = 0) }, s.pins.map { it.copy(id = 0) })
        assertEquals(bag, s.bag)
    }

    @Test fun refusesOtherFiles() {
        assertThrows(Transfer.BadFile::class.java) { Transfer.read("hello") }
        assertThrows(Transfer.BadFile::class.java) { Transfer.read("""{"app":"other","version":1}""") }
        assertThrows(Transfer.BadFile::class.java) { Transfer.read("""{"app":"windup","version":99}""") }
    }
}
