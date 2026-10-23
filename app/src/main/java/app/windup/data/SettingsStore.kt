package app.windup.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.windup.domain.Wake
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    val wake: Wake = Wake(),
    /** Minutes without a pin before a stopped toy asks for its key; 0 = never. */
    val nudge: Int = 90,
    val sound: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        wake = Wake(this[WAKE_FROM] ?: Wake().from, this[WAKE_TO] ?: Wake().to),
        nudge = this[NUDGE] ?: 90,
        sound = this[SOUND] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[WAKE_FROM] = s.wake.from
            it[WAKE_TO] = s.wake.to
            it[NUDGE] = s.nudge
            it[SOUND] = s.sound
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val WAKE_FROM = intPreferencesKey("wake_from")
        val WAKE_TO = intPreferencesKey("wake_to")
        val NUDGE = intPreferencesKey("nudge")
        val SOUND = booleanPreferencesKey("sound")
    }
}
