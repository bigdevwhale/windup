package app.windup.ui.ex

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.HapticFeedbackConstants
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.domain.Engine
import app.windup.domain.Ex
import app.windup.domain.Outcome
import app.windup.notify.Nudge
import app.windup.ui.Home
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.components.Btn
import app.windup.ui.components.Dots
import app.windup.ui.components.Kind
import app.windup.ui.components.Panel
import app.windup.ui.components.TopBar
import app.windup.ui.shelf.run
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * One set at the workbench: the toy stands on the bench, every rep is a click of its key, a hold ticks by
 * itself, and the phone in a pocket can count squats and jumps.
 */
@Composable
fun ExerciseScreen(h: Home, e: Ex, bonus: Boolean, onClose: () -> Unit, onDone: (Outcome) -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val view = LocalView.current
    val w = h.w
    val scope = rememberCoroutineScope()
    val step = h.s.step(e)
    var count by remember { mutableIntStateOf(0) }
    var holding by remember { mutableStateOf(false) }
    var pocket by remember { mutableStateOf(false) }
    var lastRep by remember { mutableLongStateOf(0L) }
    var saving by remember { mutableStateOf(false) }
    val st = remember { St() }
    val key = remember { doubleArrayOf(.4, .4) }
    val target = e.target

    fun rep() {
        count++
        key[1] += PI
        lastRep = System.currentTimeMillis()
        c.sfx.click()
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        if (count == target) c.sfx.note(e.toy.note)
    }

    BackHandler(onBack = onClose)

    if (e.hold && holding) LaunchedEffect(Unit) {
        while (holding && count < target) { delay(1000); if (holding) rep() }
        holding = false
    }

    if (pocket) DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var armed = true
        var peak = 0L
        val l = object : SensorEventListener {
            override fun onSensorChanged(ev: SensorEvent) {
                val m = sqrt(ev.values[0] * ev.values[0] + ev.values[1] * ev.values[1] + ev.values[2] * ev.values[2])
                val now = System.currentTimeMillis()
                if (armed && m > 13.5f && now - peak > 550) { armed = false; peak = now; rep() }
                if (m < 10.5f) armed = true
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
        }
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        onDispose { sm.unregisterListener(l) }
    }

    Column(Modifier.fillMaxSize().background(C.paper)) {
        TopBar(w.toy(e.toy), w.pattern(e.toy)) {
            Text("${(count * 100 / target).coerceAtMost(100)}%", style = T.Heading.copy(color = C.wallText), modifier = Modifier.clickable(onClick = onClose).padding(4.dp))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            PixelView(Scenes.EW, Scenes.EH) { b, dt, _ ->
                b.copyFrom(Scenes.bench)
                // Each click turns the key half a round and lets the toy run a little; a finished set keeps it going.
                key[0] += (key[1] - key[0]) * (dt / .09).coerceAtMost(1.0)
                val since = System.currentTimeMillis() - lastRep
                val r = when { count >= target || holding -> 1.0; lastRep > 0 && since < 1600 -> run((1 - since / 1600f).coerceIn(0f, 1f)); else -> 0.0 }
                val k = key[0]
                Toys.step(Toys[e.toy], st, dt, r)
                st.kph = k
                Toys.drawAt(b, e.toy, Toys.parts(e.toy, h.s.painted(e.toy), Engine.rust(h.s, e.toy, h.nowMs)), Scenes.EW / 2, Scenes.ETOP, st, r)
                Scenes.gauge(b, count / target.toFloat())
            }
            Panel {
                Text(w.name(e, step), style = T.Display)
                Text(w.cue(e), style = T.Body.copy(color = C.muted), modifier = Modifier.padding(top = 6.dp))
                if (e.ladder.isNotEmpty()) {
                    Dots(step, e.ladder.size, Modifier.fillMaxWidth().padding(top = 10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            w.t("ступень ${step + 1} из ${e.ladder.size}", "step ${step + 1} of ${e.ladder.size}") +
                                (if (step < e.ladder.size - 1) w.t(" · дальше: ", " · next: ") + w.name(e, step + 1) else ""),
                            style = T.Small, modifier = Modifier.weight(1f),
                        )
                        if (step > 0) Text(w.t("легче", "easier"), style = T.Small.copy(color = C.tin), modifier = Modifier.clickable { c.write { repo.step(e, step - 1) } }.padding(6.dp))
                        if (step < e.ladder.size - 1) Text(w.t("сложнее", "harder"), style = T.Small.copy(color = C.tin), modifier = Modifier.clickable { c.write { repo.step(e, step + 1) } }.padding(6.dp))
                    }
                }
            }
            Text(
                buildAnnotatedString {
                    append("$count ")
                    withStyle(SpanStyle(fontSize = T.Display.fontSize, color = C.muted)) { append("/ " + w.amount(target, e.unit)) }
                },
                style = T.Huge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        Column(Modifier.navigationBarsPadding().padding(bottom = 6.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn(
                    if (e.hold) (if (holding) w.t("Пауза", "Pause") else w.t("Держу", "Holding")) else w.t("Щёлк!", "Click!"),
                    Modifier.weight(2f), kind = Kind.BRASS, big = true, enabled = !e.hold || count < target,
                ) { if (e.hold) holding = !holding else rep() }
                if (e.pocket && !e.hold) Btn(w.t("в кармане", "in pocket"), Modifier.weight(1f), kind = if (pocket) Kind.TEAL else Kind.GHOST) { pocket = !pocket }
            }
            Box(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Btn(
                    if (count >= target) w.t("Готово — забить штифт", "Done — hammer the pin") else w.t("Закончить раньше", "Finish early"),
                    Modifier.fillMaxWidth(), enabled = count > 0 && !saving,
                ) {
                    saving = true; holding = false; pocket = false
                    scope.launch {
                        val out = c.repo.pin(e, count, bonus)
                        if (out.pin.over) c.sfx.ping() else c.sfx.note(e.toy.note)
                        Nudge.plan(ctx)
                        onDone(out)
                    }
                }
            }
        }
    }
}
