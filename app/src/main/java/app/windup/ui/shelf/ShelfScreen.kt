package app.windup.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.windup.domain.Days
import app.windup.domain.Engine
import app.windup.domain.Toy
import app.windup.domain.date
import app.windup.domain.minuteOfDay
import app.windup.ui.Home
import app.windup.ui.art.DrumPin
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.Toys
import app.windup.ui.art.rgb
import app.windup.ui.components.Btn
import app.windup.ui.components.TopBar
import app.windup.ui.theme.C
import app.windup.ui.theme.T

private const val SIL = 0x2a4744

/** The pins of today as the drum draws them. */
fun Home.todayPins(): List<DrumPin> {
    val today = nowMs.date(zone)
    return s.pins.filter { it.at.date(zone) == today }.map { DrumPin((it.at.minuteOfDay(zone) - settings.wake.from).coerceIn(0, settings.wake.len), it.toy, it.over) }
}

fun Home.elapsed() = (nowMs.minuteOfDay(zone) - settings.wake.from).coerceIn(0, settings.wake.len)

@Composable
fun ShelfScreen(h: Home, onToy: (Toy) -> Unit, onWind: () -> Unit) {
    val w = h.w
    val today = h.todayPins()
    val reading = Days.read(today.map { it.min }, h.elapsed())
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Завод", "Windup"), w.t("полка у лампы", "the shelf by the lamp")) {
            Text(w.clock(h.nowMs.minuteOfDay(h.zone)), style = T.Clock)
        }
        PixelView(Scenes.SW, Scenes.SH, onTap = { x, y ->
            val col = (x / (Scenes.SW / 3f)).toInt().coerceIn(0, 2)
            val row = when { y < Scenes.PLANK[0] + 4 -> 0; y < Scenes.PLANK[1] + 4 -> 1; else -> 2 }
            onToy(Toy.entries[row * 3 + col])
        }) { b, dt, _ ->
            b.copyFrom(Scenes.shelf)
            val now = System.currentTimeMillis()
            for (t in Toy.entries) {
                val cx = Scenes.SLOT[t.ordinal % 3]
                val by = Scenes.PLANK[t.ordinal / 3]
                val st = h.toys.getValue(t)
                if (!h.s.available(t)) {
                    Toys.drawAt(b, t, Toys.parts(t, false, 0f), cx, by, st, 0.0, sil = rgb(SIL.toLong()), shadow = false)
                    Scenes.query(b, cx, by)
                    continue
                }
                val wind = Engine.wind(h.s, t, now)
                val run = run(wind)
                Toys.step(Toys[t], st, dt, run)
                Toys.drawAt(b, t, Toys.parts(t, h.s.painted(t), Engine.rust(h.s, t, now)), cx, by, st, run)
                Scenes.plate(b, cx, by, wind)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("Барабан дня", "Today's drum"), style = T.Title, modifier = Modifier.weight(1f))
            Text(w.t("ровный ход ${reading.even}%", "even running ${reading.even}%"), style = T.Small)
        }
        PixelView(Scenes.STRIP_W, Scenes.STRIP_H, Modifier.padding(horizontal = 6.dp)) { b, _, _ ->
            Scenes.strip(b, today, h.settings.wake.len, h.elapsed())
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            val from = h.settings.wake.from / 60
            val to = h.settings.wake.to / 60
            for (hr in from..to step 2) Text("$hr", style = T.Small)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(w.pins(today.size), style = T.Sub)
            Text(w.gears(h.s.gears), style = T.Sub)
            Text(w.springs(h.s.springs), style = T.Sub)
        }
        Btn(w.t("Завести · где ты сейчас?", "Wind up · where are you?"), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), onClick = onWind)
        Text(
            w.t("Нажми на игрушку — её упражнения. Чем ровнее штифты по дню, тем красивее вечерняя мелодия.",
                "Tap a toy for its movements. The more evenly the pins spread over the day, the finer the evening tune."),
            style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        )
    }
}

/** A spring gives out evenly, and only at the very end does the toy slow down. */
fun run(wind: Float): Double = when {
    wind <= 0f -> 0.0
    wind > .18f -> 1.0
    else -> .25 + .75 * wind / .18
}
