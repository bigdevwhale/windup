package app.windup.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.windup.BuildConfig
import app.windup.container
import app.windup.notify.Nudge
import app.windup.ui.Home
import app.windup.ui.components.Btn
import app.windup.ui.components.Flow
import app.windup.ui.components.Kind
import app.windup.ui.components.Panel
import app.windup.ui.components.Sheet
import app.windup.ui.theme.C
import app.windup.ui.theme.T

@Composable
fun SettingsSheet(h: Home, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val st = h.settings
    fun save(f: (app.windup.data.Settings) -> app.windup.data.Settings) = c.write { settings.update(f); Nudge.plan(ctx) }
    Sheet(w.t("Настройки", "Settings"), onClose, w.t("Закрыть", "Close")) {
        Panel {
            Text(w.t("Язык", "Language"), style = T.Title)
            Flow(Modifier.padding(top = 8.dp)) {
                Btn("Русский", kind = if (h.ru) Kind.TEAL else Kind.GHOST) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Btn("English", kind = if (!h.ru) Kind.TEAL else Kind.GHOST) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
        }
        WakePanel(w, st.wake.from, st.wake.to, onFrom = { v -> save { it.copy(wake = it.wake.copy(from = v)) } }, onTo = { v -> save { it.copy(wake = it.wake.copy(to = v)) } })
        Panel {
            Text(w.t("Ключ ждёт", "The key is waiting"), style = T.Title)
            Text(w.t("Когда игрушки долго стоят днём, приходит одно тихое напоминание. Без ответа они реже, а после трёх — тишина до утра.",
                "When the toys stand still for a while during the day, one quiet reminder comes. Unanswered, they space out, and after three it's quiet till morning."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (m in listOf(0, 60, 90, 120, 180)) Btn(if (m == 0) w.t("никогда", "never") else w.hm(m), kind = if (st.nudge == m) Kind.TEAL else Kind.GHOST) { save { it.copy(nudge = m) } }
            }
        }
        Panel {
            Text(w.t("Звук", "Sound"), style = T.Title)
            Flow(Modifier.padding(top = 8.dp)) {
                Btn(w.t("щелчки и ноты", "clicks and notes"), kind = if (st.sound) Kind.TEAL else Kind.GHOST) { save { it.copy(sound = true) } }
                Btn(w.t("тихо", "silent"), kind = if (!st.sound) Kind.TEAL else Kind.GHOST) { save { it.copy(sound = false) } }
            }
        }
        Panel {
            Text(w.t("О «Заводе»", "About Windup"), style = T.Title)
            Text(w.t("Нас приучили, что спорт — это час пару раз в неделю. Тело любит иначе: понемногу, весь день. Игрушки на полке — напоминание об этом.",
                "We were taught that exercise is an hour a couple of times a week. The body prefers a little, all day long. The toys on the shelf are a reminder of that."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифт m6x11 — Daniel Linssen · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · m6x11 font by Daniel Linssen · source on GitHub"),
                style = T.Small.copy(color = C.tin),
                modifier = Modifier.padding(top = 8.dp).clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/bigdevwhale/windup"))) })
        }
    }
}

/** The waking hours: the music box drum spans them, and reminders keep inside them. */
@Composable
fun WakePanel(w: app.windup.ui.Words, from: Int, to: Int, onFrom: (Int) -> Unit, onTo: (Int) -> Unit) {
    Panel {
        Text(w.t("Мой день", "My day"), style = T.Title)
        Text(w.t("Барабан шкатулки — от подъёма до отбоя.", "The music box drum runs from getting up to bedtime."), style = T.Small)
        Text(w.t("Подъём", "Up at"), style = T.Body, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Flow(Modifier.padding(top = 4.dp)) {
            for (hr in 5..11) Btn(w.clock(hr * 60), kind = if (from == hr * 60) Kind.TEAL else Kind.GHOST) { onFrom(hr * 60) }
        }
        Text(w.t("Отбой", "Bed at"), style = T.Body, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Flow(Modifier.padding(top = 4.dp)) {
            for (hr in 20..24) Btn(w.clock(hr * 60), kind = if (to == hr * 60) Kind.TEAL else Kind.GHOST) { onTo(hr * 60) }
        }
    }
}
