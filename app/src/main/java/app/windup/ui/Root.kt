package app.windup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handyman
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Insights
import app.windup.ui.stats.StatsScreen
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.windup.R
import app.windup.container
import app.windup.data.Settings
import app.windup.domain.Ex
import app.windup.domain.Outcome
import app.windup.domain.State
import app.windup.domain.Toy
import app.windup.notify.Nudge
import app.windup.ru
import app.windup.ui.art.St
import app.windup.ui.box.BoxScreen
import app.windup.ui.ex.ExerciseScreen
import app.windup.ui.ex.ResultSheet
import app.windup.ui.onboarding.Onboarding
import app.windup.ui.settings.SettingsSheet
import app.windup.ui.shelf.ShelfScreen
import app.windup.ui.shelf.ToySheet
import app.windup.ui.shelf.WhereSheet
import app.windup.ui.shop.ShopScreen
import app.windup.ui.theme.C
import app.windup.ui.components.screen
import app.windup.ui.theme.T
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val nowMs: Long, val toys: Map<Toy, St>) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
}

enum class Tab { SHELF, BOX, STATS, SHOP }

sealed interface Overlay {
    data class ToyCard(val toy: Toy) : Overlay
    data object Where : Overlay
    data class Exercise(val ex: Ex, val bonus: Boolean) : Overlay
    data class Result(val out: Outcome) : Overlay
    data object Settings : Overlay
}

@Composable
fun WindupRoot(settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    val state by c.repo.state.collectAsStateWithLifecycle()
    val words = remember(ctx.ru) { Words(ctx.ru) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        c.repo.refresh()
        while (true) { delay(15_000); nowMs = System.currentTimeMillis() }
    }
    LaunchedEffect(settings.sound) { c.sfx.on = settings.sound }
    if (!settings.onboarded) {
        Onboarding(words, settings)
        return
    }
    LaunchedEffect(Unit) { c.repo.daily(); Nudge.plan(ctx) }
    val s = state ?: run { Box(Modifier.fillMaxSize().screen()); return }
    val toys = remember { Toy.entries.associateWith { St() } }
    val home = Home(s, settings, words, nowMs, toys)

    var tab by remember { mutableStateOf(Tab.SHELF) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.SHELF) { tab = Tab.SHELF }

    Box(Modifier.fillMaxSize().screen()) {
        Column(Modifier.fillMaxSize()) {
            Crossfade(tab, Modifier.weight(1f).fillMaxWidth(), animationSpec = tween(180), label = "tab") { shown ->
                when (shown) {
                    Tab.SHELF -> ShelfScreen(home, onToy = { push(Overlay.ToyCard(it)) }, onWind = { push(Overlay.Where) })
                    Tab.BOX -> BoxScreen(home)
                    Tab.STATS -> StatsScreen(home)
                    Tab.SHOP -> ShopScreen(home, onEx = { e, b -> push(Overlay.Exercise(e, b)) }, onSettings = { push(Overlay.Settings) })
                }
            }
            TabBar(tab, s.bag.capsules) { tab = it }
        }
        // The overlays stack: an exercise opened from a toy's list or from "where are you" sits on top of it, so
        // Back (or closing the exercise) returns to the list just as it was. The top one handles Back.
        overlays.forEachIndexed { i, o ->
            key(i, o) {
                when (o) {
                    is Overlay.ToyCard -> ToySheet(home, o.toy, onClose = { pop() }, onEx = { push(Overlay.Exercise(it, false)) })
                    Overlay.Where -> WhereSheet(home, onClose = { pop() }, onEx = { push(Overlay.Exercise(it, false)) })
                    // A finished set is done with the list it came from: the result stands alone, then the shelf.
                    is Overlay.Exercise -> ExerciseScreen(home, o.ex, o.bonus, onClose = { pop() }, onDone = { overlays.clear(); push(Overlay.Result(it)) })
                    is Overlay.Result -> ResultSheet(home, o.out, onClose = { overlays.clear(); tab = Tab.SHELF })
                    Overlay.Settings -> SettingsSheet(home, onClose = { pop() })
                }
            }
        }
    }
}

/**
 * The tabs along the bottom: a dark board like the wall above the shelf, each tab an icon in a brass pill when
 * it's the one open, its name under it. The workshop shows how many capsules are waiting.
 */
@Composable
private fun TabBar(tab: Tab, capsules: Int, onTab: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.SHELF, R.string.tab_shelf, Icons.Rounded.SmartToy),
        Triple(Tab.BOX, R.string.tab_box, Icons.Rounded.LibraryMusic),
        Triple(Tab.STATS, R.string.tab_stats, Icons.Rounded.Insights),
        Triple(Tab.SHOP, R.string.tab_shop, Icons.Rounded.Handyman),
    )
    Row(
        Modifier.fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
            .background(C.navBg)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        for ((t, label, icon) in items) {
            val sel = t == tab
            val pill by animateColorAsState(if (sel) C.brass else C.navBg, tween(220), label = "pill")
            val ink by animateColorAsState(if (sel) C.onBrass else C.navInk, tween(220), label = "ink")
            val text by animateColorAsState(if (sel) C.brass else C.navInk, tween(220), label = "text")
            val wide by animateDpAsState(if (sel) 64.dp else 44.dp, tween(260), label = "wide")
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(18.dp))
                    .selectable(selected = sel, role = Role.Tab, onClick = { onTab(t) }).padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width(wide).height(32.dp).clip(RoundedCornerShape(16.dp)).background(pill), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = ink, modifier = Modifier.size(22.dp))
                    if (t == Tab.SHOP && capsules > 0) Box(
                        Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp).size(16.dp).clip(CircleShape).background(C.tin),
                        contentAlignment = Alignment.Center,
                    ) { Text("$capsules", style = T.Label.copy(color = C.light, fontSize = 10.sp, lineHeight = 10.sp, letterSpacing = 0.sp)) }
                }
                Text(stringResource(label), style = T.Label.copy(color = text, fontSize = 12.sp, letterSpacing = .2.sp), modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
