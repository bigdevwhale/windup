package app.windup.ui.shelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import app.windup.domain.Rules
import app.windup.domain.Toy
import app.windup.domain.date
import app.windup.domain.minuteOfDay
import app.windup.ui.Home
import app.windup.ui.art.DrumPin
import app.windup.ui.art.Stage
import app.windup.ui.art.Scenes
import app.windup.ui.art.Toys
import app.windup.ui.art.rgb
import app.windup.ui.components.Btn
import app.windup.ui.components.GuideCard
import app.windup.ui.components.HelpButton
import app.windup.ui.components.rememberGuide
import app.windup.ui.Guides
import app.windup.ui.components.Caption
import app.windup.ui.components.Coin
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import app.windup.ui.components.ToyIcon
import app.windup.ui.components.TopBar
import app.windup.ui.components.card
import app.windup.ui.theme.C
import app.windup.ui.theme.T

private const val SIL = 0x2a4744L

/** The pins of today as the drum draws them. */
fun Home.todayPins(): List<DrumPin> {
    val today = nowMs.date(zone)
    return s.pins.filter { it.at.date(zone) == today }.map { DrumPin((it.at.minuteOfDay(zone) - settings.wake.from).coerceIn(0, settings.wake.len), it.toy, it.over) }
}

fun Home.elapsed() = (nowMs.minuteOfDay(zone) - settings.wake.from).coerceIn(0, settings.wake.len)

/** A toy as it is on the shelf: paint, rust, metal. */
fun Home.parts(t: Toy, now: Long = nowMs) = Toys.parts(t, s.painted(t), Engine.rust(s, t, now), s.alloy(t) >= Rules.GOLD)

@Composable
fun Home.Icon(t: Toy, k: Float, modifier: Modifier = Modifier) = ToyIcon(t, s.painted(t), Engine.rust(s, t, nowMs), k, modifier, alloy = s.alloy(t))

@Composable
fun ShelfScreen(h: Home, onToy: (Toy) -> Unit, onWind: () -> Unit) {
    val w = h.w
    val today = h.todayPins()
    val reading = Days.read(today.map { it.min }, h.elapsed())
    val paraded = h.s.bag.paradeDay == h.nowMs.date(h.zone).toEpochDay()
    val guide = Guides.shelf(w)
    val gs = rememberGuide(h, guide.key)
    Column(Modifier.fillMaxSize()) {
        val fair = Engine.fairOpen(h.nowMs.date(h.zone))
        TopBar(w.t("Завод", "Windup"), if (fair) w.t("ярмарка открыта — загляни в мастерскую", "the fair is open — see the workshop") else w.t("полка у лампы", "the shelf by the lamp")) {
            HelpButton(gs, w.t("Подсказка", "Help"))
            Text(w.clock(h.nowMs.minuteOfDay(h.zone)), style = T.Clock)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        GuideCard(gs, guide, w)
        val wound = today.map { it.toy }.toSet()
        val slots = h.s.shelf
        val rows = (slots.size + 2) / 3
        Stage(Scenes.SW, Scenes.shelfH(rows), onTap = { x, y ->
            val col = (x / (Scenes.SW / 3f)).toInt().coerceIn(0, 2)
            val row = (0 until rows).firstOrNull { y < Scenes.plank(it) + 4 } ?: (rows - 1)
            slots.getOrNull(row * 3 + col)?.let(onToy)
        }) { b, dt, ms ->
            Scenes.shelf(b, rows)
            if (paraded) Scenes.garland(b, ms)
            val now = System.currentTimeMillis()
            slots.forEachIndexed { i, t ->
                val cx = Scenes.SLOT[i % 3]
                val by = Scenes.plank(i / 3)
                val st = h.toys.getValue(t)
                if (!h.s.available(t)) {
                    Toys.drawAt(b, t, Toys.parts(t, false, 0f), cx, by, st, 0.0, sil = rgb(SIL), shadow = false)
                    Scenes.query(b, cx, by)
                    return@forEachIndexed
                }
                val jam = Engine.jammed(h.s, t, now)
                val wind = if (jam) 0f else Engine.wind(h.s, t, now)
                val run = run(wind)
                Toys.step(Toys[t], st, dt, run)
                Toys.drawAt(b, t, h.parts(t, now), cx, by, st, run, alloy = h.s.alloy(t), jam = jam)
                Scenes.plate(b, cx, by, wind, h.s.alloy(t), today = wound.contains(t))
            }
        }
        Btn(w.t("Завести · где ты сейчас?", "Wind up · where are you?"), Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), big = true, onClick = onWind)
        Text(
            if (paraded) w.t("Парад полки: сегодня заведены все игрушки!", "Shelf parade: every toy has been wound today!")
            else w.t("✓ на табличке — игрушку сегодня уже заводили. Заведи все — будет парад. Нажми на игрушку — её упражнения.",
                "✓ on a plate — that toy was wound today. Wind them all for a parade. Tap a toy for its movements."),
            style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text(w.pins(today.size), style = T.Button.copy(color = C.ink), modifier = Modifier.clip(RoundedCornerShape(50)).background(C.paper2.copy(alpha = .6f)).padding(horizontal = 12.dp, vertical = 6.dp))
            Coin(false, h.s.gears, dark = false)
            Coin(true, h.s.springs, dark = false)
        }
        Row(Modifier.fillMaxWidth().padding(end = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Caption(w.t("Барабан дня", "Today's drum"), Modifier.weight(1f))
            Text(w.t("ровный ход ${reading.even}%", "even running ${reading.even}%"), style = T.Small)
        }
        Stage(Scenes.STRIP_W, Scenes.STRIP_H, Modifier.padding(horizontal = 16.dp)) { b, _, _ ->
            Scenes.strip(b, today, h.settings.wake.len, h.elapsed())
        }
        HourLabels(h.settings.wake.from, h.settings.wake.len)
        ScoreSheet(h)
        Box(Modifier.height(12.dp))
        }
    }
}

/** Today's score: four notes in the quarters of the day, each ticked when its toy is wound in time. */
@Composable
fun ScoreSheet(h: Home) {
    val w = h.w
    val date = h.nowMs.date(h.zone)
    val notes = Engine.score(h.s, date, h.settings.wake)
    val played = Engine.scored(h.s, date, h.settings.wake, h.zone)
    val done = h.s.bag.scoreDay == date.toEpochDay()
    val now = h.elapsed()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).card(if (done) C.glow else C.surface, rivets = true).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("Партитура дня", "Today's score"), style = T.Heading, modifier = Modifier.weight(1f))
            Text(
                if (done) w.t("сыграна — пластинка в шкатулке", "played — a record in the box")
                else w.t("${played.count { it }} из ${notes.size} нот", "${played.count { it }} of ${notes.size} notes"),
                style = T.Small,
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            notes.forEachIndexed { i, n ->
                val past = now > n.to && !played[i]
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.height(54.dp), contentAlignment = Alignment.BottomCenter) { h.Icon(n.toy, 1f) }
                    Text(w.clock(h.settings.wake.from + n.from).substringBefore(':') + "–" + w.clock(h.settings.wake.from + n.to).substringBefore(':'), style = T.Small)
                    Text(
                        when { played[i] -> "✓"; past -> "·"; now in n.from..n.to -> w.t("сейчас", "now"); else -> "" },
                        style = T.Body.copy(color = when { played[i] -> C.tinDeep; now in n.from..n.to -> C.teal; else -> C.faint }),
                    )
                }
            }
        }
    }
}

/** A spring gives out evenly, and only at the very end does the toy slow down. */
fun run(wind: Float): Double = when {
    wind <= 0f -> 0.0
    wind > .18f -> 1.0
    else -> .25 + .75 * wind / .18
}

/** The hours under the drum, each centred on its long tick. */
@Composable
fun HourLabels(from: Int, len: Int) {
    val step = Scenes.hourStep(len)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(18.dp)) {
        val w = constraints.maxWidth
        for (k in 0..len / 60 step step) {
            val x = (Scenes.stripX(k * 60, len) * w).toInt()
            Text("${(from / 60 + k) % 24}", style = T.Small, modifier = Modifier.layout { m, c ->
                val p = m.measure(c.copy(minWidth = 0))
                layout(p.width, p.height) { p.place((x - p.width / 2).coerceIn(0, w - p.width), 0) }
            })
        }
    }
}
