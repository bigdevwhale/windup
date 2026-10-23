package app.windup.ui.ex

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.windup.domain.Catalog
import app.windup.domain.Engine
import app.windup.domain.Outcome
import app.windup.domain.Rules
import app.windup.domain.minuteOfDay
import app.windup.ui.Home
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.components.Btn
import app.windup.ui.components.Panel
import app.windup.ui.components.Sheet
import app.windup.ui.components.ToyIcon
import app.windup.ui.shelf.Icon
import app.windup.ui.shelf.elapsed
import app.windup.ui.shelf.todayPins
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** The pin is in: where it landed on today's drum and what it brought. */
@Composable
fun ResultSheet(h: Home, out: Outcome, onClose: () -> Unit) {
    val w = h.w
    val p = out.pin
    val t = p.toy
    Sheet(w.t("Штифт в ${w.clock(p.at.minuteOfDay(h.zone))}", "A pin at ${w.clock(p.at.minuteOfDay(h.zone))}"), onClose, w.t("На полку", "To the shelf")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            h.Icon(t, 2f)
            Column(Modifier.padding(start = 14.dp)) {
                Text("${w.toy(t)} ${w.g(t, w.t("заведена", "is wound"), w.t("заведён", "is wound"))}", style = T.Title)
                Text(w.t("на два часа хода", "for two hours of running"), style = T.Small)
            }
        }
        Panel {
            PixelView(Scenes.STRIP_W, Scenes.STRIP_H) { b, _, _ -> Scenes.strip(b, h.todayPins(), h.settings.wake.len, h.elapsed()) }
            val lines = buildList {
                if (p.over) add(w.t("Перекрут! Третий завод за полчаса — пружина звенит, деталей нет. Разнеси по дню, мелодия станет богаче.",
                    "Overwound! A third winding inside half an hour — the spring pings and gives no parts. Spread them out and the tune gets richer."))
                else {
                    val gap = out.gap
                    if (gap == null) add(w.t("Первый штифт на барабане.", "The first pin on the drum."))
                    else if (gap >= Rules.EVEN_MIN) add(w.t("Ровный ход: ${w.hm(gap.toInt())} с прошлого штифта · +2", "Even running: ${w.hm(gap.toInt())} since the last pin · +2"))
                    else add(w.t("Всего ${w.hm(gap.toInt())} с прошлого — бонус за ровный ход с ${Rules.EVEN_MIN} мин.", "Only ${w.hm(gap.toInt())} since the last — the even-running bonus starts at ${Rules.EVEN_MIN} min."))
                    if (!out.full) add(w.t("Неполный подход — тоже штифт, но деталей меньше.", "A short set still counts, just fewer parts."))
                }
            }
            for (l in lines) Text(l, style = T.Body, modifier = Modifier.padding(top = 8.dp))
            Text(
                "+" + w.gears(p.gears) + (if (p.springs > 0) " · +" + w.springs(p.springs) else "") + (if (out.bonus) w.t(" (капсула ×2)", " (capsule ×2)") else ""),
                style = T.Title.copy(color = C.tinDeep), modifier = Modifier.padding(top = 10.dp),
            )
            out.stepUp?.let { s -> Catalog.ex(p.ex)?.let { e -> Text(w.t("Новая ступень: ", "Next step: ") + w.name(e, s), style = T.Heading, modifier = Modifier.padding(top = 8.dp)) } }
            if (out.repair > 0) Text(
                if (out.freed) w.t("${w.toy(t)} снова на ногах — механизм расклинило, +${Rules.REPAIR_GEARS} шестерёнок.", "${w.toy(t)} is back on its feet — the works are free, +${Rules.REPAIR_GEARS} gears.")
                else w.t("Ремонт: ${out.repair} из ${Rules.REPAIR_SETS} подходов сегодня — и ${w.g(t, "она встанет", "он встанет")}.", "Repair: ${out.repair} of ${Rules.REPAIR_SETS} sets today and it stands up again."),
                style = T.Heading, modifier = Modifier.padding(top = 8.dp),
            )
            out.record?.let { r -> Text(w.t("Партитура сыграна! Новая пластинка: «${w.tune(r)}» · +${Rules.SCORE_GEARS} шестерёнок и ${Rules.SCORE_SPRINGS} пружины",
                "The score is played! A new record: «${w.tune(r)}» · +${Rules.SCORE_GEARS} gears and ${Rules.SCORE_SPRINGS} springs"), style = T.Heading.copy(color = C.tinDeep), modifier = Modifier.padding(top = 8.dp)) }
            if (out.parade) Text(w.t("Парад полки! Сегодня заведены все игрушки · капсула и +${Rules.PARADE_GEARS} шестерёнок", "Shelf parade! Every toy wound today · a capsule and +${Rules.PARADE_GEARS} gears"),
                style = T.Heading.copy(color = C.tinDeep), modifier = Modifier.padding(top = 8.dp))
            if (out.capsule) Text(w.t("Каждый третий штифт дня — капсула в мастерской.", "Every third pin of the day drops a capsule in the workshop."), style = T.Sub, modifier = Modifier.padding(top = 8.dp))
        }
        Btn(w.t("На полку", "To the shelf"), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), onClick = onClose)
    }
}
