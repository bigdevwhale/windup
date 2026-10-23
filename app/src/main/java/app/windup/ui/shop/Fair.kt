package app.windup.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.domain.Engine
import app.windup.domain.Rules
import app.windup.domain.Toy
import app.windup.domain.date
import app.windup.ui.Home
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.art.rgb
import app.windup.ui.components.Panel
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

/**
 * The weekend fair: two toys on the stall, a new family of movements each, bought with gears and springs.
 * On weekdays the stall is shut and the coming pair shows as silhouettes.
 */
@Composable
fun FairPanel(h: Home) {
    val w = h.w
    val c = LocalContext.current.container
    val today = h.nowMs.date(h.zone)
    val open = Engine.fairOpen(today)
    val (a, b) = Engine.fairToys(today)
    val sts = remember { Toy.FAIR.associateWith { St() } }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.t("Ярмарка выходного дня", "The weekend fair"), style = T.Title, modifier = Modifier.weight(1f))
        }
        Text(
            if (open) w.t("Открыта до конца воскресенья. Каждая игрушка — новая семья движений и новая полка.",
                "Open till the end of Sunday. Each toy brings a new family of movements and a new place on the shelf.")
            else {
                val days = (today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)).toEpochDay() - today.toEpochDay()).toInt()
                w.t("Закрыта. Откроется в субботу — через ${w.plural(days, "день", "дня", "дней").let { "$days $it" }}. Вот кого привезут:",
                    "Shut. Opens on Saturday — in $days day${if (days == 1) "" else "s"}. Coming this time:")
            },
            style = T.Small,
        )
        PixelView(Scenes.FW, Scenes.FH, Modifier.padding(top = 8.dp)) { buf, dt, _ ->
            buf.copyFrom(Scenes.stall(open))
            for ((i, t) in listOf(a, b).withIndex()) {
                val cx = Scenes.FAIR_SLOT[i]
                val st = sts.getValue(t)
                val sold = t in h.s.bag.bought
                if (open && !sold) {
                    Toys.step(Toys[t], st, dt, 1.0)
                    st.x = 0.0; st.dir = 1; st.turn = 0.0
                    Toys.drawAt(buf, t, Toys.parts(t, false, 0f), cx, Scenes.COUNTER, st, 1.0)
                } else if (sold) {
                    Toys.drawAt(buf, t, Toys.parts(t, h.s.painted(t), 0f), cx, Scenes.COUNTER, st, 0.0, alloy = h.s.alloy(t))
                } else {
                    Toys.drawAt(buf, t, Toys.parts(t, false, 0f), cx, Scenes.COUNTER, st, 0.0, sil = rgb(0x2a4744), shadow = false)
                }
                Scenes.tag(buf, cx, sold)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (t in listOf(a, b)) {
                val (g, sp) = Rules.FAIR_PRICE.getValue(t)
                val sold = t in h.s.bag.bought
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(w.toy(t), style = T.Heading)
                    Text(w.pattern(t), style = T.Small, textAlign = TextAlign.Center)
                    when {
                        sold -> Text(w.t("уже на полке", "on your shelf"), style = T.Body.copy(color = C.teal), modifier = Modifier.padding(top = 6.dp))
                        else -> Column(Modifier.padding(top = 6.dp)) {
                            BuyBtn(if (open) w.t("купить", "buy") else w.t("в субботу", "on Saturday"), g, sp, Engine.canBuyFair(h.s, t, today)) {
                                c.write { repo.buyFair(t) }
                                c.sfx.note(t.note); c.sfx.tone(12); c.sfx.tone(16)
                            }
                        }
                    }
                }
            }
        }
        val all = Toy.FAIR.size
        Text(w.t("Собрано ярмарочных игрушек: ${h.s.bag.bought.size} из $all. Кого не успел — вернутся через пару недель.",
            "Fair toys collected: ${h.s.bag.bought.size} of $all. Missed one? It comes back in a couple of weeks."), style = T.Small, modifier = Modifier.padding(top = 8.dp))
    }
}
