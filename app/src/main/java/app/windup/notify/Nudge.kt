package app.windup.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.windup.MainActivity
import app.windup.R
import app.windup.container
import app.windup.domain.Engine
import app.windup.domain.State
import app.windup.domain.Wake
import app.windup.domain.date
import app.windup.localized
import app.windup.ru
import app.windup.ui.Words
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * The only reminder: when a toy has stood still for a while during waking hours, it asks for its key.
 * Unanswered, the asks space out, and after three of them the shelf keeps quiet until the next day.
 */
object Nudge {
    private const val CHANNEL = "nudge"
    private const val WORK = "nudge"
    private const val MAX_UNANSWERED = 3

    fun channel(c: Context) {
        val ch = NotificationChannel(CHANNEL, c.localized().let { if (it.ru) "Ключ ждёт" else "The key is waiting" }, NotificationManager.IMPORTANCE_DEFAULT)
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    /**
     * When the next ask is due: [every] minutes after the last pin, or an hour after waking on a day
     * without pins, moved into the waking hours.
     */
    fun due(s: State, wake: Wake, every: Int, now: Long, zone: ZoneId, unanswered: Int = 0): Long? {
        if (every <= 0) return null
        fun at(d: LocalDate, minute: Int) = d.atStartOfDay(zone).toInstant().toEpochMilli() + minute * 60_000L
        val today = now.date(zone)
        val last = s.last()?.at
        var t = if (last != null && last.date(zone) == today) last + every * 60_000L * (1 + unanswered) else at(today, wake.from + 60)
        if (unanswered >= MAX_UNANSWERED) t = at(today.plusDays(1), wake.from + 60)
        if (t < now) t = now + 60_000L
        val d = t.date(zone)
        val m = ((t - at(d, 0)) / 60_000L).toInt()
        return when {
            m < wake.from + 30 -> at(d, wake.from + 60)
            m > wake.to - 30 -> at(d.plusDays(1), wake.from + 60)
            else -> t
        }
    }

    /** Re-plans the next ask; called after every pin and on opening the app. */
    suspend fun plan(c: Context, unanswered: Int = 0) {
        val app = c.applicationContext
        val st = app.container.settings.now()
        val s = app.container.repo.state.value ?: app.container.repo.refresh()
        val wm = WorkManager.getInstance(app)
        val now = System.currentTimeMillis()
        val t = if (st.onboarded) due(s, st.wake, st.nudge, now, ZoneId.systemDefault(), unanswered) else null
        if (t == null) { wm.cancelUniqueWork(WORK); return }
        val req = OneTimeWorkRequestBuilder<NudgeWorker>()
            .setInitialDelay((t - now).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putInt("n", unanswered).putLong("last", s.last()?.at ?: 0).build())
            .build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, req)
    }

    fun post(c: Context, s: State, now: Long) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val lc = c.localized()
        val w = Words(lc.ru)
        val toy = s.toys.filter { Engine.wind(s, it, now) <= 0f }.maxByOrNull { Engine.rust(s, it, now) + (now - (s.last(it)?.at ?: 0)) / 1e12f } ?: return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_key)
            .setContentTitle(w.stopped(toy))
            .setContentText(w.nudgeLine(toy, now))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(1, n)
    }
}

class NudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.repo.refresh()
        val st = c.settings.now()
        val n = inputData.getInt("n", 0)
        val answered = (s.last()?.at ?: 0) != inputData.getLong("last", 0)
        val now = System.currentTimeMillis()
        val minute = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }
        if (!answered && minute in st.wake.from..st.wake.to) Nudge.post(applicationContext, s, now)
        Nudge.plan(applicationContext, if (answered) 0 else n + 1)
        return Result.success()
    }
}
