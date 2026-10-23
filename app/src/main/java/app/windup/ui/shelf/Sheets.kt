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
import app.windup.ui.Home
import app.windup.ui.components.Btn
import app.windup.ui.components.Chip
import app.windup.ui.components.Kind
import app.windup.ui.components.Sheet
import app.windup.ui.components.ToyIcon
import app.windup.ui.components.px
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** One movement as a card: the toy, the name, how much, and whether it needs kit you don't have. */
@Composable
fun ExCard(h: Home, e: Ex, note: String? = null, bonus: Boolean = false, onClick: (Ex) -> Unit) {
    val w = h.w
    val lack = e.kit != null && e.kit !in h.s.bag.kit && Place.OUTSIDE !in e.places
    val now = h.nowMs
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).px().clickable(enabled = !lack) { onClick(e) }
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
    Sheet(w.toy(t), onClose, w.t("Закрыть", "Close")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            if (h.s.available(t)) h.Icon(t, 2f) else ToyIcon(t, false, 0f, 2f, sil = 0x7a5a3e)
            Column(Modifier.padding(start = 14.dp)) {
                Text(w.pattern(t), style = T.Heading)
                if (h.s.available(t)) {
                    Text(w.state(t, Engine.wind(h.s, t, h.nowMs), Engine.rust(h.s, t, h.nowMs), h.s.last(t) != null), style = T.Small)
                    Text(w.alloy(h.s.alloy(t)) + w.t(" · подходов ${h.s.mastery(t)}", " · ${h.s.mastery(t)} sets"), style = T.Small)
                    if (Engine.jammed(h.s, t, h.nowMs)) Text(
                        w.t("Заклинило от долгого простоя. Три подхода за день — и ${w.g(t, "она встанет", "он встанет")}.", "Jammed from standing idle. Three sets in a day and it stands up again."),
                        style = T.Small.copy(color = C.tinDeep),
                    )
                }
                else Text(w.t("Ещё не собран — чертёж в мастерской.", "Not built yet — the blueprint is in the workshop."), style = T.Small)
            }
        }
        if (h.s.available(t)) for (e in Catalog.of(t)) ExCard(h, e, onClick = onEx)
    }
}

@Composable
fun WhereSheet(h: Home, onClose: () -> Unit, onEx: (Ex) -> Unit) {
    val w = h.w
    var place by remember { mutableStateOf<Place?>(null) }
    var shuffle by remember { mutableIntStateOf(0) }
    Sheet(w.t("Где ты сейчас?", "Where are you?"), onClose, w.t("Закрыть", "Close")) {
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
