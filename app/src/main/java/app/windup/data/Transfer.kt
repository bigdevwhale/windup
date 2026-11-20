package app.windup.data

import app.windup.domain.Bag
import app.windup.domain.Pin
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.domain.Wake
import org.json.JSONArray
import org.json.JSONObject

/**
 * The whole shelf in one file, to carry to another phone by hand: every pin, the bag, and the settings that
 * belong to the person rather than the phone. There is no server, so a file the person keeps is the way across.
 */
object Transfer {
    const val VERSION = 1
    private const val APP = "windup"

    /** What a file holds once read: settings are only the parts worth carrying. */
    class Snapshot(val pins: List<Pin>, val bag: Bag, val wake: Wake?, val nudge: Int?, val sound: Boolean?, val look: Int?, val at: Long) {
        fun applyTo(s: Settings) = s.copy(
            onboarded = true,
            wake = wake ?: s.wake,
            nudge = nudge ?: s.nudge,
            sound = sound ?: s.sound,
            look = look ?: s.look,
        )
    }

    class BadFile(message: String) : Exception(message)

    fun write(s: State, settings: Settings, at: Long = System.currentTimeMillis()): String {
        val pins = JSONArray()
        for (p in s.pins) pins.put(
            JSONArray().put(p.at).put(p.toy.name).put(p.ex).put(p.done).put(p.target).put(p.gears).put(p.springs).put(if (p.over) 1 else 0),
        )
        val bag = JSONObject()
        for (row in Bags.write(s.bag)) bag.put(row.k, row.v)
        val set = JSONObject()
            .put("wakeFrom", settings.wake.from).put("wakeTo", settings.wake.to)
            .put("nudge", settings.nudge).put("sound", settings.sound).put("look", settings.look)
        return JSONObject()
            .put("app", APP).put("version", VERSION).put("at", at)
            .put("pins", pins).put("bag", bag).put("settings", set)
            .toString()
    }

    /** Reads a file written by [write]; throws [BadFile] for anything else. */
    fun read(text: String): Snapshot {
        val o = try { JSONObject(text) } catch (e: Exception) { throw BadFile("not json") }
        if (o.optString("app") != APP) throw BadFile("not a windup file")
        val v = o.optInt("version", -1)
        if (v < 1) throw BadFile("no version")
        if (v > VERSION) throw BadFile("newer version $v")
        val arr = o.optJSONArray("pins") ?: JSONArray()
        val pins = ArrayList<Pin>(arr.length())
        for (i in 0 until arr.length()) {
            val a = arr.optJSONArray(i) ?: continue
            // A toy this version doesn't know (from a newer app) is left behind rather than failing the whole file.
            val toy = Toy.entries.firstOrNull { it.name == a.optString(1) } ?: continue
            pins += Pin(0, a.optLong(0), toy, a.optString(2), a.optInt(3), a.optInt(4), a.optInt(5), a.optInt(6), a.optInt(7) == 1)
        }
        pins.sortBy { it.at }
        val b = o.optJSONObject("bag") ?: JSONObject()
        val bag = Bags.read(b.keys().asSequence().associateWith { b.optString(it) })
        val s = o.optJSONObject("settings")
        fun int(k: String) = s?.takeIf { it.has(k) }?.optInt(k)
        val from = int("wakeFrom")
        val to = int("wakeTo")
        return Snapshot(
            pins, bag,
            wake = if (from != null && to != null) Wake(from, to) else null,
            nudge = int("nudge"),
            sound = s?.takeIf { it.has("sound") }?.optBoolean("sound"),
            look = int("look")?.coerceIn(0, 2),
            at = o.optLong("at"),
        )
    }
}
