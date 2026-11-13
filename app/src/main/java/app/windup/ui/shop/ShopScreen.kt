package app.windup.ui.shop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.domain.Capsule
import app.windup.domain.Engine
import app.windup.domain.Ex
import app.windup.domain.Kit
import app.windup.domain.Rules
import app.windup.domain.Toy
import app.windup.domain.date
import app.windup.ui.Home
import app.windup.ui.art.Scenes
import app.windup.ui.art.Stage
import app.windup.ui.components.Bar
import app.windup.ui.components.Btn
import app.windup.ui.components.Caption
import app.windup.ui.components.Chip
import app.windup.ui.components.Coin
import app.windup.ui.components.Flow
import app.windup.ui.components.Kind
import app.windup.ui.components.Panel
import app.windup.ui.components.PartIcon
import app.windup.ui.components.Segments
import app.windup.ui.components.ToyIcon
import app.windup.ui.components.TopBar
import app.windup.ui.components.GuideCard
import app.windup.ui.components.HelpButton
import app.windup.ui.components.rememberGuide
import app.windup.ui.Guides
import app.windup.ui.components.card
import app.windup.ui.components.brassBrush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import app.windup.ui.shelf.ExCard
import app.windup.ui.shelf.Icon
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private const val CRANK_MS = 1100L

/**
 * The workshop, one counter at a time: the capsule machine, the weekend fair, the toys (recasting, paint and the
 * acrobat's blueprint) and the kit at home. The purse and the settings sit in the bar on top.
 */
@Composable
fun ShopScreen(h: Home, onEx: (Ex, Boolean) -> Unit, onSettings: () -> Unit) {
    val w = h.w
    val s = h.s
    val fairOpen = Engine.fairOpen(h.nowMs.date(h.zone))
    var section by rememberSaveable { mutableIntStateOf(if (fairOpen) 1 else 0) }
    val guide = Guides.shop(w)
    val gs = rememberGuide(h, guide.key)

    Column(Modifier.fillMaxSize()) {
        TopBar(w.t("Мастерская", "Workshop")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HelpButton(gs, w.t("Подсказка", "Help"))
                Coin(false, s.gears)
                Coin(true, s.springs)
                IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, w.t("Настройки", "Settings"), tint = C.wallText) }
            }
        }
        Segments(
            listOf(w.t("Капсулы", "Capsules"), w.t("Ярмарка", "Fair"), w.t("Игрушки", "Toys"), w.t("Дома", "Kit")),
            section, Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            badges = listOf(s.bag.capsules, 0, 0, 0),
        ) { section = it }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            GuideCard(gs, guide, w)
            when (section) {
                0 -> Capsules(h, onEx)
                1 -> { Spacer(Modifier.size(6.dp)); FairPanel(h) }
                2 -> ToysSection(h)
                else -> KitSection(h)
            }
        }
    }
}

@Composable
private fun Capsules(h: Home, onEx: (Ex, Boolean) -> Unit) {
    val w = h.w
    val s = h.s
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var cranked by remember { mutableLongStateOf(0L) }
    var got by remember { mutableStateOf<Capsule?>(null) }
    val drop = remember { intArrayOf(0) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).card(radius = 24.dp, rivets = true).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(170.dp)) {
            Stage(Scenes.MW, Scenes.MH) { b, _, ms ->
                val now = System.currentTimeMillis()
                val t = if (cranked > 0) now - cranked else -1L
                val on = t in 0 until CRANK_MS
                val dy = if (t in 500 until CRANK_MS) (40 + (t - 500) / 30).toInt().coerceAtMost(54) else 0
                Scenes.machine(b, if (on) (t / 300.0).coerceAtMost(1.0) else 0.0, if (dy > 0) drop[0] else null, dy, ms)
            }
        }
        Text(w.t("Капсула-сюрприз", "Surprise capsule"), style = T.Title, modifier = Modifier.padding(top = 10.dp))
        Text(
            w.t("Одна каждый день и ещё по одной за каждые ${Rules.CAPSULE_EVERY} штифта.", "One every day and one more for every ${Rules.CAPSULE_EVERY} pins."),
            style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        Btn(
            if (s.bag.capsules > 0) w.t("Крутить ручку · ${s.bag.capsules}", "Turn the crank · ${s.bag.capsules}") else w.t("Капсул пока нет", "No capsules yet"),
            Modifier.padding(top = 10.dp).width(240.dp), kind = Kind.BRASS, enabled = s.bag.capsules > 0 && cranked == 0L,
        ) {
            drop[0] = Random.nextInt(6)
            cranked = System.currentTimeMillis()
            got = null
            scope.launch {
                repeat(4) { c.sfx.click(); delay(140) }
                delay(CRANK_MS - 560)
                got = c.repo.open()
                c.sfx.note(4); delay(120); c.sfx.note(7)
                cranked = 0L
            }
        }
    }
    AnimatedVisibility(got != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
        when (val g = got) {
            is Capsule.Challenge -> Panel(bg = C.glow) {
                Text(w.t("Вызов из капсулы", "A challenge from the capsule"), style = T.Title)
                Text(w.t("Сделай сейчас — детали удвоятся.", "Do it now and the parts are doubled."), style = T.Small)
                Box(Modifier.padding(top = 6.dp)) { ExCard(h, g.ex, bonus = true) { onEx(it, true); got = null } }
            }
            is Capsule.Paint -> Panel(bg = C.glow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToyIcon(g.toy, true, 0f, 1.5f)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(w.t("Раскраска «${w.paint(g.toy)}»", "The «${w.paint(g.toy)}» paint job"), style = T.Title)
                        Text(w.t("${w.toy(g.toy)}: новая раскраска. Переключить — во вкладке «Игрушки».", "${w.toy(g.toy)} has a new coat. Switch it under Toys."), style = T.Small)
                    }
                }
            }
            Capsule.Parts -> Panel(bg = C.glow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PartIcon(false, 36.dp); PartIcon(true, 36.dp)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(w.t("Горсть деталей", "A handful of parts"), style = T.Title)
                        Text(w.t("+3 шестерёнки и пружина.", "+3 gears and a spring."), style = T.Small)
                    }
                }
            }
            null -> Unit
        }
    }
}

@Composable
private fun ToysSection(h: Home) {
    val w = h.w
    val s = h.s
    val c = LocalContext.current.container
    if (!s.bag.built) Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToyIcon(Toy.ACRO, false, 0f, 1.25f, sil = 0x7a5a3e)
            Column(Modifier.padding(start = 12.dp)) {
                Text(w.t("Чертёж: акробат на турнике", "Blueprint: the acrobat on a bar"), style = T.Heading)
                Text(w.t("тяга · собирается из деталей", "pulling · built from parts"), style = T.Small)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PartIcon(false); Bar(s.gears / Rules.ACRO_GEARS.toFloat(), C.brass, Modifier.weight(1f), height = 8.dp)
            Text("${s.gears.coerceAtLeast(0)}/${Rules.ACRO_GEARS}", style = T.Small)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PartIcon(true); Bar(s.springs / Rules.ACRO_SPRINGS.toFloat(), C.teal, Modifier.weight(1f), height = 8.dp)
            Text("${s.springs.coerceAtLeast(0)}/${Rules.ACRO_SPRINGS}", style = T.Small)
        }
        Btn(w.t("Собрать", "Build"), Modifier.fillMaxWidth().padding(top = 12.dp), enabled = Engine.canBuild(s)) {
            c.write { repo.build() }
            c.sfx.note(8)
        }
    }
    Text(
        w.t("Игрушку можно перелить: жесть → латунь → медь → серебро → золото. Нужны детали и подходы именно с ней.",
            "A toy can be recast: tin → brass → copper → silver → gold. It takes parts and sets done with that very toy."),
        style = T.Small, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
    )
    for (t in s.toys) ToyCard(h, t)
}

@Composable
private fun KitSection(h: Home) {
    val w = h.w
    val s = h.s
    val c = LocalContext.current.container
    Panel(Modifier.padding(top = 6.dp)) {
        Text(w.t("Что есть дома", "Kit at home"), style = T.Title)
        Text(w.t("Отметь снаряжение — полка будет предлагать и упражнения с ним. Остальное — без снаряжения: стул, стена, лестница, пол.",
            "Tick the kit you have and the shelf will offer movements with it too. Everything else needs nothing: a chair, a wall, stairs, the floor."), style = T.Small)
        Flow(Modifier.padding(top = 12.dp)) {
            for (k in Kit.entries) {
                val on = k in s.bag.kit
                Chip((if (on) "✓  " else "") + w.kit(k), on) { c.write { repo.kit(k, !on) } }
            }
        }
    }
    Text(w.t("Без рекламы и без интернета. Всё хранится только на телефоне.", "No ads, no internet. Everything stays on the phone."),
        style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
}

@Composable
fun Price(gears: Int, springs: Int, color: Color = C.ink) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        PartIcon(false, 16.dp); Text("$gears", style = T.Small.copy(color = color))
        Box(Modifier.width(4.dp))
        PartIcon(true, 16.dp); Text("$springs", style = T.Small.copy(color = color))
    }
}

/** A button with a label over a price in gears and springs. */
@Composable
fun BuyBtn(label: String, gears: Int, springs: Int, enabled: Boolean, onClick: () -> Unit) {
    val fg = if (enabled) C.onBrass else C.faint
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.shadow(if (enabled) 3.dp else 0.dp, shape).clip(shape)
            .then(if (enabled) Modifier.background(brassBrush()).border(1.dp, Color.White.copy(alpha = .25f), shape) else Modifier.background(C.paper2))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = T.Button.copy(color = fg))
        Price(gears, springs, fg)
    }
}

/** One toy in the workshop: its metal, how well its movements are known, its rust, and what it can become. */
@Composable
private fun ToyCard(h: Home, t: Toy) {
    val c = LocalContext.current.container
    val w = h.w
    val s = h.s
    val a = s.alloy(t)
    val rust = Engine.rust(s, t, h.nowMs)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).card().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) { h.Icon(t, 1.1f) }
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(w.toy(t), style = T.Heading)
                Text(
                    w.alloy(a) + (if (rust > .05f) w.t(" · ржавчина ${(rust * 100).toInt()}%", " · rust ${(rust * 100).toInt()}%") else ""),
                    style = T.Small.copy(color = if (rust > .05f) C.rust else C.muted),
                )
                if (a < Rules.GOLD) {
                    val need = Rules.ALLOY_MASTERY[a]
                    Bar(s.mastery(t) / need.toFloat(), C.teal, Modifier.fillMaxWidth().padding(top = 6.dp), height = 6.dp)
                    Text(w.t("подходов ${s.mastery(t).coerceAtMost(need)} из $need до «${w.alloy(a + 1).lowercase()}»", "sets ${s.mastery(t).coerceAtMost(need)} of $need to ${w.alloy(a + 1).lowercase()}"),
                        style = T.Small, modifier = Modifier.padding(top = 2.dp))
                } else Text(w.t("вершина — переливать больше некуда", "the top — nothing left to recast"), style = T.Small)
            }
        }
        if (a < Rules.GOLD) Text(w.alloy(a + 1) + ": " + w.alloyGift(a + 1), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
            if (t in s.bag.paints) Btn(if (s.painted(t)) w.paint(t) else w.tin(t), kind = if (s.painted(t)) Kind.TEAL else Kind.GHOST) {
                c.write { repo.paint(t, !s.painted(t)) }
            } else BuyBtn(w.t("раскраска", "paint"), Rules.PAINT_GEARS, Rules.PAINT_SPRINGS, Engine.canPaint(s, t)) {
                c.write { repo.buyPaint(t) }
                c.sfx.note(t.note)
            }
            if (a < Rules.GOLD) BuyBtn(w.t("перелить", "recast"), Rules.ALLOY_GEARS[a], Rules.ALLOY_SPRINGS[a], Engine.canAlloy(s, t)) {
                c.write { repo.alloy(t) }
                c.sfx.note(t.note); c.sfx.tone(12)
            }
        }
    }
}

/** A gear or a spring, for prices. */
@Composable
fun PartMark(spring: Boolean, @Suppress("UNUSED_PARAMETER") color: Color, modifier: Modifier = Modifier) = PartIcon(spring, 16.dp, modifier)
