package app.windup.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import app.windup.ui.Home
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.rgb
import app.windup.ui.components.Bar
import app.windup.ui.components.Btn
import app.windup.ui.components.Flow
import app.windup.ui.components.Kind
import app.windup.ui.components.Panel
import app.windup.ui.components.ToyIcon
import app.windup.ui.components.TopBar
import app.windup.ui.shelf.ExCard
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.windup.ui.components.px
import app.windup.ui.shelf.Icon

private const val CRANK_MS = 1100L

/** The workshop: the capsule machine, the acrobat's blueprint, the toys' rust and paint, the kit at home. */
@Composable
fun ShopScreen(h: Home, onEx: (Ex, Boolean) -> Unit, onSettings: () -> Unit) {
    val w = h.w
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    var cranked by remember { mutableLongStateOf(0L) }
    var got by remember { mutableStateOf<Capsule?>(null) }
    val drop = remember { intArrayOf(0) }
    val s = h.s

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        TopBar(w.t("Мастерская", "Workshop"), w.t("детали, чертежи, капсулы", "parts, blueprints, capsules")) {
            Column(horizontalAlignment = Alignment.End) {
                Text(w.gears(s.gears), style = T.Body.copy(color = C.brass))
                Text(w.springs(s.springs), style = T.Body.copy(color = C.brass))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(144.dp)) {
                PixelView(Scenes.MW, Scenes.MH) { b, _, ms ->
                    val now = System.currentTimeMillis()
                    val t = if (cranked > 0) now - cranked else -1L
                    val on = t in 0 until CRANK_MS
                    val dy = if (t in 500 until CRANK_MS) (40 + (t - 500) / 30).toInt().coerceAtMost(54) else 0
                    Scenes.machine(b, rgb(0xefe0bf), if (on) (t / 300.0).coerceAtMost(1.0) else 0.0, if (on) t / 90.0 else 0.0, if (dy > 0) drop[0] else null, dy, ms)
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(w.t("Капсула-сюрприз", "Surprise capsule"), style = T.Title)
                Text(w.t("Капсул: ${s.bag.capsules}. Одна каждый день и ещё по одной за каждые ${Rules.CAPSULE_EVERY} штифта.",
                    "Capsules: ${s.bag.capsules}. One every day and one more for every ${Rules.CAPSULE_EVERY} pins."), style = T.Small)
                Btn(w.t("Крутить ручку", "Turn the crank"), kind = Kind.BRASS, enabled = s.bag.capsules > 0 && cranked == 0L) {
                    drop[0] = Random.nextInt(6)
                    cranked = System.currentTimeMillis()
                    scope.launch {
                        repeat(4) { c.sfx.click(); delay(140) }
                        delay(CRANK_MS - 560)
                        got = c.repo.open()
                        c.sfx.note(4); delay(120); c.sfx.note(7)
                        cranked = 0L
                    }
                }
            }
        }
        FairPanel(h)
        when (val g = got) {
            is Capsule.Challenge -> Panel {
                Text(w.t("Вызов из капсулы", "A challenge from the capsule"), style = T.Title)
                Text(w.t("Сделай сейчас — детали удвоятся.", "Do it now and the parts are doubled."), style = T.Small)
                Box(Modifier.padding(top = 4.dp)) { ExCard(h, g.ex, bonus = true) { onEx(it, true); got = null } }
            }
            is Capsule.Paint -> Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToyIcon(g.toy, true, 0f, 1.5f)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(w.t("Раскраска «${w.paint(g.toy)}»", "The «${w.paint(g.toy)}» paint job"), style = T.Title)
                        Text(w.t("${w.toy(g.toy)}: новая раскраска. Переключить можно ниже.", "${w.toy(g.toy)} has a new coat. Switch it below."), style = T.Small)
                    }
                }
            }
            Capsule.Parts -> Panel {
                Text(w.t("Горсть деталей", "A handful of parts"), style = T.Title)
                Text(w.t("+3 шестерёнки и пружина.", "+3 gears and a spring."), style = T.Small)
            }
            null -> Unit
        }
        Panel {
            if (s.bag.built) {
                Text(w.t("Акробат собран", "The acrobat is built"), style = T.Title)
                Text(w.t("Его упражнения — тяга: полотенце, турник, вис. Он на нижней полке.", "His movements are pulls: a towel, a bar, a hang. He's on the bottom shelf."), style = T.Small)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ToyIcon(Toy.ACRO, false, 0f, 1.25f, sil = 0x7a5a3e)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(w.t("Чертёж: акробат на турнике", "Blueprint: the acrobat on a bar"), style = T.Title)
                        Text(w.t("тяга · нужно ${Rules.ACRO_GEARS} шестерёнок и ${Rules.ACRO_SPRINGS} пружины", "pulling · needs ${Rules.ACRO_GEARS} gears and ${Rules.ACRO_SPRINGS} springs"), style = T.Small)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Bar(s.gears / Rules.ACRO_GEARS.toFloat(), C.brass, Modifier.weight(1f))
                    Text("${s.gears.coerceAtLeast(0)}/${Rules.ACRO_GEARS}", style = T.Small)
                    Bar(s.springs / Rules.ACRO_SPRINGS.toFloat(), C.teal, Modifier.weight(1f))
                    Text("${s.springs.coerceAtLeast(0)}/${Rules.ACRO_SPRINGS}", style = T.Small)
                }
                Btn(w.t("Собрать", "Build"), Modifier.fillMaxWidth().padding(top = 10.dp), enabled = Engine.canBuild(s)) {
                    c.write { repo.build() }
                    c.sfx.note(8)
                }
            }
        }
        Panel {
            Text(w.t("Игрушки", "Toys"), style = T.Title)
            Text(w.t("Каждую можно перелить: жесть, латунь, медь, серебро, золото. Нужны детали и подходы именно с этой игрушкой.",
                "Each can be recast: tin, brass, copper, silver, gold. It takes parts and sets done with that very toy."), style = T.Small)
            for (t in s.toys) ToyRow(h, t)
        }
        Panel {
            Text(w.t("Что есть дома", "Kit at home"), style = T.Title)
            Text(w.t("Остальное — без снаряжения: стул, стена, лестница, пол.", "Everything else needs nothing: a chair, a wall, stairs, the floor."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (k in Kit.entries) {
                    val on = k in s.bag.kit
                    Btn(w.kit(k), kind = if (on) Kind.TEAL else Kind.GHOST) { c.write { repo.kit(k, !on) } }
                }
            }
        }
        Btn(w.t("Настройки", "Settings"), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), kind = Kind.GHOST, onClick = onSettings)
        Text(w.t("Без рекламы и без интернета. Всё хранится только на телефоне.", "No ads, no internet. Everything stays on the phone."),
            style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
    }
}

private val GEAR = listOf("..x.x..", ".xxxxx.", "xxx.xxx", ".x...x.", "xxx.xxx", ".xxxxx.", "..x.x..")
private val SPRING = listOf(".xxx.", "x...x", ".xxx.", "x...x", ".xxx.", "x...x", ".xxx.")

/** A tiny pixel gear or spring, for prices. */
@Composable
fun PartMark(spring: Boolean, color: Color, modifier: Modifier = Modifier) {
    val rows = if (spring) SPRING else GEAR
    Canvas(modifier.size(16.dp)) {
        val p = size.height / 7
        val x0 = (size.width - rows[0].length * p) / 2
        rows.forEachIndexed { j, r -> r.forEachIndexed { i, ch -> if (ch == 'x') drawRect(color, Offset(x0 + i * p, j * p), Size(p, p)) } }
    }
}

@Composable
fun Price(gears: Int, springs: Int, color: Color = C.ink) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        PartMark(false, color); Text("$gears", style = T.Body.copy(color = color))
        Box(Modifier.width(4.dp))
        PartMark(true, color); Text("$springs", style = T.Body.copy(color = color))
    }
}

/** A button with a label over a price in gears and springs. */
@Composable
fun BuyBtn(label: String, gears: Int, springs: Int, enabled: Boolean, onClick: () -> Unit) {
    val fg = if (enabled) C.ink else C.faint
    Column(
        Modifier.px(if (enabled) C.brass else C.paper2, bottom = if (enabled) C.brassDeep else C.paper2, top = if (enabled) C.brassHi else C.paper2)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = T.Body.copy(color = fg))
        Price(gears, springs, fg)
    }
}

/** One toy in the workshop: its metal, how well its movements are known, its rust, and what it can become. */
@Composable
private fun ToyRow(h: Home, t: Toy) {
    val c = LocalContext.current.container
    val w = h.w
    val s = h.s
    val a = s.alloy(t)
    val rust = Engine.rust(s, t, h.nowMs)
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) { h.Icon(t, 1f) }
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("${w.toy(t)} · ${w.alloy(a).lowercase()}", style = T.Body)
            if (a < Rules.GOLD) {
                val need = Rules.ALLOY_MASTERY[a]
                Text(w.t("подходов ${s.mastery(t).coerceAtMost(need)} из $need", "sets ${s.mastery(t).coerceAtMost(need)} of $need"), style = T.Small)
                Bar(s.mastery(t) / need.toFloat(), C.teal, Modifier.fillMaxWidth(), height = 6.dp)
                Text(w.alloy(a + 1) + ": " + w.alloyGift(a + 1), style = T.Small, modifier = Modifier.padding(top = 2.dp))
            } else Text(w.t("золото — вершина", "gold — the top"), style = T.Small)
            if (rust > .05f) Text(w.t("ржавчина ${(rust * 100).toInt()}%", "rust ${(rust * 100).toInt()}%"), style = T.Small.copy(color = C.rust))
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (a < Rules.GOLD) BuyBtn(w.alloy(a + 1), Rules.ALLOY_GEARS[a], Rules.ALLOY_SPRINGS[a], Engine.canAlloy(s, t)) {
                c.write { repo.alloy(t) }
                c.sfx.note(t.note); c.sfx.tone(12)
            }
            if (t in s.bag.paints) Btn(if (s.painted(t)) w.paint(t) else w.tin(t), kind = if (s.painted(t)) Kind.TEAL else Kind.GHOST) {
                c.write { repo.paint(t, !s.painted(t)) }
            } else BuyBtn(w.t("раскраска", "paint"), Rules.PAINT_GEARS, Rules.PAINT_SPRINGS, Engine.canPaint(s, t)) {
                c.write { repo.buyPaint(t) }
                c.sfx.note(t.note)
            }
        }
    }
}
