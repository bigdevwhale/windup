package app.windup.ui.box

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.domain.Days
import app.windup.domain.Rules
import app.windup.domain.Toy
import app.windup.domain.Tunes
import app.windup.domain.date
import app.windup.domain.minuteOfDay
import app.windup.ui.Home
import app.windup.ui.art.DrumPin
import app.windup.ui.art.Img
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Stage
import app.windup.ui.art.Toys
import app.windup.ui.art.col
import app.windup.ui.components.Caption
import app.windup.ui.components.DayStrip
import app.windup.ui.components.Metric
import app.windup.ui.components.MetricIcon
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.HourglassBottom
import app.windup.ui.components.Panel
import app.windup.ui.components.TopBar
import app.windup.ui.components.GuideCard
import app.windup.ui.components.HelpButton
import app.windup.ui.components.rememberGuide
import app.windup.ui.Guides
import app.windup.ui.components.brassBrush
import app.windup.ui.components.tinBrush
import app.windup.ui.shelf.ScoreSheet
import app.windup.ui.shelf.elapsed
import app.windup.ui.shelf.run
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A playback of a day: seconds in, the last minute sounded, each tooth's last pluck, each toy's wake-up, the notes in the air. */
private class Play {
    var t = 0.0
    var last = -1.0
    val plucked = LongArray(Toy.entries.size)
    val woke = DoubleArray(Toy.entries.size) { -1.0 }
    val notes = ArrayList<Note>()
}

private class Note(val toy: Toy, val x: Float, val at: Double, val seed: Int)

/**
 * The music box: any day's drum plays as a tune, an hour a second, and the toys wound that day march across
 * the table as their notes sound, each note rising from its toy.
 */
@Composable
fun BoxScreen(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val today = h.nowMs.date(h.zone)
    val dates = remember(today) { (13 downTo 0).map { today.minusDays(it.toLong()) } }
    val byDate = remember(h.s.pins.size, today) { h.s.pins.groupBy { it.at.date(h.zone) } }
    var sel by remember(today) { mutableIntStateOf(dates.lastIndex) }
    val isToday = sel == dates.lastIndex
    var play by remember { mutableStateOf<Play?>(null) }
    val march = remember { Toy.entries.associateWith { St() } }
    val len = h.settings.wake.len
    val raw = byDate[dates[sel]].orEmpty()
    val pins = raw.map { DrumPin((it.at.minuteOfDay(h.zone) - h.settings.wake.from).coerceIn(0, len), it.toy, it.over) }
    val reading = Days.read(pins.map { it.min }, if (isToday) h.elapsed() else len)
    val order = pins.sortedBy { it.min }.map { it.toy }.distinct()
    val xs = remember { FloatArray(Toy.entries.size) }

    Column(Modifier.fillMaxSize()) {
        val guide = Guides.box(w)
        val gs = rememberGuide(h, guide.key)
        TopBar(w.t("Шкатулка", "Music box"), w.t("день играет мелодию", "the day plays as a tune")) { HelpButton(gs, w.t("Подсказка", "Help")) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        GuideCard(gs, guide, w)
        Box {
            Stage(Scenes.BW, Scenes.BH) { b, dt, _ ->
                Scenes.box(b)
                val p = play
                // At rest the drum shows the stretch just before now (or the start of a past day).
                var at = if (isToday) (h.elapsed() - 150).coerceAtLeast(0).toDouble() else 0.0
                if (p != null) {
                    p.t += dt
                    at = p.t * 60
                    for (pin in pins) if (pin.min > p.last && pin.min <= at) {
                        c.sfx.note(pin.toy.note, .8f, pin.over)
                        p.plucked[pin.toy.ordinal] = System.currentTimeMillis()
                        p.woke[pin.toy.ordinal] = p.t
                        p.notes.add(Note(pin.toy, xs[pin.toy.ordinal], p.t, pin.min))
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
                    xs[t.ordinal] = cx.toFloat()
                    val k2 = if (order.size > 5) .8f else .9f
                    Toys.drawAt(b, t, Toys.parts(t, h.s.painted(t), 0f, h.s.alloy(t) >= Rules.GOLD), cx, Scenes.TABLE, st, r, k = k2, alloy = h.s.alloy(t))
                }
                if (p != null) {
                    p.notes.removeAll { p.t - it.at > 1.8 }
                    for (n in p.notes) Scenes.note(b, n.x + 6, Scenes.TABLE - 40f, ((p.t - n.at) / 1.8).toFloat(), Toys[n.toy].color, n.seed)
                }
            }
            PlayButton(play != null, pins.isNotEmpty(), Modifier.align(Alignment.BottomEnd).padding(14.dp)) {
                play = if (play == null) Play().also { march.values.forEach { s -> s.ph = 0.0 } } else null
            }
        }
        DayStrip(dates, dates.map { byDate[it]?.size ?: 0 }, sel, today, w.ru, Modifier.padding(top = 14.dp)) { sel = it; play = null }
        Text(
            w.day(dates[sel], today) + " · " + w.pins(reading.pins),
            style = T.Heading, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric({ MetricIcon(Icons.Rounded.PushPin) }, "${reading.pins}", w.t("штифтов", "pins"), Modifier.weight(1f),
                sub = if (reading.pins > 0) w.t("с ${w.clock(h.settings.wake.from + pins.minOf { it.min })}", "from ${w.clock(h.settings.wake.from + pins.minOf { it.min })}") else w.t("барабан пуст", "empty drum"))
            Metric({ MetricIcon(Icons.Rounded.Equalizer) }, "${reading.even}%", w.t("ровность", "evenness"), Modifier.weight(1f),
                sub = when { reading.even >= 70 -> w.t("ровно", "even"); reading.even >= 40 -> w.t("неплохо", "fair"); else -> w.t("рывками", "in bursts") },
                subColor = if (reading.even >= 70) C.tealDeep else C.muted)
            Metric({ MetricIcon(Icons.Rounded.HourglassBottom) }, w.hm(reading.longest), w.t("простой", "still"), Modifier.weight(1f),
                sub = w.t("самый долгий", "the longest"))
        }
        Text(w.verdict(reading), style = T.Body.copy(color = C.muted), modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        ScoreSheet(h)
        Records(h)
        Text(
            w.t("Каждая игрушка — своя нота. Штифты, разнесённые по дню, звучат мелодией; собранные в кучу — одним аккордом.",
                "Each toy is a note of its own. Pins spread across the day sound like a tune; crammed together they're one chord."),
            style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        )
        }
    }
}

/** The round brass button over the box that plays the day, or stops it. */
@Composable
private fun PlayButton(playing: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(56.dp).shadow(if (enabled) 6.dp else 0.dp, CircleShape).clip(CircleShape)
            .background(if (!enabled) androidx.compose.ui.graphics.SolidColor(C.paper2) else if (playing) tinBrush() else brassBrush())
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(if (playing) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, null, tint = if (!enabled) C.faint else if (playing) C.light else C.onBrass, modifier = Modifier.size(32.dp))
    }
}

private val LABELS = listOf(0xd24a32, 0x3fa098, 0xf0c040, 0x4d84c8, 0xea86a6, 0x6fae46, 0xec7e30)

/** The records won by finished scores, as discs on a shelf, newest first; the one playing spins. */
@Composable
private fun Records(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var playing by remember { mutableIntStateOf(-1) }
    var job by remember { mutableStateOf<Job?>(null) }
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "deg")
    val have = h.s.bag.records
    Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Caption(w.t("Пластинки", "Records"), Modifier.weight(1f))
        Text(w.t("$have из ${Tunes.all.size}", "$have of ${Tunes.all.size}"), style = T.Small)
    }
    if (have == 0) Panel {
        Text(w.t("Сыграй партитуру дня — заведи её четыре игрушки каждую в свою четверть дня — и получишь первую пластинку.",
            "Play today's score — wind its four toys, each in its own quarter of the day — and the first record is yours."), style = T.Small)
    } else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        for (i in have - 1 downTo 0) {
            val on = playing == i
            Column(
                Modifier.width(96.dp).clip(RoundedCornerShape(16.dp)).clickable {
                    job?.cancel()
                    if (on) { playing = -1; return@clickable }
                    playing = i
                    job = scope.launch {
                        val tn = Tunes.all[i % Tunes.all.size]
                        tn.notes.forEachIndexed { k, n -> c.sfx.tone(n); delay(tn.beats[k] * 240L) }
                        delay(400); playing = -1
                    }
                }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(84.dp).rotate(if (on) spin else i * 37f), contentAlignment = Alignment.Center) {
                    Image(Img["ui_record"], null, Modifier.fillMaxSize(), filterQuality = FilterQuality.Medium)
                    Box(Modifier.size(28.dp).clip(CircleShape).background(col(LABELS[i % LABELS.size])), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF1E130C)))
                    }
                }
                Text(w.tune(i), style = T.Small.copy(color = if (on) C.tinDeep else C.ink), maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
            }
        }
        for (i in have until Tunes.all.size.coerceAtMost(have + 2)) Box(Modifier.width(96.dp).padding(4.dp).alpha(.35f), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(C.paper2), contentAlignment = Alignment.Center) { Text("?", style = T.Title.copy(color = C.muted)) }
        }
    }
}
