package app.windup.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.windup.container
import app.windup.data.Settings
import app.windup.domain.Toy
import app.windup.ui.Words
import app.windup.ui.art.PixelView
import app.windup.ui.art.Scenes
import app.windup.ui.art.St
import app.windup.ui.art.Toys
import app.windup.ui.components.Btn
import app.windup.ui.components.Panel
import app.windup.ui.components.TopBar
import app.windup.ui.settings.WakePanel
import app.windup.ui.theme.C
import app.windup.ui.theme.T

/** Three pages: the idea, how the shelf works, and the waking day. */
@Composable
fun Onboarding(w: Words, settings: Settings) {
    val ctx = LocalContext.current
    val c = ctx.container
    var page by remember { mutableIntStateOf(0) }
    val toys = remember { Toy.entries.associateWith { St() } }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(Modifier.fillMaxSize().background(C.paper)) {
        TopBar(w.t("Завод", "Windup"), w.t("${page + 1} из 3", "${page + 1} of 3"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (page < 2) PixelView(Scenes.SW, Scenes.SH) { b, dt, _ ->
                b.copyFrom(Scenes.shelf)
                for (t in Toy.entries) {
                    val st = toys.getValue(t)
                    val run = if (page == 0 && t.ordinal % 2 == 1) 0.0 else 1.0
                    if (t.blueprint) { Toys.drawAt(b, t, Toys.parts(t, false, 0f), Scenes.SLOT[2], Scenes.PLANK[2], st, 0.0, sil = 0x2a4744, shadow = false); continue }
                    Toys.step(Toys[t], st, dt, run)
                    Toys.drawAt(b, t, Toys.parts(t, false, 0f), Scenes.SLOT[t.ordinal % 3], Scenes.PLANK[t.ordinal / 3], st, run)
                    Scenes.plate(b, Scenes.SLOT[t.ordinal % 3], Scenes.PLANK[t.ordinal / 3], run.toFloat())
                }
            }
            when (page) {
                0 -> Panel {
                    Text(w.t("Час спорта дважды в неделю — не единственный путь", "An hour of exercise twice a week isn't the only way"), style = T.Title)
                    Text(w.t("Телу естественнее двигаться понемногу, но весь день: присесть, пока закипает чайник, постоять на одной ноге, пока чистишь зубы, подняться по лестнице.",
                        "It's more natural for the body to move a little, all day long: a few squats while the kettle boils, standing on one leg while brushing your teeth, taking the stairs."),
                        style = T.Body, modifier = Modifier.padding(top = 8.dp))
                    Text(w.t("Как заводные игрушки: завода хватает ненадолго, и крутить ключ нужно часто — понемногу.",
                        "Like wind-up toys: a winding doesn't last long, so the key wants turning often — a little at a time."),
                        style = T.Body.copy(color = C.muted), modifier = Modifier.padding(top = 8.dp))
                }
                1 -> Panel {
                    Text(w.t("Как это устроено", "How it works"), style = T.Title)
                    for (l in listOf(
                        w.t("Девять жестяных игрушек — девять видов движения: присед, жим, корпус, баланс, осанка, наклоны, повороты, стопы, тяга.",
                            "Nine tin toys, nine kinds of movement: squats, pushing, core, balance, posture, hinges, turns, feet and pulling."),
                        w.t("Разминка на 1–3 минуты заводит игрушку на два часа. Каждый повтор — щелчок ключа.",
                            "A 1–3 minute set winds a toy for two hours. Every rep is a click of the key."),
                        w.t("Каждый завод — штифт в барабане шкатулки. Вечером день играет мелодию: разнесённые по дню штифты звучат красиво, сделанные разом — кашей.",
                            "Each winding is a pin in the music box drum. In the evening the day plays: pins spread over the day sound like a tune, crammed together — a mush."),
                        w.t("Каждое утро — партитура: четыре игрушки, по одной на четверть дня. Сыграешь — получишь пластинку в альбом.",
                            "Each morning brings a score: four toys, one for each quarter of the day. Play it and a record goes into the album."),
                        w.t("Шестерёнки и пружины переливают игрушки из жести в латунь, медь, серебро и золото. Все за день — парад; заброшенные ржавеют.",
                            "Gears and springs recast the toys from tin to brass, copper, silver and gold. All in a day is a parade; neglected toys rust."),
                    )) Text("· $l", style = T.Body, modifier = Modifier.padding(top = 8.dp))
                }
                else -> {
                    WakePanel(w, settings.wake.from, settings.wake.to,
                        onFrom = { v -> c.write { this.settings.update { it.copy(wake = it.wake.copy(from = v)) } } },
                        onTo = { v -> c.write { this.settings.update { it.copy(wake = it.wake.copy(to = v)) } } })
                    Panel {
                        Text(w.t("Ключ ждёт", "The key is waiting"), style = T.Title)
                        Text(w.t("Если игрушки полтора часа стоят, придёт одно тихое напоминание — только днём. Его можно выключить в настройках.",
                            "If the toys stand still for an hour and a half, one quiet reminder comes — daytime only. You can turn it off in settings."),
                            style = T.Body, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
        Column(Modifier.navigationBarsPadding().padding(14.dp)) {
            Btn(if (page < 2) w.t("Дальше", "Next") else w.t("Открыть мастерскую", "Open the workshop"), Modifier.fillMaxWidth()) {
                if (page < 2) page++ else {
                    if (Build.VERSION.SDK_INT >= 33) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    c.write { repo.start(); this.settings.update { it.copy(onboarded = true) } }
                }
            }
        }
    }
}
