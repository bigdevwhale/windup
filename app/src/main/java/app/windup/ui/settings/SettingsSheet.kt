package app.windup.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import app.windup.BuildConfig
import app.windup.container
import app.windup.data.Transfer
import app.windup.domain.date
import app.windup.notify.Nudge
import app.windup.ui.Home
import app.windup.ui.components.Chip
import app.windup.ui.components.CommunityLinks
import app.windup.ui.components.Flow
import app.windup.ui.components.Panel
import app.windup.ui.components.Sheet
import app.windup.ui.theme.C
import app.windup.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

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
                Chip("Русский", h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru")) }
                Chip("English", !h.ru) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
            }
        }
        Panel {
            Text(w.t("Оформление", "Look"), style = T.Title)
            Text(w.t("Днём — бумага и латунь, вечером — тёмное дерево и сукно.", "By day paper and brass, by evening dark walnut and felt."), style = T.Small)
            Flow(Modifier.padding(top = 10.dp)) {
                for ((v, label) in listOf(0 to w.t("как в системе", "as the phone"), 1 to w.t("день", "day"), 2 to w.t("вечер", "evening"))) {
                    Chip(label, st.look == v) { save { it.copy(look = v) } }
                }
            }
        }
        WakePanel(w, st.wake.from, st.wake.to, onFrom = { v -> save { it.copy(wake = it.wake.copy(from = v)) } }, onTo = { v -> save { it.copy(wake = it.wake.copy(to = v)) } })
        Panel {
            Text(w.t("Ключ ждёт", "The key is waiting"), style = T.Title)
            Text(w.t("Когда игрушки долго стоят днём, приходит одно тихое напоминание. Без ответа они реже, а после трёх — тишина до утра.",
                "When the toys stand still for a while during the day, one quiet reminder comes. Unanswered, they space out, and after three it's quiet till morning."), style = T.Small)
            Flow(Modifier.padding(top = 8.dp)) {
                for (m in listOf(0, 60, 90, 120, 180)) Chip(if (m == 0) w.t("никогда", "never") else w.hm(m), st.nudge == m) { save { it.copy(nudge = m) } }
            }
        }
        Panel {
            Text(w.t("Звук", "Sound"), style = T.Title)
            Flow(Modifier.padding(top = 8.dp)) {
                Chip(w.t("щелчки и ноты", "clicks and notes"), st.sound) { save { it.copy(sound = true) } }
                Chip(w.t("тихо", "silent"), !st.sound) { save { it.copy(sound = false) } }
            }
        }
        Panel {
            Text(w.t("Подсказки", "Guides"), style = T.Title)
            Text(w.t("На каждом экране есть подсказка: она показывается при первом заходе, а знак «?» в шапке открывает её снова.",
                "Every screen has a guide: it shows the first time you visit, and the \"?\" in the top bar opens it again."), style = T.Small)
            Chip(w.t("Показать все подсказки заново", "Show all guides again"), false, Modifier.padding(top = 10.dp)) { save { it.copy(seen = emptySet()) } }
        }
        TransferPanel(h)
        CommunityLinks(w)
        Panel {
            Text(w.t("О «Заводилке»", "About Windup"), style = T.Title)
            Text(w.t("Нас приучили, что спорт — это час пару раз в неделю. Тело любит иначе: понемногу, весь день. Игрушки на полке — напоминание об этом.",
                "We were taught that exercise is an hour a couple of times a week. The body prefers a little, all day long. The toys on the shelf are a reminder of that."), style = T.Small)
            Text(w.t("Версия ${BuildConfig.VERSION_NAME} · шрифт Nunito (OFL) · исходники на GitHub", "Version ${BuildConfig.VERSION_NAME} · Nunito font (OFL) · source on GitHub"),
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
            for (hr in 5..11) Chip(w.clock(hr * 60), from == hr * 60) { onFrom(hr * 60) }
        }
        Text(w.t("Отбой", "Bed at"), style = T.Body, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Flow(Modifier.padding(top = 4.dp)) {
            for (hr in 20..24) Chip(w.clock(hr * 60), to == hr * 60) { onTo(hr * 60) }
        }
    }
}

/**
 * Carrying the shelf to another phone: a file saved wherever the person likes (Drive, Telegram, a cable), then
 * opened on the other phone. The app has no network, so the file is the whole way across.
 */
@Composable
private fun TransferPanel(h: Home) {
    val ctx = LocalContext.current
    val c = ctx.container
    val w = h.w
    val scope = rememberCoroutineScope()
    var note by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<Transfer.Snapshot?>(null) }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            note = try {
                val text = Transfer.write(c.repo.refresh(), c.settings.now())
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
                w.t("Сохранено: ${w.pins(h.s.pins.size)}. Откройте этот файл в «Заводилке» на другом телефоне.",
                    "Saved: ${w.pins(h.s.pins.size)}. Open this file in Windup on the other phone.")
            } catch (e: Exception) {
                w.t("Не получилось сохранить файл.", "Couldn't save the file.")
            }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val text = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                pending = withContext(Dispatchers.Default) { Transfer.read(text) }
                note = null
            } catch (e: Exception) {
                pending = null
                note = w.t("Это не файл «Заводилки» или он из более новой версии.", "This isn't a Windup file, or it's from a newer version.")
            }
        }
    }

    Panel {
        Text(w.t("На другой телефон", "To another phone"), style = T.Title)
        Text(w.t("Вся полка — штифты, игрушки, детали и настройки — в одном файле. Сохраните его куда удобно и откройте здесь же на новом телефоне.",
            "The whole shelf — pins, toys, parts and settings — in one file. Save it wherever suits you and open it here on the new phone."), style = T.Small)
        val p = pending
        if (p == null) {
            Flow(Modifier.padding(top = 10.dp)) {
                Chip(w.t("Сохранить в файл", "Save to a file"), false) {
                    note = null
                    save.launch("windup-${LocalDate.now()}.json")
                }
                Chip(w.t("Загрузить из файла", "Load from a file"), false) {
                    note = null
                    open.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                }
            }
        } else {
            val from = if (p.at > 0) " " + w.t("от ", "from ") + p.at.date(h.zone).format(java.time.format.DateTimeFormatter.ofPattern("d.MM.yyyy")) else ""
            Text(w.t("В файле$from ${w.pins(p.pins.size)}. Всё, что сейчас на этом телефоне (${w.pins(h.s.pins.size)}), заменится им.",
                "The file$from holds ${w.pins(p.pins.size)}. Everything on this phone now (${w.pins(h.s.pins.size)}) will be replaced by it."),
                style = T.Body, modifier = Modifier.padding(top = 10.dp))
            Flow(Modifier.padding(top = 10.dp)) {
                Chip(w.t("Заменить", "Replace"), true) {
                    pending = null
                    c.write {
                        repo.replace(p.pins, p.bag)
                        settings.update(p::applyTo)
                        Nudge.plan(ctx)
                    }
                    note = w.t("Готово: полка перенесена.", "Done: the shelf has moved in.")
                }
                Chip(w.t("Отмена", "Cancel"), false) { pending = null }
            }
        }
        note?.let { Text(it, style = T.Small.copy(color = C.tin), modifier = Modifier.padding(top = 8.dp)) }
    }
}
