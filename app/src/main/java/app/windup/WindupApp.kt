package app.windup

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import app.windup.data.DemoSeed
import app.windup.data.Repo
import app.windup.data.SettingsStore
import app.windup.data.ShelfDb
import app.windup.notify.Nudge
import app.windup.sound.Sfx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = ShelfDb.create(app)
    val repo = Repo(db)
    val settings = SettingsStore(app)
    val sfx = Sfx(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Fire-and-forget so a write survives the screen being closed right after tapping. */
    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }
}

class WindupApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Nudge.channel(this)
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@WindupApp, container)
            container.repo.refresh()
            container.sfx.load()
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as WindupApp).container

/** Resources in the in-app language, which AppCompat doesn't localise outside activities before Android 13. */
fun Context.localized(): Context {
    val tags = AppCompatDelegate.getApplicationLocales()
    if (tags.isEmpty) return this
    val config = Configuration(resources.configuration)
    config.setLocale(tags[0])
    return createConfigurationContext(config)
}

/** Is this context in Russian? */
val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"
