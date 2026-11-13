package app.windup.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import app.windup.domain.Stats
import app.windup.domain.date
import app.windup.ui.Home
import app.windup.ui.components.Bar
import app.windup.ui.components.Caption
import app.windup.ui.components.Metric
import app.windup.ui.components.MetricIcon
import app.windup.ui.components.PartIcon
import app.windup.ui.components.TopBar
import app.windup.ui.components.GuideCard
import app.windup.ui.components.HelpButton
import app.windup.ui.components.rememberGuide
import app.windup.ui.Guides
import app.windup.ui.components.card
import app.windup.ui.shelf.Icon
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlin.math.roundToInt

/**
 * The statistics: how the days have gone, in figures you can read at a glance and three small charts —
 * the last two weeks, the hours you move in, and which toys you wind.
 */
@Composable
fun StatsScreen(h: Home) {
    val w = h.w
    val today = h.nowMs.date(h.zone)
    val st = remember(h.s.pins.size, today, h.settings.wake) { Stats.of(h.s, today, h.zone, h.settings.wake) }

    Column(Modifier.fillMaxSize()) {
        val guide = Guides.stats(w)
        val gs = rememberGuide(h, guide.key)
        TopBar(w.t("Статистика", "Statistics"), w.t("как идут дни", "how the days are going")) { HelpButton(gs, w.t("Подсказка", "Help")) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 20.dp)) {
            GuideCard(gs, guide, w)
            Caption(w.t("Главное", "At a glance"))
            Grid(
                {
                    Metric({ MetricIcon(Icons.Rounded.LocalFireDepartment) }, w.t("${st.streak} ${w.plural(st.streak, "день", "дня", "дней")}", "${st.streak} day${if (st.streak == 1) "" else "s"}"),
                        w.t("подряд", "in a row"), it, sub = w.t("рекорд — ${st.bestStreak}", "best — ${st.bestStreak}"))
                },
                {
                    val d = st.thisWeek - st.lastWeek
                    Metric({ MetricIcon(Icons.Rounded.TrendingUp) }, "${st.thisWeek}", w.t("штифтов за неделю", "pins this week"), it,
                        sub = when { d > 0 -> w.t("▲ $d к прошлой неделе", "▲ $d vs last week"); d < 0 -> w.t("▼ ${-d} к прошлой неделе", "▼ ${-d} vs last week"); else -> w.t("как на прошлой", "level with last week") },
                        subColor = if (d > 0) C.tealDeep else if (d < 0) C.tin else C.muted)
                },
                {
                    Metric({ MetricIcon(Icons.Rounded.Equalizer) }, "${st.evenness}%", w.t("ровность", "evenness"), it, sub = w.t("в среднем за 2 недели", "2-week average"))
                },
                {
                    Metric({ MetricIcon(Icons.Rounded.PushPin) }, "${st.pins}", w.t("штифтов всего", "pins in all"), it,
                        sub = w.t("дней с движением: ${st.activeDays}", "days you moved: ${st.activeDays}"))
                },
            )

            Caption(w.t("Последние 2 недели", "The last 2 weeks"))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).card(rivets = false).padding(16.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format(if (h.ru) java.util.Locale.forLanguageTag("ru") else java.util.Locale.ENGLISH, "%.1f", st.perActiveDay), style = T.Title.copy(fontFeatureSettings = "tnum"))
                    Text(w.t("  штифта в день, когда двигался", "  pins a day, on days you moved"), style = T.Small, modifier = Modifier.padding(bottom = 3.dp))
                }
                DaysChart(st.days, (Stats.DAYS - 1 downTo 0).map { today.minusDays(it.toLong()).dayOfMonth }, st.perActiveDay, w.t("в среднем", "average"), Modifier.padding(top = 12.dp))
                Text(
                    if (st.bestDay != null) w.t("Лучший день — ${w.day(st.bestDay, today).lowercase()}: ${w.pins(st.bestDayPins)}. Нажми на столбик — покажет число.",
                        "Best day — ${w.day(st.bestDay, today)}: ${w.pins(st.bestDayPins)}. Tap a bar to see its count.") else w.t("Пока пусто — заведи первую игрушку.", "Empty so far — wind your first toy."),
                    style = T.Small, modifier = Modifier.padding(top = 10.dp),
                )
            }

            Caption(w.t("Когда ты двигаешься", "When you move"))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).card().padding(16.dp)) {
                val peak = st.peakHour
                Text(
                    if (peak != null) w.t("Чаще всего — около ${w.clock(peak * 60)}", "Most often around ${w.clock(peak * 60)}") else w.t("Пока нет штифтов", "No pins yet"),
                    style = T.Heading,
                )
                HoursChart(st.hours, h.settings.wake.from / 60, (h.settings.wake.to / 60).coerceAtMost(24), peak, Modifier.padding(top = 12.dp))
            }

            Caption(w.t("Сделано", "Done"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric({ MetricIcon(Icons.Rounded.FitnessCenter) }, "${st.reps}", w.t("повторов", "reps"), Modifier.weight(1f))
                Metric({ MetricIcon(Icons.Rounded.Timer) }, w.hm((st.seconds + 30) / 60), w.t("в удержании", "holding"), Modifier.weight(1f))
                Metric({ MetricIcon(Icons.Rounded.Stairs) }, "${st.floors}", w.t("этажей", "floors"), Modifier.weight(1f))
            }

            Caption(w.t("Игрушки", "Toys"))
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).card().padding(horizontal = 16.dp, vertical = 10.dp)) {
                val max = st.toys.firstOrNull()?.second ?: 1
                if (st.toys.isEmpty()) Text(w.t("Ни одной ещё не заводил.", "None wound yet."), style = T.Small)
                for ((t, n) in st.toys) Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) { h.Icon(t, .7f) }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(w.toy(t), style = T.Body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
                        Bar(n / max.toFloat(), C.brassDeep, Modifier.fillMaxWidth().padding(top = 4.dp), height = 8.dp)
                    }
                    Text("$n", style = T.Heading.copy(fontFeatureSettings = "tnum"))
                }
                val idle = h.s.toys.filter { t -> st.toys.none { it.first == t } }
                if (idle.isNotEmpty()) Text(w.t("Ещё не заводились: ", "Not wound yet: ") + idle.joinToString { w.toy(it) }, style = T.Small, modifier = Modifier.padding(top = 6.dp))
            }

            Caption(w.t("Мастерская", "Workshop"))
            Grid(
                { Metric({ PartIcon(false, 20.dp) }, "${st.gears}", w.t("шестерёнок добыто", "gears earned"), it, sub = w.t("сейчас: ${h.s.gears}", "now: ${h.s.gears}")) },
                { Metric({ PartIcon(true, 20.dp) }, "${st.springs}", w.t("пружин добыто", "springs earned"), it, sub = w.t("сейчас: ${h.s.springs}", "now: ${h.s.springs}")) },
                { Metric({ MetricIcon(Icons.Rounded.Album) }, "${h.s.bag.records}", w.t("пластинок", "records"), it, sub = w.t("сыгранные партитуры", "scores played")) },
                {
                    Metric({ MetricIcon(Icons.Rounded.WarningAmber) }, "${st.over}", w.t("перекрутов", "overwinds"), it,
                        sub = w.t("три завода за полчаса", "three in half an hour"), subColor = if (st.over > 0) C.tin else C.muted)
                },
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Icon(Icons.Rounded.CalendarMonth, null, tint = C.muted, modifier = Modifier.padding(end = 8.dp))
                Text(w.t("Всё считается только на телефоне.", "Everything is counted on the phone only."), style = T.Small)
            }
        }
    }
}

/** Four tiles two by two, each the same size. */
@Composable
private fun Grid(vararg cells: @Composable (Modifier) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.toList().chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { it(Modifier.weight(1f)) } }
        }
    }
}

/**
 * Pins per day, one brass bar a day, today the brightest; a dashed line at the average of the days you moved.
 * Only today's bar and the tapped one carry their number.
 */
@Composable
private fun DaysChart(counts: List<Int>, labels: List<Int>, avg: Double, avgLabel: String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    var picked by remember { mutableIntStateOf(counts.lastIndex) }
    val ink = C.ink; val muted = C.muted; val grid = C.line
    val label = T.Small.copy(fontFeatureSettings = "tnum")
    val value = T.Button.copy(color = ink, fontFeatureSettings = "tnum")
    Canvas(
        modifier.fillMaxWidth().height(170.dp).pointerInput(counts) {
            detectTapGestures { o -> picked = (o.x / (size.width / counts.size)).toInt().coerceIn(0, counts.lastIndex) }
        },
    ) {
        val top = 22.dp.toPx(); val bottom = 20.dp.toPx()
        val ch = size.height - top - bottom
        val max = (counts.maxOrNull() ?: 0).coerceAtLeast(maxOf(4, avg.roundToInt()))
        val slot = size.width / counts.size
        val bw = (slot - 6.dp.toPx()).coerceAtLeast(2f)
        val best = counts.indices.maxByOrNull { counts[it] } ?: -1
        drawLine(grid, Offset(0f, top + ch), Offset(size.width, top + ch), 1.dp.toPx())
        counts.forEachIndexed { i, n ->
            val x = i * slot + (slot - bw) / 2
            val bh = if (n == 0) 2.dp.toPx() else ch * n / max
            val today = i == counts.lastIndex
            val brush = if (n == 0) Brush.verticalGradient(listOf(grid, grid))
            else if (today || i == picked) Brush.verticalGradient(listOf(C.brassHi, C.brass, C.brassDeep), top + ch - bh, top + ch)
            else Brush.verticalGradient(listOf(C.brass.copy(alpha = .55f), C.brassDeep.copy(alpha = .55f)), top + ch - bh, top + ch)
            drawRoundRect(brush, Offset(x, top + ch - bh), Size(bw, bh), CornerRadius(4.dp.toPx(), 4.dp.toPx()))
            if (i % 2 == counts.lastIndex % 2) {
                val l = measurer.measure("${labels[i]}", label.copy(color = if (today) ink else muted))
                drawText(l, topLeft = Offset(i * slot + (slot - l.size.width) / 2, size.height - l.size.height.toFloat()))
            }
            if (i == picked || i == best) {
                val l = measurer.measure("$n", value)
                drawText(l, topLeft = Offset((i * slot + (slot - l.size.width) / 2).coerceIn(0f, size.width - l.size.width), top + ch - bh - l.size.height - 2.dp.toPx()))
            }
        }
        if (avg > 0) {
            val y = top + ch - (ch * avg / max).toFloat()
            drawLine(muted, Offset(0f, y), Offset(size.width, y), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())))
            val l = measurer.measure(avgLabel, label.copy(color = muted))
            drawText(l, topLeft = Offset(0f, y - l.size.height - 1.dp.toPx()))
        }
    }
}

/** Pins by hour across the waking day, in one hue; the busiest hour is named, every third hour labelled. */
@Composable
private fun HoursChart(hours: IntArray, from: Int, to: Int, peak: Int?, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val range = (from until to.coerceAtLeast(from + 1)).toList()
    val grid = C.line; val muted = C.muted
    val label = T.Small.copy(fontFeatureSettings = "tnum")
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val bottom = 20.dp.toPx()
        val ch = size.height - bottom - 4.dp.toPx()
        val max = (range.maxOfOrNull { hours[it % 24] } ?: 0).coerceAtLeast(1)
        val slot = size.width / range.size
        val bw = (slot - 3.dp.toPx()).coerceAtLeast(2f)
        drawLine(grid, Offset(0f, size.height - bottom), Offset(size.width, size.height - bottom), 1.dp.toPx())
        range.forEachIndexed { i, hr ->
            val n = hours[hr % 24]
            val bh = if (n == 0) 2.dp.toPx() else ch * n / max
            val x = i * slot + (slot - bw) / 2
            val y = size.height - bottom - bh
            val c = if (n == 0) grid else if (hr == peak) C.brass else C.brassDeep.copy(alpha = .35f + .65f * n / max)
            drawRoundRect(c, Offset(x, y), Size(bw, bh), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
            if ((hr - from) % 3 == 0) {
                val l = measurer.measure("$hr", label.copy(color = muted))
                drawText(l, topLeft = Offset((i * slot + (slot - l.size.width) / 2).coerceIn(0f, size.width - l.size.width), size.height - l.size.height.toFloat()))
            }
        }
    }
}
