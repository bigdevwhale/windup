package app.windup.ui.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import app.windup.domain.Catalog
import app.windup.domain.Engine
import app.windup.domain.Ex
import app.windup.domain.Place
import app.windup.domain.Toy
import app.windup.domain.date
import app.windup.ui.Home
import app.windup.ui.components.Btn
import app.windup.ui.components.Chip
import app.windup.ui.components.Kind
import app.windup.ui.components.Sheet
import app.windup.ui.components.ToyIcon
import app.windup.ui.components.card
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import app.windup.ui.components.GuideCard
import app.windup.ui.components.HelpButton
import app.windup.ui.components.rememberGuide
import app.windup.ui.Guides
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Stage
import app.windup.ui.art.Toys
import app.windup.ui.components.Caption
import app.windup.ui.components.Bar
import app.windup.ui.components.Panel

/** One movement as a card: the toy, the name, how much, and whether it needs kit you don't have. */
@Composable
fun ExCard(h: Home, e: Ex, note: String? = null, bonus: Boolean = false, onClick: (Ex) -> Unit) {
    val w = h.w
    val lack = e.kit != null && e.kit !in h.s.bag.kit && Place.OUTSIDE !in e.places
    val now = h.nowMs
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).card().clickable(enabled = !lack) { onClick(e) }
            .padding(horizontal = 10.dp, vertical = 8.dp).alpha(if (lack) .5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(60.dp), contentAlignment = Alignment.Center) { h.Icon(e.toy, 1.25f) }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            Text(w.name(e, h.s.step(e)), style = T.Heading)
            Text(
                buildString {
                    append(w.amount(e.target, e.unit)); append(" · "); append(w.toy(e.toy))
                    if (e.kit != null) { append(" · "); append(w.kit(e.kit)) }
                    if (bonus) append(w.t(" · ×2 детали", " · ×2 parts"))
                },
                style = T.Small,
            )
            if (note != null) Text(note, style = T.Small)
        }
        Text(if (lack) w.t("нет: ${w.kit(e.kit!!)}", "no ${w.kit(e.kit!!)}") else "›", style = T.Heading.copy(color = C.tin), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
fun ToySheet(h: Home, t: Toy, onClose: () -> Unit, onEx: (Ex) -> Unit) {
    val w = h.w
    val have = h.s.available(t)
    val wind = if (have) Engine.wind(h.s, t, h.nowMs) else 0f
    val rust = if (have) Engine.rust(h.s, t, h.nowMs) else 0f
    val jam = have && Engine.jammed(h.s, t, h.nowMs)
    val last = h.s.last(t)?.at
    val woundToday = last != null && last.date(h.zone) == h.nowMs.date(h.zone)
    val st = remember(t) { St() }
    val guide = Guides.toy(w)
    val gs = rememberGuide(h, guide.key)
    Sheet(w.toy(t), onClose, w.t("Закрыть", "Close"), help = gs, helpLabel = w.t("Подсказка", "Help")) {
        GuideCard(gs, guide, w)
        // The toy on the bench: running while its spring has some left, lying on its side when it's jammed.
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)).border(1.dp, C.line, RoundedCornerShape(20.dp))) {
            Stage(Scenes.EW, Scenes.EH) { b, dt, _ ->
                Scenes.bench(b)
                val r = if (jam) 0.0 else run(wind)
                if (!have) Toys.drawAt(b, t, Toys.parts(t, false, 0f), Scenes.EW / 2, Scenes.ETOP, st, 0.0, sil = 0x2a4744, shadow = false)
                else {
                    Toys.step(Toys[t], st, dt, r)
                    st.x = 0.0; st.dir = 1; st.turn = 0.0
                    Toys.drawAt(b, t, h.parts(t), Scenes.EW / 2, Scenes.ETOP, st, r, alloy = h.s.alloy(t), jam = jam)
                }
                if (wind > 0) Scenes.gauge(b, wind)
            }
            Text(
                w.pattern(t), style = T.Label.copy(color = C.light, fontSize = 12.sp),
                modifier = Modifier.padding(12.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = .35f)).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
        if (!have) {
            Panel { Text(w.t("Ещё не собран — чертёж в мастерской.", "Not built yet — the blueprint is in the workshop."), style = T.Body) }
            return@Sheet
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).card(rivets = true).padding(horizontal = 18.dp, vertical = 16.dp)) {
            // The headline: what the toy is doing right now, in its own colour.
            val (title, tone, icon) = when {
                jam -> Triple(w.t("Заклинило", "Jammed"), C.tin, Icons.Rounded.Build)
                wind > 0 -> Triple(w.t("Идёт", "Running"), C.tealDeep, Icons.Rounded.PlayArrow)
                woundToday -> Triple(w.g(t, w.t("Остановилась — пора завести", "Stopped — wind it again"), w.t("Остановился — пора завести", "Stopped — wind it again")), C.brassDeep, Icons.Rounded.Pause)
                else -> Triple(w.t("Сегодня не заводили", "Not wound today"), C.tin, Icons.Rounded.Schedule)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(tone.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = tone, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(title, style = T.Title.copy(color = tone))
                    Text(
                        when {
                            jam -> w.t("Три подхода за день — и ${w.g(t, "она встанет", "он встанет")}.", "Three sets in a day and it stands up again.")
                            wind > 0 -> w.t("завода осталось ${(wind * 100).toInt()}%", "${(wind * 100).toInt()}% of the winding left")
                            else -> w.t("выбери упражнение ниже", "pick a movement below")
                        },
                        style = T.Small,
                    )
                }
            }
            if (wind > 0) Bar(wind, C.teal, Modifier.fillMaxWidth().padding(top = 12.dp), height = 8.dp)
            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp).height(1.dp).background(C.line))
            Info(Icons.Rounded.History, w.t("Последний завод", "Last wound"),
                last?.let { w.lastWound(it, h.nowMs, h.zone).replaceFirst(w.t("заводили ", "wound "), "") } ?: w.t("ни разу", "never"),
                if (woundToday) C.tealDeep else C.ink)
            Info(Icons.Rounded.WorkspacePremium, w.t("Металл", "Metal"), w.alloy(h.s.alloy(t)))
            Info(Icons.Rounded.FitnessCenter, w.t("Подходов", "Sets"), "${h.s.mastery(t)}")
            if (rust > .05f) Info(Icons.Rounded.WaterDrop, w.t("Ржавчина", "Rust"), "${(rust * 100).toInt()}%", C.rust)
        }
        Caption(w.t("Упражнения", "Movements"))
        for (e in Catalog.of(t)) ExCard(h, e, onClick = onEx)
    }
}

/** A line of the toy's card: an icon, what it is, and its value set apart on the right. */
@Composable
private fun Info(icon: ImageVector, label: String, value: String, tone: Color = C.ink) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = C.muted, modifier = Modifier.size(18.dp))
        Text(label, style = T.Body.copy(color = C.muted), modifier = Modifier.padding(start = 10.dp).weight(1f))
        Text(value, style = T.Body.copy(color = tone, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun WhereSheet(h: Home, onClose: () -> Unit, onEx: (Ex) -> Unit) {
    val w = h.w
    var place by remember { mutableStateOf<Place?>(null) }
    var shuffle by remember { mutableIntStateOf(0) }
    val guide = Guides.where(w)
    val gs = rememberGuide(h, guide.key)
    Sheet(w.t("Где ты сейчас?", "Where are you?"), onClose, w.t("Закрыть", "Close"), help = gs, helpLabel = w.t("Подсказка", "Help")) {
        GuideCard(gs, guide, w)
        for (row in Place.entries.chunked(2)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                for (p in row) Chip(w.place(p), place == p, Modifier.weight(1f).padding(horizontal = 4.dp), sub = w.placeSub(p)) { place = p }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
        val p = place
        if (p == null) {
            Text(w.t("Выбери, где ты, — и полка подскажет, кого завести. Сначала тех, кто стоит или ржавеет.",
                "Pick where you are and the shelf will suggest who to wind. The stopped and rusty ones first."), style = T.Sub, modifier = Modifier.padding(16.dp))
        } else {
            val picks = remember(p, shuffle, h.s) { Engine.suggest(h.s, p, h.nowMs) }
            Text(w.t("Ключ ждёт у этих:", "The key is waiting for:"), style = T.Sub, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
            if (picks.isEmpty()) Text(w.t("Здесь ничего не подходит — отметь инвентарь в мастерской.", "Nothing fits here — tick your kit in the workshop."), style = T.Sub, modifier = Modifier.padding(16.dp))
            for (e in picks) {
                val wind = Engine.wind(h.s, e.toy, h.nowMs)
                val rust = Engine.rust(h.s, e.toy, h.nowMs)
                val note = w.toy(e.toy) + ": " + (if (wind > 0) w.t("завода ${(wind * 100).toInt()}%", "${(wind * 100).toInt()}% wound") else w.t("стоит", "stopped")) + (if (rust > .3f) w.t(", ржавеет", ", rusting") else "")
                ExCard(h, e, note, onClick = onEx)
            }
            Btn(w.t("Другие варианты", "Something else"), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), kind = Kind.GHOST) { shuffle++ }
        }
    }
}
