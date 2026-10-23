package app.windup.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import app.windup.ui.theme.T
import kotlinx.coroutines.delay
import java.time.ZoneId

/** Everything a screen reads, at one moment. */
class Home(val s: State, val settings: Settings, val w: Words, val nowMs: Long, val toys: Map<Toy, St>) {
    val ru get() = w.ru
    val zone: ZoneId = ZoneId.systemDefault()
}

enum class Tab { SHELF, BOX, SHOP }

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
    val s = state ?: run { Box(Modifier.fillMaxSize().background(C.paper)); return }
    val toys = remember { Toy.entries.associateWith { St() } }
    val home = Home(s, settings, words, nowMs, toys)

    var tab by remember { mutableStateOf(Tab.SHELF) }
    val overlays = remember { mutableStateListOf<Overlay>() }
    fun push(o: Overlay) = overlays.add(o)
    fun pop() { if (overlays.isNotEmpty()) overlays.removeAt(overlays.lastIndex) }
    BackHandler(enabled = overlays.isEmpty() && tab != Tab.SHELF) { tab = Tab.SHELF }

    Box(Modifier.fillMaxSize().background(C.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    Tab.SHELF -> ShelfScreen(home, onToy = { push(Overlay.ToyCard(it)) }, onWind = { push(Overlay.Where) })
                    Tab.BOX -> BoxScreen(home)
                    Tab.SHOP -> ShopScreen(home, onEx = { e, b -> push(Overlay.Exercise(e, b)) }, onSettings = { push(Overlay.Settings) })
                }
            }
            TabBar(tab) { tab = it }
        }
        when (val o = overlays.lastOrNull()) {
            is Overlay.ToyCard -> ToySheet(home, o.toy, onClose = { pop() }, onEx = { pop(); push(Overlay.Exercise(it, false)) })
            Overlay.Where -> WhereSheet(home, onClose = { pop() }, onEx = { pop(); push(Overlay.Exercise(it, false)) })
            is Overlay.Exercise -> ExerciseScreen(home, o.ex, o.bonus, onClose = { pop() }, onDone = { pop(); push(Overlay.Result(it)) })
            is Overlay.Result -> ResultSheet(home, o.out, onClose = { pop(); tab = Tab.SHELF })
            Overlay.Settings -> SettingsSheet(home, onClose = { pop() })
            null -> Unit
        }
    }
}

@Composable
private fun TabBar(tab: Tab, onTab: (Tab) -> Unit) {
    val items = listOf(Tab.SHELF to R.string.tab_shelf, Tab.BOX to R.string.tab_box, Tab.SHOP to R.string.tab_shop)
    Row(Modifier.fillMaxWidth().background(C.wood).navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for ((t, label) in items) {
            val sel = t == tab
            Box(Modifier.weight(1f).background(if (sel) C.woodHi else C.wood).clickable { onTab(t) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(label), style = T.Body.copy(color = if (sel) C.brass else C.faint))
            }
        }
    }
}
