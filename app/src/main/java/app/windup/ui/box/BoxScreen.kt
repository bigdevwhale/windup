package app.windup.ui.box

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.domain.Days
import app.windup.domain.Rules
import app.windup.domain.Toy
import app.windup.domain.Tunes
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import app.windup.ui.shelf.ScoreSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.windup.domain.date
import app.windup.domain.minuteOfDay
import app.windup.ui.Home
import app.windup.ui.art.DrumPin
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.components.Btn
import app.windup.ui.components.Kind
import app.windup.ui.components.Panel
import app.windup.ui.components.TopBar
import app.windup.ui.shelf.elapsed
import app.windup.ui.shelf.run
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** A playback of a day: seconds in, the last minute sounded, each tooth's last pluck, each toy's wake-up. */
private class Play {
    var t = 0.0
    var last = -1.0
    val plucked = LongArray(Toy.entries.size)
    val woke = DoubleArray(Toy.entries.size) { -1.0 }
}

/**
 * The music box: any day's drum plays as a tune, an hour a second, and the toys wound that day march across
 * the table as their notes sound.
 */
@Composable
fun BoxScreen(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val today = h.nowMs.date(h.zone)
    val days = remember(h.s.pins.size, today) { Days.of(h.s, h.zone).filter { it.date != today }.take(14) }
    var sel by remember { mutableIntStateOf(-1) }
    var play by remember { mutableStateOf<Play?>(null) }
    val march = remember { Toy.entries.associateWith { St() } }
    val len = h.settings.wake.len
    val raw = if (sel < 0) h.s.pins.filter { it.at.date(h.zone) == today } else days.getOrNull(sel)?.pins.orEmpty()
    val pins = raw.map { DrumPin((it.at.minuteOfDay(h.zone) - h.settings.wake.from).coerceIn(0, len), it.toy, it.over) }
    val reading = Days.read(pins.map { it.min }, if (sel < 0) h.elapsed() else len)
    val order = pins.sortedBy { it.min }.map { it.toy }.distinct()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Шкатулка", "Music box"), w.t("день играет мелодию", "the day plays as a tune"))
        PixelView(Scenes.BW, Scenes.BH) { b, dt, ms ->
            b.copyFrom(Scenes.box)
            val p = play
            // At rest the drum shows the stretch just before now (or the start of a past day).
            var at = if (sel < 0) (h.elapsed() - 150).coerceAtLeast(0).toDouble() else 0.0
            if (p != null) {
                p.t += dt
                at = p.t * 60
                for (pin in pins) if (pin.min > p.last && pin.min <= at) {
                    c.sfx.note(pin.toy.note, .8f, pin.over)
                    p.plucked[pin.toy.ordinal] = System.currentTimeMillis()
                    p.woke[pin.toy.ordinal] = p.t
                }
                p.last = at
                if (at > len + 30) play = null
            }
            Scenes.drum(b, pins, len, at, p?.plucked ?: LongArray(Toy.entries.size), System.currentTimeMillis(), playing = p != null)
            val gap = Scenes.BW / order.size.coerceAtLeast(1).toDouble()
            order.forEachIndexed { k, t ->
                val st = march.getValue(t)
                val woke = p?.woke?.get(t.ordinal) ?: -1.0
                val wind = if (p != null && woke >= 0) (1 - (p.t - woke) / 3).coerceIn(0.0, 1.0).toFloat() else 0f
                val r = run(wind)
                Toys.step(Toys[t], st, dt, r)
                st.x = 0.0; st.dir = 1; st.turn = 0.0
                val drift = if (p != null) (p.t * 7) % Scenes.BW else 0.0
                val cx = (((k + .5) * gap + drift) % Scenes.BW).toInt()
                Toys.drawAt(b, t, Toys.parts(t, h.s.painted(t), 0f, h.s.alloy(t) >= Rules.GOLD), cx, Scenes.TABLE, st, r, alloy = h.s.alloy(t))
            }
            @Suppress("UNUSED_EXPRESSION") ms
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(w.day(today, today), kind = if (sel < 0) Kind.BRASS else Kind.GHOST) { sel = -1; play = null }
            days.forEachIndexed { i, d -> Btn(w.day(d.date, today), kind = if (sel == i) Kind.BRASS else Kind.GHOST) { sel = i; play = null } }
        }
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(w.pins(reading.pins), style = T.Title)
                Text(w.t("ровность ${reading.even}%", "evenness ${reading.even}%"), style = T.Title)
            }
            Text(w.t("самый долгий простой ", "longest still stretch ") + w.hm(reading.longest), style = T.Small, modifier = Modifier.padding(top = 4.dp))
            Text(w.verdict(reading), style = T.Body, modifier = Modifier.padding(top = 8.dp))
        }
        Btn(
            if (play == null) w.t("Сыграть день", "Play the day") else w.t("Остановить", "Stop"),
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), kind = Kind.BRASS, enabled = pins.isNotEmpty(),
        ) {
            play = if (play == null) Play().also { march.values.forEach { s -> s.ph = 0.0 } } else null
        }
        Album(h)
        Text(
            w.t("Каждая игрушка — своя нота. Штифты, разнесённые по дню, звучат мелодией; собранные в кучу — одним аккордом.",
                "Each toy is a note of its own. Pins spread across the day sound like a tune; crammed together they're one chord."),
            style = T.Small.copy(color = C.muted), modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }
}

/** The records won by finished scores, newest first, each playable on the box. */
@Composable
private fun Album(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var playing by remember { mutableIntStateOf(-1) }
    var job by remember { mutableStateOf<Job?>(null) }
    ScoreSheet(h)
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("Пластинки", "Records"), style = T.Title, modifier = Modifier.weight(1f))
            Text(w.t("${h.s.bag.records} из ${Tunes.all.size}", "${h.s.bag.records} of ${Tunes.all.size}"), style = T.Small)
        }
        if (h.s.bag.records == 0) Text(w.t("Сыграй партитуру дня — заведи её четыре игрушки каждую в свою четверть дня — и получишь первую пластинку.",
            "Play today's score — wind its four toys, each in its own quarter of the day — and the first record is yours."), style = T.Small, modifier = Modifier.padding(top = 4.dp))
        for (i in h.s.bag.records - 1 downTo 0) {
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(w.tune(i), style = T.Body, modifier = Modifier.weight(1f))
                Box {
                    Btn(if (playing == i) w.t("стоп", "stop") else w.t("слушать", "listen"), kind = if (playing == i) Kind.TEAL else Kind.GHOST) {
                        job?.cancel()
                        if (playing == i) { playing = -1; return@Btn }
                        playing = i
                        job = scope.launch {
                            val tn = Tunes.all[i % Tunes.all.size]
                            tn.notes.forEachIndexed { k, n -> c.sfx.tone(n); delay(tn.beats[k] * 240L) }
                            delay(400); playing = -1
                        }
                    }
                }
            }
        }
    }
}
